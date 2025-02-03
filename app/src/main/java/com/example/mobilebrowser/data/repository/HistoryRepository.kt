package com.example.mobilebrowser.data.repository

import com.example.mobilebrowser.data.dao.HistoryDao
import com.example.mobilebrowser.data.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    // Get all history entries
    fun getAllHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    // Search history entries
    fun searchHistory(query: String): Flow<List<HistoryEntity>> =
        historyDao.searchHistory("%$query%")

    // Add or update history entry
    suspend fun addHistoryEntry(url: String, title: String, favicon: String? = null) {
        val existingEntry = historyDao.getHistoryByUrl(url)

        if (existingEntry != null) {
            // Update existing entry
            historyDao.incrementVisitCount(url)
        } else {
            // Create new entry
            val historyEntry = HistoryEntity(
                url = url,
                title = title,
                favicon = favicon
            )
            historyDao.insertHistory(historyEntry)
        }
    }

    // Delete specific history entry
    suspend fun deleteHistoryEntry(history: HistoryEntity) =
        historyDao.deleteHistory(history)

    // Delete all history
    suspend fun deleteAllHistory() = historyDao.deleteAllHistory()

    // Delete history from last hour
    suspend fun deleteLastHourHistory() {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(Calendar.HOUR, -1)
        val startDate = calendar.time
        historyDao.deleteHistoryInRange(startDate, endDate)
    }

    // Delete today's history
    suspend fun deleteTodayHistory() {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        historyDao.deleteHistoryInRange(startDate, endDate)
    }

    // Delete yesterday's history
    suspend fun deleteYesterdayHistory() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endDate = calendar.time
        historyDao.deleteHistoryInRange(startDate, endDate)
    }

    // Get history for specific time range
    fun getHistoryInRange(startDate: Date, endDate: Date): Flow<List<HistoryEntity>> =
        historyDao.getHistoryInRange(startDate, endDate)

    // Check if URL exists in history
    suspend fun isUrlInHistory(url: String): Boolean =
        historyDao.isUrlInHistory(url)
}