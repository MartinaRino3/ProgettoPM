package com.example.progettopm

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WelcomeActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.welcome_activity)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)


        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()


        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController


        NavigationUI.setupWithNavController(navigationView, navController)

        val bottomNavigationView = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView2)
        bottomNavigationView.setupWithNavController(navController)

        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val colors = intArrayOf(
            ContextCompat.getColor(this, R.color.white), // Attivo
            ContextCompat.getColor(this, R.color.black)  // Inattivo
        )
        bottomNavigationView.itemIconTintList = ColorStateList(states, colors)
        bottomNavigationView.itemTextColor = ColorStateList(states, colors)

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    AlertDialog.Builder(this)
                        .setTitle("Logout")
                        .setMessage("Vuoi davvero uscire dall'account?")
                        .setPositiveButton("Sì") { _, _ ->
                            auth.signOut()
                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        .setNegativeButton("Annulla", null)
                        .show()
                }
                R.id.nav_action_add -> {
                    startActivity(Intent(this, AddCapoActivity::class.java))
                }
                R.id.nav_settings -> {
                    navController.navigate(R.id.editDataFragment)
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        val currentUser = auth.currentUser
        val headerView = navigationView.getHeaderView(0)
        val userNameTextView = headerView?.findViewById<TextView>(R.id.textViewUserName)
        val userEmailTextView = headerView?.findViewById<TextView>(R.id.textViewUserEmail)

        if (currentUser != null) {
            currentUser.reload().addOnSuccessListener {
                val refreshedUser = auth.currentUser
                val userId = refreshedUser?.uid
                val newEmail = refreshedUser?.email

                userEmailTextView?.text = newEmail

                if (userId != null && newEmail != null) {
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { document ->
                            val username = document.getString("username") ?: "Utente"
                            userNameTextView?.text = "Ciao, $username"

                            val emailInDb = document.getString("email")
                            if (emailInDb != newEmail) {
                                db.collection("users").document(userId)
                                    .update("email", newEmail)
                            }
                        }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                startActivity(Intent(this, AddCapoActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}