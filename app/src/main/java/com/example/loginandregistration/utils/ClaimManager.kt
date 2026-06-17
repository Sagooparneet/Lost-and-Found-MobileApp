package com.example.loginandregistration.utils

import android.util.Log
import com.example.loginandregistration.ClaimRequest
import com.example.loginandregistration.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Utility object for managing claim requests for found items.
 * Handles claim creation, approval, rejection, and notification sending.
 * Requirements: 5.3, 5.4, 5.5, 5.6
 */
object ClaimManager {
    
    private const val TAG = "ClaimManager"
    private val db = FirebaseFirestore.getInstance()
    
    /**
     * Creates a new claim request for a found item.
     * 
     * @param itemId The ID of the item being claimed
     * @param itemName The name of the item being claimed
     * @param userId The ID of the user making the claim
     * @param reason The reason for claiming the item
     * @param proofDescription Optional description of proof of ownership
     * @return Result containing the created claim request ID or error
     * Requirements: 5.3
     */
    suspend fun createClaimRequest(
        itemId: String,
        itemName: String,
        userId: String,
        reason: String,
        proofDescription: String = ""
    ): Result<String> {
        return try {
            // Fetch user details from Firestore
            val userDoc = db.collection("users")
                .document(userId)
                .get()
                .await()
            
            val user = userDoc.toObject(User::class.java)
            
            if (user == null) {
                return Result.failure(Exception("User not found"))
            }
            
            // Create claim request document
            val claimRequestRef = db.collection("claimRequests").document()
            val claimRequest = ClaimRequest(
                requestId = claimRequestRef.id,
                itemId = itemId,
                itemName = itemName,
                userId = userId,
                userEmail = user.email,
                userName = user.displayName.ifEmpty { user.email },
                userPhone = user.phone,
                reason = reason,
                proofDescription = proofDescription,
                status = ClaimRequest.ClaimStatus.PENDING,
                requestDate = Timestamp.now()
            )
            
            claimRequestRef.set(claimRequest).await()
            
            Log.d(TAG, "Claim request created: ${claimRequest.requestId}")
            
            // Send notification to security users
            // Requirements: 11.4
            NotificationManager.notifyClaimSubmitted(claimRequest.requestId)
            
            Result.success(claimRequest.requestId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating claim request", e)
            Result.failure(e)
        }
    }
    
    /**
     * Approves a claim request and updates the item status to "Returned".
     * 
     * @param requestId The ID of the claim request to approve
     * @param reviewedByUserId The user ID of the security officer approving the claim
     * @return Result indicating success or failure
     * Requirements: 5.5, 5.6
     */
    suspend fun approveClaim(
        requestId: String,
        reviewedByUserId: String
    ): Result<Unit> {
        return try {
            // Get the claim request to retrieve the item ID
            val claimDoc = db.collection("claimRequests")
                .document(requestId)
                .get()
                .await()
            
            val claimRequest = claimDoc.toObject(ClaimRequest::class.java)
            
            if (claimRequest == null) {
                return Result.failure(Exception("Claim request not found"))
            }
            
            // Update claim request status
            val claimUpdates = hashMapOf<String, Any>(
                "status" to ClaimRequest.ClaimStatus.APPROVED,
                "reviewedBy" to reviewedByUserId,
                "reviewDate" to Timestamp.now()
            )
            
            db.collection("claimRequests")
                .document(requestId)
                .update(claimUpdates)
                .await()
            
            // Update item status to "Returned"
            val itemUpdates = hashMapOf<String, Any>(
                "status" to "Returned"
            )
            
            db.collection("items")
                .document(claimRequest.itemId)
                .update(itemUpdates)
                .await()
            
            Log.d(TAG, "Claim request $requestId approved by $reviewedByUserId")
            
            // Send notification to requesting user
            // Requirements: 11.5
            NotificationManager.notifyClaimApproved(requestId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error approving claim request $requestId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Rejects a claim request with optional notes.
     * 
     * @param requestId The ID of the claim request to reject
     * @param reviewedByUserId The user ID of the security officer rejecting the claim
     * @param reviewNotes Optional notes explaining the rejection reason
     * @return Result indicating success or failure
     * Requirements: 5.5, 5.6
     */
    suspend fun rejectClaim(
        requestId: String,
        reviewedByUserId: String,
        reviewNotes: String = ""
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to ClaimRequest.ClaimStatus.REJECTED,
                "reviewedBy" to reviewedByUserId,
                "reviewDate" to Timestamp.now()
            )
            
            // Add notes if provided
            if (reviewNotes.isNotEmpty()) {
                updates["reviewNotes"] = reviewNotes
            }
            
            db.collection("claimRequests")
                .document(requestId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Claim request $requestId rejected by $reviewedByUserId")
            
            // Send notification to requesting user
            // Requirements: 11.6
            NotificationManager.notifyClaimRejected(requestId, reviewNotes)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting claim request $requestId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Checks if a user has a pending claim request for a specific item.
     * 
     * @param itemId The ID of the item
     * @param userId The ID of the user
     * @return True if the user has a pending claim for the item, false otherwise
     */
    suspend fun hasPendingClaim(itemId: String, userId: String): Boolean {
        return try {
            val querySnapshot = db.collection("claimRequests")
                .whereEqualTo("itemId", itemId)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", ClaimRequest.ClaimStatus.PENDING)
                .get()
                .await()
            
            !querySnapshot.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Error checking pending claim", e)
            false
        }
    }
}
