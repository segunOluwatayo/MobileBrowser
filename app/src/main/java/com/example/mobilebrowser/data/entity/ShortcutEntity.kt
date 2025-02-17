package com.example.mobilebrowser.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val label: String,
    val iconRes: Int,
    val url: String,
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
