package com.example.loginandregistration.admin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.ExportRequest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying export history
 * Requirements: 4.1
 * Task: 14.1, 14.4
 */
class ExportHistoryAdapter(
    private val onItemClick: (ExportRequest) -> Unit,
    private val onDeleteClick: (ExportRequest) -> Unit
) : ListAdapter<ExportRequest, ExportHistoryAdapter.ExportHistoryViewHolder>(ExportDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExportHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_export_history, parent, false)
        return ExportHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExportHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExportHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.exportTitleText)
        private val dateText: TextView = itemView.findViewById(R.id.exportDateText)
        private val statusText: TextView = itemView.findViewById(R.id.exportStatusText)
        private val formatText: TextView = itemView.findViewById(R.id.exportFormatText)
        private val fileSizeText: TextView = itemView.findViewById(R.id.fileSizeText)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(exportRequest: ExportRequest) {
            // Set title
            titleText.text = exportRequest.dataType.getDisplayName()
            
            // Set date
            dateText.text = dateFormat.format(Date(exportRequest.requestedAt))
            
            // Set status with color
            statusText.text = exportRequest.status.getDisplayName()
            statusText.setTextColor(getStatusColor(exportRequest.status))
            
            // Set format
            formatText.text = exportRequest.format.getDisplayName()
            
            // Set file size if available
            if (exportRequest.fileUrl.isNotEmpty() && exportRequest.isComplete()) {
                try {
                    val file = java.io.File(exportRequest.fileUrl)
                    if (file.exists()) {
                        fileSizeText.text = formatFileSize(file.length())
                        fileSizeText.visibility = View.VISIBLE
                    } else {
                        fileSizeText.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    fileSizeText.visibility = View.GONE
                }
            } else {
                fileSizeText.visibility = View.GONE
            }
            
            // Set click listeners
            itemView.setOnClickListener {
                onItemClick(exportRequest)
            }
            
            deleteButton.setOnClickListener {
                onDeleteClick(exportRequest)
            }
        }

        private fun formatFileSize(sizeInBytes: Long): String {
            return when {
                sizeInBytes < 1024 -> "$sizeInBytes B"
                sizeInBytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f KB", sizeInBytes / 1024f)
                else -> String.format(Locale.getDefault(), "%.2f MB", sizeInBytes / (1024f * 1024f))
            }
        }

        private fun getStatusColor(status: com.example.loginandregistration.admin.models.ExportStatus): Int {
            return when (status) {
                com.example.loginandregistration.admin.models.ExportStatus.COMPLETED -> 
                    itemView.context.getColor(android.R.color.holo_green_dark)
                com.example.loginandregistration.admin.models.ExportStatus.FAILED -> 
                    itemView.context.getColor(android.R.color.holo_red_dark)
                com.example.loginandregistration.admin.models.ExportStatus.PROCESSING -> 
                    itemView.context.getColor(android.R.color.holo_orange_dark)
                com.example.loginandregistration.admin.models.ExportStatus.PENDING -> 
                    itemView.context.getColor(android.R.color.darker_gray)
            }
        }
    }

    class ExportDiffCallback : DiffUtil.ItemCallback<ExportRequest>() {
        override fun areItemsTheSame(oldItem: ExportRequest, newItem: ExportRequest): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExportRequest, newItem: ExportRequest): Boolean {
            return oldItem == newItem
        }
    }
}
