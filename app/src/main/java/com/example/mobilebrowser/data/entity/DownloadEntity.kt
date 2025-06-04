package com.example.mobilebrowser.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents a downloaded file in the database.
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // The name of the downloaded file
    val fileName: String,

    // The original URL from which the file was downloaded
    val sourceUrl: String,

    // Local path where the file is stored
    val localPath: String,

    // Size of the file in bytes
    val fileSize: Long,

    // Current status of the download
    val status: DownloadStatus,

    // When the download was initiated
    val dateAdded: Date = Date(),

    // When the download was completed
    val dateCompleted: Date? = null,

    // MIME type of the file
    val mimeType: String,

    // Optional field for any additional metadata
    val metadata: String? = null
)

enum class DownloadStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}