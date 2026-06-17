package com.example.loginandregistration

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Firestore data model for Report documents.
 * 
 * IMPORTANT RULES FOR FIRESTORE DATA CLASSES:
 * 1. Must have a public no-argument constructor (provided by default values)
 * 2. Property names must match Firestore field names exactly (case-sensitive)
 * 3. Use @PropertyName annotation for fields with invalid Kotlin names (spaces, special chars)
 * 4. All properties should have default values for deserialization
 * 5. Use var (mutable) instead of val for Firestore to set values
 */
data class Report(
    // Basic item information
    var itemName: String = "",
    var reportedBy: String = "",
    var status: String = "",
    
    // User information
    var userId: String = "",
    var name: String = "",
    
    // Contact information
    var contactInfo: String = "",
    
    // Date fields - use Timestamp for Firestore date/time
    var dateLostFound: Timestamp? = null,
    var approvalDate: Timestamp? = null,
    var lastModifiedAt: Timestamp? = null,
    
    // Approval tracking
    var approvedBy: String = "",
    var lastModifiedBy: String = "",
    
    /**
     * Special field with space in Firestore name.
     * Use @PropertyName to map "lost found" field to a valid Kotlin property name.
     * In Firestore: "lost found"
     * In Kotlin: lostFound
     */
    @PropertyName("lost found")
    var lostFound: String = ""
)
