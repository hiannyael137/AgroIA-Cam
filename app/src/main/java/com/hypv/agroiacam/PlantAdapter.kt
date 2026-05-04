package com.hypv.agroiacam

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class PlantAdapter(
    private val plants: List<Plant>
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    class PlantViewHolder(view: View)
        : RecyclerView.ViewHolder(view) {

        val cardPlant: CardView =
            view.findViewById(R.id.cardPlant)

        val tvPlantName: TextView =
            view.findViewById(R.id.tvPlantName)

        val tvPlantStatus: TextView =
            view.findViewById(R.id.tvPlantStatus)

        val tvHumidity: TextView =
            view.findViewById(R.id.tvHumidity)

        val tvLastWater: TextView =
            view.findViewById(R.id.tvLastWater)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PlantViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_plant,
                    parent,
                    false
                )

        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: PlantViewHolder,
        position: Int
    ) {

        val plant = plants[position]

        holder.tvPlantName.text =
            plant.name

        holder.tvPlantStatus.text =
            "Estado: ${plant.status}"

        holder.tvHumidity.text =
            "Humedad: ${plant.humidity}"

        // 🔥 AQUÍ ESTÁ EL FIX
        holder.tvLastWater.text =
            "Último riego: ${plant.lastWatering}"

        holder.cardPlant.setOnClickListener {

            val context =
                holder.itemView.context

            context.startActivity(
                Intent(
                    context,
                    DashboardActivity::class.java
                )
            )
        }
    }

    override fun getItemCount(): Int {
        return plants.size
    }
}