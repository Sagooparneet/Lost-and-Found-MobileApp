package com.example.loginandregistration.admin.utils

import android.content.Context
import android.os.Environment
import com.example.loginandregistration.admin.models.*
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * PDF Export Generator for admin reports
 * Requirements: 4.2
 */
class PdfExportGenerator(private val context: Context) {

    companion object {
        private const val FONT_SIZE_TITLE = 20f
        private const val FONT_SIZE_SUBTITLE = 16f
        private const val FONT_SIZE_HEADING = 14f
        private const val FONT_SIZE_NORMAL = 12f
        private const val FONT_SIZE_SMALL = 10f
        
        // Color scheme
        private val COLOR_PRIMARY = DeviceRgb(33, 150, 243) // Blue
        private val COLOR_HEADER = DeviceRgb(66, 66, 66) // Dark gray
        private val COLOR_TEXT = DeviceRgb(33, 33, 33) // Almost black
        private val COLOR_LIGHT_GRAY = DeviceRgb(245, 245, 245) // Light gray
    }

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    /**
     * Gets the export directory for PDF files
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
    private fun generateFileName(dataType: ExportDataType, format: ExportFormat): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        return "${dataType.getFileNamePrefix()}_${timestamp}${format.getFileExtension()}"
    }

    /**
     * Creates a PDF document with standard formatting
     */
    private suspend fun createPdfDocument(
        fileName: String,
        title: String,
        dateRange: DateRange,
        generatedBy: String,
        content: suspend (Document) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val directory = getExportDirectory()
            val file = File(directory, fileName)
            
            // Use try-finally to ensure resources are properly closed
            val writer = PdfWriter(file)
            try {
                val pdfDoc = PdfDocument(writer)
                try {
                    val document = Document(pdfDoc)
                    try {
                        // Add header
                        addDocumentHeader(document, title, dateRange, generatedBy)

                        // Add content
                        content(document)

                        // Add footer
                        addDocumentFooter(document)
                    } finally {
                        document.close()
                    }
                } finally {
                    pdfDoc.close()
                }
            } finally {
                writer.close()
            }
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to generate PDF: ${e.message}", e))
        }
    }

    /**
     * Adds a formatted header to the document
     */
    private fun addDocumentHeader(
        document: Document,
        title: String,
        dateRange: DateRange,
        generatedBy: String
    ) {
        // Title
        val titleParagraph = Paragraph(title)
            .setFontSize(FONT_SIZE_TITLE)
            .setBold()
            .setFontColor(COLOR_PRIMARY)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(10f)
        document.add(titleParagraph)

        // Date range
        val dateRangeText = "Period: ${dateOnlyFormat.format(Date(dateRange.startDate))} - " +
                "${dateOnlyFormat.format(Date(dateRange.endDate))}"
        val dateRangeParagraph = Paragraph(dateRangeText)
            .setFontSize(FONT_SIZE_NORMAL)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(5f)
        document.add(dateRangeParagraph)

        // Generated info
        val generatedText = "Generated: ${dateFormat.format(Date())} | By: $generatedBy"
        val generatedParagraph = Paragraph(generatedText)
            .setFontSize(FONT_SIZE_SMALL)
            .setFontColor(COLOR_HEADER)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20f)
        document.add(generatedParagraph)

        // Separator line
        document.add(Paragraph("\n"))
    }

    /**
     * Adds a formatted footer to the document
     */
    private fun addDocumentFooter(document: Document) {
        document.add(Paragraph("\n"))
        val footerText = "Lost & Found Admin System - Confidential Report"
        val footerParagraph = Paragraph(footerText)
            .setFontSize(FONT_SIZE_SMALL)
            .setFontColor(COLOR_HEADER)
            .setTextAlignment(TextAlignment.CENTER)
            .setItalic()
        document.add(footerParagraph)
    }

    /**
     * Creates a section heading
     */
    private fun createSectionHeading(text: String): Paragraph {
        return Paragraph(text)
            .setFontSize(FONT_SIZE_HEADING)
            .setBold()
            .setFontColor(COLOR_HEADER)
            .setMarginTop(15f)
            .setMarginBottom(10f)
    }

    /**
     * Creates a statistics table with key-value pairs
     */
    private fun createStatsTable(stats: Map<String, String>): Table {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f)))
            .useAllAvailableWidth()
            .setMarginBottom(15f)

        stats.forEach { (key, value) ->
            // Key cell
            val keyCell = Cell()
                .add(Paragraph(key).setFontSize(FONT_SIZE_NORMAL).setBold())
                .setBackgroundColor(COLOR_LIGHT_GRAY)
                .setPadding(8f)
            table.addCell(keyCell)

            // Value cell
            val valueCell = Cell()
                .add(Paragraph(value).setFontSize(FONT_SIZE_NORMAL))
                .setPadding(8f)
            table.addCell(valueCell)
        }

        return table
    }

    /**
     * Creates a data table with headers and rows
     */
    private fun createDataTable(
        headers: List<String>,
        rows: List<List<String>>,
        columnWidths: FloatArray? = null
    ): Table {
        val widths = columnWidths ?: FloatArray(headers.size) { 100f / headers.size }
        val table = Table(UnitValue.createPercentArray(widths))
            .useAllAvailableWidth()
            .setMarginBottom(15f)

        // Add header row
        headers.forEach { header ->
            val headerCell = Cell()
                .add(Paragraph(header).setFontSize(FONT_SIZE_NORMAL).setBold())
                .setBackgroundColor(COLOR_PRIMARY)
                .setFontColor(ColorConstants.WHITE)
                .setPadding(8f)
                .setTextAlignment(TextAlignment.CENTER)
            table.addHeaderCell(headerCell)
        }

        // Add data rows
        rows.forEachIndexed { index, row ->
            val backgroundColor = if (index % 2 == 0) ColorConstants.WHITE else COLOR_LIGHT_GRAY
            row.forEach { cellData ->
                val cell = Cell()
                    .add(Paragraph(cellData).setFontSize(FONT_SIZE_SMALL))
                    .setBackgroundColor(backgroundColor)
                    .setPadding(6f)
                table.addCell(cell)
            }
        }

        return table
    }

    /**
     * Formats a timestamp to a readable date string
     */
    private fun formatDate(timestamp: Long): String {
        return if (timestamp > 0) {
            dateOnlyFormat.format(Date(timestamp))
        } else {
            "N/A"
        }
    }

    /**
     * Formats a timestamp to a readable date-time string
     */
    private fun formatDateTime(timestamp: Long): String {
        return if (timestamp > 0) {
            dateFormat.format(Date(timestamp))
        } else {
            "N/A"
        }
    }

    /**
     * Formats a Firebase Timestamp to a readable date string
     */
    private fun formatDate(timestamp: com.google.firebase.Timestamp?): String {
        return if (timestamp != null) {
            dateOnlyFormat.format(timestamp.toDate())
        } else {
            "N/A"
        }
    }

    /**
     * Formats a Firebase Timestamp to a readable date-time string
     */
    private fun formatDateTime(timestamp: com.google.firebase.Timestamp?): String {
        return if (timestamp != null) {
            dateFormat.format(timestamp.toDate())
        } else {
            "N/A"
        }
    }

    /**
     * Truncates text to a maximum length
     */
    private fun truncateText(text: String, maxLength: Int = 50): String {
        return if (text.length > maxLength) {
            text.take(maxLength - 3) + "..."
        } else {
            text
        }
    }

    /**
     * Generates a PDF report for items
     * Requirements: 4.2, 4.8
     */
    suspend fun generateItemsReport(
        items: List<EnhancedLostFoundItem>,
        dateRange: DateRange,
        generatedBy: String
    ): Result<String> {
        return try {
            val fileName = generateFileName(ExportDataType.ITEMS, ExportFormat.PDF)
            
            createPdfDocument(
                fileName = fileName,
                title = "Items Report",
                dateRange = dateRange,
                generatedBy = generatedBy
            ) { document ->
                // Summary Statistics
                document.add(createSectionHeading("Summary Statistics"))
                
                val totalItems = items.size
                val lostItems = items.count { it.isLost }
                val foundItems = items.count { !it.isLost }
                val activeItems = items.count { it.status == ItemStatus.ACTIVE }
                val returnedItems = items.count { it.status == ItemStatus.RETURNED }
                val donatedItems = items.count { it.status == ItemStatus.DONATED }
                
                // Category breakdown
                val categoryBreakdown = items.groupBy { it.category }
                    .mapValues { it.value.size }
                    .entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .joinToString(", ") { "${it.key}: ${it.value}" }
                
                val stats = mapOf(
                    "Total Items" to totalItems.toString(),
                    "Lost Items" to lostItems.toString(),
                    "Found Items" to foundItems.toString(),
                    "Active Items" to activeItems.toString(),
                    "Returned Items" to returnedItems.toString(),
                    "Donated Items" to donatedItems.toString(),
                    "Top Categories" to categoryBreakdown
                )
                
                document.add(createStatsTable(stats))
                
                // Items Table
                document.add(createSectionHeading("Items Details"))
                
                val headers = listOf("Name", "Type", "Category", "Status", "Location", "Reporter", "Date")
                val columnWidths = floatArrayOf(15f, 10f, 12f, 12f, 15f, 18f, 18f)
                
                val rows = items.map { item ->
                    listOf(
                        truncateText(item.name, 20),
                        if (item.isLost) "Lost" else "Found",
                        truncateText(item.category, 15),
                        item.status.name,
                        truncateText(item.location, 20),
                        truncateText(item.userEmail, 25),
                        formatDate(item.timestamp.toDate().time)
                    )
                }
                
                document.add(createDataTable(headers, rows, columnWidths))
                
                // Status Distribution
                document.add(createSectionHeading("Status Distribution"))
                
                val statusStats: Map<String, String> = items.groupBy { it.status }
                    .mapValues { it.value.size }
                    .toSortedMap()
                    .mapKeys { it.key.name }
                    .mapValues { it.value.toString() }
                
                document.add(createStatsTable(statusStats))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to generate items report: ${e.message}", e))
        }
    }

    /**
     * Generates a PDF report for users
     * Requirements: 4.2
     */
    suspend fun generateUsersReport(
        users: List<EnhancedAdminUser>,
        dateRange: DateRange,
        generatedBy: String
    ): Result<String> {
        return try {
            val fileName = generateFileName(ExportDataType.USERS, ExportFormat.PDF)
            
            createPdfDocument(
                fileName = fileName,
                title = "Users Report",
                dateRange = dateRange,
                generatedBy = generatedBy
            ) { document ->
                // Summary Statistics
                document.add(createSectionHeading("Summary Statistics"))
                
                val totalUsers = users.size
                val activeUsers = users.count { !it.isBlocked }
                val blockedUsers = users.count { it.isBlocked }
                val adminUsers = users.count { it.role == UserRole.ADMIN }
                val securityUsers = users.count { it.role == UserRole.SECURITY }
                val studentUsers = users.count { it.role == UserRole.STUDENT }
                
                val totalItemsReported = users.sumOf { it.itemsReported }
                val totalItemsFound = users.sumOf { it.itemsFound }
                val totalItemsClaimed = users.sumOf { it.itemsClaimed }
                val avgItemsPerUser = if (totalUsers > 0) {
                    String.format(Locale.getDefault(), "%.2f", totalItemsReported.toFloat() / totalUsers)
                } else "0.00"
                
                val stats = mapOf(
                    "Total Users" to totalUsers.toString(),
                    "Active Users" to activeUsers.toString(),
                    "Blocked Users" to blockedUsers.toString(),
                    "Admin Users" to adminUsers.toString(),
                    "Security Users" to securityUsers.toString(),
                    "Student Users" to studentUsers.toString(),
                    "Total Items Reported" to totalItemsReported.toString(),
                    "Total Items Found" to totalItemsFound.toString(),
                    "Total Items Claimed" to totalItemsClaimed.toString(),
                    "Avg Items Per User" to avgItemsPerUser
                )
                
                document.add(createStatsTable(stats))
                
                // Role Distribution
                document.add(createSectionHeading("Role Distribution"))
                
                val roleStats = mapOf(
                    "Admin" to "$adminUsers (${if (totalUsers > 0) String.format(Locale.getDefault(), "%.1f", adminUsers * 100f / totalUsers) else "0"}%)",
                    "Security" to "$securityUsers (${if (totalUsers > 0) String.format(Locale.getDefault(), "%.1f", securityUsers * 100f / totalUsers) else "0"}%)",
                    "Student" to "$studentUsers (${if (totalUsers > 0) String.format(Locale.getDefault(), "%.1f", studentUsers * 100f / totalUsers) else "0"}%)"
                )
                
                document.add(createStatsTable(roleStats))
                
                // Users Table
                document.add(createSectionHeading("Users Details"))
                
                val headers = listOf("Email", "Display Name", "Role", "Status", "Items", "Joined", "Last Login")
                val columnWidths = floatArrayOf(20f, 15f, 10f, 10f, 10f, 15f, 20f)
                
                val rows = users.map { user ->
                    listOf(
                        truncateText(user.email, 30),
                        truncateText(user.displayName.ifEmpty { "N/A" }, 20),
                        user.role.name,
                        if (user.isBlocked) "Blocked" else "Active",
                        "${user.itemsReported}/${user.itemsFound}/${user.itemsClaimed}",
                        formatDate(user.createdAt),
                        if (user.lastLoginAt != null) formatDate(user.lastLoginAt) else "Never"
                    )
                }
                
                document.add(createDataTable(headers, rows, columnWidths))
                
                // Activity Metrics
                document.add(createSectionHeading("Activity Metrics"))
                
                val topContributors = users
                    .sortedByDescending { it.itemsReported }
                    .take(5)
                    .mapIndexed { index, user ->
                        "${index + 1}. ${user.email} - ${user.itemsReported} items"
                    }
                    .joinToString("\n")
                
                val activityStats = mapOf(
                    "Top Contributors" to topContributors.ifEmpty { "No data" }
                )
                
                document.add(createStatsTable(activityStats))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to generate users report: ${e.message}", e))
        }
    }

    /**
     * Generates a PDF report for activity logs
     * Requirements: 4.2
     */
    suspend fun generateActivityReport(
        logs: List<ActivityLog>,
        dateRange: DateRange,
        generatedBy: String
    ): Result<String> {
        return try {
            val fileName = generateFileName(ExportDataType.ACTIVITIES, ExportFormat.PDF)
            
            createPdfDocument(
                fileName = fileName,
                title = "Activity Logs Report",
                dateRange = dateRange,
                generatedBy = generatedBy
            ) { document ->
                // Summary Statistics
                document.add(createSectionHeading("Summary Statistics"))
                
                val totalLogs = logs.size
                val userActions = logs.count { !it.isAdminAction() && !it.isSystemEvent() }
                val adminActions = logs.count { it.isAdminAction() }
                val systemEvents = logs.count { it.isSystemEvent() }
                
                val uniqueActors = logs.map { it.actorEmail }.distinct().size
                val uniqueTargets = logs.map { it.targetId }.filter { it.isNotBlank() }.distinct().size
                
                val stats = mapOf(
                    "Total Activities" to totalLogs.toString(),
                    "User Actions" to userActions.toString(),
                    "Admin Actions" to adminActions.toString(),
                    "System Events" to systemEvents.toString(),
                    "Unique Actors" to uniqueActors.toString(),
                    "Unique Targets" to uniqueTargets.toString()
                )
                
                document.add(createStatsTable(stats))
                
                // Action Type Distribution
                document.add(createSectionHeading("Action Type Distribution"))
                
                val actionTypeStats: Map<String, String> = logs.groupBy { it.actionType }
                    .mapValues { it.value.size }
                    .entries
                    .sortedByDescending { it.value }
                    .take(10)
                    .associate { it.key.getDisplayName() to it.value.toString() }
                
                document.add(createStatsTable(actionTypeStats))
                
                // Target Type Distribution
                document.add(createSectionHeading("Target Type Distribution"))
                
                val targetTypeStats: Map<String, String> = logs.groupBy { it.targetType }
                    .mapValues { it.value.size }
                    .mapKeys { it.key.getDisplayName() }
                    .mapValues { it.value.toString() }
                
                document.add(createStatsTable(targetTypeStats))
                
                // Activity Logs Table
                document.add(createSectionHeading("Activity Details"))
                
                val headers = listOf("Date/Time", "Actor", "Action", "Target", "Description")
                val columnWidths = floatArrayOf(18f, 22f, 18f, 12f, 30f)
                
                val rows = logs.take(100).map { log -> // Limit to 100 most recent logs
                    listOf(
                        formatDateTime(log.timestamp),
                        truncateText(log.actorEmail, 30),
                        log.actionType.getDisplayName(),
                        log.targetType.getDisplayName(),
                        truncateText(log.description, 50)
                    )
                }
                
                document.add(createDataTable(headers, rows, columnWidths))
                
                // Note about truncation
                if (logs.size > 100) {
                    val noteParagraph = Paragraph(
                        "Note: Showing 100 most recent activities out of ${logs.size} total. " +
                        "For complete logs, please use CSV export or filter by date range."
                    )
                        .setFontSize(FONT_SIZE_SMALL)
                        .setFontColor(COLOR_HEADER)
                        .setItalic()
                        .setMarginTop(10f)
                    document.add(noteParagraph)
                }
                
                // Most Active Users
                document.add(createSectionHeading("Most Active Users"))
                
                val topActors = logs
                    .groupBy { it.actorEmail }
                    .mapValues { it.value.size }
                    .entries
                    .sortedByDescending { it.value }
                    .take(10)
                    .mapIndexed { index, entry ->
                        "${index + 1}. ${entry.key} - ${entry.value} actions"
                    }
                    .joinToString("\n")
                
                val actorStats = mapOf(
                    "Top 10 Active Users" to topActors.ifEmpty { "No data" }
                )
                
                document.add(createStatsTable(actorStats))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to generate activity report: ${e.message}", e))
        }
    }

    /**
     * Generates a comprehensive report combining all data
     * Requirements: 4.1
     */
    suspend fun generateComprehensiveReport(
        items: List<EnhancedLostFoundItem>,
        users: List<EnhancedAdminUser>,
        logs: List<ActivityLog>,
        donations: List<DonationItem>,
        donationStats: DonationStats,
        dateRange: DateRange,
        generatedBy: String
    ): Result<String> {
        return try {
            val fileName = generateFileName(ExportDataType.COMPREHENSIVE, ExportFormat.PDF)
            
            createPdfDocument(
                fileName = fileName,
                title = "Comprehensive System Report",
                dateRange = dateRange,
                generatedBy = generatedBy
            ) { document ->
                // Executive Summary
                document.add(createSectionHeading("Executive Summary"))
                
                val totalItems = items.size
                val totalUsers = users.size
                val totalActivities = logs.size
                val totalDonations = donations.count { it.status == DonationStatus.DONATED }
                
                val executiveSummary = mapOf(
                    "Report Period" to "${dateOnlyFormat.format(Date(dateRange.startDate))} to ${dateOnlyFormat.format(Date(dateRange.endDate))}",
                    "Total Items" to totalItems.toString(),
                    "Total Users" to totalUsers.toString(),
                    "Total Activities" to totalActivities.toString(),
                    "Total Donations" to totalDonations.toString(),
                    "System Health" to "Operational"
                )
                
                document.add(createStatsTable(executiveSummary))
                
                // Items Overview
                document.add(createSectionHeading("Items Overview"))
                
                val lostItems = items.count { it.isLost }
                val foundItems = items.count { !it.isLost }
                val activeItems = items.count { it.status == ItemStatus.ACTIVE }
                val returnedItems = items.count { it.status == ItemStatus.RETURNED }
                
                val itemsOverview = mapOf(
                    "Total Items" to totalItems.toString(),
                    "Lost Items" to "$lostItems (${if (totalItems > 0) String.format(Locale.getDefault(), "%.1f", lostItems * 100f / totalItems) else "0"}%)",
                    "Found Items" to "$foundItems (${if (totalItems > 0) String.format(Locale.getDefault(), "%.1f", foundItems * 100f / totalItems) else "0"}%)",
                    "Active Items" to activeItems.toString(),
                    "Returned Items" to returnedItems.toString(),
                    "Return Rate" to if (totalItems > 0) String.format(Locale.getDefault(), "%.1f%%", returnedItems * 100f / totalItems) else "0%"
                )
                
                document.add(createStatsTable(itemsOverview))
                
                // Top Categories
                val topCategories = items.groupBy { it.category }
                    .mapValues { it.value.size }
                    .entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .mapIndexed { index, entry ->
                        "${index + 1}. ${entry.key}: ${entry.value} items"
                    }
                    .joinToString("\n")
                
                val categoryStats = mapOf(
                    "Top 5 Categories" to topCategories.ifEmpty { "No data" }
                )
                
                document.add(createStatsTable(categoryStats))
                
                // Users Overview
                document.add(createSectionHeading("Users Overview"))
                
                val activeUsers = users.count { !it.isBlocked }
                val blockedUsers = users.count { it.isBlocked }
                val adminUsers = users.count { it.role == UserRole.ADMIN }
                val securityUsers = users.count { it.role == UserRole.SECURITY }
                val studentUsers = users.count { it.role == UserRole.STUDENT }
                
                val totalItemsReported = users.sumOf { it.itemsReported }
                val avgItemsPerUser = if (totalUsers > 0) {
                    String.format(Locale.getDefault(), "%.2f", totalItemsReported.toFloat() / totalUsers)
                } else "0.00"
                
                val usersOverview = mapOf(
                    "Total Users" to totalUsers.toString(),
                    "Active Users" to "$activeUsers (${if (totalUsers > 0) String.format(Locale.getDefault(), "%.1f", activeUsers * 100f / totalUsers) else "0"}%)",
                    "Blocked Users" to blockedUsers.toString(),
                    "Admin Users" to adminUsers.toString(),
                    "Security Users" to securityUsers.toString(),
                    "Student Users" to studentUsers.toString(),
                    "Avg Items Per User" to avgItemsPerUser
                )
                
                document.add(createStatsTable(usersOverview))
                
                // Top Contributors
                val topContributors = users
                    .sortedByDescending { it.itemsReported }
                    .take(5)
                    .mapIndexed { index, user ->
                        "${index + 1}. ${user.email} - ${user.itemsReported} items"
                    }
                    .joinToString("\n")
                
                val contributorStats = mapOf(
                    "Top 5 Contributors" to topContributors.ifEmpty { "No data" }
                )
                
                document.add(createStatsTable(contributorStats))
                
                // Activity Overview
                document.add(createSectionHeading("Activity Overview"))
                
                val userActions = logs.count { !it.isAdminAction() && !it.isSystemEvent() }
                val adminActions = logs.count { it.isAdminAction() }
                val systemEvents = logs.count { it.isSystemEvent() }
                
                val activityOverview = mapOf(
                    "Total Activities" to totalActivities.toString(),
                    "User Actions" to "$userActions (${if (totalActivities > 0) String.format(Locale.getDefault(), "%.1f", userActions * 100f / totalActivities) else "0"}%)",
                    "Admin Actions" to "$adminActions (${if (totalActivities > 0) String.format(Locale.getDefault(), "%.1f", adminActions * 100f / totalActivities) else "0"}%)",
                    "System Events" to systemEvents.toString()
                )
                
                document.add(createStatsTable(activityOverview))
                
                // Top Actions
                val topActions = logs.groupBy { it.actionType }
                    .mapValues { it.value.size }
                    .entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .mapIndexed { index, entry ->
                        "${index + 1}. ${entry.key.getDisplayName()}: ${entry.value} times"
                    }
                    .joinToString("\n")
                
                val actionStats = mapOf(
                    "Top 5 Actions" to topActions.ifEmpty { "No data" }
                )
                
                document.add(createStatsTable(actionStats))
                
                // Donation Overview
                document.add(createSectionHeading("Donation Overview"))
                
                val pendingDonations = donations.count { it.status == DonationStatus.PENDING }
                val readyDonations = donations.count { it.status == DonationStatus.READY }
                val completedDonations = donations.count { it.status == DonationStatus.DONATED }
                
                val donationOverview = mapOf(
                    "Total Donated" to completedDonations.toString(),
                    "Pending Review" to pendingDonations.toString(),
                    "Ready for Donation" to readyDonations.toString(),
                    "Total Value" to String.format(Locale.getDefault(), "$%.2f", donationStats.totalValue),
                    "Avg Value Per Item" to String.format(Locale.getDefault(), "$%.2f", donationStats.getAverageValuePerItem()),
                    "Donation Rate" to String.format(Locale.getDefault(), "%.1f%%", donationStats.getDonationRate())
                )
                
                document.add(createStatsTable(donationOverview))
                
                // Donation by Category
                if (donationStats.donationsByCategory.isNotEmpty()) {
                    val donationByCategory = donationStats.donationsByCategory
                        .entries
                        .sortedByDescending { it.value }
                        .take(5)
                        .mapIndexed { index, entry ->
                            "${index + 1}. ${entry.key}: ${entry.value} items"
                        }
                        .joinToString("\n")
                    
                    val categoryDonationStats = mapOf(
                        "Top 5 Donated Categories" to donationByCategory
                    )
                    
                    document.add(createStatsTable(categoryDonationStats))
                }
                
                // System Performance Metrics
                document.add(createSectionHeading("System Performance Metrics"))
                
                // Calculate average response time (time from report to first status change)
                val itemsWithStatusChanges = items.filter { it.statusHistory.isNotEmpty() }
                val avgResponseTime = if (itemsWithStatusChanges.isNotEmpty()) {
                    val totalResponseTime = itemsWithStatusChanges.sumOf { item ->
                        val firstChange = item.statusHistory.firstOrNull()
                        if (firstChange != null) {
                            firstChange.changedAt - item.timestamp.toDate().time
                        } else {
                            0L
                        }
                    }
                    val avgMillis = totalResponseTime / itemsWithStatusChanges.size
                    val avgHours = avgMillis / (1000 * 60 * 60)
                    "$avgHours hours"
                } else {
                    "N/A"
                }
                
                val performanceMetrics = mapOf(
                    "Avg Response Time" to avgResponseTime,
                    "Items with Status Changes" to itemsWithStatusChanges.size.toString(),
                    "Active Users (Last 30 Days)" to users.count { 
                        it.lastActivityAt != null && 
                        (System.currentTimeMillis() - it.lastActivityAt.toDate().time) < (30L * 24 * 60 * 60 * 1000)
                    }.toString()
                )
                
                document.add(createStatsTable(performanceMetrics))
                
                // Trends and Insights
                document.add(createSectionHeading("Trends and Insights"))
                
                val insights = mutableListOf<String>()
                
                // Item trends
                if (returnedItems > 0) {
                    val returnRate = returnedItems * 100f / totalItems
                    insights.add("• Return rate is ${String.format(Locale.getDefault(), "%.1f", returnRate)}% - ${
                        when {
                            returnRate > 50 -> "Excellent performance"
                            returnRate > 30 -> "Good performance"
                            else -> "Room for improvement"
                        }
                    }")
                }
                
                // User engagement
                val engagedUsers = users.count { it.itemsReported > 0 }
                if (totalUsers > 0) {
                    val engagementRate = engagedUsers * 100f / totalUsers
                    insights.add("• User engagement rate is ${String.format(Locale.getDefault(), "%.1f", engagementRate)}%")
                }
                
                // Donation efficiency
                if (completedDonations > 0) {
                    insights.add("• Successfully donated $completedDonations items, contributing to campus sustainability")
                }
                
                // Admin activity
                if (adminActions > 0) {
                    insights.add("• Admin team performed $adminActions actions during this period")
                }
                
                val insightsText = if (insights.isNotEmpty()) {
                    insights.joinToString("\n")
                } else {
                    "No significant trends identified for this period"
                }
                
                val insightsStats = mapOf(
                    "Key Insights" to insightsText
                )
                
                document.add(createStatsTable(insightsStats))
                
                // Recommendations
                document.add(createSectionHeading("Recommendations"))
                
                val recommendations = mutableListOf<String>()
                
                if (pendingDonations > 10) {
                    recommendations.add("• Review $pendingDonations pending donation items")
                }
                
                if (blockedUsers > 0) {
                    recommendations.add("• Review $blockedUsers blocked user accounts")
                }
                
                val oldActiveItems = items.count { 
                    it.status == ItemStatus.ACTIVE && it.getAgeInDays() > 180 
                }
                if (oldActiveItems > 0) {
                    recommendations.add("• Follow up on $oldActiveItems items older than 6 months")
                }
                
                if (recommendations.isEmpty()) {
                    recommendations.add("• System is operating efficiently")
                    recommendations.add("• Continue monitoring key metrics")
                }
                
                val recommendationsText = recommendations.joinToString("\n")
                
                val recommendationsStats = mapOf(
                    "Action Items" to recommendationsText
                )
                
                document.add(createStatsTable(recommendationsStats))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to generate comprehensive report: ${e.message}", e))
        }
    }
}
