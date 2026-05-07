package com.hypv.agroiacam

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ProfileActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etPasswordActual: EditText
    private lateinit var etPasswordNueva: EditText
    private lateinit var btnGuardar: Button

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_profile)

        etNombre =
            findViewById(R.id.etNombre)

        etPasswordActual =
            findViewById(R.id.etPasswordActual)

        etPasswordNueva =
            findViewById(R.id.etPasswordNueva)

        btnGuardar =
            findViewById(R.id.btnGuardar)

        // =========================
        // REGRESAR
        // =========================
        findViewById<ImageButton>(R.id.btnBack)
            .setOnClickListener {

                finish()
            }

        // =========================
        // CARGAR NOMBRE
        // =========================
        val prefs =
            getSharedPreferences(
                "agroia",
                MODE_PRIVATE
            )

        etNombre.setText(
            prefs.getString(
                "nombre",
                ""
            )
        )

        // =========================
        // GUARDAR
        // =========================
        btnGuardar.setOnClickListener {

            guardarCambios()
        }
    }

    // =========================
    // GUARDAR CAMBIOS
    // =========================
    private fun guardarCambios() {

        val nombre =
            etNombre.text.toString().trim()

        val passwordActual =
            etPasswordActual.text.toString().trim()

        val passwordNueva =
            etPasswordNueva.text.toString().trim()

        // =========================
        // VALIDACIONES
        // =========================
        if (nombre.isEmpty()) {

            Toast.makeText(
                this,
                "El nombre no puede estar vacío",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (passwordActual.isEmpty()) {

            Toast.makeText(
                this,
                "Ingresa tu contraseña actual",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // PASSWORD NUEVA
        if (
            passwordNueva.isNotEmpty() &&
            passwordNueva.length < 6
        ) {

            Toast.makeText(
                this,
                "La nueva contraseña debe tener mínimo 6 caracteres",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        btnGuardar.isEnabled = false

        val prefs =
            getSharedPreferences(
                "agroia",
                MODE_PRIVATE
            )

        val usuarioId =
            prefs.getInt(
                "usuario_id",
                0
            )

        // =========================
        // JSON
        // =========================
        val json = JSONObject()

        json.put("usuario_id", usuarioId)
        json.put("nombre", nombre)
        json.put("password_actual", passwordActual)
        json.put("password_nueva", passwordNueva)

        val body =
            json.toString()
                .toRequestBody(
                    "application/json"
                        .toMediaType()
                )

        // =========================
        // REQUEST
        // =========================
        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/updateProfile")
            .post(body)
            .build()

        client.newCall(request)
            .enqueue(object : Callback {

                // =========================
                // ERROR
                // =========================
                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    runOnUiThread {

                        btnGuardar.isEnabled = true

                        Toast.makeText(
                            this@ProfileActivity,
                            "Error conectando con Node-RED",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                // =========================
                // RESPUESTA
                // =========================
                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    val result =
                        response.body?.string()

                    runOnUiThread {

                        btnGuardar.isEnabled = true

                        try {

                            val jsonResponse =
                                JSONObject(result!!)

                            if (
                                jsonResponse.getBoolean(
                                    "success"
                                )
                            ) {

                                prefs.edit()
                                    .putString(
                                        "nombre",
                                        nombre
                                    )
                                    .apply()

                                Toast.makeText(
                                    this@ProfileActivity,
                                    "Perfil actualizado",
                                    Toast.LENGTH_SHORT
                                ).show()

                                finish()

                            } else {

                                Toast.makeText(
                                    this@ProfileActivity,
                                    jsonResponse.getString(
                                        "message"
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        } catch (e: Exception) {

                            Toast.makeText(
                                this@ProfileActivity,
                                "Error procesando respuesta",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })
    }
}