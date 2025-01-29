package com.example.mobilebrowser.di

import android.content.Context
import com.example.mobilebrowser.data.database.BrowserDatabase
import com.example.mobilebrowser.data.dao.BookmarkDao
import com.example.mobilebrowser.data.dao.TabDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideBrowserDatabase(
        @ApplicationContext context: Context
    ): BrowserDatabase {
        return BrowserDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideBookmarkDao(database: BrowserDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    @Singleton
    fun provideTabDao(database: BrowserDatabase): TabDao {
        return database.tabDao()
    }
}