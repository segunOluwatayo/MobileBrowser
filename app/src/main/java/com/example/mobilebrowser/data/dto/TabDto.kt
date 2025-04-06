package com.example.mobilebrowser.data.dto

import java.util.Date

/**
 * Data Transfer Object for a tab entry.
 *
 * Fields match the MongoDB Tab model structure:
 * - id: The server-assigned identifier (optional)
 * - userId: User identifier for data separation
 * - url: The tab's current URL
 * - title: The tab's page title
 * - scrollPosition: Saved scroll position (optional)
 * - timestamp: Creation or last modification time
 * - device: Identifier of the source device
 */
data class TabDto(
    val id: String? = null,
    val userId: String? = null,
    val url: String,
    val title: String? = "",
    val scrollPosition: Int? = null,
    val timestamp: Date? = null,
    val device: String? = null
)