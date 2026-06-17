package com.example.loginandregistration.admin.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Manager for creating and managing notification channels
 * Requirements: 6.5
 */
object NotificationChannelManager {

    const val CHANNEL_ID_ITEM_MATCH = "item_match_channel"
    const val CHANNEL_ID_REQUEST_UPDATE = "request_update_channel"
    const val CHANNEL_ID_ADMIN_ALERT = "admin_alert_channel"
    const val CHANNEL_ID_SYSTEM = "system_channel"

    /**
     * Creates all notification channels for the app
     * Should be called when the app starts
     * Requirements: 6.5
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Create all channels
            val channels = listOf(
                createItemMatchChannel(),
                createRequestUpdateChannel(),
                createAdminAlertChannel(),
                createSystemChannel()
            )
            
            // Register all channels
            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Creates notification channel for item match notifications
     * High importance - user wants to know immediately when items match
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createItemMatchChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_ID_ITEM_MATCH,
            "Item Matches",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications when reported items match your lost items"
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
        }
    }

    /**
     * Creates notification channel for request update notifications
     * High importance - user needs to know about request status changes
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRequestUpdateChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_ID_REQUEST_UPDATE,
            "Request Updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications about your item request approvals and denials"
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
        }
    }

    /**
     * Creates notification channel for admin alerts and donation notices
     * Default importance - important but not urgent
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createAdminAlertChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_ID_ADMIN_ALERT,
            "Admin Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications from administrators and donation status updates"
            enableLights(true)
            enableVibration(false)
            setShowBadge(true)
        }
    }

    /**
     * Creates notification channel for system announcements
     * Default importance - informational messages
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createSystemChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_ID_SYSTEM,
            "System Announcements",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "System-wide announcements and maintenance notifications"
            enableLights(false)
            enableVibration(false)
            setShowBadge(true)
        }
    }

    /**
     * Deletes all notification channels
     * Useful for testing or resetting notification settings
     */
    fun deleteAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            listOf(
                CHANNEL_ID_ITEM_MATCH,
                CHANNEL_ID_REQUEST_UPDATE,
                CHANNEL_ID_ADMIN_ALERT,
                CHANNEL_ID_SYSTEM
            ).forEach { channelId ->
                notificationManager.deleteNotificationChannel(channelId)
            }
        }
    }

    /**
     * Checks if a specific channel is enabled
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun isChannelEnabled(context: Context, channelId: String): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel(channelId)
        return channel?.importance != NotificationManager.IMPORTANCE_NONE
    }

    /**
     * Gets the importance level of a channel
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getChannelImportance(context: Context, channelId: String): Int {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel(channelId)
        return channel?.importance ?: NotificationManager.IMPORTANCE_NONE
    }
}
