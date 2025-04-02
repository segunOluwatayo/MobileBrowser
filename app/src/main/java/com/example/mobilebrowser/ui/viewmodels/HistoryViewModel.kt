package com.example.mobilebrowser.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.data.entity.HistoryEntity
import com.example.mobilebrowser.data.repository.HistoryRepository
import com.example.mobilebrowser.data.util.UserDataStore
import com.example.mobilebrowser.sync.SyncStatusState
import com.example.mobilebrowser.sync.UserSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for handling browsing history data and operations.
 * Improved to properly handle user authentication and synchronization.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository,
    private val userSyncManager: UserSyncManager,
    private val userDataStore: UserDataStore // Add UserDataStore dependency
) : ViewModel() {

    // Expose local history data to the UI
    val allHistory = repository.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Expose the sync status so the UI can show sync progress or errors
    private val _syncStatus = MutableStateFlow<SyncStatusState>(SyncStatusState.Idle)
    val syncStatus: StateFlow<SyncStatusState> = _syncStatus.asStateFlow()

    // Last sync timestamp
    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Today's start time
    private val todayStart: Date = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    // Current time
    private val now = Date()

    // Last week's start time (excluding today)
    private val lastWeekStart: Date = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -7)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    // All history entries for combined display and debugging
    private val allHistoryEntries = repository.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Today's history entries
    val todayHistory = allHistoryEntries
        .map { entries ->
            entries.filter { entry ->
                entry.lastVisited.time >= todayStart.time
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Last week's history (excluding today)
    val lastWeekHistory = allHistoryEntries
        .map { entries ->
            entries.filter { entry ->
                entry.lastVisited.time < todayStart.time && entry.lastVisited.time >= lastWeekStart.time
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Search results
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults = _searchQuery
        .debounce(300L) // Debounce search input
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchHistory(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Recent history entries
    val recentHistory = repository.getRecentHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Update search query
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Adds a history entry with proper user association.
     * FIXED: Now gets the current user ID from UserDataStore
     *
     * @param url The URL of the visited page
     * @param title The title of the visited page
     * @param favicon Optional favicon URL
     */
    fun addHistoryEntry(url: String, title: String, favicon: String? = null) {
        viewModelScope.launch {
            try {
                // Get the current user ID from UserDataStore
                val userId = userDataStore.userId.first()

                // Only proceed with valid user ID if signed in
                val isSignedIn = userDataStore.isSignedIn.first()
                val effectiveUserId = if (isSignedIn && userId.isNotBlank()) userId else ""

                // Add history entry with proper user ID
                repository.addHistoryEntry(url, title, favicon, effectiveUserId)
            } catch (e: Exception) {
                _error.value = "Failed to add history entry: ${e.message}"
            }
        }
    }

    /**
     * Manually trigger a sync operation to push local changes to the server.
     */
    fun triggerManualSync() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatusState.Syncing
            try {
                // Get authentication data
                val isSignedIn = userDataStore.isSignedIn.first()
                if (!isSignedIn) {
                    _error.value = "Cannot sync: User not signed in"
                    _syncStatus.value = SyncStatusState.Error("User not signed in")
                    return@launch
                }

                val accessToken = userDataStore.accessToken.first()
                val deviceId = userDataStore.deviceId.first().ifEmpty { "android-device" }
                val userId = userDataStore.userId.first()

                if (accessToken.isBlank() || userId.isBlank()) {
                    _error.value = "Cannot sync: Missing authentication data"
                    _syncStatus.value = SyncStatusState.Error("Missing authentication data")
                    return@launch
                }

                // Perform sync operation
                userSyncManager.pushLocalChanges(accessToken, deviceId, userId)

                // Update sync status and timestamp
                _syncStatus.value = SyncStatusState.Synced
                _lastSyncTimestamp.value = System.currentTimeMillis()
            } catch (e: Exception) {
                _syncStatus.value = SyncStatusState.Error(e.message ?: "Unknown error during sync")
                _error.value = "Sync failed: ${e.message}"
            }
        }
    }

    // Delete a specific history entry
    fun deleteHistoryEntry(history: HistoryEntity) {
        viewModelScope.launch {
            try {
                repository.deleteHistoryEntry(history)
            } catch (e: Exception) {
                _error.value = "Failed to delete history entry: ${e.message}"
            }
        }
    }


    // Delete history by a specified time range
    fun deleteHistoryByTimeRange(timeRange: HistoryTimeRange) {
        viewModelScope.launch {
            try {
                when (timeRange) {
                    HistoryTimeRange.LAST_HOUR -> repository.deleteLastHourHistory()
                    HistoryTimeRange.TODAY -> repository.deleteTodayHistory()
                    HistoryTimeRange.YESTERDAY -> repository.deleteYesterdayHistory()
                    HistoryTimeRange.ALL -> repository.deleteAllHistory()
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete history: ${e.message}"
            }
        }
    }

    // Clear any error messages
    fun clearError() {
        _error.value = null
    }

    // Expose all history entries for debugging purposes
    fun getAllHistoryForDebug(): Flow<List<HistoryEntity>> {
        return repository.getAllHistory()
    }
}

// Enum for history deletion time ranges
enum class HistoryTimeRange {
    LAST_HOUR,
    TODAY,
    YESTERDAY,
    ALL
}