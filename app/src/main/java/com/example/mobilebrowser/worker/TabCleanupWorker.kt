package com.example.mobilebrowser.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mobilebrowser.data.database.BrowserDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class TabCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: BrowserDatabase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Retrieve the policy from input data.
        // The policy should be passed in when scheduling the work.
        val policy = inputData.getString("TAB_POLICY") ?: "MANUAL"

        // Determine the delay in milliseconds based on the policy.
        val delayMillis = when (policy) {
            "ONE_DAY" -> 24 * 60 * 60 * 1000L
            "ONE_WEEK" -> 7 * 24 * 60 * 60 * 1000L
            "ONE_MONTH" -> 30 * 24 * 60 * 60 * 1000L
            else -> 0L
        }

        if (delayMillis > 0L) {
            // Compute the threshold date: current time minus the delay.
            val thresholdCalendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis() - delayMillis
            }
            val thresholdDate = thresholdCalendar.time

            // Use the TabDao from the database to delete closed tabs older than threshold.
            database.tabDao().deleteClosedTabsOlderThan(thresholdDate)
        }

        return Result.success()
    }
}
