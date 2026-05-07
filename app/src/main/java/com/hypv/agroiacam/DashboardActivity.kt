package com.hypv.agroiacam

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvHumedad: TextView
    private lateinit var tvTemperatura: TextView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var layoutResult: LinearLayout
    private lateinit var tvPlantName: TextView
    private lateinit var tvConfianza: TextView
    private lateinit var tvEstado: TextView
    private lateinit var tvConsejo: TextView
    private lateinit var btnTakePhoto: Button
    private lateinit var btnScanESP32: Button

    private val client = OkHttpClient()

    // Cámara — thumbnail directo, sin FileProvider
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { analizarFoto(it) }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
        else Toast.makeText(this, "Se necesita permiso de cámara", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tvHumedad        = findViewById(R.id.tvHumedad)
        tvTemperatura    = findViewById(R.id.tvTemperatura)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        layoutResult     = findViewById(R.id.layoutResult)
        tvPlantName      = findViewById(R.id.tvPlantName)
        tvConfianza      = findViewById(R.id.tvConfianza)
        tvEstado         = findViewById(R.id.tvEstado)
        tvConsejo        = findViewById(R.id.tvConsejo)
        btnTakePhoto     = findViewById(R.id.btnTakePhoto)
        btnScanESP32     = findViewById(R.id.btnScanESP32)

        // Estado inicial — sin análisis
        layoutEmptyState.visibility = View.VISIBLE
        layoutResult.visibility     = View.GONE

        // Sensores al abrir
        obtenerSensores()

        // Botón regresar
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Botón cámara del teléfono
        btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                cameraLauncher.launch(null)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // Botón ESP32-CAM
        btnScanESP32.setOnClickListener { pedirFotoESP32() }

        // Navegación
        findViewById<Button>(R.id.btnCare).setOnClickListener {
            startActivity(Intent(this, PlantCareActivity::class.java))
        }
        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    // ── Sensores ────────────────────────────────────────────────────────────
    private fun obtenerSensores() {
        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/ultimoSensor")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    tvHumedad.text     = "--"
                    tvTemperatura.text = "--"
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                try {
                    val json        = JSONObject(body)
                    val humedad     = json.optInt("humedad_suelo", 0)
                    val temperatura = json.optInt("temperatura", 0)
                    runOnUiThread {
                        tvHumedad.text     = "$humedad%"
                        tvTemperatura.text = "$temperatura°C"
                    }
                } catch (_: Exception) {
                    runOnUiThread {
                        tvHumedad.text     = "--"
                        tvTemperatura.text = "--"
                    }
                }
            }
        })
    }

    // ── Foto desde ESP32-CAM via Node-RED ────────────────────────────────────
    private fun pedirFotoESP32() {
        btnScanESP32.isEnabled = false
        btnScanESP32.text      = "Conectando..."

        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/esp32foto")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    btnScanESP32.isEnabled = true
                    btnScanESP32.text      = "📡 Foto con ESP32-CAM"
                    Toast.makeText(this@DashboardActivity,
                        "No se pudo conectar con ESP32-CAM", Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val bytes = response.body?.bytes()
                if (bytes == null || bytes.isEmpty()) {
                    runOnUiThread {
                        btnScanESP32.isEnabled = true
                        btnScanESP32.text      = "📡 Foto con ESP32-CAM"
                        Toast.makeText(this@DashboardActivity,
                            "ESP32-CAM no envió imagen", Toast.LENGTH_LONG).show()
                    }
                    return
                }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                runOnUiThread {
                    btnScanESP32.isEnabled = true
                    btnScanESP32.text      = "📡 Foto con ESP32-CAM"
                    if (bitmap != null) analizarFoto(bitmap)
                    else Toast.makeText(this@DashboardActivity,
                        "Imagen inválida del ESP32-CAM", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    // ── Clasificar con TFLite ────────────────────────────────────────────────
    private fun analizarFoto(bitmap: Bitmap) {
        btnTakePhoto.isEnabled = false
        btnTakePhoto.text      = "Analizando..."
        btnScanESP32.isEnabled = false

        Thread {
            // Classifier.kt devuelve "Rosal blanco - 94%"
            val resultadoRaw = Classifier(this).classify(bitmap)

            // Separar planta y porcentaje
            val partes     = resultadoRaw.split(" - ")
            val planta     = partes.getOrElse(0) { "Desconocido" }
            val porcentaje = partes.getOrElse(1) { "0%" }

            // Determinar estado según porcentaje
            val pct = porcentaje.replace("%", "").trim().toIntOrNull() ?: 0
            val (estado, consejo) = when {
                pct >= 80 -> Pair(
                    "🟢 Saludable",
                    "Tu planta se ve bien. Mantén el riego regular y luz adecuada."
                )
                pct >= 55 -> Pair(
                    "🟡 Monitorear",
                    "Hay algunos signos a vigilar. Revisa el riego y la iluminación."
                )
                else -> Pair(
                    "🔴 Requiere atención",
                    "Tu planta necesita cuidado. Revisa agua, luz y posibles plagas."
                )
            }

            // Guardar en BD si hay sesión
            val prefs     = getSharedPreferences("agroia", MODE_PRIVATE)
            val usuarioId = prefs.getInt("usuario_id", -1)
            if (usuarioId != -1) {
                ApiHelper.guardarResultado(
                    usuarioId = usuarioId,
                    planta    = planta,
                    estado    = estado,
                    confianza = pct / 100f
                ) { /* silencioso si falla */ }
            }

            runOnUiThread {
                btnTakePhoto.isEnabled = true
                btnTakePhoto.text      = "📷 Nueva foto"
                btnScanESP32.isEnabled = true

                // Mostrar resultado
                tvPlantName.text = planta
                tvConfianza.text = "Confianza: $porcentaje"
                tvEstado.text    = estado
                tvConsejo.text   = consejo

                layoutEmptyState.visibility = View.GONE
                layoutResult.visibility     = View.VISIBLE
            }
        }.start()
    }
}