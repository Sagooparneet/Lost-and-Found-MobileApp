package com.example.loginandregistration.admin.models

import java.io.Serializable



/**
 * Push notification model for managing notifications
 * Requirements: 6.1, 6.2, 6.3, 6.9
 */
data class PushNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: NotificationType = NotificationType.CUSTOM_ADMIN,
    val targetUsers: List<String> = emptyList(), // User IDs or "ALL"
    val targetRoles: List<UserRole> = emptyList(),
    val actionUrl: String = "",
    val imageUrl: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val scheduledFor: Long = 0,
    val sentAt: Long = 0,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.PENDING,
    val deliveredCount: Int = 0,
    val openedCount: Int = 0,
    val failedCount: Int = 0,
    val metadata: Map<String, String> = emptyMap()
) : Serializable {
    
    /**
     * Validates if the notification data is complete
     */
    fun isValid(): Boolean {
        return title.isNotBlank() && 
               body.isNotBlank() && 
               createdBy.isNotBlank() &&
               (targetUsers.isNotEmpty() || targetRoles.isNotEmpty())
    }

    /**
     * Checks if the notification is scheduled for future delivery
     */
    fun isScheduled(): Boolean {
        return scheduledFor > System.currentTimeMillis()
    }

    /**
     * Checks if the notification has been sent
     */
    fun isSent(): Boolean {
        return deliveryStatus == DeliveryStatus.SENT || 
               deliveryStatus == DeliveryStatus.PARTIALLY_SENT
    }

    /**
     * Calculates the open rate percentage
     */
    fun getOpenRate(): Float {
        return if (deliveredCount > 0) {
            (openedCount.toFloat() / deliveredCount) * 100
        } else {
            0f
        }
    }

    /**
     * Calculates the delivery success rate
     */
    fun getDeliveryRate(): Float {
        val totalAttempts = deliveredCount + failedCount
        return if (totalAttempts > 0) {
            (deliveredCount.toFloat() / totalAttempts) * 100
        } else {
            0f
        }
    }

    /**
     * Converts to map for Firestore storage
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "body" to body,
            "type" to type.name,
            "targetUsers" to targetUsers,
            "targetRoles" to targetRoles.map { it.name },
            "actionUrl" to actionUrl,
            "imageUrl" to imageUrl,
            "createdBy" to createdBy,
            "createdAt" to createdAt,
            "scheduledFor" to scheduledFor,
            "sentAt" to sentAt,
            "deliveryStatus" to deliveryStatus.name,
            "deliveredCount" to deliveredCount,
            "openedCount" to openedCount,
            "failedCount" to failedCount,
            "metadata" to metadata
        )
    }
}

/**
 * Notification type enum
 * Requirements: 6.1, 6.2, 6.3
 */
enum class NotificationType {
    ITEM_MATCH,           // Auto: Item matches user's lost item
    REQUEST_APPROVED,     // Auto: Request approved by security
    REQUEST_DENIED,       // Auto: Request denied
    DONATION_NOTICE,      // Auto: Item marked for donation
    CUSTOM_ADMIN,         // Manual: Admin custom message
    SYSTEM_ANNOUNCEMENT,  // Manual: System-wide announcement
    SECURITY_ALERT;       // Auto: Security-related alerts

    /**
     * Checks if this is an automated notification type
     */
    fun isAutomatic(): Boolean {
        return this in listOf(
            ITEM_MATCH, REQUEST_APPROVED, REQUEST_DENIED,
            DONATION_NOTICE, SECURITY_ALERT
        )
    }

    /**
     * Gets a human-readable description
     */
    fun getDisplayName(): String {
        return when (this) {
            ITEM_MATCH -> "Item Match"
            REQUEST_APPROVED -> "Request Approved"
            REQUEST_DENIED -> "Request Denied"
            DONATION_NOTICE -> "Donation Notice"
            CUSTOM_ADMIN -> "Admin Message"
            SYSTEM_ANNOUNCEMENT -> "System Announcement"
            SECURITY_ALERT -> "Security Alert"
        }
    }
}

/**
 * Delivery status enum
 * Requirements: 6.9
 */
enum class DeliveryStatus {
    PENDING,
    SCHEDULED,
    SENDING,
    SENT,
    FAILED,
    PARTIALLY_SENT;

    /**
     * Checks if the status indicates completion
     */
    fun isComplete(): Boolean {
        return this in listOf(SENT, FAILED, PARTIALLY_SENT)
    }

    /**
     * Gets a human-readable description
     */
    fun getDisplayName(): String {
        return when (this) {
            PENDING -> "Pending"
            SCHEDULED -> "Scheduled"
            SENDING -> "Sending"
            SENT -> "Sent"
            FAILED -> "Failed"
            PARTIALLY_SENT -> "Partially Sent"
        }
    }
}

/**
 * Notification history record for tracking individual deliveries
 * Requirements: 6.9
 */
data class NotificationHistory(
    val id: String = "",
    val notificationId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val deliveredAt: Long = 0,
    val openedAt: Long = 0,
    val isOpened: Boolean = false,
    val deviceToken: String = "",
    val errorMessage: String = ""
) : Serializable {
    
    /**
     * Validates if the history record is complete
     */
    fun isValid(): Boolean {
        return notificationId.isNotBlank() && userId.isNotBlank()
    }

    /**
     * Checks if the notification was successfully delivered
     */
    fun wasDelivered(): Boolean {
        return deliveredAt > 0 && errorMessage.isBlank()
    }

    /**
     * Converts to map for Firestore storage
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "notificationId" to notificationId,
            "userId" to userId,
            "userEmail" to userEmail,
            "deliveredAt" to deliveredAt,
            "openedAt" to openedAt,
            "isOpened" to isOpened,
            "deviceToken" to deviceToken,
            "errorMessage" to errorMessage
        )
    }
}

/**
 * Notification statistics model
 * Requirements: 6.9
 */
data class NotificationStats(
    val totalSent: Int = 0,
    val delivered: Int = 0,
    val opened: Int = 0,
    val failed: Int = 0,
    val pending: Int = 0
) {
    
    /**
     * Calculates the open rate percentage
     */
    fun getOpenRate(): Float {
        return if (delivered > 0) {
            (opened.toFloat() / delivered) * 100
        } else {
            0f
        }
    }

    /**
     * Calculates the delivery success rate
     */
    fun getDeliveryRate(): Float {
        return if (totalSent > 0) {
            (delivered.toFloat() / totalSent) * 100
        } else {
            0f
        }
    }

    /**
     * Calculates the failure rate
     */
    fun getFailureRate(): Float {
        return if (totalSent > 0) {
            (failed.toFloat() / totalSent) * 100
        } else {
            0f
        }
    }
}
