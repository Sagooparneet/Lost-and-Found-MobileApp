package com.example.loginandregistration.admin.models

import java.io.Serializable

/**
 * Export request model for data export functionality
 * Requirements: 4.1, 4.2
 */
data class ExportRequest(
    val id: String = "",
    val format: ExportFormat = ExportFormat.PDF,
    val dataType: ExportDataType = ExportDataType.COMPREHENSIVE,
    val dateRange: DateRange = DateRange(0, System.currentTimeMillis()),
    val filters: Map<String, String> = emptyMap(),
    val requestedBy: String = "",
    val requestedAt: Long = System.currentTimeMillis(),
    val status: ExportStatus = ExportStatus.PENDING,
    val fileUrl: String = "",
    val fileName: String = "",
    val completedAt: Long = 0,
    val errorMessage: String = ""
) : Serializable {
    
    /**
     * Validates if the export request is complete
     */
    fun isValid(): Boolean {
        return requestedBy.isNotBlank() && 
               dateRange.isValid()
    }

    /**
     * Checks if the export is complete
     */
    fun isComplete(): Boolean {
        return status == ExportStatus.COMPLETED
    }

    /**
     * Checks if the export failed
     */
    fun hasFailed(): Boolean {
        return status == ExportStatus.FAILED
    }

    /**
     * Gets the processing time in seconds
     */
    fun getProcessingTimeSeconds(): Long {
        return if (completedAt > 0) {
            (completedAt - requestedAt) / 1000
        } else {
            0
        }
    }

    /**
     * Converts to map for Firestore storage
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "format" to format.name,
            "dataType" to dataType.name,
            "dateRange" to dateRange.toMap(),
            "filters" to filters,
            "requestedBy" to requestedBy,
            "requestedAt" to requestedAt,
            "status" to status.name,
            "fileUrl" to fileUrl,
            "fileName" to fileName,
            "completedAt" to completedAt,
            "errorMessage" to errorMessage
        )
    }
}

/**
 * Export format enum
 * Requirements: 4.2, 4.3
 */
enum class ExportFormat {
    PDF,
    CSV,
    EXCEL;

    /**
     * Gets the file extension for this format
     */
    fun getFileExtension(): String {
        return when (this) {
            PDF -> ".pdf"
            CSV -> ".csv"
            EXCEL -> ".xlsx"
        }
    }

    /**
     * Gets the MIME type for this format
     */
    fun getMimeType(): String {
        return when (this) {
            PDF -> "application/pdf"
            CSV -> "text/csv"
            EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        }
    }

    /**
     * Gets a human-readable description
     */
    fun getDisplayName(): String {
        return when (this) {
            PDF -> "PDF Document"
            CSV -> "CSV Spreadsheet"
            EXCEL -> "Excel Spreadsheet"
        }
    }
}

/**
 * Export data type enum
 * Requirements: 4.1, 4.2
 */
enum class ExportDataType {
    ITEMS,
    USERS,
    ACTIVITIES,
    DONATIONS,
    COMPREHENSIVE;

    /**
     * Gets a human-readable description
     */
    fun getDisplayName(): String {
        return when (this) {
            ITEMS -> "Items Report"
            USERS -> "Users Report"
            ACTIVITIES -> "Activity Logs"
            DONATIONS -> "Donations Report"
            COMPREHENSIVE -> "Comprehensive Report"
        }
    }

    /**
     * Gets the default filename prefix
     */
    fun getFileNamePrefix(): String {
        return when (this) {
            ITEMS -> "items_report"
            USERS -> "users_report"
            ACTIVITIES -> "activity_logs"
            DONATIONS -> "donations_report"
            COMPREHENSIVE -> "comprehensive_report"
        }
    }
}

/**
 * Export status enum
 * Requirements: 4.1
 */
enum class ExportStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    /**
     * Checks if the status indicates the export is in progress
     */
    fun isInProgress(): Boolean {
        return this == PENDING || this == PROCESSING
    }

    /**
     * Gets a human-readable description
     */
    fun getDisplayName(): String {
        return when (this) {
            PENDING -> "Pending"
            PROCESSING -> "Processing"
            COMPLETED -> "Completed"
            FAILED -> "Failed"
        }
    }
}

/**
 * Date range model for filtering exports
 * Requirements: 4.8
 */
data class DateRange(
    val startDate: Long = 0,
    val endDate: Long = System.currentTimeMillis()
) : Serializable {
    
    /**
     * Validates if the date range is valid
     */
    fun isValid(): Boolean {
        return startDate >= 0 && endDate >= startDate
    }

    /**
     * Gets the duration in days
     */
    fun getDurationInDays(): Long {
        val durationMillis = endDate - startDate
        return durationMillis / (24 * 60 * 60 * 1000)
    }

    /**
     * Checks if a timestamp falls within this range
     */
    fun contains(timestamp: Long): Boolean {
        return timestamp in startDate..endDate
    }

    /**
     * Converts to map for Firestore storage
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "startDate" to startDate,
            "endDate" to endDate
        )
    }

    companion object {
        /**
         * Creates a date range for the last N days
         */
        fun lastNDays(days: Int): DateRange {
            val endDate = System.currentTimeMillis()
            val startDate = endDate - (days * 24 * 60 * 60 * 1000L)
            return DateRange(startDate, endDate)
        }

        /**
         * Creates a date range for the current month
         */
        fun currentMonth(): DateRange {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startDate = calendar.timeInMillis
            val endDate = System.currentTimeMillis()
            return DateRange(startDate, endDate)
        }

        /**
         * Creates a date range for all time
         */
        fun allTime(): DateRange {
            return DateRange(0, System.currentTimeMillis())
        }
    }
}
