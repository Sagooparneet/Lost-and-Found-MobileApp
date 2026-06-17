package com.example.loginandregistration.utils

/**
 * Utility object for managing user roles and permissions.
 * Provides role-checking methods for admin, security, and sensitive information access.
 * Requirements: 10.1, 10.4, 10.5
 * 
 * Supports three roles: STUDENT, SECURITY, and ADMIN
 */
object UserRoleManager {
    
    /**
     * Checks if the user is an admin based on their role string.
     * Also supports legacy email-based check for backward compatibility.
     * 
     * @param role The user's role string from Firestore
     * @param email The user's email address (optional, for legacy support)
     * @return true if the user is an admin, false otherwise
     */
    fun isAdmin(role: String, email: String = ""): Boolean {
        // Check role field first
        if (role.equals("ADMIN", ignoreCase = true)) {
            return true
        }
        // Legacy email-based check for backward compatibility
        return email.equals("admin@gmail.com", ignoreCase = true)
    }
    
    /**
     * Checks if the user has security role privileges.
     * Admin users also have security privileges.
     * 
     * @param role The user's role string from Firestore
     * @param email The user's email address (optional, for legacy support)
     * @return true if the user is a security officer or admin, false otherwise
     */
    fun isSecurity(role: String, email: String = ""): Boolean {
        // Check role field first
        if (role.equals("SECURITY", ignoreCase = true) || role.equals("ADMIN", ignoreCase = true)) {
            return true
        }
        // Legacy email-based check for backward compatibility
        return email.contains("security", ignoreCase = true) || isAdmin(role, email)
    }
    
    /**
     * Checks if the user is a student (regular user).
     * 
     * @param role The user's role string from Firestore
     * @return true if the user is a student, false otherwise
     */
    fun isStudent(role: String): Boolean {
        return role.equals("STUDENT", ignoreCase = true)
    }
    
    /**
     * Checks if the user can view sensitive information (date, location, contact info).
     * Only admin and security users can view sensitive information.
     * 
     * @param role The user's role string from Firestore
     * @param email The user's email address (optional, for legacy support)
     * @return true if the user can view sensitive information, false otherwise
     */
    fun canViewSensitiveInfo(role: String, email: String = ""): Boolean {
        return isAdmin(role, email) || isSecurity(role, email)
    }
    
    /**
     * Validates if a role string is one of the three valid roles.
     * 
     * @param role The role string to validate
     * @return true if the role is valid (STUDENT, SECURITY, or ADMIN), false otherwise
     */
    fun isValidRole(role: String): Boolean {
        return role.uppercase() in listOf("STUDENT", "SECURITY", "ADMIN")
    }
}
