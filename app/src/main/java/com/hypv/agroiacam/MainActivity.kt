package com.hypv.agroiacam

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var rvPlants: RecyclerView
    private lateinit var btnAddPlant: Button
    private lateinit var adapter: PlantAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        rvPlants =
            findViewById(R.id.rvPlants)

        btnAddPlant =
            findViewById(R.id.btnAddPlant)

        setupRecycler()

        btnAddPlant.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    AddPlantActivity::class.java
                )
            )
        }
    }

    private fun setupRecycler() {

        val plants = listOf(

            Plant(
                "🌹 Rosa Patio",
                "Saludable",
                "65%",
                "Hace 2 días"
            ),

            Plant(
                "🌵 Sábila Cocina",
                "En Riesgo",
                "28%",
                "Hace 5 días"
            )

        )

        adapter =
            PlantAdapter(plants)

        rvPlants.layoutManager =
            LinearLayoutManager(this)

        rvPlants.adapter =
            adapter
    }
}