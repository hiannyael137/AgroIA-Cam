package com.hypv.agroiacam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hypv.agroiacam.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Si ya hay sesión activa, ir directo a Mis Plantas
        val prefs = getSharedPreferences("session", MODE_PRIVATE)
        if (prefs.getString("usuario", null) != null) {
            goToMain()
            return
        }

        binding.btnLogin.setOnClickListener { doLogin() }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun doLogin() {
        val usuario  = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (usuario.isEmpty() || password.isEmpty()) {
            showError("Completa todos los campos")
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            val result = ApiHelper.login(usuario, password)

            runOnUiThread {
                setLoading(false)
                if (result.success) {
                    getSharedPreferences("session", MODE_PRIVATE)
                        .edit()
                        .putString("usuario", usuario)
                        .apply()
                    goToMain()
                } else {
                    showError(result.message)
                }
            }
        }
    }

    private fun showError(msg: String) {
        binding.tvError.text       = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "Entrando..." else "Iniciar Sesión"
        binding.tvError.visibility = View.GONE
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}