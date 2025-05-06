package com.example.mobilebrowser.classifier

import com.google.common.net.InternetDomainName

//object DomainUtils {
//
//    /**
//     * Returns the “registered domain” (top domain under public suffix) or
//     * the complete hostname if no public suffix is recognised.
//     */
//    fun registeredDomain(url: String): String {
//        val host = url.lowercase()
//            .substringAfter("://")
//            .substringBefore("/")
//        return try {
//            val idn = InternetDomainName.from(host)
//            if (idn.isUnderPublicSuffix) idn.topPrivateDomain().toString() else host
//        } catch (e: IllegalArgumentException) {
//            host                             // not a valid domain – use as-is
//        }
//    }
//}

import java.net.URI
import java.net.URISyntaxException

/**
 * Utility for domain extraction that matches Python's tldextract behavior
 */
object DomainUtils {

    /**
     * Extract the registered domain from a URL, matching Python's tldextract behavior
     */
    fun registeredDomain(url: String): String {
        try {
            // Handle cases with or without protocol
            val urlWithProtocol = if (!url.contains("://")) "https://$url" else url

            // Parse the hostname
            val uri = URI(urlWithProtocol)
            val host = uri.host ?: return url

            try {
                val internetDomainName = InternetDomainName.from(host)

                // If under public suffix, return the top private domain (equivalent to registered_domain)
                return if (internetDomainName.isUnderPublicSuffix) {
                    internetDomainName.topPrivateDomain().toString()
                } else {
                    // If not under public suffix, return the host
                    host
                }
            } catch (e: Exception) {
                // If domain parsing fails, fall back to host or original URL
                return host
            }
        } catch (e: URISyntaxException) {
            // If URI parsing fails, fall back to original URL
            return url
        }
    }
}
