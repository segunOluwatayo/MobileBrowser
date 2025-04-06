package com.example.mobilebrowser.data.repository

import android.util.Log
import com.example.mobilebrowser.api.TabApiService
import com.example.mobilebrowser.data.dao.TabDao
import com.example.mobilebrowser.data.dto.TabDto
import com.example.mobilebrowser.data.entity.SyncStatus
import com.example.mobilebrowser.data.entity.TabEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class that abstracts the data operations for tabs.
 * Provides a clean API for the rest of the application to interact with the data layer.
 */
@Singleton
class TabRepository @Inject constructor(
    private val tabDao: TabDao,
    private val tabApiService: TabApiService
) {
    /**
     * Gets all tabs ordered by position
     */
    fun getAllTabs(): Flow<List<TabEntity>> = tabDao.getAllTabs()

    /**
     * Gets the currently active tab
     */
    fun getActiveTab(): Flow<TabEntity?> = tabDao.getActiveTab()

    /**
     * Gets the count of open tabs
     */
    fun getTabCount(): Flow<Int> = tabDao.getTabCount()

    /**
     * Creates a new tab
     */
    suspend fun createTab(url: String, title: String, position: Int): Long {
        // Deactivate all existing tabs
        tabDao.deactivateAllTabs()

        // Create and insert the new tab
        val tab = TabEntity(
            url = url,
            title = title,
            position = position,
            isActive = true
        )
        return tabDao.insertTab(tab)
    }

    /**
     * Updates an existing tab
     */
    suspend fun updateTab(tab: TabEntity) = tabDao.updateTab(tab)

    /**
     * Deletes a specific tab
     */
    suspend fun deleteTab(tab: TabEntity) = tabDao.deleteTab(tab)

    /**
     * Deletes all tabs
     */
    suspend fun deleteAllTabs() = tabDao.deleteAllTabs()

    /**
     * Switches to a specific tab
     */
    suspend fun switchToTab(tabId: Long) {
        tabDao.deactivateAllTabs()
        tabDao.setTabActive(tabId)
    }

    /**
     * Updates the position of a tab
     */
    suspend fun updateTabPosition(tabId: Long, newPosition: Int) =
        tabDao.updateTabPosition(tabId, newPosition)

    /**
     * Gets a specific tab by ID
     */
    suspend fun getTabById(tabId: Long): TabEntity? = tabDao.getTabById(tabId)

    /**
     * Marks a tab as closed
     */
    suspend fun markTabAsClosed(tabId: Long, closedAt: Date) {
        tabDao.markTabAsClosed(tabId, closedAt)
    }

    /**
     * Mark a tab as synced after successful API operation.
     */
    suspend fun markAsSynced(tab: TabEntity, serverId: String?) {
        val updatedTab = tab.copy(
            syncStatus = SyncStatus.SYNCED,
            serverId = serverId ?: tab.serverId
        )
        tabDao.updateTab(updatedTab)
    }

    /**
     * Delete a tab immediately with server synchronization.
     */
    suspend fun deleteTabImmediate(
        tab: TabEntity,
        isUserSignedIn: Boolean,
        accessToken: String,
        deviceId: String
    ) {
        if (isUserSignedIn && tab.userId.isNotBlank()) {
            try {
                // If we have a server ID, try to delete directly on server
                if (!tab.serverId.isNullOrBlank()) {
                    tabApiService.deleteTab("Bearer $accessToken", tab.serverId)
                    // Success! Remove locally
                    tabDao.deleteTab(tab)
                } else {
                    // No server ID, create a shadow entry and delete the original
                    val shadowTab = tab.copy(
                        id = 0, // Reset ID to avoid primary key conflict
                        url = "PENDING_DELETE:" + tab.url,
                        syncStatus = SyncStatus.PENDING_DELETE
                    )
                    tabDao.insertTab(shadowTab)
                    tabDao.deleteTab(tab)
                }
            } catch (e: Exception) {
                Log.e("TabRepository", "Error deleting tab from server: ${e.message}", e)

                // Create shadow entry for later sync and delete original
                val shadowTab = tab.copy(
                    id = 0,
                    url = "PENDING_DELETE:" + tab.url,
                    syncStatus = SyncStatus.PENDING_DELETE
                )
                tabDao.insertTab(shadowTab)
                tabDao.deleteTab(tab)
            }
        } else {
            // For anonymous users, just delete locally
            tabDao.deleteTab(tab)
        }
    }

    /**
     * Update the TabEntity from a TabDto received from the server.
     */
    suspend fun updateTabFromDto(remote: TabDto, userId: String) {
        val localTab = tabDao.getTabByUrl(remote.url)
        if (localTab != null) {
            // Determine if we should update based on timestamp
            val shouldUpdate = if (remote.timestamp != null && localTab.lastVisited != null) {
                remote.timestamp.after(localTab.lastVisited)
            } else {
                true
            }
            if (shouldUpdate) {
                val currentTime = Date()
                // Only update the title if the remote title is not "Loading..."
                val updatedTitle = if (!remote.title.isNullOrBlank() && remote.title != "Loading...") {
                    remote.title
                } else {
                    localTab.title
                }
                val updatedTab = localTab.copy(
                    title = updatedTitle,
                    lastVisited = remote.timestamp ?: currentTime,
                    serverId = remote.id,
                    syncStatus = SyncStatus.SYNCED
                )
                tabDao.updateTab(updatedTab)
            }
        }
    }

    /**
     * Insert a new tab from a TabDto received from the server.
     */
    suspend fun insertTabFromDto(remote: TabDto, userId: String): Long {
        // Use current timestamp if remote timestamp is null
        val currentTime = Date()

        val newTab = TabEntity(
            url = remote.url,
            title = remote.title ?: "Untitled Tab", // Default title if null
            userId = remote.userId ?: userId,       // Use provided userId if null from server

            // IMPORTANT: Handle null timestamps
            lastVisited = remote.timestamp ?: currentTime,
            createdAt = remote.timestamp ?: currentTime, // This fixes the null parameter error

            serverId = remote.id,
            syncStatus = SyncStatus.SYNCED,

            // Other fields with defaults
            isActive = false,
            position = 0
        )
        return tabDao.insertTab(newTab)
    }

    /**
     * Get all tabs as a List (not a Flow) for sync operations.
     */
    suspend fun getAllTabsAsList(): List<TabEntity> = tabDao.getAllTabsAsList()

    /**
     * Get tabs that are pending upload (need to be sent to server).
     */
    suspend fun getPendingUploads(): List<TabEntity> {
        return getAllTabsAsList().filter { tab ->
            tab.syncStatus == SyncStatus.PENDING_UPLOAD
        }
    }

    /**
     * Get tabs that are pending deletion (need to be removed from server).
     */
    suspend fun getPendingDeletes(): List<TabEntity> {
        return getAllTabsAsList().filter { tab ->
            tab.syncStatus == SyncStatus.PENDING_DELETE ||
                    tab.url.startsWith("PENDING_DELETE:")
        }
    }
}
/**
 * Converts a TabEntity to a TabDto for API communication.
 */
fun TabEntity.toDto(userId: String, deviceId: String): TabDto {
    return TabDto(
        id = this.serverId,
        userId = userId,
        url = this.url,
        title = this.title,
        scrollPosition = 0,
        timestamp = this.lastVisited,
        device = deviceId
    )
}