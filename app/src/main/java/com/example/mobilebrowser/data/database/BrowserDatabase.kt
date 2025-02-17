package com.example.mobilebrowser.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mobilebrowser.data.dao.BookmarkDao
import com.example.mobilebrowser.data.dao.DownloadDao
import com.example.mobilebrowser.data.dao.HistoryDao
import com.example.mobilebrowser.data.dao.ShortcutDao
import com.example.mobilebrowser.data.dao.TabDao
import com.example.mobilebrowser.data.entity.BookmarkEntity
import com.example.mobilebrowser.data.entity.DownloadEntity
import com.example.mobilebrowser.data.entity.HistoryEntity
import com.example.mobilebrowser.data.entity.ShortcutEntity
import com.example.mobilebrowser.data.entity.TabEntity
import com.example.mobilebrowser.data.util.Converters

/**
 * Main database class for the browser application.
 * Manages both bookmarks and tabs data.
 */
@Database(
    entities = [BookmarkEntity::class, TabEntity::class, HistoryEntity::class, DownloadEntity::class, ShortcutEntity::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BrowserDatabase : RoomDatabase() {
    // DAOs
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun tabDao(): TabDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun shortcutDao(): ShortcutDao


    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getInstance(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "browser_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}