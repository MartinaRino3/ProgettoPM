package com.example.progettopm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var nameEditText: EditText
    private lateinit var surnameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        nameEditText = findViewById(R.id.nameEditText)
        surnameEditText = findViewById(R.id.surnameEditText)
        usernameEditText = findViewById(R.id.usernameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        registerButton = findViewById(R.id.registerButton)

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val surname = surnameEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (name.isEmpty() || surname.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                passwordEditText.error = "La password deve essere di almeno 6 caratteri"
                return@setOnClickListener
            }

            // Evito i doppi click
            registerButton.isEnabled = false
            registerButton.text = "Registrazione..."

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: return@addOnSuccessListener

                    val userMap = hashMapOf(
                        "email" to email,
                        "nome" to name,
                        "cognome" to surname,
                        "username" to username,
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    db.collection("users").document(userId).set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registrazione completata!", Toast.LENGTH_SHORT).show()
                            Log.d("Register", "Utente salvato con ID = $userId")

                            val intent = Intent(this, WelcomeActivity::class.java)
                            startActivity(intent)
                            finish()
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        .addOnFailureListener { e ->
                            // Se fallisce il salvataggio su DB
                            registerButton.isEnabled = true
                            registerButton.text = "Registrati"
                            Toast.makeText(this, "Errore salvataggio dati: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    // Se fallisce la creazione account
                    registerButton.isEnabled = true
                    registerButton.text = "Registrati"
                    Toast.makeText(this, "Errore: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}