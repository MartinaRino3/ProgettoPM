package com.example.progettopm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.progettopm.notifications.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var credentialManager: CredentialManager

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var googleSignInButton: Button
    private lateinit var registerLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        credentialManager = CredentialManager.create(this)
        if (auth.currentUser != null) {
            vaiAllaWelcome()
            return
        }

        // Permessi notifiche
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        NotificationHelper.scheduleDailyNotification(this)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        registerLink = findViewById(R.id.registerLink)
        googleSignInButton = findViewById(R.id.googleSignInButton)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Inserisci email e password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    vaiAllaWelcome()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Credenziali errate o utente non trovato", Toast.LENGTH_SHORT).show()
                }
        }

        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        googleSignInButton.setOnClickListener {
            effettuaLoginGoogle()
        }

        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun vaiAllaWelcome() {
        val intent = Intent(this, WelcomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun effettuaLoginGoogle() {
        // Usa una Coroutine per operazioni asincrone
        lifecycleScope.launch {
            try {
                // A. Configura la richiesta per Google
                // IMPORTANTE: Sostituisci la stringa qui sotto con il TUO Web Client ID preso da google-services.json
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false) // false = mostra tutti gli account, non solo quelli già usati
                    .setServerClientId("142689315387-tmnhsk2mnonfn5cd0c06k7jec28qrh1v.apps.googleusercontent.com")// Firebase genera questa stringa in automatico
                    .setAutoSelectEnabled(true)
                    .build()

                // B. Crea la richiesta generica
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                // C. Mostra il popup di Google all'utente e aspetta il risultato
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity
                )

                // D. Gestisci il risultato
                gestisciRisultatoGoogle(result)

            } catch (e: GetCredentialException) {
                Log.e("GoogleLogin", "Errore Credential Manager: ${e.message}")
                Toast.makeText(this@LoginActivity, "Login annullato o fallito", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("GoogleLogin", "Errore generico: ${e.message}")
            }
        }
    }

    private fun gestisciRisultatoGoogle(result: GetCredentialResponse) {
        val credential = result.credential

        // Controlliamo se è un token di Google
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                // Estraiamo il Token
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                // Autentichiamo su Firebase con questo token
                firebaseAuthWithGoogle(idToken)

            } catch (e: GoogleIdTokenParsingException) {
                Log.e("GoogleLogin", "Token non valido", e)
            }
        } else {
            Log.e("GoogleLogin", "Tipo di credenziale non supportato")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                // Controlla se è la PRIMA volta che questo utente entra
                val isNewUser = authResult.additionalUserInfo?.isNewUser == true

                if (user != null) {
                    if (isNewUser) {
                        // 🔹 LOGICA INTELLIGENTE PER IL NOME
                        val fullName = user.displayName ?: "Utente Google"
                        val parts = fullName.split(" ") // Taglia dove c'è lo spazio

                        // Il primo pezzo è il nome (es. "Mario")
                        val nome = parts.firstOrNull() ?: ""

                        // Tutto il resto è il cognome (es. "Rossi" o "De Luca")
                        val cognome = if (parts.size > 1) {
                            parts.drop(1).joinToString(" ")
                        } else {
                            "" // Se ha solo un nome (es. "Cher"), cognome vuoto
                        }

                        val userMap = hashMapOf(
                            "email" to user.email,
                            "nome" to nome,
                            "cognome" to cognome,
                            "username" to (user.email?.substringBefore("@") ?: "user"),
                            "createdAt" to FieldValue.serverTimestamp()
                        )

                        // Salviamo su Firestore
                        db.collection("users").document(user.uid).set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Benvenuto $nome!", Toast.LENGTH_SHORT).show()
                                vaiAllaWelcome()
                            }
                            .addOnFailureListener {
                                // Se fallisce il DB, entra comunque (meglio che niente)
                                vaiAllaWelcome()
                            }
                    } else {
                        // Se l'utente esisteva già, NON sovrascrivere i dati (magari li ha cambiati lui)
                        vaiAllaWelcome()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errore Firebase: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}