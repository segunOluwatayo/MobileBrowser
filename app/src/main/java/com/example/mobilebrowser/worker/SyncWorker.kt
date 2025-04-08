//package com.example.mobilebrowser.worker
//
//import android.content.Context
//import android.util.Log
//import androidx.hilt.work.HiltWorker
//import androidx.work.*
//import com.example.mobilebrowser.data.util.UserDataStore
//import com.example.mobilebrowser.sync.UserSyncManager
//import dagger.assisted.Assisted
//import dagger.assisted.AssistedInject
//import kotlinx.coroutines.flow.first
//import java.util.concurrent.TimeUnit
//import kotlinx.coroutines.delay
//
///**
// * Worker that performs background synchronization on a fixed schedule.
// * Syncs data between local device and server every 3 minutes when user is signed in.
// */
//@HiltWorker
//class SyncWorker @AssistedInject constructor(
//    @Assisted context: Context,
//    @Assisted workerParams: WorkerParameters,
//    private val userDataStore: UserDataStore,
//    private val userSyncManager: UserSyncManager
//) : CoroutineWorker(context, workerParams) {
//
//    private val TAG = "SyncWorker"
//
//    override suspend fun doWork(): Result {
//        try {
//            Log.d(TAG, "SyncWorker doWork() started at ${System.currentTimeMillis()}")
//
//            // Check if user is signed in
//            val isSignedIn = userDataStore.isSignedIn.first()
//            if (!isSignedIn) {
//                Log.d(TAG, "User not signed in, skipping background sync")
//                return Result.success()
//            }
//
//            // Log each step of the process
//            Log.d(TAG, "Fetching auth credentials...")
//            val accessToken = userDataStore.accessToken.first()
//            val deviceId = userDataStore.deviceId.first().ifEmpty { "android-device" }
//            val userId = userDataStore.userId.first()
//
//            if (accessToken.isBlank() || userId.isBlank()) {
//                Log.d(TAG, "Missing authentication data, skipping background sync")
//                return Result.success()
//            }
//
//            Log.d(TAG, "Starting automatic background sync with valid credentials")
//
//            // Perform sync operations
//            userSyncManager.performSync(accessToken, deviceId, userId)
//
//            Log.d(TAG, "Background sync completed successfully at ${System.currentTimeMillis()}")
//            return Result.success()
//        } catch (e: Exception) {
//            Log.e(TAG, "Background sync failed with exception: ${e.message}", e)
//            // Retry on failure
//            return Result.retry()
//        }
//    }
//
//    companion object {
//        private const val TAG = "SyncWorker"
//        private const val WORK_NAME = "background_sync_worker"
//
//        /**
//         * Schedule the background sync to run every 3 minutes
//         * @param context Application context
//         */
//        fun schedule(context: Context) {
//            Log.d(TAG, "Scheduling background sync worker (every 3 minutes)")
//
//            // Create constraints - only run when network is available
//            val constraints = Constraints.Builder()
//                .setRequiredNetworkType(NetworkType.CONNECTED)
//                .build()
//
//            // Create a periodic work request that runs every 3 minutes
//            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
//                3, TimeUnit.MINUTES,
//                30, TimeUnit.SECONDS  // With 30 seconds flex time for battery optimization
//            )
//                .setConstraints(constraints)
//                // Add an initial delay to avoid immediate execution conflicts
//                .setInitialDelay(10, TimeUnit.SECONDS)
//                .addTag("sync_worker")  // Add a tag for easier debugging
//                .build()
//
//            // Use REPLACE instead of KEEP to ensure the latest configuration is used
//            WorkManager.getInstance(context)
//                .enqueueUniquePeriodicWork(
//                    WORK_NAME,
//                    ExistingPeriodicWorkPolicy.REPLACE,  // Changed from KEEP to REPLACE
//                    syncRequest
//                )
//
//            // Also schedule an immediate one-time sync to verify functionality
//            val immediateSync = OneTimeWorkRequestBuilder<SyncWorker>()
//                .setConstraints(constraints)
//                .addTag("immediate_sync")
//                .build()
//
//            WorkManager.getInstance(context).enqueue(immediateSync)
//
//            Log.d(TAG, "Background sync worker scheduled with REPLACE policy and immediate sync queued")
//        }
//
//        /**
//         * Cancel the background sync worker
//         * @param context Application context
//         */
//        fun cancel(context: Context) {
//            Log.d(TAG, "Canceling background sync worker")
//            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
//        }
//    }
//    /**
//     * Schedule an immediate one-time sync
//     * @param context Application context
//     */
//    fun scheduleImmediate(context: Context) {
//        Log.d(TAG, "Scheduling immediate one-time sync")
//
//        // Create constraints - only run when network is available
//        val constraints = Constraints.Builder()
//            .setRequiredNetworkType(NetworkType.CONNECTED)
//            .build()
//
//        // Create a one-time work request
//        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
//            .setConstraints(constraints)
//            .addTag("immediate_sync")
//            .build()
//
//        // Enqueue the work request
//        WorkManager.getInstance(context).enqueue(syncRequest)
//
//        Log.d(TAG, "Immediate sync scheduled")
//    }
//}
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
            Log.d(TAG, "SyncWorker doWork() started at ${System.currentTimeMillis()}")

            // Check if user is signed in
            val isSignedIn = userDataStore.isSignedIn.first()
            if (!isSignedIn) {
                Log.d(TAG, "User not signed in, skipping background sync")
                return Result.success()
            }

            // Log each step of the process
            Log.d(TAG, "Fetching auth credentials...")
            val accessToken = userDataStore.accessToken.first()
            val deviceId = userDataStore.deviceId.first().ifEmpty { "android-device" }
            val userId = userDataStore.userId.first()

            if (accessToken.isBlank() || userId.isBlank()) {
                Log.d(TAG, "Missing authentication data, skipping background sync")
                return Result.success()
            }

            Log.d(TAG, "Starting automatic background sync with valid credentials")

            // Perform sync operations
            userSyncManager.performSync(accessToken, deviceId, userId)

            // Successfully completed sync - update the timestamp in DataStore
            val currentTime = System.currentTimeMillis()
            userDataStore.updateLastSyncTimestamp(currentTime)

            Log.d(TAG, "Background sync completed successfully at $currentTime, timestamp saved in DataStore")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background sync failed with exception: ${e.message}", e)
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
                3, TimeUnit.MINUTES,
                30, TimeUnit.SECONDS  // With 30 seconds flex time for battery optimization
            )
                .setConstraints(constraints)
                // Add an initial delay to avoid immediate execution conflicts
                .setInitialDelay(10, TimeUnit.SECONDS)
                .addTag("sync_worker")  // Add a tag for easier debugging
                .build()

            // Use REPLACE instead of KEEP to ensure the latest configuration is used
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,  // Changed from KEEP to REPLACE
                    syncRequest
                )

            // Also schedule an immediate one-time sync to verify functionality
            val immediateSync = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .addTag("immediate_sync")
                .build()

            WorkManager.getInstance(context).enqueue(immediateSync)

            Log.d(TAG, "Background sync worker scheduled with REPLACE policy and immediate sync queued")
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
    /**
     * Schedule an immediate one-time sync
     * @param context Application context
     */
    fun scheduleImmediate(context: Context) {
        Log.d(TAG, "Scheduling immediate one-time sync")

        // Create constraints - only run when network is available
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create a one-time work request
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag("immediate_sync")
            .build()

        // Enqueue the work request
        WorkManager.getInstance(context).enqueue(syncRequest)

        Log.d(TAG, "Immediate sync scheduled")
    }
}