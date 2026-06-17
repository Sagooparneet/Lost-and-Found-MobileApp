package com.example.loginandregistration.admin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.ItemStatus
import com.example.loginandregistration.admin.models.StatusChange
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying status change history
 * Requirements: 2.3
 */
class StatusHistoryAdapter : ListAdapter<StatusChange, StatusHistoryAdapter.StatusHistoryViewHolder>(StatusHistoryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_status_history, parent, false)
        return StatusHistoryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: StatusHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class StatusHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStatusChange: TextView = itemView.findViewById(R.id.tvStatusChange)
        private val tvChangedBy: TextView = itemView.findViewById(R.id.tvChangedBy)
        private val tvChangedDate: TextView = itemView.findViewById(R.id.tvChangedDate)
        private val tvReason: TextView = itemView.findViewById(R.id.tvReason)
        
        fun bind(statusChange: StatusChange) {
            // Status change text
            tvStatusChange.text = "${getStatusDisplayName(statusChange.previousStatus)} → ${getStatusDisplayName(statusChange.newStatus)}"
            
            // Changed by
            tvChangedBy.text = "Changed by: ${statusChange.changedBy}"
            
            // Changed date
            val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            tvChangedDate.text = sdf.format(Date(statusChange.changedAt))
            
            // Reason (if provided)
            if (statusChange.reason.isNotEmpty()) {
                tvReason.text = "Reason: ${statusChange.reason}"
                tvReason.visibility = View.VISIBLE
            } else {
                tvReason.visibility = View.GONE
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
    }
    
    class StatusHistoryDiffCallback : DiffUtil.ItemCallback<StatusChange>() {
        override fun areItemsTheSame(oldItem: StatusChange, newItem: StatusChange): Boolean {
            return oldItem.changedAt == newItem.changedAt
        }
        
        override fun areContentsTheSame(oldItem: StatusChange, newItem: StatusChange): Boolean {
            return oldItem == newItem
        }
    }
}
