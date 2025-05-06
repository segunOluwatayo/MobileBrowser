//package com.example.mobilebrowser.classifier
//
//import android.content.Context
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//
///**
// * Simple 3-hash Bloom filter exactly matching the Python implementation that
// * created bad_domains.bloom.
// */
//class Bloom(ctx: Context) {
//
//    private val buf: ByteBuffer = ctx.assets.open("bad_domains.bloom").use { input ->
//        ByteBuffer.wrap(input.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
//    }
//
//    private fun hash(s: String, seed: Int): Int {
//        var h = seed
//        for (ch in s) h = h * 31 + ch.code
//        return h
//    }
//
//    /** True = “might be in the set”; False = “definitely not”. */
//    fun mightContain(domain: String): Boolean {
//        val bits = buf.capacity() * 8
//        val h1   = hash(domain, 0x7f4a7c15)
//        val h2   = hash(domain, 0x4ad5)
//        repeat(3) { i ->
//            val bit  = ((h1 + i * h2) and Int.MAX_VALUE) % bits
//            val byte = buf.get(bit / 8).toInt()
//            if (byte and (1 shl (bit % 8)) == 0) return false
//        }
//        return true
//    }
//}
