package com.hypv.agroiacam

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddPlantActivity : AppCompatActivity() {

    private lateinit var spinnerPlantType: Spinner
    private lateinit var etPlantName: EditText
    private lateinit var btnSavePlant: Button
    private lateinit var btnCamera: Button
    private lateinit var btnGallery: Button
    private lateinit var imgPreview: ImageView

    private val client = OkHttpClient()
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    // Lista cerrada: solo plantas que el modelo de Teachable Machine conoce.
    // Si el modelo usa "Cola de borrego", no guardes "cola_de_burro" como tipo,
    // porque luego la validación IA no podrá compararla bien.
    private val plantTypes = listOf(
        "Rosal",
        "Cactus",
        "Cinta",
        "Cola de borrego",
        "Alcatraz"
    )

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { imgPreview.setImageURI(it) }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = cameraImageUri
            selectedImageUri?.let { imgPreview.setImageURI(it) }
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
        setContentView(R.layout.activity_add_plant)

        spinnerPlantType = findViewById(R.id.spinnerPlantType)
        etPlantName = findViewById(R.id.etPlantName)
        btnSavePlant = findViewById(R.id.btnSavePlant)
        btnCamera = findViewById(R.id.btnCamera)
        btnGallery = findViewById(R.id.btnGallery)
        imgPreview = findViewById(R.id.imgPreview)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        setupSpinner()

        btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        btnGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(intent)
        }

        btnSavePlant.setOnClickListener { savePlant() }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, R.layout.spinner_item, plantTypes)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinnerPlantType.adapter = adapter
    }

    private fun openCamera() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFile = File(cacheDir, "plant_$timeStamp.jpg")

        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            imageFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        cameraLauncher.launch(intent)
    }

    private fun savePlant() {
        val plantType = spinnerPlantType.selectedItem.toString()
        val customName = etPlantName.text.toString().trim()

        if (customName.isEmpty()) {
            Toast.makeText(this, "Ponle un nombre a tu planta", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Selecciona una foto de la planta", Toast.LENGTH_SHORT).show()
            return
        }

        val usuarioId = getSharedPreferences("agroia", MODE_PRIVATE).getInt("usuario_id", 0)
        if (usuarioId <= 0) {
            Toast.makeText(this, "Sesión inválida. Inicia sesión otra vez", Toast.LENGTH_SHORT).show()
            return
        }

        btnSavePlant.isEnabled = false
        btnSavePlant.text = "Guardando..."

        val uri = selectedImageUri ?: return
        ApiHelper.subirImagen(this, uri, null) { ok, filename, error ->
            runOnUiThread {
                if (!ok) {
                    btnSavePlant.isEnabled = true
                    btnSavePlant.text = "Guardar planta"
                    Toast.makeText(this, error ?: "Error subiendo imagen", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                savePlantToServer(
                    usuarioId = usuarioId,
                    plantType = plantType,
                    customName = customName,
                    imagenUrl = filename
                )
            }
        }
    }

    private fun savePlantToServer(
        usuarioId: Int,
        plantType: String,
        customName: String,
        imagenUrl: String
    ) {
        val json = JSONObject().apply {
            put("usuario_id", usuarioId)
            put("tipo_planta", plantType)
            put("nombre_personalizado", customName)
            put("estado", "Sin análisis")
            put("humedad", 0)
            put("ultimo_riego", "Sin riego")
            put("salud", 100)
            put("imagen_url", imagenUrl)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/addPlant")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    btnSavePlant.isEnabled = true
                    btnSavePlant.text = "Guardar planta"
                    Toast.makeText(
                        this@AddPlantActivity,
                        "Error conectando con Node-RED: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val text = response.body?.string().orEmpty()
                runOnUiThread {
                    btnSavePlant.isEnabled = true
                    btnSavePlant.text = "Guardar planta"
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@AddPlantActivity,
                            "Planta y foto guardadas correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(
                            this@AddPlantActivity,
                            "No se pudo guardar la planta: $text",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }
}
