package com.hypv.agroiacam

// app/src/main/java/com/hypv/agroiacam/DashboardActivity.kt

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    // ── Vistas ────────────────────────────────────────────
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var layoutResult:     LinearLayout
    private lateinit var tvPlantName:      TextView
    private lateinit var tvConfianza:      TextView
    private lateinit var tvEstado:         TextView
    private lateinit var tvConsejo:        TextView
    private lateinit var btnTakePhoto:     Button

    // ── Lanzadores de cámara / galería ───────────────────
    // TakePicturePreview devuelve un thumbnail Bitmap sin necesitar FileProvider
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { analizarFoto(it) }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
            analizarFoto(bitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Vincular vistas
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        layoutResult     = findViewById(R.id.layoutResult)
        tvPlantName      = findViewById(R.id.tvPlantName)
        tvConfianza      = findViewById(R.id.tvConfianza)
        tvEstado         = findViewById(R.id.tvEstado)
        tvConsejo        = findViewById(R.id.tvConsejo)
        btnTakePhoto     = findViewById(R.id.btnTakePhoto)

        // Estado inicial — vacío
        mostrarEstadoVacio()

        // ── Botón tomar foto: abre diálogo cámara/galería ─
        btnTakePhoto.setOnClickListener {
            mostrarOpcionesFoto()
        }

        // ── Navegación ─────────────────────────────────────
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnCare).setOnClickListener {
            startActivity(Intent(this, PlantCareActivity::class.java))
        }
        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    // ── Muestra el selector cámara / galería ──────────────
    private fun mostrarOpcionesFoto() {
        val opciones = arrayOf("Tomar foto con cámara", "Elegir de galería")
        android.app.AlertDialog.Builder(this)
            .setTitle("Seleccionar imagen")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(null)
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    // ── Clasificar y mostrar resultado ────────────────────
    private fun analizarFoto(bitmap: Bitmap) {
        btnTakePhoto.isEnabled = false
        btnTakePhoto.text      = "Analizando..."

        // Ejecutar en hilo de fondo para no bloquear la UI
        Thread {
            val result = Classifier(this).classify(bitmap)
            val pct    = "%.1f".format(result.confianza * 100)

            runOnUiThread {
                // Mostrar resultados
                mostrarResultado(result, pct)

                // Guardar en BD si hay sesión activa
                val prefs     = getSharedPreferences("agroia", MODE_PRIVATE)
                val usuarioId = prefs.getInt("usuario_id", -1)
                if (usuarioId != -1) {
                    ApiHelper.guardarResultado(
                        usuarioId = usuarioId,
                        planta    = result.planta,
                        estado    = result.estadoTexto,
                        confianza = result.confianza
                    ) { ok ->
                        if (!ok) {
                            runOnUiThread {
                                Toast.makeText(this,
                                    "No se pudo guardar en servidor",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                btnTakePhoto.isEnabled = true
                btnTakePhoto.text      = "Nueva foto"
            }
        }.start()
    }

    // ── Actualizar UI con el resultado ────────────────────
    private fun mostrarResultado(result: ClasificacionResult, pct: String) {
        // Ocultar estado vacío, mostrar resultado
        layoutEmptyState.visibility = View.GONE
        layoutResult.visibility     = View.VISIBLE

        tvPlantName.text  = result.planta
        tvConfianza.text  = "Confianza: $pct%"
        tvConsejo.text    = result.consejo

        // Texto e ícono de estado con color
        val (texto, color) = when (result.estadoColor) {
            0    -> Pair("Saludable",  "#4ADE80")  // verde brillante
            1    -> Pair("Monitorear","#FACC15")   // amarillo
            else -> Pair("En riesgo", "#F87171")   // rojo
        }
        tvEstado.text      = texto
        tvEstado.setTextColor(Color.parseColor(color))
    }

    // ── Estado vacío inicial ──────────────────────────────
    private fun mostrarEstadoVacio() {
        layoutEmptyState.visibility = View.VISIBLE
        layoutResult.visibility     = View.GONE
    }
}