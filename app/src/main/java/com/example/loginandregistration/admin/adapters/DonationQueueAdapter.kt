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
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.DonationItem
import com.example.loginandregistration.admin.models.DonationStatus
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Adapter for displaying donation queue items
 * Requirements: 3.2
 * Task: 11.2
 */
class DonationQueueAdapter(
    private val onItemAction: (DonationItem, String) -> Unit
) : ListAdapter<DonationItem, DonationQueueAdapter.DonationViewHolder>(DonationDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_donation_queue, parent, false)
        return DonationViewHolder(view, onItemAction)
    }
    
    override fun onBindViewHolder(holder: DonationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class DonationViewHolder(
        itemView: View,
        private val onItemAction: (DonationItem, String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val ivItemImage: ImageView = itemView.findViewById(R.id.ivItemImage)
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvItemCategory: TextView = itemView.findViewById(R.id.tvItemCategory)
        private val tvItemLocation: TextView = itemView.findViewById(R.id.tvItemLocation)
        private val tvItemAge: TextView = itemView.findViewById(R.id.tvItemAge)
        private val tvEligibilityDate: TextView = itemView.findViewById(R.id.tvEligibilityDate)
        private val chipStatus: Chip = itemView.findViewById(R.id.chipStatus)
        private val chipAgeIndicator: Chip? = itemView.findViewById(R.id.chipAgeIndicator)
        private val tvDonatedDate: TextView? = itemView.findViewById(R.id.tvDonatedDate)
        private val btnViewDetails: MaterialButton = itemView.findViewById(R.id.btnViewDetails)
        private val btnMarkReady: MaterialButton = itemView.findViewById(R.id.btnMarkReady)
        private val btnMarkDonated: MaterialButton = itemView.findViewById(R.id.btnMarkDonated)
        
        fun bind(item: DonationItem) {
            // Set basic item information
            tvItemName.text = item.itemName
            tvItemCategory.text = item.category
            tvItemLocation.text = "📍 ${item.location}"
            
            // Calculate and display item age with visual indicator for 365+ days
            // Requirements: 3.3, 3.5
            val ageInDays = item.getAgeInDays()
            val ageText = when {
                ageInDays < 365 -> "$ageInDays days old"
                ageInDays < 730 -> {
                    val years = ageInDays / 365
                    val remainingDays = ageInDays % 365
                    "$ageInDays days old ($years year, $remainingDays days)"
                }
                else -> {
                    val years = ageInDays / 365
                    val remainingDays = ageInDays % 365
                    "$ageInDays days old ($years years, $remainingDays days)"
                }
            }
            tvItemAge.text = "Age: $ageText"
            
            // Show age badge chip with warning color for items 365+ days old
            // Requirements: 3.3, 3.5
            chipAgeIndicator?.let { chip ->
                if (ageInDays >= 365) {
                    chip.visibility = View.VISIBLE
                    chip.text = "365+ days old"
                    chip.setChipBackgroundColorResource(R.color.warning_orange)
                    chip.setTextColor(Color.WHITE)
                } else {
                    chip.visibility = View.GONE
                }
            }
            
            // Display eligibility date
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val eligibilityDate = Date(item.eligibleAt)
            tvEligibilityDate.text = "Eligible since: ${sdf.format(eligibilityDate)}"
            
            // Set status chip with proper styling
            chipStatus.text = getStatusDisplayName(item.status)
            val statusColor = getStatusColor(item.status)
            chipStatus.setChipBackgroundColorResource(statusColor)
            chipStatus.setTextColor(Color.WHITE)
            
            // Load image
            if (item.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(ivItemImage)
            } else {
                ivItemImage.setImageResource(R.drawable.ic_image_placeholder)
            }
            
            // Configure action buttons based on status
            // Requirements: 3.3, 3.5
            when (item.status) {
                DonationStatus.PENDING -> {
                    btnMarkReady.visibility = View.VISIBLE
                    btnMarkReady.text = "Mark for Donation"
                    btnMarkDonated.visibility = View.GONE
                    tvDonatedDate?.visibility = View.GONE
                }
                DonationStatus.READY -> {
                    btnMarkReady.visibility = View.GONE
                    btnMarkDonated.visibility = View.VISIBLE
                    btnMarkDonated.text = "Mark as Donated"
                    tvDonatedDate?.visibility = View.GONE
                }
                DonationStatus.DONATED -> {
                    btnMarkReady.visibility = View.GONE
                    btnMarkDonated.visibility = View.GONE
                    // Display donation date for donated items
                    // Requirements: 3.3, 3.5
                    tvDonatedDate?.let { tv ->
                        tv.visibility = View.VISIBLE
                        if (item.donatedAt > 0) {
                            val donatedDate = Date(item.donatedAt)
                            tv.text = "Donated on ${sdf.format(donatedDate)}"
                        } else {
                            tv.text = "Donated"
                        }
                    }
                }
            }
            
            // Set click listeners
            btnViewDetails.setOnClickListener {
                onItemAction(item, "view_details")
            }
            
            btnMarkReady.setOnClickListener {
                onItemAction(item, "mark_ready")
            }
            
            btnMarkDonated.setOnClickListener {
                onItemAction(item, "mark_donated")
            }
            
            itemView.setOnClickListener {
                onItemAction(item, "view_details")
            }
        }
        
        private fun getStatusDisplayName(status: DonationStatus): String {
            return when (status) {
                DonationStatus.PENDING -> "Pending Review"
                DonationStatus.READY -> "Ready for Donation"
                DonationStatus.DONATED -> "Donated"
            }
        }
        
        private fun getStatusColor(status: DonationStatus): Int {
            return when (status) {
                DonationStatus.PENDING -> R.color.status_donation_pending
                DonationStatus.READY -> R.color.status_donation_ready
                DonationStatus.DONATED -> R.color.status_donated
            }
        }
    }
    
    class DonationDiffCallback : DiffUtil.ItemCallback<DonationItem>() {
        override fun areItemsTheSame(oldItem: DonationItem, newItem: DonationItem): Boolean {
            return oldItem.itemId == newItem.itemId
        }
        
        override fun areContentsTheSame(oldItem: DonationItem, newItem: DonationItem): Boolean {
            return oldItem == newItem
        }
    }
}
