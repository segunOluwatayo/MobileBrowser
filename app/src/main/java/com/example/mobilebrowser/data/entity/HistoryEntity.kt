package com.example.mobilebrowser.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents a single history entry in the database.
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // The title of the visited page
    val title: String,

    // The URL of the visited page
    val url: String,

    // Optional field to store the favicon of the visited page
    val favicon: String?,

    // First time the page was visited
    val firstVisited: Date = Date(),

    // Last time the page was visited (updated on revisits)
    val lastVisited: Date = Date(),

    // Number of times the page has been visited
    val visitCount: Int = 1
)