package com.example.loginandregistration.admin.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.PushNotification
import com.example.loginandregistration.admin.models.UserRole
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for previewing how a notification will appear
 * Requirements: 6.5
 * Task: 13.5
 */
class NotificationPreviewDialog : DialogFragment() {
    
    // Views
    private lateinit var tvPreviewTime: TextView
    private lateinit var tvNotificationTime: TextView
    private lateinit var tvPreviewTitle: TextView
    private lateinit var tvPreviewBody: TextView
    private lateinit var tvPreviewType: TextView
    private lateinit var tvPreviewRecipients: TextView
    private lateinit var btnClose: MaterialButton
    
    private var notification: PushNotification? = null
    
    companion object {
        private const val ARG_NOTIFICATION = "notification"
        
        fun newInstance(notification: PushNotification): NotificationPreviewDialog {
            val dialog = NotificationPreviewDialog()
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
        return inflater.inflate(R.layout.dialog_notification_preview, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get notification from arguments
        notification = arguments?.getSerializable(ARG_NOTIFICATION) as? PushNotification
        
        initViews(view)
        setupClickListeners()
        displayPreview()
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
        tvPreviewTime = view.findViewById(R.id.tvPreviewTime)
        tvNotificationTime = view.findViewById(R.id.tvNotificationTime)
        tvPreviewTitle = view.findViewById(R.id.tvPreviewTitle)
        tvPreviewBody = view.findViewById(R.id.tvPreviewBody)
        tvPreviewType = view.findViewById(R.id.tvPreviewType)
        tvPreviewRecipients = view.findViewById(R.id.tvPreviewRecipients)
        btnClose = view.findViewById(R.id.btnClose)
    }
    
    private fun setupClickListeners() {
        btnClose.setOnClickListener {
            dismiss()
        }
    }
    
    /**
     * Display notification preview
     * Requirements: 6.5
     */
    private fun displayPreview() {
        notification?.let { notif ->
            // Set current time for lock screen simulation
            val currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            tvPreviewTime.text = currentTime
            tvNotificationTime.text = "now"
            
            // Set notification content
            tvPreviewTitle.text = notif.title
            tvPreviewBody.text = notif.body
            
            // Set notification type
            tvPreviewType.text = notif.type.getDisplayName()
            
            // Set recipients
            tvPreviewRecipients.text = formatRecipients(notif)
        }
    }
    
    /**
     * Format recipients for display
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
