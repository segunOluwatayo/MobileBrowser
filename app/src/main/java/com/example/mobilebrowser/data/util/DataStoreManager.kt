package com.example.mobilebrowser.data.util

import android.content.Context
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
     * Updates the search engine setting.
     *
     * @param searchEngine The new search engine URL to persist.
     */
    suspend fun updateSearchEngine(searchEngine: String) {
        context.dataStore.edit { preferences ->
            preferences[SEARCH_ENGINE_KEY] = searchEngine
        }
    }
}
