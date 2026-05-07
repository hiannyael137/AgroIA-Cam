package com.hypv.agroiacam

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

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

    // Lanzador para galería
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            Glide.with(this).load(selectedImageUri).circleCrop().into(imgPreview)
        }
    }

    // Lanzador para cámara
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = cameraImageUri
            Glide.with(this).load(selectedImageUri).circleCrop().into(imgPreview)
        }
    }

    // Permiso de cámara
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
                == PackageManager.PERMISSION_GRANTED) {
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

    private fun setupSpinner() {
        val plants = listOf(
            "🌹 Rosa", "🌵 Sábila", "🌿 Menta", "🌸 Orquídea", "🪴 Helecho"
        )
        val adapter = ArrayAdapter(this, R.layout.spinner_item, plants)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinnerPlantType.adapter = adapter
    }

    private fun savePlant() {
        val plantType = spinnerPlantType.selectedItem.toString()
        val customName = etPlantName.text.toString().trim()

        if (customName.isEmpty()) {
            Toast.makeText(this, "Ponle un nombre a tu planta", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("agroia", MODE_PRIVATE)
        val usuarioId = prefs.getInt("usuario_id", 0)

        // Si hay imagen la subimos primero, si no guardamos directo
        if (selectedImageUri != null) {
            uploadImageAndSave(usuarioId, plantType, customName)
        } else {
            savePlantToServer(usuarioId, plantType, customName, "")
        }
    }

    private fun uploadImageAndSave(usuarioId: Int, plantType: String, customName: String) {
        val uri = selectedImageUri ?: return

        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val bytes = inputStream.readBytes()
            inputStream.close()

            if (bytes.isEmpty()) {
                runOnUiThread {
                    Toast.makeText(this, "No se pudo leer la imagen", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val fileName = "plant_${System.currentTimeMillis()}.jpg"

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "imagen",
                    fileName,
                    bytes.toRequestBody("image/*".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("${ApiHelper.BASE_URL}/uploadImage")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@AddPlantActivity, "Error subiendo imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onResponse(call: Call, response: Response) {
                    val result = response.body?.string()
                    runOnUiThread {
                        try {
                            val jsonResponse = JSONObject(result!!)
                            val imagenUrl = jsonResponse.optString("filename", "")
                            savePlantToServer(usuarioId, plantType, customName, imagenUrl)
                        } catch (e: Exception) {
                            Toast.makeText(this@AddPlantActivity, "Error procesando respuesta", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })

        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Error leyendo imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePlantToServer(usuarioId: Int, plantType: String, customName: String, imagenUrl: String) {
        val json = JSONObject()
        json.put("usuario_id", usuarioId)
        json.put("tipo_planta", plantType)
        json.put("nombre_personalizado", customName)
        json.put("estado", "Saludable")
        json.put("humedad", "0%")
        json.put("ultimo_riego", "Sin riego")
        json.put("salud", 100)
        json.put("imagen_url", imagenUrl)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/addPlant")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AddPlantActivity, "Error conectando con Node-RED", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@AddPlantActivity, "Planta guardada correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        })
    }
}