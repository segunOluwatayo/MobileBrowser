//package com.example.mobilebrowser.classifier
//
//import android.content.Context
//import android.util.Log
//import org.tensorflow.lite.DataType
//import org.tensorflow.lite.Interpreter
//import org.tensorflow.lite.support.common.FileUtil
//import org.json.JSONObject
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//
///**
// * Complete “hybrid” classifier:
// *   • Bloom filter    → fast feed-based block
// *   • (optional) allow-list
// *   • CNN + numerical features
// *   • raw probability from the model
// *   • threshold chosen by find_threshold.py
// *
// * Includes sanity checks and runtime debug to ensure the TFLite inputs are correct.
// */
//class UrlCnnClassifier(ctx: Context) {
//
//    private val interp: Interpreter = Interpreter(
//        FileUtil.loadMappedFile(ctx, "url_cnn_int8.tflite"),
//        Interpreter.Options().setNumThreads(2)
//    )
//
//    init {
//        // Debug: dump input tensor specs
//        for (i in 0 until interp.inputTensorCount) {
//            val t = interp.getInputTensor(i)
//            Log.d("TFLITE", "in[${i}] name=${t.name()}  type=${t.dataType()}  shape=${t.shape().contentToString()}")
//        }
//        // Sanity checks: ensure tensor dtypes and shapes match expectations
//        // 🔢 Numeric features go in tensor #0
//        check(interp.getInputTensor(0).dataType() == DataType.FLOAT32) {
//            "Tensor#0 must be FLOAT32 (numeric features)"
//        }
//        check(interp.getInputTensor(0).shape().contentToString() == "[1, 8]") {
//            "Tensor#0 shape must be [1,8]"
//        }
//
//// 🔠 Char-ID sequence goes in tensor #1
//        check(interp.getInputTensor(1).dataType() == DataType.INT32) {
//            "Tensor#1 must be INT32 (char IDs)"
//        }
//        check(interp.getInputTensor(1).shape().contentToString() == "[1, 200]") {
//            "Tensor#1 shape must be [1,200]"
//        }
//
//    }
//
//    private val scaler    = Scaler(ctx)
//    private val bloom     = Bloom(ctx)
//    private val allowList = AllowList(ctx)
//
//    /** threshold.json produced by scripts/find_threshold.py */
//    private val thr: Float = ctx.assets.open("threshold.json")
//        .bufferedReader()
//        .use { JSONObject(it.readText()).getDouble("threshold").toFloat() }
//
//    /* ------------------------------------------------------------------ */
//
//    /**
//     * Encode-and-run, return raw P(malicious).
//     * Ensures numeric and char inputs are wrapped exactly once.
//     * Dumps the character-id input buffer at runtime for sanity.
//     */
//    private fun rawScore(url: String): Float {
//        /* 1️⃣  Build a 2-D primitive array for the numeric features  */
//        val num2d: Array<FloatArray> =
//            arrayOf(scaler.normalize(feats(url)))      // shape [1, 8]  FLOAT32
//
//        /* 2️⃣  Build a 2-D primitive array for the character ids     */
//        val ids2d: Array<IntArray> =
//            arrayOf(encode(url))                       // shape [1, 200] INT32
//
//        // ← insert it here:
//        Log.d("URLTest", "first 10 char-ids = ${ids2d[0].take(10)}")
//
//        /* 3️⃣  Supply them in the order printed by the interpreter   */
//        val inputs: Array<Any> = arrayOf(num2d, ids2d)
//
//        val rawNum = feats(url)
//        val normNum = scaler.normalize(rawNum)
//        Log.d("URLTest", "raw-feats = ${rawNum.joinToString()}, scaled = ${normNum.joinToString()}")
//
//        /* 4️⃣  Run                                                         */
//        val out = arrayOf(FloatArray(1))                // FLOAT32[1,1]
//        interp.runForMultipleInputsOutputs(
//            inputs,
//            mapOf(0 to out)
//        )
//
//        /* 5️⃣  Runtime check (pre-run): log the first 10 char IDs */
//        Log.d("DEBUG", "first 10 char-ids = ${ids2d[0].take(10)}")
//
//        return out[0][0]
//    }
//
//    /** Public: probability in [0,1] straight from the network. */
//    fun score(url: String): Float = rawScore(url)
//
//    /** Verdict using allow-list → Bloom → CNN threshold cascade. */
//    fun verdict(url: String): String {
//        val dom = registeredDomain(url)
//
//        // 1) allow-list
//        if (dom in allowList) {
//            return "Benign (allow-list)"
//        }
//
//        // 2) feed via Bloom
//        if (bloom.mightContain(dom)) {
//            return "Malicious (feed)"
//        }
//
//        // 3) ML model
//        val p = score(url)
//        return if (p >= thr) {
//            "Malicious (p=%.3f)".format(p)
//        } else {
//            "Benign (p=%.3f)".format(p)
//        }
//    }
//}
