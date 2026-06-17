package com.example.loginandregistration.admin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.adapters.ExportHistoryAdapter
import com.example.loginandregistration.admin.dialogs.ExportProgressDialog
import com.example.loginandregistration.admin.models.*
import com.example.loginandregistration.admin.utils.ExportFileManager
import com.example.loginandregistration.admin.viewmodel.AdminDashboardViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for data export configuration and history
 * Requirements: 4.1, 4.8
 * Task: 14.1
 */
class AdminExportFragment : Fragment() {

    private val viewModel: AdminDashboardViewModel by activityViewModels()
    
    // UI Components
    private lateinit var formatChipGroup: ChipGroup
    private lateinit var pdfChip: Chip
    private lateinit var csvChip: Chip
    
    private lateinit var dataTypeChipGroup: ChipGroup
    private lateinit var itemsChip: Chip
    private lateinit var usersChip: Chip
    private lateinit var activitiesChip: Chip
    private lateinit var comprehensiveChip: Chip
    
    private lateinit var dateRangeButton: MaterialButton
    private lateinit var dateRangeText: TextView
    
    private lateinit var exportButton: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyAdapter: ExportHistoryAdapter
    private lateinit var emptyHistoryText: TextView
    private lateinit var cleanupButton: MaterialButton
    
    // Progress dialog
    private var progressDialog: ExportProgressDialog? = null
    
    // Export file manager
    private lateinit var exportFileManager: ExportFileManager
    
    // Selected values
    private var selectedFormat: ExportFormat = ExportFormat.PDF
    private var selectedDataType: ExportDataType = ExportDataType.COMPREHENSIVE
    private var selectedDateRange: DateRange = DateRange.lastNDays(30)
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_export, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize export file manager
        exportFileManager = ExportFileManager(requireContext(), FirebaseFirestore.getInstance())
        
        initializeViews(view)
        setupFormatSelection()
        setupDataTypeSelection()
        setupDateRangePicker()
        setupExportButton()
        setupHistoryRecyclerView()
        observeViewModel()
        
