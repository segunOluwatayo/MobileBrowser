package com.example.mobilebrowser.data.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.view.View
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThumbnailUtil {
    private const val TAG = "ThumbnailUtil"

    /**
     * Captures a screenshot of the provided view as a Bitmap.
     * This method now runs on a background thread.
     */
    suspend fun captureThumbnail(view: View): Bitmap? = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Capturing thumbnail for view: ${view::class.java.simpleName}, width=${view.width}, height=${view.height}")
            if (view.width <= 0 || view.height <= 0) {
                Log.e(TAG, "Cannot capture thumbnail: View has invalid dimensions")
                return@withContext null
            }
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            Log.d(TAG, "Thumbnail captured successfully: ${bitmap.width}x${bitmap.height}")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing thumbnail", e)
            null
        }
    }

    /**
     * Saves the given bitmap as a PNG file.
     * This method now runs on the IO dispatcher.
     * Returns the absolute path of the saved file.
     */
    suspend fun saveBitmapToFile(bitmap: Bitmap, file: File): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving bitmap to file: ${file.absolutePath}")
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
            }
            Log.d(TAG, "Bitmap saved successfully, file exists: ${file.exists()}, size: ${file.length()} bytes")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to file: ${file.absolutePath}", e)
            null
        }
    }

    /**
     * Verifies if the thumbnail file exists and is valid.
     * Runs on the IO dispatcher.
     */
    suspend fun verifyThumbnailFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        val exists = file.exists() && file.length() > 0
        Log.d(TAG, "Verifying thumbnail file: $filePath, exists: $exists, size: ${if (exists) file.length() else 0} bytes")
        exists
    }
}
