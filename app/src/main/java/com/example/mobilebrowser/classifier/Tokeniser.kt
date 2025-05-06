//package com.example.mobilebrowser.classifier
//
//// Pad and unknown tokens as Ints
//private const val PAD: Int = 0
//private const val UNK: Int = 1
//
//// Map printable ASCII characters to Int indices (starting at 2)
//private val VOCAB: Map<Char, Int> = (' '..'~').withIndex()
//    .associate { it.value to it.index + 2 }
//
///**
// * Encode a URL into a fixed-length IntArray for TFLite INT32 input.
// * @param url the input URL string
// * @param max maximum length (default 200)
// * @return IntArray of length max, padded with PAD (0) and using UNK (1) for unknown chars
// */
//fun encode(url: String, max: Int = 200): IntArray {
//    val arr = IntArray(max) { PAD }
//    url.take(max).forEachIndexed { i, ch ->
//        arr[i] = VOCAB[ch] ?: UNK
//    }
//    return arr
//}
