package com.example.mobilebrowser.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing a stored password.
 */
@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val siteUrl: String,
    val username: String,
    val encryptedPassword: String,
    val dateAdded: Date = Date()
)
