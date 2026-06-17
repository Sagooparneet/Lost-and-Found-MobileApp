package com.example.loginandregistration.security.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.loginandregistration.models.Report
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar

class SecurityViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val reportsCollection = db.collection("items")

    // --- EXISTING LIVEDATA ---
    private val _reports = MutableLiveData<List<Report>>()
    val reports: LiveData<List<Report>> get() = _reports

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    // --- NEW LIVEDATA FOR DASHBOARD STATS ---

    // Holds the count of reports for the "Reports This Month" card
    private val _reportsThisMonthCount = MutableLiveData<Int>()
    val reportsThisMonthCount: LiveData<Int> get() = _reportsThisMonthCount

    // Holds the data entries for the "By Type" Pie Chart
    private val _pieChartDataByType = MutableLiveData<List<PieEntry>>()
    val pieChartDataByType: LiveData<List<PieEntry>> get() = _pieChartDataByType

    // Holds the data entries for the "By Reporter" Pie Chart
    private val _pieChartDataByReporter = MutableLiveData<List<PieEntry>>()
    val pieChartDataByReporter: LiveData<List<PieEntry>> get() = _pieChartDataByReporter

    init {
        fetchReports()
    }

    private fun fetchReports() {
        reportsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _toastMessage.value = "Error fetching reports: ${error.message}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val reportList = snapshot.documents.mapNotNull { doc ->
                        val report = doc.toObject(Report::class.java)
                        report?.id = doc.id
                        report
                    }
                    _reports.value = reportList

                    // --- NEW: Calculate stats every time the report list is updated ---
                    calculateDashboardStats(reportList)
                }
            }
    }

    /**
     * NEW: This function processes the full list of reports and updates the
     * LiveData objects for the dashboard UI.
     */
    private fun calculateDashboardStats(reports: List<Report>) {
        // 1. Calculate "Reports This Month"
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val monthlyCount = reports.count { report ->
            report.timestamp?.let {
                calendar.time = it
                calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
            } ?: false
        }
        _reportsThisMonthCount.value = monthlyCount

        // 2. Calculate "By Type" breakdown
        val typeCounts = reports.groupingBy { it.category ?: "Unknown"}.eachCount()
        val typeEntries = typeCounts.map { (category, count) ->
            PieEntry(count.toFloat(), category) // e.g., PieEntry(15.0, "Lost")
        }
        _pieChartDataByType.value = typeEntries

        // 3. Calculate "By Reporter" breakdown
        // This assumes you have a field in your 'Report' model to distinguish reporters.
        // Let's assume a placeholder logic for now. We'll check the user's role.
        val reporterCounts = reports.groupingBy {
            if (it.userRole == "Security" || it.userRole == "Admin") "Security" else "Student"
        }.eachCount()

        val reporterEntries = reporterCounts.map { (role, count) ->
            PieEntry(count.toFloat(), role)
        }
        _pieChartDataByReporter.value = reporterEntries

        // TODO: Add calculations for Line Chart and other stats as needed
    }

    fun updateReportStatus(reportId: String, newStatus: String) {
        reportsCollection.document(reportId)
            .update("status", newStatus)
            .addOnSuccessListener {
                _toastMessage.value = "Report status updated to '$newStatus'"
            }
            .addOnFailureListener { e ->
                _toastMessage.value = "Failed to update status: ${e.message}"
            }
    }
}




