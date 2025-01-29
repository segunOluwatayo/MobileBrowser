package com.example.mobilebrowser.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents a browser tab in the database.
 *
 */
@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // The current URL being displayed in the tab
    val url: String,

    // The title of the current page
    val title: String,

    // Timestamp when the tab was created
    val createdAt: Date = Date(),

    // Timestamp of the last visit/update
    val lastVisited: Date = Date(),

    // Whether this tab is currently active/selected
    val isActive: Boolean = false,

    // Position/order of the tab in the tab list
    val position: Int = 0
)