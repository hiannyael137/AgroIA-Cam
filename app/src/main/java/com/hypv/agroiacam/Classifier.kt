package com.hypv.agroiacam

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.get
import androidx.core.graphics.scale
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Clasificador TensorFlow Lite para AgroIA.
 *
 * Versión mejorada:
 * - Devuelve top1 y top2.
 * - También devuelve TODAS las probabilidades por etiqueta.
 * Esto permite que DashboardActivity sume probabilidades por especie.
 *
 * Ejemplo importante:
 * Si el modelo devuelve:
 *   Cola de borrego enfermo-tallo seco: 50%
 *   Cola de borrego Sana: 49%
 * Entonces top1 parece enfermo, pero por especie es 99% Cola de borrego.
 * La app puede decir: "planta reconocida, salud no concluyente".
 */
data class ClassificationResult(
    val label: String,
    val confidence: Float,
    val secondLabel: String,
    val secondConfidence: Float,
    val probabilities: Map<String, Float>
) {
    val confidencePercent: Int get() = (confidence * 100f).toInt().coerceIn(0, 100)
    val secondConfidencePercent: Int get() = (secondConfidence * 100f).toInt().coerceIn(0, 100)
    val margin: Float get() = confidence - secondConfidence
    val marginPercent: Int get() = (margin * 100f).toInt().coerceIn(0, 100)
}

class Classifier(private val context: Context) {

    private val interpreter: Interpreter
    private val labels = mutableListOf<String>()
    private val imageSize = 224

    init {
        interpreter = Interpreter(loadModelFile())
        loadLabels()
    }

    private fun loadModelFile(): ByteBuffer {
        val fileDescriptor = context.assets.openFd("model_unquant.tflite")
        val inputStream = fileDescriptor.createInputStream()
        val bytes = inputStream.readBytes()

        val byteBuffer = ByteBuffer.allocateDirect(bytes.size)
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.put(bytes)
        byteBuffer.rewind()
        return byteBuffer
    }

    private fun loadLabels() {
        val reader = BufferedReader(InputStreamReader(context.assets.open("labels.txt")))
        reader.forEachLine { line ->
            val clean = line.trim()
            val label = if (clean.matches(Regex("^\\d+\\s+.+"))) {
                clean.substringAfter(" ").trim()
            } else {
                clean
            }
            if (label.isNotBlank()) labels.add(label)
        }
        reader.close()
    }

    /** Método viejo para compatibilidad con pantallas anteriores. */
    fun classify(bitmap: Bitmap): String {
        val r = classifyDetailed(bitmap)
        return "${r.label} - ${r.confidencePercent}%"
    }

    fun classifyDetailed(bitmap: Bitmap): ClassificationResult {
        if (labels.isEmpty()) {
            return ClassificationResult("Sin etiquetas", 0f, "", 0f, emptyMap())
        }

        val resizedBitmap = bitmap.scale(imageSize, imageSize)
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        for (y in 0 until imageSize) {
            for (x in 0 until imageSize) {
                val pixel = resizedBitmap[x, y]
                byteBuffer.putFloat(((pixel shr 16 and 0xFF) / 255f))
                byteBuffer.putFloat(((pixel shr 8 and 0xFF) / 255f))
                byteBuffer.putFloat(((pixel and 0xFF) / 255f))
            }
        }
        byteBuffer.rewind()

        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(byteBuffer, output)

        val probabilities = labels.mapIndexed { index, label ->
            label to output[0].getOrElse(index) { 0f }
        }.toMap()

        val sorted = probabilities.entries.sortedByDescending { it.value }
        val first = sorted.getOrNull(0)
        val second = sorted.getOrNull(1)

        return ClassificationResult(
            label = first?.key ?: "Desconocido",
            confidence = first?.value ?: 0f,
            secondLabel = second?.key ?: "",
            secondConfidence = second?.value ?: 0f,
            probabilities = probabilities
        )
    }
}
