package com.hypv.agroiacam

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)

        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)

        // IR A REGISTRO
        btnRegister.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    RegisterActivity::class.java
                )
            )
        }

        // LOGIN
        btnLogin.setOnClickListener {

            loginUser()
        }
    }

    private fun loginUser() {

        val email =
            etUsername.text.toString().trim()

        val password =
            etPassword.text.toString().trim()

        // VALIDAR CAMPOS
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

        // JSON
        val json = JSONObject()

        json.put("email", email)
        json.put("password", password)

        val body =
            json.toString().toRequestBody(
                "application/json".toMediaType()
            )

        // REQUEST
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

                        Toast.makeText(
                            this@LoginActivity,
                            "Error conectando con Node-RED",
                            Toast.LENGTH_LONG
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

                        try {

                            // MOSTRAR RESPUESTA COMPLETA
                            Toast.makeText(
                                this@LoginActivity,
                                "Respuesta: $result",
                                Toast.LENGTH_LONG
                            ).show()

                            // VALIDAR VACIO
                            if (
                                result == null ||
                                result.isEmpty()
                            ) {

                                Toast.makeText(
                                    this@LoginActivity,
                                    "Respuesta vacía del servidor",
                                    Toast.LENGTH_LONG
                                ).show()

                                return@runOnUiThread
                            }

                            // JSON
                            val jsonResponse =
                                JSONObject(result)

                            // LOGIN OK
                            if (
                                jsonResponse.optBoolean(
                                    "success",
                                    false
                                )
                            ) {

                                val usuarioId =
                                    jsonResponse.optInt(
                                        "usuario_id",
                                        0
                                    )

                                val nombre =
                                    jsonResponse.optString(
                                        "nombre",
                                        ""
                                    )

                                // GUARDAR SESION
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

                                // ABRIR MAIN
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
                                    jsonResponse.optString(
                                        "message",
                                        "Correo o contraseña incorrectos"
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                        } catch (e: Exception) {

                            e.printStackTrace()

                            Toast.makeText(
                                this@LoginActivity,
                                "Error procesando:\n$result",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            })
    }
}