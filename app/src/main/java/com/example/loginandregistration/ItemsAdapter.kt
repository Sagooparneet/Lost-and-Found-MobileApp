package com.example.loginandregistration

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.loginandregistration.utils.UserRoleManager
import java.text.SimpleDateFormat
import java.util.*

class ItemsAdapter(
    private val currentUserId: String = "",
    private val userEmail: String = "",
    private val onClaimClickListener: ((LostFoundItem) -> Unit)? = null,
    private val pendingClaimItemIds: Set<String> = emptySet(),
    private val onItemClickListener: ((LostFoundItem) -> Unit)? = null
) : ListAdapter<LostFoundItem, ItemsAdapter.ItemViewHolder>(ItemDiffCallback()) {

    // It's more efficient to create the SimpleDateFormat instance once
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lost_found, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val canViewSensitiveInfo = UserRoleManager.canViewSensitiveInfo(userEmail)
        holder.bind(
            getItem(position), 
            dateFormat, 
            currentUserId, 
            canViewSensitiveInfo,
            onClaimClickListener,
            pendingClaimItemIds,
            onItemClickListener
        )
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivItemIcon: ImageView = itemView.findViewById(R.id.iv_item_icon)
        private val tvItemName: TextView = itemView.findViewById(R.id.tv_item_name)
        private val tvItemLocation: TextView = itemView.findViewById(R.id.tv_item_location)
        private val tvItemStatus: TextView = itemView.findViewById(R.id.tv_item_status)
        private val tvItemDate: TextView = itemView.findViewById(R.id.tv_item_date)
        private val btnRequestItem: Button = itemView.findViewById(R.id.btn_request_item)

        // Pass the date formatter here to avoid creating it repeatedly in a loop
        fun bind(
            item: LostFoundItem, 
            dateFormat: SimpleDateFormat,
            currentUserId: String,
            canViewSensitiveInfo: Boolean,
            onClaimClickListener: ((LostFoundItem) -> Unit)?,
            pendingClaimItemIds: Set<String>,
            onItemClickListener: ((LostFoundItem) -> Unit)?
        ) {
            // Set click listener for the entire item view
            itemView.setOnClickListener {
                onItemClickListener?.invoke(item)
            }
            
            tvItemName.text = item.name
            
            // Show "Returned" status if item is returned, otherwise show Lost/Found
            val displayStatus = if (item.status.equals("Returned", ignoreCase = true)) {
                "Returned"
            } else {
                if (item.isLost) "Lost" else "Found"
            }
            tvItemStatus.text = displayStatus

            // Role-based visibility for sensitive fields
            // Requirements: 10.1, 10.2, 10.3, 10.4, 10.5
            if (canViewSensitiveInfo) {
                // Security/Admin users can see all fields
                tvItemLocation.visibility = View.VISIBLE
                tvItemDate.visibility = View.VISIBLE
                
                tvItemLocation.text = item.location
                
                // Safely handle the nullable timestamp
                item.timestamp?.let { timestamp ->
                    tvItemDate.text = dateFormat.format(timestamp.toDate())
                } ?: run {
                    tvItemDate.text = "Date not available"
                }
            } else {
                // Regular users cannot see location and date
                tvItemLocation.visibility = View.GONE
                tvItemDate.visibility = View.GONE
            }

            // Set status background color
            val context = itemView.context
            when {
                item.status.equals("Returned", ignoreCase = true) -> {
                    // Use purple color for returned items
                    tvItemStatus.setBackgroundColor(context.getColor(R.color.status_returned))
                }
                item.isLost -> {
                    tvItemStatus.setBackgroundColor(context.getColor(R.color.lost_tag))
                }
                else -> {
                    tvItemStatus.setBackgroundColor(context.getColor(R.color.found_tag))
                }
            }

            // Load image using Glide with optimized settings
            // Handle nullable imageUrl properly
            if (!item.imageUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_item_default)
                    .centerCrop()
                    .thumbnail(0.1f) // Load low-res thumbnail first
                    .override(200, 200) // Resize to fixed size for better performance
                    .into(ivItemIcon)
            } else {
                // Set default icon based on item type if no image URL
                ivItemIcon.setImageResource(
                    if (item.name.lowercase().contains("phone")) R.drawable.ic_phone
                    else R.drawable.ic_item_default
                )
            }

            // Handle claim button visibility and click
            // Requirements: 5.1, 5.2
            // Show button only for approved found items
            // Hide button if user already has pending claim for the item
            val shouldShowClaimButton = !item.isLost && 
                                       item.status == "Approved" && 
                                       currentUserId.isNotEmpty() &&
                                       item.userId != currentUserId &&
                                       !pendingClaimItemIds.contains(item.id)
            
            btnRequestItem.visibility = if (shouldShowClaimButton) View.VISIBLE else View.GONE
            
            if (shouldShowClaimButton) {
                btnRequestItem.setOnClickListener {
                    onClaimClickListener?.invoke(item)
                }
            }
        }
    }
    
    class ItemDiffCallback : DiffUtil.ItemCallback<LostFoundItem>() {
        override fun areItemsTheSame(oldItem: LostFoundItem, newItem: LostFoundItem): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: LostFoundItem, newItem: LostFoundItem): Boolean {
            return oldItem == newItem
        }
    }
}
