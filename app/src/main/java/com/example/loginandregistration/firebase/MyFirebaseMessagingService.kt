package com.example.loginandregistration.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.loginandregistration.MainActivity
import com.example.loginandregistration.Notification
import com.example.loginandregistration.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Firebase Cloud Messaging Service for handling push notifications
 * Requirements: 6.5, 6.10
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID_ITEM_MATCH = "item_match_channel"
        const val CHANNEL_ID_REQUEST_UPDATE = "request_update_channel"
        const val CHANNEL_ID_ADMIN_ALERT = "admin_alert_channel"
        const val CHANNEL_ID_SYSTEM = "system_channel"
        
        // Notification IDs
        private const val NOTIFICATION_ID_ITEM_MATCH = 1001
        private const val NOTIFICATION_ID_REQUEST = 1002
        private const val NOTIFICATION_ID_ADMIN = 1003
        private const val NOTIFICATION_ID_SYSTEM = 1004
    }

    /**
     * Called when a new FCM token is generated
     * Updates the token in Firestore for the current user
     * Requirements: 6.10
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token generated: $token")
        
        // Update token in Firestore for current user
        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateFcmTokenInFirestore(currentUser.uid, token)
        } else {
            Log.w(TAG, "No authenticated user to update FCM token")
        }
    }

    /**
     * Called when a message is received from FCM
     * Handles incoming notifications and displays them
     * Requirements: 6.5
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d(TAG, "Message received from: ${message.from}")
        
        // Check if message contains data payload
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${message.data}")
            handleDataPayload(message.data)
        }
        
        // Check if message contains notification payload
        message.notification?.let {
            Log.d(TAG, "Message notification: ${it.title} - ${it.body}")
            showNotification(
                title = it.title ?: "Lost & Found",
                body = it.body ?: "",
                notificationType = message.data["type"] ?: "CUSTOM_ADMIN",
                actionUrl = message.data["actionUrl"] ?: "",
                notificationId = message.data["notificationId"] ?: ""
            )
        }
    }

    /**
     * Handles data payload from FCM message
     */
    private fun handleDataPayload(data: Map<String, String>) {
        val title = data["title"] ?: "Lost & Found"
        val body = data["body"] ?: ""
        val type = data["type"] ?: "CUSTOM_ADMIN"
        val actionUrl = data["actionUrl"] ?: ""
        val notificationId = data["notificationId"] ?: ""
        
        showNotification(title, body, type, actionUrl, notificationId)
        
        // Update notification history
        updateNotificationHistory(notificationId, true, "")
    }

    /**
     * Displays a notification to the user
     * Requirements: 6.5
     */
    private fun showNotification(
        title: String,
        body: String,
        notificationType: String,
        actionUrl: String,
        notificationId: String
    ) {
        val channelId = getChannelIdForType(notificationType)
        val notificationIdInt = getNotificationIdForType(notificationType)
        
        // Create intent for notification tap
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", notificationType)
            putExtra("action_url", actionUrl)
            putExtra("notification_id", notificationId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationIdInt,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        // Show notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationIdInt, notificationBuilder.build())
        
        Log.d(TAG, "Notification displayed: $title")
    }

    /**
     * Gets the appropriate channel ID based on notification type
     * Requirements: 11.7
     */
    private fun getChannelIdForType(type: String): String {
        return when (type) {
            "ITEM_MATCH" -> CHANNEL_ID_ITEM_MATCH
            "REQUEST_APPROVED", "REQUEST_DENIED" -> CHANNEL_ID_REQUEST_UPDATE
            "DONATION_NOTICE", "CUSTOM_ADMIN", "SECURITY_ALERT" -> CHANNEL_ID_ADMIN_ALERT
            "SYSTEM_ANNOUNCEMENT" -> CHANNEL_ID_SYSTEM
            // New notification types for found items and claims
            Notification.TYPE_FOUND_ITEM_SUBMITTED -> CHANNEL_ID_ADMIN_ALERT
            Notification.TYPE_FOUND_ITEM_APPROVED -> CHANNEL_ID_REQUEST_UPDATE
            Notification.TYPE_FOUND_ITEM_REJECTED -> CHANNEL_ID_REQUEST_UPDATE
            Notification.TYPE_CLAIM_SUBMITTED -> CHANNEL_ID_ADMIN_ALERT
            Notification.TYPE_CLAIM_APPROVED -> CHANNEL_ID_REQUEST_UPDATE
            Notification.TYPE_CLAIM_REJECTED -> CHANNEL_ID_REQUEST_UPDATE
            else -> CHANNEL_ID_SYSTEM
        }
    }

    /**
     * Gets the appropriate notification ID based on notification type
     * Requirements: 11.7
     */
    private fun getNotificationIdForType(type: String): Int {
        return when (type) {
            "ITEM_MATCH" -> NOTIFICATION_ID_ITEM_MATCH
            "REQUEST_APPROVED", "REQUEST_DENIED" -> NOTIFICATION_ID_REQUEST
            "DONATION_NOTICE", "CUSTOM_ADMIN", "SECURITY_ALERT" -> NOTIFICATION_ID_ADMIN
            "SYSTEM_ANNOUNCEMENT" -> NOTIFICATION_ID_SYSTEM
            // New notification types for found items and claims
            Notification.TYPE_FOUND_ITEM_SUBMITTED -> NOTIFICATION_ID_ADMIN
            Notification.TYPE_FOUND_ITEM_APPROVED -> NOTIFICATION_ID_REQUEST
            Notification.TYPE_FOUND_ITEM_REJECTED -> NOTIFICATION_ID_REQUEST
            Notification.TYPE_CLAIM_SUBMITTED -> NOTIFICATION_ID_ADMIN
            Notification.TYPE_CLAIM_APPROVED -> NOTIFICATION_ID_REQUEST
            Notification.TYPE_CLAIM_REJECTED -> NOTIFICATION_ID_REQUEST
            else -> NOTIFICATION_ID_SYSTEM
        }
    }

    /**
     * Updates FCM token in Firestore for the user
     */
    private fun updateFcmTokenInFirestore(userId: String, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.collection("users")
                    .document(userId)
                    .update("fcmToken", token)
                    .await()
                
                Log.d(TAG, "FCM token updated in Firestore for user: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update FCM token in Firestore", e)
            }
        }
    }

    /**
     * Updates notification history in Firestore
     */
    private fun updateNotificationHistory(
        notificationId: String,
        delivered: Boolean,
        errorMessage: String
    ) {
        if (notificationId.isBlank()) return
        
        val currentUser = auth.currentUser ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val historyData = mapOf(
                    "notificationId" to notificationId,
                    "userId" to currentUser.uid,
                    "userEmail" to (currentUser.email ?: ""),
                    "deliveredAt" to System.currentTimeMillis(),
                    "isOpened" to false,
                    "errorMessage" to errorMessage
                )
                
                firestore.collection("notificationHistory")
                    .add(historyData)
                    .await()
                
                // Update notification delivered count
                if (delivered) {
                    firestore.collection("notifications")
                        .document(notificationId)
                        .update(
                            mapOf(
                                "deliveredCount" to com.google.firebase.firestore.FieldValue.increment(1),
                                "deliveryStatus" to "SENT"
                            )
                        )
                        .await()
                }
                
                Log.d(TAG, "Notification history updated for: $notificationId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update notification history", e)
            }
        }
    }
}
