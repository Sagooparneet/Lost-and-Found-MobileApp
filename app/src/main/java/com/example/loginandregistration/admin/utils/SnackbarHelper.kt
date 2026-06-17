package com.example.loginandregistration.admin.utils

import android.view.View
import com.example.loginandregistration.admin.models.AdminError
import com.google.android.material.snackbar.Snackbar

/**
 * Helper class for displaying user-friendly error and success messages using Snackbar
 * Requirements: 10.2, 10.7, 8.3, 8.6
 */
object SnackbarHelper {
    
    /**
     * Show an error Snackbar with user-friendly message
     */
    fun showError(
        view: View,
        error: AdminError,
        duration: Int = Snackbar.LENGTH_LONG,
        actionCallback: (() -> Unit)? = null
    ) {
        val message = ErrorMessageMapper.getErrorMessage(error)
        val actionText = ErrorMessageMapper.getSuggestedAction(error)
        
        val snackbar = Snackbar.make(view, message, duration)
        
        if (actionText != null && actionCallback != null) {
            snackbar.setAction(actionText) {
                actionCallback()
            }
        }
        
        snackbar.show()
    }
    
    /**
     * Show an error Snackbar with custom message
     */
    fun showError(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_LONG,
        actionText: String? = null,
        actionCallback: (() -> Unit)? = null
    ) {
        val snackbar = Snackbar.make(view, message, duration)
        
        if (actionText != null && actionCallback != null) {
            snackbar.setAction(actionText) {
                actionCallback()
            }
        }
        
        snackbar.show()
    }
    
    /**
     * Show a success Snackbar
     */
    fun showSuccess(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT
    ) {
        Snackbar.make(view, message, duration).show()
    }
    
    /**
     * Show an info Snackbar
     */
    fun showInfo(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT
    ) {
        Snackbar.make(view, message, duration).show()
    }
    
    /**
     * Show a warning Snackbar
     */
    fun showWarning(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_LONG,
        actionText: String? = null,
        actionCallback: (() -> Unit)? = null
    ) {
        val snackbar = Snackbar.make(view, message, duration)
        
        if (actionText != null && actionCallback != null) {
            snackbar.setAction(actionText) {
                actionCallback()
            }
        }
        
        snackbar.show()
    }
    
    /**
     * Show error from Result failure
     */
    fun showErrorFromResult(
        view: View,
        result: Result<*>,
        duration: Int = Snackbar.LENGTH_LONG,
        actionCallback: (() -> Unit)? = null
    ) {
        result.exceptionOrNull()?.let { exception ->
            showError(view, exception.message ?: "An error occurred", duration, "Retry", actionCallback)
        }
    }
    
    /**
     * Common error messages for specific operations
     */
    object Messages {
        // User management
        const val USER_BLOCKED_SUCCESS = "User blocked successfully"
        const val USER_UNBLOCKED_SUCCESS = "User unblocked successfully"
        const val USER_ROLE_UPDATED_SUCCESS = "User role updated successfully"
        const val USER_DETAILS_UPDATED_SUCCESS = "User details updated successfully"
        const val USER_NOT_FOUND = "User not found"
        
        // Item management
        const val ITEM_UPDATED_SUCCESS = "Item updated successfully"
        const val ITEM_DELETED_SUCCESS = "Item deleted successfully"
        const val ITEM_STATUS_UPDATED_SUCCESS = "Item status updated successfully"
        const val ITEM_NOT_FOUND = "Item not found"
        
        // Donation management
        const val DONATION_MARKED_READY_SUCCESS = "Item marked ready for donation"
        const val DONATION_COMPLETED_SUCCESS = "Item marked as donated"
        const val DONATION_FLAGGED_SUCCESS = "Item flagged for donation"
        
        // Notification
        const val NOTIFICATION_SENT_SUCCESS = "Notification sent successfully"
        const val NOTIFICATION_SCHEDULED_SUCCESS = "Notification scheduled successfully"
        const val NOTIFICATION_FAILED = "Failed to send notification"
        
        // Export
        const val EXPORT_STARTED = "Export started. This may take a moment..."
        const val EXPORT_SUCCESS = "Export completed successfully"
        const val EXPORT_FAILED = "Export failed. Please try again"
        
        // Activity log
        const val ACTIVITY_LOGGED = "Activity logged successfully"
        
        // General
        const val OPERATION_SUCCESS = "Operation completed successfully"
        const val OPERATION_FAILED = "Operation failed. Please try again"
        const val LOADING = "Loading..."
        const val NETWORK_ERROR = "Network error. Please check your connection"
        const val PERMISSION_DENIED = "Permission denied"
        const val INVALID_INPUT = "Invalid input. Please check your data"
    }
}

/**
 * Extension function for View to show error Snackbar
 */
fun View.showError(error: AdminError, actionCallback: (() -> Unit)? = null) {
    SnackbarHelper.showError(this, error, actionCallback = actionCallback)
}

/**
 * Extension function for View to show error message
 */
fun View.showError(message: String, actionText: String? = null, actionCallback: (() -> Unit)? = null) {
    SnackbarHelper.showError(this, message, actionText = actionText, actionCallback = actionCallback)
}

/**
 * Extension function for View to show success Snackbar
 */
fun View.showSuccess(message: String) {
    SnackbarHelper.showSuccess(this, message)
}

/**
 * Extension function for View to show info Snackbar
 */
fun View.showInfo(message: String) {
    SnackbarHelper.showInfo(this, message)
}

/**
 * Extension function for View to show warning Snackbar
 */
fun View.showWarning(message: String, actionText: String? = null, actionCallback: (() -> Unit)? = null) {
    SnackbarHelper.showWarning(this, message, actionText = actionText, actionCallback = actionCallback)
}
