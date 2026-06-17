package com.example.loginandregistration

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.io.Serializable

/**
 * Data model representing a notification in the system.
 * Stores notification details including recipient, type, content, and status.
 * Requirements: 11.7
 */
data class Notification(
    @get:PropertyName("notificationId")
    @set:PropertyName("notificationId")
    var notificationId: String = "",
    
    @get:PropertyName("userId")
    @set:PropertyName("userId")
    var userId: String = "",
    
    @get:PropertyName("userEmail")
    @set:PropertyName("userEmail")
    var userEmail: String = "",
    
    @get:PropertyName("type")
    @set:PropertyName("type")
    var type: String = "",
    
    @get:PropertyName("title")
    @set:PropertyName("title")
    var title: String = "",
    
    @get:PropertyName("message")
    @set:PropertyName("message")
    var message: String = "",
    
    @get:PropertyName("itemId")
    @set:PropertyName("itemId")
    var itemId: String = "",
    
    @get:PropertyName("requestId")
    @set:PropertyName("requestId")
    var requestId: String = "",
    
    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Timestamp = Timestamp.now(),
    
    @get:PropertyName("read")
    @set:PropertyName("read")
    var read: Boolean = false,
    
    @get:PropertyName("delivered")
    @set:PropertyName("delivered")
    var delivered: Boolean = false
) : Serializable {
    
    companion object {
        // Notification types
        const val TYPE_FOUND_ITEM_SUBMITTED = "FOUND_ITEM_SUBMITTED"
        const val TYPE_FOUND_ITEM_APPROVED = "FOUND_ITEM_APPROVED"
        const val TYPE_FOUND_ITEM_REJECTED = "FOUND_ITEM_REJECTED"
        const val TYPE_CLAIM_SUBMITTED = "CLAIM_SUBMITTED"
        const val TYPE_CLAIM_APPROVED = "CLAIM_APPROVED"
        const val TYPE_CLAIM_REJECTED = "CLAIM_REJECTED"
    }
}
