package com.example.mobilebrowser.data.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Create an extension property for DataStore
val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * DataStoreManager handles the persistence of app settings using Jetpack DataStore.
 *
 * Currently, it manages the search engine setting.
 */
class DataStoreManager(private val context: Context) {

    companion object {
        // Key for storing the search engine URL
        val SEARCH_ENGINE_KEY = stringPreferencesKey("search_engine")
        // Default search engine URL (Google in this case)
        const val DEFAULT_SEARCH_ENGINE = "https://www.google.com/search?q="

        // New key for Tab Management Policy
        val TAB_MANAGEMENT_POLICY_KEY = stringPreferencesKey("tab_management_policy")
        // Default policy (other options could be "ONE_DAY", "ONE_WEEK", "ONE_MONTH")
        const val DEFAULT_TAB_POLICY = "MANUAL"

        // New key for Theme Mode
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        // Default theme mode ("SYSTEM" means following the device's setting)
        const val DEFAULT_THEME_MODE = "SYSTEM"

        // New key for homepage setting
        val HOMEPAGE_ENABLED_KEY = booleanPreferencesKey("homepage_enabled")
        const val DEFAULT_HOMEPAGE_ENABLED = true

        // New keys for other settings
        val RECENT_TAB_ENABLED_KEY = booleanPreferencesKey("recent_tab_enabled")
        const val DEFAULT_RECENT_TAB_ENABLED = true

        val BOOKMARKS_ENABLED_KEY = booleanPreferencesKey("bookmarks_enabled")
        const val DEFAULT_BOOKMARKS_ENABLED = true

        val HISTORY_ENABLED_KEY = booleanPreferencesKey("history_enabled")
        const val DEFAULT_HISTORY_ENABLED = true

        // New key for address bar location
        val ADDRESS_BAR_LOCATION_KEY = stringPreferencesKey("address_bar_location")
        // Default location is "TOP" (other option is "BOTTOM")
        const val DEFAULT_ADDRESS_BAR_LOCATION = "TOP"
    }

    /**
     * Exposes the current search engine setting as a Flow.
     * If not set, returns the default search engine URL.
     */
    val searchEngineFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            // Emit emptyPreferences in case of an IOException
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SEARCH_ENGINE_KEY] ?: DEFAULT_SEARCH_ENGINE
        }

    /**
     * Exposes the current tab management policy as a Flow.
     */
    val tabManagementPolicyFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[TAB_MANAGEMENT_POLICY_KEY] ?: DEFAULT_TAB_POLICY
        }

    /**
     * Exposes the current theme mode as a Flow.
     * If not set, returns the default theme mode.
     */
    val themeModeFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[THEME_MODE_KEY] ?: DEFAULT_THEME_MODE
        }

    /**
     * Exposes the current homepage setting as a Flow.
     * If not set, returns the default homepage setting.
     */
    val homepageEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[HOMEPAGE_ENABLED_KEY] ?: DEFAULT_HOMEPAGE_ENABLED
        }

    val recentTabEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[RECENT_TAB_ENABLED_KEY] ?: DEFAULT_RECENT_TAB_ENABLED
        }

    val bookmarksEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[BOOKMARKS_ENABLED_KEY] ?: DEFAULT_BOOKMARKS_ENABLED
        }

    val historyEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[HISTORY_ENABLED_KEY] ?: DEFAULT_HISTORY_ENABLED
        }

    /**
     * Exposes the address bar location setting as a Flow.
     * If not set, returns the default location (TOP).
     */
    val addressBarLocationFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[ADDRESS_BAR_LOCATION_KEY] ?: DEFAULT_ADDRESS_BAR_LOCATION
        }

    suspend fun updateRecentTabEnabled(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[RECENT_TAB_ENABLED_KEY] = isEnabled
        }
    }

    suspend fun updateBookmarksEnabled(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BOOKMARKS_ENABLED_KEY] = isEnabled
        }
    }

    suspend fun updateHistoryEnabled(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HISTORY_ENABLED_KEY] = isEnabled
        }
    }

    /**
     * Updates the search engine setting.
     *
     * @param searchEngine The new search engine URL to persist.
     */
    suspend fun updateSearchEngine(searchEngine: String) {
        context.dataStore.edit { preferences ->
            preferences[SEARCH_ENGINE_KEY] = searchEngine
        }
    }

    /**
     * Updates the tab management policy.
     *
     * @param policy The new policy as a string (e.g., "MANUAL", "ONE_DAY", "ONE_WEEK", "ONE_MONTH").
     */
    suspend fun updateTabManagementPolicy(policy: String) {
        context.dataStore.edit { preferences ->
            preferences[TAB_MANAGEMENT_POLICY_KEY] = policy
        }
    }

    /**
     * Updates the theme mode setting.
     *
     * @param themeMode The new theme mode to persist (e.g., "SYSTEM", "LIGHT", "DARK").
     */
    suspend fun updateThemeMode(themeMode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = themeMode
        }
    }

    /**
     * Updates the homepage setting.
     *
     * @param isEnabled Whether the homepage should be enabled.
     */
    suspend fun updateHomepageEnabled(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HOMEPAGE_ENABLED_KEY] = isEnabled
        }
    }

    /**
     * Updates the address bar location setting.
     *
     * @param location The new location as a string (e.g., "TOP", "BOTTOM").
     */
    suspend fun updateAddressBarLocation(location: String) {
        context.dataStore.edit { preferences ->
            preferences[ADDRESS_BAR_LOCATION_KEY] = location
        }
    }
}