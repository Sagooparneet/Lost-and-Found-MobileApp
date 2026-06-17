package com.example.loginandregistration.admin.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.loginandregistration.Login
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.repository.AdminRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Admin Profile Fragment
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5
 */
class AdminProfileFragment : Fragment() {
    
    private lateinit var tvAdminEmail: TextView
    private lateinit var tvAdminName: TextView
    private lateinit var tvAdminRole: TextView
    private lateinit var tvLastLogin: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnRefreshData: Button
    
    private val auth = FirebaseAuth.getInstance()
    private val adminRepository = AdminRepository()
    
    companion object {
        private const val TAG = "AdminProfileFragment"
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupUserInfo()
        setupClickListeners()
    }
    
    private fun initViews(view: View) {
        tvAdminEmail = view.findViewById(R.id.tvAdminEmail)
        tvAdminName = view.findViewById(R.id.tvAdminName)
        tvAdminRole = view.findViewById(R.id.tvAdminRole)
        tvLastLogin = view.findViewById(R.id.tvLastLogin)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnRefreshData = view.findViewById(R.id.btnRefreshData)
    }
    
    /**
     * Setup user information display
     * Requirements: 8.2, 8.3
     */
    private fun setupUserInfo() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Display basic info from Firebase Auth
            tvAdminEmail.text = currentUser.email ?: "N/A"
            tvAdminName.text = currentUser.displayName ?: "Admin User"
            tvAdminRole.text = "Administrator"
            
            // Fetch additional info from Firestore including last login time
            fetchAdminUserDetails(currentUser.uid)
        } else {
            tvAdminEmail.text = "Not logged in"
            tvAdminName.text = "N/A"
            tvAdminRole.text = "N/A"
            tvLastLogin.text = "N/A"
        }
    }
    
    /**
     * Fetch admin user details from Firestore
     * Requirements: 8.2, 8.3
     */
    private fun fetchAdminUserDetails(userId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = adminRepository.getUserDetails(userId)
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val user = result.getOrNull()
                        if (user != null) {
                            // Update display name if available from Firestore
                            if (user.displayName.isNotBlank()) {
                                tvAdminName.text = user.displayName
                            }
                            
                            // Update role
                            tvAdminRole.text = user.role.name
                            
                            // Format and display last login time
                            if (user.lastLoginAt != null) {
                                val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                                val lastLoginDate = user.lastLoginAt.toDate()
                                tvLastLogin.text = dateFormat.format(lastLoginDate)
                            } else {
                                tvLastLogin.text = "Never"
                            }
                            
                            Log.d(TAG, "Admin user details loaded successfully")
                        } else {
                            tvLastLogin.text = "N/A"
                        }
                    } else {
                        // If user details not found in Firestore, show N/A
                        tvLastLogin.text = "N/A"
                        Log.w(TAG, "Failed to load admin user details: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching admin user details", e)
                withContext(Dispatchers.Main) {
                    tvLastLogin.text = "N/A"
                }
            }
        }
    }
    
    /**
     * Setup click listeners for buttons
     * Requirements: 8.4, 8.5
     */
    private fun setupClickListeners() {
        // Logout button - Requirements: 8.4, 8.5
        btnLogout.setOnClickListener {
            performLogout()
        }
        
        btnRefreshData.setOnClickListener {
            // Refresh dashboard data
            (activity as? com.example.loginandregistration.admin.AdminDashboardActivity)?.refreshData()
        }
    }
    
    /**
     * Perform logout operation
     * Requirements: 8.4, 8.5
     */
    private fun performLogout() {
        try {
            // Sign out from Firebase Authentication
            auth.signOut()
            
            Log.d(TAG, "User logged out successfully")
            
            // Navigate to Login activity with flags to clear back stack
            val intent = Intent(activity, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            
            // Finish the current activity to prevent back navigation
            activity?.finish()
            
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
            Toast.makeText(context, "Error logging out: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}