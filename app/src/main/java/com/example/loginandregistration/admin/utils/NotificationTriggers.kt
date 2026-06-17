package com.example.loginandregistration.admin.utils

import android.util.Log
import com.example.loginandregistration.admin.models.*
import com.example.loginandregistration.admin.repository.AdminRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Utility class for triggering automatic notifications
 * Requirements: 6.1, 6.2, 6.3, 6.12
 */
object NotificationTriggers {
    
    private const val TAG = "NotificationTriggers"
    private val repository = AdminRepository()
    
    /**
     * Send notification when an item matches a user's lost item
     * Requirements: 6.1
     */
    fun sendItemMatchNotification(
        userId: String,
        itemId: String,
        itemName: String,
        itemDescription: String,
        location: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = PushNotification(
                    title = "Item Match Found!",
                    body = "A $itemName matching your lost item was found at $location",
                    type = NotificationType.ITEM_MATCH,
                    targetUsers = listOf(userId),
                    actionUrl = "item/$itemId",
                    metadata = mapOf(
                        "itemId" to itemId,
                        "itemName" to itemName,
                        "location" to location
                    )
                )
                
                val result = repository.sendPushNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Item match notification sent to user: $userId")
                } else {
                    Log.e(TAG, "Failed to send item match notification: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending item match notification", e)
            }
        }
    }
    
    /**
     * Send notification when a request is approved
     * Requirements: 6.2
     */
    fun sendRequestApprovedNotification(
        userId: String,
        itemId: String,
        itemName: String,
        pickupLocation: String,
        pickupInstructions: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = PushNotification(
                    title = "Request Approved!",
                    body = "Your request for $itemName has been approved. Pickup at: $pickupLocation",
                    type = NotificationType.REQUEST_APPROVED,
                    targetUsers = listOf(userId),
                    actionUrl = "request/$itemId",
                    metadata = mapOf(
                        "itemId" to itemId,
                        "itemName" to itemName,
                        "pickupLocation" to pickupLocation,
                        "pickupInstructions" to pickupInstructions
                    )
                )
                
                val result = repository.sendPushNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Request approved notification sent to user: $userId")
                } else {
                    Log.e(TAG, "Failed to send request approved notification: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending request approved notification", e)
            }
        }
    }
    
    /**
     * Send notification when a request is denied
     * Requirements: 6.3
     */
    fun sendRequestDeniedNotification(
        userId: String,
        itemId: String,
        itemName: String,
        denialReason: String,
        nextSteps: String = "Please contact security for more information."
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = PushNotification(
                    title = "Request Denied",
                    body = "Your request for $itemName was denied. Reason: $denialReason",
                    type = NotificationType.REQUEST_DENIED,
                    targetUsers = listOf(userId),
                    actionUrl = "request/$itemId",
                    metadata = mapOf(
                        "itemId" to itemId,
                        "itemName" to itemName,
                        "denialReason" to denialReason,
                        "nextSteps" to nextSteps
                    )
                )
                
                val result = repository.sendPushNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Request denied notification sent to user: $userId")
                } else {
                    Log.e(TAG, "Failed to send request denied notification: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending request denied notification", e)
            }
        }
    }
    
    /**
     * Send notification when an item is marked as ready for donation
     * Requirements: 6.12
     */
    fun sendDonationReadyNotification(
        userId: String,
        itemId: String,
        itemName: String,
        reportedDate: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = PushNotification(
                    title = "Item Ready for Donation",
                    body = "Your reported item '$itemName' from $reportedDate is being prepared for donation. Contact us if you still need it.",
                    type = NotificationType.DONATION_NOTICE,
                    targetUsers = listOf(userId),
                    actionUrl = "item/$itemId",
                    metadata = mapOf(
                        "itemId" to itemId,
                        "itemName" to itemName,
                        "reportedDate" to reportedDate
                    )
                )
                
                val result = repository.sendPushNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Donation ready notification sent to user: $userId")
                } else {
                    Log.e(TAG, "Failed to send donation ready notification: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending donation ready notification", e)
            }
        }
    }
    
    /**
     * Send notification when an item is donated
     */
    fun sendItemDonatedNotification(
        userId: String,
        itemId: String,
        itemName: String,
        donationRecipient: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = PushNotification(
                    title = "Item Donated",
                    body = "Your reported item '$itemName' has been donated to $donationRecipient. Thank you for your contribution!",
                    type = NotificationType.DONATION_NOTICE,
                    targetUsers = listOf(userId),
                    actionUrl = "item/$itemId",
                    metadata = mapOf(
                        "itemId" to itemId,
                        "itemName" to itemName,
                        "donationRecipient" to donationRecipient
                    )
                )
                
                val result = repository.sendPushNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Item donated notification sent to user: $userId")
                } else {
                    Log.e(TAG, "Failed to send item donated notification: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending item donated notification", e)
            }
        }
    }
    
    /**
     * Send security alert notification to admins
     */
    fun sendSecurityAlertNotification(
        title: String,
        message: String,
        severity: String = "HIGH"
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = PushNotification(
                    title = "Security Alert: $title",
                    body = message,
                    type = NotificationType.SECURITY_ALERT,
                    targetRoles = listOf(UserRole.ADMIN, UserRole.SECURITY),
                    metadata = mapOf(
                        "severity" to severity,
                        "timestamp" to System.currentTimeMillis().toString()
                    )
                )
                
                val result = repository.sendPushNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Security alert notification sent to admins")
                } else {
                    Log.e(TAG, "Failed to send security alert notification: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending security alert notification", e)
            }
        }
    }
    
    /**
     * Send system announcement to all users or specific roles
     */
    fun sendSystemAnnouncement(
        title: String,
        message: String,
        targetRoles: List<UserRole> = emptyList(),
        actionUrl: String = ""
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = PushNotification(
                    title = title,
                    body = message,
                    type = NotificationType.SYSTEM_ANNOUNCEMENT,
                    targetUsers = if (targetRoles.isEmpty()) listOf("ALL") else emptyList(),
                    targetRoles = targetRoles,
                    actionUrl = actionUrl,
                    metadata = mapOf(
                        "timestamp" to System.currentTimeMillis().toString()
                    )
                )
                
                val result = repository.sendPushNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "System announcement sent successfully")
                } else {
                    Log.e(TAG, "Failed to send system announcement: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending system announcement", e)
            }
        }
    }
    
    /**
     * Send notification when user account is blocked
     */
    fun sendAccountBlockedNotification(
        userId: String,
        reason: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = PushNotification(
                    title = "Account Blocked",
                    body = "Your account has been blocked. Reason: $reason. Please contact support for assistance.",
                    type = NotificationType.SECURITY_ALERT,
                    targetUsers = listOf(userId),
                    metadata = mapOf(
                        "reason" to reason,
                        "timestamp" to System.currentTimeMillis().toString()
                    )
                )
                
                val result = repository.sendPushNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Account blocked notification sent to user: $userId")
                } else {
                    Log.e(TAG, "Failed to send account blocked notification: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending account blocked notification", e)
            }
        }
    }
    
    /**
     * Send notification when user account is unblocked
     */
    fun sendAccountUnblockedNotification(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = PushNotification(
                    title = "Account Restored",
                    body = "Your account has been unblocked. You can now access all features again.",
                    type = NotificationType.SYSTEM_ANNOUNCEMENT,
                    targetUsers = listOf(userId),
                    metadata = mapOf(
                        "timestamp" to System.currentTimeMillis().toString()
                    )
                )
                
                val result = repository.sendPushNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Account unblocked notification sent to user: $userId")
                } else {
                    Log.e(TAG, "Failed to send account unblocked notification: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending account unblocked notification", e)
            }
        }
    }
}
