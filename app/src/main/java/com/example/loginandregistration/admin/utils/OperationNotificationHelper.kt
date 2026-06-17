package com.example.loginandregistration.admin.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.loginandregistration.R

/**
 * Helper for showing system notifications for operation completion
 * Requirements: 8.3, 8.6
 */
object OperationNotificationHelper {
    
    private const val CHANNEL_ID = "admin_operations"
    private const val CHANNEL_NAME = "Admin Operations"
    private const val CHANNEL_DESCRIPTION = "Notifications for admin operation completions"
    
    private var notificationId = 1000
    
    /**
     * Initialize notification channel (call this once at app startup)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show a success notification
     */
    fun showSuccessNotification(
        context: Context,
        title: String,
        message: String
    ) {
        showNotification(context, title, message, NotificationCompat.PRIORITY_DEFAULT)
    }
    
    /**
     * Show an error notification
     */
    fun showErrorNotification(
        context: Context,
        title: String,
        message: String
    ) {
        showNotification(context, title, message, NotificationCompat.PRIORITY_HIGH)
    }
    
    /**
     * Show an info notification
     */
    fun showInfoNotification(
        context: Context,
        title: String,
        message: String
    ) {
        showNotification(context, title, message, NotificationCompat.PRIORITY_LOW)
    }
    
    /**
     * Show export completion notification
     */
    fun showExportCompleteNotification(
        context: Context,
        fileName: String
    ) {
        showSuccessNotification(
            context,
            "Export Complete",
            "File '$fileName' has been exported successfully"
        )
    }
    
    /**
     * Show export failed notification
     */
    fun showExportFailedNotification(
        context: Context,
        reason: String
    ) {
        showErrorNotification(
            context,
            "Export Failed",
            "Export failed: $reason"
        )
    }
    
    /**
     * Show notification sent notification
     */
    fun showNotificationSentNotification(
        context: Context,
        recipientCount: Int
    ) {
        showSuccessNotification(
            context,
            "Notification Sent",
            "Notification sent to $recipientCount user(s)"
        )
    }
    
    /**
     * Show background operation complete notification
     */
    fun showBackgroundOperationComplete(
        context: Context,
        operationName: String,
        result: String
    ) {
        showSuccessNotification(
            context,
            "$operationName Complete",
            result
        )
    }
    
    private fun showNotification(
        context: Context,
        title: String,
        message: String,
        priority: Int
    ) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Use a default icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority)
                .setAutoCancel(true)
            
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId++, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted, silently fail
        }
    }
    
    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancelAll()
    }
}

/**
 * Extension functions for Context
 */
fun Context.showOperationSuccessNotification(title: String, message: String) {
    OperationNotificationHelper.showSuccessNotification(this, title, message)
}

fun Context.showOperationErrorNotification(title: String, message: String) {
    OperationNotificationHelper.showErrorNotification(this, title, message)
}

fun Context.showExportCompleteNotification(fileName: String) {
    OperationNotificationHelper.showExportCompleteNotification(this, fileName)
}
