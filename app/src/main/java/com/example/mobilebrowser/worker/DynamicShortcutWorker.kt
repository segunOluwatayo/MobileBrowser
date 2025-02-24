package com.example.mobilebrowser.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.mobilebrowser.ui.viewmodels.ShortcutViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class DynamicShortcutWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val shortcutViewModel: ShortcutViewModel
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            shortcutViewModel.updateDynamicShortcuts()
            Result.success()
        } catch (e: Exception) {
            Log.e("DynamicShortcutWorker", "Error updating shortcuts", e)
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)

            // Define constraints - run when device is idle and has network
            val constraints = Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Create a periodic work request
            val workRequest = PeriodicWorkRequestBuilder<DynamicShortcutWorker>(
                repeatInterval = 6,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            // Enqueue the work
            workManager.enqueueUniquePeriodicWork(
                "dynamic_shortcut_update",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}