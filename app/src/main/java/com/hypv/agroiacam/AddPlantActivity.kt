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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
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

    // =========================
    // PLANTAS REALES
    // =========================
    private val plantMap = mapOf(
        "🌹 Rosal blanco" to "rosal_blanco",
        "🪴 Cola de burro" to "cola_de_burro",
        "🌿 Pasto de limón" to "pasto_de_limon",
        "🤍 Alcatraz" to "alcatraz",
        "🌱 Cinta" to "cinta"
    )

    // =========================
    // GALERIA
    // =========================
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {

            selectedImageUri = result.data?.data

            if (selectedImageUri != null) {
                imgPreview.setImageURI(selectedImageUri)
            }
        }
    }

    // =========================
    // CAMARA
    // =========================
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {

            selectedImageUri = cameraImageUri

            if (selectedImageUri != null) {
                imgPreview.setImageURI(selectedImageUri)
            }
        }
    }

    // =========================
    // PERMISO CAMARA
    // =========================
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->

        if (granted) {
            openCamera()
        } else {

            Toast.makeText(
                this,
                "Permiso de cámara denegado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================
    // ON CREATE
    // =========================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_plant)

        spinnerPlantType = findViewById(R.id.spinnerPlantType)
        etPlantName = findViewById(R.id.etPlantName)
        btnSavePlant = findViewById(R.id.btnSavePlant)
        btnCamera = findViewById(R.id.btnCamera)
        btnGallery = findViewById(R.id.btnGallery)
        imgPreview = findViewById(R.id.imgPreview)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        setupSpinner()

        // =========================
        // BOTON CAMARA
        // =========================
        btnCamera.setOnClickListener {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                openCamera()

            } else {

                cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }

        // =========================
        // BOTON GALERIA
        // =========================
        btnGallery.setOnClickListener {

            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )

            galleryLauncher.launch(intent)
        }

        // =========================
        // GUARDAR PLANTA
        // =========================
        btnSavePlant.setOnClickListener {
            savePlant()
        }
    }

    // =========================
    // SPINNER
    // =========================
    private fun setupSpinner() {

        val plants = listOf(
            "🌹 Rosal blanco",
            "🪴 Cola de burro",
            "🌿 Pasto de limón",
            "🤍 Alcatraz",
            "🌱 Cinta"
        )

        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            plants
        )

        adapter.setDropDownViewResource(
            R.layout.spinner_item
        )

        spinnerPlantType.adapter = adapter
    }

    // =========================
    // ABRIR CAMARA
    // =========================
    private fun openCamera() {

        val timeStamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())

        val imageFile = File(
            cacheDir,
            "plant_$timeStamp.jpg"
        )

        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            imageFile
        )

        val intent = Intent(
            MediaStore.ACTION_IMAGE_CAPTURE
        )

        intent.putExtra(
            MediaStore.EXTRA_OUTPUT,
            cameraImageUri
        )

        cameraLauncher.launch(intent)
    }

    // =========================
    // GUARDAR PLANTA
    // =========================
    private fun savePlant() {

        val plantDisplay =
            spinnerPlantType.selectedItem.toString()

        val plantType =
            plantMap[plantDisplay] ?: "desconocida"

        val customName =
            etPlantName.text.toString().trim()

        // VALIDAR NOMBRE
        if (customName.isEmpty()) {

            Toast.makeText(
                this,
                "Ponle un nombre a tu planta",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // VALIDAR FOTO
        if (selectedImageUri == null) {

            Toast.makeText(
                this,
                "Selecciona una foto de la planta",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val prefs = getSharedPreferences(
            "agroia",
            MODE_PRIVATE
        )

        val usuarioId =
            prefs.getInt("usuario_id", 0)

        uploadImageAndSave(
            usuarioId,
            plantType,
            customName
        )
    }

    // =========================
    // SUBIR IMAGEN
    // =========================
    private fun uploadImageAndSave(
        usuarioId: Int,
        plantType: String,
        customName: String
    ) {

        val uri = selectedImageUri ?: return

        try {

            val inputStream =
                contentResolver.openInputStream(uri)

            val bytes =
                inputStream?.readBytes()

            inputStream?.close()

            if (bytes == null || bytes.isEmpty()) {

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "No se pudo leer la imagen",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                return
            }

            val fileName =
                "plant_${System.currentTimeMillis()}.jpg"

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "imagen",
                    fileName,
                    bytes.toRequestBody(
                        "image/*".toMediaTypeOrNull()
                    )
                )
                .build()

            val request = Request.Builder()
                .url("${ApiHelper.BASE_URL}/uploadImage")
                .post(requestBody)
                .build()

            client.newCall(request)
                .enqueue(object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {

                        runOnUiThread {

                            Toast.makeText(
                                this@AddPlantActivity,
                                "Error subiendo imagen",
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

                        runOnUiThread {

                            try {

                                val jsonResponse =
                                    JSONObject(result!!)

                                val imagenUrl =
                                    jsonResponse.optString(
                                        "filename",
                                        ""
                                    )

                                savePlantToServer(
                                    usuarioId,
                                    plantType,
                                    customName,
                                    imagenUrl
                                )

                            } catch (e: Exception) {

                                Toast.makeText(
                                    this@AddPlantActivity,
                                    "Error procesando respuesta",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                })

        } catch (e: Exception) {

            runOnUiThread {

                Toast.makeText(
                    this,
                    "Error leyendo imagen",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =========================
    // GUARDAR EN NODE-RED
    // =========================
    private fun savePlantToServer(
        usuarioId: Int,
        plantType: String,
        customName: String,
        imagenUrl: String
    ) {

        val json = JSONObject()

        json.put("usuario_id", usuarioId)
        json.put("tipo_planta", plantType)
        json.put("nombre_personalizado", customName)

        json.put("estado", "Saludable")
        json.put("humedad", "0%")
        json.put("ultimo_riego", "Sin riego")
        json.put("salud", 100)

        json.put("imagen_url", imagenUrl)

        val body = json.toString()
            .toRequestBody(
                "application/json".toMediaType()
            )

        val request = Request.Builder()
            .url("${ApiHelper.BASE_URL}/addPlant")
            .post(body)
            .build()

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    runOnUiThread {

                        Toast.makeText(
                            this@AddPlantActivity,
                            "Error conectando con Node-RED",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    runOnUiThread {

                        Toast.makeText(
                            this@AddPlantActivity,
                            "Planta guardada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        setResult(RESULT_OK)

                        finish()
                    }
                }
            })
    }
}