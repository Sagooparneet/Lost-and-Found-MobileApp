package com.example.loginandregistration.admin.models

import java.io.Serializable

/**
 * Donation item model for managing donation workflow
 * Requirements: 3.1, 3.2, 3.5
 */
data class DonationItem(
    val itemId: String = "",
    val itemName: String = "",
    val category: String = "",
    val location: String = "",
    val reportedAt: Long = 0,
    val eligibleAt: Long = 0,
    val status: DonationStatus = DonationStatus.PENDING,
    val markedReadyBy: String = "",
    val markedReadyAt: Long = 0,
    val donatedAt: Long = 0,
    val donatedBy: String = "",
    val estimatedValue: Double = 0.0,
    val donationRecipient: String = "",
    val imageUrl: String = "",
    val description: String = ""
) : Serializable {
    
    /**
     * Validates if the donation item data is complete
     */
    fun isValid(): Boolean {
        return itemId.isNotBlank() && 
               itemName.isNotBlank() && 
               category.isNotBlank()
    }

    /**
     * Gets the age of the item in days since it was reported
     */
    fun getAgeInDays(): Long {
        val ageInMillis = System.currentTimeMillis() - reportedAt
        return ageInMillis / (24 * 60 * 60 * 1000)
    }

    /**
     * Checks if the item is ready for donation
     */
    fun isReadyForDonation(): Boolean {
        return status == DonationStatus.READY
    }

    /**
     * Converts to map for Firestore updates
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "itemId" to itemId,
            "itemName" to itemName,
            "category" to category,
            "location" to location,
            "reportedAt" to reportedAt,
            "eligibleAt" to eligibleAt,
            "status" to status.name,
            "markedReadyBy" to markedReadyBy,
            "markedReadyAt" to markedReadyAt,
            "donatedAt" to donatedAt,
            "donatedBy" to donatedBy,
            "estimatedValue" to estimatedValue,
            "donationRecipient" to donationRecipient,
            "imageUrl" to imageUrl,
            "description" to description
        )
    }
}

/**
 * Donation status enum
 * Requirements: 3.1, 3.2
 */
enum class DonationStatus {
    PENDING,    // Eligible but not reviewed
    READY,      // Admin marked as ready
    DONATED;    // Final status

    /**
     * Checks if the status allows marking as ready
     */
    fun canMarkReady(): Boolean {
        return this == PENDING
    }

    /**
     * Checks if the status allows marking as donated
     */
    fun canMarkDonated(): Boolean {
        return this == READY
    }
}

/**
 * Donation statistics model
 * Requirements: 3.5, 3.6, 3.7
 */
data class DonationStats(
    val totalDonated: Int = 0,
    val totalValue: Double = 0.0,
    val donationsByCategory: Map<String, Int> = emptyMap(),
    val donationsByMonth: Map<String, Int> = emptyMap(),
    val pendingDonations: Int = 0,
    val readyForDonation: Int = 0,
    val averageItemAge: Float = 0f,
    val mostDonatedCategory: String = ""
) {
    
    /**
     * Calculates the donation rate (donated / total eligible)
     */
    fun getDonationRate(): Float {
        val totalEligible = totalDonated + pendingDonations + readyForDonation
        return if (totalEligible > 0) {
            (totalDonated.toFloat() / totalEligible) * 100
        } else {
            0f
        }
    }

    /**
     * Gets the average value per donated item
     */
    fun getAverageValuePerItem(): Double {
        return if (totalDonated > 0) {
            totalValue / totalDonated
        } else {
            0.0
        }
    }
}

/**
 * Donation queue filter model
 * Requirements: 3.9
 */
data class DonationFilter(
    val category: String? = null,
    val minAge: Long? = null, // Minimum age in days
    val maxAge: Long? = null, // Maximum age in days
    val location: String? = null,
    val status: DonationStatus? = null
) : Serializable {
    
    /**
     * Checks if any filter is active
     */
    fun hasActiveFilters(): Boolean {
        return category != null || 
               minAge != null || 
               maxAge != null || 
               location != null || 
               status != null
    }
    
    /**
     * Applies the filter to a donation item
     */
    fun matches(item: DonationItem): Boolean {
        // Check category filter
        if (category != null && !item.category.equals(category, ignoreCase = true)) {
            return false
        }
        
        // Check age range filter
        val itemAgeInDays = item.getAgeInDays()
        if (minAge != null && itemAgeInDays < minAge) {
            return false
        }
        if (maxAge != null && itemAgeInDays > maxAge) {
            return false
        }
        
        // Check location filter
        if (location != null && !item.location.contains(location, ignoreCase = true)) {
            return false
        }
        
        // Check status filter
        if (status != null && item.status != status) {
            return false
        }
        
        return true
    }
    
    /**
     * Creates a copy with updated category
     */
    fun withCategory(category: String?): DonationFilter {
        return copy(category = category)
    }
    
    /**
     * Creates a copy with updated age range
     */
    fun withAgeRange(minAge: Long?, maxAge: Long?): DonationFilter {
        return copy(minAge = minAge, maxAge = maxAge)
    }
    
    /**
     * Creates a copy with updated location
     */
    fun withLocation(location: String?): DonationFilter {
        return copy(location = location)
    }
    
    /**
     * Creates a copy with updated status
     */
    fun withStatus(status: DonationStatus?): DonationFilter {
        return copy(status = status)
    }
    
    /**
     * Clears all filters
     */
    fun clear(): DonationFilter {
        return DonationFilter()
    }
    
    companion object {
        /**
         * Creates a filter for pending donations only
         */
        fun pendingOnly(): DonationFilter {
            return DonationFilter(status = DonationStatus.PENDING)
        }
        
        /**
         * Creates a filter for ready donations only
         */
        fun readyOnly(): DonationFilter {
            return DonationFilter(status = DonationStatus.READY)
        }
        
        /**
         * Creates a filter for donated items only
         */
        fun donatedOnly(): DonationFilter {
            return DonationFilter(status = DonationStatus.DONATED)
        }
        
        /**
         * Creates a filter for items older than specified days
         */
        fun olderThan(days: Long): DonationFilter {
            return DonationFilter(minAge = days)
        }
    }
}
