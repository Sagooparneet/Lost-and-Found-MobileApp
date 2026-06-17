package com.example.loginandregistration.admin.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Security helper for admin access verification and session management
 * Requirements: 7.1, 7.2, 7.3
 */
object SecurityHelper {
    private const val TAG = "SecurityHelper"
    private const val ADMIN_EMAIL = "admin@gmail.com"
    private const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes
    private const val USERS_COLLECTION = "users"
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    // Track last activity timestamp for session timeout
    private var lastActivityTimestamp: Long = System.currentTimeMillis()
    
    /**
     * Require admin access - throws SecurityException if not admin
     * Requirements: 7.1
     */
    @Throws(SecurityException::class)
    fun requireAdminAccess() {
        if (!isAdminUser()) {
            Log.w(TAG, "Unauthorized access attempt by: ${auth.currentUser?.email}")
            throw SecurityException("Admin access required")
        }
        
        // Check session timeout
        if (isSessionExpired()) {
            Log.w(TAG, "Session expired for: ${auth.currentUser?.email}")
            throw SecurityException("Session expired. Please log in again.")
        }
        
        // Update last activity timestamp
        updateLastActivity()
    }
    
    /**
     * Check if current user is admin
     * Requirements: 7.1
     */
    fun isAdminUser(): Boolean {
        val currentUser = auth.currentUser
        val isAdmin = currentUser?.email == ADMIN_EMAIL
        Log.d(TAG, "Checking admin access for user: ${currentUser?.email}, isAdmin: $isAdmin")
        return isAdmin
    }
    
    /**
     * Check if current user is authenticated
     * Requirements: 7.2
     */
    fun isAuthenticated(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * Check if session has expired
     * Requirements: 7.3
     */
    fun isSessionExpired(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastActivity = currentTime - lastActivityTimestamp
        return timeSinceLastActivity > SESSION_TIMEOUT_MS
    }
    
    /**
     * Update last activity timestamp
     * Requirements: 7.3
     */
    fun updateLastActivity() {
        lastActivityTimestamp = System.currentTimeMillis()
        Log.d(TAG, "Last activity updated: $lastActivityTimestamp")
    }
    
    /**
     * Reset session (for logout)
     * Requirements: 7.3
     */
    fun resetSession() {
        lastActivityTimestamp = 0L
        Log.d(TAG, "Session reset")
    }
    
    /**
     * Get remaining session time in milliseconds
     * Requirements: 7.3
     */
    fun getRemainingSessionTime(): Long {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastActivity = currentTime - lastActivityTimestamp
        val remaining = SESSION_TIMEOUT_MS - timeSinceLastActivity
        return if (remaining > 0) remaining else 0L
    }
    
    /**
     * Verify admin role from Firestore
     * Requirements: 7.1, 7.2
     */
    suspend fun verifyAdminRole(): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.success(false)
            
            if (currentUser.email != ADMIN_EMAIL) {
                return Result.success(false)
            }
            
            // Verify role in Firestore
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .get()
                .await()
            
            if (!userDoc.exists()) {
                Log.w(TAG, "Admin user document not found in Firestore")
                return Result.success(false)
            }
            
            val role = userDoc.getString("role") ?: "USER"
            val isAdmin = role == "ADMIN"
            
            Log.d(TAG, "Verified admin role from Firestore: $isAdmin")
            Result.success(isAdmin)
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying admin role", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if user is blocked
     * Requirements: 7.1
     */
    suspend fun isUserBlocked(userId: String): Result<Boolean> {
        return try {
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (!userDoc.exists()) {
                return Result.success(false)
            }
            
            val isBlocked = userDoc.getBoolean("isBlocked") ?: false
            Result.success(isBlocked)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if user is blocked", e)
            Result.failure(e)
        }
    }
    
    /**
     * Validate sensitive operation with re-authentication check
     * Requirements: 7.1
     */
    fun requireRecentAuthentication(maxAgeMs: Long = 5 * 60 * 1000L): Boolean {
        val currentUser = auth.currentUser ?: return false
        val metadata = currentUser.metadata ?: return false
        
        val lastSignInTime = metadata.lastSignInTimestamp
        val currentTime = System.currentTimeMillis()
        val timeSinceSignIn = currentTime - lastSignInTime
        
        return timeSinceSignIn <= maxAgeMs
    }
}
