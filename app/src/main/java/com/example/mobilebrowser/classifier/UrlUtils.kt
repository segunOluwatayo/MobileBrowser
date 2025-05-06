//package com.example.mobilebrowser.classifier
//
//import android.net.Uri
//import com.google.common.net.InternetDomainName
//import kotlin.math.log2
//
///** Same ASCII alphabet used during training. */
//private const val PAD = 0
//private const val UNK = 1
//private const val MAX_LEN = 200
//private val VOCAB = IntArray(128).apply {
//    var idx = 2
//    for (c in 32..126) this[c] = idx++      // printable ASCII
//}
//
///** Extract the “registered” (top-private) domain. */
//fun registeredDomain(url: String): String =
//    runCatching {
//        val host = Uri.parse(url).host ?: url
//        InternetDomainName.from(host).topPrivateDomain().toString()
//    }.getOrDefault(url)
//
///** Python-equivalent encode() – returns IntArray[200]. */
//fun encode(url: String): IntArray {
//    val dom = registeredDomain(url)
//    val arr = IntArray(MAX_LEN) { PAD }
//    for (i in 0 until minOf(dom.length, MAX_LEN)) {
//        val ch = dom[i].code
//        arr[i] = if (ch < 128) VOCAB[ch] else UNK
//    }
//    return arr
//}
//
//fun feats(url: String): FloatArray {
//    // 1) Get the full registered domain (e.g. "fantasticfilms.ru")
//    val dom = registeredDomain(url)
//    val L   = dom.length
//
//    // 2) Count special / dash / digit characters
//    val spec  = dom.count { !it.isLetterOrDigit() }
//    val dash  = dom.count { it == '-' }
//    val digit = dom.count { it.isDigit() }
//
//    // 3) Compute subdomain-label count (same as Python's ext.subdomain.count + 1 if subdomain)
//    //    We split the host by '.', drop the last two (domain + suffix) to get sublabels.
//    val hostParts = Uri.parse(url).host?.split('.') ?: listOf(dom)
//    val subLabelCount = if (hostParts.size > 2) hostParts.take(hostParts.size - 2).size else 0
//
//    // 4) Compute Shannon entropy over the registered-domain string
//    val freq = dom.groupingBy { it }.eachCount()
//    var entropy = 0.0
//    for ((_, count) in freq) {
//        val p = count.toDouble() / L
//        entropy -= p * log2(p)
//    }
//
//    // 5) The 6th feature in Python was len(ext.domain) (i.e. second-level domain).
//    //    We can recompute that by splitting off the public suffix.
//    val coreDomain = InternetDomainName.from(dom).parts().let { parts ->
//        if (parts.size >= 2) parts[parts.size - 2] else dom
//    }
//    val coreLen = coreDomain.length.toFloat()
//
//    return floatArrayOf(
//        L.toFloat(),            // total length
//        spec.toFloat(),         // non-alnum chars
//        dash.toFloat(),         // '-' count
//        digit.toFloat(),        // digit count
//        digit.toFloat() / L,    // digit fraction
//        coreLen,                // length of second-level domain
//        subLabelCount.toFloat(),
//        entropy.toFloat()
//    )
//}
//
///**  exact equivalent of tldextract.top_domain_under_public_suffix  */
//private fun coreDomain(url: String): String {
//    val host = Uri.parse(url).host ?: url
//    val parts = InternetDomainName.from(host).parts()
//    // last element = public suffix (e.g. "ru"); one before that = core domain
//    return if (parts.size >= 2) parts[parts.size - 2] else host
//}
