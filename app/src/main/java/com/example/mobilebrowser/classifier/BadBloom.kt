package com.example.mobilebrowser.classifier

import android.content.Context
import com.google.common.hash.BloomFilter
import com.google.common.hash.Funnels
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class BadBloom(context: Context) {

    val bloom: BloomFilter<CharSequence> = BloomFilter.create(
        Funnels.stringFunnel(StandardCharsets.UTF_8),
        /* expectedInsertions = */ 200_000,
        /* fpp = */ 0.01
    )

    init {
        context.assets.open("bad_domains.txt").use { `in` ->
            BufferedReader(InputStreamReader(`in`)).forEachLine { bloom.put(it) }
        }
    }

    operator fun contains(domain: String) = bloom.mightContain(domain)
}
