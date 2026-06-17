package com.example.loginandregistration.admin.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.DeliveryStatus
import com.example.loginandregistration.admin.models.NotificationType
import com.example.loginandregistration.admin.models.PushNotification
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Adapter for displaying notification history
 * Requirements: 6.9
 * Task: 13.3
 */
class NotificationHistoryAdapter(
    private val onItemClick: (PushNotification) -> Unit
) : ListAdapter<PushNotification, NotificationHistoryAdapter.NotificationViewHolder>(NotificationDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification_history, parent, false)
        return NotificationViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = getItem(position)
        holder.bind(notification)
    }
    
    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivNotificationIcon: ImageView = itemView.findViewById(R.id.ivNotificationIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvBody: TextView = itemView.findViewById(R.id.tvBody)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val tvSentCount: TextView = itemView.findViewById(R.id.tvSentCount)
        private val tvDeliveredCount: TextView = itemView.findViewById(R.id.tvDeliveredCount)
        private val tvOpenedCount: TextView = itemView.findViewById(R.id.tvOpenedCount)
        private val tvOpenRate: TextView = itemView.findViewById(R.id.tvOpenRate)
        
        fun bind(notification: PushNotification) {
            // Set title
            tvTitle.text = notification.title
            
            // Set notification type
            tvType.text = notification.type.getDisplayName()
            
            // Set body
            tvBody.text = notification.body
            
            // Set timestamp
            tvTimestamp.text = formatTimestamp(notification)
            
            // Set icon based on notification type
            val iconRes = getIconForNotificationType(notification.type)
            ivNotificationIcon.setImageResource(iconRes)
            
            // Set icon background color based on type
            val iconColor = getColorForNotificationType(notification.type)
            ivNotificationIcon.setColorFilter(ContextCompat.getColor(itemView.context, iconColor))
            
            // Set status badge
            tvStatus.text = notification.deliveryStatus.getDisplayName()
            val statusColor = getColorForDeliveryStatus(notification.deliveryStatus)
            tvStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, statusColor))
            
            // Set delivery statistics
            val totalSent = notification.deliveredCount + notification.failedCount
            tvSentCount.text = totalSent.toString()
            tvDeliveredCount.text = notification.deliveredCount.toString()
            tvOpenedCount.text = notification.openedCount.toString()
            tvOpenRate.text = String.format(Locale.getDefault(), "%.1f%%", notification.getOpenRate())
            
            // Set click listener
            itemView.setOnClickListener {
                onItemClick(notification)
            }
        }
        
        /**
         * Format timestamp for display
         */
        private fun formatTimestamp(notification: PushNotification): String {
            val timestamp = if (notification.sentAt > 0) {
                notification.sentAt
            } else if (notification.scheduledFor > 0) {
                return "Scheduled for ${formatDate(notification.scheduledFor)}"
            } else {
                notification.createdAt
            }
            
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "Sent $minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "Sent $hours ${if (hours == 1L) "hour" else "hours"} ago"
                }
                diff < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "Sent $days ${if (days == 1L) "day" else "days"} ago"
                }
                else -> "Sent on ${formatDate(timestamp)}"
            }
        }
        
        /**
         * Format date for display
         */
        private fun formatDate(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            return dateFormat.format(Date(timestamp))
        }
        
        /**
         * Get icon resource for notification type
         */
        private fun getIconForNotificationType(type: NotificationType): Int {
            return when (type) {
                NotificationType.ITEM_MATCH -> android.R.drawable.ic_menu_search
                NotificationType.REQUEST_APPROVED -> android.R.drawable.ic_menu_info_details
                NotificationType.REQUEST_DENIED -> android.R.drawable.ic_delete
                NotificationType.DONATION_NOTICE -> android.R.drawable.ic_menu_share
                NotificationType.CUSTOM_ADMIN -> android.R.drawable.ic_dialog_email
                NotificationType.SYSTEM_ANNOUNCEMENT -> android.R.drawable.ic_dialog_info
                NotificationType.SECURITY_ALERT -> android.R.drawable.ic_dialog_alert
            }
        }
        
        /**
         * Get color for notification type
         */
        private fun getColorForNotificationType(type: NotificationType): Int {
            return when (type) {
                NotificationType.ITEM_MATCH -> android.R.color.holo_blue_dark
                NotificationType.REQUEST_APPROVED -> android.R.color.holo_green_dark
                NotificationType.REQUEST_DENIED -> android.R.color.holo_red_dark
                NotificationType.DONATION_NOTICE -> android.R.color.holo_orange_dark
                NotificationType.CUSTOM_ADMIN -> android.R.color.holo_purple
                NotificationType.SYSTEM_ANNOUNCEMENT -> android.R.color.holo_blue_light
                NotificationType.SECURITY_ALERT -> android.R.color.holo_red_light
            }
        }
        
        /**
         * Get color for delivery status
         */
        private fun getColorForDeliveryStatus(status: DeliveryStatus): Int {
            return when (status) {
                DeliveryStatus.PENDING -> android.R.color.darker_gray
                DeliveryStatus.SCHEDULED -> android.R.color.holo_blue_light
                DeliveryStatus.SENDING -> android.R.color.holo_orange_light
                DeliveryStatus.SENT -> android.R.color.holo_green_dark
                DeliveryStatus.FAILED -> android.R.color.holo_red_dark
                DeliveryStatus.PARTIALLY_SENT -> android.R.color.holo_orange_dark
            }
        }
    }
    
    /**
     * DiffUtil callback for efficient list updates
     */
    class NotificationDiffCallback : DiffUtil.ItemCallback<PushNotification>() {
        override fun areItemsTheSame(oldItem: PushNotification, newItem: PushNotification): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: PushNotification, newItem: PushNotification): Boolean {
            return oldItem == newItem
        }
    }
}
