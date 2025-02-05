package com.example.mobilebrowser.receiver

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.util.Log
import com.example.mobilebrowser.data.entity.DownloadStatus
import com.example.mobilebrowser.data.repository.DownloadRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadCompleteReceiver : BroadcastReceiver() {

    // Instead of passing a ViewModel in the constructor,
    // we inject the Repository (or other dependencies) directly.
    @Inject
    lateinit var downloadRepository: DownloadRepository

    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) {
            Log.e(TAG, "Invalid download ID received")
            return
        } else {
            Log.d(TAG, "Download ID received: $downloadId")
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = dm.query(query)

        if (cursor != null && cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            when (val status = cursor.getInt(statusIndex)) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    Log.d(TAG, "System Download $downloadId completed successfully")
                    updateDownloadStatusBySystemId(downloadId, DownloadStatus.COMPLETED)
                }
                DownloadManager.STATUS_FAILED -> {
                    Log.d(TAG, "System Download $downloadId failed")
                    updateDownloadStatusBySystemId(downloadId, DownloadStatus.FAILED)
                }
                else -> {
                    // Other statuses like PAUSED, RUNNING, etc. are possible
                    Log.d(TAG, "System Download $downloadId status: $status")
                }
            }
        }
        cursor?.close()
    }

    private fun updateDownloadStatusBySystemId(androidDownloadId: Long, status: DownloadStatus) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Directly call repository methods.
                downloadRepository.updateDownloadStatusByAndroidId(androidDownloadId, status)

                if (status == DownloadStatus.COMPLETED) {
                    val download = downloadRepository.getDownloadByAndroidId(androidDownloadId)
                    if (download != null) {
                        Log.d(TAG, "Marked download ${download.filename} as completed in DB.")
                        // If you want to display a notification or do something else,
                        // you'd do it here. For a UI dialog, typically you'd rely on your 
                        // app's DB state or a notification instead, since we aren't in an Activity.
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update status: ${e.message}", e)
            }
        }
    }

    companion object {
        private const val TAG = "DownloadCompleteReceiver"
    }
}
