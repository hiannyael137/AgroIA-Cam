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

class RegisterActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnCreate: Button

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_register)

        etName =
            findViewById(R.id.etName)

        etEmail =
            findViewById(R.id.etEmail)

        etPassword =
            findViewById(R.id.etPassword)

        btnCreate =
            findViewById(R.id.btnCreateAccount)

        // =========================
        // BOTON REGRESAR
        // =========================
        findViewById<ImageButton>(R.id.btnBack)
            .setOnClickListener {

                finish()
            }

        // =========================
        // CREAR CUENTA
        // =========================
        btnCreate.setOnClickListener {

            registerUser()
        }
    }

    // =========================
    // REGISTRAR USUARIO
    // =========================
    private fun registerUser() {

        val name =
            etName.text.toString().trim()

        val email =
            etEmail.text.toString().trim()

        val password =
            etPassword.text.toString().trim()

        // =========================
        // VALIDACIONES
        // =========================
        if (
            name.isEmpty() ||
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

        // SOLO GMAIL
        if (!email.endsWith("@gmail.com")) {

            Toast.makeText(
                this,
                "Solo se permiten cuentas Gmail",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // PASSWORD MINIMA
        if (password.length < 6) {

            Toast.makeText(
                this,
                "La contraseña debe tener mínimo 6 caracteres",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // DESACTIVAR BOTON
        btnCreate.isEnabled = false

        // =========================
        // JSON
        // =========================
        val json = JSONObject()

        json.put("nombre", name)
        json.put("email", email)
        json.put("password", password)

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
            .url("${ApiHelper.BASE_URL}/register")
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

                        btnCreate.isEnabled = true

                        Toast.makeText(
                            this@RegisterActivity,
                            "Error de conexión con Node-RED",
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

                    runOnUiThread {

                        btnCreate.isEnabled = true

                        if (response.isSuccessful) {

                            Toast.makeText(
                                this@RegisterActivity,
                                "Cuenta creada correctamente",
                                Toast.LENGTH_SHORT
                            ).show()

                            finish()

                        } else {

                            Toast.makeText(
                                this@RegisterActivity,
                                "No se pudo crear la cuenta",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })
    }
}