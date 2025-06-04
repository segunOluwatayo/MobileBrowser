package com.example.mobilebrowser.classifier

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class UrlCnnInterpreter(context: Context) {

    private val interpreter = Interpreter(
        FileUtil.loadMappedFile(context, "url_cnn_fp32.tflite")
    )

    private val idxCh:  Int
    private val idxNum: Int

    init {
        var ch = -1
        var num = -1
        for (i in 0 until interpreter.inputTensorCount) {
            val t = interpreter.getInputTensor(i)
            when {
                t.dataType() == DataType.INT32   && t.shape().contentEquals(intArrayOf(1, 200)) -> ch  = i
                t.dataType() == DataType.FLOAT32 && t.shape().contentEquals(intArrayOf(1, 8))   -> num = i
            }
        }
        check(ch  >= 0) { "Char-input tensor not found." }
        check(num >= 0) { "Num-input tensor not found." }
        idxCh  = ch
        idxNum = num
    }


    private val scaler = StandardScaler(
        AssetUtils.bytes(context, "scaler.json")
    )

    private val threshold: Float = JSONObject(
        AssetUtils.text(context, "threshold.json")
    ).getDouble("threshold").toFloat()

    private val badBloom = BadBloom(context)
    private val good = HashSet<String>().apply {
        context.assets.open("allowlist.txt")
            .bufferedReader()
            .forEachLine { add(it) }
    }

    init {
        val dbgUrl = "https://fantasticfilms.ru"
        val feats   = FeatureExtractor.features(dbgUrl)
        val scaled  = scaler.transform(feats)
        val chars   = CharEncoder.encode(dbgUrl).take(50)
        val p       = predict(dbgUrl)

        Log.d("UrlCnnDBG", "── ANDROID DEBUG ──")
        Log.d("UrlCnnDBG", "features : ${'$'}{feats.contentToString()}")
        Log.d("UrlCnnDBG", "scaled   : ${'$'}{scaled.contentToString()}")
        Log.d("UrlCnnDBG", "char ids : ${'$'}{chars.toIntArray().contentToString()}")
        Log.d("UrlCnnDBG", "model p  : ${'$'}p")
        for (i in 0 until interpreter.inputTensorCount) {
            val t = interpreter.getInputTensor(i)
            Log.d("TFL_INPUT", "${'$'}i → ${'$'}{t.name()}  ${'$'}{t.dataType()}  ${'$'}{t.shape().contentToString()}")
        }
    }

    fun predict(url: String): Float {

        // build the two feature vectors exactly like Python
        val chVec  = CharEncoder.encode(url)
        val numVec = scaler.transform(FeatureExtractor.features(url))

        val inputs = arrayOfNulls<Any>(2)
        inputs[idxCh]  = arrayOf(chVec)
        inputs[idxNum] = arrayOf(numVec)

        // run and pull the single scalar output
        val outputs = hashMapOf<Int, Any>().apply {
            put(0, Array(1) { FloatArray(1) })
        }
        interpreter.runForMultipleInputsOutputs(inputs, outputs)
        return (outputs[0] as Array<FloatArray>)[0][0]
    }


    fun verdict(url: String): String {
        val domain = DomainUtils.registeredDomain(url)

        if (domain in good) return "Benign (allow-list)"

        if (domain in badBloom) {
            val p = predict(url)
            return if (p < threshold)
                "Benign (bloom FP, p=%.3f)".format(p)
            else "Malicious (feed)"
        }

        val p = predict(url)
        return if (p >= threshold)
            "Malicious (p=%.3f)".format(p)
        else  "Benign (p=%.3f)".format(p)
    }
}
