package com.example.mobilebrowser.sync

import com.example.mobilebrowser.data.util.UserDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * InitialSyncManager orchestrates the first-time sync operation immediately after a successful login.
 * It retrieves the access token, device ID, and user ID from the UserDataStore and triggers
 * the UserSyncManager to perform the initial data fetch and merge.
 */
@Singleton
class InitialSyncManager @Inject constructor(
    private val userSyncManager: UserSyncManager,
    private val userDataStore: UserDataStore
) {
    /**
     * Performs the initial sync operation.
     */
    suspend fun performInitialSync() {
        val token = userDataStore.accessToken.first()
        val deviceId = userDataStore.deviceId.first()
        val userId = userDataStore.userId.first() // Using the userId flow from UserDataStore
        userSyncManager.initialSync(token, deviceId, userId)
    }
}
