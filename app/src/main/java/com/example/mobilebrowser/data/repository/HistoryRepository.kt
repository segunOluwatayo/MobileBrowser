package com.example.mobilebrowser.data.repository

import com.example.mobilebrowser.data.dao.HistoryDao
import com.example.mobilebrowser.data.dto.HistoryDto
import com.example.mobilebrowser.data.entity.HistoryEntity
import com.example.mobilebrowser.data.entity.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    // Retrieve all history entries from the local database.
    fun getAllHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    // Search history entries by a query string.
    fun searchHistory(query: String): Flow<List<HistoryEntity>> =
        historyDao.searchHistory("%$query%")

    // Retrieve history entries within a specific date range.
    fun getHistoryInRange(startDate: Date, endDate: Date): Flow<List<HistoryEntity>> =
        historyDao.getHistoryInRange(startDate, endDate)

    // Retrieve today's history by calculating the start and end timestamps.
    fun getTodayHistory(): Flow<List<HistoryEntity>> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        return historyDao.getHistoryInRange(startDate, endDate)
    }

    // Retrieve history for the last 7 days (excluding today).
    fun getLastWeekHistory(): Flow<List<HistoryEntity>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.time

        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val weekStart = calendar.time

        return historyDao.getHistoryInRange(weekStart, todayStart)
    }

    /**
     * Returns a Flow of history entries that are marked as PENDING_UPLOAD,
     * indicating they need to be sent to the remote server.
     */
    fun getPendingUploads(): Flow<List<HistoryEntity>> =
        historyDao.getHistoryBySyncStatus(SyncStatus.PENDING_UPLOAD)

    /**
     * Returns a Flow of history entries that are marked as PENDING_DELETE,
     * indicating they need to be deleted from the remote server.
     */
    fun getPendingDeletes(): Flow<List<HistoryEntity>> =
        historyDao.getHistoryBySyncStatus(SyncStatus.PENDING_DELETE)

    /**
     * Retrieves a single history entry by its URL.
     * Returns null if no entry is found.
     */
    suspend fun getHistoryByUrl(url: String): HistoryEntity? = historyDao.getHistoryByUrl(url)

    /**
     * Adds a new history entry or updates an existing one.
     * New or updated entries are marked as PENDING_UPLOAD to signal
     * that they need to be synchronized with the backend.
     *
     * @param url The URL of the visited page.
     * @param title The title of the visited page.
     * @param favicon Optional favicon URL.
     * @param userId The identifier of the currently logged in user.
     */
    suspend fun addHistoryEntry(url: String, title: String, favicon: String? = null, userId: String) {
        // Check if the URL already exists in the local database.
        val existingEntry = historyDao.getHistoryByUrl(url)

        if (existingEntry != null) {
            // Update the existing entry:
            // Increment visit count, update timestamps, and mark as pending upload.
            val updatedEntry = existingEntry.copy(
                visitCount = existingEntry.visitCount + 1,
                lastVisited = Date(),
                lastModified = Date(),
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
            historyDao.updateHistory(updatedEntry)
        } else {
            // Create a new entry with the sync status set to PENDING_UPLOAD.
            val newEntry = HistoryEntity(
                userId = userId,
                url = url,
                title = title,
                favicon = favicon,
                firstVisited = Date(),
                lastVisited = Date(),
                visitCount = 1,
                lastModified = Date(),
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
            historyDao.insertHistory(newEntry)
        }
    }

    /**
     * Deletes a history entry properly based on user sign-in status.
     * If user is signed in, the entry is marked for server deletion before local removal.
     * If not signed in, the entry is just deleted locally.
     *
     * @param history The history entry to be deleted
     * @param isUserSignedIn Whether the user is currently signed in
     */
    suspend fun deleteHistoryEntry(history: HistoryEntity, isUserSignedIn: Boolean) {
        if (isUserSignedIn && history.userId.isNotBlank()) {
            // For signed-in users with a valid userId, create a copy marked for deletion
            // This entry will be synced to the server and then removed locally
            val entryToDelete = history.copy(
                syncStatus = SyncStatus.PENDING_DELETE,
                lastModified = Date()
            )
            historyDao.updateHistory(entryToDelete)
        }

        // Delete the entry from local view immediately for both signed-in and anonymous users
        historyDao.deleteHistory(history)
    }

    /**
     * Deletes history entries within a specific date range.
     * When signed in, entries are first marked for server deletion.
     *
     * @param startDate Beginning of the time range
     * @param endDate End of the time range
     * @param isUserSignedIn Whether the user is currently signed in
     */
    suspend fun deleteHistoryInRange(startDate: Date, endDate: Date, isUserSignedIn: Boolean) {
        if (isUserSignedIn) {
            // For signed-in users, first get entries and mark them for server deletion
            val entriesToDelete = historyDao.getHistoryInRangeAsList(startDate, endDate)
            for (entry in entriesToDelete) {
                // Only mark entries with a valid userId
                if (entry.userId.isNotBlank()) {
                    val markedEntry = entry.copy(
                        syncStatus = SyncStatus.PENDING_DELETE,
                        lastModified = Date()
                    )
                    historyDao.updateHistory(markedEntry)
                }
            }
        }

        // Delete from local database regardless of sign-in status
        historyDao.deleteHistoryInRange(startDate, endDate)
    }

    /**
     * Deletes all history entries.
     * When signed in, entries are first marked for server deletion.
     *
     * @param isUserSignedIn Whether the user is currently signed in
     */
    suspend fun deleteAllHistory(isUserSignedIn: Boolean) {
        if (isUserSignedIn) {
            // For signed-in users, first get all entries and mark them for server deletion
            val allEntries = historyDao.getAllHistoryAsList()
            for (entry in allEntries) {
                // Only mark entries with a valid userId
                if (entry.userId.isNotBlank()) {
                    val markedEntry = entry.copy(
                        syncStatus = SyncStatus.PENDING_DELETE,
                        lastModified = Date()
                    )
                    historyDao.updateHistory(markedEntry)
                }
            }
        }

        // Delete all entries from local database regardless of sign-in status
        historyDao.deleteAllHistory()
    }

    /**
     * Get all history as a List for processing (not as a Flow)
     */
    suspend fun getAllHistoryAsList(): List<HistoryEntity> {
        return historyDao.getAllHistoryAsList()
    }

    /**
     * Deletes history entries from the last hour.
     */
    suspend fun deleteLastHourHistory(isUserSignedIn: Boolean) {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(Calendar.HOUR, -1)
        val startDate = calendar.time
        deleteHistoryInRange(startDate, endDate, isUserSignedIn)
    }

    /**
     * Deletes history entries for today.
     */
    suspend fun deleteTodayHistory(isUserSignedIn: Boolean) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        val endDate = Date()
        deleteHistoryInRange(startDate, endDate, isUserSignedIn)
    }

    /**
     * Deletes history entries for yesterday.
     */
    suspend fun deleteYesterdayHistory(isUserSignedIn: Boolean) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endDate = calendar.time
        deleteHistoryInRange(startDate, endDate, isUserSignedIn)
    }

    /**
     * Retrieves the most recent history entries, limited by the provided count.
     */
    fun getRecentHistory(limit: Int = 10): Flow<List<HistoryEntity>> =
        historyDao.getRecentHistory(limit)

    /**
     * Once the server confirms deletion, remove the entry from the local database.
     *
     * @param history The history entry to be permanently removed.
     */
    suspend fun finalizeDeletion(history: HistoryEntity) {
        historyDao.deleteHistory(history)
    }

    /**
     * Update an existing local history entry with data from the remote DTO.
     * Uses remote timestamp for conflict resolution (last-write-wins).
     *
     * @param remote The HistoryDto received from the server.
     */
    suspend fun updateHistoryFromDto(remote: HistoryDto) {
        val localEntry = historyDao.getHistoryByUrl(remote.url)
        if (localEntry != null) {
            // Update only if the remote data is newer.
            if (remote.timestamp.after(localEntry.lastModified)) {
                val updatedEntry = localEntry.copy(
                    title = remote.title,
                    // Leave favicon unchanged if remote doesn't provide one.
                    lastVisited = remote.timestamp,
                    lastModified = remote.timestamp,
                    serverId = remote.id,
                    syncStatus = SyncStatus.SYNCED
                )
                historyDao.updateHistory(updatedEntry)
            }
        }
    }

    /**
     * Inserts a new history entry based on data from the remote DTO.
     *
     * @param remote The HistoryDto received from the server.
     */
    suspend fun insertHistoryFromDto(remote: HistoryDto) {
        val newEntry = HistoryEntity(
            userId = remote.userId,
            url = remote.url,
            title = remote.title,
            favicon = null,  // Favicon not provided by backend.
            firstVisited = remote.timestamp,
            lastVisited = remote.timestamp,
            visitCount = 1,
            lastModified = remote.timestamp,
            serverId = remote.id,
            syncStatus = SyncStatus.SYNCED
        )
        historyDao.insertHistory(newEntry)
    }

    /**
     * Marks a local history entry as synchronized.
     * Updates syncStatus to SYNCED and stores the server-assigned ID if provided.
     *
     * @param localEntry The local history entry to update.
     * @param updatedServerId The server ID returned from the backend (if any).
     */
    suspend fun markAsSynced(localEntry: HistoryEntity, updatedServerId: String?) {
        val updatedEntry = localEntry.copy(
            syncStatus = SyncStatus.SYNCED,
            serverId = updatedServerId ?: localEntry.serverId,
            lastModified = Date()
        )
        historyDao.updateHistory(updatedEntry)
    }
}

/**
 * Extension function to convert a HistoryEntity to a HistoryDto.
 * Adapts the local model to the backend schema by including the device identifier.
 *
 * @param deviceId The device identification to be sent with the API request.
 * @return A HistoryDto instance.
 */
fun HistoryEntity.toDto(deviceId: String): HistoryDto {
    return HistoryDto(
        id = this.serverId,
        userId = this.userId,
        url = this.url,
        title = this.title,
        timestamp = this.lastModified,
        device = deviceId
    )
}