        // Load export history
        viewModel.loadExportHistory()
    }

    private fun initializeViews(view: View) {
        // Format selection
        formatChipGroup = view.findViewById(R.id.formatChipGroup)
        pdfChip = view.findViewById(R.id.pdfChip)
        csvChip = view.findViewById(R.id.csvChip)
        
        // Data type selection
        dataTypeChipGroup = view.findViewById(R.id.dataTypeChipGroup)
        itemsChip = view.findViewById(R.id.itemsChip)
        usersChip = view.findViewById(R.id.usersChip)
        activitiesChip = view.findViewById(R.id.activitiesChip)
        comprehensiveChip = view.findViewById(R.id.comprehensiveChip)
        
        // Date range
        dateRangeButton = view.findViewById(R.id.dateRangeButton)
        dateRangeText = view.findViewById(R.id.dateRangeText)
        
        // Export button and progress
        exportButton = view.findViewById(R.id.exportButton)
        progressBar = view.findViewById(R.id.exportProgressBar)
        progressText = view.findViewById(R.id.progressText)
        
        // History
        historyRecyclerView = view.findViewById(R.id.historyRecyclerView)
        emptyHistoryText = view.findViewById(R.id.emptyHistoryText)
        cleanupButton = view.findViewById(R.id.cleanupButton)
        
        // Set default date range text
        updateDateRangeText()
    }

    private fun setupFormatSelection() {
        // Set PDF as default
        pdfChip.isChecked = true
        
        formatChipGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedFormat = when (checkedId) {
                R.id.pdfChip -> ExportFormat.PDF
                R.id.csvChip -> ExportFormat.CSV
                else -> ExportFormat.PDF
            }
        }
    }

    private fun setupDataTypeSelection() {
        // Set Comprehensive as default
        comprehensiveChip.isChecked = true
        
        dataTypeChipGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedDataType = when (checkedId) {
                R.id.itemsChip -> ExportDataType.ITEMS
                R.id.usersChip -> ExportDataType.USERS
                R.id.activitiesChip -> ExportDataType.ACTIVITIES
                R.id.comprehensiveChip -> ExportDataType.COMPREHENSIVE
                else -> ExportDataType.COMPREHENSIVE
            }
        }
    }

    private fun setupDateRangePicker() {
        dateRangeButton.setOnClickListener {
            showDateRangePicker()
        }
    }

    private fun showDateRangePicker() {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")
            .setSelection(
                androidx.core.util.Pair(
                    selectedDateRange.startDate,
                    selectedDateRange.endDate
                )
            )
            .build()
        
        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            selectedDateRange = DateRange(
                startDate = selection.first ?: 0,
                endDate = selection.second ?: System.currentTimeMillis()
            )
            updateDateRangeText()
        }
        
        dateRangePicker.show(parentFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun updateDateRangeText() {
        val startDateStr = dateFormat.format(Date(selectedDateRange.startDate))
        val endDateStr = dateFormat.format(Date(selectedDateRange.endDate))
        dateRangeText.text = "$startDateStr - $endDateStr"
    }

    private fun setupExportButton() {
        exportButton.setOnClickListener {
            startExport()
        }
    }

    private fun startExport() {
        // Validate selection
        if (!selectedDateRange.isValid()) {
            showError("Invalid date range selected")
            return
        }
        
        // Create export request
        val exportRequest = ExportRequest(
            id = UUID.randomUUID().toString(),
            format = selectedFormat,
            dataType = selectedDataType,
            dateRange = selectedDateRange,
            requestedBy = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: "",
            requestedAt = System.currentTimeMillis(),
            status = ExportStatus.PENDING
        )
        
        // Start export
        viewModel.exportData(exportRequest)
        
        // Show progress UI
        showProgressUI()
    }

    private fun setupHistoryRecyclerView() {
        historyAdapter = ExportHistoryAdapter(
            onItemClick = { exportRequest ->
                // Handle item click - view/share export
                handleExportHistoryClick(exportRequest)
            },
            onDeleteClick = { exportRequest ->
                // Handle delete
                handleExportDelete(exportRequest)
            }
        )
        
        historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
        
        // Setup cleanup button
        cleanupButton.setOnClickListener {
            cleanupOldExports()
        }
    }

    private fun handleExportHistoryClick(exportRequest: ExportRequest) {
        if (exportRequest.isComplete() && exportRequest.fileUrl.isNotEmpty()) {
            // Show options to view or share
            showExportOptions(exportRequest)
        } else if (exportRequest.hasFailed()) {
            showError("Export failed: ${exportRequest.errorMessage}")
        } else {
            showMessage("Export is still processing")
        }
    }

    private fun showExportOptions(exportRequest: ExportRequest) {
        val options = arrayOf("View File", "Share File", "Re-download", "Delete", "Cancel")
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Export Options")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> viewExportFile(exportRequest)
                    1 -> shareExportFile(exportRequest)
                    2 -> redownloadExport(exportRequest)
                    3 -> handleExportDelete(exportRequest)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun redownloadExport(exportRequest: ExportRequest) {
        // Re-generate the export with the same parameters
        showMessage("Re-generating export...")
        viewModel.exportData(exportRequest.copy(
            id = UUID.randomUUID().toString(),
            requestedAt = System.currentTimeMillis(),
            status = ExportStatus.PENDING
        ))
        showProgressUI()
    }

    private fun viewExportFile(exportRequest: ExportRequest) {
        if (exportRequest.fileUrl.isEmpty()) {
            showError("Export file not found")
            return
        }
        
        exportFileManager.openExportFile(exportRequest.fileUrl, exportRequest.format)
            .onSuccess { intent ->
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    showError("No app available to open this file type")
                }
            }
            .onFailure { e ->
                showError("Failed to open file: ${e.message}")
            }
    }

    private fun shareExportFile(exportRequest: ExportRequest) {
        if (exportRequest.fileUrl.isEmpty()) {
            showError("Export file not found")
            return
        }
        
        exportFileManager.shareExportFile(exportRequest.fileUrl, exportRequest.format)
            .onSuccess { intent ->
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    showError("Failed to share file: ${e.message}")
                }
            }
            .onFailure { e ->
                showError("Failed to share file: ${e.message}")
            }
    }

    private fun handleExportDelete(exportRequest: ExportRequest) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Export")
            .setMessage("Are you sure you want to delete this export? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteExport(exportRequest)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteExport(exportRequest: ExportRequest) {
        lifecycleScope.launch {
            try {
                exportFileManager.deleteExportFile(exportRequest.id, exportRequest.fileUrl)
                    .onSuccess {
                        showSuccess("Export deleted successfully")
                        // Refresh export history
                        viewModel.loadExportHistory()
                    }
                    .onFailure { e ->
                        showError("Failed to delete export: ${e.message}")
                    }
            } catch (e: Exception) {
                showError("Error deleting export: ${e.message}")
            }
        }
    }

    private fun cleanupOldExports() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cleanup Old Exports")
            .setMessage("This will delete all exports older than 30 days. Continue?")
            .setPositiveButton("Cleanup") { _, _ ->
                performCleanup()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performCleanup() {
        lifecycleScope.launch {
            try {
                exportFileManager.cleanupOldExports()
                    .onSuccess { count ->
                        showSuccess("Cleaned up $count old export(s)")
                        // Refresh export history
                        viewModel.loadExportHistory()
                    }
                    .onFailure { e ->
                        showError("Failed to cleanup exports: ${e.message}")
                    }
            } catch (e: Exception) {
                showError("Error during cleanup: ${e.message}")
            }
        }
    }

    private fun observeViewModel() {
        // Observe export progress
        viewModel.exportProgress.observe(viewLifecycleOwner) { progress ->
            updateProgress(progress)
        }
        
        // Observe export result
        viewModel.exportResult.observe(viewLifecycleOwner) { fileUrl ->
            if (fileUrl.isNotEmpty()) {
                progressDialog?.showCompletion("Export completed successfully!")
                hideProgressUI()
                showSuccess("Export completed successfully!")
                viewModel.clearExportResult()
            }
        }
        
        // Observe export history
        viewModel.exportHistory.observe(viewLifecycleOwner) { history ->
            if (history.isEmpty()) {
                emptyHistoryText.visibility = View.VISIBLE
                historyRecyclerView.visibility = View.GONE
            } else {
                emptyHistoryText.visibility = View.GONE
                historyRecyclerView.visibility = View.VISIBLE
                historyAdapter.submitList(history)
            }
        }
        
        // Error display disabled - uncomment to show errors
        // viewModel.error.observe(viewLifecycleOwner) { error ->
        //     if (error.isNotEmpty()) {
        //         progressDialog?.showError(error)
        //         hideProgressUI()
        //         showError(error)
        //     }
        // }
        
        // Observe success messages
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                showSuccess(message)
            }
        }
    }

    private fun showProgressUI() {
        // Show progress dialog
        progressDialog = ExportProgressDialog.newInstance("Exporting Data", true)
        progressDialog?.setOnCancelListener {
            // Handle cancel - could add cancel logic here
            showMessage("Export cancelled")
        }
        progressDialog?.show(parentFragmentManager, "EXPORT_PROGRESS")
        
        // Also update inline progress
        exportButton.isEnabled = false
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        progressBar.progress = 0
        progressText.text = "Starting export..."
    }

    private fun hideProgressUI() {
        progressDialog?.dismiss()
        progressDialog = null
        
        exportButton.isEnabled = true
        progressBar.visibility = View.GONE
        progressText.visibility = View.GONE
    }

    private fun updateProgress(progress: Int) {
        val statusMessage = when {
            progress < 20 -> "Preparing export..."
            progress < 50 -> "Collecting data..."
            progress < 80 -> "Generating file..."
            progress < 100 -> "Finalizing..."
            else -> "Complete!"
        }
        
        // Update dialog
        progressDialog?.updateProgress(progress, statusMessage)
        
        // Update inline progress
        progressBar.progress = progress
        progressText.text = statusMessage
    }

    private fun showMessage(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showSuccess(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(resources.getColor(android.R.color.holo_green_dark, null))
                .show()
        }
    }

    private fun showError(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(resources.getColor(android.R.color.holo_red_dark, null))
                .show()
        }
    }
}
