package com.example.mobilebrowser.data.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import com.example.mobilebrowser.data.entity.BookmarkEntity
import com.example.mobilebrowser.data.repository.BookmarkRepository

@Singleton
class BookmarkThumbnailService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookmarkRepository: BookmarkRepository
) {
    private val TAG = "BookmarkThumbnailService"
    private val scope = CoroutineScope(Dispatchers.IO)

    // Where we store the bookmark thumbnails
    private val thumbnailFolder by lazy {
        File(context.cacheDir, "bookmark_thumbnails").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Save a captured [bitmap] for the given [bookmark].
     * This does not do any offscreen rendering—just write the bitmap to a file and update the DB.
     */
    fun saveThumbnailForBookmark(bookmark: BookmarkEntity, bitmap: Bitmap) {
        scope.launch {
            try {
                // Save to e.g. /data/data/your.package/cache/bookmark_thumbnails/bookmark_{id}.png
                val thumbnailFile = File(thumbnailFolder, "bookmark_${bookmark.id}.png")
                FileOutputStream(thumbnailFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
                Log.d(TAG, "Thumbnail saved at: ${thumbnailFile.absolutePath}")

                // Update bookmark record with the local file:// path
                val updatedBookmark = bookmark.copy(favicon = "file://${thumbnailFile.absolutePath}")
                bookmarkRepository.updateBookmark(updatedBookmark)
                Log.d(TAG, "Bookmark updated with thumbnail path: ${updatedBookmark.favicon}")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving thumbnail: ${e.message}", e)
            }
        }
    }
}
