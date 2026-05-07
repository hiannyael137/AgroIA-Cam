package com.hypv.agroiacam

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class PlantCareActivity : AppCompatActivity() {

    private lateinit var spinnerPlant: Spinner
    private lateinit var spinnerActivity: Spinner
    private lateinit var etNotes: EditText
    private lateinit var btnSave: Button

    private val client = OkHttpClient()

    private val plantList =
        mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_plant_care)

        spinnerPlant =
            findViewById(R.id.spinnerPlant)

        spinnerActivity =
            findViewById(R.id.spinnerActivity)

        etNotes =
            findViewById(R.id.etNotes)

        btnSave =
            findViewById(R.id.btnSaveActivity)

        // =========================
        // BOTON REGRESAR
        // =========================
        findViewById<ImageButton>(R.id.btnBack)
            .setOnClickListener {

                finish()
            }

        // =========================
        // CARGAR PLANTAS
        // =========================
        loadPlants()

        // =========================
        // ACTIVIDADES
        // =========================
        setupActivitySpinner()

        // =========================
        // GUARDAR
        // =========================
        btnSave.setOnClickListener {

            saveActivity()
        }
    }

    // =========================
    // CARGAR PLANTAS USUARIO
    // =========================
    private fun loadPlants() {

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

        val request = Request.Builder()
            .url(
                "${ApiHelper.BASE_URL}/getPlants?usuario_id=$usuarioId"
            )
            .get()
            .build()

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    runOnUiThread {

                        Toast.makeText(
                            this@PlantCareActivity,
                            "Error cargando plantas",
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

                    if (result != null) {

                        val jsonArray =
                            JSONArray(result)

                        plantList.clear()

                        for (i in 0 until jsonArray.length()) {

                            val obj =
                                jsonArray.getJSONObject(i)

                            plantList.add(
                                obj.getString(
                                    "nombre_personalizado"
                                )
                            )
                        }

                        runOnUiThread {

                            setupPlantSpinner()
                        }
                    }
                }
            })
    }

    // =========================
    // SPINNER PLANTAS
    // =========================
    private fun setupPlantSpinner() {

        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            plantList
        )

        adapter.setDropDownViewResource(
            R.layout.spinner_item
        )

        spinnerPlant.adapter = adapter
    }

    // =========================
    // SPINNER ACTIVIDADES
    // =========================
    private fun setupActivitySpinner() {

        val activities = listOf(

            "💧 Riego",

            "✂️ Poda",

            "🧪 Fertilizante",

            "☀️ Cambio de lugar",

            "📷 Revisión IA"
        )

        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            activities
        )

        adapter.setDropDownViewResource(
            R.layout.spinner_item
        )

        spinnerActivity.adapter = adapter
    }

    // =========================
    // GUARDAR ACTIVIDAD
    // =========================
    private fun saveActivity() {

        val plant =
            spinnerPlant.selectedItem.toString()

        val activity =
            spinnerActivity.selectedItem.toString()

        val notes =
            etNotes.text.toString().trim()

        // =========================
        // VALIDAR
        // =========================
        if (plant.isEmpty()) {

            Toast.makeText(
                this,
                "Selecciona una planta",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        btnSave.isEnabled = false

        // =========================
        // JSON
        // =========================
        val json = JSONObject()

        json.put("planta", plant)

        json.put("actividad", activity)

        json.put("notas", notes)

        // =========================
        // BODY
        // =========================
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
            .url("${ApiHelper.BASE_URL}/saveActivity")
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

                        btnSave.isEnabled = true

                        Toast.makeText(
                            this@PlantCareActivity,
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

                    runOnUiThread {

                        btnSave.isEnabled = true

                        if (response.isSuccessful) {

                            Toast.makeText(
                                this@PlantCareActivity,
                                "Actividad guardada",
                                Toast.LENGTH_SHORT
                            ).show()

                            finish()

                        } else {

                            Toast.makeText(
                                this@PlantCareActivity,
                                "No se pudo guardar",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })
    }
}