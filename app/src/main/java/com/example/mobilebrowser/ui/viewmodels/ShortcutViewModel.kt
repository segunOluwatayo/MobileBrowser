package com.example.mobilebrowser.ui.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.R
import com.example.mobilebrowser.data.entity.ShortcutEntity
import com.example.mobilebrowser.data.entity.ShortcutType
import com.example.mobilebrowser.data.repository.HistoryRepository
import com.example.mobilebrowser.data.repository.ShortcutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ShortcutViewModel is responsible for handling UI-related data for the homepage shortcuts.
 * It exposes a reactive state (StateFlow) for the list of shortcuts and provides methods to
 * interact with the ShortcutRepository for CRUD operations.
 */
@HiltViewModel
class ShortcutViewModel @Inject constructor(
    private val shortcutRepository: ShortcutRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _shortcuts = MutableStateFlow<List<ShortcutEntity>>(emptyList())
    val shortcuts: StateFlow<List<ShortcutEntity>> = _shortcuts

    val pinnedShortcuts = _shortcuts.map { shortcuts ->
        shortcuts.filter { it.isPinned }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dynamicShortcuts = _shortcuts.map { shortcuts ->
        shortcuts.filter { it.shortcutType == ShortcutType.DYNAMIC && !it.isPinned }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Number of dynamic shortcuts to maintain
    private val maxDynamicShortcuts = 8

    init {
        // Observe changes from the repository and update the state flow.
        viewModelScope.launch {
            val existingShortcuts = shortcutRepository.getAllShortcuts().first()
            if (existingShortcuts.isEmpty()) {
                insertShortcut(
                    ShortcutEntity(
                        label = "Google",
                        url = "https://www.google.com",
                        iconRes = R.drawable.google_icon,
                        isPinned = true
                    )
                )
                insertShortcut(
                    ShortcutEntity(
                        label = "Bing",
                        url = "https://www.bing.com",
                        iconRes = R.drawable.bing_icon,
                        isPinned = false
                    )
                )
                insertShortcut(
                    ShortcutEntity(
                        label = "DuckDuckGo",
                        url = "https://www.duckduckgo.com",
                        iconRes = R.drawable.duckduckgo_icon,
                        isPinned = false
                    )
                )
            }
            shortcutRepository.getAllShortcuts().collectLatest { shortcutList ->
                _shortcuts.value = shortcutList
            }

            // Schedule periodic dynamic shortcut updates
            setupDynamicShortcutUpdates()
        }

    }

    /**
     * Inserts a new shortcut into the database.
     *
     * @param shortcut The shortcut entity to insert.
     */
    fun insertShortcut(shortcut: ShortcutEntity) {
        viewModelScope.launch {
            shortcutRepository.insertShortcut(shortcut)
        }
    }

    // Function to setup periodic updates of dynamic shortcuts
    private fun setupDynamicShortcutUpdates() {
        viewModelScope.launch {
            while (true) {
                updateDynamicShortcuts()
                delay(3600000)
            }
        }
    }

    // Function to update dynamic shortcuts based on history
//    suspend fun updateDynamicShortcuts() {
//        try {
//            // Get top visited sites from history
//            val topSites = historyRepository.getAllHistory().first()
//                .sortedByDescending { it.visitCount }
//                .take(maxDynamicShortcuts * 2) // Get more than needed to filter against existing pinned
//
//            // Get current shortcuts
//            val currentShortcuts = shortcutRepository.getAllShortcuts().first()
//            val pinnedUrls = currentShortcuts.filter { it.isPinned }.map { it.url }
//
//            // Filter out URLs that are already pinned
//            val candidateUrls = topSites.filter { historyEntry ->
//                !pinnedUrls.contains(historyEntry.url)
//            }.take(maxDynamicShortcuts)
//
//            // Delete existing dynamic shortcuts
//            currentShortcuts
//                .filter { it.shortcutType == ShortcutType.DYNAMIC && !it.isPinned }
//                .forEach { shortcutRepository.deleteShortcut(it) }
//
//            // Add new dynamic shortcuts
//            candidateUrls.forEach { historyEntry ->
//                val favicon = historyEntry.favicon
//
//                // Determine icon resource based on domain or use default
//                val iconRes = getIconResForUrl(historyEntry.url)
//
//                val shortcut = ShortcutEntity(
//                    label = historyEntry.title.takeIf { it.isNotBlank() }
//                        ?: extractDomainFromUrl(historyEntry.url),
//                    url = historyEntry.url,
//                    iconRes = iconRes,
//                    isPinned = false,
//                    shortcutType = ShortcutType.DYNAMIC,
//                    visitCount = historyEntry.visitCount,
//                    lastVisited = historyEntry.lastVisited.time
//                )
//                shortcutRepository.insertShortcut(shortcut)
//            }
//        } catch (e: Exception) {
//            Log.e("ShortcutViewModel", "Error updating dynamic shortcuts", e)
//        }
//    }
    fun updateDynamicShortcuts() = viewModelScope.launch {
        shortcutRepository.updateDynamicShortcuts(historyRepository)
    }

    // Helper function to extract domain from URL
    private fun extractDomainFromUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            uri.host?.removePrefix("www.")?.substringBefore(".") ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    // Helper function to determine icon resource based on URL
    private fun getIconResForUrl(url: String): Int {
        return when {
            url.contains("google.com") -> R.drawable.google_icon
            url.contains("bing.com") -> R.drawable.bing_icon
            url.contains("duckduckgo.com") -> R.drawable.duckduckgo_icon
            url.contains("qwant.com") -> R.drawable.qwant_icon
            url.contains("wikipedia.org") -> R.drawable.wikipedia_icon
            url.contains("ebay.com") -> R.drawable.ebay_icon
            // Add more mappings as needed
            else -> R.drawable.generic_searchengine// Default icon
        }
    }

    // Update the toggle pin function
    fun togglePin(shortcut: ShortcutEntity) {
        viewModelScope.launch {
            // When pinning a dynamic shortcut, keep it as dynamic but mark as pinned
            shortcutRepository.updateShortcut(shortcut.copy(
                isPinned = !shortcut.isPinned,
                // Don't change shortcutType - keep as is
                timestamp = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Updates an existing shortcut in the database.
     *
     * @param shortcut The shortcut entity with updated values.
     */
    fun updateShortcut(shortcut: ShortcutEntity, newLabel: String? = null, newUrl: String? = null) {
        viewModelScope.launch {
            val updatedShortcut = shortcut.copy(
                label = newLabel ?: shortcut.label,
                url = newUrl ?: shortcut.url,
                timestamp = System.currentTimeMillis()
            )
            shortcutRepository.updateShortcut(updatedShortcut)
        }
    }

    /**
     * Deletes a shortcut from the database.
     *
     * @param shortcut The shortcut entity to delete.
     */
    fun deleteShortcut(shortcut: ShortcutEntity) {
        viewModelScope.launch {
            shortcutRepository.deleteShortcut(shortcut)
        }
    }

    /**
     * Handles a shortcut tap event.
     *
     * @param shortcut The shortcut that was tapped.
     */
    fun onShortcutClick(shortcut: ShortcutEntity) {
        // TODO: Implement navigation logic (e.g., open the URL in a new tab)
    }

    /**
     * Handles a shortcut long press event.
     * This is typically used to show a context menu for additional actions (e.g., edit, pin/unpin, delete).
     *
     * @param shortcut The shortcut that was long pressed.
     */
    fun onShortcutLongPress(shortcut: ShortcutEntity) {
        // TODO: Trigger the context menu or dialog for shortcut options.
        // This could include actions such as editing, pinning/unpinning, or deleting the shortcut.
    }

    /**
     * Toggles the "isPinned" status of a shortcut.
     *
     * @param shortcut The shortcut whose "isPinned" status should be toggled.
     */

    /**
     * Adds a new shortcut to the database.
     *
     *
     */
    fun addShortcut(label: String, url: String, iconRes: Int) {
        viewModelScope.launch {
            val shortcut = ShortcutEntity(
                label = label,
                url = url,
                iconRes = iconRes,
                isPinned = false,
                timestamp = System.currentTimeMillis()
            )
            shortcutRepository.insertShortcut(shortcut)
        }
    }

    /**
     * Record a visit to a URL that might be a shortcut
     * If the URL has been visited at least 4 times, it will be added as a dynamic shortcut
     */
    fun recordVisit(url: String) {
        viewModelScope.launch {
            try {
                try {
                    shortcutRepository.incrementShortcutVisit(url)
                } catch (e: Exception) {
                }

                // Check if this URL should be added as a dynamic shortcut
                val historyEntry = historyRepository.getHistoryByUrl(url)
                if (historyEntry != null && historyEntry.visitCount >= 4) {
                    // Get current shortcuts
                    val currentShortcuts = shortcutRepository.getAllShortcuts().first()

                    // Check if it's already a shortcut
                    val isAlreadyShortcut = currentShortcuts.any { it.url == url }

                    if (!isAlreadyShortcut) {
                        // Count existing dynamic shortcuts
                        val dynamicShortcuts = currentShortcuts.filter {
                            it.shortcutType == ShortcutType.DYNAMIC && !it.isPinned
                        }

                        val iconRes = getIconResForUrl(url)
                        val label = historyEntry.title.takeIf { it.isNotBlank() }
                            ?: extractDomainFromUrl(url)

                        val newShortcut = ShortcutEntity(
                            label = label,
                            url = url,
                            iconRes = iconRes,
                            isPinned = false,
                            shortcutType = ShortcutType.DYNAMIC,
                            visitCount = historyEntry.visitCount,
                            lastVisited = historyEntry.lastVisited.time,
                            favicon = historyEntry.favicon
                        )

                        if (dynamicShortcuts.size >= maxDynamicShortcuts) {
                            val leastVisited = dynamicShortcuts.minByOrNull { it.visitCount }

                            if (leastVisited != null && historyEntry.visitCount > leastVisited.visitCount) {
                                Log.d("ShortcutViewModel", "Replacing least visited shortcut: ${leastVisited.url} with: $url")
                                shortcutRepository.deleteShortcut(leastVisited)
                                shortcutRepository.insertShortcut(newShortcut)
                            }
                        } else {
                            Log.d("ShortcutViewModel", "Adding dynamic shortcut for: $url with visit count: ${historyEntry.visitCount}")
                            shortcutRepository.insertShortcut(newShortcut)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ShortcutViewModel", "Error recording visit", e)
            }
        }
    }

    fun restoreDefaultShortcuts() {
        viewModelScope.launch {
            val defaultShortcuts = listOf(
                Triple("Google", "https://www.google.com", R.drawable.google_icon),
                Triple("Bing", "https://www.bing.com", R.drawable.bing_icon),
                Triple("DuckDuckGo", "https://www.duckduckgo.com", R.drawable.duckduckgo_icon)
            )

            val currentShortcuts = shortcutRepository.getAllShortcuts().first()

            for ((label, url, iconRes) in defaultShortcuts) {
                // Check if the shortcut exists
                val existingShortcut = currentShortcuts.find { it.url == url }

                if (existingShortcut != null) {
                    // Shortcut exists but might be unpinned - update it to be pinned
                    if (!existingShortcut.isPinned) {
                        shortcutRepository.updateShortcut(existingShortcut.copy(
                            isPinned = true,
                            timestamp = System.currentTimeMillis()
                        ))
                        Log.d("ShortcutViewModel", "Repinned existing shortcut: $label")
                    }
                } else {
                    // Shortcut doesn't exist create a new one
                    val newShortcut = ShortcutEntity(
                        label = label,
                        url = url,
                        iconRes = iconRes,
                        isPinned = true,
                        shortcutType = ShortcutType.MANUAL,
                        timestamp = System.currentTimeMillis()
                    )
                    shortcutRepository.insertShortcut(newShortcut)
                    Log.d("ShortcutViewModel", "Created new default shortcut: $label")
                }
            }
        }
    }

}
