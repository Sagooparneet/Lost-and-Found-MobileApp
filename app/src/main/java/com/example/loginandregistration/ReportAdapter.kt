package com.example.loginandregistration

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class ReportAdapter(
    private val reportList: List<LostFoundItem>,
    private val onApproveClicked: (LostFoundItem) -> Unit,
    private val onRejectClicked: (LostFoundItem) -> Unit
) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardItem: MaterialCardView = itemView.findViewById(R.id.card_item)
        val itemName: TextView = itemView.findViewById(R.id.tv_item_name)
        val itemDescription: TextView = itemView.findViewById(R.id.tv_item_description)
        val reportedBy: TextView = itemView.findViewById(R.id.tv_reported_by)
        val status: TextView = itemView.findViewById(R.id.tv_status)
        val approveButton: Button = itemView.findViewById(R.id.btn_approve)
        val rejectButton: Button = itemView.findViewById(R.id.btn_reject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reportList[position]

        // Display item type (Lost/Found) with the name
        val itemType = if (report.isLost) "Lost" else "Found"
        holder.itemName.text = "$itemType: ${report.name}"
        holder.itemDescription.text = report.description
        holder.reportedBy.text = "By: ${report.userEmail}" // Shows user email
        holder.status.text = report.status

        // Highlight pending items with orange stroke
        // Requirements: 4.6
        when (report.status) {
            "Approved" -> {
                holder.status.setTextColor(Color.parseColor("#4CAF50")) // Green
                holder.cardItem.strokeColor = Color.parseColor("#E0E0E0") // Default gray
            }
            "Rejected" -> {
                holder.status.setTextColor(Color.parseColor("#F44336")) // Red
                holder.cardItem.strokeColor = Color.parseColor("#E0E0E0") // Default gray
            }
            "Pending Approval" -> {
                holder.status.setTextColor(Color.parseColor("#FF9800")) // Orange
                holder.cardItem.strokeColor = Color.parseColor("#FF9800") // Orange highlight
            }
            else -> {
                holder.status.setTextColor(Color.parseColor("#FF9800")) // Orange
                holder.cardItem.strokeColor = Color.parseColor("#FF9800") // Orange highlight
            }
        }

        // Set click listeners for the buttons
        holder.approveButton.setOnClickListener {
            onApproveClicked(report)
        }
        holder.rejectButton.setOnClickListener {
            onRejectClicked(report)
        }
    }

    override fun getItemCount() = reportList.size
}
