//package com.example.mobilebrowser.classifier
//
//object CharEncoder {
//    private const val PAD: Int = 0
//    private const val UNK: Int = 1
//    private const val MAX_LEN = 200
//    private val vocab: Map<Char, Int> = buildMap {
//        (' '..'~').forEachIndexed { idx, ch -> put(ch, idx + 2) }
//    }
//
//    fun encode(url: String): IntArray {
//        val dom = DomainUtils.registeredDomain(url)
//        val arr = IntArray(MAX_LEN) { PAD }
//        dom.take(MAX_LEN).forEachIndexed { i, ch ->
//            arr[i] = vocab[ch] ?: UNK
//        }
//        return arr
//    }
//}
package com.example.mobilebrowser.classifier

/**
 * Encodes URL characters to match Python implementation exactly
 */
object CharEncoder {
    private const val PAD: Int = 0
    private const val UNK: Int = 1
    private const val MAX_LEN = 200

    // Build vocab map for characters ASCII 32-126 (same as Python)
    private val vocab: Map<Char, Int> = buildMap {
        for (i in 32..126) {
            val ch = i.toChar()
            put(ch, i - 32 + 2) // +2 to account for PAD and UNK tokens
        }
    }

    /**
     * Encode a URL's domain characters to integers exactly like Python
     */
    fun encode(url: String): IntArray {
        // Get the registered domain (or top domain under public suffix)
        val dom = DomainUtils.registeredDomain(url)

        // Create the output array filled with PAD token
        val arr = IntArray(MAX_LEN) { PAD }

        // Map each character to its token ID
        dom.take(MAX_LEN).forEachIndexed { i, ch ->
            arr[i] = vocab[ch] ?: UNK
        }

        return arr
    }
}
