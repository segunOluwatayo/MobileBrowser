package com.example.mobilebrowser.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Enum class representing the synchronization status of a history entry.
 * - SYNCED: Entry is fully synchronized with the server.
 * - PENDING_UPLOAD: New or updated entry waiting to be sent to the server.
 * - PENDING_DELETE: Entry marked for deletion on the server.
 * - CONFLICT: A data conflict has been detected between local and remote versions.
 */
enum class SyncStatus {
    SYNCED,
    PENDING_UPLOAD,
    PENDING_DELETE,
    CONFLICT
}

/**
 * Data class representing a browsing history entry in the local database.
 * Extended to support two-way data synchronization with a remote MongoDB server.
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: String,

    val title: String,

    val url: String,

    val favicon: String?,

    val firstVisited: Date = Date(),

    val lastVisited: Date = Date(),

    val visitCount: Int = 1,

    val lastModified: Date = Date(),

    val serverId: String? = null,

    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)
