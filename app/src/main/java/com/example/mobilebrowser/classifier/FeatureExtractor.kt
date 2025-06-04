//// FeatureExtractor.kt
//package com.example.mobilebrowser.classifier
//
//import com.google.common.net.InternetDomainName
//import kotlin.math.ln
//
//object FeatureExtractor {
//
//    fun features(url: String): FloatArray {
//        val host = url.lowercase()
//            .substringAfter("://")
//            .substringBefore("/")
//
//        val idn = InternetDomainName.from(host)
//
//        // registered / top-private domain, e.g. "fantasticfilms.ru"
//        val regDomain = if (idn.isUnderPublicSuffix) idn.topPrivateDomain().toString()
//        else host
//
//        // ─── feature calculations ──────────────────────────────────────
//        val L = regDomain.length.coerceAtLeast(1)
//
//        // helpers
//        fun count(ch: Char) = regDomain.count { it == ch }
//        val digits = regDomain.count { it.isDigit() }
//        val nonAlnum = regDomain.count { !it.isLetterOrDigit() }
//
//        // length of "domain" part (without public suffix)
//        val domainLen = regDomain.substringBeforeLast('.').length
//
//        // # sub-domain labels = dots in subdomain + 1
//        val subLabels = (idn.parts().size -
//                (idn.publicSuffix()?.parts()?.size ?: 1) - 1)
//            .coerceAtLeast(0)
//            .toFloat()
//
//        // entropy in bits
//        val entropy = regDomain.toSet().sumOf { c ->
//            val p = count(c).toDouble() / L
//            -p * ln(p) / ln(2.0)
//        }
//
//        return floatArrayOf(
//            L.toFloat(),                     // 0
//            nonAlnum.toFloat(),              // 1
//            count('-').toFloat(),            // 2
//            digits.toFloat(),                // 3
//            digits.toFloat() / L,            // 4
//            domainLen.toFloat(),             // 5
//            subLabels,                       // 6  ← fixed
//            entropy.toFloat()                // 7
//        )
//    }
//}
package com.example.mobilebrowser.classifier

import kotlin.math.ln

// Extract features from a URL in exactly the same way as the Python implementation
object FeatureExtractor {

    fun features(url: String): FloatArray {
        // Get the registered domain using the utility
        val dom = DomainUtils.registeredDomain(url)

        // Length of the domain
        val L = dom.length.coerceAtLeast(1)

        // Helper for character counting
        fun count(char: Char): Int = dom.count { it == char }

        // Count of non alphanumeric characters
        val nonAlnum = dom.count { !it.isLetterOrDigit() }

        // Count of hyphens
        val hyphens = count('-')

        // Count of digits
        val digits = dom.count { it.isDigit() }

        // Ratio of digits to total length
        val digitRatio = digits.toFloat() / L

        // Extract domain length
        val domainPart = if (dom.contains('.')) dom.substringBeforeLast('.') else dom
        val domainLength = domainPart.length

        // Count subdomain parts
        val subdomainCount = if (url.contains("://")) {
            val host = url.substringAfter("://").substringBefore("/")
            val parts = host.split('.')
            val domParts = dom.split('.')
            // Subdomain count is the difference between host parts and domain parts
            (parts.size - domParts.size).coerceAtLeast(0) + 1
        } else {
            1
        }

        // Calculate entropy
        val entropy = dom.toSet().sumOf { char ->
            val p = count(char).toDouble() / L
            -p * ln(p) / ln(2.0)
        }

        return floatArrayOf(
            L.toFloat(),
            nonAlnum.toFloat(),
            hyphens.toFloat(),
            digits.toFloat(),
            digitRatio,
            domainLength.toFloat(),
            subdomainCount.toFloat(),
            entropy.toFloat()
        )
    }
}
