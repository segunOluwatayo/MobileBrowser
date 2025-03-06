package com.example.mobilebrowser.data.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.FrameLayout
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
 * Service for managing bookmark thumbnails.
 *
 * This version wraps all UI-related operations in withContext(Dispatchers.Main)
 * so that the GeckoSession and GeckoView work on the main thread.
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
     * Generates a thumbnail for a bookmark using GeckoView.
     *
     * All operations that involve view creation or manipulation are executed on the main thread.
     */
    fun generateThumbnailForBookmark(bookmark: BookmarkEntity) {
        scope.launch {
            try {
                Log.d(TAG, "Generating thumbnail for: ${bookmark.url}")

                // Run all UI related work on the main thread.
                val bitmap = withContext(Dispatchers.Main) {
                    // Create and open the GeckoSession on the main thread.
                    val runtime = GeckoRuntime.getDefault(context)
                    val session = GeckoSession()
                    session.open(runtime)

                    // Wait for page load (15 second timeout).
                    withTimeout(15000) {
                        waitForPageLoad(session, bookmark.url)
                    }

                    // Create a temporary container and GeckoView with fixed dimensions.
                    val frameLayout = FrameLayout(context).apply {
                        layoutParams = FrameLayout.LayoutParams(1080, 1920)
                    }
                    val geckoView = GeckoView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(1080, 1920)
                        setSession(session)
                    }
                    frameLayout.addView(geckoView)

                    // Force a measure and layout pass so the view gets valid dimensions.
                    val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
                    val heightSpec = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                    frameLayout.measure(widthSpec, heightSpec)
                    frameLayout.layout(0, 0, frameLayout.measuredWidth, frameLayout.measuredHeight)

                    // Allow a brief delay for the view to render.
                    delay(3000)

                    // Capture the thumbnail using your existing utility.
                    val capturedBitmap = ThumbnailUtil.captureThumbnail(geckoView)
                    if (capturedBitmap != null && capturedBitmap.width > 0 && capturedBitmap.height > 0) {
                        Log.d(TAG, "Captured bitmap dimensions: ${capturedBitmap.width}x${capturedBitmap.height}")
                    } else {
                        Log.e(TAG, "Bitmap capture failed or bitmap is empty!")
                    }

                    // Close the session on the main thread.
                    session.close()
                    capturedBitmap
                }

                if (bitmap != null) {
                    saveThumbnailForBookmark(bookmark, bitmap)
                } else {
                    // Fallback to a favicon URL if capturing fails.
                    val faviconUrl = getFaviconForDomain(bookmark.url)
                    updateBookmarkWithFavicon(bookmark, faviconUrl)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating thumbnail: ${e.message}")
            }
        }
    }

    /**
     * Waits for the page to load using GeckoSession’s progress delegate.
     */
    private suspend fun waitForPageLoad(session: GeckoSession, url: String) =
        suspendCancellableCoroutine<Unit> { continuation ->
            var isResumed = false

            val progressDelegate = object : GeckoSession.ProgressDelegate {
                override fun onPageStop(session: GeckoSession, success: Boolean) {
                    if (!isResumed) {
                        isResumed = true
                        if (success) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(Exception("Page load failed"))
                        }
                    }
                }

                override fun onProgressChange(session: GeckoSession, progress: Int) {
                    Log.d(TAG, "Loading progress: $progress%")
                }
            }

            session.progressDelegate = progressDelegate
            session.loadUri(url)

            continuation.invokeOnCancellation {
                session.progressDelegate = null
            }
        }

    /**
     * Saves the captured bitmap as a PNG file and updates the bookmark.
     */
    private fun saveThumbnailForBookmark(bookmark: BookmarkEntity, bitmap: Bitmap) {
        scope.launch {
            try {
                // Create a unique file name based on the bookmark ID.
                val thumbnailFile = File(thumbnailFolder, "bookmark_${bookmark.id}.png")
                FileOutputStream(thumbnailFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
                Log.d(TAG, "Thumbnail saved at: ${thumbnailFile.absolutePath}")

                // Update the bookmark with the thumbnail file path.
                val updatedBookmark = bookmark.copy(favicon = "file://${thumbnailFile.absolutePath}")
                bookmarkRepository.updateBookmark(updatedBookmark)
                Log.d(TAG, "Bookmark updated with thumbnail path")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving thumbnail: ${e.message}")
            }
        }
    }

    /**
     * Updates the bookmark with a favicon URL.
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
     * Returns the local thumbnail path for a bookmark if it exists.
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
     * Attempts to get a favicon URL based on the bookmark's domain.
     */
    fun getFaviconForDomain(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host ?: return null
            // Try a common favicon location.
            "https://$host/favicon.ico"
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing URL for favicon: ${e.message}")
            null
        }
    }
}
