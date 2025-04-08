package com.example.mobilebrowser.data.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Singleton
class UserDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.userDataStore

    // Keys for user authentication data.
    companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        val USER_ID_KEY = stringPreferencesKey("user_id")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        val IS_SIGNED_IN_KEY = booleanPreferencesKey("is_signed_in")
        val DEVICE_ID_KEY = stringPreferencesKey("device_id")

        // New keys for sync preferences
        val SYNC_HISTORY_ENABLED_KEY = booleanPreferencesKey("sync_history_enabled")
        val SYNC_BOOKMARKS_ENABLED_KEY = booleanPreferencesKey("sync_bookmarks_enabled")
        val SYNC_TABS_ENABLED_KEY = booleanPreferencesKey("sync_tabs_enabled")

        // New key for last sync timestamp
        val LAST_SYNC_TIMESTAMP_KEY = longPreferencesKey("last_sync_timestamp")

        // Default sync settings (all enabled by default)
        const val DEFAULT_SYNC_HISTORY_ENABLED = true
        const val DEFAULT_SYNC_BOOKMARKS_ENABLED = true
        const val DEFAULT_SYNC_TABS_ENABLED = true
    }

    // Expose authentication state as a Flow.
    val isSignedIn: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[IS_SIGNED_IN_KEY] ?: false
        }

    // Flow for last sync timestamp
    val lastSyncTimestamp: Flow<Long?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[LAST_SYNC_TIMESTAMP_KEY]
        }

    // Flow for user name (display name).
    val userName: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[USER_NAME_KEY] ?: ""
        }

    // Flow for user email.
    val userEmail: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[USER_EMAIL_KEY] ?: ""
        }

    // Retrieve the access token.
    val accessToken: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[ACCESS_TOKEN_KEY] ?: ""
        }

    // Retrieve the refresh token.
    val refreshToken: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[REFRESH_TOKEN_KEY] ?: ""
        }

    // Expose the user ID.
    val userId: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[USER_ID_KEY] ?: ""
        }

    // Expose the device ID.
    val deviceId: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[DEVICE_ID_KEY] ?: ""
        }

    // New flows for sync preferences

    // History sync preference
    val syncHistoryEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SYNC_HISTORY_ENABLED_KEY] ?: DEFAULT_SYNC_HISTORY_ENABLED
        }

    // Bookmarks sync preference
    val syncBookmarksEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SYNC_BOOKMARKS_ENABLED_KEY] ?: DEFAULT_SYNC_BOOKMARKS_ENABLED
        }

    // Tabs sync preference
    val syncTabsEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SYNC_TABS_ENABLED_KEY] ?: DEFAULT_SYNC_TABS_ENABLED
        }

    // Save user authentication data.
    suspend fun saveUserAuthData(
        accessToken: String,
        refreshToken: String,
        userId: String,
        displayName: String,
        email: String,
        deviceId: String? = null
    ) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
            preferences[USER_ID_KEY] = userId
            preferences[USER_NAME_KEY] = displayName
            preferences[USER_EMAIL_KEY] = email
            preferences[IS_SIGNED_IN_KEY] = true
            deviceId?.let {
                preferences[DEVICE_ID_KEY] = it
            }
        }
    }

    // Clear user authentication data (logout).
    suspend fun clearUserAuthData() {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = ""
            preferences[REFRESH_TOKEN_KEY] = ""
            preferences[USER_ID_KEY] = ""
            preferences[USER_NAME_KEY] = ""
            preferences[USER_EMAIL_KEY] = ""
            preferences[IS_SIGNED_IN_KEY] = false
            // Optionally keep device ID for analytics or future sign-ins.
        }
    }

    // Methods to update sync preferences

    // Update history sync preference
    suspend fun updateSyncHistoryEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SYNC_HISTORY_ENABLED_KEY] = enabled
        }
    }

    // Update bookmarks sync preference
    suspend fun updateSyncBookmarksEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SYNC_BOOKMARKS_ENABLED_KEY] = enabled
        }
    }

    // Update tabs sync preference
    suspend fun updateSyncTabsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SYNC_TABS_ENABLED_KEY] = enabled
        }
    }

    // Update last sync timestamp
    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_SYNC_TIMESTAMP_KEY] = timestamp
        }
    }
}