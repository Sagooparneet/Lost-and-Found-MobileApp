package com.example.loginandregistration.admin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.AdminUser
import com.example.loginandregistration.admin.models.UserRole
import com.example.loginandregistration.admin.viewmodel.AdminDashboardViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment to display detailed user information
 * Requirements: 1.2, 8.1
 */
class UserDetailsFragment : Fragment() {
    
    private val viewModel: AdminDashboardViewModel by activityViewModels()
    
    // Views
    private lateinit var btnBack: android.widget.ImageButton
    private lateinit var ivUserAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var chipRole: Chip
    private lateinit var chipStatus: Chip
    private lateinit var tvItemsReported: TextView
    private lateinit var tvItemsFound: TextView
    private lateinit var tvItemsClaimed: TextView
    private lateinit var tvAccountCreated: TextView
    private lateinit var tvLastLogin: TextView
    private lateinit var tvUserId: TextView
    private lateinit var btnEditUser: MaterialButton
    private lateinit var btnChangeRole: MaterialButton
    private lateinit var btnBlockUnblock: MaterialButton
    private lateinit var btnDeleteUser: MaterialButton
    
    private var currentUser: AdminUser? = null
    
    companion object {
        private const val ARG_USER_ID = "user_id"
        
        fun newInstance(userId: String): UserDetailsFragment {
            val fragment = UserDetailsFragment()
            val args = Bundle()
            args.putString(ARG_USER_ID, userId)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_details, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupClickListeners()
        observeViewModel()
        
        // Load user details - support both manual arguments and navigation args
        val userId = arguments?.getString(ARG_USER_ID) 
            ?: arguments?.getString("user_id")
        userId?.let {
            loadUserDetails(it)
        }
    }
    
    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        ivUserAvatar = view.findViewById(R.id.ivUserAvatar)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        chipRole = view.findViewById(R.id.chipRole)
        chipStatus = view.findViewById(R.id.chipStatus)
        tvItemsReported = view.findViewById(R.id.tvItemsReported)
        tvItemsFound = view.findViewById(R.id.tvItemsFound)
        tvItemsClaimed = view.findViewById(R.id.tvItemsClaimed)
        tvAccountCreated = view.findViewById(R.id.tvAccountCreated)
        tvLastLogin = view.findViewById(R.id.tvLastLogin)
        tvUserId = view.findViewById(R.id.tvUserId)
        btnEditUser = view.findViewById(R.id.btnEditUser)
        btnChangeRole = view.findViewById(R.id.btnChangeRole)
        btnBlockUnblock = view.findViewById(R.id.btnBlockUnblock)
        btnDeleteUser = view.findViewById(R.id.btnDeleteUser)
    }
    
    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        btnEditUser.setOnClickListener {
            currentUser?.let { user ->
                showEditUserDialog(user)
            }
        }
        
        btnChangeRole.setOnClickListener {
            currentUser?.let { user ->
                showRoleChangeDialog(user)
            }
        }
        
        btnBlockUnblock.setOnClickListener {
            currentUser?.let { user ->
                if (user.isBlocked) {
                    showUnblockConfirmation(user)
                } else {
                    showBlockUserDialog(user)
                }
            }
        }
        
