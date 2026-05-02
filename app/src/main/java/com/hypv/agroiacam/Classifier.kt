package com.hypv.agroiacam

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Carga model.tflite y labels.txt desde assets/ y clasifica una imagen.
 *
 * Uso:
 *   val classifier = Classifier(context)
 *   val result = classifier.classify(bitmap)
 *   // result.label  → "Pothos sano"
 *   // result.confidence → 0.94f  (94%)
 */
class Classifier(private val context: Context) {

    data class Result(val label: String, val confidence: Float) {
        val percent: Int get() = (confidence * 100).toInt()
        val isHealthy: Boolean get() = label.contains("sano", ignoreCase = true)
    }

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    // Teachable Machine exporta imágenes de 224×224
    private val inputSize = 224

    init {
        try {
            interpreter = Interpreter(loadModelFile())
            labels = loadLabels()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // ── Clasificar bitmap ────────────────────────────────────────────────────
    fun classify(bitmap: Bitmap): Result {
        val interp = interpreter
            ?: return Result("Sin modelo", 0f)

        if (labels.isEmpty())
            return Result("Sin etiquetas", 0f)

        // Preprocesar imagen
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

        // Buffer de salida: [1][num_clases]
        val output = Array(1) { FloatArray(labels.size) }
        interp.run(tensorImage.buffer, output)

        // Encontrar la clase con mayor confianza
        val scores = output[0]
        val maxIdx = scores.indices.maxByOrNull { scores[it] } ?: 0

        return Result(
            label      = labels.getOrElse(maxIdx) { "Desconocido" },
            confidence = scores[maxIdx]
        )
    }

    // ── Cargar model.tflite desde assets/ ───────────────────────────────────
    private fun loadModelFile(): MappedByteBuffer {
        val fd = context.assets.openFd("model.tflite")
        val stream = FileInputStream(fd.fileDescriptor)
        val channel = stream.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    // ── Cargar labels.txt desde assets/ ─────────────────────────────────────
    // Teachable Machine genera labels.txt con una etiqueta por línea
    private fun loadLabels(): List<String> {
        return try {
            context.assets.open("labels.txt")
                .bufferedReader()
                .readLines()
                .filter { it.isNotBlank() }
        } catch (e: IOException) {
            emptyList()
        }
    }

    fun close() {
        interpreter?.close()
    }
}