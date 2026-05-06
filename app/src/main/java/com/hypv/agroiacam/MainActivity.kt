package com.hypv.agroiacam

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var rvPlants: RecyclerView
    private lateinit var btnAddPlant: Button
    private lateinit var btnLogout: ImageButton
    private lateinit var adapter: PlantAdapter

    private val client = OkHttpClient()
    private val plantList = mutableListOf<Plant>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // ← SIEMPRE primero

        rvPlants = findViewById(R.id.rvPlants)
        btnAddPlant = findViewById(R.id.btnAddPlant)
        btnLogout = findViewById(R.id.btnLogout)
        val btnProfile = findViewById<ImageButton>(R.id.btnProfile) // ← después del setContentView

        adapter = PlantAdapter(plantList)
        rvPlants.layoutManager = LinearLayoutManager(this)
        rvPlants.adapter = adapter

        btnAddPlant.setOnClickListener {
            startActivity(Intent(this, AddPlantActivity::class.java))
        }

        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro que deseas cerrar sesión?")
                .setPositiveButton("Sí, salir") { _, _ ->
                    val prefs = getSharedPreferences("agroia", MODE_PRIVATE)
                    prefs.edit().clear().apply()

                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadPlants()
    }

    private fun loadPlants() {
        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/getPlants")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error cargando plantas", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string()
                if (result != null) {
                    try {
                        val jsonArray = JSONArray(result)
                        plantList.clear()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val plant = Plant(
                                obj.getString("nombre_personalizado"),
                                obj.getString("estado"),
                                obj.getString("humedad"),
                                obj.getString("ultimo_riego")
                            )
                            plantList.add(plant)
                        }
                        runOnUiThread {
                            adapter.notifyDataSetChanged()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error al procesar datos", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }
}