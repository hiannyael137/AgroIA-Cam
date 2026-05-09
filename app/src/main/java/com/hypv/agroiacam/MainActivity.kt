package com.hypv.agroiacam

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var rvPlants: RecyclerView
    private lateinit var btnAddPlant: Button
    private lateinit var btnLogout: ImageButton
    private lateinit var btnProfile: ImageButton

    private val client = OkHttpClient()
    private val plantList = mutableListOf<Plant>()
    private lateinit var adapter: PlantAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvPlants = findViewById(R.id.rvPlants)
        btnAddPlant = findViewById(R.id.btnAddPlant)
        btnLogout = findViewById(R.id.btnLogout)
        btnProfile = findViewById(R.id.btnProfile)

        setupRecycler()

        btnAddPlant.setOnClickListener {
            startActivity(Intent(this, AddPlantActivity::class.java))
        }

        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnLogout.setOnClickListener {
            getSharedPreferences("agroia", MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadPlants()
    }

    private fun setupRecycler() {
        adapter = PlantAdapter(plantList) { plant, position ->
            deletePlant(plant, position)
        }
        rvPlants.layoutManager = LinearLayoutManager(this)
        rvPlants.adapter = adapter
    }

    private fun loadPlants() {
        val usuarioId = getSharedPreferences("agroia", MODE_PRIVATE).getInt("usuario_id", 0)

        if (usuarioId <= 0) {
            Toast.makeText(this, "Sesión no válida, inicia sesión otra vez", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/getPlants?usuario_id=$usuarioId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error cargando plantas", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string().orEmpty()

                try {
                    val jsonArray = JSONArray(result)
                    val tempList = mutableListOf<Plant>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        tempList.add(
                            Plant(
                                id = obj.optInt("id", 0),
                                nombre_personalizado = obj.optString("nombre_personalizado", "Mi planta"),
                                tipo_planta = obj.optString("tipo_planta", "planta"),
                                estado = obj.optString("estado", "Saludable"),
                                humedad = formatPercent(obj.optString("humedad", "--")),
                                ultimo_riego = obj.optString("ultimo_riego", "Sin registro"),
                                salud = readSalud(obj),
                                imagen_url = obj.optString("imagen_url", "")
                            )
                        )
                    }

                    runOnUiThread {
                        plantList.clear()
                        plantList.addAll(tempList)
                        adapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Respuesta inválida de Node-RED", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun readSalud(obj: JSONObject): Int {
        val raw = obj.optString("salud", "100").trim()
        return when {
            raw.equals("Saludable", ignoreCase = true) -> 100
            raw.equals("En riesgo", ignoreCase = true) -> 30
            raw.equals("Monitorear", ignoreCase = true) -> 60
            raw.endsWith("%") -> raw.replace("%", "").trim().toIntOrNull() ?: 100
            else -> raw.toIntOrNull() ?: obj.optInt("salud", 100)
        }.coerceIn(0, 100)
    }

    private fun formatPercent(value: String): String {
        val v = value.trim()
        if (v.isEmpty() || v == "null" || v == "--") return "--%"
        return if (v.endsWith("%")) v else "$v%"
    }

    private fun deletePlant(plant: Plant, position: Int) {
        val json = JSONObject().apply {
            put("id", plant.id)
            put("planta_id", plant.id)
        }

        val body = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/deletePlant")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "No se pudo eliminar", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        if (position in plantList.indices) {
                            plantList.removeAt(position)
                            adapter.notifyItemRemoved(position)
                        } else {
                            loadPlants()
                        }
                        Toast.makeText(this@MainActivity, "Planta eliminada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Error eliminando planta", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
