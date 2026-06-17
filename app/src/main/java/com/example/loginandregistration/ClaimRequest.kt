package com.example.loginandregistration

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class ClaimRequest(
    val requestId: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val reason: String = "",
    val proofDescription: String = "",
    val status: String = ClaimStatus.PENDING,
    val requestDate: Timestamp = Timestamp.now(),
    val reviewedBy: String = "",
    val reviewDate: Timestamp? = null,
    val reviewNotes: String = ""
) : Serializable {
    // Constructor for compatibility with Firestore
    constructor() : this("", "", "", "", "", "", "", "", "", ClaimStatus.PENDING, Timestamp.now(), "", null, "")
    
    object ClaimStatus {
        const val PENDING = "Pending"
        const val APPROVED = "Approved"
        const val REJECTED = "Rejected"
    }
}
