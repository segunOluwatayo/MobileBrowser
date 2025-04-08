package com.example.mobilebrowser

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import com.example.mobilebrowser.data.service.AuthService
import dagger.hilt.android.HiltAndroidApp
import androidx.work.Configuration
import javax.inject.Inject

@HiltAndroidApp
class BrowserApplication : Application(), HasAuthService, Configuration.Provider {
    @Inject
    lateinit var authServiceInstance: AuthService

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun getAuthService(): AuthService = authServiceInstance

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}