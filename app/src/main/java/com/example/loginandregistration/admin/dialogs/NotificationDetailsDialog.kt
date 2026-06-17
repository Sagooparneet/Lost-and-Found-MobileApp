package com.example.loginandregistration.admin.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.PushNotification
import com.example.loginandregistration.admin.models.UserRole
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for displaying detailed notification information
 * Requirements: 6.9
 * Task: 13.4
 */
class NotificationDetailsDialog : DialogFragment() {
    
    // Views
    private lateinit var tvTitle: TextView
    private lateinit var tvBody: TextView
    private lateinit var tvType: TextView
    private lateinit var tvCreatedBy: TextView
    private lateinit var tvTimestamp: TextView
    private lateinit var tvRecipients: TextView
    private lateinit var tvTotalSent: TextView
    private lateinit var tvDelivered: TextView
    private lateinit var tvOpened: TextView
    private lateinit var tvFailed: TextView
    private lateinit var tvDeliveryRate: TextView
    private lateinit var tvOpenRate: TextView
    private lateinit var layoutActionUrl: LinearLayout
    private lateinit var tvActionUrl: TextView
    private lateinit var btnClose: MaterialButton
    
    private var notification: PushNotification? = null
    
    companion object {
        private const val ARG_NOTIFICATION = "notification"
        
        fun newInstance(notification: PushNotification): NotificationDetailsDialog {
            val dialog = NotificationDetailsDialog()
            val args = Bundle()
            args.putSerializable(ARG_NOTIFICATION, notification)
            dialog.arguments = args
            return dialog
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_notification_details, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get notification from arguments
        notification = arguments?.getSerializable(ARG_NOTIFICATION) as? PushNotification
        
        initViews(view)
        setupClickListeners()
        displayNotificationDetails()
    }
    
    override fun onStart() {
        super.onStart()
        // Make dialog full width
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    
    private fun initViews(view: View) {
        tvTitle = view.findViewById(R.id.tvTitle)
        tvBody = view.findViewById(R.id.tvBody)
        tvType = view.findViewById(R.id.tvType)
        tvCreatedBy = view.findViewById(R.id.tvCreatedBy)
        tvTimestamp = view.findViewById(R.id.tvTimestamp)
        tvRecipients = view.findViewById(R.id.tvRecipients)
        tvTotalSent = view.findViewById(R.id.tvTotalSent)
        tvDelivered = view.findViewById(R.id.tvDelivered)
        tvOpened = view.findViewById(R.id.tvOpened)
        tvFailed = view.findViewById(R.id.tvFailed)
        tvDeliveryRate = view.findViewById(R.id.tvDeliveryRate)
        tvOpenRate = view.findViewById(R.id.tvOpenRate)
        layoutActionUrl = view.findViewById(R.id.layoutActionUrl)
        tvActionUrl = view.findViewById(R.id.tvActionUrl)
        btnClose = view.findViewById(R.id.btnClose)
    }
    
    private fun setupClickListeners() {
        btnClose.setOnClickListener {
            dismiss()
        }
    }
    
    /**
     * Display complete notification information
     * Requirements: 6.9
     */
    private fun displayNotificationDetails() {
        notification?.let { notif ->
            // Basic information
            tvTitle.text = notif.title
            tvBody.text = notif.body
            tvType.text = notif.type.getDisplayName()
            tvCreatedBy.text = notif.createdBy
            
            // Timestamp
            val timestamp = if (notif.sentAt > 0) {
                notif.sentAt
            } else if (notif.scheduledFor > 0) {
                notif.scheduledFor
            } else {
                notif.createdAt
            }
            tvTimestamp.text = formatTimestamp(timestamp)
            
            // Recipients
            tvRecipients.text = formatRecipients(notif)
            
            // Delivery statistics
            val totalSent = notif.deliveredCount + notif.failedCount
            tvTotalSent.text = totalSent.toString()
            tvDelivered.text = notif.deliveredCount.toString()
            tvOpened.text = notif.openedCount.toString()
            tvFailed.text = notif.failedCount.toString()
            
            // Calculate and display percentages
            tvDeliveryRate.text = String.format(Locale.getDefault(), "%.1f%%", notif.getDeliveryRate())
            tvOpenRate.text = String.format(Locale.getDefault(), "%.1f%%", notif.getOpenRate())
            
            // Action URL (if present)
            if (notif.actionUrl.isNotBlank()) {
                layoutActionUrl.visibility = View.VISIBLE
                tvActionUrl.text = notif.actionUrl
            } else {
                layoutActionUrl.visibility = View.GONE
            }
        }
    }
    
    /**
     * Format timestamp for display
     */
    private fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * Format recipients for display
     * Requirements: 6.9
     */
    private fun formatRecipients(notification: PushNotification): String {
        return when {
            notification.targetUsers.contains("ALL") -> {
                "All Users"
            }
            notification.targetRoles.isNotEmpty() -> {
                val roles = notification.targetRoles.joinToString(", ") { role: UserRole -> role.getDisplayName() }
                "Users with roles: $roles"
            }
            notification.targetUsers.isNotEmpty() -> {
                if (notification.targetUsers.size == 1) {
                    notification.targetUsers.first()
                } else {
                    "${notification.targetUsers.size} individual users"
                }
            }
            else -> {
                "No recipients specified"
            }
        }
    }
}
