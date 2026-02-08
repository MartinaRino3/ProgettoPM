package com.example.progettopm

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progettopm.notifications.NotificationHelper
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditDataFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.edit_data_fragment, container, false)

        val backArrowButton = view?.findViewById<ImageButton>(R.id.backArrowButton)
        backArrowButton?.setOnClickListener {
            findNavController().popBackStack() // 🔙 Torna all'AccountFragment
        }

        val logoutArrowButton = view?.findViewById<ImageButton>(R.id.logoutArrowButton)
        logoutArrowButton?.setOnClickListener {
            val auth = FirebaseAuth.getInstance()

            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Vuoi davvero uscire dall'account?")
                .setPositiveButton("Sì") { _, _ ->
                    auth.signOut()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Annulla", null)
                .show()
        }

        val nameEditText = view.findViewById<EditText>(R.id.editTextName)
        val surnameEditText = view.findViewById<EditText>(R.id.editTextSurname)
        val usernameEditText = view.findViewById<EditText>(R.id.editTextUsername)
        val saveButton = view.findViewById<Button>(R.id.saveButton)
        val changeEmailButton = view.findViewById<Button>(R.id.changeEmailButton)
        val changePasswordButton = view.findViewById<Button>(R.id.changePasswordButton)

        val user = auth.currentUser ?: return view
        val userId = user.uid

        // 🔹 Carica dati utente da Firestore
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                nameEditText.setText(document.getString("nome"))
                surnameEditText.setText(document.getString("cognome"))
                usernameEditText.setText(document.getString("username"))
            }

        // 🔹 Salva modifiche profilo e torna indietro
        saveButton.setOnClickListener {
            NotificationHelper.showInstantNotification(
                requireContext(),
                "Modifica Dati", "La tua modifica ai dati è stata salvata"
            )

            val nome = nameEditText.text.toString().trim()
            val cognome = surnameEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()

            if (nome.isEmpty() || cognome.isEmpty() || username.isEmpty()) {
                Toast.makeText(requireContext(), "Compila tutti i campi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = mapOf(
                "nome" to nome,
                "cognome" to cognome,
                "username" to username
            )

            db.collection("users").document(userId).update(updates)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Dati aggiornati!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Errore durante l’aggiornamento", Toast.LENGTH_SHORT).show()
                }
        }

        changeEmailButton.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_email, null)
            val newEmailEditText = dialogView.findViewById<EditText>(R.id.newEmailEditText)
            val passwordEditText = dialogView.findViewById<EditText>(R.id.passwordEditText)

            AlertDialog.Builder(requireContext())
                .setTitle("Cambia email")
                .setView(dialogView)
                .setPositiveButton("Conferma") { _, _ ->
                    val newEmail = newEmailEditText.text.toString().trim()
                    val password = passwordEditText.text.toString().trim()

                    if (newEmail.isEmpty() || password.isEmpty()) {
                        Toast.makeText(requireContext(), "Compila tutti i campi", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val user = auth.currentUser ?: return@setPositiveButton
                    // Crea credenziali per ri-autenticarsi
                    val credential = EmailAuthProvider.getCredential(user.email!!, password)

                    // 1. Reautenticazione (Sicurezza)
                    user.reauthenticate(credential)
                        .addOnSuccessListener {
                            // 2. Avvia processo di verifica nuova email
                            // Firebase controllerà automaticamente se l'email è già usata qui
                            user.verifyBeforeUpdateEmail(newEmail)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "Ti abbiamo inviato una mail di verifica sulla NUOVA casella. Clicca sul link per confermare.",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    // Logout forzato per sicurezza
                                    auth.signOut()
                                    val intent = Intent(requireContext(), LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                }
                                .addOnFailureListener { e ->
                                    // Qui catturi l'errore se l'email è già in uso o non valida
                                    Toast.makeText(requireContext(), "Errore: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Password errata. Riprova.", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Annulla", null)
                .show()
        }

        // 🔹 Cambia password (invia email di reset)
        changePasswordButton.setOnClickListener {
            val email = user.email
            if (email != null) {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Email di reset inviata a $email", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Errore durante il reset password", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // 🔹 Elimina account
        val deleteAccountButton = view.findViewById<Button>(R.id.deleteAccountButton)
        deleteAccountButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Elimina account")
                .setMessage("Sei sicuro? Perderai tutti i tuoi vestiti salvati.")
                .setPositiveButton("Elimina") { _, _ ->
                    val user = auth.currentUser
                    if (user != null) {
                        val userId = user.uid

                        // 1. Cancella prima tutti i vestiti (Sottocollezione)
                        db.collection("users").document(userId).collection("guardaroba")
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val batch = db.batch() // Batch per efficienza
                                for (doc in snapshot.documents) {
                                    batch.delete(doc.reference)
                                }

                                // Esegui cancellazione vestiti
                                batch.commit().addOnCompleteListener {
                                    // 2. Cancella documento utente
                                    db.collection("users").document(userId).delete()
                                        .addOnSuccessListener {
                                            // 3. Cancella account Auth
                                            user.delete().addOnSuccessListener {
                                                Toast.makeText(requireContext(), "Addio! Account eliminato.", Toast.LENGTH_SHORT).show()
                                                val intent = Intent(requireContext(), LoginActivity::class.java)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                startActivity(intent)
                                            }
                                        }
                                }
                            }
                    }
                }
                .setNegativeButton("Annulla", null)
                .show()
        }

        return view
    }
}
