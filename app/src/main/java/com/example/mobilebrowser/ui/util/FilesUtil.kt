package com.example.mobilebrowser.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

object FileUtils {
    fun getMimeType(url: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    fun createShareIntent(context: Context, file: File, mimeType: String): Intent {
        val uri = getUriForFile(context, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createOpenIntent(context: Context, file: File, mimeType: String): Intent {
        val uri = getUriForFile(context, file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun getSafeFileName(fileName: String): String {
        return fileName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
    }

    @SuppressLint("DefaultLocale")
    fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(sizeInBytes.toDouble()) / Math.log10(1024.0)).toInt()

        return String.format(
            "%.1f %s",
            sizeInBytes / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    fun getFormattedFileName(fileName: String): String {
        // Decode URL encoded characters
        var decodedName = try {
            java.net.URLDecoder.decode(fileName, "UTF-8")
        } catch (e: Exception) {
            fileName
        }

        // Remove any URL parameters
        decodedName = decodedName.substringBefore('?')

        // Remove any extra path information
        decodedName = decodedName.substringAfterLast('/')

        // Replace problematic characters
        decodedName = decodedName.replace(Regex("[\\\\/:*?\"<>|]"), "_")

        return decodedName
    }

}