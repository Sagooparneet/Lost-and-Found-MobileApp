package com.example.loginandregistration.admin.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.AdminUser
import com.example.loginandregistration.admin.viewmodel.AdminDashboardViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Dialog for blocking a user with reason input
 * Requirements: 6.8, 6.9, 6.11
 */
class BlockUserDialog : DialogFragment() {
    
    private val viewModel: AdminDashboardViewModel by activityViewModels()
    
    private lateinit var tvBlockMessage: TextView
    private lateinit var tilBlockReason: TextInputLayout
    private lateinit var etBlockReason: TextInputEditText
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnBlock: MaterialButton
    
    private var currentUser: AdminUser? = null
    
    companion object {
        private const val ARG_USER = "user"
        
        fun newInstance(user: AdminUser): BlockUserDialog {
            val dialog = BlockUserDialog()
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
        return inflater.inflate(R.layout.dialog_block_user, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        loadUserData()
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
        tvBlockMessage = view.findViewById(R.id.tvBlockMessage)
        tilBlockReason = view.findViewById(R.id.tilBlockReason)
        etBlockReason = view.findViewById(R.id.etBlockReason)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnBlock = view.findViewById(R.id.btnBlock)
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
            
            val role = com.example.loginandregistration.admin.models.UserRole.fromString(roleString)
            
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
            
            // Update message with user name
            val userName = if (displayName.isNotEmpty()) displayName else email
            tvBlockMessage.text = "Are you sure you want to block $userName? They will not be able to log in until unblocked."
        }
    }
    
    private fun setupClickListeners() {
        btnCancel.setOnClickListener {
            dismiss()
        }
        
        btnBlock.setOnClickListener {
            blockUser()
        }
    }
    
    private fun blockUser() {
        val user = currentUser ?: return
        
        // Get block reason
        val reason = etBlockReason.text.toString().trim()
        
        // Validate reason
        if (reason.isEmpty()) {
            tilBlockReason.error = "Please provide a reason for blocking this user"
            return
        }
        
        if (reason.length < 10) {
            tilBlockReason.error = "Reason must be at least 10 characters"
            return
        }
        
        tilBlockReason.error = null
        
        // Block user via ViewModel
        viewModel.blockUser(user.uid, reason)
        
        // Dismiss dialog
        dismiss()
    }
}
