package com.example.loginandregistration.admin.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.loginandregistration.admin.workers.DonationAutoFlagWorker
import java.util.concurrent.TimeUnit

/**
 * Utility class to schedule donation auto-flagging background work
 * Requirements: 3.1, 3.8
 */
object DonationWorkScheduler {
    
    private const val TAG = "DonationWorkScheduler"
    
    /**
     * Schedule daily work to check and flag old items for donation
     * Runs at midnight every day
     */
    fun scheduleDailyAutoFlag(context: Context) {
        try {
            // Calculate initial delay to run at midnight
            val currentTime = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = currentTime
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                
                // If midnight has passed today, schedule for tomorrow
                if (timeInMillis <= currentTime) {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }
            
            val initialDelay = calendar.timeInMillis - currentTime
            
            // Create constraints - only run when device is idle and charging
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            
            // Create periodic work request - runs every 24 hours
            val workRequest = PeriodicWorkRequestBuilder<DonationAutoFlagWorker>(
                24, TimeUnit.HOURS,
                15, TimeUnit.MINUTES // Flex interval
            )
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag("donation_auto_flag")
                .build()
            
            // Enqueue the work with replace policy
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DonationAutoFlagWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
                workRequest
            )
            
            Log.d(TAG, "Scheduled daily auto-flag work with initial delay of ${initialDelay / 1000 / 60} minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling daily auto-flag work", e)
        }
    }
    
    /**
     * Cancel the scheduled auto-flag work
     */
    fun cancelAutoFlag(context: Context) {
        try {
            WorkManager.getInstance(context)
                .cancelUniqueWork(DonationAutoFlagWorker.WORK_NAME)
            Log.d(TAG, "Cancelled auto-flag work")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling auto-flag work", e)
        }
    }
    
    /**
     * Run the auto-flag work immediately (for testing)
     */
    fun runAutoFlagNow(context: Context) {
        try {
            val workRequest = OneTimeWorkRequestBuilder<DonationAutoFlagWorker>()
                .addTag("donation_auto_flag_manual")
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "Triggered immediate auto-flag work")
        } catch (e: Exception) {
            Log.e(TAG, "Error running immediate auto-flag work", e)
        }
    }
    
    /**
     * Check the status of the scheduled work
     * Returns LiveData that can be observed
     */
    fun getWorkStatus(context: Context): androidx.lifecycle.LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(DonationAutoFlagWorker.WORK_NAME)
    }
}
