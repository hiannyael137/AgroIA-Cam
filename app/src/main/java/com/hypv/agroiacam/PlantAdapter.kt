package com.hypv.agroiacam

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
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
import coil.load
import coil.transform.CircleCropTransformation

class PlantAdapter(
    private val plants: MutableList<Plant>,
    private val onDelete: (Plant, Int) -> Unit
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    class PlantViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val cardPlant: CardView =
            view.findViewById(R.id.cardPlant)

        val imgPlant: ImageView =
            view.findViewById(R.id.imgPlant)

        val tvPlantName: TextView =
            view.findViewById(R.id.tvPlantName)

        val tvPlantStatus: TextView =
            view.findViewById(R.id.tvPlantStatus)

        val tvHumidity: TextView =
            view.findViewById(R.id.tvHumidity)

        val tvLastWater: TextView =
            view.findViewById(R.id.tvLastWater)

        val progressSalud: ProgressBar =
            view.findViewById(R.id.progressSalud)

        val tvSaludPorcentaje: TextView =
            view.findViewById(R.id.tvSaludPorcentaje)

        val btnDeletePlant: ImageButton =
            view.findViewById(R.id.btnDeletePlant)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PlantViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_plant,
                parent,
                false
            )

        return PlantViewHolder(view)
    }

    override fun getItemCount(): Int {
        return plants.size
    }

    override fun onBindViewHolder(
        holder: PlantViewHolder,
        position: Int
    ) {

        val plant = plants[position]

        // =========================
        // DATOS
        // =========================
        holder.tvPlantName.text =
            plant.nombre_personalizado

        holder.tvPlantStatus.text =
            "Estado: ${plant.estado}"

        holder.tvHumidity.text =
            "Humedad: ${plant.humedad}"

        holder.tvLastWater.text =
            "Último riego: ${plant.ultimo_riego}"

        // =========================
        // SALUD
        // =========================
        holder.progressSalud.progress =
            plant.salud

        holder.tvSaludPorcentaje.text =
            "${plant.salud}%"

        val color = when {

            plant.salud >= 70 ->
                Color.parseColor("#86EFAC")

            plant.salud >= 40 ->
                Color.parseColor("#FCD34D")

            else ->
                Color.parseColor("#FF6B6B")
        }

        holder.progressSalud.progressTintList =
            ColorStateList.valueOf(color)

        holder.tvSaludPorcentaje.setTextColor(color)

        // =========================
        // IMAGEN
        // =========================
        if (plant.imagen_url.isNotEmpty()) {

            holder.imgPlant.load(plant.imagen_url) {
                placeholder(R.drawable.ic_plant_placeholder)
                error(R.drawable.ic_plant_placeholder)
                transformations(CircleCropTransformation())
            }

        } else {

            holder.imgPlant.setImageResource(
                R.drawable.ic_plant_placeholder
            )
        }

        // =========================
        // CLICK CARD
        // =========================
        holder.cardPlant.setOnClickListener {

            val context = holder.itemView.context

            val intent = Intent(
                context,
                DashboardActivity::class.java
            )

            intent.putExtra("plant_id",   plant.id)
            intent.putExtra("plant_name", plant.nombre_personalizado)
            intent.putExtra("estado",     plant.estado)
            intent.putExtra("humedad",    plant.humedad)
            intent.putExtra("salud",      plant.salud)
            intent.putExtra("imagen_url", plant.imagen_url)

            context.startActivity(intent)
        }

        // =========================
        // ELIMINAR
        // =========================
        holder.btnDeletePlant.setOnClickListener {

            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Eliminar planta")
                .setMessage(
                    "¿Seguro que deseas eliminar ${plant.nombre_personalizado}?"
                )
                .setPositiveButton("Sí, eliminar") { _, _ ->
                    onDelete(plant, position)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}