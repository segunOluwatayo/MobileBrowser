package com.example.mobilebrowser.data.dto

import java.util.Date

data class UserProfileDto(
    val _id: String,
    val email: String,
    val name: String,
    val profilePicture: String? = null,
    val googleId: String? = null,
    val createdAt: Date,
    val updatedAt: Date
)