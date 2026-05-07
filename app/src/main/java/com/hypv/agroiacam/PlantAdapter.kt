package com.hypv.agroiacam

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PlantAdapter(
    private val plants: MutableList<Plant>,
    private val onDelete: (Plant, Int) -> Unit
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    class PlantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardPlant: CardView = view.findViewById(R.id.cardPlant)
        val imgPlant: ImageView = view.findViewById(R.id.imgPlant)
        val tvPlantName: TextView = view.findViewById(R.id.tvPlantName)
        val tvPlantStatus: TextView = view.findViewById(R.id.tvPlantStatus)
        val tvHumidity: TextView = view.findViewById(R.id.tvHumidity)
        val tvLastWater: TextView = view.findViewById(R.id.tvLastWater)
        val progressSalud: ProgressBar = view.findViewById(R.id.progressSalud)
        val tvSaludPorcentaje: TextView = view.findViewById(R.id.tvSaludPorcentaje)
        val btnDeletePlant: ImageButton = view.findViewById(R.id.btnDeletePlant)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]

        holder.tvPlantName.text = plant.name
        holder.tvPlantStatus.text = "Estado: ${plant.status}"
        holder.tvHumidity.text = "Humedad: ${plant.humidity}"
        holder.tvLastWater.text = "Último riego: ${plant.lastWatering}"

        // Barra de salud
        holder.progressSalud.progress = plant.salud
        holder.tvSaludPorcentaje.text = "${plant.salud}%"

        // Color de barra según salud
        val color = when {
            plant.salud >= 70 -> android.graphics.Color.parseColor("#86EFAC") // verde
            plant.salud >= 40 -> android.graphics.Color.parseColor("#FCD34D") // amarillo
            else -> android.graphics.Color.parseColor("#FF6B6B")               // rojo
        }
        holder.progressSalud.progressTintList =
            android.content.res.ColorStateList.valueOf(color)
        holder.tvSaludPorcentaje.setTextColor(color)

        // Foto de la planta
        if (plant.imagenUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load("${ApiHelper.BASE_URL}/uploads/${plant.imagenUrl}")
                .placeholder(R.drawable.ic_plant_placeholder)
                .circleCrop()
                .into(holder.imgPlant)
        } else {
            holder.imgPlant.setImageResource(R.drawable.ic_plant_placeholder)
        }

        // Click en card → Dashboard
        holder.cardPlant.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DashboardActivity::class.java)
            intent.putExtra("plant_id", plant.id)
            intent.putExtra("plant_name", plant.name)
            context.startActivity(intent)
        }

        // Click en eliminar
        holder.btnDeletePlant.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Eliminar planta")
                .setMessage("¿Seguro que deseas eliminar ${plant.name}?")
                .setPositiveButton("Sí, eliminar") { _, _ ->
                    onDelete(plant, position)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    override fun getItemCount() = plants.size
}