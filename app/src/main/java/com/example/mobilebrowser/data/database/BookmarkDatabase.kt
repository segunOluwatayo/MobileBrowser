package com.example.mobilebrowser.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mobilebrowser.data.dao.BookmarkDao
import com.example.mobilebrowser.data.entity.BookmarkEntity
import com.example.mobilebrowser.data.util.Converters

@Database(
    entities = [BookmarkEntity::class], // The BookmarkEntity table is part of this database
    version = 1, // Database version, used for migrations
    exportSchema = false // Prevents exporting schema
)
@TypeConverters(Converters::class) // Allows Room to use custom converters for Date fields
abstract class BookmarkDatabase : RoomDatabase() {

    // Provides access to the DAO (Data Access Object) for performing database operations
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        // Volatile ensures that changes to this variable are visible to all threads immediately
        @Volatile
        private var INSTANCE: BookmarkDatabase? = null

        // Singleton pattern to provide a single instance of the database.
        fun getInstance(context: Context): BookmarkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, // Use the application context to prevent memory leaks
                    BookmarkDatabase::class.java,
                    "bookmark_database"
                )
                    .fallbackToDestructiveMigration() // Deletes data if the schema changes
                    .build()
                INSTANCE = instance // Save the instance to the singleton
                instance
            }
        }
    }
}
