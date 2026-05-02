package com.hypv.agroiacam

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Todas las llamadas HTTP a Node-RED van aquí.
 *
 * IP del emulador  → 10.0.2.2  (apunta a localhost de tu laptop)
 * IP de teléfono físico → pon la IP de tu laptop en la misma red WiFi
 *   Ejemplo: "192.168.1.100"
 *
 * Node-RED corre en puerto 1880 por defecto.
 */
object ApiHelper {

    // ── Cambia esta IP cuando uses teléfono físico ──────────────────────────
    private const val BASE_URL = "http://10.0.2.2:1880"
    // ────────────────────────────────────────────────────────────────────────

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    // ── Resultado de llamadas HTTP ───────────────────────────────────────────
    data class Result(val success: Boolean, val message: String)

    // ── LOGIN ────────────────────────────────────────────────────────────────
    suspend fun login(usuario: String, password: String): Result =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("usuario", usuario)
                    .put("password", password)
                    .toString()
                    .toRequestBody(JSON)

                val req = Request.Builder()
                    .url("$BASE_URL/login")
                    .post(body)
                    .build()

                val resp = client.newCall(req).execute()
                val text = resp.body?.string() ?: ""

                if (resp.isSuccessful) Result(true, text)
                else Result(false, "Error ${resp.code}: $text")

            } catch (e: Exception) {
                Result(false, "Sin conexión: ${e.message}")
            }
        }

    // ── REGISTRO ─────────────────────────────────────────────────────────────
    suspend fun register(usuario: String, email: String, password: String): Result =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("usuario", usuario)
                    .put("email", email)
                    .put("password", password)
                    .toString()
                    .toRequestBody(JSON)

                val req = Request.Builder()
                    .url("$BASE_URL/register")
                    .post(body)
                    .build()

                val resp = client.newCall(req).execute()
                val text = resp.body?.string() ?: ""

                if (resp.isSuccessful) Result(true, text)
                else Result(false, "Error ${resp.code}: $text")

            } catch (e: Exception) {
                Result(false, "Sin conexión: ${e.message}")
            }
        }

    // ── GUARDAR RESULTADO IA ─────────────────────────────────────────────────
    suspend fun saveResult(
        usuario: String,
        planta: String,
        diagnostico: String,
        precision: Int,
        metodo: String          // "telefono" o "esp32"
    ): Result = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
                .put("usuario", usuario)
                .put("planta", planta)
                .put("diagnostico", diagnostico)
                .put("precision", precision)
                .put("metodo", metodo)
                .toString()
                .toRequestBody(JSON)

            val req = Request.Builder()
                .url("$BASE_URL/save_result")
                .post(body)
                .build()

            val resp = client.newCall(req).execute()
            val text = resp.body?.string() ?: ""

            if (resp.isSuccessful) Result(true, text)
            else Result(false, "Error ${resp.code}: $text")

        } catch (e: Exception) {
            Result(false, "Sin conexión: ${e.message}")
        }
    }

    // ── OBTENER HISTORIAL ────────────────────────────────────────────────────
    suspend fun getHistory(usuario: String): String =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("$BASE_URL/history?usuario=$usuario")
                    .get()
                    .build()

                val resp = client.newCall(req).execute()
                resp.body?.string() ?: "[]"

            } catch (e: Exception) {
                "[]"
            }
        }

    // ── SENSORES ESP32 (lectura en tiempo real) ──────────────────────────────
    suspend fun getSensors(): String =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("$BASE_URL/sensors")
                    .get()
                    .build()

                val resp = client.newCall(req).execute()
                resp.body?.string() ?: "{}"

            } catch (e: Exception) {
                "{}"
            }
        }

    // ── GUARDAR ACTIVIDAD PLANT CARE ─────────────────────────────────────────
    suspend fun saveCareActivity(
        usuario: String,
        planta: String,
        actividad: String,
        notas: String
    ): Result = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
                .put("usuario", usuario)
                .put("planta", planta)
                .put("actividad", actividad)
                .put("notas", notas)
                .toString()
                .toRequestBody(JSON)

            val req = Request.Builder()
                .url("$BASE_URL/care")
                .post(body)
                .build()

            val resp = client.newCall(req).execute()
            val text = resp.body?.string() ?: ""

            if (resp.isSuccessful) Result(true, text)
            else Result(false, "Error ${resp.code}: $text")

        } catch (e: Exception) {
            Result(false, "Sin conexión: ${e.message}")
        }
    }
}