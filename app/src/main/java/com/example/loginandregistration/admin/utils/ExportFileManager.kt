package com.example.loginandregistration.admin.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.loginandregistration.admin.models.ExportFormat
import com.example.loginandregistration.admin.models.ExportRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Export file manager for handling file storage, sharing, and history
 * Requirements: 4.2, 4.3, 4.9
 */
class ExportFileManager(
    private val context: Context,
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val EXPORTS_COLLECTION = "exports"
        private const val MAX_EXPORT_AGE_DAYS = 30 // Auto-delete exports older than 30 days
    }

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    /**
     * Gets the export directory
     */
    fun getExportDirectory(): File {
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
     * Saves export metadata to Firestore for history tracking
     * Requirements: 4.2, 4.3
     */
    suspend fun saveExportHistory(exportRequest: ExportRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(EXPORTS_COLLECTION)
                .document(exportRequest.id)
                .set(exportRequest.toMap())
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save export history: ${e.message}", e))
        }
    }

    /**
     * Gets export history from Firestore
     * Requirements: 4.2, 4.3
     */
    suspend fun getExportHistory(limit: Int = 50): Result<List<ExportRequest>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(EXPORTS_COLLECTION)
                .orderBy("requestedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val exports = snapshot.documents.mapNotNull { doc ->
                try {
                    ExportRequest(
                        id = doc.id,
                        format = ExportFormat.valueOf(doc.getString("format") ?: "PDF"),
                        dataType = com.example.loginandregistration.admin.models.ExportDataType.valueOf(
                            doc.getString("dataType") ?: "COMPREHENSIVE"
                        ),
                        dateRange = com.example.loginandregistration.admin.models.DateRange(
                            startDate = doc.getLong("dateRange.startDate") ?: 0,
                            endDate = doc.getLong("dateRange.endDate") ?: System.currentTimeMillis()
                        ),
                        filters = doc.get("filters") as? Map<String, String> ?: emptyMap(),
                        requestedBy = doc.getString("requestedBy") ?: "",
                        requestedAt = doc.getLong("requestedAt") ?: 0,
                        status = com.example.loginandregistration.admin.models.ExportStatus.valueOf(
                            doc.getString("status") ?: "PENDING"
                        ),
                        fileUrl = doc.getString("fileUrl") ?: "",
                        fileName = doc.getString("fileName") ?: "",
                        completedAt = doc.getLong("completedAt") ?: 0,
                        errorMessage = doc.getString("errorMessage") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(exports)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get export history: ${e.message}", e))
        }
    }

    /**
     * Shares an export file using Android share intent
     * Requirements: 4.2, 4.3
     */
    fun shareExportFile(filePath: String, format: ExportFormat): Result<Intent> {
        return try {
            val file = File(filePath)
            
            if (!file.exists()) {
                return Result.failure(Exception("Export file not found"))
            }
            
            // Use FileProvider to get a content URI
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = format.getMimeType()
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Lost & Found Export - ${file.name}")
                putExtra(Intent.EXTRA_TEXT, "Exported data from Lost & Found Admin System")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, "Share Export File")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            Result.success(chooserIntent)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to share export file: ${e.message}", e))
        }
    }

    /**
     * Opens an export file with appropriate app
     * Requirements: 4.2, 4.3
     */
    fun openExportFile(filePath: String, format: ExportFormat): Result<Intent> {
        return try {
            val file = File(filePath)
            
            if (!file.exists()) {
                return Result.failure(Exception("Export file not found"))
            }
            
            // Use FileProvider to get a content URI
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, format.getMimeType())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            Result.success(openIntent)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to open export file: ${e.message}", e))
        }
    }

    /**
     * Deletes an export file from storage
     * Requirements: 4.2, 4.3
     */
    suspend fun deleteExportFile(exportId: String, filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete the physical file
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
            
            // Delete from Firestore history
            firestore.collection(EXPORTS_COLLECTION)
                .document(exportId)
                .delete()
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete export file: ${e.message}", e))
        }
    }

    /**
     * Cleans up old export files
     * Requirements: 4.2, 4.3
     */
    suspend fun cleanupOldExports(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val directory = getExportDirectory()
            val cutoffTime = System.currentTimeMillis() - (MAX_EXPORT_AGE_DAYS * 24 * 60 * 60 * 1000L)
            
            var deletedCount = 0
            
            // Get old exports from Firestore
            val snapshot = firestore.collection(EXPORTS_COLLECTION)
                .whereLessThan("requestedAt", cutoffTime)
                .get()
                .await()
            
            snapshot.documents.forEach { doc ->
                try {
                    val fileUrl = doc.getString("fileUrl") ?: ""
                    if (fileUrl.isNotEmpty()) {
                        val file = File(fileUrl)
                        if (file.exists()) {
                            file.delete()
                            deletedCount++
                        }
                    }
                    
                    // Delete from Firestore
                    doc.reference.delete().await()
                } catch (e: Exception) {
                    // Continue with next file even if one fails
                }
            }
            
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to cleanup old exports: ${e.message}", e))
        }
    }

    /**
     * Gets the size of all export files in MB
     */
    fun getExportDirectorySize(): Float {
        val directory = getExportDirectory()
        var totalSize = 0L
        
        directory.listFiles()?.forEach { file ->
            if (file.isFile) {
                totalSize += file.length()
            }
        }
        
        return totalSize / (1024f * 1024f) // Convert to MB
    }

    /**
     * Checks if there's enough storage space for export
     * Requirements: 4.9
     */
    fun hasEnoughStorageSpace(estimatedSizeMB: Float = 10f): Boolean {
        val directory = getExportDirectory()
        val freeSpace = directory.freeSpace / (1024f * 1024f) // Convert to MB
        return freeSpace > estimatedSizeMB
    }

    /**
     * Gets a list of all export files in the directory
     */
    fun getAllExportFiles(): List<File> {
        val directory = getExportDirectory()
        return directory.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Formats file size for display
     */
    fun formatFileSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes < 1024 -> "$sizeInBytes B"
            sizeInBytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f KB", sizeInBytes / 1024f)
            else -> String.format(Locale.getDefault(), "%.2f MB", sizeInBytes / (1024f * 1024f))
        }
    }

    /**
     * Gets file info for display
     */
    fun getFileInfo(file: File): Map<String, String> {
        return mapOf(
            "name" to file.name,
            "size" to formatFileSize(file.length()),
            "modified" to dateFormat.format(Date(file.lastModified())),
            "path" to file.absolutePath
        )
    }
}
