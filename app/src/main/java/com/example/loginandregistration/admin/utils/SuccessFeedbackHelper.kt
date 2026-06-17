package com.example.loginandregistration.admin.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

/**
 * Helper for providing success feedback to users
 * Requirements: 8.3, 8.6
 */
object SuccessFeedbackHelper {
    
    /**
     * Show success Snackbar
     */
    fun showSuccessSnackbar(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT
    ) {
        Snackbar.make(view, message, duration)
            .setAction("OK") { /* Dismiss */ }
            .show()
    }
    
    /**
     * Show success Toast
     */
    fun showSuccessToast(
        context: Context,
        message: String,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        Toast.makeText(context, message, duration).show()
    }
    
    /**
     * Show operation completion notification
     */
    fun showOperationComplete(
        view: View,
        operationName: String,
        duration: Int = Snackbar.LENGTH_SHORT
    ) {
        val message = "$operationName completed successfully"
        showSuccessSnackbar(view, message, duration)
    }
    
    /**
     * Show save success feedback
     */
    fun showSaveSuccess(view: View) {
        showSuccessSnackbar(view, "Changes saved successfully")
    }
    
    /**
     * Show delete success feedback
     */
    fun showDeleteSuccess(view: View, itemName: String = "Item") {
        showSuccessSnackbar(view, "$itemName deleted successfully")
    }
    
    /**
     * Show update success feedback
     */
    fun showUpdateSuccess(view: View, itemName: String = "Item") {
        showSuccessSnackbar(view, "$itemName updated successfully")
    }
    
    /**
     * Show create success feedback
     */
    fun showCreateSuccess(view: View, itemName: String = "Item") {
        showSuccessSnackbar(view, "$itemName created successfully")
    }
    
    /**
     * Common success messages
     */
    object Messages {
        // User operations
        const val USER_BLOCKED = "User blocked successfully"
        const val USER_UNBLOCKED = "User unblocked successfully"
        const val USER_ROLE_CHANGED = "User role changed successfully"
        const val USER_UPDATED = "User updated successfully"
        
        // Item operations
        const val ITEM_UPDATED = "Item updated successfully"
        const val ITEM_DELETED = "Item deleted successfully"
        const val ITEM_STATUS_CHANGED = "Item status changed successfully"
        
        // Donation operations
        const val DONATION_MARKED_READY = "Item marked ready for donation"
        const val DONATION_COMPLETED = "Item marked as donated"
        const val DONATION_FLAGGED = "Item flagged for donation"
        
        // Notification operations
        const val NOTIFICATION_SENT = "Notification sent successfully"
        const val NOTIFICATION_SCHEDULED = "Notification scheduled successfully"
        
        // Export operations
        const val EXPORT_COMPLETED = "Export completed successfully"
        const val EXPORT_STARTED = "Export started"
        
        // General
        const val CHANGES_SAVED = "Changes saved successfully"
        const val OPERATION_COMPLETED = "Operation completed successfully"
    }
}

/**
 * Extension functions for View to show success feedback
 */
fun View.showSuccessFeedback(message: String) {
    SuccessFeedbackHelper.showSuccessSnackbar(this, message)
}

fun View.showOperationComplete(operationName: String) {
    SuccessFeedbackHelper.showOperationComplete(this, operationName)
}

fun View.showSaveSuccess() {
    SuccessFeedbackHelper.showSaveSuccess(this)
}

fun View.showDeleteSuccess(itemName: String = "Item") {
    SuccessFeedbackHelper.showDeleteSuccess(this, itemName)
}

fun View.showUpdateSuccess(itemName: String = "Item") {
    SuccessFeedbackHelper.showUpdateSuccess(this, itemName)
}

fun View.showCreateSuccess(itemName: String = "Item") {
    SuccessFeedbackHelper.showCreateSuccess(this, itemName)
}

/**
 * Extension functions for Context to show success Toast
 */
fun Context.showSuccessToast(message: String) {
    SuccessFeedbackHelper.showSuccessToast(this, message)
}
