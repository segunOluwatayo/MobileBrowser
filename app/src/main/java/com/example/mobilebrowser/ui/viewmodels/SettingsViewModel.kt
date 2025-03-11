package com.example.mobilebrowser.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.data.util.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

    /**
     * Exposes the current search engine selection as a StateFlow.
     * The default value is provided by DataStoreManager.DEFAULT_SEARCH_ENGINE.
     */
    val searchEngine: StateFlow<String> = dataStoreManager.searchEngineFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DataStoreManager.DEFAULT_SEARCH_ENGINE
    )

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
}
