package com.example.mobilebrowser.ui.util

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Maintains the drag and drop state for tab reordering
 */
class DragDropState(
    private val lazyListState: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit
) {
    private var draggedDistance by mutableStateOf(0f)
    private var draggingItemInitialOffset by mutableStateOf(0)
    private var startIndex: Int? by mutableStateOf(null)
    private var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)
    private var overScrollJob by mutableStateOf<Job?>(null)

    // Calculated total offset for the dragged item
    val draggingItemOffset: Float
        get() = draggingItemInitialOffset + draggedDistance

    // Whether an item is currently being dragged
    val isDragging: Boolean
        get() = currentIndexOfDraggedItem != null

    /**
     * Start dragging an item
     */
    fun onDragStart(offset: Int) {
        draggingItemInitialOffset = offset
        startIndex = currentIndexOfDraggedItem
    }

    /**
     * Update the dragging state
     */
    fun onDrag(offset: Float) {
        draggedDistance += offset
        val currentIndex = currentIndexOfDraggedItem ?: return
        val targetIndex = getTargetIndex(currentIndex, draggedDistance)

        if (targetIndex != currentIndex) {
            currentIndexOfDraggedItem = targetIndex
            onMove(currentIndex, targetIndex)
        }

        // Handle auto-scrolling when near edges
        when {
            draggedDistance < 0 -> autoScroll(true)
            draggedDistance > 0 -> autoScroll(false)
            else -> overScrollJob?.cancel()
        }
    }

    /**
     * End the dragging operation
     */
    fun onDragEnd() {
        draggedDistance = 0f
        currentIndexOfDraggedItem = null
        startIndex = null
        overScrollJob?.cancel()
    }

    /**
     * Calculate the target index based on drag distance
     */
    private fun getTargetIndex(currentIndex: Int, dragDistance: Float): Int {
        val itemSize = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: return currentIndex
        val targetIndex = currentIndex + (dragDistance / itemSize).toInt()
        return targetIndex.coerceIn(0, lazyListState.layoutInfo.totalItemsCount - 1)
    }

    /**
     * Handle auto-scrolling when dragging near list edges
     */
    private fun autoScroll(up: Boolean) {
        if (overScrollJob?.isActive == true) return

        overScrollJob = scope.launch {
            while (true) {
                lazyListState.scrollBy(if (up) -10f else 10f)
                kotlinx.coroutines.delay(10)
            }
        }
    }

    companion object {
        /**
         * Create a saver for the drag state
         */
        fun Saver(): Saver<DragDropState, *> = listSaver(
            save = { listOf(it.draggedDistance, it.draggingItemInitialOffset) },
            restore = { DragDropState(LazyListState(), CoroutineScope(kotlinx.coroutines.Dispatchers.Main)) { _, _ -> } }
        )
    }
}

/**
 * Remember the drag drop state with save functionality
 */
@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit
): DragDropState {
    val scope = rememberCoroutineScope()
    return rememberSaveable(
        lazyListState,
        saver = DragDropState.Saver()
    ) {
        DragDropState(
            lazyListState = lazyListState,
            scope = scope,
            onMove = onMove
        )
    }
}