// ===============================
// MainActivity.kt
// app/src/main/java/com/hypv/agroiacam/MainActivity.kt
// ===============================

package com.hypv.agroiacam

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
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
    private lateinit var btnLogout: ImageButton
    private lateinit var btnProfile: ImageButton

    private val client = OkHttpClient()

    private val plantList =
        mutableListOf<Plant>()

    private lateinit var adapter: PlantAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        rvPlants =
            findViewById(R.id.rvPlants)

        btnAddPlant =
            findViewById(R.id.btnAddPlant)

        btnLogout =
            findViewById(R.id.btnLogout)

        btnProfile =
            findViewById(R.id.btnProfile)

        setupRecycler()

        // ==========================
        // REGISTRAR PLANTA
        // ==========================
        btnAddPlant.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    AddPlantActivity::class.java
                )
            )
        }

        // ==========================
        // PERFIL
        // ==========================
        btnProfile.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ProfileActivity::class.java
                )
            )
        }

        // ==========================
        // LOGOUT
        // ==========================
        btnLogout.setOnClickListener {

            val prefs =
                getSharedPreferences(
                    "agroia",
                    MODE_PRIVATE
                )

            prefs.edit().clear().apply()

            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )

            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        loadPlants()
    }

    // ==========================
    // RECYCLER
    // ==========================
    private fun setupRecycler() {

        adapter = PlantAdapter(
            this,
            plantList
        ) {

            loadPlants()
        }

        rvPlants.layoutManager =
            LinearLayoutManager(this)

        rvPlants.adapter = adapter
    }

    // ==========================
    // CARGAR PLANTAS
    // ==========================
    private fun loadPlants() {

        val prefs =
            getSharedPreferences(
                "agroia",
                MODE_PRIVATE
            )

        val usuarioId =
            prefs.getInt("usuario_id", 0)

        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/getPlants?usuario_id=$usuarioId")
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
                            this@MainActivity,
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

                            val plant = Plant(

                                id =
                                    obj.getInt("id"),

                                nombre_personalizado =
                                    obj.getString("nombre_personalizado"),

                                tipo_planta =
                                    obj.getString("tipo_planta"),

                                estado =
                                    obj.getString("estado"),

                                humedad =
                                    obj.getString("humedad"),

                                ultimo_riego =
                                    obj.getString("ultimo_riego"),

                                salud =
                                    obj.getInt("salud"),

                                imagen_url =
                                    obj.optString(
                                        "imagen_url",
                                        ""
                                    )
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