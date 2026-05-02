package com.hypv.agroiacam

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.hypv.agroiacam.databinding.ActivityDashboardBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var classifier: Classifier
    private var currentBitmap: Bitmap? = null
    private var usuario = ""

    // ── Lanzador de cámara ────────────────────────────────────────────────────
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                currentBitmap = bitmap
                analyzeImage(bitmap, metodo = "telefono")
            }
        }
    }

    // ── Lanzador de galería ───────────────────────────────────────────────────
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            currentBitmap = bitmap
            analyzeImage(bitmap, metodo = "galeria")
        }
    }

    // ── Permiso de cámara ─────────────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else Toast.makeText(this, "Se necesita permiso de cámara", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usuario = getSharedPreferences("session", MODE_PRIVATE)
            .getString("usuario", "Usuario") ?: "Usuario"

        binding.tvGreeting.text = "Hola, $usuario 👋"

        classifier = Classifier(this)

        // Actualizar sensores al abrir
        refreshSensors()

        binding.btnPhoneAnalyze.setOnClickListener { checkCameraPermission() }

        binding.btnEspAnalyze.setOnClickListener { fetchEspImage() }

        binding.btnCare.setOnClickListener {
            startActivity(Intent(this, PlantCareActivity::class.java))
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    // ── Sensores ──────────────────────────────────────────────────────────────
    private fun refreshSensors() {
        lifecycleScope.launch {
            while (true) {
                val raw = ApiHelper.getSensors()
                runOnUiThread { updateSensorUI(raw) }
                delay(5_000) // actualizar cada 5 segundos
            }
        }
    }

    private fun updateSensorUI(raw: String) {
        try {
            val json = JSONObject(raw)
            binding.tvHumidity.text = "${json.optInt("humedad", 0)}%"
            binding.tvLight.text    = "${json.optInt("luz", 0)} lx"
            binding.tvSoil.text     = "${json.optInt("suelo", 0)}%"
        } catch (e: Exception) {
            // Sin datos — dejar valores anteriores
        }
    }

    // ── Cámara ────────────────────────────────────────────────────────────────
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    // ── Analizar imagen con TFLite ────────────────────────────────────────────
    private fun analyzeImage(bitmap: Bitmap, metodo: String) {
        binding.btnPhoneAnalyze.isEnabled = false
        binding.btnPhoneAnalyze.text = "Analizando..."

        lifecycleScope.launch {
            val result = classifier.classify(bitmap)

            // Guardar en BD via Node-RED
            ApiHelper.saveResult(
                usuario     = usuario,
                planta      = binding.tvPlantName.text.toString(),
                diagnostico = result.label,
                precision   = result.percent,
                metodo      = metodo
            )

            runOnUiThread {
                binding.btnPhoneAnalyze.isEnabled = true
                binding.btnPhoneAnalyze.text = "📷 Analizar con Teléfono"

                // Actualizar card de estado
                binding.tvIaStatus.text = result.label
                binding.tvStatusBadge.text = if (result.isHealthy) "OK" else "⚠"

                val msg = "${result.label} — ${result.percent}% de confianza"
                Toast.makeText(this@DashboardActivity, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    // ── Imagen desde ESP32-CAM via Node-RED ───────────────────────────────────
    private fun fetchEspImage() {
        binding.btnEspAnalyze.isEnabled = false
        binding.btnEspAnalyze.text = "Conectando ESP32..."

        lifecycleScope.launch {
            // Node-RED devuelve los sensores; la foto la analiza al recibirla
            // Por ahora simula el flujo hasta tener el endpoint real
            val raw = ApiHelper.getSensors()
            updateSensorUI(raw)

            runOnUiThread {
                binding.btnEspAnalyze.isEnabled = true
                binding.btnEspAnalyze.text = "📡 Foto con ESP32-CAM"
                Toast.makeText(
                    this@DashboardActivity,
                    "Configura el endpoint /esp_photo en Node-RED",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        classifier.close()
    }
}