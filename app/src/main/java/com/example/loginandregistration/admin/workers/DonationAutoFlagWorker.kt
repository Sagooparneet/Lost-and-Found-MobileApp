package com.example.loginandregistration.admin.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.loginandregistration.admin.models.ItemStatus
import com.example.loginandregistration.admin.repository.AdminRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker to automatically flag items older than 1 year for donation
 * Requirements: 3.1, 3.8
 */
class DonationAutoFlagWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val repository = AdminRepository()
    
    companion object {
        private const val TAG = "DonationAutoFlagWorker"
        const val WORK_NAME = "donation_auto_flag_work"
        private const val ONE_YEAR_IN_MILLIS = 365L * 24 * 60 * 60 * 1000 // 1 year in milliseconds
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting auto-flag job for old items")
            
            // Check and flag old items
            val result = repository.checkAndFlagOldItems()
            
            result.fold(
                onSuccess = { count ->
                    Log.d(TAG, "Successfully flagged $count items for donation")
                    Result.success()
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to flag items for donation", error)
                    // Retry on failure
                    Result.retry()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in auto-flag worker", e)
            Result.failure()
        }
    }
}
