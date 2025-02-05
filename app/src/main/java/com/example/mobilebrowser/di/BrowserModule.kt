package com.example.mobilebrowser.di

import android.app.DownloadManager
import android.content.Context
import com.example.mobilebrowser.browser.GeckoSessionManager
import com.example.mobilebrowser.data.repository.DownloadRepository
import com.example.mobilebrowser.ui.viewmodels.DownloadViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BrowserModule {
    @Provides
    @Singleton
    fun provideDownloadManager(@ApplicationContext context: Context): DownloadManager {
        return context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    @Provides
    @Singleton
    fun provideGeckoSessionManager(
        @ApplicationContext context: Context,
        downloadManager: DownloadManager,
        downloadRepository: DownloadRepository
    ): GeckoSessionManager {
        return GeckoSessionManager(context, downloadManager, downloadRepository)
    }
}