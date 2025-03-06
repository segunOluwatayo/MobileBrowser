package com.example.mobilebrowser.data.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.View
import com.example.mobilebrowser.data.entity.BookmarkEntity
import com.example.mobilebrowser.data.repository.BookmarkRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

                // Perform GeckoView and GeckoSession operations on the main thread
                val (session, geckoView) = withContext(Dispatchers.Main) {
                    val runtime = GeckoRuntime.getDefault(context)
                    val session = GeckoSession().apply {
                        open(runtime)
                    }
                    val geckoView = GeckoView(context).apply {
                        setSession(session)
                        // Manually measure and layout the view
                        val desiredWidth = 800
                        val desiredHeight = 600
                        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(desiredWidth, View.MeasureSpec.EXACTLY)
                        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(desiredHeight, View.MeasureSpec.EXACTLY)
                        measure(widthMeasureSpec, heightMeasureSpec)
                        layout(0, 0, measuredWidth, measuredHeight)
                    }
                    Pair(session, geckoView)
                }

                // Wait for page load on the main thread
                withTimeout(15000) {
                    withContext(Dispatchers.Main) {
                        waitForPageLoad(session, bookmark.url)
                    }
                }

                // Allow time for rendering after layout
                kotlinx.coroutines.delay(1000)

                // Capture the thumbnail; capture may need to be on main thread as well
                val bitmap = withContext(Dispatchers.Main) {
                    ThumbnailUtil.captureThumbnail(geckoView)
                }

                if (bitmap != null) {
                    saveThumbnailForBookmark(bookmark, bitmap)
                } else {
                    // Fallback to favicon if capture fails
                    val faviconUrl = getFaviconForDomain(bookmark.url)
                    updateBookmarkWithFavicon(bookmark, faviconUrl)
                }

                // Close the session on the main thread
                withContext(Dispatchers.Main) {
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