package com.example.mobilebrowser.data.repository

import com.example.mobilebrowser.data.dao.ShortcutDao
import com.example.mobilebrowser.data.entity.ShortcutEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * It communicates with the ShortcutDao to perform CRUD operations.
 *
 * @property shortcutDao The data access object for shortcut operations.
 */
@Singleton
class ShortcutRepository @Inject constructor(
    private val shortcutDao: ShortcutDao
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
}
