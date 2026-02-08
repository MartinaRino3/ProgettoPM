package com.example.progettopm

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Capo(
    @DocumentId
    var id: String = "",
    val userId: String = "",
    val nome: String = "",
    val categoria: String = "",
    val taglia: String = "",
    val colore: String = "",
    val brand: String = "",
    val materiale: String = "",
    val stagione: String = "",
    val aggiuntoAt: Timestamp = Timestamp.now(),
    val imageBase64: String? = null
)
