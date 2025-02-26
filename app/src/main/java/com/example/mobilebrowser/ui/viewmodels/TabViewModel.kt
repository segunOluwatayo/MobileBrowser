package com.example.mobilebrowser.ui.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.mobilebrowser.data.entity.TabEntity
import com.example.mobilebrowser.data.repository.TabRepository
import com.example.mobilebrowser.data.util.DataStoreManager
import com.example.mobilebrowser.data.util.ThumbnailUtil
import com.example.mobilebrowser.worker.TabAutoCloseWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoView
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class TabViewModel @Inject constructor(
    private val repository: TabRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    // Initialization state
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // DataStoreManager instance using the injected context.
    private val dataStoreManager = DataStoreManager(context)

    // Define currentTabPolicy from DataStore.
    private val currentTabPolicy: StateFlow<String> = dataStoreManager.tabManagementPolicyFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DataStoreManager.DEFAULT_TAB_POLICY
    )

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Stream of all tabs
    val tabs = repository.getAllTabs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Stream of the active tab
    val activeTab = repository.getActiveTab()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Stream of tab count
    val tabCount = repository.getTabCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Selection mode state
    private val _isSelectionModeActive = MutableStateFlow(false)
    val isSelectionModeActive = _isSelectionModeActive.asStateFlow()

    private val _selectedTabs = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTabs = _selectedTabs.asStateFlow()

    init {
        // Initialize browser with default tab on startup
        viewModelScope.launch {
            initializeDefaultTab()
            // Schedule auto-close worker based on the current policy at startup.
            scheduleTabAutoClose(currentTabPolicy.value)
        }
    }

    private fun initializeDefaultTab() {
        viewModelScope.launch {
            try {
                val count = repository.getTabCount().first()
                if (count == 0) {
                    Log.d("TabViewModel", "Initializing with default tab...")
                    val newTabId = createTab()
                    switchToTab(newTabId)  // Ensuring it's set as active
                    _isInitialized.value = true
                }
            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to initialize browser: ${e.message}")
                _error.value = "Failed to initialize browser: ${e.message}"
            }
        }
    }



    suspend fun createTab(
        url: String = "",
        title: String = "New Tab"
    ): Long {
        return try {
            val newTabId = repository.createTab(url, title, tabCount.value)
            Log.d("TabViewModel", "Created new tab: $newTabId")
            newTabId
        } catch (e: Exception) {
            Log.e("TabViewModel", "Failed to create tab: ${e.message}")
            throw e
        }
    }


    fun switchToTab(tabId: Long) {
        viewModelScope.launch {
            try {
                repository.switchToTab(tabId)
                Log.d("TabViewModel", "Switched to tab: $tabId")
            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to switch tab: ${e.message}")
                _error.value = "Failed to switch tab: ${e.message}"
            }
        }
    }

    fun updateActiveTabContent(url: String, title: String) {
        Log.d("TabViewModel", "updateActiveTabContent with URL=$url, title=$title")
        viewModelScope.launch {
            try {
                activeTab.value?.let { tab ->
                    repository.updateTab(
                        tab.copy(
                            url = url,
                            title = title,
                            lastVisited = Date()
                        )
                    )
                    Log.d("TabViewModel", "Updated tab content - URL: $url, Title: $title")
                }
            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to update tab content: ${e.message}")
            }
        }
    }

    fun closeTab(tab: TabEntity) {
        viewModelScope.launch {
            try {
                // Retrieve the current policy (e.g., "MANUAL", "ONE_DAY", etc.)
                val policy = currentTabPolicy.value

                if (policy == "MANUAL") {
                    // Delete the tab immediately.
                    repository.deleteTab(tab)
                } else {
                    // Mark the tab as closed by setting the closedAt timestamp.
                    repository.markTabAsClosed(tab.id, Date())
                }

                Log.d("TabViewModel", "Closed tab: ${tab.id}")

                // If no open tabs remain, create a new one.
                if (tabCount.value == 0) {
                    createTab()
                }
            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to close tab: ${e.message}")
            }
        }
    }


    fun closeAllTabs() {
        viewModelScope.launch {
            try {
                repository.deleteAllTabs()
                Log.d("TabViewModel", "Closed all tabs")

                // Ensure a new tab is created and set as active
                val newTabId = createTab()
                switchToTab(newTabId)  // Ensuring it's set as active

            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to close all tabs: ${e.message}")
            }
        }
    }

    private fun scheduleTabAutoClose(policy: String) {
        // Determine the delay based on the policy.
        val delayMillis = when (policy) {
            "ONE_DAY" -> 24 * 60 * 60 * 1000L
            "ONE_WEEK" -> 7 * 24 * 60 * 60 * 1000L
            "ONE_MONTH" -> 30 * 24 * 60 * 60 * 1000L
            else -> 0L
        }

        if (delayMillis > 0L) {
            val inputData = workDataOf("TAB_POLICY" to policy)
            val autoCloseRequest = OneTimeWorkRequestBuilder<TabAutoCloseWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork("tab_autoclose_$policy", ExistingWorkPolicy.REPLACE, autoCloseRequest)

            Log.d("TabViewModel", "Scheduled auto-close worker with delay: $delayMillis ms for policy: $policy")
        }
    }



    fun toggleSelectionMode() {
        _isSelectionModeActive.value = !_isSelectionModeActive.value
        if (!_isSelectionModeActive.value) {
            clearSelection()
        }
    }

    fun toggleTabSelection(tabId: Long) {
        val currentSelection = _selectedTabs.value.toMutableSet()
        if (currentSelection.contains(tabId)) {
            currentSelection.remove(tabId)
        } else {
            currentSelection.add(tabId)
        }
        _selectedTabs.value = currentSelection
    }

    private fun clearSelection() {
        _selectedTabs.value = emptySet()
    }

    fun closeSelectedTabs() {
        viewModelScope.launch {
            try {
                _selectedTabs.value.forEach { tabId ->
                    repository.getTabById(tabId)?.let { tab ->
                        repository.deleteTab(tab)
                    }
                }
                clearSelection()
                toggleSelectionMode()

                // If we closed all tabs, create a new one
                if (tabCount.value == 0) {
                    createTab()
                }

                Log.d("TabViewModel", "Closed selected tabs")
            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to close selected tabs: ${e.message}")
            }
        }
    }

    fun moveTab(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            try {
                val tabList = tabs.value.toMutableList()
                if (fromIndex in tabList.indices && toIndex in tabList.indices) {
                    val tab = tabList.removeAt(fromIndex)
                    tabList.add(toIndex, tab)

                    // Update positions for all affected tabs
                    tabList.forEachIndexed { index, tabEntity ->
                        repository.updateTabPosition(tabEntity.id, index)
                    }
                    Log.d("TabViewModel", "Moved tab from $fromIndex to $toIndex")
                }
            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to move tab: ${e.message}")
            }
        }
    }

    suspend fun getTabById(tabId: Long): TabEntity? {
        return try {
            repository.getTabById(tabId)
        } catch (e: Exception) {
            Log.e("TabViewModel", "Failed to get tab by ID: ${e.message}")
            null
        }
    }

    fun clearError() {
        _error.value = null
    }

