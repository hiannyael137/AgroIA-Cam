package com.hypv.agroiacam

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object ApiHelper {

    // Emulador → 10.0.2.2 | Celular físico → IP de tu laptop
    var BASE_URL = "http://10.0.2.2:1880"

    private val client   = OkHttpClient()
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    // ── Guardar resultado IA ─────────────────────────────────────────────────
    fun guardarResultado(
        usuarioId: Int,
        planta:    String,
        estado:    String,
        confianza: Float,
        callback:  (ok: Boolean) -> Unit
    ) {
        val body = JSONObject().apply {
            put("usuario_id", usuarioId)
            put("resultado",  planta)
            put("estado",     estado)
            put("confianza",  confianza)
            put("metodo",     "IA")
        }.toString().toRequestBody(JSON_TYPE)

        post("$BASE_URL/guardarResultado", body, callback)
    }

    // ── Historial ─────────────────────────────────────────────────────────────
    fun obtenerHistorial(usuarioId: Int, callback: (JSONArray?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/getHistory?usuario_id=$usuarioId")
            .get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(null)
            override fun onResponse(call: Call, response: Response) {
                try { callback(JSONArray(response.body?.string())) }
                catch (e: Exception) { callback(null) }
            }
        })
    }

    // ── Guardar actividad ─────────────────────────────────────────────────────
    fun guardarActividad(planta: String, actividad: String, notas: String,
                         callback: (ok: Boolean) -> Unit) {
        val body = JSONObject().apply {
            put("planta",    planta)
            put("actividad", actividad)
            put("notas",     notas)
        }.toString().toRequestBody(JSON_TYPE)
        post("$BASE_URL/saveActivity", body, callback)
    }

    // ── Configurar ESP32 para una planta ──────────────────────────────────────
    fun configurarESP32(plantaId: Int, callback: (ok: Boolean) -> Unit) {
        val body = JSONObject().apply {
            put("planta_id", plantaId)
        }.toString().toRequestBody(JSON_TYPE)
        post("$BASE_URL/configESP32", body, callback)
    }

    // ── Obtener datos sensores de una planta ──────────────────────────────────
    fun obtenerDatosPlanta(plantaId: Int, callback: (JSONArray?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/getPlantData?planta_id=$plantaId")
            .get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(null)
            override fun onResponse(call: Call, response: Response) {
                try { callback(JSONArray(response.body?.string())) }
                catch (e: Exception) { callback(null) }
            }
        })
    }

    // ── Foto ESP32-CAM ────────────────────────────────────────────────────────
    fun obtenerFotoESP32(callback: (bytes: ByteArray?, error: String?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/esp32foto").get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) =
                callback(null, "Sin conexión: ${e.message}")
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) { callback(null, "Error: ${response.code}"); return }
                val bytes = response.body?.bytes()
                if (bytes == null || bytes.isEmpty()) callback(null, "ESP32-CAM no envió imagen")
                else callback(bytes, null)
            }
        })
    }

    // ── Helper interno ────────────────────────────────────────────────────────
    private fun post(url: String, body: RequestBody, callback: (Boolean) -> Unit) {
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(false)
            override fun onResponse(call: Call, response: Response) = callback(response.isSuccessful)
        })
    }
}