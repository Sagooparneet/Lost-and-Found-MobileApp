package com.example.loginandregistration.admin.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.loginandregistration.admin.workers.ActivityLogArchiveWorker
import java.util.concurrent.TimeUnit

/**
 * Scheduler for activity log archiving worker
 * Schedules monthly archiving of logs older than 1 year
 * Requirements: 5.11
 */
object ActivityLogArchiveScheduler {
    
    private const val TAG = "ActivityLogArchiveScheduler"
    
    /**
     * Schedule monthly activity log archiving
     * Runs on the 1st day of each month at 2 AM
     */
    fun scheduleMonthlyArchiving(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            
            // Schedule to run monthly (approximately every 30 days)
            val archiveRequest = PeriodicWorkRequestBuilder<ActivityLogArchiveWorker>(
                30, TimeUnit.DAYS,
                1, TimeUnit.DAYS // Flex interval
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag("activity_log_archive")
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                ActivityLogArchiveWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                archiveRequest
            )
            
            Log.d(TAG, "Monthly activity log archiving scheduled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule activity log archiving", e)
        }
    }
    
    /**
     * Cancel scheduled archiving
     */
    fun cancelArchiving(context: Context) {
        try {
            WorkManager.getInstance(context)
                .cancelUniqueWork(ActivityLogArchiveWorker.WORK_NAME)
            Log.d(TAG, "Activity log archiving cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel activity log archiving", e)
        }
    }
    
    /**
     * Trigger immediate archiving (for testing or manual execution)
     */
    fun triggerImmediateArchiving(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val archiveRequest = OneTimeWorkRequestBuilder<ActivityLogArchiveWorker>()
                .setConstraints(constraints)
                .addTag("activity_log_archive_immediate")
                .build()
            
            WorkManager.getInstance(context).enqueue(archiveRequest)
            
            Log.d(TAG, "Immediate activity log archiving triggered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger immediate archiving", e)
        }
    }
    
    /**
     * Get archiving work status
     * Note: This returns LiveData that can be observed
     */
    fun getArchivingWorkStatus(context: Context): androidx.lifecycle.LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(ActivityLogArchiveWorker.WORK_NAME)
    }
}
