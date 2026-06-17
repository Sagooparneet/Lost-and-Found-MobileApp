package com.example.loginandregistration.admin.utils

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Helper utility to fix data inconsistencies in Firestore
 * Addresses deserialization issues and permission problems
 */
object DataMigrationHelper {
    private const val TAG = "DataMigration"
    
    /**
     * Fix user role values - convert lowercase to uppercase
     * Fixes: "Could not find enum value of UserRole for value 'user'"
     */
    suspend fun fixUserRoles(): Result<Int> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val usersSnapshot = db.collection("users").get().await()
            var fixedCount = 0
            
            for (doc in usersSnapshot.documents) {
                val role = doc.getString("role")
                if (role != null && role != role.uppercase()) {
                    doc.reference.update("role", role.uppercase()).await()
                    fixedCount++
                    Log.d(TAG, "Fixed role for user ${doc.id}: $role -> ${role.uppercase()}")
                }
            }
            
            Log.i(TAG, "Fixed $fixedCount user roles")
            Result.success(fixedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error fixing user roles", e)
            Result.failure(e)
        }
    }
    
    /**
     * Fix createdAt fields - convert Long to Timestamp
     * Fixes: "Failed to convert value of type java.lang.Long to Timestamp"
     */
    suspend fun fixTimestampFields(): Result<Int> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val usersSnapshot = db.collection("users").get().await()
            var fixedCount = 0
            
            for (doc in usersSnapshot.documents) {
                val createdAt = doc.get("createdAt")
                if (createdAt is Long) {
                    val timestamp = Timestamp(createdAt / 1000, ((createdAt % 1000) * 1000000).toInt())
                    doc.reference.update("createdAt", timestamp).await()
                    fixedCount++
                    Log.d(TAG, "Fixed createdAt for user ${doc.id}")
                }
            }
            
            Log.i(TAG, "Fixed $fixedCount timestamp fields")
            Result.success(fixedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error fixing timestamp fields", e)
            Result.failure(e)
        }
    }
    
    /**
     * Ensure admin user document exists with correct role
     * Fixes: PERMISSION_DENIED errors for admin users
     */
    suspend fun ensureAdminUserDocument(userId: String, email: String): Result<Unit> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val userDoc = db.collection("users").document(userId).get().await()
            
            if (!userDoc.exists()) {
                // Create admin user document
                val adminUser = hashMapOf(
                    "uid" to userId,
                    "email" to email,
                    "displayName" to "Admin",
                    "photoUrl" to "",
                    "role" to "ADMIN",
                    "isBlocked" to false,
                    "createdAt" to Timestamp.now(),
                    "itemsReported" to 0,
                    "itemsFound" to 0,
                    "itemsClaimed" to 0
                )
                db.collection("users").document(userId).set(adminUser).await()
                Log.i(TAG, "Created admin user document for $email")
            } else {
                // Ensure role is uppercase
                val role = userDoc.getString("role")
                if (role != null && role != role.uppercase()) {
                    userDoc.reference.update("role", role.uppercase()).await()
                    Log.i(TAG, "Fixed role for admin user $email: $role -> ${role.uppercase()}")
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring admin user document", e)
            Result.failure(e)
        }
    }
    
    /**
     * Run all migrations at once
     */
    suspend fun runAllMigrations(): Result<String> {
        return try {
            val roleResult = fixUserRoles()
            val timestampResult = fixTimestampFields()
            
            val rolesFixed = roleResult.getOrNull() ?: 0
            val timestampsFixed = timestampResult.getOrNull() ?: 0
            
            val message = "Migration complete: $rolesFixed roles fixed, $timestampsFixed timestamps fixed"
            Log.i(TAG, message)
            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error running migrations", e)
            Result.failure(e)
        }
    }
}
