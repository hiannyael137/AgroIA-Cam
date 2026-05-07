package com.hypv.agroiacam

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // =========================
        // VERIFICAR SESIÓN
        // =========================
        val prefs = getSharedPreferences(
            "agroia",
            MODE_PRIVATE
        )

        val usuarioId =
            prefs.getInt("usuario_id", -1)

        if (usuarioId != -1) {

            startActivity(
                Intent(
                    this,
                    MainActivity::class.java
                )
            )

            finish()

            return
        }

        etUsername =
            findViewById(R.id.etUsername)

        etPassword =
            findViewById(R.id.etPassword)

        btnLogin =
            findViewById(R.id.btnLogin)

        btnRegister =
            findViewById(R.id.btnRegister)

        // =========================
        // IR A REGISTER
        // =========================
        btnRegister.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    RegisterActivity::class.java
                )
            )
        }

        // =========================
        // LOGIN
        // =========================
        btnLogin.setOnClickListener {

            loginUser()
        }
    }

    // =========================
    // LOGIN USER
    // =========================
    private fun loginUser() {

        val email =
            etUsername.text.toString().trim()

        val password =
            etPassword.text.toString().trim()

        // =========================
        // VALIDAR CAMPOS
        // =========================
        if (
            email.isEmpty() ||
            password.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Completa todos los campos",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // =========================
        // VALIDAR GMAIL
        // =========================
        if (!email.endsWith("@gmail.com")) {

            Toast.makeText(
                this,
                "Ingresa un correo Gmail válido",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // =========================
        // DESACTIVAR BOTÓN
        // =========================
        btnLogin.isEnabled = false
        btnLogin.text = "Ingresando..."

        val json = JSONObject()

        json.put("email", email)
        json.put("password", password)

        val body = json.toString()
            .toRequestBody(
                "application/json".toMediaType()
            )

        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/login")
            .post(body)
            .build()

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    runOnUiThread {

                        btnLogin.isEnabled = true
                        btnLogin.text = "Iniciar sesión"

                        Toast.makeText(
                            this@LoginActivity,
                            "Error conectando con Node-RED",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    val result =
                        response.body?.string()

                    runOnUiThread {

                        btnLogin.isEnabled = true
                        btnLogin.text = "Iniciar sesión"

                        try {

                            val jsonResponse =
                                JSONObject(result!!)

                            if (
                                jsonResponse.getBoolean(
                                    "success"
                                )
                            ) {

                                // =========================
                                // GUARDAR SESIÓN
                                // =========================
                                val usuarioId =
                                    jsonResponse.getInt(
                                        "usuario_id"
                                    )

                                val nombre =
                                    jsonResponse.getString(
                                        "nombre"
                                    )

                                val prefs =
                                    getSharedPreferences(
                                        "agroia",
                                        MODE_PRIVATE
                                    )

                                prefs.edit()
                                    .putInt(
                                        "usuario_id",
                                        usuarioId
                                    )
                                    .putString(
                                        "nombre",
                                        nombre
                                    )
                                    .apply()

                                Toast.makeText(
                                    this@LoginActivity,
                                    "Bienvenido $nombre",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // =========================
                                // ABRIR MAIN
                                // =========================
                                startActivity(
                                    Intent(
                                        this@LoginActivity,
                                        MainActivity::class.java
                                    )
                                )

                                finish()

                            } else {

                                Toast.makeText(
                                    this@LoginActivity,
                                    "Correo o contraseña incorrectos",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        } catch (e: Exception) {

                            Toast.makeText(
                                this@LoginActivity,
                                "Error al procesar respuesta",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })
    }
}