package com.ap.neighborrentapplication.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ap.neighborrentapplication.activity.DashboardActivity
import com.google.firebase.auth.FirebaseAuth
import com.ap.neighborrentapplication.databinding.ActivityLoginBinding
import com.ap.neighborrentapplication.ui.dashboard.DashboardViewModel
import com.ap.neighborrentapplication.ui.register.RegisterActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = "Aanmelden"
        // Initialiseer View Binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialiseer FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Login button
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            loginUser(email, password)
        }

        // Ga naar registratie pagina
        binding.registerText.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

    }

    private fun loginUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vul alle velden in.", Toast.LENGTH_SHORT).show()
            return
        }

        // Aanmelden met Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Welkom!", Toast.LENGTH_SHORT).show()
                    // Ga naar Dashboard Activity
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    val errorMessage = when (task.exception?.message) {
                        "The email address is badly formatted." -> "Ongeldig e-mailadres."
                        "The password is invalid or the user does not have a password." -> "Ongeldig wachtwoord."
                        "The supplied auth credential is incorrect, malformed or has expired." -> "De inloggegevens zijn onjuist."
                        else -> "Inloggen mislukt: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }
}
