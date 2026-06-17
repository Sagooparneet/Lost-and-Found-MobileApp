package com.example.loginandregistration.admin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.AdminUser
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class AdminUsersAdapter(
    private val onUserAction: (AdminUser, String) -> Unit
) : ListAdapter<AdminUser, AdminUsersAdapter.UserViewHolder>(UserDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)
        return UserViewHolder(view, onUserAction)
    }
    
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class UserViewHolder(
        itemView: View,
        private val onUserAction: (AdminUser, String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val ivUserAvatar: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvUserEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        private val tvUserStats: TextView = itemView.findViewById(R.id.tvUserStats)
        private val tvJoinDate: TextView = itemView.findViewById(R.id.tvJoinDate)
        private val chipRole: Chip = itemView.findViewById(R.id.chipRole)
        private val chipStatus: Chip = itemView.findViewById(R.id.chipStatus)
        private val btnBlockUser: MaterialButton = itemView.findViewById(R.id.btnBlockUser)
        private val btnChangeRole: MaterialButton = itemView.findViewById(R.id.btnChangeRole)
        private val btnViewDetails: MaterialButton = itemView.findViewById(R.id.btnViewDetails)
        
        fun bind(user: AdminUser) {
            tvUserName.text = user.displayName.ifEmpty { "Unknown User" }
            tvUserEmail.text = user.email
            
            // User stats
            tvUserStats.text = "Reported: ${user.itemsReported} • Found: ${user.itemsFound} • Claimed: ${user.itemsClaimed}"
            
            // Join date
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            tvJoinDate.text = "Joined: ${sdf.format(user.createdAt.toDate())}"
            
            // Role chip
            chipRole.text = user.role.name
            chipRole.setChipBackgroundColorResource(getRoleColor(user.role.name))
            
            // Status chip
            chipStatus.text = if (user.isBlocked) "Blocked" else "Active"
            chipStatus.setChipBackgroundColorResource(
                if (user.isBlocked) R.color.status_lost else R.color.status_found
            )
            
            // Block/Unblock button
            btnBlockUser.text = if (user.isBlocked) "Unblock" else "Block"
            btnBlockUser.setIconResource(
                if (user.isBlocked) R.drawable.ic_check else R.drawable.ic_block
            )
            
            // Load avatar - handle nullable photoUrl properly
            if (!user.photoUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(user.photoUrl)
                    .placeholder(R.drawable.ic_person_placeholder)
                    .error(R.drawable.ic_person_placeholder)
                    .circleCrop()
                    .into(ivUserAvatar)
            } else {
                ivUserAvatar.setImageResource(R.drawable.ic_person_placeholder)
            }
            
            // Set click listeners
            btnBlockUser.setOnClickListener {
                onUserAction(user, "block")
            }
            
            btnChangeRole.setOnClickListener {
                onUserAction(user, "change_role")
            }
            
            btnViewDetails.setOnClickListener {
                onUserAction(user, "view_details")
            }
            
            itemView.setOnClickListener {
                onUserAction(user, "view_details")
            }
        }
        
        private fun getRoleColor(role: String): Int {
            return when (role) {
                "ADMIN" -> R.color.status_lost
                "MODERATOR" -> R.color.status_pending
                "USER" -> R.color.status_default
                else -> R.color.status_default
            }
        }
    }
    
    class UserDiffCallback : DiffUtil.ItemCallback<AdminUser>() {
        override fun areItemsTheSame(oldItem: AdminUser, newItem: AdminUser): Boolean {
            return oldItem.uid == newItem.uid
        }
        
        override fun areContentsTheSame(oldItem: AdminUser, newItem: AdminUser): Boolean {
            return oldItem == newItem
        }
    }
}