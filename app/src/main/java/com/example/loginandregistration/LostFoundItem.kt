package com.example.loginandregistration

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class LostFoundItem(
    var id: String = "", // Important: To hold the Firestore document ID for updates
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val contactInfo: String = "",
    val category: String = "",
    val dateLostFound: Timestamp? = null,
    
    // Map Firestore "lost found" field to isLost property
    @get:PropertyName("lost found")
    @set:PropertyName("lost found")
    var isLost: Boolean = true, // true for lost, false for found

    var status: String = "Pending", // Add this field with a default value
    val approvedBy: String = "",
    val approvalDate: Timestamp? = null,
    val userId: String = "",
    val userEmail: String = "",
    val imageUrl: String? = null,
    val timestamp: Timestamp = Timestamp.now()
) : Serializable {
    // Constructor for compatibility with Firestore
    constructor() : this("", "", "", "", "", "", null, true, "", "", null, "", "", null, Timestamp.now())
}
