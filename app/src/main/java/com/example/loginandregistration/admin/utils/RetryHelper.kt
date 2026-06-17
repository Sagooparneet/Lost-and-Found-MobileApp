package com.example.loginandregistration.admin.utils

import android.util.Log
import com.example.loginandregistration.admin.models.AdminError
import com.example.loginandregistration.admin.models.toAdminError
import kotlinx.coroutines.delay

/**
 * Helper class for implementing retry logic with exponential backoff
 * Requirements: 10.2, 10.3
 */
object RetryHelper {
    private const val TAG = "RetryHelper"
    
    /**
     * Retry an operation with exponential backoff
     * 
     * @param maxRetries Maximum number of retry attempts (default: 3)
     * @param initialDelay Initial delay in milliseconds (default: 1000ms)
     * @param maxDelay Maximum delay in milliseconds (default: 10000ms)
     * @param factor Exponential backoff factor (default: 2.0)
     * @param operation The suspend function to retry
     * @return Result containing the operation result or AdminError
     */
    suspend fun <T> retryOperation(
        maxRetries: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        operation: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelay
        var lastException: Throwable? = null
        
        repeat(maxRetries) { attempt ->
            try {
                Log.d(TAG, "Attempting operation (attempt ${attempt + 1}/$maxRetries)")
                val result = operation()
                if (attempt > 0) {
                    Log.d(TAG, "Operation succeeded after ${attempt + 1} attempts")
                }
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e
                val adminError = e.toAdminError()
                
                Log.w(TAG, "Operation failed (attempt ${attempt + 1}/$maxRetries): ${adminError.message}", e)
                
                // Check if error is retryable
                if (!adminError.isRetryable()) {
                    Log.d(TAG, "Error is not retryable, failing immediately")
                    return Result.failure(e)
                }
                
                // If this was the last attempt, don't delay
                if (attempt == maxRetries - 1) {
                    Log.e(TAG, "Max retries exceeded, operation failed", e)
                    return Result.failure(e)
                }
                
                // Wait before retrying
                Log.d(TAG, "Waiting ${currentDelay}ms before retry")
                delay(currentDelay)
                
                // Calculate next delay with exponential backoff
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        
        // This should never be reached, but just in case
        return Result.failure(lastException ?: Exception("Operation failed after $maxRetries attempts"))
    }
    
    /**
     * Retry an operation that returns Result<T>
     * 
     * @param maxRetries Maximum number of retry attempts (default: 3)
     * @param initialDelay Initial delay in milliseconds (default: 1000ms)
     * @param maxDelay Maximum delay in milliseconds (default: 10000ms)
     * @param factor Exponential backoff factor (default: 2.0)
     * @param operation The suspend function that returns Result<T>
     * @return Result containing the operation result or error
     */
    suspend fun <T> retryResultOperation(
        maxRetries: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        operation: suspend () -> Result<T>
    ): Result<T> {
        var currentDelay = initialDelay
        var lastResult: Result<T>? = null
        
        repeat(maxRetries) { attempt ->
            Log.d(TAG, "Attempting operation (attempt ${attempt + 1}/$maxRetries)")
            val result = operation()
            
            if (result.isSuccess) {
                if (attempt > 0) {
                    Log.d(TAG, "Operation succeeded after ${attempt + 1} attempts")
                }
                return result
            }
            
            lastResult = result
            val exception = result.exceptionOrNull()
            val adminError = exception?.toAdminError()
            
            Log.w(TAG, "Operation failed (attempt ${attempt + 1}/$maxRetries): ${adminError?.message}", exception)
            
            // Check if error is retryable
            if (adminError != null && !adminError.isRetryable()) {
                Log.d(TAG, "Error is not retryable, failing immediately")
                return result
            }
            
            // If this was the last attempt, don't delay
            if (attempt == maxRetries - 1) {
                Log.e(TAG, "Max retries exceeded, operation failed", exception)
                return result
            }
            
            // Wait before retrying
            Log.d(TAG, "Waiting ${currentDelay}ms before retry")
            delay(currentDelay)
            
            // Calculate next delay with exponential backoff
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        
        // Return the last result
        return lastResult ?: Result.failure(Exception("Operation failed after $maxRetries attempts"))
    }
    
    /**
     * Retry a Firestore operation with appropriate defaults
     */
    suspend fun <T> retryFirestoreOperation(
        operation: suspend () -> T
    ): Result<T> {
        return retryOperation(
            maxRetries = 3,
            initialDelay = 500,
            maxDelay = 5000,
            factor = 2.0,
            operation = operation
        )
    }
    
    /**
     * Retry a network operation with appropriate defaults
     */
    suspend fun <T> retryNetworkOperation(
        operation: suspend () -> T
    ): Result<T> {
        return retryOperation(
            maxRetries = 3,
            initialDelay = 1000,
            maxDelay = 10000,
            factor = 2.0,
            operation = operation
        )
    }
}
