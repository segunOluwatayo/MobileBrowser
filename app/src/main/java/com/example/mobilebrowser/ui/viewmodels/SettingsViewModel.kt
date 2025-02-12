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
}
