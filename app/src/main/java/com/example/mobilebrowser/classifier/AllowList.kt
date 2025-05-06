//package com.example.mobilebrowser.classifier
//
//import android.content.Context
//
//class AllowList(ctx: Context) {
//    private val set: Set<String> =
//        ctx.assets.open("allowlist.txt").bufferedReader().readLines().toSet()
//
//    operator fun contains(domain: String): Boolean = domain in set
//}
