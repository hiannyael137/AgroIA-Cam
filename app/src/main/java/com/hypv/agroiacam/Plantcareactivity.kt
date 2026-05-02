package com.hypv.agroiacam

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hypv.agroiacam.databinding.ActivityPlantCareBinding
import kotlinx.coroutines.launch

class PlantCareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlantCareBinding
    private var usuario = ""

    private val plantas = listOf(
        "Pothos dorado",
        "Monstera deliciosa",
        "Sansevieria",
        "Cactus navideño",
        "Suculenta echeveria"
    )

    private val actividades = listOf(
        "💧 Riego",
        "🌿 Fertilización",
        "✂️ Poda",
        "🪴 Trasplante",
        "🧼 Limpieza de hojas",
        "🔍 Revisión general"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlantCareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usuario = getSharedPreferences("session", MODE_PRIVATE)
            .getString("usuario", "") ?: ""

        setupSpinners()

        binding.btnSaveActivity.setOnClickListener { saveActivity() }
    }

    private fun setupSpinners() {
        binding.spinnerPlant.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            plantas
        )
        binding.spinnerActivity.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            actividades
        )
    }

    private fun saveActivity() {
        val planta    = binding.spinnerPlant.selectedItem?.toString() ?: ""
        val actividad = binding.spinnerActivity.selectedItem?.toString() ?: ""
        val notas     = binding.etNotes.text.toString().trim()

        if (planta.isEmpty()) {
            Toast.makeText(this, "Selecciona una planta", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSaveActivity.isEnabled = false
        binding.btnSaveActivity.text      = "Guardando..."

        lifecycleScope.launch {
            val result = ApiHelper.saveCareActivity(usuario, planta, actividad, notas)

            runOnUiThread {
                binding.btnSaveActivity.isEnabled = true
                binding.btnSaveActivity.text      = "Guardar Registro 🌱"

                if (result.success) {
                    Toast.makeText(this@PlantCareActivity, "✅ Registro guardado", Toast.LENGTH_SHORT).show()
                    binding.etNotes.text?.clear()
                } else {
                    Toast.makeText(this@PlantCareActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}