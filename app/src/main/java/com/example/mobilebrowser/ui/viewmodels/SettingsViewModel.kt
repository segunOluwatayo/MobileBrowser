package com.example.mobilebrowser.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.data.entity.CustomSearchEngine
import com.example.mobilebrowser.data.util.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SettingsViewModel manages the search engine setting.
 *
 * It exposes the current search engine as a StateFlow and provides
 * methods to update the search engine preference.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    // Create an instance of DataStoreManager using the injected application context.
    private val dataStoreManager = DataStoreManager(context)

    val searchEngine: StateFlow<String> = dataStoreManager.searchEngineFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DataStoreManager.DEFAULT_SEARCH_ENGINE
    )

    /**
     * Exposes the list of custom search engines stored in DataStore.
     */
    val customSearchEngines: StateFlow<List<CustomSearchEngine>> = dataStoreManager.customSearchEnginesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )



    /**
     * Holds an error message if the custom search engine URL is invalid.
     */
    private val _customEngineErrorMessage = MutableStateFlow<String?>(null)
    val customEngineErrorMessage: StateFlow<String?> = _customEngineErrorMessage

    /**
     * Exposes the current tab management policy as a StateFlow.
     */
    val tabManagementPolicy: StateFlow<String> = dataStoreManager.tabManagementPolicyFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DataStoreManager.DEFAULT_TAB_POLICY
    )

    /**
     * Exposes the current theme mode as a StateFlow.
     * The default value is provided by DataStoreManager.DEFAULT_THEME_MODE.
     */
    val themeMode: StateFlow<String> = dataStoreManager.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DataStoreManager.DEFAULT_THEME_MODE
    )

    /**
     * Exposes the homepage setting as a StateFlow.
     * This determines whether the homepage is enabled (e.g., showing pinned shortcuts).
     */
    val homepageEnabled: StateFlow<Boolean> = dataStoreManager.homepageEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DataStoreManager.DEFAULT_HOMEPAGE_ENABLED
    )

    /**
     * Exposes the address bar location setting as a StateFlow.
     * This determines whether the address bar is positioned at the top or bottom.
     */
    val addressBarLocation: StateFlow<String> = dataStoreManager.addressBarLocationFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DataStoreManager.DEFAULT_ADDRESS_BAR_LOCATION
    )

    /**
     * Updates the search engine setting.
     *
     * @param newEngine The new search engine URL to persist.
     */
    fun updateSearchEngine(newEngine: String) {
        viewModelScope.launch {
            dataStoreManager.updateSearchEngine(newEngine)
        }
    }

    /**
     * Updates the tab management policy.
     *
     * @param newPolicy The new policy as a string (e.g., "MANUAL", "ONE_DAY", "ONE_WEEK", "ONE_MONTH").
     */
    fun updateTabManagementPolicy(newPolicy: String) {
        viewModelScope.launch {
            dataStoreManager.updateTabManagementPolicy(newPolicy)
        }
    }

    /**
     * Updates the theme mode.
     *
     * @param newMode The new theme mode to persist (e.g., "SYSTEM", "LIGHT", "DARK").
     */
    fun updateThemeMode(newMode: String) {
        viewModelScope.launch {
            dataStoreManager.updateThemeMode(newMode)
        }
    }

    /**
     * Updates the homepage setting.
     *
     * @param isEnabled True if homepage should be enabled (displayed on new tabs/app launch).
     */
    fun updateHomepageEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.updateHomepageEnabled(isEnabled)
        }
    }

    /**
     * Updates the address bar location setting.
     *
     * @param location The new address bar location (e.g., "TOP", "BOTTOM").
     */
    fun updateAddressBarLocation(location: String) {
        viewModelScope.launch {
            dataStoreManager.updateAddressBarLocation(location)
        }
    }

    /**
     * Exposes the "Recent Tab" section visibility setting as a StateFlow.
     */
    val recentTabEnabled: StateFlow<Boolean> = dataStoreManager.recentTabEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DataStoreManager.DEFAULT_RECENT_TAB_ENABLED
    )

    /**
     * Exposes the "Bookmarks" section visibility setting as a StateFlow.
     */
    val bookmarksEnabled: StateFlow<Boolean> = dataStoreManager.bookmarksEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DataStoreManager.DEFAULT_BOOKMARKS_ENABLED
    )

    /**
     * Exposes the "History" section visibility setting as a StateFlow.
     */
    val historyEnabled: StateFlow<Boolean> = dataStoreManager.historyEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DataStoreManager.DEFAULT_HISTORY_ENABLED
    )

    /**
     * Updates the "Recent Tab" section visibility setting.
     *
     * @param isEnabled True if the section should be visible.
     */
    fun updateRecentTabEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.updateRecentTabEnabled(isEnabled)
        }
    }

    /**
     * Updates the "Bookmarks" section visibility setting.
     *
     * @param isEnabled True if the section should be visible.
     */
    fun updateBookmarksEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.updateBookmarksEnabled(isEnabled)
        }
    }

    /**
     * Updates the "History" section visibility setting.
     *
     * @param isEnabled True if the section should be visible.
     */
    fun updateHistoryEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.updateHistoryEnabled(isEnabled)
        }
    }

    /**
     * Validates and transforms the provided URL.
     *
     * - If the URL already contains a "%s" placeholder, it is returned as is.
     * - If it contains a common query parameter (e.g., "?q="), it attempts to
     *   replace the query string with "%s".
     * - Otherwise, returns null to indicate an invalid URL format.
     */
    private fun transformSearchUrl(url: String): String? {
        return if (url.contains("%s")) {
            url
        } else if (url.contains("?q=")) {
            // Use a regex to replace the query parameter's value with "%s"
            val regex = Regex("""(\?q=)[^&]*""")
            if (regex.containsMatchIn(url)) {
                url.replace(regex, "$1%s")
            } else {
                null
            }
        } else {
            null
        }
    }

