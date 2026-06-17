package com.example.loginandregistration.admin.utils

import com.example.loginandregistration.admin.models.EnhancedLostFoundItem
import com.example.loginandregistration.admin.models.ItemStatus
import com.example.loginandregistration.admin.models.StatusChange
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for managing item status history
 * Requirements: 2.3
 */
object StatusHistoryUtils {
    
    /**
     * Adds a new status change to an item's history
     * Requirements: 2.3
     * 
     * @param item The item to add status change to
     * @param newStatus The new status to transition to
     * @param changedBy User ID of who made the change
     * @param reason Optional reason for the status change
     * @return Updated item with new status change in history
     */
    fun addStatusChange(
        item: EnhancedLostFoundItem,
        newStatus: ItemStatus,
        changedBy: String,
        reason: String = ""
    ): EnhancedLostFoundItem {
        // Validate the status change
        if (!isValidStatusChange(item.status, newStatus)) {
            throw IllegalArgumentException(
                "Invalid status transition from ${item.status} to $newStatus"
            )
        }
        
        // Create new status change record
        val statusChange = StatusChange(
            previousStatus = item.status,
            newStatus = newStatus,
            changedBy = changedBy,
            changedAt = System.currentTimeMillis(),
            reason = reason
        )
        
        // Validate the status change record
        if (!statusChange.isValid()) {
            throw IllegalArgumentException("Invalid status change record")
        }
        
        // Add to history
        val updatedHistory = item.statusHistory.toMutableList().apply {
            add(statusChange)
        }
        
        // Return updated item
        return item.copy(
            status = newStatus,
            statusHistory = updatedHistory,
            lastModifiedAt = System.currentTimeMillis(),
            lastModifiedBy = changedBy
        )
    }
    
    /**
     * Validates if a status change is allowed
     * Requirements: 2.3
     * 
     * @param currentStatus The current status
     * @param newStatus The desired new status
     * @return true if the transition is valid, false otherwise
     */
    fun isValidStatusChange(currentStatus: ItemStatus, newStatus: ItemStatus): Boolean {
        // Same status is not a valid change
        if (currentStatus == newStatus) {
            return false
        }
        
        return when (currentStatus) {
            ItemStatus.ACTIVE -> newStatus in listOf(
                ItemStatus.REQUESTED,
                ItemStatus.RETURNED,
                ItemStatus.DONATION_PENDING
            )
            ItemStatus.REQUESTED -> newStatus in listOf(
                ItemStatus.RETURNED,
                ItemStatus.ACTIVE
            )
            ItemStatus.RETURNED -> false // Final state - no transitions allowed
            ItemStatus.DONATION_PENDING -> newStatus in listOf(
                ItemStatus.DONATION_READY,
                ItemStatus.ACTIVE // Allow reverting if marked by mistake
            )
            ItemStatus.DONATION_READY -> newStatus in listOf(
                ItemStatus.DONATED,
                ItemStatus.DONATION_PENDING // Allow reverting if not ready yet
            )
            ItemStatus.DONATED -> false // Final state - no transitions allowed
        }
    }
    
    /**
     * Gets all valid next statuses for a given current status
     * 
     * @param currentStatus The current status
     * @return List of valid next statuses
     */
    fun getValidNextStatuses(currentStatus: ItemStatus): List<ItemStatus> {
        return when (currentStatus) {
            ItemStatus.ACTIVE -> listOf(
                ItemStatus.REQUESTED,
                ItemStatus.RETURNED,
                ItemStatus.DONATION_PENDING
            )
            ItemStatus.REQUESTED -> listOf(
                ItemStatus.RETURNED,
                ItemStatus.ACTIVE
            )
            ItemStatus.RETURNED -> emptyList()
            ItemStatus.DONATION_PENDING -> listOf(
                ItemStatus.DONATION_READY,
                ItemStatus.ACTIVE
            )
            ItemStatus.DONATION_READY -> listOf(
                ItemStatus.DONATED,
                ItemStatus.DONATION_PENDING
            )
            ItemStatus.DONATED -> emptyList()
        }
    }
    
