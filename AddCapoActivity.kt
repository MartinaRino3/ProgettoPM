package com.example.progettopm

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import com.example.progettopm.notifications.NotificationHelper
import java.io.ByteArrayOutputStream
import java.io.InputStream


class AddCapoActivity : AppCompatActivity() {

    private lateinit var spinnerCategoria: Spinner
    private lateinit var spinnerTaglia: Spinner
    private lateinit var spinnerMarca: Spinner
    private lateinit var spinnerColore: Spinner
    private lateinit var spinnerMateriale: Spinner
    private lateinit var spinnerStagione: Spinner
    private lateinit var nomeCapo: EditText
    private lateinit var addCapoButton: Button
    private lateinit var selectImageButton: Button
    private lateinit var imagePreview: ImageView

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_capo)

        nomeCapo = findViewById(R.id.nomeCapo)
        spinnerCategoria = findViewById(R.id.categoria)
        spinnerTaglia = findViewById(R.id.taglia)
        spinnerMarca = findViewById(R.id.marca)
        spinnerColore = findViewById(R.id.colore)
        spinnerMateriale = findViewById(R.id.materiale)
        spinnerStagione = findViewById(R.id.stagione)
        addCapoButton = findViewById(R.id.buttonAggiungi)
        selectImageButton = findViewById(R.id.buttonSelectImage)
        imagePreview = findViewById(R.id.imagePreview)

        // Popola gli spinner
        val categoria = listOf("Maglietta", "Pantalone", "Camicia", "Maglione", "Felpa", "Jeans")
        val taglia = listOf("XS", "S", "M", "L", "XL")
        val marca = listOf("Nike", "Adidas", "Zara", "H&M", "Levi’s")
        val colore = listOf("Rosso", "Blu", "Nero", "Bianco", "Verde", "Giallo")
        val materiale = listOf("Cotone", "Lana", "Poliestere", "Jeans", "Seta")
        val stagione = listOf("Estate", "Inverno")

        spinnerCategoria.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoria)
        spinnerTaglia.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, taglia)
        spinnerMarca.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, marca)
        spinnerColore.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, colore)
        spinnerMateriale.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, materiale)
        spinnerStagione.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, stagione)

        // Selezione immagine
        val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                imageUri = result.data?.data
                imagePreview.setImageURI(imageUri)
            }
        }

        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImage.launch(intent)
        }

        // Salvataggio capo
        addCapoButton.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "Utente non loggato", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nome = nomeCapo.text.toString()
            if (nome.isEmpty()) {
                Toast.makeText(this, "Inserisci il nome del capo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (imageUri == null) {
                Toast.makeText(this, "Seleziona un'immagine", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Converti immagine in Base64
            val base64Image = uriToBase64(imageUri!!)
            if (base64Image == null) {
                Toast.makeText(this, "Errore immagine troppo grande o non valida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Salva in Firestore
            val capo = hashMapOf(
                "nome" to nome,
                "categoria" to spinnerCategoria.selectedItem.toString(),
                "taglia" to spinnerTaglia.selectedItem.toString(),
                "brand" to spinnerMarca.selectedItem.toString(),
                "colore" to spinnerColore.selectedItem.toString(),
                "materiale" to spinnerMateriale.selectedItem.toString(),
                "stagione" to spinnerStagione.selectedItem.toString(),
                "imageBase64" to base64Image
            )

            val db = FirebaseFirestore.getInstance()
            db.collection("users")
                .document(userId)
                .collection("guardaroba")
                .document()
                .set(capo)
                .addOnSuccessListener {
                    NotificationHelper.showInstantNotification(
                        this,
                        "Aggiunta capo", "Il tuo capo è stato aggiunto con successo")
                    onBackPressedDispatcher.onBackPressed()
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Errore nel salvataggio: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }


    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)

            // Ridimensiona a max 800px di larghezza
            val resizedBitmap = getResizedBitmap(originalBitmap, 800)

            val baos = ByteArrayOutputStream()
            // Compressione JPEG
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
            val bytes = baos.toByteArray()

            // Controlla se è ancora troppo grande per Firestore (> 1MB)
            if (bytes.size > 1024 * 1024) {
                return null // Troppo grande anche dopo resize
            }

            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }
}
