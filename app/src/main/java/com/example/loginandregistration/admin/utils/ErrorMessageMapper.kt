package com.example.loginandregistration.admin.utils

import com.example.loginandregistration.admin.models.AdminError

/**
 * Maps AdminError types to user-friendly error messages
 * Requirements: 10.2, 10.7
 */
object ErrorMessageMapper {
    
    /**
     * Get a user-friendly error message for an AdminError
     */
    fun getErrorMessage(error: AdminError): String {
        return when (error) {
            is AdminError.NetworkError -> getNetworkErrorMessage(error)
            is AdminError.AuthenticationError -> getAuthenticationErrorMessage(error)
            is AdminError.PermissionError -> getPermissionErrorMessage(error)
            is AdminError.ValidationError -> getValidationErrorMessage(error)
            is AdminError.NotFoundError -> getNotFoundErrorMessage(error)
            is AdminError.FirestoreError -> getFirestoreErrorMessage(error)
            is AdminError.ExportError -> getExportErrorMessage(error)
            is AdminError.NotificationError -> getNotificationErrorMessage(error)
            is AdminError.StorageError -> getStorageErrorMessage(error)
            is AdminError.UnknownError -> getUnknownErrorMessage(error)
        }
    }
    
    /**
     * Get a short title for the error (for Snackbar action or dialog title)
     */
    fun getErrorTitle(error: AdminError): String {
        return when (error) {
            is AdminError.NetworkError -> "Connection Error"
            is AdminError.AuthenticationError -> "Authentication Required"
            is AdminError.PermissionError -> "Access Denied"
            is AdminError.ValidationError -> "Invalid Input"
            is AdminError.NotFoundError -> "Not Found"
            is AdminError.FirestoreError -> "Database Error"
            is AdminError.ExportError -> "Export Failed"
            is AdminError.NotificationError -> "Notification Failed"
            is AdminError.StorageError -> "Storage Error"
            is AdminError.UnknownError -> "Error"
        }
    }
    
    private fun getNetworkErrorMessage(error: AdminError.NetworkError): String {
        return when {
            error.message.contains("timeout", ignoreCase = true) ->
                "Connection timed out. Please check your internet connection and try again."
            error.message.contains("unavailable", ignoreCase = true) ->
                "Service is temporarily unavailable. Please try again in a few moments."
            error.message.contains("host", ignoreCase = true) ->
                "Unable to reach the server. Please check your internet connection."
            else ->
                "Network error occurred. Please check your connection and try again."
        }
    }
    
    private fun getAuthenticationErrorMessage(error: AdminError.AuthenticationError): String {
        return when {
            error.message.contains("expired", ignoreCase = true) ->
                "Your session has expired. Please log in again."
            error.message.contains("invalid", ignoreCase = true) ->
                "Invalid credentials. Please log in again."
            error.message.contains("not authenticated", ignoreCase = true) ->
                "You are not logged in. Please log in to continue."
            else ->
                "Authentication error. Please log in again."
        }
    }
    
    private fun getPermissionErrorMessage(error: AdminError.PermissionError): String {
        return when {
            error.message.contains("admin", ignoreCase = true) ->
                "This action requires administrator privileges."
            error.message.contains("denied", ignoreCase = true) ->
                "You don't have permission to perform this action."
            else ->
                "Access denied. You don't have the required permissions."
        }
    }
    
    private fun getValidationErrorMessage(error: AdminError.ValidationError): String {
        val field = error.field.replaceFirstChar { it.uppercase() }
        return when {
            error.message.contains("required", ignoreCase = true) ->
                "$field is required. Please provide a value."
            error.message.contains("invalid", ignoreCase = true) ->
                "$field is invalid. Please check your input."
            error.message.contains("too long", ignoreCase = true) ->
                "$field is too long. Please use fewer characters."
            error.message.contains("too short", ignoreCase = true) ->
                "$field is too short. Please provide more information."
            error.message.contains("format", ignoreCase = true) ->
                "$field has an invalid format. Please check your input."
            else ->
                "$field: ${error.message}"
        }
    }
    
    private fun getNotFoundErrorMessage(error: AdminError.NotFoundError): String {
        val entity = error.entity.lowercase()
        return when {
            error.message.contains("deleted", ignoreCase = true) ->
                "This $entity has been deleted and is no longer available."
            error.message.contains("exist", ignoreCase = true) ->
                "The requested $entity could not be found."
            else ->
                "$entity not found. It may have been removed or doesn't exist."
        }
    }
    
    private fun getFirestoreErrorMessage(error: AdminError.FirestoreError): String {
        return when {
            error.message.contains("permission", ignoreCase = true) ->
                "Database access denied. Please contact support."
            error.message.contains("quota", ignoreCase = true) ->
                "Database quota exceeded. Please try again later."
            error.message.contains("index", ignoreCase = true) ->
                "Database configuration error. Please contact support."
            else ->
                "Database error occurred. Please try again later."
        }
    }
    
    private fun getExportErrorMessage(error: AdminError.ExportError): String {
        return when {
            error.message.contains("permission", ignoreCase = true) ->
                "Storage permission required. Please grant storage access."
            error.message.contains("space", ignoreCase = true) ->
                "Insufficient storage space. Please free up some space and try again."
            error.message.contains("format", ignoreCase = true) ->
                "Export format error. Please try a different format."
            else ->
                "Failed to export data. Please try again."
        }
    }
    
    private fun getNotificationErrorMessage(error: AdminError.NotificationError): String {
        return when {
            error.message.contains("token", ignoreCase = true) ->
                "Notification delivery failed. User may have disabled notifications."
            error.message.contains("permission", ignoreCase = true) ->
                "Notification permission required. Please enable notifications."
            error.message.contains("quota", ignoreCase = true) ->
                "Notification quota exceeded. Please try again later."
            else ->
                "Failed to send notification. It may not have been delivered."
        }
    }
    
    private fun getStorageErrorMessage(error: AdminError.StorageError): String {
        return when {
            error.message.contains("permission", ignoreCase = true) ->
                "Storage permission required. Please grant storage access in settings."
            error.message.contains("space", ignoreCase = true) ->
                "Insufficient storage space. Please free up some space."
            error.message.contains("not found", ignoreCase = true) ->
                "File not found. It may have been moved or deleted."
            error.message.contains("read", ignoreCase = true) ->
                "Unable to read file. Please check file permissions."
            error.message.contains("write", ignoreCase = true) ->
                "Unable to write file. Please check storage permissions."
            else ->
                "Storage operation failed. Please try again."
        }
    }
    
    private fun getUnknownErrorMessage(error: AdminError.UnknownError): String {
        return "An unexpected error occurred. Please try again or contact support if the problem persists."
    }
    
    /**
     * Get a suggested action message for the error
     */
    fun getSuggestedAction(error: AdminError): String? {
        return when (error) {
            is AdminError.NetworkError -> "Retry"
            is AdminError.AuthenticationError -> "Log In"
            is AdminError.PermissionError -> null
            is AdminError.ValidationError -> "Fix Input"
            is AdminError.NotFoundError -> null
            is AdminError.FirestoreError -> "Retry"
            is AdminError.ExportError -> "Retry"
            is AdminError.NotificationError -> "Retry"
            is AdminError.StorageError -> "Settings"
            is AdminError.UnknownError -> "Retry"
        }
    }
}
