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

class AddPlantActivity : AppCompatActivity() {

    private lateinit var spinnerPlantType: Spinner
    private lateinit var etPlantName: EditText
    private lateinit var btnSavePlant: Button

    private val client =
        OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_plant)

        spinnerPlantType =
            findViewById(R.id.spinnerPlantType)

        etPlantName =
            findViewById(R.id.etPlantName)

        btnSavePlant =
            findViewById(R.id.btnSavePlant)

        setupSpinner()

        btnSavePlant.setOnClickListener {

            savePlant()

        }
    }

    private fun setupSpinner() {

        val plants = listOf(

            "🌹 Rosa",
            "🌵 Sábila",
            "🌿 Menta",
            "🌸 Orquídea",
            "🪴 Helecho"

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

        spinnerPlantType.adapter =
            adapter
    }

    private fun savePlant() {

        val plantType =
            spinnerPlantType.selectedItem.toString()

        val customName =
            etPlantName.text.toString().trim()

        if(customName.isEmpty()) {

            Toast.makeText(
                this,
                "Ponle un nombre a tu planta",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val json =
            JSONObject()

        json.put(
            "tipo_planta",
            plantType
        )

        json.put(
            "nombre_personalizado",
            customName
        )

        val body =
            json.toString()
                .toRequestBody(
                    "application/json"
                        .toMediaType()
                )

        val request =
            Request.Builder()
                .url("${ApiHelper.BASE_URL}/addPlant")
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
                            this@AddPlantActivity,
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
                            this@AddPlantActivity,
                            "Planta registrada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()

                    }
                }
            })
    }
}