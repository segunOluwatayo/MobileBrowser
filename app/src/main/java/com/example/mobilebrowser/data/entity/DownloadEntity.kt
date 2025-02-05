package com.example.mobilebrowser.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Basic file information
    val filename: String,
    val path: String,
    val mimeType: String,
    val fileSize: Long,

    // Download status and metadata
    val downloadDate: Date = Date(),
    val status: DownloadStatus,
    val sourceUrl: String,

    // Android DownloadManager ID
    val androidDownloadId: Long,

    // Optional metadata
    val contentDisposition: String? = null,
    val error: String? = null
)

enum class DownloadStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED,
    DELETED
}