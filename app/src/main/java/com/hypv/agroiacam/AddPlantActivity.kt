package com.hypv.agroiacam

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class AddPlantActivity : AppCompatActivity() {

    private lateinit var spinnerPlantType: Spinner
    private lateinit var etPlantName: EditText
    private lateinit var btnSavePlant: Button

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_plant)

        spinnerPlantType = findViewById(R.id.spinnerPlantType)
        etPlantName = findViewById(R.id.etPlantName)
        btnSavePlant = findViewById(R.id.btnSavePlant)

        setupSpinner()

        btnSavePlant.setOnClickListener {
            savePlant()
        }
    }

    private fun setupSpinner() {

        val plants = listOf(
            " Rosa",
            "Sábila",
            "Menta",
            " Orquídea",
            "Helecho"
        )

        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            plants
        )

        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinnerPlantType.adapter = adapter
    }

    private fun savePlant() {

        val plantType = spinnerPlantType.selectedItem.toString()
        val customName = etPlantName.text.toString().trim()

        if (customName.isEmpty()) {
            Toast.makeText(
                this,
                "Ponle un nombre a tu planta",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val json = JSONObject()

        json.put("tipo_planta", plantType)
        json.put("nombre_personalizado", customName)
        json.put("estado", "Saludable")
        json.put("humedad", "0%")
        json.put("ultima_actividad", "Sin registro")

        val body = json.toString().toRequestBody(
            "application/json".toMediaType()
        )

        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/addPlant")
            .post(body)
            .build()

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@AddPlantActivity,
                            "Error conectando con Node-RED",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        Toast.makeText(
                            this@AddPlantActivity,
                            "Planta guardada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            })
    }
}