package com.example.mobilebrowser.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import java.io.File
import java.io.FileOutputStream

object ThumbnailUtil {

    /**
     * Captures a screenshot of the provided view as a Bitmap.
     */
    fun captureThumbnail(view: View): Bitmap? {
        return try {
            // Created a bitmap with the view's dimensions and draw the view onto the bitmap.
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves the given bitmap as a PNG file in the provided directory.
     * Returns the absolute path of the saved file.
     */
    fun saveBitmapToFile(bitmap: Bitmap, file: File): String? {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
