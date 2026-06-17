package com.example.loginandregistration.security.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.loginandregistration.databinding.ItemReportBinding
import com.example.loginandregistration.models.Report
import java.text.SimpleDateFormat
import java.util.*

class SecurityReportsAdapter(
    private val onApproveClicked: (Report) -> Unit,
    private val onRejectClicked: (Report) -> Unit
) : ListAdapter<Report, SecurityReportsAdapter.ReportViewHolder>(ReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = getItem(position)
        holder.bind(report, onApproveClicked, onRejectClicked)
    }

    class ReportViewHolder(private val binding: ItemReportBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(report: Report, onApprove: (Report) -> Unit, onReject: (Report) -> Unit) {
            binding.tvItemName.text = report.itemName
            binding.tvStatus.text = report.status
            binding.tvItemDescription.text = report.description
            val reportedByText = "Reported by: ${report.userEmail} on ${report.timestamp?.let { dateFormat.format(it) } ?: "N/A"}"
            binding.tvReportedBy.text = reportedByText

            binding.btnApprove.setOnClickListener { onApprove(report) }
            binding.btnReject.setOnClickListener { onReject(report) }
        }
    }

    class ReportDiffCallback : DiffUtil.ItemCallback<Report>() {
        override fun areItemsTheSame(oldItem: Report, newItem: Report): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Report, newItem: Report): Boolean {
            return oldItem == newItem
        }
    }
}

