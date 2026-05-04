package com.hypv.agroiacam

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dashboard)

        val btnCare =
            findViewById<Button>(R.id.btnCare)

        val btnHistory =
            findViewById<Button>(R.id.btnHistory)

        btnCare.setOnClickListener {

            startActivity(
                Intent(this, PlantCareActivity::class.java)
            )

        }

        btnHistory.setOnClickListener {

            startActivity(
                Intent(this, HistoryActivity::class.java)
            )

        }
    }
}