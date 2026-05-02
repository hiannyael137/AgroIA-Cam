package com.hypv.agroiacam

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
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
    private var usuario  = ""
    private var plantName = ""

    // ── Lanzador cámara (ACTION_IMAGE_CAPTURE devuelve thumbnail en extras) ──
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            @Suppress("DEPRECATION")
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let { analyzeImage(it, "telefono") }
        }
    }

    // ── Lanzador galería ─────────────────────────────────────────────────────
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@registerForActivityResult
        try {
            val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
            analyzeImage(bitmap, "galeria")
        } catch (e: Exception) {
            Toast.makeText(this, "Error cargando imagen", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Permiso cámara ───────────────────────────────────────────────────────
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

        usuario   = getSharedPreferences("session", MODE_PRIVATE)
            .getString("usuario", "Usuario") ?: "Usuario"
        plantName = intent.getStringExtra("plant_name") ?: "Mi Planta"

        binding.tvGreeting.text  = "Hola, $usuario 👋"
        binding.tvPlantName.text = plantName

        classifier = Classifier(this)

        startSensorPolling()

        binding.btnPhoneAnalyze.setOnClickListener { checkCameraPermission() }
        binding.btnEspAnalyze.setOnClickListener   { fetchEspImage() }
        binding.btnCare.setOnClickListener {
            startActivity(Intent(this, PlantCareActivity::class.java))
        }
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    // ── Polling de sensores cada 5 s ─────────────────────────────────────────
    private fun startSensorPolling() {
        lifecycleScope.launch {
            while (true) {
                val raw = ApiHelper.getSensors()
                runOnUiThread { updateSensorUI(raw) }
                delay(5_000)
            }
        }
    }

    private fun updateSensorUI(raw: String) {
        try {
            val json = JSONObject(raw)
            binding.tvHumidity.text = "${json.optInt("humedad", 0)}%"
            binding.tvLight.text    = "${json.optInt("luz", 0)} lx"
            binding.tvSoil.text     = "${json.optInt("suelo", 0)}%"
        } catch (_: Exception) { /* sin datos — mantener último valor */ }
    }

    // ── Cámara ───────────────────────────────────────────────────────────────
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

    // ── Análisis TFLite ──────────────────────────────────────────────────────
    private fun analyzeImage(bitmap: Bitmap, metodo: String) {
        binding.btnPhoneAnalyze.isEnabled = false
        binding.btnPhoneAnalyze.text      = "Analizando..."

        lifecycleScope.launch {
            val result = classifier.classify(bitmap)

            ApiHelper.saveResult(
                usuario     = usuario,
                planta      = plantName,
                diagnostico = result.label,
                precision   = result.percent,
                metodo      = metodo
            )

            runOnUiThread {
                binding.btnPhoneAnalyze.isEnabled = true
                binding.btnPhoneAnalyze.text      = "📷  Analizar con Cámara"
                binding.tvIaStatus.text           = result.label
                binding.tvStatusBadge.text        = if (result.isHealthy) "Saludable" else "⚠ Revisar"

                Toast.makeText(
                    this@DashboardActivity,
                    "${result.label} — ${result.percent}%",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ── ESP32-CAM ────────────────────────────────────────────────────────────
    private fun fetchEspImage() {
        binding.btnEspAnalyze.isEnabled = false
        binding.btnEspAnalyze.text      = "Conectando ESP32..."

        lifecycleScope.launch {
            val raw = ApiHelper.getSensors()
            updateSensorUI(raw)

            runOnUiThread {
                binding.btnEspAnalyze.isEnabled = true
                binding.btnEspAnalyze.text      = "📡  Foto con ESP32-CAM"
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