    /**
     * Formats status history for display
     * Requirements: 2.3
     * 
     * @param statusHistory List of status changes
     * @return Formatted string representation of the history
     */
    fun formatStatusHistory(statusHistory: List<StatusChange>): String {
        if (statusHistory.isEmpty()) {
            return "No status changes recorded"
        }
        
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        
        return statusHistory.joinToString("\n\n") { change ->
            buildString {
                append("${change.previousStatus.getDisplayName()} → ${change.newStatus.getDisplayName()}")
                append("\n")
                append("Changed: ${dateFormat.format(Date(change.changedAt))}")
                append("\n")
                append("By: ${change.changedBy}")
                if (change.reason.isNotBlank()) {
                    append("\n")
                    append("Reason: ${change.reason}")
                }
            }
        }
    }
    
    /**
     * Formats status history as a list of formatted entries
     * 
     * @param statusHistory List of status changes
     * @return List of formatted status change entries
     */
    fun formatStatusHistoryList(statusHistory: List<StatusChange>): List<StatusHistoryEntry> {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        return statusHistory.map { change ->
            StatusHistoryEntry(
                previousStatus = change.previousStatus.getDisplayName(),
                newStatus = change.newStatus.getDisplayName(),
                date = dateFormat.format(Date(change.changedAt)),
                time = timeFormat.format(Date(change.changedAt)),
                changedBy = change.changedBy,
                reason = change.reason,
                timestamp = change.changedAt
            )
        }.sortedByDescending { it.timestamp }
    }
    
    /**
     * Gets a summary of the most recent status change
     * 
     * @param statusHistory List of status changes
     * @return Summary string or null if no history
     */
    fun getRecentStatusChangeSummary(statusHistory: List<StatusChange>): String? {
        val mostRecent = statusHistory.maxByOrNull { it.changedAt } ?: return null
        
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = dateFormat.format(Date(mostRecent.changedAt))
        
        return "Changed to ${mostRecent.newStatus.getDisplayName()} on $date"
    }
    
    /**
     * Validates if a status change reason is required
     * 
     * @param currentStatus Current status
     * @param newStatus New status
     * @return true if reason is required
     */
    fun isReasonRequired(currentStatus: ItemStatus, newStatus: ItemStatus): Boolean {
        // Require reason for certain critical transitions
        return when {
            newStatus == ItemStatus.DONATION_PENDING -> true
            newStatus == ItemStatus.DONATED -> true
            currentStatus == ItemStatus.RETURNED && newStatus == ItemStatus.ACTIVE -> true
            else -> false
        }
    }
    
    /**
     * Gets a user-friendly description of a status
     * 
     * @param status The item status
     * @return Display name for the status
     */
    private fun ItemStatus.getDisplayName(): String {
        return when (this) {
            ItemStatus.ACTIVE -> "Active"
            ItemStatus.REQUESTED -> "Requested"
            ItemStatus.RETURNED -> "Returned"
            ItemStatus.DONATION_PENDING -> "Pending Donation"
            ItemStatus.DONATION_READY -> "Ready for Donation"
            ItemStatus.DONATED -> "Donated"
        }
    }
    
    /**
     * Gets a color resource ID for a status (for UI display)
     * 
     * @param status The item status
     * @return Color identifier string
     */
    fun getStatusColor(status: ItemStatus): String {
        return when (status) {
            ItemStatus.ACTIVE -> "#4CAF50" // Green
            ItemStatus.REQUESTED -> "#2196F3" // Blue
            ItemStatus.RETURNED -> "#9C27B0" // Purple
            ItemStatus.DONATION_PENDING -> "#FF9800" // Orange
            ItemStatus.DONATION_READY -> "#FF5722" // Deep Orange
            ItemStatus.DONATED -> "#607D8B" // Blue Grey
        }
    }
    
    /**
     * Gets an icon identifier for a status (for UI display)
     * 
     * @param status The item status
     * @return Icon identifier string
     */
    fun getStatusIcon(status: ItemStatus): String {
        return when (status) {
            ItemStatus.ACTIVE -> "check_circle"
            ItemStatus.REQUESTED -> "pending"
            ItemStatus.RETURNED -> "assignment_turned_in"
            ItemStatus.DONATION_PENDING -> "schedule"
            ItemStatus.DONATION_READY -> "card_giftcard"
            ItemStatus.DONATED -> "volunteer_activism"
        }
    }
}

/**
 * Data class for formatted status history entry
 */
data class StatusHistoryEntry(
    val previousStatus: String,
    val newStatus: String,
    val date: String,
    val time: String,
    val changedBy: String,
    val reason: String,
    val timestamp: Long
)
