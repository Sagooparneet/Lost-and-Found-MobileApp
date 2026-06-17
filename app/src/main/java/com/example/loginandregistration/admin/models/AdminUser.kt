package com.example.loginandregistration.admin.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class AdminUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val role: UserRole = UserRole.STUDENT,
    val isBlocked: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val lastLoginAt: Timestamp? = null,
    val itemsReported: Int = 0,
    val itemsFound: Int = 0,
    val itemsClaimed: Int = 0
)

/**
 * User role enum for role-based access and notifications
 * Requirements: 10.1, 10.2, 10.3, 10.4, 10.5
 * 
 * Simplified to three roles: STUDENT, SECURITY, and ADMIN
 * 
 * Firestore Deserialization Notes:
 * - Firestore automatically deserializes enum values by matching the string in the database
 *   to the enum constant name (case-sensitive)
 * - The fromString() method provides case-insensitive parsing with fallback to STUDENT
 * - For automatic Firestore deserialization to work, database values should be stored
 *   in uppercase (STUDENT, SECURITY, ADMIN) which is done automatically via role.name
 * - Legacy data with old roles (USER, MODERATOR) will be mapped to STUDENT
 */
enum class UserRole(val value: String) {
    STUDENT("STUDENT"),
    SECURITY("SECURITY"),
    ADMIN("ADMIN");
    
    fun getDisplayName(): String {
        return when (this) {
            STUDENT -> "Student"
            SECURITY -> "Security"
            ADMIN -> "Admin"
        }
    }
    
    companion object {
        /**
         * Safely parse role from string with case-insensitive matching and fallback to STUDENT
         * Prevents crashes from unknown role values in database
         * Maps legacy roles (USER, MODERATOR) to STUDENT
         * Requirements: 10.1, 10.2, 10.5
         */
        fun fromString(value: String): UserRole {
            return when (value.uppercase()) {
                "STUDENT" -> STUDENT
                "SECURITY" -> SECURITY
                "ADMIN" -> ADMIN
                // Map legacy roles to STUDENT
                "USER", "MODERATOR" -> {
                    android.util.Log.i("UserRole", "Mapping legacy role '$value' to STUDENT")
                    STUDENT
                }
                else -> {
                    android.util.Log.w("UserRole", "Unknown role: $value, defaulting to STUDENT")
                    STUDENT
                }
            }
        }
    }
}