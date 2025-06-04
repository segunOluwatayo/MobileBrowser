package com.example.mobilebrowser.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val userId: String,

    val url: String,

    // Optional field to store the favicon of the bookmarked page.
    val favicon: String?,

    val lastVisited: Date,

    // Optional field to store tags for categorizing bookmarks.
    val tags: String?,

    val dateAdded: Date = Date(),

    val serverId: String? = null,

    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)
