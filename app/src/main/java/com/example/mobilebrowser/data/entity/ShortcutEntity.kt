package com.example.mobilebrowser.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shortcuts",
    indices = [Index(value = ["url"], unique = true)]
)
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val label: String,
    val iconRes: Int,
    val url: String,
    val isPinned: Boolean = false,
    val shortcutType: ShortcutType = ShortcutType.MANUAL,
    val visitCount: Int = 0,
    val lastVisited: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val favicon: String? = null
)

enum class ShortcutType {
    MANUAL,
    DYNAMIC,
    PINNED
}
