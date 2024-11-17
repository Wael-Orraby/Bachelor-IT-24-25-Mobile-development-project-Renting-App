package com.ap.neighborrentapplication.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ap.neighborrentapplication.R
import com.ap.neighborrentapplication.data.repository.UserRepository
import com.ap.neighborrentapplication.databinding.ActivityRegisterBinding
import com.ap.neighborrentapplication.models.User
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID


class RegisterActivity : BaseActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var userRepository: UserRepository


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar("Account Aanmaken",true)


        // Initialiseer UserRepository
        userRepository = UserRepository()

        // registreer-knop
        binding.registerButton.setOnClickListener {
            registerUser()
        }

        // ga naar inlog pagina
        binding.backToLoginText.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun registerUser() {
        // Gegevens verzamelen
        val lastName = binding.lastNameEditText.text.toString().trim()
        val firstName = binding.firstNameEditText.text.toString().trim()
        val street = binding.streetEditText.text.toString().trim()
        val streetNumber = binding.streetNumberEditText.text.toString().trim()
        val city = binding.cityEditText.text.toString().trim()
        val postalCode = binding.postalCodeEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        // Validaties
        if (lastName.isEmpty() || firstName.isEmpty() || street.isEmpty() ||
            streetNumber.isEmpty() || city.isEmpty() || postalCode.isEmpty() ||
            email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Vul alle velden in", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Ongeldig e-mailadres", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Wachtwoord moet minstens 6 tekens bevatten", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Wachtwoorden komen niet overeen", Toast.LENGTH_SHORT).show()
            return
        }

        // Maak een User object
        val user = User(
            id = UUID.randomUUID().toString(),
            firstName = firstName,
            lastName = lastName,
            street = street,
            streetNumber = streetNumber,
            city = city,
            postalCode = postalCode,
            email = email
        )

        // Registratie en opslag in Firestore
        lifecycleScope.launch {
            try {
                // Gebruiker registreren bij Firebase Authentication
                userRepository.registerUser(email, password).await()

                // Gebruikersgegevens opslaan in Firestore
                userRepository.saveUserToFirestore(user).await()

                Toast.makeText(this@RegisterActivity, "Registratie geslaagd!", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                // Foutmelding als de registratie of opslag mislukt
                Toast.makeText(this@RegisterActivity, "Registratie mislukt: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}