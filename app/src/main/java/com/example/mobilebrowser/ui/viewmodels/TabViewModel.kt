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
import com.example.mobilebrowser.data.dao.TabDao
import com.example.mobilebrowser.data.entity.SyncStatus
import com.example.mobilebrowser.data.entity.TabEntity
import com.example.mobilebrowser.data.repository.BookmarkRepository
import com.example.mobilebrowser.data.repository.TabRepository
import com.example.mobilebrowser.data.util.DataStoreManager
import com.example.mobilebrowser.data.util.ThumbnailUtil
import com.example.mobilebrowser.data.util.UserDataStore
import com.example.mobilebrowser.sync.SyncStatusState
import com.example.mobilebrowser.sync.UserSyncManager
import com.example.mobilebrowser.worker.TabAutoCloseWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private val bookmarkRepository: BookmarkRepository,
    private val userDataStore: UserDataStore,
    private val userSyncManager: UserSyncManager,
    private val tabDao: TabDao,
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
            // Schedule auto close worker based on the current policy at startup.
            scheduleTabAutoClose(currentTabPolicy.value)
        }
    }

    // Sync status state flow to provide UI feedback
    private val _syncStatus = MutableStateFlow<SyncStatusState>(SyncStatusState.Idle)
    val syncStatus: StateFlow<SyncStatusState> = _syncStatus

    // Last sync timestamp
    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    /**
     * Triggers manual synchronization for tabs.
     */
    fun triggerTabSync() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatusState.Syncing
            try {
                // Get authentication data
                val isSignedIn = userDataStore.isSignedIn.first()
                if (!isSignedIn) {
                    _syncStatus.value = SyncStatusState.Error("User not signed in")
                    return@launch
                }

                val accessToken = userDataStore.accessToken.first()
                val deviceId = userDataStore.deviceId.first().ifEmpty { "android-device" }
                val userId = userDataStore.userId.first()

                if (accessToken.isBlank() || userId.isBlank()) {
                    _syncStatus.value = SyncStatusState.Error("Missing authentication data")
                    return@launch
                }

                // Sync tabs
                userSyncManager.pushLocalTabChanges(accessToken, deviceId, userId)
                userSyncManager.pullRemoteTabs(accessToken, userId)

                // Update status
                _syncStatus.value = SyncStatusState.Synced
                _lastSyncTimestamp.value = System.currentTimeMillis()
            } catch (e: Exception) {
                _syncStatus.value = SyncStatusState.Error(e.message ?: "Unknown error during sync")
                Log.e("TabViewModel", "Sync failed: ${e.message}")
            }
        }
    }


    private fun initializeDefaultTab() {
        viewModelScope.launch {
            try {
                val count = repository.getTabCount().first()
                if (count == 0) {
                    Log.d("TabViewModel", "Initializing with default tab...")
                    val newTabId = createTab()
                    switchToTab(newTabId)
                    _isInitialized.value = true
                }
            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to initialize browser: ${e.message}")
                _error.value = "Failed to initialize browser: ${e.message}"
            }
        }
    }

    suspend fun createTab(url: String = "", title: String = "New Tab"): Long {
        return try {
            // Get user ID if signed in
            val isSignedIn = userDataStore.isSignedIn.first()
            val userId = if (isSignedIn) userDataStore.userId.first() else ""

            // Deactivate existing tabs
            tabDao.deactivateAllTabs()

            // Only set PENDING_UPLOAD if it's not a loading title
            val syncStatus = if (title != "Loading..." && url.isNotBlank()) {
                SyncStatus.PENDING_UPLOAD
            } else {
                SyncStatus.SYNCED
            }

            val tab = TabEntity(
                url = url,
                title = title,
                position = tabCount.value,
                isActive = true,
                userId = userId,
                syncStatus = syncStatus
            )

            val tabId = tabDao.insertTab(tab)
            Log.d("TabViewModel", "Created new tab with ID: $tabId, URL: $url, Title: $title")

            // Only trigger sync if not a loading tab
            if (isSignedIn && title != "Loading...") {
                triggerTabSync()
            }

            tabId
        } catch (e: Exception) {
            Log.e("TabViewModel", "Failed to create tab: ${e.message}", e)
            throw e
        }
    }

    fun switchToTab(tabId: Long) {
        viewModelScope.launch {
            try {
                // Get the current tab
                val tab = repository.getTabById(tabId)
                if (tab != null) {
                    // Update last visited time
                    repository.updateTab(tab.copy(lastVisited = Date()))
                    // Activate this tab
                    repository.switchToTab(tabId)
                    Log.d("TabViewModel", "Switched to tab: $tabId, updated lastVisited timestamp")
                }
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
                    val syncStatus = if (title != "Loading...") {
                        SyncStatus.PENDING_UPLOAD
                    } else {
                        tab.syncStatus
                    }

                    repository.updateTab(
                        tab.copy(
                            url = url,
                            title = title,
                            lastVisited = Date(),
                            syncStatus = syncStatus
                        )
                    )
                    Log.d("TabViewModel", "Updated tab content - URL: $url, Title: $title, SyncStatus: $syncStatus")
                }
            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to update tab content: ${e.message}")
            }
        }
    }

    fun closeTab(tab: TabEntity) {
        viewModelScope.launch {
            try {
                Log.d("TabViewModel", "Closing tab: ${tab.id}, URL: ${tab.url}")

                // Check if user is signed in
                val isSignedIn = userDataStore.isSignedIn.first()

                if (isSignedIn) {
                    val accessToken = userDataStore.accessToken.first()
                    val deviceId = userDataStore.deviceId.first().ifEmpty { "android-device" }

                    // Delete with server sync
                    repository.deleteTabImmediate(
                        tab = tab,
                        isUserSignedIn = isSignedIn,
                        accessToken = accessToken,
                        deviceId = deviceId,
                        userId = userDataStore.userId.first()
                    )

                    // Trigger sync to clean up
                    // Don't pull tabs right after deletion to avoid re adding
                    // Just push the deletion to server
                    viewModelScope.launch {
                        try {
                            val userId = userDataStore.userId.first()
                            userSyncManager.pushLocalTabChanges(accessToken, deviceId, userId)
                            Log.d("TabViewModel", "Deletion sync complete for tab ${tab.id}")
                        } catch (e: Exception) {
                            Log.e("TabViewModel", "Error during tab deletion sync: ${e.message}")
                        }
                    }
                } else {
                    // Just delete locally for non signed in users
                    repository.deleteTab(tab)
                    Log.d("TabViewModel", "Deleted local tab (anonymous user): ${tab.url}")
                }

                // Retrieve the current policy
                val policy = currentTabPolicy.value

                // For MANUAL policy, we already deleted the tab above, so nothing to do
                // For other policies, mark the tab as closed instead of deleting
                if (policy != "MANUAL") {
                    repository.markTabAsClosed(tab.id, Date())
                    Log.d("TabViewModel", "Marked tab ${tab.id} as closed according to policy: $policy")
                }

                // If no open tabs remain, create a new one
                if (tabCount.value <= 1) { // <= 1 because we're in the process of closing one tab
                    Log.d("TabViewModel", "Creating new tab as we're closing the last one")
                    createTab()
                }
            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to close tab: ${e.message}", e)
                _error.value = "Failed to close tab: ${e.message}"
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
                switchToTab(newTabId)
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

    /**
     * Updates the thumbnail for the given tab by capturing the visible portion of the GeckoView.
     * This version offloads file I/O and repository updates to the IO dispatcher.
     */
    fun updateTabThumbnail(tabId: Long, view: View) {
        Log.d("TabViewModel", "updateTabThumbnail called for tabId: $tabId with view: $view")
        viewModelScope.launch {
            if (view is GeckoView) {
                try {
                    val activeTabId = activeTab.value?.id
                    if (activeTabId != tabId) {
                        Log.w("TabViewModel", "Skipping thumbnail capture - active tab ($activeTabId) doesn't match requested tab ($tabId)")
                        return@launch
                    }

                    // Delay to ensure content is fully rendered.
                    delay(3000)

                    val result: GeckoResult<Bitmap> = view.capturePixels()
                    result.accept { bitmap: Bitmap? ->
                        if (bitmap != null) {
                            viewModelScope.launch(Dispatchers.IO) {
                                val thumbnailFile = File(context.cacheDir, "thumbnail_$tabId.png")
                                val thumbnailPath = ThumbnailUtil.saveBitmapToFile(bitmap, thumbnailFile)
                                if (thumbnailPath != null) {
                                    repository.getTabById(tabId)?.let { tab ->
                                        repository.updateTab(tab.copy(thumbnail = thumbnailPath))
                                        Log.d("TabViewModel", "Updated thumbnail for tab $tabId")

                                        // Sync thumbnail to bookmark if available.
                                        val bookmark = bookmarkRepository.getBookmarkByUrl(tab.url)
                                        if (bookmark != null && thumbnailFile.exists()) {
                                            val bookmarkDir = File(context.cacheDir, "bookmark_thumbnails")
                                            if (!bookmarkDir.exists()) bookmarkDir.mkdirs()
                                            val bookmarkFile = File(bookmarkDir, "bookmark_${bookmark.id}.png")
                                            thumbnailFile.copyTo(bookmarkFile, overwrite = true)
                                            bookmarkRepository.updateBookmark(
                                                bookmark.copy(favicon = "file://${bookmarkFile.absolutePath}")
                                            )
                                            Log.d("TabViewModel", "Synced thumbnail to bookmark #${bookmark.id}")
                                        }
                                    } ?: Log.e("TabViewModel", "No tab found with ID: $tabId")
                                } else {
                                    Log.e("TabViewModel", "Failed to save captured thumbnail for tab $tabId")
                                }
                            }
                        } else {
                            Log.e("TabViewModel", "capturePixels returned null for tab $tabId")
                            captureUsingFallbackMethod(tabId, view)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TabViewModel", "Error capturing pixels from GeckoView", e)
                    captureUsingFallbackMethod(tabId, view)
                }
            } else {
                Log.d("TabViewModel", "View is not a GeckoView, using fallback capture method")
                captureUsingFallbackMethod(tabId, view)
            }
        }
    }

    /**
     * Fallback capture method that offloads thumbnail capture and file I/O to the IO dispatcher.
     */
    private fun captureUsingFallbackMethod(tabId: Long, view: View) {
        viewModelScope.launch {
            try {
                val bitmap = ThumbnailUtil.captureThumbnail(view)
                if (bitmap != null) {
                    Log.d("TabViewModel", "Fallback capture returned bitmap of size ${bitmap.width}x${bitmap.height}")
                    viewModelScope.launch(Dispatchers.IO) {
                        val thumbnailFile = File(context.cacheDir, "thumbnail_$tabId.png")
                        val thumbnailPath = ThumbnailUtil.saveBitmapToFile(bitmap, thumbnailFile)
                        if (thumbnailPath != null) {
                            repository.getTabById(tabId)?.let { tab ->
                                repository.updateTab(tab.copy(thumbnail = thumbnailPath))
                                Log.d("TabViewModel", "Updated thumbnail for tab $tabId via fallback capture")
                            } ?: Log.e("TabViewModel", "No tab found with ID: $tabId in fallback")
                        } else {
                            Log.e("TabViewModel", "Fallback: Failed to save thumbnail for tab $tabId")
                        }
                    }
                } else {
                    Log.e("TabViewModel", "Fallback: Failed to capture thumbnail for tab $tabId")
                }
            } catch (e: Exception) {
                Log.e("TabViewModel", "Error in fallback capture method", e)
            }
        }
    }

    /**
     * Updates the thumbnail from a provided bitmap (for example, after a scroll capture).
     * Offloads file operations to IO.
     */
    fun updateTabThumbnailFromBitmap(tabId: Long, bitmap: Bitmap) {
        Log.d("TabViewModel", "updateTabThumbnailFromBitmap called for tabId: $tabId")
        viewModelScope.launch(Dispatchers.IO) {
            val thumbnailFile = File(context.cacheDir, "thumbnail_$tabId.png")
            val thumbnailPath = ThumbnailUtil.saveBitmapToFile(bitmap, thumbnailFile)
            if (thumbnailPath != null) {
                repository.getTabById(tabId)?.let { tab ->
                    repository.updateTab(tab.copy(thumbnail = thumbnailPath))
                    Log.d("TabViewModel", "Updated thumbnail for tab $tabId via scroll capture")
                } ?: Log.e("TabViewModel", "No tab found with ID: $tabId for scroll capture")
            } else {
                Log.e("TabViewModel", "Failed to save thumbnail for tab $tabId via scroll capture")
            }
        }
    }

    fun debugThumbnails() {
        viewModelScope.launch {
            val allTabs = tabs.value
            Log.d("TabViewModel", "Debugging thumbnails for ${allTabs.size} tabs")
            allTabs.forEach { tab ->
                Log.d("TabViewModel", "Tab ${tab.id}: URL=${tab.url}, Title=${tab.title}, Thumbnail=${tab.thumbnail}")
                if (!tab.thumbnail.isNullOrEmpty()) {
                    val file = File(tab.thumbnail!!)
                    Log.d("TabViewModel", "  Thumbnail file exists: ${file.exists()}, size: ${file.length()} bytes")
                    if (!file.exists() || file.length() == 0L) {
                        Log.w("TabViewModel", "  ⚠️ Thumbnail file is missing or empty!")
                    }
                } else {
                    Log.d("TabViewModel", "  No thumbnail path set for this tab")
                }
            }
        }
    }
}
