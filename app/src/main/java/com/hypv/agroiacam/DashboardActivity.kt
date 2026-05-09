package com.hypv.agroiacam

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    // Sensores
    private lateinit var tvPlantName: TextView
    private lateinit var tvEstado: TextView
    private lateinit var tvHumedad: TextView
    private lateinit var tvTemperatura: TextView
    private lateinit var tvHumedadAmbiente: TextView
    private lateinit var tvHumedadSuelo: TextView
    private lateinit var tvLuz: TextView
    private lateinit var tvSalinidad: TextView
    private lateinit var tvBateria: TextView
    private lateinit var tvSensorStatus: TextView

    // Resultado IA
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var layoutResult: LinearLayout
    private lateinit var tvIAPlant: TextView
    private lateinit var tvIAConfianza: TextView
    private lateinit var tvIAEstado: TextView
    private lateinit var tvIAConsejo: TextView
    private lateinit var tvIAMensajeDashboard: TextView
    private lateinit var btnScanCamera: Button
    private lateinit var btnScanESP32: Button

    private val handler = Handler(Looper.getMainLooper())

    private var plantaId = -1
    private var plantName = "Mi planta"
    private var plantType = ""
    private var cameraImageUri: Uri? = null
    private var esp32Conectada = false
    private var classifier: Classifier? = null

    // Evita salidas raras por recreación de Activity al abrir cámara o por callbacks tardíos.
    private var isAnalyzingPhoto = false
    private var hasVisibleDiagnosis = false

    // Validación por especie.
    // Ya no usamos solo el top1 porque el modelo puede dudar entre:
    // Cola de borrego Sana 49% y Cola de borrego enfermo-tallo seco 50%.
    private val minSpeciesScore = 0.65f
    private val noPlantRejectScore = 0.55f

    // Validación por salud/enfermedad dentro de la especie detectada.
    // Importante para Rosal y Cactus, porque tienen 2 enfermedades cada uno.
    private val minWithinSpeciesShare = 0.55f
    private val minHealthStateMargin = 0.18f
    private val minDiseaseTypeMargin = 0.12f
    private val minDiseaseShareWithinDiseases = 0.55f

    private data class ParsedLabel(
        val original: String,
        val species: String,
        val problem: String,
        val isHealthy: Boolean,
        val isNoPlant: Boolean
    )

    private data class SpeciesAnalysis(
        val registeredSpecies: String,
        val detectedSpecies: String,
        val registeredSpeciesScore: Float,
        val detectedSpeciesScore: Float,
        val noPlantScore: Float,
        val healthyScore: Float,
        val diseaseTotalScore: Float,
        val topHealthyLabel: String,
        val topDiseaseLabel: String,
        val topDiseaseProblem: String,
        val topDiseaseScore: Float,
        val secondDiseaseLabel: String,
        val secondDiseaseProblem: String,
        val secondDiseaseScore: Float,
        val diseaseOptionsCount: Int,
        val decisionLabel: String,
        val decisionProblem: String,
        val decisionHealthy: Boolean,
        val decisionConclusive: Boolean,
        val decisionReason: String
    ) {
        val registeredSpeciesPercent: Int get() = (registeredSpeciesScore * 100f).toInt().coerceIn(0, 100)
        val detectedSpeciesPercent: Int get() = (detectedSpeciesScore * 100f).toInt().coerceIn(0, 100)
        val noPlantPercent: Int get() = (noPlantScore * 100f).toInt().coerceIn(0, 100)
        val healthyPercent: Int get() = (healthyScore * 100f).toInt().coerceIn(0, 100)
        val diseaseTotalPercent: Int get() = (diseaseTotalScore * 100f).toInt().coerceIn(0, 100)
        val topDiseasePercent: Int get() = (topDiseaseScore * 100f).toInt().coerceIn(0, 100)
        val secondDiseasePercent: Int get() = (secondDiseaseScore * 100f).toInt().coerceIn(0, 100)
        val topDiseaseShareWithinDiseases: Float get() = if (diseaseTotalScore > 0f) topDiseaseScore / diseaseTotalScore else 0f
        val topDiseaseSharePercent: Int get() = (topDiseaseShareWithinDiseases * 100f).toInt().coerceIn(0, 100)
        val diseaseMargin: Float get() = topDiseaseScore - secondDiseaseScore
        val diseaseMarginPercent: Int get() = (diseaseMargin * 100f).toInt().coerceIn(0, 100)
        val healthyShareWithinSpecies: Float get() = if (registeredSpeciesScore > 0f) healthyScore / registeredSpeciesScore else 0f
        val diseaseShareWithinSpecies: Float get() = if (registeredSpeciesScore > 0f) diseaseTotalScore / registeredSpeciesScore else 0f
        val healthySharePercent: Int get() = (healthyShareWithinSpecies * 100f).toInt().coerceIn(0, 100)
        val diseaseSharePercent: Int get() = (diseaseShareWithinSpecies * 100f).toInt().coerceIn(0, 100)
    }

    private data class ValidationResult(
        val accepted: Boolean,
        val status: String,
        val message: String,
        val validationCode: String
    )

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
            val uri = cameraImageUri
            if (uri != null) {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    analizarFoto(bitmap, uri)
                } catch (e: Exception) {
                    resetCameraButton()
                    Toast.makeText(this, "No se pudo leer la foto: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                resetCameraButton()
                Toast.makeText(this, "No se encontró la foto tomada", Toast.LENGTH_SHORT).show()
            }
        } else {
            resetCameraButton()
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

        tvPlantName = findViewById(R.id.tvPlantName)
        tvEstado = findViewById(R.id.tvEstado)
        tvHumedad = findViewById(R.id.tvHumedad)
        tvTemperatura = findViewById(R.id.tvTemperatura)
        tvHumedadAmbiente = findViewById(R.id.tvHumedadAmbiente)
        tvHumedadSuelo = findViewById(R.id.tvHumedadSuelo)
        tvLuz = findViewById(R.id.tvLuz)
        tvSalinidad = findViewById(R.id.tvSalinidad)
        tvBateria = findViewById(R.id.tvBateria)
        tvSensorStatus = findViewById(R.id.tvSensorStatus)

        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        layoutResult = findViewById(R.id.layoutResult)
        tvIAPlant = findViewById(R.id.tvIAPlant)
        tvIAConfianza = findViewById(R.id.tvIAConfianza)
        tvIAEstado = findViewById(R.id.tvIAEstado)
        tvIAConsejo = findViewById(R.id.tvIAConsejo)
        tvIAMensajeDashboard = findViewById(R.id.tvIAMensajeDashboard)
        btnScanCamera = findViewById(R.id.btnScanCamera)
        btnScanESP32 = findViewById(R.id.btnScanESP32)

        layoutEmptyState.visibility = View.VISIBLE
        layoutResult.visibility = View.GONE
        tvIAMensajeDashboard.text = "Sin diagnóstico todavía. Toma una foto para analizar tu planta."

        restoreDashboardState(savedInstanceState)

        // Se quita ESP32-CAM. Se mantiene cámara del teléfono + sensores ESP32/TTGO.
        btnScanESP32.visibility = View.GONE

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnCare).setOnClickListener {
            val intent = Intent(this, PlantCareActivity::class.java)
            intent.putExtra("plant_id", plantaId)
            intent.putExtra("plant_name", plantName)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        btnScanCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        findViewById<Button>(R.id.btnConectarESP32).setOnClickListener { conectarESP32() }
    }

    override fun onResume() {
        super.onResume()

        plantaId = intent.getIntExtra("plant_id", -1)
        plantName = intent.getStringExtra("plant_name") ?: "Mi Planta"
        plantType = intent.getStringExtra("plant_type") ?: ""

        val estado = intent.getStringExtra("estado") ?: "Sin diagnóstico"
        val humedad = formatPercent(intent.getStringExtra("humedad") ?: "--")

        tvPlantName.text = plantName
        tvEstado.text = estadoConIcono(estado)
        tvHumedad.text = humedad
        tvHumedadSuelo.text = humedad

        esp32Conectada = false
        limpiarSensores()
        handler.removeCallbacks(refreshRunnable)
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("plantaId", plantaId)
        outState.putString("plantName", plantName)
        outState.putString("plantType", plantType)
        outState.putString("cameraImageUri", cameraImageUri?.toString())
        outState.putBoolean("hasVisibleDiagnosis", hasVisibleDiagnosis)
        outState.putBoolean("isAnalyzingPhoto", isAnalyzingPhoto)
        outState.putString("tvIAPlant", if (::tvIAPlant.isInitialized) tvIAPlant.text.toString() else "")
        outState.putString("tvIAConfianza", if (::tvIAConfianza.isInitialized) tvIAConfianza.text.toString() else "")
        outState.putString("tvIAEstado", if (::tvIAEstado.isInitialized) tvIAEstado.text.toString() else "")
        outState.putString("tvIAConsejo", if (::tvIAConsejo.isInitialized) tvIAConsejo.text.toString() else "")
        outState.putString("tvIAMensajeDashboard", if (::tvIAMensajeDashboard.isInitialized) tvIAMensajeDashboard.text.toString() else "")
        outState.putString("tvEstado", if (::tvEstado.isInitialized) tvEstado.text.toString() else "")
    }

    private fun restoreDashboardState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return

        plantaId = savedInstanceState.getInt("plantaId", plantaId)
        plantName = savedInstanceState.getString("plantName", plantName) ?: plantName
        plantType = savedInstanceState.getString("plantType", plantType) ?: plantType
        cameraImageUri = savedInstanceState.getString("cameraImageUri")?.let { Uri.parse(it) }
        isAnalyzingPhoto = savedInstanceState.getBoolean("isAnalyzingPhoto", false)
        hasVisibleDiagnosis = savedInstanceState.getBoolean("hasVisibleDiagnosis", false)

        val savedTitle = savedInstanceState.getString("tvIAPlant", "") ?: ""
        if (hasVisibleDiagnosis && savedTitle.isNotBlank()) {
            tvIAPlant.text = savedTitle
            tvIAConfianza.text = savedInstanceState.getString("tvIAConfianza", "") ?: ""
            tvIAEstado.text = savedInstanceState.getString("tvIAEstado", "") ?: ""
            tvIAConsejo.text = savedInstanceState.getString("tvIAConsejo", "") ?: ""
            tvIAMensajeDashboard.text = savedInstanceState.getString("tvIAMensajeDashboard", "Última revisión guardada") ?: "Última revisión guardada"
            tvEstado.text = savedInstanceState.getString("tvEstado", tvEstado.text.toString()) ?: tvEstado.text
            layoutEmptyState.visibility = View.GONE
            layoutResult.visibility = View.VISIBLE
        }

        if (isAnalyzingPhoto) {
            resetCameraButton()
            isAnalyzingPhoto = false
        }
    }

    private fun safeUi(block: () -> Unit) {
        if (isFinishing || isDestroyed) return
        runOnUiThread {
            if (!isFinishing && !isDestroyed) block()
        }
    }

    // ============================================================
    // SENSORES
    // ============================================================
    private fun limpiarSensores() {
        tvTemperatura.text = "--°C"
        tvHumedadAmbiente.text = "--%"
        tvHumedadSuelo.text = "--%"
        tvLuz.text = "-- lux"
        tvSalinidad.text = "--"
        tvBateria.text = "--%"
        tvSensorStatus.text = "● Desconectado"
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
                    handler.postDelayed({ fetchSensorData() }, 2500)
                } else {
                    tvSensorStatus.text = "● Error conexión"
                    tvSensorStatus.setTextColor(0xFFEF4444.toInt())
                    Toast.makeText(this, "No se pudo enlazar el ESP32", Toast.LENGTH_SHORT).show()
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

                if (temp.isBlank() || temp == "null") {
                    tvSensorStatus.text = "● Esperando ESP32..."
                    tvSensorStatus.setTextColor(0xFFFCD34D.toInt())
                    return@runOnUiThread
                }

                val humSuelo = formatPercent(data.optString("humedad", ""))
                val humAmbiente = formatPercent(data.optString("humedad_ambiente", ""))
                val luz = data.optString("luz", "")
                val salinidad = data.optString("salinidad", "")
                val bateria = formatPercent(data.optString("bateria", ""))
                val estado = data.optString("estado", "Normal")

                tvTemperatura.text = "${formatNumber(temp)}°C"
                tvHumedad.text = humSuelo
                tvHumedadSuelo.text = humSuelo
                tvHumedadAmbiente.text = humAmbiente
                tvLuz.text = if (luz.isBlank() || luz == "null") "-- lux" else "${formatNumber(luz)} lux"
                tvSalinidad.text = if (salinidad.isBlank() || salinidad == "null") "--" else formatNumber(salinidad)
                tvBateria.text = bateria
                tvEstado.text = estadoConIcono(estado)

                tvSensorStatus.text = "● En vivo"
                tvSensorStatus.setTextColor(0xFF86EFAC.toInt())
            }
        }
    }

    // ============================================================
    // CÁMARA DEL TELÉFONO
    // ============================================================
    private fun openCamera() {
        btnScanCamera.isEnabled = false
        btnScanCamera.text = "Abriendo cámara..."

        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(cacheDir, "scan_$ts.jpg")

        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        cameraLauncher.launch(intent)
    }

    // ============================================================
    // CLASIFICAR + VALIDAR + SUBIR FOTO + GUARDAR HISTORIAL
    // ============================================================
    private fun analizarFoto(bitmap: Bitmap, uri: Uri) {
        btnScanCamera.isEnabled = false
        btnScanCamera.text = "Analizando..."
        isAnalyzingPhoto = true
        hasVisibleDiagnosis = false
        layoutEmptyState.visibility = View.VISIBLE
        layoutResult.visibility = View.GONE
        tvIAMensajeDashboard.text = "Analizando imagen con IA... espera unos segundos."

        Thread {
            try {
                val c = classifier ?: Classifier(this).also { classifier = it }
                val result = c.classifyDetailed(bitmap)
                val registeredSpecies = canonicalSpecies(plantType.ifBlank { plantName })
                val speciesAnalysis = analyzeBySpecies(result, registeredSpecies)
                val labelForDiagnosis = speciesAnalysis.decisionLabel.ifBlank { result.label }
                val parsed = parseLabel(labelForDiagnosis)
                val validation = validateResult(result, parsed, speciesAnalysis)

                safeUi {
                    showDiagnosis(result, parsed, validation, speciesAnalysis)
                }

                if (!validation.accepted) {
                    safeUi {
                        isAnalyzingPhoto = false
                        resetCameraButton()
                        Toast.makeText(this, validation.status, Toast.LENGTH_LONG).show()
                    }
                    return@Thread
                }

                val prefs = getSharedPreferences("agroia", MODE_PRIVATE)
                val usuarioId = prefs.getInt("usuario_id", -1)

                if (usuarioId == -1) {
                    safeUi {
                        isAnalyzingPhoto = false
                        resetCameraButton()
                        Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                ApiHelper.subirImagen(applicationContext, uri, plantaId) { okUpload, filename, _ ->
                    val imagenUrl = if (okUpload) filename else ""
                    val estadoGuardar = validation.status
                    val solucion = solutionFor(parsed.original, parsed.problem, parsed.isHealthy)

                    ApiHelper.guardarResultado(
                        usuarioId = usuarioId,
                        plantaId = plantaId,
                        plantaRegistrada = registeredSpecies,
                        claseDetectada = parsed.original,
                        especieDetectada = parsed.species,
                        problemaDetectado = parsed.problem,
                        estado = estadoGuardar,
                        confianza = result.confidence,
                        solucion = solucion,
                        validacion = validation.validationCode,
                        mensajeValidacion = validation.message,
                        imagenUrl = imagenUrl
                    ) { okSave ->
                        safeUi {
                            isAnalyzingPhoto = false
                            resetCameraButton()
                            if (okSave) {
                                val msg = if (imagenUrl.isNotBlank()) {
                                    "Diagnóstico y foto guardados"
                                } else {
                                    "Diagnóstico guardado sin foto"
                                }
                                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "No se pudo guardar el diagnóstico", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                safeUi {
                    isAnalyzingPhoto = false
                    showErrorDiagnosis("Error analizando la imagen", e.message ?: "Error desconocido")
                    resetCameraButton()
                }
            }
        }.start()
    }

    private fun showDiagnosis(
        result: ClassificationResult,
        parsed: ParsedLabel,
        validation: ValidationResult,
        analysis: SpeciesAnalysis
    ) {
        val speciesLine = "Planta registrada: ${analysis.registeredSpecies.ifBlank { "Sin dato" }}" +
                "\nCoincidencia de especie: ${analysis.registeredSpeciesPercent}%" +
                "\nEspecie más probable: ${analysis.detectedSpecies.ifBlank { "Sin dato" }} (${analysis.detectedSpeciesPercent}%)"

        val healthLine = "\nSano: ${analysis.healthyPercent}% (${analysis.healthySharePercent}% dentro de la especie)" +
                "\nEnfermedad total: ${analysis.diseaseTotalPercent}% (${analysis.diseaseSharePercent}% dentro de la especie)" +
                if (analysis.topDiseaseLabel.isNotBlank()) {
                    "\nEnfermedad sugerida: ${analysis.topDiseaseProblem} (${analysis.topDiseasePercent}%)" +
                            if (analysis.diseaseOptionsCount > 1 && analysis.secondDiseaseLabel.isNotBlank()) {
                                "\nSegunda enfermedad: ${analysis.secondDiseaseProblem} (${analysis.secondDiseasePercent}%)" +
                                        "\nMargen entre enfermedades: ${analysis.diseaseMarginPercent}%"
                            } else ""
                } else ""

        tvIAPlant.text = if (validation.accepted) {
            "${parsed.original}\nDetectado como: ${parsed.species}"
        } else {
            when (validation.validationCode) {
                "SALUD_NO_CONCLUYENTE" -> "Planta reconocida\nSalud/enfermedad no concluyente"
                "ENFERMEDAD_NO_CONCLUYENTE" -> "Planta reconocida\nTipo de enfermedad no concluyente"
                "ESPECIE_NO_SEGURA" -> "Foto no confirmada\nNo se reconoce bien la especie"
                "NO_COINCIDE" -> "La foto no coincide\nIA sugirió: ${result.label}"
                else -> "Diagnóstico no aceptado\nIA sugirió: ${result.label}"
            }
        }

        tvIAConfianza.text = "$speciesLine$healthLine"
        tvIAEstado.text = validation.status
        tvIAConsejo.text = validation.message
        tvIAMensajeDashboard.text = "Última revisión: ${validation.status}"

        val estadoVisible = if (validation.accepted) validation.status else "Sin diagnóstico"
        tvEstado.text = estadoConIcono(estadoVisible)

        layoutEmptyState.visibility = View.GONE
        layoutResult.visibility = View.VISIBLE
        hasVisibleDiagnosis = true
    }

    private fun showErrorDiagnosis(title: String, msg: String) {
        tvIAPlant.text = title
        tvIAConfianza.text = ""
        tvIAEstado.text = "No se pudo analizar"
        tvIAConsejo.text = msg
        tvIAMensajeDashboard.text = "Error analizando la foto. Intenta otra vez."
        layoutEmptyState.visibility = View.GONE
        layoutResult.visibility = View.VISIBLE
        hasVisibleDiagnosis = true
    }

    private fun analyzeBySpecies(result: ClassificationResult, registeredSpecies: String): SpeciesAnalysis {
        val speciesScores = mutableMapOf<String, Float>()
        var noPlantScore = 0f

        result.probabilities.forEach { (label, prob) ->
            val parsed = parseLabel(label)
            if (parsed.isNoPlant) {
                noPlantScore += prob
            } else if (parsed.species.isNotBlank()) {
                speciesScores[parsed.species] = (speciesScores[parsed.species] ?: 0f) + prob
            }
        }

        val detected = speciesScores.maxByOrNull { it.value }
        val detectedSpecies = detected?.key ?: ""
        val detectedScore = detected?.value ?: 0f
        val targetSpecies = registeredSpecies.ifBlank { detectedSpecies }
        val registeredScore = speciesScores[targetSpecies] ?: 0f

        val labelsForRegistered = result.probabilities
            .filter { (label, _) -> parseLabel(label).species == targetSpecies }
            .entries
            .sortedByDescending { it.value }

        val healthyEntries = labelsForRegistered.filter { parseLabel(it.key).isHealthy }
        val diseaseEntries = labelsForRegistered.filter {
            val parsed = parseLabel(it.key)
            !parsed.isHealthy && !parsed.isNoPlant
        }

        val healthyScore = healthyEntries.sumOf { it.value.toDouble() }.toFloat()
        val diseaseTotalScore = diseaseEntries.sumOf { it.value.toDouble() }.toFloat()
        val topHealthy = healthyEntries.maxByOrNull { it.value }

        val groupedDiseases = diseaseEntries
            .groupBy { parseLabel(it.key).problem }
            .mapValues { entry -> entry.value.sumOf { it.value.toDouble() }.toFloat() }
            .entries
            .sortedByDescending { it.value }

        val topDiseaseGroup = groupedDiseases.getOrNull(0)
        val secondDiseaseGroup = groupedDiseases.getOrNull(1)
        val topDiseaseProblem = topDiseaseGroup?.key ?: ""
        val secondDiseaseProblem = secondDiseaseGroup?.key ?: ""
        val topDiseaseScore = topDiseaseGroup?.value ?: 0f
        val secondDiseaseScore = secondDiseaseGroup?.value ?: 0f

        val topDiseaseLabel = diseaseEntries
            .filter { parseLabel(it.key).problem == topDiseaseProblem }
            .maxByOrNull { it.value }
            ?.key ?: ""
        val secondDiseaseLabel = diseaseEntries
            .filter { parseLabel(it.key).problem == secondDiseaseProblem }
            .maxByOrNull { it.value }
            ?.key ?: ""

        val diseaseOptionsCount = groupedDiseases.size
        val healthyShare = if (registeredScore > 0f) healthyScore / registeredScore else 0f
        val diseaseShare = if (registeredScore > 0f) diseaseTotalScore / registeredScore else 0f
        val topDiseaseShare = if (diseaseTotalScore > 0f) topDiseaseScore / diseaseTotalScore else 0f

        var decisionLabel = ""
        var decisionProblem = ""
        var decisionHealthy = false
        var decisionConclusive = false
        var decisionReason = ""

        when {
            healthyScore >= diseaseTotalScore + minHealthStateMargin && healthyShare >= minWithinSpeciesShare -> {
                decisionLabel = topHealthy?.key ?: "$targetSpecies Sano"
                decisionProblem = "Sin enfermedad visible"
                decisionHealthy = true
                decisionConclusive = true
                decisionReason = "La clase sana supera claramente a las clases enfermas."
            }

            diseaseTotalScore >= healthyScore + minHealthStateMargin && diseaseShare >= minWithinSpeciesShare -> {
                if (diseaseOptionsCount <= 1) {
                    decisionLabel = topDiseaseLabel.ifBlank { "$targetSpecies enfermo" }
                    decisionProblem = topDiseaseProblem.ifBlank { "Problema detectado" }
                    decisionHealthy = false
                    decisionConclusive = true
                    decisionReason = "La planta solo tiene una enfermedad entrenada y la suma de enfermedad supera a sano."
                } else if (topDiseaseScore >= secondDiseaseScore + minDiseaseTypeMargin &&
                    topDiseaseShare >= minDiseaseShareWithinDiseases
                ) {
                    decisionLabel = topDiseaseLabel
                    decisionProblem = topDiseaseProblem
                    decisionHealthy = false
                    decisionConclusive = true
                    decisionReason = "La enfermedad principal supera claramente a las otras enfermedades de la misma planta."
                } else {
                    decisionProblem = topDiseaseProblem.ifBlank { "Problema detectado" }
                    decisionConclusive = false
                    decisionReason = "La planta parece enferma, pero no se distingue con seguridad entre sus enfermedades entrenadas."
                }
            }

            else -> {
                decisionConclusive = false
                decisionReason = "La IA duda entre sano y enfermo dentro de la misma especie."
            }
        }

        return SpeciesAnalysis(
            registeredSpecies = targetSpecies,
            detectedSpecies = detectedSpecies,
            registeredSpeciesScore = registeredScore,
            detectedSpeciesScore = detectedScore,
            noPlantScore = noPlantScore,
            healthyScore = healthyScore,
            diseaseTotalScore = diseaseTotalScore,
            topHealthyLabel = topHealthy?.key ?: "",
            topDiseaseLabel = topDiseaseLabel,
            topDiseaseProblem = topDiseaseProblem,
            topDiseaseScore = topDiseaseScore,
            secondDiseaseLabel = secondDiseaseLabel,
            secondDiseaseProblem = secondDiseaseProblem,
            secondDiseaseScore = secondDiseaseScore,
            diseaseOptionsCount = diseaseOptionsCount,
            decisionLabel = decisionLabel,
            decisionProblem = decisionProblem,
            decisionHealthy = decisionHealthy,
            decisionConclusive = decisionConclusive,
            decisionReason = decisionReason
        )
    }

    private fun validateResult(
        result: ClassificationResult,
        parsed: ParsedLabel,
        analysis: SpeciesAnalysis
    ): ValidationResult {
        if (analysis.noPlantScore >= noPlantRejectScore && analysis.noPlantScore >= analysis.registeredSpeciesScore) {
            return ValidationResult(
                accepted = false,
                status = "No es planta compatible",
                message = "La imagen parece ser fondo, objeto o una planta fuera del modelo. Toma una foto clara de una planta registrada.",
                validationCode = "NO_PLANTA"
            )
        }

        if (analysis.registeredSpeciesScore < minSpeciesScore) {
            return ValidationResult(
                accepted = false,
                status = "Especie no segura",
                message = "La IA no puede confirmar que la imagen sea ${analysis.registeredSpecies}. Coincidencia de especie: ${analysis.registeredSpeciesPercent}%. Toma otra foto con la planta completa, buena luz y fondo limpio.",
                validationCode = "ESPECIE_NO_SEGURA"
            )
        }

        if (analysis.detectedSpecies.isNotBlank() &&
            analysis.registeredSpecies.isNotBlank() &&
            analysis.detectedSpecies != analysis.registeredSpecies &&
            analysis.detectedSpeciesScore > analysis.registeredSpeciesScore + 0.15f
        ) {
            return ValidationResult(
                accepted = false,
                status = "No coincide con esta planta",
                message = "Esta ficha es de '${analysis.registeredSpecies}', pero la IA detectó más fuerte '${analysis.detectedSpecies}' (${analysis.detectedSpeciesPercent}%). No se guardó diagnóstico para evitar un falso resultado.",
                validationCode = "NO_COINCIDE"
            )
        }

        if (!analysis.decisionConclusive) {
            val code = if (analysis.diseaseTotalScore > analysis.healthyScore + minHealthStateMargin && analysis.diseaseOptionsCount > 1) {
                "ENFERMEDAD_NO_CONCLUYENTE"
            } else {
                "SALUD_NO_CONCLUYENTE"
            }

            val msg = if (code == "ENFERMEDAD_NO_CONCLUYENTE") {
                "La planta sí parece ser ${analysis.registeredSpecies}, pero no se puede distinguir con seguridad entre sus enfermedades entrenadas. " +
                        "Principal: ${analysis.topDiseaseProblem.ifBlank { "sin dato" }} (${analysis.topDiseasePercent}%). " +
                        "Segunda: ${analysis.secondDiseaseProblem.ifBlank { "sin dato" }} (${analysis.secondDiseasePercent}%). " +
                        "Toma una foto más cerca del síntoma: hojas, tallo, manchas o plaga."
            } else {
                "La planta sí parece ser ${analysis.registeredSpecies}, pero la IA duda entre sana y enferma. " +
                        "Sano: ${analysis.healthyPercent}%, enfermedad total: ${analysis.diseaseTotalPercent}%. " +
                        "Toma otra foto con buena luz, enfoque y mostrando claramente hojas/tallo."
            }

            return ValidationResult(
                accepted = false,
                status = if (code == "ENFERMEDAD_NO_CONCLUYENTE") "Enfermedad no concluyente" else "Salud no concluyente",
                message = msg,
                validationCode = code
            )
        }

        return if (analysis.decisionHealthy) {
            ValidationResult(
                accepted = true,
                status = "Saludable",
                message = solutionFor(parsed.original, "Sin enfermedad visible", true),
                validationCode = "VALIDO"
            )
        } else {
            ValidationResult(
                accepted = true,
                status = "Problema detectado: ${analysis.decisionProblem}",
                message = solutionFor(parsed.original, analysis.decisionProblem, false),
                validationCode = "VALIDO"
            )
        }
    }

    private fun parseLabel(label: String): ParsedLabel {
        val clean = label.trim()
        val lower = clean.lowercase(Locale.ROOT)

        val noPlant = lower.contains("no planta") ||
                lower.contains("fondo") ||
                lower.contains("background") ||
                lower.contains("objeto") ||
                lower.contains("random")

        if (noPlant) {
            return ParsedLabel(clean, "No planta", "No planta / Fondo", false, true)
        }

        val species = canonicalSpecies(clean)
        val healthy = lower.contains("sano") || lower.contains("sana") || lower.contains("saludable")

        val problem = when {
            healthy -> "Sin enfermedad visible"
            lower.contains("roya") -> "Roya"
            lower.contains("pulg") -> "Pulgones"
            lower.contains("hongo") -> "Hongo"
            lower.contains("infeccion") || lower.contains("infección") -> "Infección"
            lower.contains("falta de luz") -> "Falta de luz"
            lower.contains("tallo seco") -> "Tallo seco"
            lower.contains("cochinilla") -> "Cochinilla escamosa"
            lower.contains("enfermo-") -> clean.substringAfter("enfermo-", "Problema detectado")
            lower.contains("enferma-") -> clean.substringAfter("enferma-", "Problema detectado")
            else -> "Problema detectado"
        }

        return ParsedLabel(clean, species, problem, healthy, false)
    }

    private fun canonicalSpecies(value: String): String {
        val v = value.lowercase(Locale.ROOT)
            .replace("_", " ")
            .replace("-", " ")
            .trim()

        return when {
            v.contains("rosal") || v.contains("rosa") -> "Rosal"
            v.contains("cactus") -> "Cactus"
            v.contains("cinta") -> "Cinta"
            v.contains("cola de burro") || v.contains("cola de borrego") || v.contains("sedum") -> "Cola de borrego"
            v.contains("alcatraz") -> "Alcatraz"
            else -> ""
        }
    }

    private fun solutionFor(label: String, problem: String, healthy: Boolean): String {
        if (healthy) {
            return when (canonicalSpecies(label)) {
                "Rosal" -> "Se observa saludable. Mantén buena luz, riego moderado y revisa hojas una vez por semana."
                "Cactus" -> "Se observa saludable. Riega solo cuando el sustrato esté completamente seco."
                "Cinta" -> "Se observa saludable. Mantén luz indirecta y riego moderado."
                "Cola de borrego" -> "Se observa saludable. Necesita buena luz y poco riego; evita exceso de agua."
                "Alcatraz" -> "Se observa saludable. Mantén sustrato ligeramente húmedo y luz indirecta brillante."
                else -> "La planta se observa saludable. Mantén monitoreo normal."
            }
        }

        return when (problem.lowercase(Locale.ROOT)) {
            "roya" -> "Retira hojas afectadas, mejora ventilación y aplica fungicida a base de azufre o cobre. Evita mojar el follaje."
            "pulgones" -> "Limpia hojas y brotes. Aplica jabón potásico o aceite de neem. Revisa el envés de las hojas."
            "hongo" -> "Reduce riego, mejora ventilación y aplica fungicida. Retira tejido blando o manchado."
            "infección" -> "Aísla la planta, elimina zonas dañadas y cambia sustrato si hay pudrición. Usa herramienta desinfectada."
            "falta de luz" -> "Mueve la planta a una zona con más luz indirecta. Evita sol directo fuerte de golpe."
            "tallo seco" -> "Revisa raíces y riego. Elimina partes secas y ajusta riego cuando el sustrato esté seco."
            "cochinilla escamosa" -> "Limpia hojas con algodón y alcohol isopropílico. Aísla la planta y aplica tratamiento contra cochinilla."
            else -> "Revisa luz, riego, manchas, tallos y plagas. Repite la foto si el síntoma no es claro."
        }
    }

    private fun resetCameraButton() {
        isAnalyzingPhoto = false
        btnScanCamera.isEnabled = true
        btnScanCamera.text = "Tomar otra foto"
    }

    // ============================================================
    // FORMATO
    // ============================================================
    private fun formatPercent(value: String): String {
        val v = value.trim()
        if (v.isEmpty() || v == "null" || v == "--") return "--%"
        return if (v.endsWith("%")) v else "${formatNumber(v)}%"
    }

    private fun formatNumber(value: String): String {
        val n = value.toDoubleOrNull()
        return if (n == null) {
            value
        } else {
            if (n % 1.0 == 0.0) n.toInt().toString() else String.format(Locale.US, "%.1f", n)
        }
    }

    private fun estadoConIcono(estado: String): String {
        val limpio = estado
            .replace("🟢", "")
            .replace("🟡", "")
            .replace("🔴", "")
            .trim()

        return when {
            limpio.contains("sin diagnóstico", ignoreCase = true) -> "Sin diagnóstico"
            limpio.contains("no coincide", ignoreCase = true) -> "Sin diagnóstico"
            limpio.contains("no confiable", ignoreCase = true) -> "Sin diagnóstico"
            limpio.contains("ambiguo", ignoreCase = true) -> "Sin diagnóstico"
            limpio.contains("riego", ignoreCase = true) -> "Necesita riego"
            limpio.contains("exceso", ignoreCase = true) -> "Exceso de humedad"
            limpio.contains("riesgo", ignoreCase = true) -> "En riesgo"
            limpio.contains("atención", ignoreCase = true) -> "Requiere atención"
            limpio.contains("problema", ignoreCase = true) -> limpio
            limpio.contains("enfer", ignoreCase = true) -> limpio
            limpio.contains("normal", ignoreCase = true) -> "Normal"
            limpio.contains("salud", ignoreCase = true) -> "Saludable"
            else -> limpio
        }
    }
}
