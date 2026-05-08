package com.hypv.agroiacam

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    // Views — sensores
    private lateinit var tvPlantName:       TextView
    private lateinit var tvEstado:          TextView
    private lateinit var tvHumedad:         TextView
    private lateinit var tvTemperatura:     TextView
    private lateinit var tvHumedadAmbiente: TextView
    private lateinit var tvHumedadSuelo:    TextView
    private lateinit var tvLuz:             TextView
    private lateinit var tvSalinidad:       TextView
    private lateinit var tvBateria:         TextView
    private lateinit var tvSensorStatus:    TextView

    // Views — resultado IA
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var layoutResult:     LinearLayout
    private lateinit var tvIAPlant:        TextView
    private lateinit var tvIAConfianza:    TextView
    private lateinit var tvIAEstado:       TextView
    private lateinit var tvIAConsejo:      TextView
    private lateinit var btnScanCamera:    Button
    private lateinit var btnScanESP32:     Button

    private val handler  = Handler(Looper.getMainLooper())
    private var plantaId = -1
    private var cameraImageUri: Uri? = null
    private var esp32Conectada = false

    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (plantaId != -1 && esp32Conectada) fetchSensorData()
            handler.postDelayed(this, 10000)
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            cameraImageUri?.let { uri ->
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                analizarFoto(bitmap)
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Sensores
        tvPlantName       = findViewById(R.id.tvPlantName)
        tvEstado          = findViewById(R.id.tvEstado)
        tvHumedad         = findViewById(R.id.tvHumedad)
        tvTemperatura     = findViewById(R.id.tvTemperatura)
        tvHumedadAmbiente = findViewById(R.id.tvHumedadAmbiente)
        tvHumedadSuelo    = findViewById(R.id.tvHumedadSuelo)
        tvLuz             = findViewById(R.id.tvLuz)
        tvSalinidad       = findViewById(R.id.tvSalinidad)
        tvBateria         = findViewById(R.id.tvBateria)
        tvSensorStatus    = findViewById(R.id.tvSensorStatus)

        // IA
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        layoutResult     = findViewById(R.id.layoutResult)
        tvIAPlant        = findViewById(R.id.tvIAPlant)
        tvIAConfianza    = findViewById(R.id.tvIAConfianza)
        tvIAEstado       = findViewById(R.id.tvIAEstado)
        tvIAConsejo      = findViewById(R.id.tvIAConsejo)
        btnScanCamera    = findViewById(R.id.btnScanCamera)
        btnScanESP32     = findViewById(R.id.btnScanESP32)

        layoutEmptyState.visibility = View.VISIBLE
        layoutResult.visibility     = View.GONE

        // Navegación
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnCare).setOnClickListener {
            startActivity(Intent(this, PlantCareActivity::class.java))
        }
        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        btnScanCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) openCamera()
            else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        btnScanESP32.setOnClickListener { pedirFotoESP32() }

        findViewById<Button>(R.id.btnConectarESP32).setOnClickListener { conectarESP32() }
    }

    override fun onResume() {
        super.onResume()
        plantaId    = intent.getIntExtra("plant_id", -1)
        val nombre  = intent.getStringExtra("plant_name") ?: "Mi Planta"
        val estado  = intent.getStringExtra("estado")     ?: "Saludable"
        val humedad = intent.getStringExtra("humedad")    ?: "--"

        tvPlantName.text = nombre
        tvEstado.text    = "🟢 $estado"
        tvHumedad.text   = humedad

        esp32Conectada = false
        limpiarSensores()
        handler.removeCallbacks(refreshRunnable)
        handler.post(refreshRunnable)
    }

    override fun onPause()   { super.onPause();   handler.removeCallbacks(refreshRunnable) }
    override fun onDestroy() { super.onDestroy(); handler.removeCallbacks(refreshRunnable) }

    // ── Sensores ──────────────────────────────────────────────────────────────
    private fun limpiarSensores() {
        tvTemperatura.text     = "--°C"
        tvHumedadAmbiente.text = "--%"
        tvHumedadSuelo.text    = "--%"
        tvLuz.text             = "-- lux"
        tvSalinidad.text       = "--"
        tvBateria.text         = "--%"
        tvSensorStatus.text    = "● Desconectado"
        tvSensorStatus.setTextColor(0xFFEF4444.toInt())
    }

    private fun conectarESP32() {
        if (plantaId == -1) {
            Toast.makeText(this, "Sin planta_id", Toast.LENGTH_SHORT).show()
            return
        }
        tvSensorStatus.text = "● Conectando..."
        tvSensorStatus.setTextColor(0xFFFCD34D.toInt())

        ApiHelper.configurarESP32(plantaId) { ok ->
            runOnUiThread {
                if (ok) {
                    esp32Conectada = true
                    tvSensorStatus.text = "● Esperando ESP32..."
                    tvSensorStatus.setTextColor(0xFFFCD34D.toInt())
                    handler.postDelayed({ fetchSensorData() }, 4000)
                } else {
                    tvSensorStatus.text = "● Error conexión"
                    tvSensorStatus.setTextColor(0xFFEF4444.toInt())
                }
            }
        }
    }

    private fun fetchSensorData() {
        ApiHelper.obtenerDatosPlanta(plantaId) { arr ->
            runOnUiThread {
                if (arr == null || arr.length() == 0) {
                    tvSensorStatus.text = "● Sin señal"
                    tvSensorStatus.setTextColor(0xFFEF4444.toInt())
                    return@runOnUiThread
                }
                val data = arr.getJSONObject(0)
                val temp = data.optString("temperatura", "")
                if (temp.isEmpty() || temp == "null") {
                    tvSensorStatus.text = "● Esperando ESP32..."
                    tvSensorStatus.setTextColor(0xFFFCD34D.toInt())
                    return@runOnUiThread
                }
                tvTemperatura.text     = "${temp}°C"
                val humS = data.optString("humedad", "--")
                tvHumedad.text         = humS
                tvHumedadSuelo.text    = if (humS != "null") humS else "--%"
                val humA = data.optString("humedad_ambiente", "--")
                tvHumedadAmbiente.text = if (humA != "null") "${humA}%" else "--%"
                val luz = data.optString("luz", "--")
                tvLuz.text             = if (luz != "null") "${luz} lux" else "-- lux"
                val sal = data.optString("salinidad", "--")
                tvSalinidad.text       = if (sal != "null") sal else "--"
                val bat = data.optString("bateria", "--")
                tvBateria.text         = if (bat != "null") "${bat}%" else "--%"
                tvSensorStatus.text    = "● En vivo"
                tvSensorStatus.setTextColor(0xFF86EFAC.toInt())
            }
        }
    }

    // ── Cámara ────────────────────────────────────────────────────────────────
    private fun openCamera() {
        val ts   = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(cacheDir, "scan_$ts.jpg")
        cameraImageUri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        cameraLauncher.launch(intent)
    }

    // ── Foto desde ESP32-CAM ──────────────────────────────────────────────────
    private fun pedirFotoESP32() {
        btnScanESP32.isEnabled = false
        btnScanESP32.text = "Conectando..."
        ApiHelper.obtenerFotoESP32 { bytes, error ->
            runOnUiThread {
                btnScanESP32.isEnabled = true
                btnScanESP32.text = "📡 ESP32-CAM"
                if (bytes != null) {
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) analizarFoto(bitmap)
                    else Toast.makeText(this, "Imagen inválida", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, error ?: "Error ESP32-CAM", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ── Clasificar con TFLite ─────────────────────────────────────────────────
    private fun analizarFoto(bitmap: Bitmap) {
        btnScanCamera.isEnabled = false
        btnScanCamera.text = "Analizando..."
        btnScanESP32.isEnabled = false

        Thread {
            val raw        = Classifier(this).classify(bitmap)
            val partes     = raw.split(" - ")
            val planta     = partes.getOrElse(0) { "Desconocido" }
            val porcentaje = partes.getOrElse(1) { "0%" }
            val pct        = porcentaje.replace("%", "").trim().toIntOrNull() ?: 0

            // ── Soluciones específicas por enfermedad ─────────────────────────
            val (estado, consejo) = when (planta) {
                "Rosal Sano" -> Pair(
                    "🟢 Saludable",
                    "Tu rosal está en buen estado. Mantén riego regular cada 2-3 días y buena exposición solar."
                )
                "Rosal enfermo-roya" -> Pair(
                    "🔴 Roya detectada",
                    "Elimina las hojas infectadas. Aplica fungicida a base de azufre o cobre. Evita mojar el follaje al regar y mejora la ventilación."
                )
                "Rosal enfermo-pulgones" -> Pair(
                    "🔴 Pulgones detectados",
                    "Aplica jabón potásico diluido en agua. También puedes usar insecticida de neem. Retira los pulgones manualmente si son pocos."
                )
                "Cactus Sano" -> Pair(
                    "🟢 Saludable",
                    "Tu cactus está bien. Riega solo cuando la tierra esté completamente seca, cada 2-3 semanas."
                )
                "Cactus enfermo-Hongo" -> Pair(
                    "🔴 Hongo detectado",
                    "Reduce el riego inmediatamente. Aplica fungicida sistémico. Retira las partes blandas o ennegrecidas con tijera desinfectada."
                )
                "Cactus enfermo-Infeccion" -> Pair(
                    "🔴 Infección detectada",
                    "Saca el cactus de su maceta, revisa las raíces y corta las partes podridas. Deja secar 2 días antes de replantar en sustrato nuevo."
                )
                "Cinta Sana" -> Pair(
                    "🟢 Saludable",
                    "Tu cinta está en perfecto estado. Riego moderado y luz indirecta es todo lo que necesita."
                )
                "Cinta enferma-Falta de luz" -> Pair(
                    "🟡 Falta de luz",
                    "Mueve la planta a un lugar con más luz indirecta. Evita el sol directo. Las hojas deben recuperar su color en 1-2 semanas."
                )
                "Cola de borrego Sana" -> Pair(
                    "🟢 Saludable",
                    "Tu cola de borrego está bien. Necesita poca agua y mucha luz. Riego cada 10-15 días en verano."
                )
                "Cola de borrego enfermo-tallo seco" -> Pair(
                    "🔴 Tallo seco detectado",
                    "El tallo seco indica falta de agua o raíces dañadas. Revisa las raíces, elimina las secas y aumenta el riego gradualmente."
                )
                "Alcatraz Sano" -> Pair(
                    "🟢 Saludable",
                    "Tu alcatraz está en buen estado. Mantén el sustrato húmedo y ubícalo en un lugar con luz indirecta brillante."
                )
                "Alcatraz enfermo-cochinilla escamosa" -> Pair(
                    "🔴 Cochinilla escamosa",
                    "Limpia las hojas con un algodón con alcohol isopropílico. Aplica insecticida sistémico. Aísla la planta para evitar contagio."
                )
                else -> when {
                    pct >= 80 -> Pair(
                        "🟢 Saludable",
                        "Tu planta se ve bien. Mantén el riego regular y luz adecuada."
                    )
                    pct >= 55 -> Pair(
                        "🟡 Monitorear",
                        "Hay signos a vigilar. Revisa el riego y la iluminación."
                    )
                    else -> Pair(
                        "🔴 Requiere atención",
                        "Tu planta necesita cuidado. Revisa agua, luz y posibles plagas."
                    )
                }
            }

            val prefs     = getSharedPreferences("agroia", MODE_PRIVATE)
            val usuarioId = prefs.getInt("usuario_id", -1)
            if (usuarioId != -1) {
                ApiHelper.guardarResultado(usuarioId, planta, estado, pct / 100f) {}
            }

            runOnUiThread {
                btnScanCamera.isEnabled = true
                btnScanCamera.text = "📷 Cámara"
                btnScanESP32.isEnabled = true

                tvIAPlant.text     = planta
                tvIAConfianza.text = "Confianza: $porcentaje"
                tvIAEstado.text    = estado
                tvIAConsejo.text   = consejo

                layoutEmptyState.visibility = View.GONE
                layoutResult.visibility     = View.VISIBLE
            }
        }.start()
    }
}