        btnDeleteUser.setOnClickListener {
            currentUser?.let { user ->
                showDeleteUserConfirmation(user)
            }
        }
    }
    
    private fun observeViewModel() {
        // Observe all users list for updates
        viewModel.allUsers.observe(viewLifecycleOwner) { users ->
            // Update current user if it's in the list
            currentUser?.let { current ->
                users.find { it.uid == current.uid }?.let { updated ->
                    currentUser = updated
                    displayUserDetails(updated)
                }
            }
        }
        
        // Observe userDetails for initial load from repository
        viewModel.userDetails.observe(viewLifecycleOwner) { enhancedUser ->
            // Convert EnhancedAdminUser to AdminUser for display
            val adminUser = AdminUser(
                uid = enhancedUser.uid,
                email = enhancedUser.email,
                displayName = enhancedUser.displayName,
                photoUrl = enhancedUser.photoUrl,
                role = enhancedUser.role,
                isBlocked = enhancedUser.isBlocked,
                createdAt = enhancedUser.createdAt,
                lastLoginAt = enhancedUser.lastLoginAt,
                itemsReported = enhancedUser.itemsReported,
                itemsFound = enhancedUser.itemsFound,
                itemsClaimed = enhancedUser.itemsClaimed
            )
            currentUser = adminUser
            displayUserDetails(adminUser)
        }
        
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                view?.let { v ->
                    Snackbar.make(v, message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        
        // Error display disabled - uncomment to show errors
        // viewModel.error.observe(viewLifecycleOwner) { error ->
        //     if (error.isNotEmpty()) {
        //         view?.let { v ->
        //             Snackbar.make(v, error, Snackbar.LENGTH_LONG).show()
        //         }
        //     }
        // }
    }
    
    private fun loadUserDetails(userId: String) {
        // First try to find user from cached list
        viewModel.allUsers.value?.find { it.uid == userId }?.let { user ->
            currentUser = user
            displayUserDetails(user)
            return
        }
        
        // If not found in cache, load from repository
        android.util.Log.d("UserDetailsFragment", "User not found in cache, loading from repository for userId: $userId")
        viewModel.loadUserDetails(userId)
    }
    
    private fun displayUserDetails(user: AdminUser) {
        // Basic info
        tvUserName.text = user.displayName.ifEmpty { "Unknown User" }
        tvUserEmail.text = user.email
        tvUserId.text = user.uid
        
        // Load avatar - handle nullable photoUrl properly
        if (!user.photoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(ivUserAvatar)
        } else {
            ivUserAvatar.setImageResource(R.drawable.ic_person_placeholder)
        }
        
        // Role chip
        chipRole.text = user.role.name
        chipRole.setChipBackgroundColorResource(getRoleColor(user.role))
        
        // Status chip
        chipStatus.text = if (user.isBlocked) "Blocked" else "Active"
        chipStatus.setChipBackgroundColorResource(
            if (user.isBlocked) R.color.status_lost else R.color.status_found
        )
        
        // Statistics
        tvItemsReported.text = user.itemsReported.toString()
        tvItemsFound.text = user.itemsFound.toString()
        tvItemsClaimed.text = user.itemsClaimed.toString()
        
        // Account information
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        tvAccountCreated.text = sdf.format(user.createdAt.toDate())
        
        if (user.lastLoginAt != null) {
            tvLastLogin.text = getTimeAgo(user.lastLoginAt.seconds * 1000)
        } else {
            tvLastLogin.text = "Never"
        }
        
        // Update block/unblock button
        btnBlockUnblock.text = if (user.isBlocked) "Unblock User" else "Block User"
        btnBlockUnblock.setIconResource(
            if (user.isBlocked) R.drawable.ic_check else R.drawable.ic_block
        )
    }
    
    private fun getRoleColor(role: UserRole): Int {
        return when (role) {
            UserRole.ADMIN -> R.color.status_lost
            UserRole.SECURITY -> R.color.status_pending
            UserRole.STUDENT -> R.color.status_default
        }
    }
    
    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            else -> "Just now"
        }
    }
    
    private fun showEditUserDialog(user: AdminUser) {
        val dialog = com.example.loginandregistration.admin.dialogs.EditUserDialog.newInstance(user)
        dialog.show(parentFragmentManager, "EditUserDialog")
    }
    
    private fun showRoleChangeDialog(user: AdminUser) {
        // Requirement 10.3: Show only three valid roles
        val roles = arrayOf("STUDENT", "SECURITY", "ADMIN")
        val roleDisplayNames = arrayOf("Student", "Security", "Admin")
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Change User Role")
            .setItems(roleDisplayNames) { _, which ->
                val newRole = when (which) {
                    0 -> UserRole.STUDENT
                    1 -> UserRole.SECURITY
                    2 -> UserRole.ADMIN
                    else -> UserRole.STUDENT
                }
                viewModel.updateUserRole(user.uid, newRole)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showBlockUserDialog(user: AdminUser) {
        val dialog = com.example.loginandregistration.admin.dialogs.BlockUserDialog.newInstance(user)
        dialog.show(parentFragmentManager, "BlockUserDialog")
    }
    
    private fun showUnblockConfirmation(user: AdminUser) {
        val userName = if (user.displayName.isNotEmpty()) user.displayName else user.email
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Unblock User")
            .setMessage("Are you sure you want to unblock $userName? They will be able to log in again.")
            .setPositiveButton("Unblock") { _, _ ->
                viewModel.unblockUser(user.uid)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showDeleteUserConfirmation(user: AdminUser) {
        val userName = if (user.displayName.isNotEmpty()) user.displayName else user.email
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete User")
            .setMessage("⚠️ WARNING: This action is PERMANENT and cannot be undone!\n\n" +
                    "Are you sure you want to delete $userName?\n\n" +
                    "This will:\n" +
                    "• Delete the user's Firestore profile\n" +
                    "• Remove all user data\n" +
                    "• Log this action in the activity log\n\n" +
                    "Note: The Firebase Authentication account must be deleted separately via Admin SDK or Cloud Function.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteUser(user.uid)
                // Navigate back to user list after deletion
                requireActivity().supportFragmentManager.popBackStack()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }
}
