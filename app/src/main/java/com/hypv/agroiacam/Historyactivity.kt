package com.hypv.agroiacam

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hypv.agroiacam.databinding.ActivityHistoryBinding
import kotlinx.coroutines.launch
import org.json.JSONArray

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

                    // TODO: conectar a RecyclerView cuando lo agregues al layout
                    // Por ahora muestra un resumen en Toast
                    val sb = StringBuilder()
                    for (i in 0 until minOf(array.length(), 3)) {
                        val obj = array.getJSONObject(i)
                        sb.appendLine("${obj.optString("planta")} — ${obj.optString("diagnostico")}")
                    }
                    Toast.makeText(this@HistoryActivity, sb.toString(), Toast.LENGTH_LONG).show()

                } catch (e: Exception) {
                    Toast.makeText(this@HistoryActivity, "Error cargando historial", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}