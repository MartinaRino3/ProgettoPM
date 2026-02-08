package com.example.progettopm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var userUsernameTextView: TextView
    private lateinit var userSurnameTextView: TextView
    private lateinit var userCreatedTextView: TextView
    private lateinit var changeButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.account_fragment, container, false)

        userNameTextView = view.findViewById(R.id.textViewUserName)
        userEmailTextView = view.findViewById(R.id.textViewUserEmail)
        userUsernameTextView = view.findViewById(R.id.textViewUserUsername)
        userCreatedTextView = view.findViewById(R.id.textViewUserCreated)
        userSurnameTextView = view.findViewById(R.id.textViewUserSurname)
        changeButton = view.findViewById(R.id.changeDataButton)

        loadUserInfo()

        changeButton.setOnClickListener {
            findNavController().navigate(R.id.action_accountFragment_to_editDataFragment)
        }

        return view
    }

    private fun loadUserInfo() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val nome = document.getString("nome")
                    val cognome = document.getString("cognome")
                    val username = document.getString("username")
                    val email = document.getString("email")
                    val data = document.getTimestamp("createdAt")
                    val formattedDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(data?.toDate())
                    userNameTextView.text = "$nome"
                    userSurnameTextView.text = "$cognome"
                    userUsernameTextView.text = "$username"
                    userEmailTextView.text = "$email"
                    userCreatedTextView.text = "$formattedDate"
                }
        }
    }
}
