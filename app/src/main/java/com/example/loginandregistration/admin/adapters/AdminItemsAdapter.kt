package com.example.loginandregistration.admin.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.loginandregistration.LostFoundItem
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.EnhancedLostFoundItem
import com.example.loginandregistration.admin.models.ItemStatus
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class AdminItemsAdapter(
    private val onItemAction: (LostFoundItem, String) -> Unit
) : ListAdapter<EnhancedLostFoundItem, AdminItemsAdapter.ItemViewHolder>(ItemDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_item, parent, false)
        return ItemViewHolder(view, onItemAction)
    }
    
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ItemViewHolder(
        itemView: View,
        private val onItemAction: (LostFoundItem, String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val ivItemImage: ImageView = itemView.findViewById(R.id.ivItemImage)
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvItemDescription: TextView = itemView.findViewById(R.id.tvItemDescription)
        private val tvItemLocation: TextView = itemView.findViewById(R.id.tvItemLocation)
        private val tvItemDate: TextView = itemView.findViewById(R.id.tvItemDate)
        private val tvReporterName: TextView = itemView.findViewById(R.id.tvReporterName)
        private val chipStatus: Chip = itemView.findViewById(R.id.chipStatus)
        private val chipCategory: Chip? = itemView.findViewById(R.id.chipCategory)
        private val btnViewDetails: MaterialButton = itemView.findViewById(R.id.btnViewDetails)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)
        
        fun bind(item: EnhancedLostFoundItem) {
            tvItemName.text = item.name
            tvItemDescription.text = item.description
            tvItemLocation.text = "📍 ${item.location}"
            tvReporterName.text = "Reported by: ${item.userEmail}"
            
            // Format date
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            tvItemDate.text = sdf.format(item.timestamp.toDate())
            
            // Set status chip with proper styling
            chipStatus.text = getStatusDisplayName(item.status)
            val statusColor = getStatusColor(item.status)
            chipStatus.setChipBackgroundColorResource(statusColor)
            chipStatus.setTextColor(Color.WHITE)
            
            // Set category chip if available
            chipCategory?.let { chip ->
                if (item.category.isNotEmpty()) {
                    chip.text = item.category
                    chip.visibility = View.VISIBLE
                } else {
                    chip.visibility = View.GONE
                }
            }
            
            // Load image - handle nullable imageUrl properly
            if (!item.imageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(ivItemImage)
            } else {
                ivItemImage.setImageResource(R.drawable.ic_image_placeholder)
            }
            
            // Convert to LostFoundItem for compatibility
            val basicItem = LostFoundItem(
                id = item.id,
                name = item.name,
                description = item.description,
                location = item.location,
                contactInfo = item.contactInfo,
                isLost = item.isLost,
                userId = item.userId,
                userEmail = item.userEmail,
                imageUrl = item.imageUrl,
                timestamp = item.timestamp
            )
            
            // Set click listeners
            btnViewDetails.setOnClickListener {
                onItemAction(basicItem, "view")
            }
            
            btnEdit.setOnClickListener {
                onItemAction(basicItem, "edit")
            }
            
            btnDelete.setOnClickListener {
                onItemAction(basicItem, "delete")
            }
            
            itemView.setOnClickListener {
                onItemAction(basicItem, "view")
            }
        }
        
        private fun getStatusDisplayName(status: ItemStatus): String {
            return when (status) {
                ItemStatus.ACTIVE -> "Active"
                ItemStatus.REQUESTED -> "Requested"
                ItemStatus.RETURNED -> "Returned"
                ItemStatus.DONATION_PENDING -> "Donation Pending"
                ItemStatus.DONATION_READY -> "Donation Ready"
                ItemStatus.DONATED -> "Donated"
            }
        }
        
        private fun getStatusColor(status: ItemStatus): Int {
            return when (status) {
                ItemStatus.ACTIVE -> R.color.status_active
                ItemStatus.REQUESTED -> R.color.status_requested
                ItemStatus.RETURNED -> R.color.status_returned
                ItemStatus.DONATION_PENDING -> R.color.status_donation_pending
                ItemStatus.DONATION_READY -> R.color.status_donation_ready
                ItemStatus.DONATED -> R.color.status_donated
            }
        }
    }
    
    class ItemDiffCallback : DiffUtil.ItemCallback<EnhancedLostFoundItem>() {
        override fun areItemsTheSame(oldItem: EnhancedLostFoundItem, newItem: EnhancedLostFoundItem): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: EnhancedLostFoundItem, newItem: EnhancedLostFoundItem): Boolean {
            return oldItem == newItem
        }
    }
}