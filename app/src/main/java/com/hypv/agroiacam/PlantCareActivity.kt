package com.hypv.agroiacam

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
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

class PlantCareActivity : AppCompatActivity() {

    private lateinit var spinnerPlant: Spinner
    private lateinit var spinnerActivity: Spinner
    private lateinit var etNotes: EditText
    private lateinit var btnSave: Button

    private val client =
        OkHttpClient()

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

        setupPlantSpinner()

        setupActivitySpinner()

        btnSave.setOnClickListener {

            saveActivity()

        }
    }

    private fun setupPlantSpinner() {

        val plants = listOf(

            "🌹 Rosa Patio",
            "🌵 Sábila Cocina"

        )

        val adapter =
            ArrayAdapter(
                this,
                R.layout.spinner_item,
                plants
            )

        adapter.setDropDownViewResource(
            R.layout.spinner_item
        )

        spinnerPlant.adapter =
            adapter
    }

    private fun setupActivitySpinner() {

        val activities = listOf(

            "💧 Riego",
            "✂️ Poda",
            "🧪 Fertilizante",
            "☀️ Cambio de lugar",
            "📷 Revisión IA"

        )

        val adapter =
            ArrayAdapter(
                this,
                R.layout.spinner_item,
                activities
            )

        adapter.setDropDownViewResource(
            R.layout.spinner_item
        )

        spinnerActivity.adapter =
            adapter
    }

    private fun saveActivity() {

        val plant =
            spinnerPlant.selectedItem.toString()

        val activity =
            spinnerActivity.selectedItem.toString()

        val notes =
            etNotes.text.toString()

        val json =
            JSONObject()

        json.put(
            "planta",
            plant
        )

        json.put(
            "actividad",
            activity
        )

        json.put(
            "notas",
            notes
        )

        val body =
            json.toString()
                .toRequestBody(
                    "application/json"
                        .toMediaType()
                )

        val request =
            Request.Builder()
                .url("${ApiHelper.BASE_URL}/saveActivity")
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
                            this@PlantCareActivity,
                            "Error conectando con Node-RED",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    runOnUiThread {

                        Toast.makeText(
                            this@PlantCareActivity,
                            "Actividad guardada",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()

                    }
                }
            })
    }
}