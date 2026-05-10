package com.hypv.agroiacam

import android.content.Context
import android.net.Uri
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiHelper {

    // Cambia esta IP si tu laptop cambia de red.
    // Para emulador Android Studio usa: http://10.0.2.2:1880
    var BASE_URL = "http:// 192.168.0.16:1880"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    fun guardarResultado(
        usuarioId: Int,
        plantaId: Int = 0,
        plantaRegistrada: String = "",
        claseDetectada: String,
        especieDetectada: String = "",
        problemaDetectado: String = "",
        estado: String,
        confianza: Float,
        solucion: String = "",
        validacion: String = "VALIDO",
        mensajeValidacion: String = "",
        imagenUrl: String = "",
        callback: (ok: Boolean) -> Unit
    ) {
        val confianzaNormalizada = if (confianza <= 1f) confianza else confianza / 100f

        val estadoLimpio = estado
            .replace("🟢", "")
            .replace("🟡", "")
            .replace("🔴", "")
            .trim()

        val body = JSONObject().apply {
            put("usuario_id", usuarioId)
            put("planta_id", plantaId)
            put("planta", plantaRegistrada.ifBlank { especieDetectada })
            put("planta_registrada", plantaRegistrada)
            put("resultado", claseDetectada)
            put("clase_detectada", claseDetectada)
            put("especie_detectada", especieDetectada)
            put("problema_detectado", problemaDetectado)
            put("estado", estadoLimpio)
            put("estado_salud", estadoLimpio)
            put("confianza", confianzaNormalizada)
            put("metodo", "IA Validada")
            put("solucion", solucion)
            put("validacion", validacion)
            put("mensaje_validacion", mensajeValidacion)
            put("imagen_url", imagenUrl)
        }.toString().toRequestBody(JSON_TYPE)

        post("$BASE_URL/guardarResultado", body, callback)
    }

    fun obtenerHistorial(usuarioId: Int, callback: (JSONArray?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/getHistory?usuario_id=$usuarioId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(null)

            override fun onResponse(call: Call, response: Response) {
                try {
                    val text = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        callback(null)
                        return
                    }
                    callback(JSONArray(text))
                } catch (e: Exception) {
                    callback(null)
                }
            }
        })
    }

    fun guardarActividad(
        usuarioId: Int,
        plantaId: Int,
        planta: String,
        actividad: String,
        notas: String,
        callback: (ok: Boolean) -> Unit
    ) {
        val body = JSONObject().apply {
            put("usuario_id", usuarioId)
            put("planta_id", plantaId)
            put("planta", planta)
            put("actividad", actividad)
            put("tipo_actividad", actividad)
            put("notas", notas)
        }.toString().toRequestBody(JSON_TYPE)

        post("$BASE_URL/saveActivity", body, callback)
    }

    fun configurarESP32(plantaId: Int, callback: (ok: Boolean) -> Unit) {
        val body = JSONObject().apply {
            put("planta_id", plantaId)
        }.toString().toRequestBody(JSON_TYPE)

        post("$BASE_URL/configESP32", body, callback)
    }

    fun obtenerDatosPlanta(plantaId: Int, callback: (JSONArray?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/getPlantData?planta_id=$plantaId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(null)

            override fun onResponse(call: Call, response: Response) {
                try {
                    val text = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        callback(null)
                        return
                    }
                    callback(JSONArray(text))
                } catch (e: Exception) {
                    callback(null)
                }
            }
        })
    }

    fun subirImagen(
        context: Context,
        uri: Uri,
        plantaId: Int? = null,
        callback: (ok: Boolean, filename: String, error: String?) -> Unit
    ) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                callback(false, "", "No se pudo leer la imagen")
                return
            }

            val fileName = "agroia_${System.currentTimeMillis()}.jpg"
            val builder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "imagen",
                    fileName,
                    bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )

            if (plantaId != null && plantaId > 0) {
                builder.addFormDataPart("planta_id", plantaId.toString())
            }

            val request = Request.Builder()
                .url("$BASE_URL/uploadImage")
                .post(builder.build())
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback(false, "", e.message ?: "Error de conexión")
                }

                override fun onResponse(call: Call, response: Response) {
                    val text = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        callback(false, "", "Servidor respondió ${response.code}: $text")
                        return
                    }

                    try {
                        val json = JSONObject(text)
                        val filename = json.optString("filename", "")
                        val imagenUrl = json.optString("imagen_url", "")
                        val finalName = when {
                            imagenUrl.isNotBlank() -> imagenUrl
                            filename.isNotBlank() -> "/uploads/$filename"
                            else -> ""
                        }

                        if (finalName.isBlank()) {
                            callback(false, "", "Node-RED no devolvió nombre de imagen")
                        } else {
                            callback(true, finalName, null)
                        }
                    } catch (e: Exception) {
                        callback(false, "", "Respuesta inválida: $text")
                    }
                }
            })
        } catch (e: Exception) {
            callback(false, "", e.message ?: "Error leyendo imagen")
        }
    }

    private fun post(url: String, body: RequestBody, callback: (Boolean) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(false)
            override fun onResponse(call: Call, response: Response) = callback(response.isSuccessful)
        })
    }
}
