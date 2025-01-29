package com.example.mobilebrowser.data.repository

import com.example.mobilebrowser.data.dao.TabDao
import com.example.mobilebrowser.data.entity.TabEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class that abstracts the data operations for tabs.
 * Provides a clean API for the rest of the application to interact with the data layer.
 */
@Singleton
class TabRepository @Inject constructor(
    private val tabDao: TabDao
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
}