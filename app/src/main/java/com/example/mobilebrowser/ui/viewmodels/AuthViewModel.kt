package com.example.mobilebrowser.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.data.service.AuthService
import com.example.mobilebrowser.data.util.UserDataStore
import com.example.mobilebrowser.sync.InitialSyncManager
import com.example.mobilebrowser.sync.SyncStatusState
import com.example.mobilebrowser.sync.UserSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService,
    private val userDataStore: UserDataStore,
    private val userSyncManager: UserSyncManager,
    private val initialSyncManager: InitialSyncManager
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

    // Expose sync status.
    private val _syncStatus = MutableStateFlow<SyncStatusState>(SyncStatusState.Idle)
    val syncStatus: StateFlow<SyncStatusState> = _syncStatus.asStateFlow()

    // Expose last sync timestamp (epoch millis).
    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    // Error message state.
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Loading state.
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Updates the history sync preference.
     */
    fun updateSyncHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userDataStore.updateSyncHistoryEnabled(enabled)
        }
    }

    /**
     * Updates the bookmarks sync preference.
     */
    fun updateSyncBookmarksEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userDataStore.updateSyncBookmarksEnabled(enabled)
        }
    }

    /**
     * Updates the tabs sync preference.
     */
    fun updateSyncTabsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userDataStore.updateSyncTabsEnabled(enabled)
        }
    }

    /**
     * Initiates an initial sync operation after a successful login.
     * It now uses InitialSyncManager to perform the sync and respects sync preferences.
     */
    fun performInitialSync() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatusState.Syncing
            try {
                // Collect sync preferences
                val syncHistory = userDataStore.syncHistoryEnabled.first()
                val syncBookmarks = userDataStore.syncBookmarksEnabled.first()
                val syncTabs = userDataStore.syncTabsEnabled.first()

                // Use InitialSyncManager to perform first-time sync with preferences
                initialSyncManager.performInitialSync(
                    syncHistory = syncHistory,
                    syncBookmarks = syncBookmarks,
                    syncTabs = syncTabs
                )

                _syncStatus.value = SyncStatusState.Synced
                _lastSyncTimestamp.value = System.currentTimeMillis()
            } catch (e: Exception) {
                _syncStatus.value = SyncStatusState.Error(e.message ?: "Unknown error during sync")
            }
        }
    }

    /**
     * Initiates the sign in process.
     */
    fun signIn() {
        authService.openLoginPage()
    }

    /**
     * Signs out the current user.
     */
    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                userDataStore.clearUserAuthData()
                authService.signOut()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to sign out: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Retrieves an access token and calls the provided callback.
     */
    fun getAccessToken(callback: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = userDataStore.accessToken.first()
                if (token.isNotBlank()) {
                    callback(token)
                } else {
                    _errorMessage.value = "Authentication token not available"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error retrieving authentication token: ${e.message}"
            }
        }
    }

    /**
     * Processes authentication data received from the web authentication flow.
     * After saving the authentication data, it triggers the first-time sync.
     */
    fun processAuthResult(
        accessToken: String?,
        refreshToken: String?,
        userId: String?,
        displayName: String?,
        email: String?,
        deviceId: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (accessToken != null && refreshToken != null && (userId != null || email != null)) {
                    userDataStore.saveUserAuthData(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        userId = userId ?: "",
                        displayName = displayName ?: email ?: "User",
                        email = email ?: "",
                        deviceId = deviceId
                    )
                    // After saving the auth data, trigger the initial sync.
                    performInitialSync()
                } else {
                    _errorMessage.value = "Incomplete authentication data received"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to process authentication: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}