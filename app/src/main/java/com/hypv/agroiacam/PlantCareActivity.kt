package com.hypv.agroiacam

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class PlantCareActivity : AppCompatActivity() {

    private lateinit var spinnerPlant: Spinner
    private lateinit var spinnerActivity: Spinner
    private lateinit var etNotes: EditText
    private lateinit var btnSave: Button

    private val client = OkHttpClient()
    private val plantList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_care)

        spinnerPlant = findViewById(R.id.spinnerPlant)
        spinnerActivity = findViewById(R.id.spinnerActivity)
        etNotes = findViewById(R.id.etNotes)
        btnSave = findViewById(R.id.btnSaveActivity)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        loadPlants()
        setupActivitySpinner()

        btnSave.setOnClickListener {
            saveActivity()
        }
    }

    private fun loadPlants() {
        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/getPlants")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@PlantCareActivity, "Error cargando plantas", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string()
                if (result != null) {
                    val jsonArray = JSONArray(result)
                    plantList.clear()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        plantList.add(obj.getString("nombre_personalizado"))
                    }
                    runOnUiThread { setupPlantSpinner() }
                }
            }
        })
    }

    private fun setupPlantSpinner() {
        val adapter = ArrayAdapter(this, R.layout.spinner_item, plantList)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinnerPlant.adapter = adapter
    }

    private fun setupActivitySpinner() {
        val activities = listOf(
            "💧 Riego",
            "✂️ Poda",
            "🧪 Fertilizante",
            "☀️ Cambio de lugar",
            "📷 Revisión IA"
        )
        val adapter = ArrayAdapter(this, R.layout.spinner_item, activities)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinnerActivity.adapter = adapter
    }

    private fun saveActivity() {
        val plant = spinnerPlant.selectedItem.toString()
        val activity = spinnerActivity.selectedItem.toString()
        val notes = etNotes.text.toString()

        val json = JSONObject()
        json.put("planta", plant)
        json.put("actividad", activity)
        json.put("notas", notes)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/saveActivity")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@PlantCareActivity, "Error conectando con Node-RED", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@PlantCareActivity, "Actividad guardada", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        })
    }
}