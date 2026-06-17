package com.example.loginandregistration.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Data migration utility for updating user roles to the new three-role system.
 * Requirements: 10.1, 10.2, 10.5
 * 
 * This script migrates existing user documents from the old role system (USER, MODERATOR)
 * to the new three-role system (STUDENT, SECURITY, ADMIN).
 * 
 * Migration mapping:
 * - USER -> STUDENT
 * - MODERATOR -> SECURITY (or ADMIN based on email)
 * - SECURITY -> SECURITY (no change)
 * - ADMIN -> ADMIN (no change)
 * - Any other value -> STUDENT (default)
 * 
 * Usage:
 * Call migrateUserRoles() from an admin activity or use Firebase Cloud Functions
 * to run this migration on the server side.
 */
object UserRoleMigration {
    
    private const val TAG = "UserRoleMigration"
    private const val USERS_COLLECTION = "users"
    
    /**
     * Migrates all user documents to the new three-role system.
     * 
     * @param firestore FirebaseFirestore instance
     * @param dryRun If true, only logs changes without updating the database
     * @return Result containing the number of users migrated or an error
     */
    suspend fun migrateUserRoles(
        firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
        dryRun: Boolean = true
    ): Result<MigrationResult> {
        return try {
            Log.i(TAG, "Starting user role migration (dryRun=$dryRun)...")
            
            // Fetch all users
            val snapshot = firestore.collection(USERS_COLLECTION)
                .get()
                .await()
            
            val totalUsers = snapshot.documents.size
            var migratedCount = 0
            var skippedCount = 0
            var errorCount = 0
            val migrationDetails = mutableListOf<String>()
            
            Log.i(TAG, "Found $totalUsers users to process")
            
            for (doc in snapshot.documents) {
                try {
                    val userId = doc.id
                    val currentRole = doc.getString("role") ?: "USER"
                    val userEmail = doc.getString("email") ?: ""
                    
                    // Determine new role based on migration mapping
                    val newRole = mapOldRoleToNew(currentRole, userEmail)
                    
                    if (currentRole.uppercase() == newRole) {
                        // Role doesn't need migration
                        skippedCount++
                        Log.d(TAG, "Skipping user $userId ($userEmail): role '$currentRole' is already valid")
                        continue
                    }
                    
                    val migrationInfo = "User $userId ($userEmail): $currentRole -> $newRole"
                    migrationDetails.add(migrationInfo)
                    Log.i(TAG, migrationInfo)
                    
                    if (!dryRun) {
                        // Update the user document
                        firestore.collection(USERS_COLLECTION)
                            .document(userId)
                            .update("role", newRole)
                            .await()
                        
                        Log.i(TAG, "Successfully migrated user $userId")
                    }
                    
                    migratedCount++
                    
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Error migrating user ${doc.id}: ${e.message}", e)
                }
            }
            
            val result = MigrationResult(
                totalUsers = totalUsers,
                migratedCount = migratedCount,
                skippedCount = skippedCount,
                errorCount = errorCount,
                dryRun = dryRun,
                details = migrationDetails
            )
            
            Log.i(TAG, "Migration complete: $result")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Maps old role values to new three-role system.
     * 
     * @param oldRole The current role value from the database
     * @param userEmail The user's email (for special case handling)
     * @return The new role value (STUDENT, SECURITY, or ADMIN)
     */
    private fun mapOldRoleToNew(oldRole: String, userEmail: String): String {
        return when (oldRole.uppercase()) {
            "STUDENT" -> "STUDENT"  // Already valid
            "SECURITY" -> "SECURITY"  // Already valid
            "ADMIN" -> "ADMIN"  // Already valid
            
            // Legacy roles to migrate
            "USER" -> "STUDENT"
            "MODERATOR" -> {
                // Check if moderator should be admin based on email
                if (userEmail.equals("admin@gmail.com", ignoreCase = true)) {
                    "ADMIN"
                } else {
                    "SECURITY"
                }
            }
            
            // Unknown roles default to STUDENT
            else -> {
                Log.w(TAG, "Unknown role '$oldRole' for user $userEmail, defaulting to STUDENT")
                "STUDENT"
            }
        }
    }
    
    /**
     * Validates that all users have valid roles after migration.
     * 
     * @param firestore FirebaseFirestore instance
     * @return Result containing validation results
     */
    suspend fun validateMigration(
        firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    ): Result<ValidationResult> {
        return try {
            Log.i(TAG, "Validating user role migration...")
            
            val snapshot = firestore.collection(USERS_COLLECTION)
                .get()
                .await()
            
            val validRoles = setOf("STUDENT", "SECURITY", "ADMIN")
            val invalidUsers = mutableListOf<Pair<String, String>>()
            
            for (doc in snapshot.documents) {
                val userId = doc.id
                val role = doc.getString("role") ?: ""
                
                if (role.uppercase() !in validRoles) {
                    invalidUsers.add(userId to role)
                    Log.w(TAG, "Invalid role found: User $userId has role '$role'")
                }
            }
            
            val result = ValidationResult(
                totalUsers = snapshot.documents.size,
                validUsers = snapshot.documents.size - invalidUsers.size,
                invalidUsers = invalidUsers
            )
            
            Log.i(TAG, "Validation complete: $result")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Validation failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}

/**
 * Result of the migration operation.
 */
data class MigrationResult(
    val totalUsers: Int,
    val migratedCount: Int,
    val skippedCount: Int,
    val errorCount: Int,
    val dryRun: Boolean,
    val details: List<String>
) {
    override fun toString(): String {
        return """
            Migration Result (dryRun=$dryRun):
            - Total users: $totalUsers
            - Migrated: $migratedCount
            - Skipped (already valid): $skippedCount
            - Errors: $errorCount
        """.trimIndent()
    }
}

/**
 * Result of the validation operation.
 */
data class ValidationResult(
    val totalUsers: Int,
    val validUsers: Int,
    val invalidUsers: List<Pair<String, String>>
) {
    override fun toString(): String {
        return """
            Validation Result:
            - Total users: $totalUsers
            - Valid roles: $validUsers
            - Invalid roles: ${invalidUsers.size}
            ${if (invalidUsers.isNotEmpty()) "Invalid users: ${invalidUsers.joinToString { "(${it.first}: ${it.second})" }}" else ""}
        """.trimIndent()
    }
}
