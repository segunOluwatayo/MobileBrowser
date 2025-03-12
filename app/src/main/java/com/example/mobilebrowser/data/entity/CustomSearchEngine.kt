package com.example.mobilebrowser.data.entity

data class CustomSearchEngine(
    val name: String,
    val searchUrl: String,
    val faviconUrl: String? = null
)