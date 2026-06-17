package com.example.loginandregistration.admin.utils

import android.util.Patterns
import com.example.loginandregistration.admin.models.ItemStatus
import com.example.loginandregistration.admin.models.UserRole

/**
 * Data validation utility for admin operations
 * Requirements: 10.1, 10.4, 10.6
 */
object DataValidator {
    
    /**
     * Validation result class
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList()
    ) {
        fun getErrorMessage(): String = errors.joinToString("\n")
        
        companion object {
            fun success() = ValidationResult(true, emptyList())
            fun failure(vararg errors: String) = ValidationResult(false, errors.toList())
        }
    }
    
    // ========== Input Sanitization ==========
    
    /**
     * Sanitize string input by removing potentially harmful characters
     * Requirements: 10.1
     */
    fun sanitizeString(input: String): String {
        return input
            .trim()
            .replace(Regex("[<>\"'&]"), "") // Remove HTML/XML special characters
            .replace(Regex("\\s+"), " ") // Normalize whitespace
    }
    
    /**
     * Sanitize email input
     * Requirements: 10.1
     */
    fun sanitizeEmail(email: String): String {
        return email.trim().lowercase()
    }
    
    /**
     * Sanitize numeric input
     * Requirements: 10.1
     */
    fun sanitizeNumeric(input: String): String {
        return input.replace(Regex("[^0-9.]"), "")
    }
    
    // ========== Field Validation ==========
    
