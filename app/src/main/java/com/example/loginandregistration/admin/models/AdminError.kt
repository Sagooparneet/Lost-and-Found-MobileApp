package com.example.loginandregistration.admin.models

/**
 * Sealed class representing all possible errors in the admin module
 * Requirements: 10.2, 10.3
 */
sealed class AdminError(open val message: String, open val cause: Throwable? = null) {
    
    /**
     * Network-related errors (connectivity, timeout, etc.)
     */
    data class NetworkError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AdminError(message, cause)
    
    /**
     * Authentication errors (not logged in, session expired, etc.)
     */
    data class AuthenticationError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AdminError(message, cause)
    
    /**
     * Permission/Authorization errors (not admin, insufficient permissions, etc.)
     */
    data class PermissionError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AdminError(message, cause)
    
    /**
     * Data validation errors (invalid input, missing required fields, etc.)
     */
    data class ValidationError(
        val field: String,
        override val message: String,
        override val cause: Throwable? = null
    ) : AdminError(message, cause)
    
    /**
     * Resource not found errors (user, item, etc. doesn't exist)
     */
    data class NotFoundError(
        val entity: String,
        override val message: String,
        override val cause: Throwable? = null
    ) : AdminError(message, cause)
    
    /**
     * Firestore database errors
     */
    data class FirestoreError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AdminError(message, cause)
    
    /**
     * Export operation errors (PDF/CSV generation, file storage, etc.)
     */
    data class ExportError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AdminError(message, cause)
    
    /**
     * Push notification errors (FCM, delivery failures, etc.)
     */
    data class NotificationError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AdminError(message, cause)
    
    /**
     * Storage errors (file system, Firebase Storage, etc.)
     */
    data class StorageError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AdminError(message, cause)
    
    /**
     * Unknown or unexpected errors
     */
    data class UnknownError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AdminError(message, cause)
    
    /**
     * Convert to a user-friendly error message
     */
    fun toUserMessage(): String {
        return when (this) {
            is NetworkError -> "Network error: $message. Please check your connection and try again."
            is AuthenticationError -> "Authentication error: $message. Please log in again."
            is PermissionError -> "Permission denied: $message. You don't have access to perform this action."
            is ValidationError -> "Invalid $field: $message"
            is NotFoundError -> "$entity not found: $message"
            is FirestoreError -> "Database error: $message. Please try again later."
            is ExportError -> "Export failed: $message. Please try again."
            is NotificationError -> "Notification error: $message. The notification may not have been delivered."
            is StorageError -> "Storage error: $message. Please check storage permissions and available space."
            is UnknownError -> "An unexpected error occurred: $message. Please try again."
        }
    }
    
    /**
     * Check if this error is retryable
     */
    fun isRetryable(): Boolean {
        return when (this) {
            is NetworkError -> true
            is FirestoreError -> true
            is StorageError -> true
            is AuthenticationError -> false
            is PermissionError -> false
            is ValidationError -> false
            is NotFoundError -> false
            is ExportError -> false
            is NotificationError -> true
            is UnknownError -> false
        }
    }
}

/**
 * Extension function to convert exceptions to AdminError
 */
fun Throwable.toAdminError(): AdminError {
    return when (this) {
        is SecurityException -> AdminError.PermissionError(
            message = this.message ?: "Security check failed",
            cause = this
        )
        is IllegalArgumentException -> AdminError.ValidationError(
            field = "input",
            message = this.message ?: "Invalid input",
            cause = this
        )
        is NoSuchElementException -> AdminError.NotFoundError(
            entity = "Resource",
            message = this.message ?: "Resource not found",
            cause = this
        )
        is com.google.firebase.firestore.FirebaseFirestoreException -> {
            when (this.code) {
                com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE,
                com.google.firebase.firestore.FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> 
                    AdminError.NetworkError(
                        message = "Database connection failed",
                        cause = this
                    )
                com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    AdminError.PermissionError(
                        message = "Database permission denied",
                        cause = this
                    )
                com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND ->
                    AdminError.NotFoundError(
                        entity = "Document",
                        message = "Document not found in database",
                        cause = this
                    )
                else -> AdminError.FirestoreError(
                    message = this.message ?: "Database error",
                    cause = this
                )
            }
        }
        is java.io.IOException -> AdminError.StorageError(
            message = this.message ?: "File operation failed",
            cause = this
        )
        is java.net.UnknownHostException,
        is java.net.SocketTimeoutException -> AdminError.NetworkError(
            message = "Network connection failed",
            cause = this
        )
        else -> AdminError.UnknownError(
            message = this.message ?: "An unexpected error occurred",
            cause = this
        )
    }
}