//    /**
//     * Adds a new custom search engine after validating the URL format.
//     *
//     * If the URL doesn't contain (or can't be converted to contain) the required
//     * "%s" placeholder, an error message is set.
//     *
//     * @param name The name of the custom search engine.
//     * @param rawUrl The user-provided URL string.
//     */
//    fun addCustomSearchEngine(name: String, rawUrl: String) {
//        viewModelScope.launch {
//            val validatedUrl = transformSearchUrl(rawUrl)
//            if (validatedUrl == null) {
//                _customEngineErrorMessage.value = "Invalid URL format. URL must contain a '%s' placeholder."
//                return@launch
//            }
//            // Clear any previous error.
//            _customEngineErrorMessage.value = null
//            // Create a new custom search engine entity.
//            val newEngine = CustomSearchEngine(name = name, searchUrl = validatedUrl)
//            // Add the new engine to the stored list.
//            dataStoreManager.addCustomSearchEngine(newEngine)
//        }
//    }

    /**
     * Adds a new custom search engine after validating the URL format.
     *
     * If the URL doesn't contain (or can't be converted to contain) the required
     * "%s" placeholder, an error message is set.
     *
     * @param name The name of the custom search engine.
     * @param rawUrl The user-provided URL string.
     */
    fun addCustomSearchEngine(name: String, rawUrl: String) {
        viewModelScope.launch {
            val validatedUrl = transformSearchUrl(rawUrl)
            if (validatedUrl == null) {
                _customEngineErrorMessage.value = "Invalid URL format. URL must contain a '%s' placeholder."
                return@launch
            }

            // Extract domain for favicon
            val domain = try {
                val urlWithPlaceholder = validatedUrl.replace("%s", "query")
                val uri = java.net.URI(urlWithPlaceholder)
                uri.host
            } catch (e: Exception) {
                null
            }

            // Create favicon URL using Google's favicon service
            // This is a reliable way to get favicons for most websites
            val faviconUrl = domain?.let { "https://www.google.com/s2/favicons?domain=$it&sz=64" }

            // Clear any previous error
            _customEngineErrorMessage.value = null

            // Create a new custom search engine entity with the favicon URL
            val newEngine = CustomSearchEngine(
                name = name,
                searchUrl = validatedUrl,
                faviconUrl = faviconUrl
            )

            // Add the new engine to the stored list
            dataStoreManager.addCustomSearchEngine(newEngine)
        }
    }


    /**
     * Updates an existing custom search engine.
     *
     * @param existingEngine The engine to update
     * @param newName The new name for the engine
     * @param newUrl The new URL for the engine
     */
    fun updateCustomSearchEngine(existingEngine: CustomSearchEngine, newName: String, newUrl: String) {
        viewModelScope.launch {
            val validatedUrl = transformSearchUrl(newUrl)
            if (validatedUrl == null) {
                _customEngineErrorMessage.value = "Invalid URL format. URL must contain a '%s' placeholder."
                return@launch
            }

            // Extract domain for favicon (only if URL changed)
            val faviconUrl = if (existingEngine.searchUrl != validatedUrl) {
                val domain = try {
                    val urlWithPlaceholder = validatedUrl.replace("%s", "query")
                    val uri = java.net.URI(urlWithPlaceholder)
                    uri.host
                } catch (e: Exception) {
                    null
                }

                domain?.let { "https://www.google.com/s2/favicons?domain=$it&sz=64" }
            } else {
                existingEngine.faviconUrl
            }

            // Clear any previous error
            _customEngineErrorMessage.value = null

            // Get current list of engines
            val currentEngines = customSearchEngines.value.toMutableList()

            // Find and replace the existing engine
            val index = currentEngines.indexOfFirst {
                it.name == existingEngine.name && it.searchUrl == existingEngine.searchUrl
            }

            if (index != -1) {
                // Replace the engine at found index with updated favicon
                currentEngines[index] = CustomSearchEngine(
                    name = newName,
                    searchUrl = validatedUrl,
                    faviconUrl = faviconUrl
                )

                // Update the stored list
                dataStoreManager.updateCustomSearchEngines(currentEngines)

                // If this was the selected search engine, update the selection too
                if (existingEngine.searchUrl == searchEngine.value) {
                    updateSearchEngine(validatedUrl)
                }
            }
        }
    }

    /**
     * Deletes a custom search engine.
     *
     * @param engine The engine to delete
     */
    fun deleteCustomSearchEngine(engine: CustomSearchEngine) {
        viewModelScope.launch {
            // Get current list of engines
            val currentEngines = customSearchEngines.value.toMutableList()

            // Remove the engine
            currentEngines.removeIf {
                it.name == engine.name && it.searchUrl == engine.searchUrl
            }

            // Update the stored list
            dataStoreManager.updateCustomSearchEngines(currentEngines)

            // If this was the selected search engine, revert to default
            if (engine.searchUrl == searchEngine.value) {
                updateSearchEngine(DataStoreManager.DEFAULT_SEARCH_ENGINE)
            }
        }
    }
}