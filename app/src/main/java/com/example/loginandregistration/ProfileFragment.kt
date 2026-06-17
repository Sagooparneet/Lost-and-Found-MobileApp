package com.example.loginandregistration

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.loginandregistration.databinding.FragmentProfileBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var scrollView: android.widget.ScrollView
    
    private var currentUser: User? = null
    private val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root

        // Initialize Firebase Auth and Firestore
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        // Initialize loading views
        progressBar = view.findViewById(R.id.progress_bar)
        scrollView = view.findViewById(R.id.scroll_view)

        // Set up gender dropdown
        setupGenderDropdown()

        // Load user profile data
        loadUserProfile()

        // Set up button click listeners
        binding.btnSaveProfile.setOnClickListener {
            handleSaveProfile()
        }

        binding.btnChangePassword.setOnClickListener {
            handleChangePassword()
        }

        binding.btnLogout.setOnClickListener {
            handleLogout()
        }

        return view
    }

    private fun setupGenderDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genderOptions)
        binding.actvGender.setAdapter(adapter)
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading()

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                hideLoading()
                if (document.exists()) {
                    currentUser = document.toObject(User::class.java)
                    populateUserData(currentUser)
                } else {
                    // User document doesn't exist, create one with basic info
                    createUserDocument(user.uid, user.email ?: "")
                }
            }
            .addOnFailureListener { e ->
                hideLoading()
                Log.e("ProfileFragment", "Error loading user profile", e)
                Toast.makeText(context, "Failed to load profile: ${e.message}", Toast.LENGTH_LONG).show()
                // Still populate email from auth
                binding.etEmail.setText(user.email ?: "")
            }
    }

    private fun createUserDocument(userId: String, email: String) {
        val newUser = User(
            uid = userId,
            email = email,
            displayName = "",
            phone = "",
            gender = "",
            fcmToken = "",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        db.collection("users").document(userId)
            .set(newUser)
            .addOnSuccessListener {
                currentUser = newUser
                populateUserData(newUser)
                Log.d("ProfileFragment", "User document created")
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error creating user document", e)
                // Still populate email
                binding.etEmail.setText(email)
            }
    }

    private fun populateUserData(user: User?) {
        if (user != null) {
            binding.etName.setText(user.displayName)
            binding.etEmail.setText(user.email)
            binding.etPhone.setText(user.phone)
            
            // Set gender if it exists
            if (user.gender.isNotEmpty() && genderOptions.contains(user.gender)) {
                binding.actvGender.setText(user.gender, false)
            }
        }
    }
    private fun handleSaveProfile() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val gender = binding.actvGender.text.toString().trim()

        // Validate required fields
        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            return
        } else {
            binding.tilName.error = null
        }

        if (phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required"
            return
        } else {
            binding.tilPhone.error = null
        }

        // Validate phone number format (basic validation)
        if (phone.length < 10) {
            binding.tilPhone.error = "Please enter a valid phone number"
            return
        } else {
            binding.tilPhone.error = null
        }

        showLoading()

        // Update user document in Firestore
        val updates = hashMapOf<String, Any>(
            "displayName" to name,
            "phone" to phone,
            "gender" to gender,
            "updatedAt" to Timestamp.now()
        )

        db.collection("users").document(user.uid)
            .update(updates)
            .addOnSuccessListener {
                hideLoading()
                // Update current user object
                currentUser = currentUser?.copy(
                    displayName = name,
                    phone = phone,
                    gender = gender,
                    updatedAt = Timestamp.now()
                )
                Log.d("ProfileFragment", "Profile updated successfully")
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                hideLoading()
                Log.e("ProfileFragment", "Error updating profile", e)
                Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun handleChangePassword() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val newPassword = binding.etNewPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // --- Validations ---
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(context, "Please fill in both password fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 6) {
            Toast.makeText(context, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading indicator
        showLoading()

        // --- Update Password in Firebase ---
        updatePasswordWithRetry(newPassword)
    }

    private fun updatePasswordWithRetry(newPassword: String) {
        val user = auth.currentUser ?: return

        user.updatePassword(newPassword).addOnCompleteListener { task ->
            hideLoading()
            
            if (task.isSuccessful) {
                Log.d("ProfileFragment", "User password updated.")
                Toast.makeText(context, "Password updated successfully.", Toast.LENGTH_SHORT).show()
                // Clear the fields after success
                binding.etNewPassword.text?.clear()
                binding.etConfirmPassword.text?.clear()
            } else {
                // Handle specific error cases
                val exception = task.exception
                when (exception) {
                    is FirebaseAuthRecentLoginRequiredException -> {
                        // User needs to re-authenticate
                        Log.w("ProfileFragment", "Recent login required for password update")
                        showReauthenticationDialog(newPassword)
                    }
                    is FirebaseAuthWeakPasswordException -> {
                        Log.e("ProfileFragment", "Weak password", exception)
                        Toast.makeText(
                            context,
                            "Password is too weak. Please use a stronger password with a mix of letters, numbers, and symbols.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is FirebaseAuthInvalidCredentialsException -> {
                        Log.e("ProfileFragment", "Invalid credentials", exception)
                        Toast.makeText(
                            context,
                            "Invalid password format. Please try a different password.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {
                        Log.e("ProfileFragment", "Password update failed", exception)
                        Toast.makeText(
                            context,
                            "Failed to update password: ${exception?.message ?: "Unknown error"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun showReauthenticationDialog(newPassword: String) {
        val user = auth.currentUser ?: return
        val email = user.email

        if (email.isNullOrEmpty()) {
            Toast.makeText(
                context,
                "Unable to re-authenticate. Please log out and log in again.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Create dialog with password input
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            android.R.layout.simple_list_item_1, null
        )
        
        val passwordInput = EditText(requireContext()).apply {
            hint = "Enter your current password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(50, 40, 50, 40)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Re-authentication Required")
            .setMessage("For security reasons, please enter your current password to continue.")
            .setView(passwordInput)
            .setPositiveButton("Confirm") { dialog, _ ->
                val currentPassword = passwordInput.text.toString().trim()
                if (currentPassword.isEmpty()) {
                    Toast.makeText(context, "Please enter your current password", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@setPositiveButton
                }
                
                // Re-authenticate and retry password update
                reauthenticateAndUpdatePassword(email, currentPassword, newPassword)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun reauthenticateAndUpdatePassword(email: String, currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: return

        showLoading()

        // Create credential with current password
        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        // Re-authenticate user
        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                Log.d("ProfileFragment", "Re-authentication successful")
                // Now retry password update
                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                    hideLoading()
                    
                    if (updateTask.isSuccessful) {
                        Log.d("ProfileFragment", "Password updated after re-authentication")
                        Toast.makeText(
                            context,
                            "Password updated successfully.",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Clear the fields after success
                        binding.etNewPassword.text?.clear()
                        binding.etConfirmPassword.text?.clear()
                    } else {
                        Log.e("ProfileFragment", "Password update failed after re-auth", updateTask.exception)
                        val exception = updateTask.exception
                        val errorMessage = when (exception) {
                            is FirebaseAuthWeakPasswordException -> 
                                "Password is too weak. Please use a stronger password."
                            else -> 
                                "Failed to update password: ${exception?.message ?: "Unknown error"}"
                        }
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                hideLoading()
                Log.e("ProfileFragment", "Re-authentication failed", reauthTask.exception)
                val exception = reauthTask.exception
                val errorMessage = when (exception) {
                    is FirebaseAuthInvalidCredentialsException -> 
                        "Incorrect current password. Please try again."
                    else -> 
                        "Re-authentication failed: ${exception?.message ?: "Unknown error"}"
                }
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleLogout() {
        auth.signOut()
        // Navigate back to the Login activity
        val intent = Intent(activity, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        scrollView.visibility = View.GONE
        binding.btnSaveProfile.isEnabled = false
        binding.btnChangePassword.isEnabled = false
        binding.btnLogout.isEnabled = false
    }
    
    private fun hideLoading() {
        progressBar.visibility = View.GONE
        scrollView.visibility = View.VISIBLE
        binding.btnSaveProfile.isEnabled = true
        binding.btnChangePassword.isEnabled = true
        binding.btnLogout.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}


