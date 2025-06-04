package com.example.mobilebrowser.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mobilebrowser.data.database.BrowserDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date

@HiltWorker
class TabAutoCloseWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: BrowserDatabase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Get the tab management policy from the input data.
        val policy = inputData.getString("TAB_POLICY") ?: "MANUAL"

        // If policy is MANUAL or unrecognized, do nothing.
        if (policy == "MANUAL") return Result.success()

        // Calculate the delay threshold based on the policy.
        val delayMillis = when (policy) {
            "ONE_DAY" -> 24 * 60 * 60 * 1000L
            "ONE_WEEK" -> 7 * 24 * 60 * 60 * 1000L
            "ONE_MONTH" -> 30 * 24 * 60 * 60 * 1000L
            else -> 0L
        }

        if (delayMillis <= 0L) return Result.success()

        val thresholdDate = Date(System.currentTimeMillis() - delayMillis)

        database.tabDao().markOpenTabsOlderThan(thresholdDate, Date())

        return Result.success()
    }
}
