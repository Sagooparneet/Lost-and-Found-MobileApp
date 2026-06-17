package com.example.loginandregistration.admin.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.ActionType
import com.example.loginandregistration.admin.models.ActivityLog
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Adapter for displaying activity logs with expandable details
 * Requirements: 5.3, 5.6
 * Task: 12.2 Create ActivityLogAdapter
 */
class ActivityLogAdapter(
    private val onItemClick: (ActivityLog) -> Unit
) : ListAdapter<ActivityLog, ActivityLogAdapter.ActivityLogViewHolder>(ActivityLogDiffCallback()) {
    
    private val expandedItems = mutableSetOf<String>()
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_log, parent, false)
        return ActivityLogViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ActivityLogViewHolder, position: Int) {
        val log = getItem(position)
        holder.bind(log, expandedItems.contains(log.id))
    }
    
    inner class ActivityLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivActionIcon: ImageView = itemView.findViewById(R.id.ivActionIcon)
        private val tvActionType: TextView = itemView.findViewById(R.id.tvActionType)
        private val tvActorEmail: TextView = itemView.findViewById(R.id.tvActorEmail)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val layoutTargetInfo: LinearLayout = itemView.findViewById(R.id.layoutTargetInfo)
        private val tvTargetInfo: TextView = itemView.findViewById(R.id.tvTargetInfo)
        private val layoutExpandedDetails: LinearLayout = itemView.findViewById(R.id.layoutExpandedDetails)
        private val layoutPreviousValue: LinearLayout = itemView.findViewById(R.id.layoutPreviousValue)
        private val tvPreviousValue: TextView = itemView.findViewById(R.id.tvPreviousValue)
        private val layoutNewValue: LinearLayout = itemView.findViewById(R.id.layoutNewValue)
        private val tvNewValue: TextView = itemView.findViewById(R.id.tvNewValue)
        private val layoutDeviceInfo: LinearLayout = itemView.findViewById(R.id.layoutDeviceInfo)
        private val tvDeviceInfo: TextView = itemView.findViewById(R.id.tvDeviceInfo)
        private val tvExpandIndicator: TextView = itemView.findViewById(R.id.tvExpandIndicator)
        
        fun bind(log: ActivityLog, isExpanded: Boolean) {
            // Set action type with color coding
            tvActionType.text = log.actionType.getDisplayName()
            
            // Set actor email
            tvActorEmail.text = log.actorEmail
            
            // Set timestamp
            tvTimestamp.text = formatTimestamp(log.timestamp)
            
            // Set description
            tvDescription.text = log.description
            
            // Set action icon and color based on action type
            val (iconRes, colorRes) = getIconAndColorForAction(log.actionType)
            ivActionIcon.setImageResource(iconRes)
            ivActionIcon.setColorFilter(ContextCompat.getColor(itemView.context, colorRes))
            ivActionIcon.background.setTint(getBackgroundColorForAction(log.actionType))
            
            // Set target info if available
            if (log.targetId.isNotBlank()) {
                layoutTargetInfo.visibility = View.VISIBLE
                tvTargetInfo.text = "${log.targetType.getDisplayName()}: ${log.targetId}"
            } else {
                layoutTargetInfo.visibility = View.GONE
            }
            
            // Handle expanded state
            if (isExpanded) {
                layoutExpandedDetails.visibility = View.VISIBLE
                tvExpandIndicator.text = "▲ Hide Details"
                
                // Show previous value if available
                if (log.previousValue.isNotBlank()) {
                    layoutPreviousValue.visibility = View.VISIBLE
                    tvPreviousValue.text = log.previousValue
                } else {
                    layoutPreviousValue.visibility = View.GONE
                }
                
                // Show new value if available
                if (log.newValue.isNotBlank()) {
                    layoutNewValue.visibility = View.VISIBLE
                    tvNewValue.text = log.newValue
                } else {
                    layoutNewValue.visibility = View.GONE
                }
                
                // Show device info if available
                if (log.deviceInfo.isNotBlank()) {
                    layoutDeviceInfo.visibility = View.VISIBLE
                    tvDeviceInfo.text = log.deviceInfo
                } else {
                    layoutDeviceInfo.visibility = View.GONE
                }
            } else {
                layoutExpandedDetails.visibility = View.GONE
                tvExpandIndicator.text = "▼ Show Details"
            }
            
            // Handle expand/collapse click
            tvExpandIndicator.setOnClickListener {
                toggleExpanded(log.id)
            }
            
            // Handle item click
            itemView.setOnClickListener {
                onItemClick(log)
            }
        }
        
        private fun toggleExpanded(logId: String) {
            if (expandedItems.contains(logId)) {
                expandedItems.remove(logId)
            } else {
                expandedItems.add(logId)
            }
            notifyItemChanged(bindingAdapterPosition)
        }
        
        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "${minutes}m ago"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "${hours}h ago"
                }
                diff < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "${days}d ago"
                }
                else -> {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    dateFormat.format(Date(timestamp))
                }
            }
        }
        
        private fun getIconAndColorForAction(actionType: ActionType): Pair<Int, Int> {
            return when {
                actionType.isAdminAction() -> {
                    when (actionType) {
                        ActionType.USER_BLOCK -> Pair(android.R.drawable.ic_delete, android.R.color.holo_red_dark)
                        ActionType.USER_UNBLOCK -> Pair(android.R.drawable.ic_menu_revert, android.R.color.holo_green_dark)
                        ActionType.USER_ROLE_CHANGE -> Pair(android.R.drawable.ic_menu_manage, android.R.color.holo_blue_dark)
                        ActionType.USER_EDIT -> Pair(android.R.drawable.ic_menu_edit, android.R.color.holo_blue_dark)
                        ActionType.ITEM_EDIT -> Pair(android.R.drawable.ic_menu_edit, android.R.color.holo_blue_dark)
                        ActionType.ITEM_STATUS_CHANGE -> Pair(android.R.drawable.ic_menu_rotate, android.R.color.holo_orange_dark)
                        ActionType.ITEM_DELETE -> Pair(android.R.drawable.ic_menu_delete, android.R.color.holo_red_dark)
                        ActionType.DONATION_MARK_READY -> Pair(android.R.drawable.ic_menu_upload, android.R.color.holo_purple)
                        ActionType.DONATION_COMPLETE -> Pair(android.R.drawable.ic_menu_send, android.R.color.holo_green_dark)
                        ActionType.NOTIFICATION_SEND -> Pair(android.R.drawable.ic_dialog_email, android.R.color.holo_blue_dark)
                        ActionType.DATA_EXPORT -> Pair(android.R.drawable.ic_menu_save, android.R.color.holo_blue_dark)
                        else -> Pair(android.R.drawable.ic_menu_info_details, android.R.color.holo_blue_dark)
                    }
                }
                actionType.isSystemEvent() -> {
                    Pair(android.R.drawable.ic_dialog_info, android.R.color.darker_gray)
                }
                else -> {
                    // User actions
                    when (actionType) {
                        ActionType.USER_LOGIN -> Pair(android.R.drawable.ic_menu_mylocation, android.R.color.holo_green_dark)
                        ActionType.USER_LOGOUT -> Pair(android.R.drawable.ic_lock_power_off, android.R.color.darker_gray)
                        ActionType.USER_REGISTER -> Pair(android.R.drawable.ic_menu_add, android.R.color.holo_green_dark)
                        ActionType.ITEM_REPORT -> Pair(android.R.drawable.ic_menu_add, android.R.color.holo_blue_light)
                        ActionType.ITEM_REQUEST -> Pair(android.R.drawable.ic_menu_search, android.R.color.holo_orange_light)
                        ActionType.ITEM_CLAIM -> Pair(android.R.drawable.ic_menu_compass, android.R.color.holo_green_light)
                        else -> Pair(android.R.drawable.ic_menu_info_details, android.R.color.holo_blue_light)
                    }
                }
            }
        }
        
        private fun getBackgroundColorForAction(actionType: ActionType): Int {
            return when {
                actionType.isAdminAction() -> {
                    when (actionType) {
                        ActionType.USER_BLOCK, ActionType.ITEM_DELETE -> 
                            Color.parseColor("#FFEBEE") // Light red
                        ActionType.USER_UNBLOCK, ActionType.DONATION_COMPLETE -> 
                            Color.parseColor("#E8F5E9") // Light green
                        ActionType.DONATION_MARK_READY -> 
                            Color.parseColor("#F3E5F5") // Light purple
                        ActionType.ITEM_STATUS_CHANGE -> 
                            Color.parseColor("#FFF3E0") // Light orange
                        else -> 
                            Color.parseColor("#E3F2FD") // Light blue
                    }
                }
                actionType.isSystemEvent() -> 
                    Color.parseColor("#F5F5F5") // Light gray
                else -> 
                    Color.parseColor("#E8F5E9") // Light green for user actions
            }
        }
    }
    
    class ActivityLogDiffCallback : DiffUtil.ItemCallback<ActivityLog>() {
        override fun areItemsTheSame(oldItem: ActivityLog, newItem: ActivityLog): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ActivityLog, newItem: ActivityLog): Boolean {
            return oldItem == newItem
        }
    }
}
