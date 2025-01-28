package com.example.mobilebrowser.data.util

import androidx.room.TypeConverter
import java.util.Date

// Custom TypeConverters for Room to handle non-primitive data types (e.g., Date).
class Converters {

    // Converts a Long timestamp (stored in the database) to a Date object.
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) } // Converts the timestamp to a Date if not null.
    }

    // Converts a Date object to a Long timestamp for storing in the database.
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time // Extracts the time (in milliseconds) from the Date object.
    }
}
