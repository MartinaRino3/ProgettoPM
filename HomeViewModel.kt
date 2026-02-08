package com.example.progettopm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _username = MutableLiveData<String>()
    val username: LiveData<String> get() = _username

    private val _totaleCapi = MutableLiveData<Int>()
    val totaleCapi: LiveData<Int> get() = _totaleCapi

    private val _capiByStagione = MutableLiveData<String>()
    val capiByStagione: LiveData<String> get() = _capiByStagione

    private val _capiRandom = MutableLiveData<List<Capo>>()
    val capiRandom: LiveData<List<Capo>> get() = _capiRandom

    init {
        // Appena il ViewModel nasce, iniziamo ad ascoltare le modifiche all'utente
        ascoltaUtente()
    }

    // 🔹 NUOVO METODO: Ascolta i cambiamenti del nome in tempo reale
    private fun ascoltaUtente() {
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid
            // Usiamo addSnapshotListener invece di get()
            db.collection("users").document(uid)
                .addSnapshotListener { document, error ->
                    if (error != null) {
                        Log.e("HomeViewModel", "Errore ascolto utente", error)
                        return@addSnapshotListener
                    }

                    if (document != null && document.exists()) {
                        val nome = document.getString("username")
                        // Aggiorna la variabile LiveData automaticamente
                        _username.value = nome ?: user.email?.substringBefore("@")
                    }
                }
        }
    }

    fun caricaDashboard() {
        val uid = auth.currentUser?.uid ?: return

        // Anche qui usiamo addSnapshotListener (già corretto prima)
        db.collection("users").document(uid).collection("guardaroba")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _totaleCapi.value = 0
                    _capiByStagione.value = "Errore nel caricamento"
                    return@addSnapshotListener
                }

                val capi = snapshot?.toObjects(Capo::class.java) ?: listOf()
                _totaleCapi.value = capi.size

                val perStagione = capi.groupingBy { it.stagione }.eachCount()
                _capiByStagione.value =
                    perStagione.entries.joinToString("  ") { "${it.key}: ${it.value}" }
            }
    }

    fun caricaCapiRandom() {
        val uid = auth.currentUser?.uid ?: return
        val stagioneCorrente = determinaStagioneCorrente()

        db.collection("users").document(uid).collection("guardaroba")
            .whereEqualTo("stagione", stagioneCorrente)
            .get()
            .addOnSuccessListener { snapshot ->
                // Qui mappiamo manualmente per avere l'ID corretto (FIX fatto in precedenza)
                val lista = snapshot.documents.mapNotNull { doc ->
                    val capo = doc.toObject(Capo::class.java)
                    capo?.id = doc.id
                    capo
                }
                _capiRandom.value = lista.shuffled().take(3)
            }
            .addOnFailureListener {
                Log.e("HomeViewModel", "Errore caricamento capi random: ${it.message}")
            }
    }

    private fun determinaStagioneCorrente(): String {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        return when (month) {
            in 4..9 -> "Estate"
            else -> "Inverno"
        }
    }
}