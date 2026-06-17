package com.example.loginandregistration.admin.models

import com.google.firebase.firestore.IgnoreExtraProperties
import java.io.Serializable

/**
 * Activity log model for comprehensive system audit trail
 * Requirements: 5.1, 5.2
 */
@IgnoreExtraProperties
data class ActivityLog(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val actorId: String = "",
    val actorEmail: String = "",
    val actorRole: UserRole = UserRole.STUDENT,
    val actionType: ActionType = ActionType.USER_LOGIN,
    val targetType: TargetType = TargetType.SYSTEM,
    val targetId: String = "",
    val description: String = "",
    val previousValue: String = "",
    val newValue: String = "",
    val ipAddress: String = "",
    val deviceInfo: String = "",
    val metadata: Map<String, String> = emptyMap()
) : Serializable {
    
    /**
     * Validates if the activity log data is complete
     */
    fun isValid(): Boolean {
        return actorId.isNotBlank() && 
               actorEmail.isNotBlank() && 
               description.isNotBlank()
    }

    /**
     * Checks if this is an admin action
     */
    fun isAdminAction(): Boolean {
        return actionType.isAdminAction()
    }

    /**
     * Checks if this is a system event
     */
    fun isSystemEvent(): Boolean {
        return actionType.isSystemEvent()
    }

    /**
     * Converts to map for Firestore storage
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "timestamp" to timestamp,
            "actorId" to actorId,
            "actorEmail" to actorEmail,
            "actorRole" to actorRole.name,
            "actionType" to actionType.name,
            "targetType" to targetType.name,
            "targetId" to targetId,
            "description" to description,
            "previousValue" to previousValue,
            "newValue" to newValue,
            "ipAddress" to ipAddress,
            "deviceInfo" to deviceInfo,
            "metadata" to metadata
        )
    }
}

/**
 * Action type enum for categorizing activities
 * Requirements: 5.1, 5.2, 5.7, 5.8, 5.9, 5.10
 */
enum class ActionType {
    // User actions
    USER_LOGIN,
    USER_LOGOUT,
    USER_REGISTER,
    ITEM_REPORT,
    ITEM_REQUEST,
    ITEM_CLAIM,
    
    // Admin actions
    USER_BLOCK,
    USER_UNBLOCK,
    USER_ROLE_CHANGE,
    USER_EDIT,
    USER_DELETE,
    ITEM_EDIT,
    ITEM_STATUS_CHANGE,
    ITEM_DELETE,
    DONATION_MARK_READY,
    DONATION_COMPLETE,
    NOTIFICATION_SEND,
    DATA_EXPORT,
    
    // System events
    SYSTEM_MAINTENANCE,
    SYSTEM_ERROR,
    AUTO_DONATION_FLAG,
    LOG_ARCHIVE;

    /**
     * Checks if this action type is an admin action
     */
    fun isAdminAction(): Boolean {
        return this in listOf(
            USER_BLOCK, USER_UNBLOCK, USER_ROLE_CHANGE, USER_EDIT, USER_DELETE,
            ITEM_EDIT, ITEM_STATUS_CHANGE, ITEM_DELETE,
            DONATION_MARK_READY, DONATION_COMPLETE,
            NOTIFICATION_SEND, DATA_EXPORT
        )
    }

    /**
     * Checks if this action type is a system event
     */
    fun isSystemEvent(): Boolean {
        return this in listOf(
            SYSTEM_MAINTENANCE, SYSTEM_ERROR, 
            AUTO_DONATION_FLAG, LOG_ARCHIVE
        )
    }

    /**
     * Gets a human-readable description of the action
     */
    fun getDisplayName(): String {
        return when (this) {
            USER_LOGIN -> "User Login"
            USER_LOGOUT -> "User Logout"
            USER_REGISTER -> "User Registration"
            ITEM_REPORT -> "Item Reported"
            ITEM_REQUEST -> "Item Requested"
            ITEM_CLAIM -> "Item Claimed"
            USER_BLOCK -> "User Blocked"
            USER_UNBLOCK -> "User Unblocked"
            USER_ROLE_CHANGE -> "User Role Changed"
            USER_EDIT -> "User Edited"
            USER_DELETE -> "User Deleted"
            ITEM_EDIT -> "Item Edited"
            ITEM_STATUS_CHANGE -> "Item Status Changed"
            ITEM_DELETE -> "Item Deleted"
            DONATION_MARK_READY -> "Marked Ready for Donation"
            DONATION_COMPLETE -> "Donation Completed"
            NOTIFICATION_SEND -> "Notification Sent"
            DATA_EXPORT -> "Data Exported"
            SYSTEM_MAINTENANCE -> "System Maintenance"
            SYSTEM_ERROR -> "System Error"
            AUTO_DONATION_FLAG -> "Auto-flagged for Donation"
            LOG_ARCHIVE -> "Logs Archived"
        }
    }
}

/**
 * Target type enum for identifying the entity affected by an action
 * Requirements: 5.1, 5.2
 */
enum class TargetType {
    USER,
    ITEM,
    DONATION,
    NOTIFICATION,
    SYSTEM;

    /**
     * Gets a human-readable description of the target type
     */
    fun getDisplayName(): String {
        return when (this) {
            USER -> "User"
            ITEM -> "Item"
            DONATION -> "Donation"
            NOTIFICATION -> "Notification"
            SYSTEM -> "System"
        }
    }
}
