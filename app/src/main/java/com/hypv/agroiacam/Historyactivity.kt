package com.hypv.agroiacam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hypv.agroiacam.databinding.ActivityHistoryBinding
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private var usuario = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usuario = getSharedPreferences("session", MODE_PRIVATE)
            .getString("usuario", "") ?: ""

        loadHistory()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val raw = ApiHelper.getHistory(usuario)

            runOnUiThread {
                try {
                    val array = JSONArray(raw)

                    if (array.length() == 0) {
                        Toast.makeText(this@HistoryActivity, "Sin historial aún", Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }

                    // Construir lista de HistoryItem
                    val items = mutableListOf<HistoryItem>()
                    for (i in 0 until array.length()) {
                        val obj: JSONObject = array.getJSONObject(i)
                        items.add(
                            HistoryItem(
                                tipo      = obj.optString("diagnostico", "Análisis IA"),
                                planta    = obj.optString("planta", "—"),
                                fecha     = obj.optString("fecha", "—"),
                                precision = "${obj.optInt("precision", 0)}%",
                                metodo    = obj.optString("metodo", "—")
                            )
                        )
                    }

                    // Inyectar RecyclerView en el ScrollView del layout existente
                    setupRecyclerView(items)

                } catch (e: Exception) {
                    Toast.makeText(this@HistoryActivity, "Error cargando historial", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupRecyclerView(items: List<HistoryItem>) {
        // El layout activity_history.xml tiene un ScrollView con rvHistory adentro
        // Si no existe rvHistory en ese layout, lo buscamos por id de la lista interna
        val rv = findViewById<RecyclerView>(R.id.rvHistory) ?: return

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = HistoryAdapter(items)
    }

    // ── Modelo ───────────────────────────────────────────────────────────────
    data class HistoryItem(
        val tipo: String,
        val planta: String,
        val fecha: String,
        val precision: String,
        val metodo: String
    )

    // ── Adapter ──────────────────────────────────────────────────────────────
    inner class HistoryAdapter(
        private val items: List<HistoryItem>
    ) : RecyclerView.Adapter<HistoryAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvResultType: TextView = view.findViewById(R.id.tvResultType)
            val tvPlantName:  TextView = view.findViewById(R.id.tvPlantName)
            val tvPrecision:  TextView = view.findViewById(R.id.tvPrecision)
            val tvMethod:     TextView = view.findViewById(R.id.tvMethod)
            val tvDate:       TextView = view.findViewById(R.id.tvDate)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_result, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvResultType.text = item.tipo
            holder.tvPlantName.text  = "Planta: ${item.planta}"
            holder.tvPrecision.text  = item.precision
            holder.tvMethod.text     = "Método: ${item.metodo}"
            holder.tvDate.text       = item.fecha
        }

        override fun getItemCount() = items.size
    }
}