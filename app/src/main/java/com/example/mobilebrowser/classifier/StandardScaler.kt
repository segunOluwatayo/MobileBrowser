package com.example.mobilebrowser.classifier

import org.json.JSONObject
import java.nio.charset.StandardCharsets

//class StandardScaler(jsonBytes: ByteArray) {
//    private val mean: FloatArray
//    private val scale: FloatArray
//    init {
//        val obj = JSONObject(String(jsonBytes, StandardCharsets.UTF_8))
//        mean = obj.getJSONArray("mean").let { ja ->
//            FloatArray(ja.length()) { ja.getDouble(it).toFloat() }
//        }
//        scale = obj.getJSONArray("scale").let { ja ->
//            FloatArray(ja.length()) { ja.getDouble(it).toFloat() }
//        }
//    }
//
//    fun transform(x: FloatArray): FloatArray =
//        FloatArray(x.size) { i -> (x[i] - mean[i]) / scale[i] }
//}


// Implements sklearn's StandardScaler functionality for feature normalization

class StandardScaler(jsonBytes: ByteArray) {
    private val mean: FloatArray
    private val scale: FloatArray

    init {
        val json = JSONObject(String(jsonBytes))

        val meanArray = json.getJSONArray("mean")
        val scaleArray = json.getJSONArray("scale")

        mean = FloatArray(meanArray.length())
        scale = FloatArray(scaleArray.length())

        for (i in 0 until meanArray.length()) {
            mean[i] = meanArray.getDouble(i).toFloat()
            scale[i] = scaleArray.getDouble(i).toFloat()
        }
    }

//     Transform features using the loaded mean and scale values

    fun transform(features: FloatArray): FloatArray {
        val result = FloatArray(features.size)

        for (i in features.indices) {
            result[i] = (features[i] - mean[i]) / scale[i]
        }

        return result
    }
}
