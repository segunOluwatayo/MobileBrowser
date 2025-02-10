package com.example.mobilebrowser.data.model

/**
 * Data class representing a search engine.
 */
data class SearchEngine(
    val name: String,
    val logo: Int, // Resource ID for the logo
    val baseUrl: String
)