package com.example.mobilebrowser.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.data.entity.ShortcutEntity
import com.example.mobilebrowser.data.repository.ShortcutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ShortcutViewModel is responsible for handling UI-related data for the homepage shortcuts.
 * It exposes a reactive state (StateFlow) for the list of shortcuts and provides methods to
 * interact with the ShortcutRepository for CRUD operations.
 */
@HiltViewModel
class ShortcutViewModel @Inject constructor(
    private val repository: ShortcutRepository
) : ViewModel() {

    // MutableStateFlow to hold the list of shortcuts, initially empty.
    private val _shortcuts = MutableStateFlow<List<ShortcutEntity>>(emptyList())

    // Public read-only state flow for UI components to observe.
    val shortcuts: StateFlow<List<ShortcutEntity>> = _shortcuts

    init {
        // Observe changes from the repository and update the state flow.
        viewModelScope.launch {
            repository.getAllShortcuts().collectLatest { shortcutList ->
                _shortcuts.value = shortcutList
            }
        }
    }

    /**
     * Inserts a new shortcut into the database.
     *
     * @param shortcut The shortcut entity to insert.
     */
    fun insertShortcut(shortcut: ShortcutEntity) {
        viewModelScope.launch {
            repository.insertShortcut(shortcut)
        }
    }

    /**
     * Updates an existing shortcut in the database.
     *
     * @param shortcut The shortcut entity with updated values.
     */
    fun updateShortcut(shortcut: ShortcutEntity) {
        viewModelScope.launch {
            repository.updateShortcut(shortcut)
        }
    }

    /**
     * Deletes a shortcut from the database.
     *
     * @param shortcut The shortcut entity to delete.
     */
    fun deleteShortcut(shortcut: ShortcutEntity) {
        viewModelScope.launch {
            repository.deleteShortcut(shortcut)
        }
    }

    /**
     * Handles a shortcut tap event.
     * Here, you might navigate to the URL associated with the shortcut.
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
}
