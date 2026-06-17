package com.example.loginandregistration.utils

import android.util.Log
import com.example.loginandregistration.Notification
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Utility object for managing notifications in the Lost & Found system.
 * Handles creation of notification documents and sending FCM push notifications.
 * Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7
 */
object NotificationManager {
    
    private const val TAG = "NotificationManager"
    private val db = FirebaseFirestore.getInstance()
    
    /**
     * Creates a notification document in Firestore.
     * 
     * @param userId The ID of the user to receive the notification
     * @param userEmail The email of the user to receive the notification
     * @param type The type of notification
     * @param title The notification title
     * @param message The notification message
     * @param itemId Optional item ID related to the notification
     * @param requestId Optional request ID related to the notification
     * @return Result containing the notification ID or error
     */
    private suspend fun createNotification(
        userId: String,
        userEmail: String,
        type: String,
        title: String,
        message: String,
        itemId: String = "",
        requestId: String = ""
    ): Result<String> {
        return try {
            val notificationRef = db.collection("notifications").document()
            val notification = Notification(
                notificationId = notificationRef.id,
                userId = userId,
                userEmail = userEmail,
                type = type,
                title = title,
                message = message,
                itemId = itemId,
                requestId = requestId,
                timestamp = Timestamp.now(),
                read = false,
                delivered = false
            )
            
            notificationRef.set(notification).await()
            
            Log.d(TAG, "Notification created: ${notification.notificationId} for user: $userEmail")
            
            // Send FCM notification
            sendFcmNotification(userId, type, title, message, itemId, requestId, notification.notificationId)
            
            Result.success(notification.notificationId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sends an FCM push notification to a user.
     * Note: This creates a notification document that can be picked up by a Cloud Function
     * to send the actual FCM message. Direct FCM sending requires server-side implementation.
     * 
     * @param userId The ID of the user to receive the notification
     * @param type The type of notification
     * @param title The notification title
     * @param message The notification message
     * @param itemId Optional item ID related to the notification
     * @param requestId Optional request ID related to the notification
     * @param notificationId The ID of the notification document
     */
    private suspend fun sendFcmNotification(
        userId: String,
        type: String,
        title: String,
        message: String,
        itemId: String,
        requestId: String,
        notificationId: String
    ) {
        try {
            // Get user's FCM token
            val userDoc = db.collection("users")
                .document(userId)
                .get()
                .await()
            
            val fcmToken = userDoc.getString("fcmToken")
            
            if (fcmToken.isNullOrEmpty()) {
                Log.w(TAG, "No FCM token found for user: $userId")
                return
            }
            
            // Create FCM notification request document
            // This can be picked up by a Cloud Function to send the actual FCM message
            val fcmData = hashMapOf(
                "token" to fcmToken,
                "type" to type,
                "title" to title,
                "body" to message,
                "itemId" to itemId,
                "requestId" to requestId,
                "notificationId" to notificationId,
                "timestamp" to Timestamp.now()
            )
            
            db.collection("fcmQueue")
                .add(fcmData)
                .await()
            
            Log.d(TAG, "FCM notification queued for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending FCM notification", e)
        }
    }
    
    /**
     * Gets all security users from Firestore.
     * 
     * @return List of user IDs and emails for security users
     */
    private suspend fun getSecurityUsers(): List<Pair<String, String>> {
        return try {
            val users = mutableListOf<Pair<String, String>>()
            
            // Get all users
            val querySnapshot = db.collection("users")
                .get()
                .await()
            
            for (document in querySnapshot.documents) {
                val email = document.getString("email") ?: continue
                val userId = document.id
                
                // Check if user is security or admin
                if (UserRoleManager.isSecurity(email)) {
                    users.add(Pair(userId, email))
                }
            }
            
            Log.d(TAG, "Found ${users.size} security users")
            users
        } catch (e: Exception) {
            Log.e(TAG, "Error getting security users", e)
            emptyList()
        }
    }
    
    /**
     * Sends notification when a found item is submitted.
     * Notifies all security users about the new pending item.
     * Requirements: 11.1
     * 
     * @param itemId The ID of the submitted found item
     * @param itemName The name of the submitted item
     */
    suspend fun notifyFoundItemSubmitted(itemId: String, itemName: String) {
        try {
            val securityUsers = getSecurityUsers()
            
            if (securityUsers.isEmpty()) {
                Log.w(TAG, "No security users found to notify")
                return
            }
            
            val title = "New Found Item Pending"
            val message = "A new found item '$itemName' has been submitted and requires approval."
            
            for ((userId, userEmail) in securityUsers) {
                createNotification(
                    userId = userId,
                    userEmail = userEmail,
                    type = Notification.TYPE_FOUND_ITEM_SUBMITTED,
                    title = title,
                    message = message,
                    itemId = itemId
                )
            }
            
            Log.d(TAG, "Notified ${securityUsers.size} security users about found item: $itemId")
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying found item submitted", e)
        }
    }
    
    /**
     * Sends notification when a found item is approved.
     * Notifies the reporting user that their item has been approved.
     * Requirements: 11.2
     * 
     * @param itemId The ID of the approved item
     */
    suspend fun notifyFoundItemApproved(itemId: String) {
        try {
            // Get item details
            val itemDoc = db.collection("items")
                .document(itemId)
                .get()
                .await()
            
            val itemName = itemDoc.getString("name") ?: "Item"
            val userId = itemDoc.getString("userId") ?: return
            val userEmail = itemDoc.getString("userEmail") ?: return
            
            val title = "Found Item Approved"
            val message = "Your found item '$itemName' has been approved and is now visible to all users."
            
            createNotification(
                userId = userId,
                userEmail = userEmail,
                type = Notification.TYPE_FOUND_ITEM_APPROVED,
                title = title,
                message = message,
                itemId = itemId
            )
            
            Log.d(TAG, "Notified user $userEmail about approved item: $itemId")
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying found item approved", e)
        }
    }
    
    /**
     * Sends notification when a found item is rejected.
     * Notifies the reporting user that their item has been rejected with reason.
     * Requirements: 11.3
     * 
     * @param itemId The ID of the rejected item
     * @param reason The reason for rejection
     */
    suspend fun notifyFoundItemRejected(itemId: String, reason: String) {
        try {
            // Get item details
            val itemDoc = db.collection("items")
                .document(itemId)
                .get()
                .await()
            
            val itemName = itemDoc.getString("name") ?: "Item"
            val userId = itemDoc.getString("userId") ?: return
            val userEmail = itemDoc.getString("userEmail") ?: return
            
            val title = "Found Item Rejected"
            val message = if (reason.isNotEmpty()) {
                "Your found item '$itemName' has been rejected. Reason: $reason"
            } else {
                "Your found item '$itemName' has been rejected."
            }
            
            createNotification(
                userId = userId,
                userEmail = userEmail,
                type = Notification.TYPE_FOUND_ITEM_REJECTED,
                title = title,
                message = message,
                itemId = itemId
            )
            
            Log.d(TAG, "Notified user $userEmail about rejected item: $itemId")
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying found item rejected", e)
        }
    }
    
    /**
     * Sends notification when a claim request is submitted.
     * Notifies all security users about the new claim request.
     * Requirements: 11.4
     * 
     * @param requestId The ID of the submitted claim request
     */
    suspend fun notifyClaimSubmitted(requestId: String) {
        try {
            // Get claim request details
            val claimDoc = db.collection("claimRequests")
                .document(requestId)
                .get()
                .await()
            
            val itemName = claimDoc.getString("itemName") ?: "Item"
            val userName = claimDoc.getString("userName") ?: "User"
            
            val securityUsers = getSecurityUsers()
            
            if (securityUsers.isEmpty()) {
                Log.w(TAG, "No security users found to notify")
                return
            }
            
            val title = "New Claim Request"
            val message = "$userName has submitted a claim request for '$itemName'."
            
            for ((userId, userEmail) in securityUsers) {
                createNotification(
                    userId = userId,
                    userEmail = userEmail,
                    type = Notification.TYPE_CLAIM_SUBMITTED,
                    title = title,
                    message = message,
                    requestId = requestId
                )
            }
            
            Log.d(TAG, "Notified ${securityUsers.size} security users about claim request: $requestId")
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying claim submitted", e)
        }
    }
    
    /**
     * Sends notification when a claim request is approved.
     * Notifies the requesting user to collect their item.
     * Requirements: 11.5
     * 
     * @param requestId The ID of the approved claim request
     */
    suspend fun notifyClaimApproved(requestId: String) {
        try {
            // Get claim request details
            val claimDoc = db.collection("claimRequests")
                .document(requestId)
                .get()
                .await()
            
            val itemName = claimDoc.getString("itemName") ?: "Item"
            val userId = claimDoc.getString("userId") ?: return
            val userEmail = claimDoc.getString("userEmail") ?: return
            
            val title = "Claim Approved - Collect Item"
            val message = "Your claim for '$itemName' has been approved. Please visit the security office to collect your item."
            
            createNotification(
                userId = userId,
                userEmail = userEmail,
                type = Notification.TYPE_CLAIM_APPROVED,
                title = title,
                message = message,
                requestId = requestId
            )
            
            Log.d(TAG, "Notified user $userEmail about approved claim: $requestId")
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying claim approved", e)
        }
    }
    
    /**
     * Sends notification when a claim request is rejected.
     * Notifies the requesting user that their claim has been rejected with reason.
     * Requirements: 11.6
     * 
     * @param requestId The ID of the rejected claim request
     * @param reason The reason for rejection
     */
    suspend fun notifyClaimRejected(requestId: String, reason: String) {
        try {
            // Get claim request details
            val claimDoc = db.collection("claimRequests")
                .document(requestId)
                .get()
                .await()
            
            val itemName = claimDoc.getString("itemName") ?: "Item"
            val userId = claimDoc.getString("userId") ?: return
            val userEmail = claimDoc.getString("userEmail") ?: return
            
            val title = "Claim Rejected"
            val message = if (reason.isNotEmpty()) {
                "Your claim for '$itemName' has been rejected. Reason: $reason"
            } else {
                "Your claim for '$itemName' has been rejected."
            }
            
            createNotification(
                userId = userId,
                userEmail = userEmail,
                type = Notification.TYPE_CLAIM_REJECTED,
                title = title,
                message = message,
                requestId = requestId
            )
            
            Log.d(TAG, "Notified user $userEmail about rejected claim: $requestId")
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying claim rejected", e)
        }
    }
}
