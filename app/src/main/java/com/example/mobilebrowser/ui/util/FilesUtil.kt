package com.example.mobilebrowser.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

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
        // If mimeType is generic, infer the MIME type from the file extension.
        val actualMimeType = if (mimeType == "application/octet-stream") {
            val ext = file.extension.lowercase()
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: mimeType
        } else {
            mimeType
        }

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, actualMimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Ensure we have the new task flag if the context isn’t an Activity
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

    fun extractFileNameFromContentDisposition(contentDisposition: String): String? {
        // This regex tries to match:
        //   filename*=UTF-8''<encoded-filename> OR filename="<filename>"
        val regex = Regex("filename\\*=UTF-8''([^;]+)|filename=\"?([^;\"]+)\"?")
        val matchResult = regex.find(contentDisposition)
        return when {
            matchResult != null -> matchResult.groups[1]?.value ?: matchResult.groups[2]?.value
            else -> null
        }
    }

    suspend fun fetchContentLength(url: String): Long = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            // Request the uncompressed length
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            val contentLength = connection.getHeaderField("Content-Length")?.toLongOrNull() ?: 0L
            connection.disconnect()
            contentLength
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }


}