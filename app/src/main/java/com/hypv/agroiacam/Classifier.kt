package com.hypv.agroiacam

// app/src/main/java/com/hypv/agroiacam/Classifier.kt
// Copiar model_unquant.tflite y labels.txt a app/src/main/assets/

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

// ── Soluciones por planta ─────────────────────────────────
private val CUIDADOS = mapOf(
    "Rosal blanco"    to "Riego moderado 2 veces por semana. Plena luz solar. Poda ramas secas. Abona en primavera.",
    "Cola de burro"   to "Riego escaso cada 2-3 semanas. Tolera la sequía. Luz indirecta brillante. Tierra con buen drenaje.",
    "Pasto de limón"  to "Riego frecuente, suelo húmedo. Pleno sol. Corta cada mes para mantener aroma y forma.",
    "Alcatraz"        to "Suelo húmedo constantemente. Sombra parcial. Abono mensual en época de crecimiento.",
    "Cinta"           to "Riego moderado cada semana. Tolera poca luz. Evita luz solar directa. Limpia las hojas con paño húmedo."
)

data class ClasificacionResult(
    val planta:     String,
    val confianza:  Float,
    val estadoTexto:String,   // "Saludable" / "Monitorear" / "En riesgo"
    val estadoColor:Int,      // 0=verde 1=amarillo 2=rojo
    val consejo:    String
)

class Classifier(private val context: Context) {

    companion object { private const val INPUT_SIZE = 224 }

    private var interpreter: Interpreter? = null
    private var labels: List<String>      = emptyList()

    init {
        try {
            interpreter = Interpreter(loadModel())
            labels      = loadLabels()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun loadModel(): MappedByteBuffer {
        val fd = context.assets.openFd("model_unquant.tflite")
        return FileInputStream(fd.fileDescriptor).channel
            .map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    private fun loadLabels(): List<String> =
        context.assets.open("labels.txt")
            .bufferedReader()
            .readLines()
            .filter { it.isNotBlank() }
            // Teachable Machine pone "0 Rosal blanco" — quitamos el número
            .map { it.trim().replace(Regex("^\\d+\\s+"), "") }

    fun classify(bitmap: Bitmap): ClasificacionResult {
        if (interpreter == null || labels.isEmpty())
            return ClasificacionResult("Sin modelo", 0f, "Error", 2, "Asegúrate de copiar model_unquant.tflite a assets/")

        // Escalar a 224×224
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)

        // Tensor entrada: [1][224][224][3]  float normalizado 0-1
        val input = Array(1) { Array(INPUT_SIZE) { Array(INPUT_SIZE) { FloatArray(3) } } }
        for (y in 0 until INPUT_SIZE) for (x in 0 until INPUT_SIZE) {
            val px = resized.getPixel(x, y)
            input[0][y][x][0] = ((px shr 16) and 0xFF) / 255f  // R
            input[0][y][x][1] = ((px shr 8)  and 0xFF) / 255f  // G
            input[0][y][x][2] = ( px          and 0xFF) / 255f  // B
        }

        val output = Array(1) { FloatArray(labels.size) }
        interpreter!!.run(input, output)

        // Clase con mayor confianza
        val idx        = output[0].indices.maxByOrNull { output[0][it] } ?: 0
        val confianza  = output[0][idx]
        val nombrePlanta = if (idx < labels.size) labels[idx] else "Desconocida"

        // ── Determinar estado de salud ────────────────────
        // El modelo solo fue entrenado con plantas SANAS.
        // Alta confianza → la reconoce bien → Saludable
        // Baja confianza → la planta luce diferente a la foto de entrenamiento → posible problema
        val (estadoTexto, estadoColor) = when {
            confianza >= 0.75f -> Pair("Saludable",  0)  // verde
            confianza >= 0.50f -> Pair("Monitorear", 1)  // amarillo
            else               -> Pair("En riesgo",  2)  // rojo
        }

        val consejo = when (estadoColor) {
            0    -> (CUIDADOS[nombrePlanta] ?: "Continúa con el cuidado habitual.")
            1    -> "Revisa la humedad del suelo y la exposición a la luz. Observa si aparecen manchas o cambios de color en las hojas.\n\n${CUIDADOS[nombrePlanta] ?: ""}"
            else -> "Inspecciona la planta en busca de plagas, manchas o raíces podridas. Considera cambiar el sustrato o ajustar el riego.\n\n${CUIDADOS[nombrePlanta] ?: ""}"
        }

        return ClasificacionResult(nombrePlanta, confianza, estadoTexto, estadoColor, consejo)
    }
}