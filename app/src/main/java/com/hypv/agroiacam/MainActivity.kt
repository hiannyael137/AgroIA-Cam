package com.hypv.agroiacam

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var rvPlants: RecyclerView
    private lateinit var btnAddPlant: Button
    private lateinit var adapter: PlantAdapter

    private val client = OkHttpClient()
    private val plantList = mutableListOf<Plant>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        rvPlants = findViewById(R.id.rvPlants)
        btnAddPlant = findViewById(R.id.btnAddPlant)

        adapter = PlantAdapter(plantList)

        rvPlants.layoutManager = LinearLayoutManager(this)
        rvPlants.adapter = adapter

        btnAddPlant.setOnClickListener {
            startActivity(Intent(this, AddPlantActivity::class.java))
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

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Error cargando plantas",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {

                    val result = response.body?.string()

                    if (result != null) {

                        val jsonArray = JSONArray(result)

                        plantList.clear()

                        for (i in 0 until jsonArray.length()) {

                            val obj = jsonArray.getJSONObject(i)

                            val plant = Plant(
                                obj.getString("nombre_personalizado"),
                                obj.getString("estado"),
                                obj.getString("humedad"),
                                obj.getString("ultima_actividad")
                            )

                            plantList.add(plant)
                        }

                        runOnUiThread {
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            })
    }
}