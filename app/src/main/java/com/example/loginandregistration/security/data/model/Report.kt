package com.example.loginandregistration.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Report(
    var id: String = "",
    val itemName: String = "",
    val description: String = "",    val location: String = "",
    val status: String = "Pending",
    val imageUrl: String? = null,
    @ServerTimestamp val timestamp: Date? = null,
    val category: String = "",
    val userRole: String = "",

    val userEmail: String = "" // To display the reporter's email
)

