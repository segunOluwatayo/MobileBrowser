// DownloadCompleteReceiver.kt

package com.example.mobilebrowser.receiver

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.util.Log
import com.example.mobilebrowser.data.entity.DownloadStatus
import com.example.mobilebrowser.ui.viewmodels.DownloadViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadCompleteReceiver(
    private val downloadViewModel: DownloadViewModel
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) {
            Log.e("DownloadCompleteReceiver", "Invalid download ID received")
            return
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = dm.query(query)

        if (cursor != null && cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            when (val status = cursor.getInt(statusIndex)) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    Log.d("DownloadCompleteReceiver", "Download $downloadId completed successfully")
                    updateDownloadStatus(downloadId, DownloadStatus.COMPLETED)
                }
                DownloadManager.STATUS_FAILED -> {
                    Log.d("DownloadCompleteReceiver", "Download $downloadId failed")
                    updateDownloadStatus(downloadId, DownloadStatus.FAILED)
                }
                else -> {
                    Log.d("DownloadCompleteReceiver", "Download $downloadId has status: $status")
                }
            }
        }
        cursor?.close()
    }

    private fun updateDownloadStatus(downloadId: Long, status: DownloadStatus) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                downloadViewModel.updateDownloadStatus(downloadId, status)
            } catch (e: Exception) {
                Log.e("DownloadCompleteReceiver", "Failed to update download status: ${e.message}")
            }
        }
    }
}