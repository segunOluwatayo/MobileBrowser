package com.example.mobilebrowser.classifier

import android.content.Context

object AssetUtils {
    fun bytes(context: Context, file: String): ByteArray =
        context.assets.open(file).use { it.readBytes() }

    fun text(context: Context, file: String): String =
        bytes(context, file).decodeToString()
}
