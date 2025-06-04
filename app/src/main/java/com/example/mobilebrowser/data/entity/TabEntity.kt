package com.example.mobilebrowser.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents a browser tab in the database.
 */
@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val url: String,

    val title: String,

    val createdAt: Date = Date(),

    val lastVisited: Date = Date(),

    val isActive: Boolean = false,

    val position: Int = 0,

    val closedAt: Date? = null,

    val thumbnail: String? = null,

    val userId: String = "",
    val serverId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)
