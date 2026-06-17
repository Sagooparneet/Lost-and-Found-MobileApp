package com.example.loginandregistration

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class User(
    // Primary fields (compatible with AdminUser)
    val uid: String = "",              // Changed from userId
    val email: String = "",
    val displayName: String = "",      // Changed from name
    val photoUrl: String = "",         // New field
    val role: String = "STUDENT",      // Default role - Requirement 10.2
    val isBlocked: Boolean = false,    // New field
    
    // Additional user fields
    val phone: String = "",
    val gender: String = "",
    val fcmToken: String = "",
    
    // Timestamps
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val lastLoginAt: Timestamp? = null,
    
    // Statistics (for admin dashboard)
    val itemsReported: Int = 0,
    val itemsFound: Int = 0,
    val itemsClaimed: Int = 0
) : Serializable {
    // Constructor for compatibility with Firestore
    constructor() : this("", "", "", "", "STUDENT", false, "", "", "", Timestamp.now(), Timestamp.now(), null, 0, 0, 0)
}
