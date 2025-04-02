package com.example.mobilebrowser.data.dto

import java.util.Date

/**
 * Data Transfer Object for a history entry that matches the current backend schema.
 * Fields:
 * - id: The server-assigned identifier (optional).
 * - userId: The user identifier, ensuring data isolation.
 * - url: The visited webpage URL.
 * - title: The title of the webpage.
 * - timestamp: The time the entry was created or last modified.
 * - device: The device identifier making the request.
 */
data class HistoryDto(
    val id: String? = null,
    val userId: String,
    val url: String,
    val title: String,
    val timestamp: Date,
    val device: String
)
data class ApiResponse<T>(
    val message: String,
    val data: T
)

