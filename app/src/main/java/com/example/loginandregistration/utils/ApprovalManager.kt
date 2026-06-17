package com.example.loginandregistration.utils

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Utility object for managing approval and rejection of found items.
 * Handles status updates, approval metadata, and notification sending.
 * Requirements: 4.3, 4.4
 */
object ApprovalManager {
    
    private const val TAG = "ApprovalManager"
    private val db = FirebaseFirestore.getInstance()
    
    /**
     * Approves a found item, updating its status and approval metadata.
     * 
     * @param itemId The ID of the item to approve
     * @param approvedByUserId The user ID of the security officer approving the item
     * @return Result indicating success or failure
     */
    suspend fun approveItem(
        itemId: String,
        approvedByUserId: String
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to "Approved",
                "approvedBy" to approvedByUserId,
                "approvalDate" to Timestamp.now()
            )
            
            db.collection("items")
                .document(itemId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Item $itemId approved successfully by $approvedByUserId")
            
            // Send notification to reporting user
            // Requirements: 11.2
            NotificationManager.notifyFoundItemApproved(itemId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error approving item $itemId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Rejects a found item, updating its status and approval metadata.
     * 
     * @param itemId The ID of the item to reject
     * @param rejectedByUserId The user ID of the security officer rejecting the item
     * @param notes Optional notes explaining the rejection reason
     * @return Result indicating success or failure
     */
    suspend fun rejectItem(
        itemId: String,
        rejectedByUserId: String,
        notes: String = ""
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to "Rejected",
                "approvedBy" to rejectedByUserId,
                "approvalDate" to Timestamp.now()
            )
            
            // Add notes if provided
            if (notes.isNotEmpty()) {
                updates["rejectionNotes"] = notes
            }
            
            db.collection("items")
                .document(itemId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Item $itemId rejected by $rejectedByUserId")
            
            // Send notification to reporting user
            // Requirements: 11.3
            NotificationManager.notifyFoundItemRejected(itemId, notes)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting item $itemId", e)
            Result.failure(e)
        }
    }
}
