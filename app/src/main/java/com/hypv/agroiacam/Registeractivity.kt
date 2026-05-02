package com.hypv.agroiacam

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hypv.agroiacam.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCreate.setOnClickListener { doRegister() }
    }

    private fun doRegister() {
        val usuario  = binding.etUser.text.toString().trim()
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPass.text.toString().trim()

        if (usuario.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            val result = ApiHelper.register(usuario, email, password)

            runOnUiThread {
                setLoading(false)
                if (result.success) {
                    Toast.makeText(this@RegisterActivity, "Cuenta creada. Inicia sesión.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@RegisterActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnCreate.isEnabled = !loading
        binding.btnCreate.text = if (loading) "Registrando..." else "Registrarse"
    }
}