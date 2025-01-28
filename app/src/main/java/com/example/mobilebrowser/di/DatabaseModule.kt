package com.example.mobilebrowser.di

import android.content.Context
import com.example.mobilebrowser.data.database.BookmarkDatabase
import com.example.mobilebrowser.data.dao.BookmarkDao
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
    fun provideBookmarkDatabase(
        @ApplicationContext context: Context
    ): BookmarkDatabase {
        return BookmarkDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideBookmarkDao(database: BookmarkDatabase): BookmarkDao {
        return database.bookmarkDao()
    }
}