    /**
     * Validate email format
     * Requirements: 10.4
     */
    fun validateEmail(email: String): ValidationResult {
        val sanitized = sanitizeEmail(email)
        
        return when {
            sanitized.isBlank() -> ValidationResult.failure("Email cannot be empty")
            !Patterns.EMAIL_ADDRESS.matcher(sanitized).matches() -> 
                ValidationResult.failure("Invalid email format")
            sanitized.length > 254 -> 
                ValidationResult.failure("Email is too long (max 254 characters)")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate display name
     * Requirements: 10.4
     */
    fun validateDisplayName(name: String): ValidationResult {
        val sanitized = sanitizeString(name)
        
        return when {
            sanitized.isBlank() -> ValidationResult.failure("Display name cannot be empty")
            sanitized.length < 2 -> 
                ValidationResult.failure("Display name must be at least 2 characters")
            sanitized.length > 50 -> 
                ValidationResult.failure("Display name is too long (max 50 characters)")
            !sanitized.matches(Regex("^[a-zA-Z0-9\\s._-]+$")) -> 
                ValidationResult.failure("Display name contains invalid characters")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate item name
     * Requirements: 10.4
     */
    fun validateItemName(name: String): ValidationResult {
        val sanitized = sanitizeString(name)
        
        return when {
            sanitized.isBlank() -> ValidationResult.failure("Item name cannot be empty")
            sanitized.length < 3 -> 
                ValidationResult.failure("Item name must be at least 3 characters")
            sanitized.length > 100 -> 
                ValidationResult.failure("Item name is too long (max 100 characters)")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate item description
     * Requirements: 10.4
     */
    fun validateDescription(description: String): ValidationResult {
        val sanitized = sanitizeString(description)
        
        return when {
            sanitized.isBlank() -> ValidationResult.failure("Description cannot be empty")
            sanitized.length < 10 -> 
                ValidationResult.failure("Description must be at least 10 characters")
            sanitized.length > 1000 -> 
                ValidationResult.failure("Description is too long (max 1000 characters)")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate location
     * Requirements: 10.4
     */
    fun validateLocation(location: String): ValidationResult {
        val sanitized = sanitizeString(location)
        
        return when {
            sanitized.isBlank() -> ValidationResult.failure("Location cannot be empty")
            sanitized.length < 3 -> 
                ValidationResult.failure("Location must be at least 3 characters")
            sanitized.length > 200 -> 
                ValidationResult.failure("Location is too long (max 200 characters)")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate block reason
     * Requirements: 10.4
     */
    fun validateBlockReason(reason: String): ValidationResult {
        val sanitized = sanitizeString(reason)
        
        return when {
            sanitized.isBlank() -> ValidationResult.failure("Block reason cannot be empty")
            sanitized.length < 10 -> 
                ValidationResult.failure("Block reason must be at least 10 characters")
            sanitized.length > 500 -> 
                ValidationResult.failure("Block reason is too long (max 500 characters)")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate donation recipient
     * Requirements: 10.4
     */
    fun validateDonationRecipient(recipient: String): ValidationResult {
        val sanitized = sanitizeString(recipient)
        
        return when {
            sanitized.isBlank() -> ValidationResult.failure("Recipient cannot be empty")
            sanitized.length < 3 -> 
                ValidationResult.failure("Recipient must be at least 3 characters")
            sanitized.length > 200 -> 
                ValidationResult.failure("Recipient is too long (max 200 characters)")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate donation value
     * Requirements: 10.4
     */
    fun validateDonationValue(value: Double): ValidationResult {
        return when {
            value < 0 -> ValidationResult.failure("Donation value cannot be negative")
            value > 1000000 -> ValidationResult.failure("Donation value is too high")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate notification title
     * Requirements: 10.4
     */
    fun validateNotificationTitle(title: String): ValidationResult {
        val sanitized = sanitizeString(title)
        
        return when {
            sanitized.isBlank() -> ValidationResult.failure("Notification title cannot be empty")
            sanitized.length < 3 -> 
                ValidationResult.failure("Notification title must be at least 3 characters")
            sanitized.length > 100 -> 
                ValidationResult.failure("Notification title is too long (max 100 characters)")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate notification body
     * Requirements: 10.4
     */
    fun validateNotificationBody(body: String): ValidationResult {
        val sanitized = sanitizeString(body)
        
        return when {
            sanitized.isBlank() -> ValidationResult.failure("Notification body cannot be empty")
            sanitized.length < 10 -> 
                ValidationResult.failure("Notification body must be at least 10 characters")
            sanitized.length > 500 -> 
                ValidationResult.failure("Notification body is too long (max 500 characters)")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate URL format
     * Requirements: 10.4
     */
    fun validateUrl(url: String): ValidationResult {
        if (url.isBlank()) {
            return ValidationResult.success() // URL is optional
        }
        
        return when {
            !Patterns.WEB_URL.matcher(url).matches() -> 
                ValidationResult.failure("Invalid URL format")
            url.length > 500 -> 
                ValidationResult.failure("URL is too long (max 500 characters)")
            else -> ValidationResult.success()
        }
    }
    
    // ========== Business Rule Validation ==========
    
    /**
     * Validate user role change
     * Requirements: 10.6
     */
    fun validateRoleChange(currentRole: UserRole, newRole: UserRole): ValidationResult {
        return when {
            currentRole == newRole -> 
                ValidationResult.failure("New role must be different from current role")
            newRole == UserRole.ADMIN && currentRole != UserRole.ADMIN -> 
                ValidationResult.failure("Cannot promote user to ADMIN role")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate item status change
     * Requirements: 10.6
     */
    fun validateStatusChange(currentStatus: ItemStatus, newStatus: ItemStatus): ValidationResult {
        return when {
            currentStatus == newStatus -> 
                ValidationResult.failure("New status must be different from current status")
            currentStatus == ItemStatus.DONATED -> 
                ValidationResult.failure("Cannot change status of donated items")
            newStatus == ItemStatus.DONATED && currentStatus != ItemStatus.DONATION_READY -> 
                ValidationResult.failure("Item must be marked as ready for donation first")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate date range
     * Requirements: 10.6
     */
    fun validateDateRange(startDate: Long, endDate: Long): ValidationResult {
        return when {
            startDate <= 0 -> ValidationResult.failure("Start date is invalid")
            endDate <= 0 -> ValidationResult.failure("End date is invalid")
            startDate > endDate -> 
                ValidationResult.failure("Start date must be before end date")
            endDate > System.currentTimeMillis() -> 
                ValidationResult.failure("End date cannot be in the future")
            (endDate - startDate) > (365L * 24 * 60 * 60 * 1000) -> 
                ValidationResult.failure("Date range cannot exceed 1 year")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate user ID
     * Requirements: 10.4
     */
    fun validateUserId(userId: String): ValidationResult {
        return when {
            userId.isBlank() -> ValidationResult.failure("User ID cannot be empty")
            userId.length < 10 -> ValidationResult.failure("Invalid user ID format")
            userId.length > 128 -> ValidationResult.failure("User ID is too long")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate item ID
     * Requirements: 10.4
     */
    fun validateItemId(itemId: String): ValidationResult {
        return when {
            itemId.isBlank() -> ValidationResult.failure("Item ID cannot be empty")
            itemId.length < 10 -> ValidationResult.failure("Invalid item ID format")
            itemId.length > 128 -> ValidationResult.failure("Item ID is too long")
            else -> ValidationResult.success()
        }
    }
    
    // ========== Composite Validation ==========
    
    /**
     * Validate user update data
     * Requirements: 10.4, 10.6
     */
    fun validateUserUpdate(updates: Map<String, Any>): ValidationResult {
        val errors = mutableListOf<String>()
        
        updates["displayName"]?.let { name ->
            val result = validateDisplayName(name.toString())
            if (!result.isValid) errors.addAll(result.errors)
        }
        
        updates["email"]?.let { email ->
            val result = validateEmail(email.toString())
            if (!result.isValid) errors.addAll(result.errors)
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.success()
        } else {
            ValidationResult(false, errors)
        }
    }
    
    /**
     * Validate item update data
     * Requirements: 10.4, 10.6
     */
    fun validateItemUpdate(updates: Map<String, Any>): ValidationResult {
        val errors = mutableListOf<String>()
        
        updates["name"]?.let { name ->
            val result = validateItemName(name.toString())
            if (!result.isValid) errors.addAll(result.errors)
        }
        
        updates["description"]?.let { desc ->
            val result = validateDescription(desc.toString())
            if (!result.isValid) errors.addAll(result.errors)
        }
        
        updates["location"]?.let { loc ->
            val result = validateLocation(loc.toString())
            if (!result.isValid) errors.addAll(result.errors)
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.success()
        } else {
            ValidationResult(false, errors)
        }
    }
    
    /**
     * Validate donation data
     * Requirements: 10.4, 10.6
     */
    fun validateDonationData(recipient: String, value: Double): ValidationResult {
        val errors = mutableListOf<String>()
        
        val recipientResult = validateDonationRecipient(recipient)
        if (!recipientResult.isValid) errors.addAll(recipientResult.errors)
        
        val valueResult = validateDonationValue(value)
        if (!valueResult.isValid) errors.addAll(valueResult.errors)
        
        return if (errors.isEmpty()) {
            ValidationResult.success()
        } else {
            ValidationResult(false, errors)
        }
    }
    
    /**
     * Validate notification data
     * Requirements: 10.4
     */
    fun validateNotificationData(
        title: String,
        body: String,
        actionUrl: String = ""
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        val titleResult = validateNotificationTitle(title)
        if (!titleResult.isValid) errors.addAll(titleResult.errors)
        
        val bodyResult = validateNotificationBody(body)
        if (!bodyResult.isValid) errors.addAll(bodyResult.errors)
        
        if (actionUrl.isNotBlank()) {
            val urlResult = validateUrl(actionUrl)
            if (!urlResult.isValid) errors.addAll(urlResult.errors)
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.success()
        } else {
            ValidationResult(false, errors)
        }
    }
}
