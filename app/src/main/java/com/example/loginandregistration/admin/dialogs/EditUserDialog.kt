package com.example.loginandregistration.admin.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.AdminUser
import com.example.loginandregistration.admin.models.UserRole
import com.example.loginandregistration.admin.viewmodel.AdminDashboardViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Dialog for editing user details
 * Requirements: 6.3, 6.4, 6.5
 */
class EditUserDialog : DialogFragment() {
    
    private val viewModel: AdminDashboardViewModel by activityViewModels()
    
    private lateinit var tilDisplayName: TextInputLayout
    private lateinit var etDisplayName: TextInputEditText
    private lateinit var rgRole: RadioGroup
    private lateinit var rbStudent: RadioButton
    private lateinit var rbSecurity: RadioButton
    private lateinit var rbAdmin: RadioButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSave: MaterialButton
    
    private var currentUser: AdminUser? = null
    
    companion object {
        private const val ARG_USER = "user"
        
        fun newInstance(user: AdminUser): EditUserDialog {
            val dialog = EditUserDialog()
            val args = Bundle()
            args.putString(ARG_USER + "_uid", user.uid)
            args.putString(ARG_USER + "_email", user.email)
            args.putString(ARG_USER + "_displayName", user.displayName)
            args.putString(ARG_USER + "_photoUrl", user.photoUrl)
            args.putString(ARG_USER + "_role", user.role.name)
            args.putBoolean(ARG_USER + "_isBlocked", user.isBlocked)
            args.putInt(ARG_USER + "_itemsReported", user.itemsReported)
            args.putInt(ARG_USER + "_itemsFound", user.itemsFound)
            args.putInt(ARG_USER + "_itemsClaimed", user.itemsClaimed)
            dialog.arguments = args
            return dialog
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_user, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        loadUserData()
        setupClickListeners()
        observeViewModel()
    }
    
    private fun observeViewModel() {
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                android.widget.Toast.makeText(requireContext(), "Error: $it", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        // Make dialog full width
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    
    private fun initViews(view: View) {
        tilDisplayName = view.findViewById(R.id.tilDisplayName)
        etDisplayName = view.findViewById(R.id.etDisplayName)
        rgRole = view.findViewById(R.id.rgRole)
        rbStudent = view.findViewById(R.id.rbStudent)
        rbSecurity = view.findViewById(R.id.rbSecurity)
        rbAdmin = view.findViewById(R.id.rbAdmin)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnSave = view.findViewById(R.id.btnSave)
    }
    
    private fun loadUserData() {
        arguments?.let { args ->
            val uid = args.getString(ARG_USER + "_uid", "")
            val email = args.getString(ARG_USER + "_email", "")
            val displayName = args.getString(ARG_USER + "_displayName", "")
            val photoUrl = args.getString(ARG_USER + "_photoUrl", "")
            val roleString = args.getString(ARG_USER + "_role", "USER")
            val isBlocked = args.getBoolean(ARG_USER + "_isBlocked", false)
            val itemsReported = args.getInt(ARG_USER + "_itemsReported", 0)
            val itemsFound = args.getInt(ARG_USER + "_itemsFound", 0)
            val itemsClaimed = args.getInt(ARG_USER + "_itemsClaimed", 0)
            
            val role = UserRole.fromString(roleString)
            
            currentUser = AdminUser(
                uid = uid,
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                role = role,
                isBlocked = isBlocked,
                itemsReported = itemsReported,
                itemsFound = itemsFound,
                itemsClaimed = itemsClaimed
            )
            
            // Populate fields using post to avoid InputConnection issues
            etDisplayName.post {
                etDisplayName.setText(displayName)
                etDisplayName.setSelection(displayName.length)
            }
            
            // Select appropriate role radio button - Requirement 10.3
            rgRole.post {
                when (role) {
                    UserRole.STUDENT -> rbStudent.isChecked = true
                    UserRole.SECURITY -> rbSecurity.isChecked = true
                    UserRole.ADMIN -> rbAdmin.isChecked = true
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        btnCancel.setOnClickListener {
            dismiss()
        }
        
        btnSave.setOnClickListener {
            saveUserDetails()
        }
    }
    
    private fun saveUserDetails() {
        val user = currentUser ?: run {
            android.widget.Toast.makeText(requireContext(), "Error: User data not loaded", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get updated values
        val newDisplayName = etDisplayName.text.toString().trim()
        
        // Validate display name
        if (newDisplayName.isEmpty()) {
            tilDisplayName.error = "Display name cannot be empty"
            return
        }
        
        tilDisplayName.error = null
        
        // Get selected role - Requirement 10.3
        val newRole = when (rgRole.checkedRadioButtonId) {
            R.id.rbStudent -> UserRole.STUDENT
            R.id.rbSecurity -> UserRole.SECURITY
            R.id.rbAdmin -> UserRole.ADMIN
            else -> UserRole.STUDENT
        }
        
        // Check if anything changed
        if (newDisplayName == user.displayName && newRole == user.role) {
            android.widget.Toast.makeText(requireContext(), "No changes to save", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        // Prepare updates map
        val updates = mutableMapOf<String, Any>()
        
        if (newDisplayName != user.displayName) {
            updates["displayName"] = newDisplayName
        }
        
        if (newRole != user.role) {
            updates["role"] = newRole.name
        }
        
        android.util.Log.d("EditUserDialog", "Saving user ${user.uid} with updates: $updates")
        
        // Update user details via ViewModel
        viewModel.updateUserDetailsEnhanced(user.uid, updates)
        
        // Show progress
        android.widget.Toast.makeText(requireContext(), "Saving changes...", android.widget.Toast.LENGTH_SHORT).show()
        
        // Dismiss dialog
        dismiss()
    }
}
