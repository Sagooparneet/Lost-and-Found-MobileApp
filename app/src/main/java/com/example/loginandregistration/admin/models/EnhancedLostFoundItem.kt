package com.example.loginandregistration.admin.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.io.Serializable

/**
 * Enhanced LostFoundItem model with status tracking and donation workflow
 * Requirements: 2.1, 2.4, 3.1
 */
data class EnhancedLostFoundItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val contactInfo: String = "",
    
    // Map Firestore "lost found" field to isLost property
    @get:PropertyName("lost found")
    @set:PropertyName("lost found")
    var isLost: Boolean = true,
    
    val userId: String = "",
    val userEmail: String = "",
    val imageUrl: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val category: String = "",
    // Status tracking fields
    val status: ItemStatus = ItemStatus.ACTIVE,
    val statusHistory: List<StatusChange> = emptyList(),
    val requestedBy: String = "",
    val requestedAt: Long = 0,
    val returnedAt: Long = 0,
    val donationEligibleAt: Long = 0,
    val donatedAt: Long = 0,
    val lastModifiedBy: String = "",
    val lastModifiedAt: Long = 0
) : Serializable {
    
    /**
     * Validates if the item data is complete and valid
     */
    fun isValid(): Boolean {
        return id.isNotBlank() && 
               name.isNotBlank() && 
               userId.isNotBlank() &&
               category.isNotBlank()
    }

    /**
     * Checks if the item is eligible for donation (1 year old)
     * Requirements: 3.1
     */
    fun isEligibleForDonation(): Boolean {
        return try {
            val oneYearInMillis = 365L * 24 * 60 * 60 * 1000
            val timestampMillis = timestamp.toDate().time
            
            // Validate timestamp is not in the future or invalid
            if (timestampMillis > System.currentTimeMillis() || timestampMillis <= 0) {
                android.util.Log.w("EnhancedLostFoundItem", "Invalid timestamp for item $id: $timestampMillis")
                return false
            }
            
            val itemAge = System.currentTimeMillis() - timestampMillis
            itemAge >= oneYearInMillis && status == ItemStatus.ACTIVE
        } catch (e: Exception) {
            android.util.Log.e("EnhancedLostFoundItem", "Error checking donation eligibility for item $id", e)
            false
        }
    }

    /**
     * Gets the age of the item in days
     * Returns -1 if timestamp is invalid
     * Requirements: 3.1
     */
    fun getAgeInDays(): Long {
        return try {
            val timestampMillis = timestamp.toDate().time
            
            // Validate timestamp is not in the future or invalid
            if (timestampMillis > System.currentTimeMillis() || timestampMillis <= 0) {
                android.util.Log.w("EnhancedLostFoundItem", "Invalid timestamp for item $id: $timestampMillis")
                return -1
            }
            
            val ageInMillis = System.currentTimeMillis() - timestampMillis
            ageInMillis / (24 * 60 * 60 * 1000)
        } catch (e: Exception) {
            android.util.Log.e("EnhancedLostFoundItem", "Error calculating age for item $id", e)
            -1
        }
    }

    /**
     * Converts to map for Firestore updates
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "location" to location,
            "contactInfo" to contactInfo,
            "isLost" to isLost,
            "userId" to userId,
            "userEmail" to userEmail,
            "imageUrl" to imageUrl,
            "timestamp" to timestamp,
            "category" to category,
            "status" to status.name,
            "statusHistory" to statusHistory.map { it.toMap() },
            "requestedBy" to requestedBy,
            "requestedAt" to requestedAt,
            "returnedAt" to returnedAt,
            "donationEligibleAt" to donationEligibleAt,
            "donatedAt" to donatedAt,
            "lastModifiedBy" to lastModifiedBy,
            "lastModifiedAt" to lastModifiedAt
        )
    }
}

/**
 * Item status enum representing the lifecycle of an item
 * Requirements: 2.1, 3.1
 */
enum class ItemStatus {
    ACTIVE,           // Lost or Found, visible to users
    REQUESTED,        // User has requested the item
    RETURNED,         // Item returned to owner
    DONATION_PENDING, // Eligible for donation (1 year old)
    DONATION_READY,   // Admin marked as ready for donation
    DONATED;          // Final status - donated

    /**
     * Checks if the status allows user visibility
     */
    fun isVisibleToUsers(): Boolean {
        return this == ACTIVE || this == REQUESTED
    }

    /**
     * Checks if the status is in donation workflow
     */
    fun isDonationStatus(): Boolean {
        return this == DONATION_PENDING || this == DONATION_READY || this == DONATED
    }
}

/**
 * Status change record for tracking item history
 * Requirements: 2.3
 */
data class StatusChange(
    val previousStatus: ItemStatus = ItemStatus.ACTIVE,
    val newStatus: ItemStatus = ItemStatus.ACTIVE,
    val changedBy: String = "",
    val changedAt: Long = System.currentTimeMillis(),
    val reason: String = ""
) : Serializable {
    
    /**
     * Validates if the status change is valid
     */
    fun isValid(): Boolean {
        return changedBy.isNotBlank() && changedAt > 0
    }

    /**
     * Converts to map for Firestore storage
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "previousStatus" to previousStatus.name,
            "newStatus" to newStatus.name,
            "changedBy" to changedBy,
            "changedAt" to changedAt,
            "reason" to reason
        )
    }
}
