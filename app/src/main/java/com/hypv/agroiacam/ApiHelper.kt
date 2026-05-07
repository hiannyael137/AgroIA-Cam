package com.hypv.agroiacam

// app/src/main/java/com/hypv/agroiacam/ApiHelper.kt

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object ApiHelper {

    // IP configurable — se guarda en SharedPreferences
    // Por defecto apunta al emulador. En celular físico del compañero
    // debe cambiarse a la IP de la laptop desde Configuración.
    var BASE_URL = "http://10.0.2.2:1880"

    private val client = OkHttpClient()
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    // ── Guardar resultado de análisis en BD ───────────────
    fun guardarResultado(
        usuarioId: Int,
        planta:    String,
        estado:    String,
        confianza: Float,
        callback:  (ok: Boolean) -> Unit
    ) {
        val body = JSONObject().apply {
            put("usuario_id", usuarioId)
            put("resultado",  "$planta — $estado")
            put("confianza",  confianza)
            put("metodo",     "camara")
        }.toString().toRequestBody(JSON_TYPE)

        val request = Request.Builder()
            .url("$BASE_URL/guardar")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: okio.IOException) = callback(false)
            override fun onResponse(call: Call, response: Response) {
                try {
                    val r = JSONObject(response.body?.string() ?: "")
                    callback(r.optBoolean("ok", false))
                } catch (e: Exception) { callback(false) }
            }
        })
    }
}