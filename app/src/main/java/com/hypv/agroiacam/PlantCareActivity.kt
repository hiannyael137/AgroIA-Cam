package com.hypv.agroiacam

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException

class PlantCareActivity : AppCompatActivity() {

    private data class PlantOption(
        val id: Int,
        val nombre: String
    ) {
        override fun toString(): String = nombre
    }

    private lateinit var spinnerPlant: Spinner
    private lateinit var spinnerActivity: Spinner
    private lateinit var etNotes: EditText
    private lateinit var btnSave: Button

    private val client = OkHttpClient()
    private val plantList = mutableListOf<PlantOption>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_care)

        spinnerPlant = findViewById(R.id.spinnerPlant)
        spinnerActivity = findViewById(R.id.spinnerActivity)
        etNotes = findViewById(R.id.etNotes)
        btnSave = findViewById(R.id.btnSaveActivity)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        setupActivitySpinner()
        loadPlants()

        btnSave.setOnClickListener { saveActivity() }
    }

    private fun loadPlants() {
        val usuarioId = getSharedPreferences("agroia", MODE_PRIVATE).getInt("usuario_id", 0)

        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/getPlants?usuario_id=$usuarioId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@PlantCareActivity, "Error cargando plantas", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string().orEmpty()
                try {
                    val jsonArray = JSONArray(result)
                    val tempPlants = mutableListOf<PlantOption>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.optInt("id", 0)
                        val nombre = obj.optString("nombre_personalizado", "Mi planta")
                        tempPlants.add(PlantOption(id, nombre))
                    }

                    runOnUiThread {
                        plantList.clear()
                        plantList.addAll(tempPlants)
                        setupPlantSpinner()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@PlantCareActivity, "No se pudieron leer tus plantas", Toast.LENGTH_SHORT).show()
                    }
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
            "Riego",
            "Poda",
            "Fertilizante",
            "Cambio de lugar",
            "Revisión IA"
        )
        val adapter = ArrayAdapter(this, R.layout.spinner_item, activities)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinnerActivity.adapter = adapter
    }

    private fun saveActivity() {
        if (plantList.isEmpty() || spinnerPlant.selectedItem == null) {
            Toast.makeText(this, "Primero registra una planta", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("agroia", MODE_PRIVATE)
        val usuarioId = prefs.getInt("usuario_id", 0)
        val plantOption = spinnerPlant.selectedItem as PlantOption
        val activity = spinnerActivity.selectedItem.toString()
        val notes = etNotes.text.toString().trim()

        if (usuarioId <= 0) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        btnSave.isEnabled = false

        ApiHelper.guardarActividad(
            usuarioId = usuarioId,
            plantaId = plantOption.id,
            planta = plantOption.nombre,
            actividad = activity,
            notas = notes
        ) { ok ->
            runOnUiThread {
                btnSave.isEnabled = true
                if (ok) {
                    Toast.makeText(this, "Actividad guardada", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "No se pudo guardar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
