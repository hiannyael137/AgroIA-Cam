package com.hypv.agroiacam

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hypv.agroiacam.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // 5 plantas del hogar — datos estáticos hasta conectar BD
    private val plantas = mutableListOf(
        PlantItem("Pothos dorado",         "💚", "Riego hace 1 día",   "65%", "OK"),
        PlantItem("Monstera deliciosa",     "🌿", "Riego hace 3 días",  "48%", "OK"),
        PlantItem("Sansevieria",            "🪴", "Riego hace 7 días",  "30%", "OK"),
        PlantItem("Cactus navideño",        "🌵", "Riego hace 12 días", "20%", "OK"),
        PlantItem("Suculenta echeveria",    "🌸", "Riego hace 5 días",  "25%", "OK")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView
        binding.rvPlants.layoutManager = LinearLayoutManager(this)
        binding.rvPlants.adapter = PlantAdapter(plantas) { planta ->
            // Abrir Dashboard con el nombre de la planta seleccionada
            val intent = Intent(this, DashboardActivity::class.java)
            intent.putExtra("plant_name", planta.nombre)
            startActivity(intent)
        }

        binding.btnAddPlant.setOnClickListener {
            Toast.makeText(this, "Máximo 5 plantas por cuenta", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Modelo de datos ──────────────────────────────────────────────────────
    data class PlantItem(
        val nombre: String,
        val emoji: String,
        val ultimaActividad: String,
        val humedad: String,
        val estado: String
    )

    // ── Adapter del RecyclerView ─────────────────────────────────────────────
    inner class PlantAdapter(
        private val items: List<PlantItem>,
        private val onClick: (PlantItem) -> Unit
    ) : RecyclerView.Adapter<PlantAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvEmoji:         TextView = view.findViewById(R.id.tvEmoji)
            val tvPlantName:     TextView = view.findViewById(R.id.tvPlantName)
            val tvLastActivity:  TextView = view.findViewById(R.id.tvLastActivity)
            val tvHumidity:      TextView = view.findViewById(R.id.tvHumidity)
            val tvStatus:        TextView = view.findViewById(R.id.tvStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_plant, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvEmoji.text        = item.emoji
            holder.tvPlantName.text    = item.nombre
            holder.tvLastActivity.text = item.ultimaActividad
            holder.tvHumidity.text     = "Humedad: ${item.humedad}"
            holder.tvStatus.text       = item.estado
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size
    }
}