package com.example.loginandregistration.admin.repository

import com.example.loginandregistration.admin.models.AdminError
import com.example.loginandregistration.admin.models.toAdminError
import com.example.loginandregistration.admin.utils.ErrorHandler
import com.example.loginandregistration.admin.utils.RetryHelper
import android.util.Log

/**
 * Extension functions for AdminRepository to add comprehensive error handling
 * Requirements: 10.2, 10.3
 */

private const val TAG = "AdminRepositoryExt"

/**
 * Execute a repository operation with error handling
 */
suspend fun <T> executeRepositoryOperation(
    operationName: String,
    operation: suspend () -> T
): Result<T> {
    return try {
        Log.d(TAG, "Executing operation: $operationName")
        Result.success(operation())
    } catch (e: Exception) {
        val adminError = ErrorHandler.handleException(e, operationName)
        Log.e(TAG, "Operation failed: $operationName - ${adminError.message}", e)
        Result.failure(Exception(adminError.toUserMessage(), e))
    }
}

/**
 * Execute a repository operation with retry logic
 */
suspend fun <T> executeRepositoryOperationWithRetry(
    operationName: String,
    maxRetries: Int = 3,
    operation: suspend () -> T
): Result<T> {
    return try {
        Log.d(TAG, "Executing operation with retry: $operationName")
        RetryHelper.retryOperation(
            maxRetries = maxRetries,
            operation = operation
        )
    } catch (e: Exception) {
        val adminError = ErrorHandler.handleException(e, operationName)
        Log.e(TAG, "Operation failed after retries: $operationName - ${adminError.message}", e)
        Result.failure(Exception(adminError.toUserMessage(), e))
    }
}

/**
 * Execute a Firestore operation with retry logic
 */
suspend fun <T> executeFirestoreOperation(
    operationName: String,
    operation: suspend () -> T
): Result<T> {
    return try {
        Log.d(TAG, "Executing Firestore operation: $operationName")
        RetryHelper.retryFirestoreOperation(operation)
    } catch (e: Exception) {
        val adminError = ErrorHandler.handleException(e, operationName)
        Log.e(TAG, "Firestore operation failed: $operationName - ${adminError.message}", e)
        Result.failure(Exception(adminError.toUserMessage(), e))
    }
}

/**
 * Validate and execute an operation
 */
suspend fun <T> validateAndExecute(
    operationName: String,
    validations: List<() -> AdminError?>,
    operation: suspend () -> T
): Result<T> {
    // Run all validations
    for (validation in validations) {
        val error = validation()
        if (error != null) {
            Log.w(TAG, "Validation failed for $operationName: ${error.message}")
            return Result.failure(Exception(error.toUserMessage()))
        }
    }
    
    // Execute operation if all validations pass
    return executeRepositoryOperation(operationName, operation)
}

/**
 * Handle Result and convert exceptions to AdminError
 */
fun <T> Result<T>.toAdminResult(): Result<T> {
    return this.fold(
        onSuccess = { Result.success(it) },
        onFailure = { exception ->
            val adminError = exception.toAdminError()
            Result.failure(Exception(adminError.toUserMessage(), exception))
        }
    )
}
