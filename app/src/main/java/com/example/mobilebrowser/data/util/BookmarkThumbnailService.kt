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
import kotlinx.coroutines.delay
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
     * Generates a thumbnail for a bookmark using GeckoView and capturePixels().
     */
    fun generateThumbnailForBookmark(bookmark: BookmarkEntity) {
        scope.launch {
            try {
                Log.d(TAG, "Generating thumbnail for: ${bookmark.url}")

                // Create GeckoSession and GeckoView on the main thread
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
                        Log.d(TAG, "GeckoView measured and laid out with dimensions: $measuredWidth x $measuredHeight")
                    }
                    Pair(session, geckoView)
                }

                // Wait for page load (with timeout)
                withTimeout(15000) {
                    withContext(Dispatchers.Main) {
                        waitForPageLoad(session, bookmark.url)
                    }
                }
                Log.d(TAG, "Page load completed, waiting 1 second for rendering")
                delay(1000)

                // Capture the thumbnail using GeckoView's capturePixels()
                val bitmap = withContext(Dispatchers.Main) {
                    suspendCancellableCoroutine<Bitmap?> { continuation ->
                        Log.d(TAG, "Attempting to capture thumbnail using capturePixels()")
                        val result = geckoView.capturePixels()
                        result.accept { capturedBitmap ->
                            if (capturedBitmap != null) {
                                Log.d(TAG, "capturePixels() returned bitmap of size ${capturedBitmap.width}x${capturedBitmap.height}")
                            } else {
                                Log.e(TAG, "capturePixels() returned null")
                            }
                            continuation.resume(capturedBitmap)
                        }
                    }
                }

                if (bitmap != null) {
                    saveThumbnailForBookmark(bookmark, bitmap)
                } else {
                    Log.e(TAG, "Bitmap capture failed, falling back to favicon")
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
     * Wait for a page to load using coroutines.
     */
    private suspend fun waitForPageLoad(session: GeckoSession, url: String) = suspendCancellableCoroutine<Unit> { continuation ->
        var isCompleted = false

        val progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                if (!isCompleted) {
                    isCompleted = true
                    if (success) {
                        Log.d(TAG, "Page loaded successfully for URL: $url")
                        continuation.resume(Unit)
                    } else {
                        Log.e(TAG, "Page load failed for URL: $url")
                        continuation.resumeWithException(Exception("Page load failed"))
                    }
                }
            }
        }

        session.progressDelegate = progressDelegate
        Log.d(TAG, "Loading URL: $url")
        session.loadUri(url)

        // Clean up if coroutine is cancelled
        continuation.invokeOnCancellation {
            session.progressDelegate = null
        }
    }

    /**
     * Saves a bitmap as a thumbnail for a bookmark.
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
     * Update bookmark with favicon URL.
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
     * Gets the thumbnail path for a bookmark if it exists.
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
     * Get a favicon URL from the domain.
     */
    fun getFaviconForDomain(url: String): String? {
        try {
            val uri = Uri.parse(url)
            val host = uri.host ?: return null
            // Try common favicon location
            return "https://$host/favicon.ico"
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing URL for favicon: ${e.message}")
            return null
        }
    }
}
