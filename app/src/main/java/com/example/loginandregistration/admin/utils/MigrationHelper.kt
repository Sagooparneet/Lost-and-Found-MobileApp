package com.example.loginandregistration.admin.utils

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.example.loginandregistration.utils.UserRoleMigration
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper class for running data migrations from the admin dashboard.
 * Requirements: 10.5
 */
object MigrationHelper {
    
    private const val TAG = "MigrationHelper"
    
    /**
     * Shows a dialog to confirm and run the user role migration.
     * 
     * @param context Android context for showing dialogs
     * @param scope CoroutineScope for running the migration
     * @param onComplete Callback when migration completes (success or failure)
     */
    fun showMigrationDialog(
        context: Context,
        scope: CoroutineScope,
        onComplete: (Boolean, String) -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("User Role Migration")
            .setMessage(
                "This will migrate all user roles to the new three-role system:\n\n" +
                "• USER → STUDENT\n" +
                "• MODERATOR → SECURITY\n" +
                "• SECURITY → SECURITY (no change)\n" +
                "• ADMIN → ADMIN (no change)\n\n" +
                "Do you want to run a dry run first to preview changes?"
            )
            .setPositiveButton("Dry Run") { _, _ ->
                runMigration(context, scope, dryRun = true, onComplete)
            }
            .setNegativeButton("Run Migration") { _, _ ->
                confirmActualMigration(context, scope, onComplete)
            }
            .setNeutralButton("Cancel", null)
            .show()
    }
    
    /**
     * Shows a confirmation dialog before running the actual migration.
     */
    private fun confirmActualMigration(
        context: Context,
        scope: CoroutineScope,
        onComplete: (Boolean, String) -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("⚠️ Confirm Migration")
            .setMessage(
                "WARNING: This will permanently update user roles in the database.\n\n" +
                "This action cannot be undone automatically. Make sure you have:\n" +
                "1. Backed up your Firestore database\n" +
                "2. Run a dry run to preview changes\n" +
                "3. Tested the migration in a development environment\n\n" +
                "Are you sure you want to proceed?"
            )
            .setPositiveButton("Yes, Migrate") { _, _ ->
                runMigration(context, scope, dryRun = false, onComplete)
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }
    
    /**
     * Runs the user role migration.
     */
    private fun runMigration(
        context: Context,
        scope: CoroutineScope,
        dryRun: Boolean,
        onComplete: (Boolean, String) -> Unit
    ) {
        scope.launch {
            try {
                Log.i(TAG, "Starting migration (dryRun=$dryRun)...")
                
                val result = UserRoleMigration.migrateUserRoles(
                    firestore = FirebaseFirestore.getInstance(),
                    dryRun = dryRun
                )
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val migrationResult = result.getOrNull()!!
                        val message = buildString {
                            append(if (dryRun) "Dry Run Complete\n\n" else "Migration Complete\n\n")
                            append("Total users: ${migrationResult.totalUsers}\n")
                            append("Migrated: ${migrationResult.migratedCount}\n")
                            append("Skipped: ${migrationResult.skippedCount}\n")
                            append("Errors: ${migrationResult.errorCount}\n\n")
                            
                            if (migrationResult.details.isNotEmpty()) {
                                append("Changes:\n")
                                migrationResult.details.take(10).forEach { detail ->
                                    append("• $detail\n")
                                }
                                if (migrationResult.details.size > 10) {
                                    append("... and ${migrationResult.details.size - 10} more\n")
                                }
                            }
                            
                            if (dryRun && migrationResult.migratedCount > 0) {
                                append("\nRun the actual migration to apply these changes.")
                            }
                        }
                        
                        showResultDialog(context, "Migration Result", message)
                        onComplete(true, message)
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        val message = "Migration failed: $error"
                        showResultDialog(context, "Migration Error", message)
                        onComplete(false, message)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Migration error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    val message = "Migration error: ${e.message}"
                    showResultDialog(context, "Error", message)
                    onComplete(false, message)
                }
            }
        }
    }
    
    /**
     * Shows a dialog to validate the migration results.
     */
    fun showValidationDialog(
        context: Context,
        scope: CoroutineScope,
        onComplete: (Boolean, String) -> Unit
    ) {
        scope.launch {
            try {
                Log.i(TAG, "Starting validation...")
                
                val result = UserRoleMigration.validateMigration(
                    firestore = FirebaseFirestore.getInstance()
                )
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val validationResult = result.getOrNull()!!
                        val message = buildString {
                            append("Validation Complete\n\n")
                            append("Total users: ${validationResult.totalUsers}\n")
                            append("Valid roles: ${validationResult.validUsers}\n")
                            append("Invalid roles: ${validationResult.invalidUsers.size}\n\n")
                            
                            if (validationResult.invalidUsers.isNotEmpty()) {
                                append("Users with invalid roles:\n")
                                validationResult.invalidUsers.take(10).forEach { (userId, role) ->
                                    append("• $userId: $role\n")
                                }
                                if (validationResult.invalidUsers.size > 10) {
                                    append("... and ${validationResult.invalidUsers.size - 10} more\n")
                                }
                            } else {
                                append("✓ All users have valid roles!")
                            }
                        }
                        
                        showResultDialog(context, "Validation Result", message)
                        onComplete(true, message)
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        val message = "Validation failed: $error"
                        showResultDialog(context, "Validation Error", message)
                        onComplete(false, message)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Validation error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    val message = "Validation error: ${e.message}"
                    showResultDialog(context, "Error", message)
                    onComplete(false, message)
                }
            }
        }
    }
    
    /**
     * Shows a result dialog with the given title and message.
     */
    private fun showResultDialog(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
