package com.example.loginandregistration.admin.utils

import android.content.Context
import android.os.Environment
import com.example.loginandregistration.admin.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * CSV Export Generator for admin reports
 * Requirements: 4.3, 4.9
 */
class CsvExportGenerator(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Gets the export directory for CSV files
     */
    private fun getExportDirectory(): File {
        val directory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "LostFoundExports"
        )
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    /**
     * Generates a unique filename for the export
     */
    private fun generateFileName(dataType: ExportDataType): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        return "${dataType.getFileNamePrefix()}_${timestamp}.csv"
    }

    /**
     * Escapes CSV special characters
     * Requirements: 4.9
     */
    private fun escapeCsv(value: String): String {
        // If the value contains comma, quote, or newline, wrap it in quotes
        // and escape any quotes inside by doubling them
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Writes a CSV row
     */
    private fun writeCsvRow(writer: FileWriter, values: List<String>) {
        val escapedValues = values.map { escapeCsv(it) }
        writer.write(escapedValues.joinToString(","))
        writer.write("\n")
    }

    /**
     * Formats a timestamp to a readable date string
     */
    private fun formatDate(timestamp: Long): String {
        return if (timestamp > 0) {
            dateOnlyFormat.format(Date(timestamp))
        } else {
            ""
        }
    }

    /**
     * Formats a timestamp to a readable date-time string
     */
    private fun formatDateTime(timestamp: Long): String {
        return if (timestamp > 0) {
            dateFormat.format(Date(timestamp))
        } else {
            ""
        }
    }

    /**
     * Formats a Firebase Timestamp to a readable date-time string
     */
    private fun formatDateTime(timestamp: com.google.firebase.Timestamp?): String {
        return if (timestamp != null) {
            dateFormat.format(timestamp.toDate())
        } else {
            ""
        }
    }

    /**
     * Generates a CSV export for items
     * Requirements: 4.3, 4.9
     */
    suspend fun generateItemsCsv(
        items: List<EnhancedLostFoundItem>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val directory = getExportDirectory()
            val fileName = generateFileName(ExportDataType.ITEMS)
            val file = File(directory, fileName)
            
            FileWriter(file).use { writer ->
                // Write header
                val headers = listOf(
                    "ID",
                    "Name",
                    "Description",
                    "Category",
                    "Type",
                    "Status",
                    "Location",
                    "Contact Info",
                    "Reporter Email",
                    "Reporter ID",
                    "Date Reported",
                    "Requested By",
                    "Requested At",
                    "Returned At",
                    "Donation Eligible At",
                    "Donated At",
                    "Last Modified By",
                    "Last Modified At",
                    "Image URL"
                )
                writeCsvRow(writer, headers)
                
                // Write data rows
                items.forEach { item ->
                    val row = listOf(
                        item.id,
                        item.name,
                        item.description,
                        item.category,
                        if (item.isLost) "Lost" else "Found",
                        item.status.name,
                        item.location,
                        item.contactInfo,
                        item.userEmail,
                        item.userId,
                        formatDateTime(item.timestamp.toDate().time),
                        item.requestedBy,
                        formatDateTime(item.requestedAt),
                        formatDateTime(item.returnedAt),
                        formatDateTime(item.donationEligibleAt),
                        formatDateTime(item.donatedAt),
                        item.lastModifiedBy,
                        formatDateTime(item.lastModifiedAt),
                        item.imageUrl
                    )
                    writeCsvRow(writer, row)
                }
            }
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to generate items CSV: ${e.message}", e))
        }
    }

    /**
     * Generates a CSV export for users
     * Requirements: 4.3, 4.9
     */
    suspend fun generateUsersCsv(
        users: List<EnhancedAdminUser>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val directory = getExportDirectory()
            val fileName = generateFileName(ExportDataType.USERS)
            val file = File(directory, fileName)
            
            FileWriter(file).use { writer ->
                // Write header
                val headers = listOf(
                    "UID",
                    "Email",
                    "Display Name",
                    "Role",
                    "Status",
                    "Items Reported",
                    "Items Found",
                    "Items Claimed",
                    "Created At",
                    "Last Login At",
                    "Last Activity At",
                    "Is Blocked",
                    "Block Reason",
                    "Blocked By",
                    "Blocked At",
                    "Device Info",
                    "Photo URL"
                )
                writeCsvRow(writer, headers)
                
                // Write data rows
                users.forEach { user ->
                    val row = listOf(
                        user.uid,
                        user.email,
                        user.displayName,
                        user.role.name,
                        if (user.isBlocked) "Blocked" else "Active",
                        user.itemsReported.toString(),
                        user.itemsFound.toString(),
                        user.itemsClaimed.toString(),
                        formatDateTime(user.createdAt),
                        formatDateTime(user.lastLoginAt),
                        formatDateTime(user.lastActivityAt),
                        user.isBlocked.toString(),
                        user.blockReason,
                        user.blockedBy,
                        formatDateTime(user.blockedAt),
                        user.deviceInfo,
                        user.photoUrl
                    )
                    writeCsvRow(writer, row)
                }
            }
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to generate users CSV: ${e.message}", e))
        }
    }

    /**
     * Generates a CSV export for activity logs
     * Requirements: 4.3, 4.9
     */
    suspend fun generateActivityLogsCsv(
        logs: List<ActivityLog>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val directory = getExportDirectory()
            val fileName = generateFileName(ExportDataType.ACTIVITIES)
            val file = File(directory, fileName)
            
            FileWriter(file).use { writer ->
                // Write header
                val headers = listOf(
                    "ID",
                    "Timestamp",
                    "Actor ID",
                    "Actor Email",
                    "Actor Role",
                    "Action Type",
                    "Target Type",
                    "Target ID",
                    "Description",
                    "Previous Value",
                    "New Value",
                    "IP Address",
                    "Device Info"
                )
                writeCsvRow(writer, headers)
                
                // Write data rows
                logs.forEach { log ->
                    val row = listOf(
                        log.id,
                        formatDateTime(log.timestamp),
                        log.actorId,
                        log.actorEmail,
                        log.actorRole.name,
                        log.actionType.name,
                        log.targetType.name,
                        log.targetId,
                        log.description,
                        log.previousValue,
                        log.newValue,
                        log.ipAddress,
                        log.deviceInfo
                    )
                    writeCsvRow(writer, row)
                }
            }
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to generate activity logs CSV: ${e.message}", e))
        }
    }
}
