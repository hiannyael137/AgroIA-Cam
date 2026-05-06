package com.hypv.agroiacam

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val btnCare = findViewById<Button>(R.id.btnCare)
        val btnHistory = findViewById<Button>(R.id.btnHistory)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnCare.setOnClickListener {
            startActivity(Intent(this, PlantCareActivity::class.java))
        }

        btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }
}