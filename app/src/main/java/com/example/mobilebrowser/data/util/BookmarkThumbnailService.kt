package com.example.mobilebrowser.data.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.mobilebrowser.data.entity.BookmarkEntity
import com.example.mobilebrowser.data.repository.BookmarkRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Service for managing bookmark thumbnails
 */
@Singleton
class BookmarkThumbnailService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookmarkRepository: BookmarkRepository
) {
    private val TAG = "BookmarkThumbnailService"
    private val thumbnailFolder by lazy { File(context.cacheDir, "bookmark_thumbnails") }
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Ensure the thumbnail directory exists
        thumbnailFolder.mkdirs()
    }

    /**
     * Generates a thumbnail for a bookmark using GeckoView
     */
    fun generateThumbnailForBookmark(bookmark: BookmarkEntity) {
        scope.launch {
            try {
                Log.d(TAG, "Generating thumbnail for: ${bookmark.url}")

                val runtime = GeckoRuntime.getDefault(context)
                val session = GeckoSession()
                session.open(runtime)

                try {
                    // Use a suspend function to wait for page load
                    withTimeout(15000) { // 15 second timeout
                        waitForPageLoad(session, bookmark.url)
                    }

                    // Use GeckoView for actual rendering
                    val geckoView = GeckoView(context)
                    geckoView.setSession(session)

                    // Wait a moment for rendering
                    kotlinx.coroutines.delay(1000)

                    // Use our existing ThumbnailUtil to capture the view
                    val bitmap = ThumbnailUtil.captureThumbnail(geckoView)
                    if (bitmap != null) {
                        saveThumbnailForBookmark(bookmark, bitmap)
                    } else {
                        // Fallback to favicon
                        val faviconUrl = getFaviconForDomain(bookmark.url)
                        updateBookmarkWithFavicon(bookmark, faviconUrl)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during thumbnail capture: ${e.message}")
                    // Fallback to favicon
                    val faviconUrl = getFaviconForDomain(bookmark.url)
                    updateBookmarkWithFavicon(bookmark, faviconUrl)
                } finally {
                    // Always close the session
                    session.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating thumbnail: ${e.message}")
            }
        }
    }

    /**
     * Wait for a page to load using coroutines
     */
    private suspend fun waitForPageLoad(session: GeckoSession, url: String) = suspendCancellableCoroutine { continuation ->
        var isCompleted = false

        val progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                if (!isCompleted) {
                    isCompleted = true
                    if (success) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resumeWithException(Exception("Page load failed"))
                    }
                }
            }
        }

        session.progressDelegate = progressDelegate
        session.loadUri(url)

        // Ensure we clean up if coroutine is cancelled
        continuation.invokeOnCancellation {
            session.progressDelegate = null
        }
    }

    /**
     * Saves a bitmap as a thumbnail for a bookmark
     */
    private fun saveThumbnailForBookmark(bookmark: BookmarkEntity, bitmap: Bitmap) {
        scope.launch {
            try {
                // Create a unique filename based on the bookmark ID
                val thumbnailFile = File(thumbnailFolder, "bookmark_${bookmark.id}.png")

                // Save the bitmap to file
                FileOutputStream(thumbnailFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                }

                Log.d(TAG, "Thumbnail saved at: ${thumbnailFile.absolutePath}")

                // Update the bookmark with the file path
                val updatedBookmark = bookmark.copy(
                    favicon = "file://${thumbnailFile.absolutePath}"
                )
                bookmarkRepository.updateBookmark(updatedBookmark)

                Log.d(TAG, "Bookmark updated with thumbnail path")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving thumbnail: ${e.message}")
            }
        }
    }

    /**
     * Update bookmark with favicon URL
     */
    private fun updateBookmarkWithFavicon(bookmark: BookmarkEntity, faviconUrl: String?) {
        if (faviconUrl != null) {
            scope.launch {
                val updatedBookmark = bookmark.copy(favicon = faviconUrl)
                bookmarkRepository.updateBookmark(updatedBookmark)
            }
        }
    }

    /**
     * Gets the thumbnail path for a bookmark if it exists
     */
    fun getThumbnailForBookmark(bookmarkId: Long): String? {
        val thumbnailFile = File(thumbnailFolder, "bookmark_${bookmarkId}.png")
        return if (thumbnailFile.exists()) {
            "file://${thumbnailFile.absolutePath}"
        } else {
            null
        }
    }

    /**
     * Get a thumbnail from a domain favicon
     */
    fun getFaviconForDomain(url: String): String? {
        try {
            val uri = Uri.parse(url)
            val host = uri.host ?: return null
            // Try common favicon locations
            return "https://$host/favicon.ico"
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing URL for favicon: ${e.message}")
            return null
        }
    }
}