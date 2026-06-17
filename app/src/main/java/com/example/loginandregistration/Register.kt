package com.example.loginandregistration

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth // Correct import
import com.google.firebase.auth.ktx.auth // KTX import for Firebase.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase // KTX import for Firebase.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "RegisterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth // Using KTX version

        val etRegName = findViewById<EditText>(R.id.etRegName)
        val etRegPhone = findViewById<EditText>(R.id.etRegPhone)
        val etRegEmail = findViewById<EditText>(R.id.etRegEmail)
        val etRegPassword = findViewById<EditText>(R.id.etRegPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        btnRegister.setOnClickListener {
            val name = etRegName.text.toString().trim()
            val phone = etRegPhone.text.toString().trim()
            val email = etRegEmail.text.toString().trim()
            val password = etRegPassword.text.toString().trim()

            // Validate all required fields
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phone.isEmpty()) {
                Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create user with Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "createUserWithEmail:success")
                        val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                        
                        // Create user document in Firestore
                        createUserDocument(userId, name, phone, email)
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish() // Finish RegisterActivity
        }
    }

    private fun createUserDocument(userId: String, name: String, phone: String, email: String) {
        val db = FirebaseFirestore.getInstance()
        val user = User(
            uid = userId,                    // Map to uid
            displayName = name,              // Map to displayName
            email = email,
            phone = phone,
            photoUrl = "",                   // Empty initially
            role = "STUDENT",                // Default role - Requirement 10.2
            isBlocked = false,               // Not blocked by default
            gender = "",
            fcmToken = "",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now(),
            lastLoginAt = Timestamp.now(),   // Set on registration
            itemsReported = 0,
            itemsFound = 0,
            itemsClaimed = 0
        )
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.collection("users").document(userId)
                    .set(user)
                    .await()
                
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "User document created successfully")
                    Toast.makeText(this@Register, "Registration successful!", Toast.LENGTH_SHORT).show()
                    navigateToDashboard()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error creating user document", e)
                    Toast.makeText(this@Register, "Error saving user data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToDashboard() { // Assuming MainActivity is DashboardActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Finish RegisterActivity
    }
}
