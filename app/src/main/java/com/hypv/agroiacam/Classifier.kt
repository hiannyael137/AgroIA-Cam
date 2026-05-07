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

class Classifier(private val context: Context) {

    private var interpreter: Interpreter
    private var labels = mutableListOf<String>()

    private val imageSize = 224

    init {

        val modelBuffer = loadModelFile()

        interpreter = Interpreter(modelBuffer)

        loadLabels()
    }

    private fun loadModelFile(): ByteBuffer {

        val fileDescriptor =
            context.assets.openFd("model_unquant.tflite")

        val inputStream =
            fileDescriptor.createInputStream()

        val bytes =
            inputStream.readBytes()

        val byteBuffer =
            ByteBuffer.allocateDirect(bytes.size)

        byteBuffer.order(ByteOrder.nativeOrder())

        byteBuffer.put(bytes)

        byteBuffer.rewind()

        return byteBuffer
    }

    private fun loadLabels() {

        val reader = BufferedReader(
            InputStreamReader(
                context.assets.open("labels.txt")
            )
        )

        reader.forEachLine {
            labels.add(it)
        }

        reader.close()
    }

    fun classify(bitmap: Bitmap): String {

        val resizedBitmap =
            bitmap.scale(
                imageSize,
                imageSize
            )

        val byteBuffer =
            ByteBuffer.allocateDirect(
                4 * imageSize * imageSize * 3
            )

        byteBuffer.order(ByteOrder.nativeOrder())

        for (y in 0 until imageSize) {

            for (x in 0 until imageSize) {

                val pixel = resizedBitmap[x, y]

                byteBuffer.putFloat(
                    ((pixel shr 16 and 0xFF) / 255f)
                )

                byteBuffer.putFloat(
                    ((pixel shr 8 and 0xFF) / 255f)
                )

                byteBuffer.putFloat(
                    ((pixel and 0xFF) / 255f)
                )
            }
        }

        val output =
            Array(1) {
                FloatArray(labels.size)
            }

        interpreter.run(byteBuffer, output)

        var maxIndex = 0
        var maxConfidence = 0f

        output[0].forEachIndexed { index, confidence ->

            if (confidence > maxConfidence) {

                maxConfidence = confidence
                maxIndex = index
            }
        }

        return "${labels[maxIndex]} - ${(maxConfidence * 100).toInt()}%"
    }
}