//    fun updateTabThumbnail(tabId: Long, view: View) {
//        Log.d("TabViewModel", "updateTabThumbnail called for tabId: $tabId")
//        viewModelScope.launch {
//            // Capture the thumbnail from the provided view.
//            val bitmap = ThumbnailUtil.captureThumbnail(view)
//            if (bitmap != null) {
//                // Define where to store the thumbnail. Here we use the cache directory.
//                val thumbnailFile = File(context.cacheDir, "thumbnail_$tabId.png")
//                val thumbnailPath = ThumbnailUtil.saveBitmapToFile(bitmap, thumbnailFile)
//                if (thumbnailPath != null) {
//                    // Retrieve the tab, update its thumbnail property, and save it.
//                    repository.getTabById(tabId)?.let { tab ->
//                        repository.updateTab(tab.copy(thumbnail = thumbnailPath))
//                    }
//                }
//            }
//        }
//    }
fun updateTabThumbnail(tabId: Long, view: View) {
    viewModelScope.launch {
        if (view is GeckoView) {
            val result: GeckoResult<Bitmap> = view.capturePixels()
            result.accept { bitmap: Bitmap? ->
                if (bitmap != null) {
                    // Optionally downscale or process bitmap if desired.
                    val thumbnailFile = File(context.cacheDir, "thumbnail_$tabId.png")
                    val thumbnailPath = ThumbnailUtil.saveBitmapToFile(bitmap, thumbnailFile)
                    if (thumbnailPath != null) {
                        // Launch another coroutine to call suspend functions.
                        viewModelScope.launch {
                            repository.getTabById(tabId)?.let { tab ->
                                repository.updateTab(tab.copy(thumbnail = thumbnailPath))
                                Log.d("TabViewModel", "Updated thumbnail for tab $tabId via GeckoView capture")
                            }
                        }
                    } else {
                        Log.e("TabViewModel", "Failed to save captured thumbnail for tab $tabId")
                    }
                } else {
                    Log.e("TabViewModel", "GeckoView capturePixels returned null for tab $tabId")
                }
            }
        } else {
            // Fallback: use your existing capture method.
            val bitmap = ThumbnailUtil.captureThumbnail(view)
            if (bitmap != null) {
                val thumbnailFile = File(context.cacheDir, "thumbnail_$tabId.png")
                val thumbnailPath = ThumbnailUtil.saveBitmapToFile(bitmap, thumbnailFile)
                if (thumbnailPath != null) {
                    viewModelScope.launch {
                        repository.getTabById(tabId)?.let { tab ->
                            repository.updateTab(tab.copy(thumbnail = thumbnailPath))
                            Log.d("TabViewModel", "Updated thumbnail for tab $tabId via fallback capture")
                        }
                    }
                } else {
                    Log.e("TabViewModel", "Fallback: Failed to save thumbnail for tab $tabId")
                }
            } else {
                Log.e("TabViewModel", "Fallback: Failed to capture thumbnail for tab $tabId")
            }
        }
    }
}


    fun debugThumbnails() {
        viewModelScope.launch {
            val allTabs = tabs.value
            Log.d("TabViewModel", "Debugging thumbnails for ${allTabs.size} tabs")

            allTabs.forEach { tab ->
                Log.d("TabViewModel", "Tab ${tab.id}: URL=${tab.url}, Thumbnail=${tab.thumbnail}")
                if (!tab.thumbnail.isNullOrEmpty()) {
                    val file = File(tab.thumbnail!!)
                    Log.d("TabViewModel", "  Thumbnail file exists: ${file.exists()}, size: ${file.length()} bytes")
                }
            }
        }
    }

}