package com.example.mobilebrowser.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mobilebrowser.data.service.AuthService
import com.example.mobilebrowser.data.util.UserDataStore
import com.example.mobilebrowser.sync.InitialSyncManager
import com.example.mobilebrowser.sync.SyncStatusState
import com.example.mobilebrowser.sync.UserSyncManager
import com.example.mobilebrowser.worker.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService,
    private val userDataStore: UserDataStore,
    private val userSyncManager: UserSyncManager,
    private val initialSyncManager: InitialSyncManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Expose authentication state from datastore.
    val isSignedIn: StateFlow<Boolean> = userDataStore.isSignedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Expose user display name.
    val userName: StateFlow<String> = userDataStore.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Expose user email.
    val userEmail: StateFlow<String> = userDataStore.userEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Expose sync preferences
    val syncHistoryEnabled: StateFlow<Boolean> = userDataStore.syncHistoryEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserDataStore.DEFAULT_SYNC_HISTORY_ENABLED)

    val syncBookmarksEnabled: StateFlow<Boolean> = userDataStore.syncBookmarksEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserDataStore.DEFAULT_SYNC_BOOKMARKS_ENABLED)

    val syncTabsEnabled: StateFlow<Boolean> = userDataStore.syncTabsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserDataStore.DEFAULT_SYNC_TABS_ENABLED)


    // Use the timestamp from DataStore instead of internal state
    val lastSyncTimestamp: StateFlow<Long?> = userDataStore.lastSyncTimestamp
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Sync status state flow to provide UI feedback
    private val _syncStatus = MutableStateFlow<SyncStatusState>(SyncStatusState.Idle)
    val syncStatus: StateFlow<SyncStatusState> = _syncStatus

    // Sync preferences
//    val syncHistoryEnabled = userDataStore.syncHistoryEnabled
//    val syncBookmarksEnabled = userDataStore.syncBookmarksEnabled
//    val syncTabsEnabled = userDataStore.syncTabsEnabled

    // Sign out the current user
    suspend fun signOut() {
        authService.signOut()
    }

    // Get access token for authenticated requests
    fun getAccessToken(callback: (String) -> Unit) {
        viewModelScope.launch {
            val token = userDataStore.accessToken.first()
            callback(token)
        }
    }

    // Initiate a manual sync operation
    fun performManualSync() {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Starting manual sync")
                _syncStatus.value = SyncStatusState.Syncing

                // Get auth data
                val accessToken = userDataStore.accessToken.first()
                val deviceId = userDataStore.deviceId.first().ifEmpty { "android-device" }
                val userId = userDataStore.userId.first()

                if (accessToken.isBlank() || userId.isBlank()) {
                    _syncStatus.value = SyncStatusState.Error("Missing authentication data")
                    return@launch
                }

                // Perform sync
                userSyncManager.performSync(accessToken, deviceId, userId)

                // Update timestamp after successful sync
                val currentTime = System.currentTimeMillis()
                userDataStore.updateLastSyncTimestamp(currentTime)

                _syncStatus.value = SyncStatusState.Synced
                Log.d("AuthViewModel", "Manual sync completed successfully at $currentTime")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error during manual sync: ${e.message}", e)
                _syncStatus.value = SyncStatusState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Perform initial sync after login
    fun performInitialSync() {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Starting initial sync")
                _syncStatus.value = SyncStatusState.Syncing

                val syncHistory = userDataStore.syncHistoryEnabled.first()
                val syncBookmarks = userDataStore.syncBookmarksEnabled.first()
                val syncTabs = userDataStore.syncTabsEnabled.first()

                initialSyncManager.performInitialSync(
                    syncHistory = syncHistory,
                    syncBookmarks = syncBookmarks,
                    syncTabs = syncTabs
                )

                // Update timestamp after successful sync
                val currentTime = System.currentTimeMillis()
                userDataStore.updateLastSyncTimestamp(currentTime)

                _syncStatus.value = SyncStatusState.Synced
                Log.d("AuthViewModel", "Initial sync completed successfully at $currentTime")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error during initial sync: ${e.message}", e)
                _syncStatus.value = SyncStatusState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Schedule immediate sync via WorkManager
    fun triggerImmediateSyncViaWorker() {
        Log.d("AuthViewModel", "Triggering immediate sync via WorkManager")
        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .addTag("manual_trigger_sync")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    // Update sync preferences
    fun updateSyncHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userDataStore.updateSyncHistoryEnabled(enabled)
        }
    }

    fun updateSyncBookmarksEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userDataStore.updateSyncBookmarksEnabled(enabled)
        }
    }

    fun updateSyncTabsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userDataStore.updateSyncTabsEnabled(enabled)
        }
    }

    fun updateSyncStatus(newStatus: SyncStatusState) {
        _syncStatus.value = newStatus
    }
}