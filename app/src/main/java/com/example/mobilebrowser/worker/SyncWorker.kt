package com.example.mobilebrowser.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.mobilebrowser.data.util.UserDataStore
import com.example.mobilebrowser.sync.UserSyncManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

/**
 * Worker that performs background synchronization on a fixed schedule.
 * Syncs data between local device and server every 3 minutes when user is signed in.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val userDataStore: UserDataStore,
    private val userSyncManager: UserSyncManager
) : CoroutineWorker(context, workerParams) {

    private val TAG = "SyncWorker"

    override suspend fun doWork(): Result {
        try {
            // Check if user is signed in
            val isSignedIn = userDataStore.isSignedIn.first()
            if (!isSignedIn) {
                Log.d(TAG, "User not signed in, skipping background sync")
                return Result.success()
            }

            // Get authentication data
            val accessToken = userDataStore.accessToken.first()
            val deviceId = userDataStore.deviceId.first().ifEmpty { "android-device" }
            val userId = userDataStore.userId.first()

            if (accessToken.isBlank() || userId.isBlank()) {
                Log.d(TAG, "Missing authentication data, skipping background sync")
                return Result.success()
            }

            Log.d(TAG, "Starting automatic background sync")

            // Perform sync operations - push local changes to server
            userSyncManager.performSync(accessToken, deviceId, userId)

            // Only push local changes in background sync to avoid conflicts
            // with any active editing the user might be doing

            Log.d(TAG, "Background sync completed successfully")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background sync failed: ${e.message}", e)
            // Retry on failure
            return Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "background_sync_worker"

        /**
         * Schedule the background sync to run every 3 minutes
         * @param context Application context
         */
        fun schedule(context: Context) {
            Log.d(TAG, "Scheduling background sync worker (every 3 minutes)")

            // Create constraints - only run when network is available
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Create a periodic work request that runs every 3 minutes
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                3, TimeUnit.MINUTES, // Run every 3 minutes
                30, TimeUnit.SECONDS  // With 30 seconds flex time for battery optimization
            )
                .setConstraints(constraints)
                .build()

            // Enqueue the work request, replacing any existing one with the same name
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP, // Keep existing if any
                    syncRequest
                )
        }

        /**
         * Cancel the background sync worker
         * @param context Application context
         */
        fun cancel(context: Context) {
            Log.d(TAG, "Canceling background sync worker")
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}