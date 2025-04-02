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
    // Auto-generated primary key for the local database.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // User identifier to associate this history entry with a specific authenticated user.
    val userId: String,

    // Title of the visited webpage.
    val title: String,

    // URL of the visited webpage.
    val url: String,

    // Optional field to store the favicon URL of the visited page.
    val favicon: String?,

    // The timestamp when the page was first visited.
    val firstVisited: Date = Date(),

    // The timestamp when the page was last visited (updated on revisits).
    val lastVisited: Date = Date(),

    // Number of times the page has been visited.
    val visitCount: Int = 1,

    // Timestamp used for conflict resolution during synchronization.
    val lastModified: Date = Date(),

    // Optional field to store the record's ID from the MongoDB server.
    val serverId: String? = null,

    // Current sync status of the entry. Default is set to PENDING_UPLOAD, indicating a new entry to be synced.
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)
