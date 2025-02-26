package com.example.mobilebrowser.data.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.view.View
import java.io.File
import java.io.FileOutputStream

object ThumbnailUtil {
    private const val TAG = "ThumbnailUtil"

    /**
     * Captures a screenshot of the provided view as a Bitmap.
     */
    fun captureThumbnail(view: View): Bitmap? {
        return try {
            Log.d(TAG, "Capturing thumbnail for view: width=${view.width}, height=${view.height}")
            if (view.width <= 0 || view.height <= 0) {
                Log.e(TAG, "Cannot capture thumbnail: View has invalid dimensions")
                return null
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
     * Saves the given bitmap as a PNG file in the provided directory.
     * Returns the absolute path of the saved file.
     */
    fun saveBitmapToFile(bitmap: Bitmap, file: File): String? {
        return try {
            Log.d(TAG, "Saving bitmap to file: ${file.absolutePath}")
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d(TAG, "Bitmap saved successfully, file exists: ${file.exists()}, size: ${file.length()} bytes")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to file: ${file.absolutePath}", e)
            null
        }
    }
}