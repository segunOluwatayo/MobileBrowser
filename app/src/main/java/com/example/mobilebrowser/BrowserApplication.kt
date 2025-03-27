package com.example.mobilebrowser

import android.app.Application
import com.example.mobilebrowser.data.service.AuthService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BrowserApplication : Application(), HasAuthService {
    @Inject
    lateinit var authServiceInstance: AuthService

    override fun getAuthService(): AuthService = authServiceInstance
}