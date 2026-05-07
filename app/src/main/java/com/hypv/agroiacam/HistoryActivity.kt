package com.hypv.agroiacam

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class HistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView

    private val client = OkHttpClient()

    private val historyList =
        mutableListOf<HistoryItem>()

    private lateinit var adapter:
            HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_history)

        rvHistory =
            findViewById(R.id.rvHistory)

        findViewById<ImageButton>(R.id.btnBack)
            .setOnClickListener {

                finish()
            }

        setupRecycler()

        loadHistory()
    }

    // =========================
    // RECYCLER
    // =========================
    private fun setupRecycler() {

        adapter =
            HistoryAdapter(historyList)

        rvHistory.layoutManager =
            LinearLayoutManager(this)

        rvHistory.adapter =
            adapter
    }

    // =========================
    // CARGAR HISTORIAL
    // =========================
    private fun loadHistory() {

        val prefs =
            getSharedPreferences(
                "agroia",
                MODE_PRIVATE
            )

        val usuarioId =
            prefs.getInt("usuario_id", 0)

        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/getHistory?usuario_id=$usuarioId")
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
                            this@HistoryActivity,
                            "Error cargando historial",
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

                        historyList.clear()

                        for (i in 0 until jsonArray.length()) {

                            val obj =
                                jsonArray.getJSONObject(i)

                            val item = HistoryItem(

                                resultado =
                                    obj.getString("resultado"),

                                planta =
                                    obj.getString("planta"),

                                estado =
                                    obj.getString("estado"),

                                confianza =
                                    obj.getInt("confianza"),

                                metodo =
                                    obj.getString("metodo"),

                                fecha =
                                    obj.getString("fecha")
                            )

                            historyList.add(item)
                        }

                        runOnUiThread {

                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            })
    }
}