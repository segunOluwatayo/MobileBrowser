package com.example.mobilebrowser.data.dto

import java.util.Date

/**
 * Data Transfer Object for a bookmark entry.
 *
 * Fields:
 * - id: The server-assigned identifier (optional).
 * - userId: The identifier of the user owning this bookmark.
 * - title: The bookmark title.
 * - url: The bookmark URL.
 * - favicon: The favicon URL or path.
 * - tags: Optional tags for the bookmark.
 * - timestamp: The time the bookmark was added or last modified.
 */
data class BookmarkDto(
    val id: String? = null,
    val userId: String,
    val title: String,
    val url: String,
    val favicon: String?,
    val tags: String?,
    val timestamp: Date
)
