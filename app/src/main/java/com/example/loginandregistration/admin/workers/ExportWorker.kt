package com.example.loginandregistration.admin.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import androidx.work.workDataOf
import com.example.loginandregistration.admin.models.*
import com.example.loginandregistration.admin.repository.AdminRepository
import com.example.loginandregistration.admin.utils.PdfExportGenerator
import com.example.loginandregistration.admin.utils.ExportFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Background worker for processing large export operations
 * Uses WorkManager to handle export generation in the background
 * Requirements: 9.4
 */
class ExportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val repository = AdminRepository()
    private val pdfGenerator = PdfExportGenerator(context)
    private val fileManager = ExportFileManager(context, com.google.firebase.firestore.FirebaseFirestore.getInstance())
    
    companion object {
        private const val TAG = "ExportWorker"
        
        // Input data keys
        const val KEY_EXPORT_FORMAT = "export_format"
        const val KEY_DATA_TYPE = "export_data_type"
        const val KEY_START_DATE = "start_date"
        const val KEY_END_DATE = "end_date"
        const val KEY_FILTERS = "filters"
        
        // Output data keys
        const val KEY_FILE_PATH = "file_path"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val KEY_PROGRESS = "progress"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting export worker")
            
            // Get input parameters
            val format = inputData.getString(KEY_EXPORT_FORMAT) ?: "PDF"
            val dataType = inputData.getString(KEY_DATA_TYPE) ?: "ITEMS"
            val startDate = inputData.getLong(KEY_START_DATE, 0L)
            val endDate = inputData.getLong(KEY_END_DATE, System.currentTimeMillis())
            
            // Update progress
            setProgress(workDataOf(KEY_PROGRESS to 10))
            
            // Create export request
            val exportRequest = ExportRequest(
                format = ExportFormat.valueOf(format),
                dataType = ExportDataType.valueOf(dataType),
                dateRange = DateRange(startDate, endDate),
                requestedBy = "admin",
                status = ExportStatus.PROCESSING
            )
            
            Log.d(TAG, "Processing export: format=$format, dataType=$dataType")
            
            // Generate export based on format and data type
            val filePath = when (exportRequest.format) {
                ExportFormat.PDF -> generatePdfExport(exportRequest)
                ExportFormat.CSV -> generateCsvExport(exportRequest)
                ExportFormat.EXCEL -> {
                    // Excel export not implemented yet, fallback to CSV
                    Log.w(TAG, "Excel format not implemented, using CSV instead")
                    generateCsvExport(exportRequest)
                }
            }
            
            // Update progress
            setProgress(workDataOf(KEY_PROGRESS to 100))
            
            Log.d(TAG, "Export completed successfully: $filePath")
            
            // Return success with file path
            Result.success(workDataOf(KEY_FILE_PATH to filePath))
            
        } catch (e: Exception) {
            Log.e(TAG, "Export worker failed", e)
            Result.failure(workDataOf(KEY_ERROR_MESSAGE to (e.message ?: "Unknown error")))
        }
    }
    
    /**
     * Generate PDF export
     */
    private suspend fun generatePdfExport(request: ExportRequest): String {
        setProgress(workDataOf(KEY_PROGRESS to 20))
        
        val generatedBy = request.requestedBy
        
        val result = when (request.dataType) {
            ExportDataType.ITEMS -> {
                val items = fetchItems(request.dateRange)
                setProgress(workDataOf(KEY_PROGRESS to 50))
                pdfGenerator.generateItemsReport(items, request.dateRange, generatedBy)
            }
            ExportDataType.USERS -> {
                val users = fetchUsers()
                setProgress(workDataOf(KEY_PROGRESS to 50))
                pdfGenerator.generateUsersReport(users, request.dateRange, generatedBy)
            }
            ExportDataType.ACTIVITIES -> {
                val activities = fetchActivityLogs(request.dateRange)
                setProgress(workDataOf(KEY_PROGRESS to 50))
                pdfGenerator.generateActivityReport(activities, request.dateRange, generatedBy)
            }
            ExportDataType.DONATIONS -> {
                // Donations report not implemented yet, use items as fallback
                val items = fetchItems(request.dateRange)
                setProgress(workDataOf(KEY_PROGRESS to 50))
                pdfGenerator.generateItemsReport(items, request.dateRange, generatedBy)
            }
            ExportDataType.COMPREHENSIVE -> {
                val items = fetchItems(request.dateRange)
                setProgress(workDataOf(KEY_PROGRESS to 30))
                val users = fetchUsers()
                setProgress(workDataOf(KEY_PROGRESS to 50))
                val activities = fetchActivityLogs(request.dateRange)
                setProgress(workDataOf(KEY_PROGRESS to 60))
                val donations = fetchDonations(request.dateRange)
                setProgress(workDataOf(KEY_PROGRESS to 70))
                val donationStats = DonationStats() // Empty stats for now
                pdfGenerator.generateComprehensiveReport(
                    items, users, activities, donations, donationStats, request.dateRange, generatedBy
                )
            }
        }
        
        return result.getOrThrow()
    }
    
    /**
     * Generate CSV export
     */
    private suspend fun generateCsvExport(request: ExportRequest): String {
        setProgress(workDataOf(KEY_PROGRESS to 20))
        
        val csvContent = when (request.dataType) {
            ExportDataType.ITEMS -> {
                val items = fetchItems(request.dateRange)
                setProgress(workDataOf(KEY_PROGRESS to 50))
                generateItemsCsv(items)
            }
            ExportDataType.USERS -> {
                val users = fetchUsers()
                setProgress(workDataOf(KEY_PROGRESS to 50))
                generateUsersCsv(users)
            }
            ExportDataType.ACTIVITIES -> {
                val activities = fetchActivityLogs(request.dateRange)
                setProgress(workDataOf(KEY_PROGRESS to 50))
                generateActivityLogsCsv(activities)
            }
            ExportDataType.DONATIONS -> {
                // Donations CSV not implemented yet, use items as fallback
                val items = fetchItems(request.dateRange)
                setProgress(workDataOf(KEY_PROGRESS to 50))
                generateItemsCsv(items)
            }
            ExportDataType.COMPREHENSIVE -> {
                // For comprehensive, combine all data
                val items = fetchItems(request.dateRange)
                setProgress(workDataOf(KEY_PROGRESS to 40))
                val users = fetchUsers()
                setProgress(workDataOf(KEY_PROGRESS to 60))
                // For now, just export items
                generateItemsCsv(items)
            }
        }
        
        setProgress(workDataOf(KEY_PROGRESS to 80))
        
        // Save CSV to file
        val fileName = "export_${request.dataType.name.lowercase()}_${System.currentTimeMillis()}.csv"
        val directory = fileManager.getExportDirectory()
        val file = File(directory, fileName)
        file.writeText(csvContent)
        
        return file.absolutePath
    }
    
    /**
     * Generate CSV for items
     */
    private fun generateItemsCsv(items: List<EnhancedLostFoundItem>): String {
        val header = "ID,Name,Description,Category,Type,Status,Location,Reporter,Date\n"
        val rows = items.joinToString("\n") { item ->
            "${item.id},${escapeCsv(item.name)},${escapeCsv(item.description)},${escapeCsv(item.category)}," +
            "${if (item.isLost) "Lost" else "Found"},${item.status.name},${escapeCsv(item.location)}," +
            "${escapeCsv(item.userEmail)},${item.timestamp.toDate()}"
        }
        return header + rows
    }
    
    /**
     * Generate CSV for users
     */
    private fun generateUsersCsv(users: List<EnhancedAdminUser>): String {
        val header = "ID,Email,Display Name,Role,Status,Items Reported,Items Found,Items Claimed,Created At\n"
        val rows = users.joinToString("\n") { user ->
            "${user.uid},${escapeCsv(user.email)},${escapeCsv(user.displayName)},${user.role.name}," +
            "${if (user.isBlocked) "Blocked" else "Active"},${user.itemsReported},${user.itemsFound}," +
            "${user.itemsClaimed},${user.createdAt.toDate()}"
        }
        return header + rows
    }
    
    /**
     * Generate CSV for activity logs
     */
    private fun generateActivityLogsCsv(logs: List<ActivityLog>): String {
        val header = "Timestamp,Actor,Action,Target Type,Target ID,Description\n"
        val rows = logs.joinToString("\n") { log ->
            "${java.util.Date(log.timestamp)},${escapeCsv(log.actorEmail)},${log.actionType.name}," +
            "${log.targetType.name},${escapeCsv(log.targetId)},${escapeCsv(log.description)}"
        }
        return header + rows
    }
    
    /**
     * Escape CSV values
     */
    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
    
    /**
     * Fetch items from repository
     */
    private suspend fun fetchItems(dateRange: DateRange): List<EnhancedLostFoundItem> {
        val result = repository.getItemsOptimized(
            filters = mapOf(
                "startDate" to dateRange.startDate,
                "endDate" to dateRange.endDate
            ),
            limit = Int.MAX_VALUE // No limit for exports
        )
        return result.getOrDefault(emptyList())
    }
    
    /**
     * Fetch users from repository
     */
    private suspend fun fetchUsers(): List<EnhancedAdminUser> {
        val result = repository.getUsersOptimized(
            limit = Int.MAX_VALUE
        )
        return result.getOrDefault(emptyList())
    }
    
    /**
     * Fetch activity logs from repository
     */
    private suspend fun fetchActivityLogs(dateRange: DateRange): List<ActivityLog> {
        val result = repository.getActivityLogsOptimized(
            filters = mapOf(
                "startDate" to dateRange.startDate,
                "endDate" to dateRange.endDate
            ),
            limit = Int.MAX_VALUE
        )
        return result.getOrDefault(emptyList())
    }
    
    /**
     * Fetch donations from repository
     */
    private suspend fun fetchDonations(dateRange: DateRange): List<DonationItem> {
        val result = repository.getDonationsOptimized(
            limit = Int.MAX_VALUE
        )
        return result.getOrDefault(emptyList())
    }
}
