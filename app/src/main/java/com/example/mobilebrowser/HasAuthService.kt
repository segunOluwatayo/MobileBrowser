package com.example.mobilebrowser

import com.example.mobilebrowser.data.service.AuthService

interface HasAuthService {
    fun getAuthService(): AuthService
}