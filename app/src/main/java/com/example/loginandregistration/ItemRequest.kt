package com.example.loginandregistration

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class ItemRequest(
    val requestId: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val reason: String = "",
    val status: String = RequestStatus.PENDING,
    val requestDate: Timestamp = Timestamp.now(),
    val reviewedBy: String = "",
    val reviewDate: Timestamp? = null,
    val reviewNotes: String = ""
) : Serializable {
    // Constructor for compatibility with Firestore
    constructor() : this("", "", "", "", "", "", "", RequestStatus.PENDING, Timestamp.now(), "", null, "")
    
    object RequestStatus {
        const val PENDING = "Pending"
        const val APPROVED = "Approved"
        const val REJECTED = "Rejected"
    }
}
