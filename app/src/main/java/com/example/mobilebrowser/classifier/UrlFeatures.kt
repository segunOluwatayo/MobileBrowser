//package com.example.mobilebrowser.classifier
//
//import android.net.Uri
//
//object UrlFeatures {
//
//    /** Extracts the same four features used in Python: length, special-chars, subdomains, entropy. */
//    fun fromUrl(url: String): FloatArray {
//        val length = url.length.toFloat()
//        val specials = url.count { !it.isLetterOrDigit() }.toFloat()
//
//        val uri = Uri.parse(url)
//        val host = uri.host ?: ""
//        // subdomain count = number of dots in host minus 1 for the TLD
//        val subdomains = host.split(".").let { if (it.size > 2) it.size - 2 else 0 }.toFloat()
//
//        // Shannon entropy over characters
//        val freqs = url.groupingBy { it }.eachCount()
//        val entropy = freqs.values
//            .map { freq ->
//                val p = freq.toDouble() / length
//                -p * kotlin.math.log2(p)
//            }
//            .sum()
//            .toFloat()
//
//        return floatArrayOf(length, specials, subdomains, entropy)
//    }
//}
