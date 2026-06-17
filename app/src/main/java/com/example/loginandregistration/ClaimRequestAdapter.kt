package com.example.loginandregistration

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying claim requests
 * Requirements: 6.5
 */
class ClaimRequestAdapter(
    private val onApproveClicked: ((ClaimRequest) -> Unit)? = null,
    private val onRejectClicked: ((ClaimRequest) -> Unit)? = null
) : ListAdapter<ClaimRequest, ClaimRequestAdapter.ClaimRequestViewHolder>(ClaimRequestDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClaimRequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_claim_request, parent, false)
        return ClaimRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClaimRequestViewHolder, position: Int) {
        holder.bind(getItem(position), dateFormat, onApproveClicked, onRejectClicked)
    }

    class ClaimRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemName: TextView = itemView.findViewById(R.id.tv_item_name)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val tvRequestDate: TextView = itemView.findViewById(R.id.tv_request_date)
        private val tvReason: TextView = itemView.findViewById(R.id.tv_reason)
        private val tvReviewNotesLabel: TextView = itemView.findViewById(R.id.tv_review_notes_label)
        private val tvReviewNotes: TextView = itemView.findViewById(R.id.tv_review_notes)

        fun bind(
            claimRequest: ClaimRequest, 
            dateFormat: SimpleDateFormat,
            onApproveClicked: ((ClaimRequest) -> Unit)?,
            onRejectClicked: ((ClaimRequest) -> Unit)?
        ) {
            val context = itemView.context
            
            // Set item name
            tvItemName.text = claimRequest.itemName
            
            // Set status with appropriate color
            tvStatus.text = claimRequest.status
            when (claimRequest.status) {
                ClaimRequest.ClaimStatus.PENDING -> {
                    tvStatus.setBackgroundColor(context.getColor(android.R.color.holo_orange_dark))
                }
                ClaimRequest.ClaimStatus.APPROVED -> {
                    tvStatus.setBackgroundColor(context.getColor(android.R.color.holo_green_dark))
                }
                ClaimRequest.ClaimStatus.REJECTED -> {
                    tvStatus.setBackgroundColor(context.getColor(android.R.color.holo_red_dark))
                }
            }
            
            // Set request date
            val dateStr = dateFormat.format(claimRequest.requestDate.toDate())
            tvRequestDate.text = "Requested on: $dateStr"
            
            // Set reason
            tvReason.text = claimRequest.reason
            
            // Show review notes only if rejected and notes exist
            if (claimRequest.status == ClaimRequest.ClaimStatus.REJECTED && 
                claimRequest.reviewNotes.isNotEmpty()) {
                tvReviewNotesLabel.visibility = View.VISIBLE
                tvReviewNotes.visibility = View.VISIBLE
                tvReviewNotes.text = claimRequest.reviewNotes
            } else {
                tvReviewNotesLabel.visibility = View.GONE
                tvReviewNotes.visibility = View.GONE
            }
            
            // Note: Click listeners for approve/reject buttons would be set here
            // if they were part of the layout. Currently, this adapter is used
            // for displaying user's own requests in the browse tab.
            // The SecurityClaimReviewFragment may use a different layout with buttons.
        }
    }
    
    class ClaimRequestDiffCallback : DiffUtil.ItemCallback<ClaimRequest>() {
        override fun areItemsTheSame(oldItem: ClaimRequest, newItem: ClaimRequest): Boolean {
            return oldItem.requestId == newItem.requestId
        }
        
        override fun areContentsTheSame(oldItem: ClaimRequest, newItem: ClaimRequest): Boolean {
            return oldItem == newItem
        }
    }
}
