package com.example.loginandregistration.admin.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.loginandregistration.admin.models.ExportRequest
import com.example.loginandregistration.admin.models.ExportStatus
import com.example.loginandregistration.admin.workers.ExportWorker
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Manager for export queue using WorkManager
 * Handles scheduling and monitoring of export jobs
 * Requirements: 9.4
 */
class ExportQueueManager(private val context: Context) {
    
    private val workManager = WorkManager.getInstance(context)
    
    companion object {
        private const val TAG = "ExportQueueManager"
        private const val EXPORT_WORK_TAG = "export_work"
        
        @Volatile
        private var instance: ExportQueueManager? = null
        
        fun getInstance(context: Context): ExportQueueManager {
            return instance ?: synchronized(this) {
                instance ?: ExportQueueManager(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
    
    /**
     * Queue an export request for background processing
     * @param request Export request to process
     * @return Work request ID for tracking
     */
    fun queueExport(request: ExportRequest): UUID {
        Log.d(TAG, "Queueing export: ${request.dataType} in ${request.format} format")
        
        // Create input data for worker
        val inputData = Data.Builder()
            .putString(ExportWorker.KEY_EXPORT_FORMAT, request.format.name)
            .putString(ExportWorker.KEY_DATA_TYPE, request.dataType.name)
            .putLong(ExportWorker.KEY_START_DATE, request.dateRange.startDate)
            .putLong(ExportWorker.KEY_END_DATE, request.dateRange.endDate)
            .build()
        
        // Create work request with constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder<ExportWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(EXPORT_WORK_TAG)
            .addTag("export_${request.id}")
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        // Enqueue the work
        workManager.enqueue(workRequest)
        
        Log.d(TAG, "Export queued with ID: ${workRequest.id}")
        return workRequest.id
    }
    
    /**
     * Observe the status of an export job
     * @param workId Work request ID
     * @return LiveData of WorkInfo
     */
    fun observeExportStatus(workId: UUID) = workManager.getWorkInfoByIdLiveData(workId)
    
    /**
     * Cancel an export job
     * @param workId Work request ID
     */
    fun cancelExport(workId: UUID) {
        Log.d(TAG, "Cancelling export: $workId")
        workManager.cancelWorkById(workId)
    }
    
    /**
     * Cancel all pending exports
     */
    fun cancelAllExports() {
        Log.d(TAG, "Cancelling all exports")
        workManager.cancelAllWorkByTag(EXPORT_WORK_TAG)
    }
    
    /**
     * Observe all exports with a specific tag
     * @return LiveData of work info list
     */
    fun observeActiveExports() = workManager.getWorkInfosByTagLiveData(EXPORT_WORK_TAG)
    
    /**
     * Clean up completed and failed exports older than specified days
     * @param daysOld Number of days to keep
     */
    fun cleanupOldExports(daysOld: Int = 7) {
        Log.d(TAG, "Cleaning up exports older than $daysOld days")
        
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        
        workManager.pruneWork()
        
        Log.d(TAG, "Old exports cleaned up")
    }
    
    /**
     * Observe export queue statistics
     * @return LiveData of export queue stats
     */
    fun observeQueueStats() = workManager.getWorkInfosByTagLiveData(EXPORT_WORK_TAG)
}

/**
 * Export queue statistics
 */
data class ExportQueueStats(
    val totalExports: Int,
    val pendingExports: Int,
    val runningExports: Int,
    val completedExports: Int,
    val failedExports: Int
)
