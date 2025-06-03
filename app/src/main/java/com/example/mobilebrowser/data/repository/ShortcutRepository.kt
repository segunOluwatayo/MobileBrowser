package com.example.mobilebrowser.data.repository

import android.net.Uri
import com.example.mobilebrowser.R
import com.example.mobilebrowser.data.dao.ShortcutDao
import com.example.mobilebrowser.data.entity.ShortcutEntity
import com.example.mobilebrowser.data.entity.ShortcutType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first


/**
 *
 * It communicates with the ShortcutDao to perform CRUD operations.
 *
 * @property shortcutDao The data access object for shortcut operations.
 */
@Singleton
class ShortcutRepository @Inject constructor(
    private val shortcutDao: ShortcutDao,
) {

    /**
     * Returns a flow of all shortcuts, ordered by pinned status and timestamp.
     */
    fun getAllShortcuts(): Flow<List<ShortcutEntity>> {
        return shortcutDao.getAllShortcuts()
    }

    /**
     * Returns a flow of only the pinned shortcuts.
     */
    fun getPinnedShortcuts(): Flow<List<ShortcutEntity>> {
        return shortcutDao.getPinnedShortcuts()
    }

    /**
     * Inserts a new shortcut into the database.
     *
     * @param shortcut The shortcut entity to insert.
     */
    suspend fun insertShortcut(shortcut: ShortcutEntity) {
        shortcutDao.insertShortcut(shortcut)
    }

    /**
     * Updates an existing shortcut in the database.
     *
     * @param shortcut The shortcut entity with updated values.
     */
    suspend fun updateShortcut(shortcut: ShortcutEntity) {
        shortcutDao.updateShortcut(shortcut)
    }

    /**
     * Deletes a shortcut from the database.
     *
     * @param shortcut The shortcut entity to delete.
     */
    suspend fun deleteShortcut(shortcut: ShortcutEntity) {
        shortcutDao.deleteShortcut(shortcut)
    }

    /**
     * Gets dynamic shortcuts only
     */
    fun getDynamicShortcuts(): Flow<List<ShortcutEntity>> {
        return shortcutDao.getDynamicShortcuts()
    }

    /**
     * Updates a shortcut's visit count
     */
    suspend fun incrementShortcutVisit(url: String) {
        shortcutDao.incrementVisitCount(url)
    }

    /**
     * Updates dynamic shortcuts based on history.
     */
    suspend fun updateDynamicShortcuts(historyRepository: HistoryRepository, maxDynamicShortcuts: Int = 8) {
        // Get top visited sites from history
        val topSites = historyRepository.getAllHistory().first()
            .sortedByDescending { it.visitCount }
            .take(maxDynamicShortcuts * 2)

        // Get current shortcuts
        val currentShortcuts = getAllShortcuts().first()
        val pinnedUrls = currentShortcuts.filter { it.isPinned }.map { it.url }

        // Filter candidate URLs that are not already pinned
        val candidateUrls = topSites.filter { historyEntry ->
            !pinnedUrls.contains(historyEntry.url)
        }.take(maxDynamicShortcuts)

        // Delete existing dynamic shortcuts
        currentShortcuts
            .filter { it.shortcutType == ShortcutType.DYNAMIC && !it.isPinned }
            .forEach { deleteShortcut(it) }

        // Insert new dynamic shortcuts based on candidate URLs
        candidateUrls.forEach { historyEntry ->
            val iconRes = getIconResForUrl(historyEntry.url)
            val shortcut = ShortcutEntity(
                label = historyEntry.title.takeIf { it.isNotBlank() }
                    ?: extractDomainFromUrl(historyEntry.url),
                url = historyEntry.url,
                iconRes = iconRes,
                isPinned = false,
                shortcutType = ShortcutType.DYNAMIC,
                visitCount = historyEntry.visitCount,
                lastVisited = historyEntry.lastVisited.time
            )
            insertShortcut(shortcut)
        }
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
}
