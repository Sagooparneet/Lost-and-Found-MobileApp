package com.example.loginandregistration.security.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.loginandregistration.Login
import com.example.loginandregistration.R
import com.google.firebase.auth.FirebaseAuth

class SecurityProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_security_profile, container, false)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Find the UI elements from the layout
        val emailTextView: TextView = view.findViewById(R.id.tvSecurityEmail)
        val changePasswordButton: Button = view.findViewById(R.id.btnChangePassword)
        val logoutButton: Button = view.findViewById(R.id.btnLogout)

        // Set the user's email
        val currentUser = auth.currentUser
        if (currentUser != null) {
            emailTextView.text = "Logged in as: ${currentUser.email}"
        } else {
            // This is a fallback in case something goes wrong
            emailTextView.text = "Not logged in"
            logoutUser() // If no user, send back to login
        }

        // Set up button click listeners
        changePasswordButton.setOnClickListener {
            // TODO: Implement password change logic here
            Toast.makeText(context, "Change Password clicked", Toast.LENGTH_SHORT).show()
        }

        logoutButton.setOnClickListener {
            logoutUser()
        }

        return view
    }

    private fun logoutUser() {
        auth.signOut()
        // Redirect to the Login screen
        val intent = Intent(activity, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish() // Close the current activity
    }
}
