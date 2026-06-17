package com.example.loginandregistration.admin.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.AdminUser
import com.example.loginandregistration.admin.models.UserRole
import com.example.loginandregistration.admin.viewmodel.AdminDashboardViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar

/**
 * Dialog for changing user role
 * Requirements: 1.5
 */
class RoleChangeDialog : DialogFragment() {
    
    private val viewModel: AdminDashboardViewModel by activityViewModels()
    
    private lateinit var tvUserInfo: TextView
    private lateinit var chipCurrentRole: Chip
    private lateinit var rgRoles: RadioGroup
    private lateinit var rbStudent: RadioButton
    private lateinit var rbSecurity: RadioButton
    private lateinit var rbAdmin: RadioButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnChangeRole: MaterialButton
    
    private var currentRole: UserRole = UserRole.STUDENT
    
    companion object {
        private const val ARG_USER_UID = "user_uid"
        private const val ARG_USER_NAME = "user_name"
        private const val ARG_USER_EMAIL = "user_email"
        private const val ARG_USER_ROLE = "user_role"
        
        fun newInstance(user: AdminUser): RoleChangeDialog {
            val dialog = RoleChangeDialog()
            val args = Bundle()
            args.putString(ARG_USER_UID, user.uid)
            args.putString(ARG_USER_NAME, user.displayName)
            args.putString(ARG_USER_EMAIL, user.email)
            args.putString(ARG_USER_ROLE, user.role.name)
            dialog.arguments = args
            return dialog
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_role_change, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupUserInfo()
        setupClickListeners()
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
        tvUserInfo = view.findViewById(R.id.tvUserInfo)
        chipCurrentRole = view.findViewById(R.id.chipCurrentRole)
        rgRoles = view.findViewById(R.id.rgRoles)
        rbStudent = view.findViewById(R.id.rbStudent)
        rbSecurity = view.findViewById(R.id.rbSecurity)
        rbAdmin = view.findViewById(R.id.rbAdmin)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnChangeRole = view.findViewById(R.id.btnChangeRole)
    }
    
    private fun setupUserInfo() {
        val userName = arguments?.getString(ARG_USER_NAME) ?: "Unknown User"
        val userEmail = arguments?.getString(ARG_USER_EMAIL) ?: ""
        val roleString = arguments?.getString(ARG_USER_ROLE) ?: "USER"
        
        currentRole = try {
            UserRole.fromString(roleString)
        } catch (e: Exception) {
            UserRole.STUDENT
        }
        
        tvUserInfo.text = "Select a new role for $userName ($userEmail)"
        chipCurrentRole.text = currentRole.name
        chipCurrentRole.setChipBackgroundColorResource(getRoleColor(currentRole))
        
        // Pre-select current role - Requirement 10.3
        when (currentRole) {
            UserRole.STUDENT -> rbStudent.isChecked = true
            UserRole.SECURITY -> rbSecurity.isChecked = true
            UserRole.ADMIN -> rbAdmin.isChecked = true
        }
    }
    
    private fun setupClickListeners() {
        btnCancel.setOnClickListener {
            dismiss()
        }
        
        btnChangeRole.setOnClickListener {
            changeUserRole()
        }
    }
    
    private fun changeUserRole() {
        // Get selected role - Requirement 10.3
        val selectedRole = when (rgRoles.checkedRadioButtonId) {
            R.id.rbStudent -> UserRole.STUDENT
            R.id.rbSecurity -> UserRole.SECURITY
            R.id.rbAdmin -> UserRole.ADMIN
            else -> null
        }
        
        if (selectedRole == null) {
            view?.let { v ->
                Snackbar.make(v, "Please select a role", Snackbar.LENGTH_SHORT).show()
            }
            return
        }
        
        // Check if role changed
        if (selectedRole == currentRole) {
            view?.let { v ->
                Snackbar.make(v, "Role is already ${selectedRole.name}", Snackbar.LENGTH_SHORT).show()
            }
            dismiss()
            return
        }
        
        // Show confirmation dialog
        showConfirmationDialog(selectedRole)
    }
    
    private fun showConfirmationDialog(newRole: UserRole) {
        val userName = arguments?.getString(ARG_USER_NAME) ?: "this user"
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirm Role Change")
            .setMessage("Are you sure you want to change $userName's role from ${currentRole.name} to ${newRole.name}?")
            .setPositiveButton("Confirm") { _, _ ->
                performRoleChange(newRole)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performRoleChange(newRole: UserRole) {
        val userId = arguments?.getString(ARG_USER_UID) ?: return
        
        // Update role through ViewModel
        viewModel.updateUserRoleEnhanced(userId, newRole)
        
        // Show success message
        view?.let { v ->
            Snackbar.make(v, "User role updated to ${newRole.name}", Snackbar.LENGTH_SHORT).show()
        }
        
        // Dismiss dialog
        dismiss()
    }
    
    private fun getRoleColor(role: UserRole): Int {
        return when (role) {
            UserRole.ADMIN -> R.color.status_lost
            UserRole.SECURITY -> R.color.status_pending
            UserRole.STUDENT -> R.color.status_default
        }
    }
}
