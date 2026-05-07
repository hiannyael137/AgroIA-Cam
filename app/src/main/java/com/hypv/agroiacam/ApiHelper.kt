package com.hypv.agroiacam

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object ApiHelper {

    // =========================================
    // URL BASE NODE-RED
    // =========================================
    var BASE_URL = "http://10.0.2.2:1880"

    private val client = OkHttpClient()

    private val JSON_TYPE =
        "application/json; charset=utf-8".toMediaType()

    // =========================================
    // GUARDAR RESULTADO IA
    // =========================================
    fun guardarResultado(
        usuarioId: Int,
        planta: String,
        estado: String,
        confianza: Float,
        callback: (ok: Boolean) -> Unit
    ) {

        val json = JSONObject()

        json.put("usuario_id", usuarioId)
        json.put("resultado", planta)
        json.put("estado", estado)
        json.put("confianza", confianza)
        json.put("metodo", "IA")

        val body = json.toString()
            .toRequestBody(JSON_TYPE)

        val request = Request.Builder()
            .url("$BASE_URL/guardarResultado")
            .post(body)
            .build()

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    callback(false)
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    callback(response.isSuccessful)
                }
            })
    }

    // =========================================
    // OBTENER HISTORIAL
    // =========================================
    fun obtenerHistorial(
        usuarioId: Int,
        callback: (JSONArray?) -> Unit
    ) {

        val request = Request.Builder()
            .url("$BASE_URL/getHistory?usuario_id=$usuarioId")
            .get()
            .build()

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    callback(null)
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    try {

                        val result =
                            response.body?.string()

                        val jsonArray =
                            JSONArray(result)

                        callback(jsonArray)

                    } catch (e: Exception) {

                        callback(null)
                    }
                }
            })
    }

    // =========================================
    // GUARDAR ACTIVIDAD
    // =========================================
    fun guardarActividad(
        planta: String,
        actividad: String,
        notas: String,
        callback: (ok: Boolean) -> Unit
    ) {

        val json = JSONObject()

        json.put("planta", planta)
        json.put("actividad", actividad)
        json.put("notas", notas)

        val body = json.toString()
            .toRequestBody(JSON_TYPE)

        val request = Request.Builder()
            .url("$BASE_URL/saveActivity")
            .post(body)
            .build()

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    callback(false)
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    callback(response.isSuccessful)
                }
            })
    }

    // =========================================
    // OBTENER FOTO ESP32-CAM
    // =========================================
    fun obtenerFotoESP32(
        callback: (
            bytes: ByteArray?,
            error: String?
        ) -> Unit
    ) {

        val request = Request.Builder()
            .url("$BASE_URL/esp32foto")
            .get()
            .build()

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    callback(
                        null,
                        "Sin conexión: ${e.message}"
                    )
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    if (!response.isSuccessful) {

                        callback(
                            null,
                            "Error del servidor"
                        )

                        return
                    }

                    val bytes =
                        response.body?.bytes()

                    if (
                        bytes == null ||
                        bytes.isEmpty()
                    ) {

                        callback(
                            null,
                            "ESP32-CAM no envió imagen"
                        )

                    } else {

                        callback(bytes, null)
                    }
                }
            })
    }

    // =========================================
    // OBTENER SENSORES
    // =========================================
    fun obtenerSensores(
        callback: (
            humedad: Int,
            temperatura: Int
        ) -> Unit
    ) {

        val request = Request.Builder()
            .url("$BASE_URL/ultimoSensor")
            .get()
            .build()

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    callback(0, 0)
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    try {

                        val result =
                            response.body?.string()

                        val json =
                            JSONObject(result!!)

                        val humedad =
                            json.optInt(
                                "humedad_suelo",
                                0
                            )

                        val temperatura =
                            json.optInt(
                                "temperatura",
                                0
                            )

                        callback(
                            humedad,
                            temperatura
                        )

                    } catch (e: Exception) {

                        callback(0, 0)
                    }
                }
            })
    }
}