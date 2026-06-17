package com.example.loginandregistration.admin.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.loginandregistration.admin.repository.AdminRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker for archiving old activity logs
 * Runs monthly to archive logs older than 1 year
 * Requirements: 5.11
 */
class ActivityLogArchiveWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ActivityLogArchiveWorker"
        const val WORK_NAME = "activity_log_archive_work"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting activity log archiving job")
            
            val repository = AdminRepository()
            
            // Check if user is admin
            if (!repository.isAdminUser()) {
                Log.w(TAG, "Archiving job skipped - not admin user")
                return@withContext Result.success()
            }
            
            // Get count of archivable logs
            val countResult = repository.getArchivableLogsCount()
            if (countResult.isFailure) {
                Log.e(TAG, "Failed to get archivable logs count", countResult.exceptionOrNull())
                return@withContext Result.retry()
            }
            
            val count = countResult.getOrNull() ?: 0
            Log.d(TAG, "Found $count logs eligible for archiving")
            
            if (count == 0) {
                Log.d(TAG, "No logs to archive")
                return@withContext Result.success()
            }
            
            // Archive old logs
            val archiveResult = repository.archiveOldLogs()
            
            if (archiveResult.isSuccess) {
                val archivedCount = archiveResult.getOrNull() ?: 0
                Log.d(TAG, "Successfully archived $archivedCount activity logs")
                Result.success()
            } else {
                Log.e(TAG, "Failed to archive logs", archiveResult.exceptionOrNull())
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in activity log archiving job", e)
            Result.retry()
        }
    }
}
