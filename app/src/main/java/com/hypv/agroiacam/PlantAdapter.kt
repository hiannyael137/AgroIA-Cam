package com.hypv.agroiacam

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class PlantAdapter(
    private val context: Context,
    private val plants: MutableList<Plant>,
    private val onReload: () -> Unit
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    private val client = OkHttpClient()

    class PlantViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val imgPlant: ImageView =
            view.findViewById(R.id.imgPlant)

        val tvPlantName: TextView =
            view.findViewById(R.id.tvPlantName)

        val tvPlantStatus: TextView =
            view.findViewById(R.id.tvPlantStatus)

        val progressSalud: ProgressBar =
            view.findViewById(R.id.progressSalud)

        val tvSaludPorcentaje: TextView =
            view.findViewById(R.id.tvSaludPorcentaje)

        val tvHumidity: TextView =
            view.findViewById(R.id.tvHumidity)

        val tvLastWater: TextView =
            view.findViewById(R.id.tvLastWater)

        val btnDeletePlant: ImageButton =
            view.findViewById(R.id.btnDeletePlant)

        val cardPlant: View =
            view.findViewById(R.id.cardPlant)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PlantViewHolder {

        val view = LayoutInflater.from(context)
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

        holder.progressSalud.progress =
            plant.salud

        holder.tvSaludPorcentaje.text =
            "${plant.salud}%"

        holder.tvHumidity.text =
            "Humedad: ${plant.humedad}"

        holder.tvLastWater.text =
            "Último riego: ${plant.ultimo_riego}"

        // =========================
        // IMAGEN
        // =========================
        holder.imgPlant.setImageResource(
            R.drawable.ic_plant_placeholder
        )

        // =========================
        // CLICK CARD
        // =========================
        holder.cardPlant.setOnClickListener {

            val intent = Intent(
                context,
                DashboardActivity::class.java
            )

            intent.putExtra(
                "plant_id",
                plant.id
            )

            intent.putExtra(
                "plant_name",
                plant.nombre_personalizado
            )

            intent.putExtra(
                "plant_type",
                plant.tipo_planta
            )

            intent.putExtra(
                "plant_status",
                plant.estado
            )

            intent.putExtra(
                "plant_humidity",
                plant.humedad
            )

            intent.putExtra(
                "plant_health",
                plant.salud
            )

            context.startActivity(intent)
        }

        // =========================
        // ELIMINAR
        // =========================
        holder.btnDeletePlant.setOnClickListener {

            deletePlant(plant.id)
        }
    }

    // =========================
    // ELIMINAR PLANTA
    // =========================
    private fun deletePlant(id: Int) {

        val json = JSONObject()
        json.put("id", id)

        val body = json.toString()
            .toRequestBody(
                "application/json".toMediaType()
            )

        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/deletePlant")
            .post(body)
            .build()

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    (context as MainActivity)
                        .runOnUiThread {

                            Toast.makeText(
                                context,
                                "Error eliminando planta",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    (context as MainActivity)
                        .runOnUiThread {

                            Toast.makeText(
                                context,
                                "Planta eliminada",
                                Toast.LENGTH_SHORT
                            ).show()

                            onReload()
                        }
                }
            })
    }
}