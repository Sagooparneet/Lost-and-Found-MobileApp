package com.example.loginandregistration.admin.utils

import android.util.Log
import com.example.loginandregistration.admin.models.AdminError
import com.example.loginandregistration.admin.models.toAdminError

/**
 * Centralized error handling utility for the admin module
 * Requirements: 10.2, 10.3
 */
object ErrorHandler {
    private const val TAG = "ErrorHandler"
    
    /**
     * Handle an exception and convert it to AdminError
     */
    fun handleException(exception: Throwable, context: String = ""): AdminError {
        val adminError = exception.toAdminError()
        
        val logMessage = if (context.isNotEmpty()) {
            "Error in $context: ${adminError.message}"
        } else {
            "Error: ${adminError.message}"
        }
        
        Log.e(TAG, logMessage, exception)
        
        return adminError
    }
    
    /**
     * Wrap a suspend operation with error handling
     */
    suspend fun <T> wrapOperation(
        context: String = "",
        operation: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(operation())
        } catch (e: Exception) {
            val adminError = handleException(e, context)
            Result.failure(Exception(adminError.toUserMessage(), e))
        }
    }
    
    /**
     * Wrap a suspend operation with error handling and retry logic
     */
    suspend fun <T> wrapOperationWithRetry(
        context: String = "",
        maxRetries: Int = 3,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            RetryHelper.retryOperation(
                maxRetries = maxRetries,
                operation = operation
            )
        } catch (e: Exception) {
            val adminError = handleException(e, context)
            Result.failure(Exception(adminError.toUserMessage(), e))
        }
    }
    
    /**
     * Log an error without throwing
     */
    fun logError(exception: Throwable, context: String = "") {
        handleException(exception, context)
    }
    
    /**
     * Create a validation error
     */
    fun createValidationError(field: String, message: String): AdminError {
        return AdminError.ValidationError(field, message)
    }
    
    /**
     * Create a not found error
     */
    fun createNotFoundError(entity: String, message: String): AdminError {
        return AdminError.NotFoundError(entity, message)
    }
    
    /**
     * Create a permission error
     */
    fun createPermissionError(message: String): AdminError {
        return AdminError.PermissionError(message)
    }
}

/**
 * Extension function to safely execute a block and return Result
 */
suspend fun <T> safeExecute(
    context: String = "",
    block: suspend () -> T
): Result<T> {
    return ErrorHandler.wrapOperation(context, block)
}

/**
 * Extension function to safely execute a block with retry and return Result
 */
suspend fun <T> safeExecuteWithRetry(
    context: String = "",
    maxRetries: Int = 3,
    block: suspend () -> T
): Result<T> {
    return ErrorHandler.wrapOperationWithRetry(context, maxRetries, block)
}
