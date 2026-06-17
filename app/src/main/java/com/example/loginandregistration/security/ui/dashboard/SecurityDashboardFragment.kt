package com.example.loginandregistration.security.ui.dashboard

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.loginandregistration.ItemRequest
import com.example.loginandregistration.R // Make sure this is your app's R file
import com.example.loginandregistration.databinding.FragmentSecurityDashboardBinding
import com.example.loginandregistration.models.Report
import com.example.loginandregistration.security.data.viewmodel.SecurityViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class SecurityDashboardFragment : Fragment() {

    private var _binding: FragmentSecurityDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SecurityViewModel by viewModels()
    private lateinit var securityReportsAdapter: SecurityReportsAdapter
    private lateinit var itemRequestsAdapter: ItemRequestAdapter

    // We no longer need the getThemeColor function
    // private fun getThemeColor(...) {}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecurityDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupItemRequestsRecyclerView()
        observeViewModel()
        loadItemRequests()

        setupFakeLineChart()
        setupFakePieCharts()
    }

    private fun setupRecyclerView() {
        securityReportsAdapter = SecurityReportsAdapter(
            onApproveClicked = { report -> viewModel.updateReportStatus(report.id, "Approved") },
            onRejectClicked = { report -> viewModel.updateReportStatus(report.id, "Rejected") }
        )
        binding.securityReportsRecyclerview.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = securityReportsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupItemRequestsRecyclerView() {
        itemRequestsAdapter = ItemRequestAdapter(
            onApproveClicked = { request -> updateItemRequestStatus(request, ItemRequest.RequestStatus.APPROVED) },
            onRejectClicked = { request -> updateItemRequestStatus(request, ItemRequest.RequestStatus.REJECTED) }
        )
        binding.itemRequestsRecyclerview.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = itemRequestsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun loadItemRequests() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                getItemRequestsFlow()
                    .flowOn(Dispatchers.IO)
                    .catch { e ->
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error loading item requests: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .collect { requests ->
                        withContext(Dispatchers.Main) {
                            itemRequestsAdapter.submitList(requests)
                        }
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load item requests", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getItemRequestsFlow() = callbackFlow {
        val db = FirebaseFirestore.getInstance()
        val listener = db.collection("itemRequests")
            .orderBy("requestDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(ItemRequest::class.java)?.copy(requestId = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    private fun updateItemRequestStatus(request: ItemRequest, newStatus: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Authentication required", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val updates = hashMapOf<String, Any>(
            "status" to newStatus,
            "reviewedBy" to currentUser.email.orEmpty(),
            "reviewDate" to Timestamp.now()
        )

        db.collection("itemRequests")
            .document(request.requestId)
            .update(updates)
            .addOnSuccessListener {
                val message = if (newStatus == ItemRequest.RequestStatus.APPROVED) {
                    "Request approved successfully"
                } else {
                    "Request rejected"
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update request: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun observeViewModel() {
        viewModel.reports.observe(viewLifecycleOwner, Observer { reports ->
            securityReportsAdapter.submitList(reports)
        })

        viewModel.toastMessage.observe(viewLifecycleOwner, Observer { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        })
    }

    private fun setupFakeLineChart() {
        val lineChart: LineChart = binding.chartReportsOverTime

        val entries = ArrayList<Entry>().apply {
            add(Entry(0f, 10f)) // Jan
            add(Entry(1f, 12f)) // Feb
            add(Entry(2f, 25f)) // Mar
            add(Entry(3f, 18f)) // Apr
            add(Entry(4f, 22f)) // May
        }

        val dataSet = LineDataSet(entries, "Reports Over Time").apply {
            // --- FIX START: Use a defined color from your project's colors.xml ---
            val chartColor = ContextCompat.getColor(requireContext(), R.color.purple_500)
            color = chartColor
            fillColor = chartColor
            // --- FIX END ---

            valueTextColor = Color.TRANSPARENT
            lineWidth = 2.5f
            setDrawCircles(false)
            setDrawFilled(true)
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(arrayOf("Jan", "Feb", "Mar", "Apr", "May"))
            }

            axisLeft.apply {
                setDrawGridLines(false)
                setDrawLabels(false)
                setDrawAxisLine(false)
            }

            axisRight.isEnabled = false
        }

        lineChart.data = LineData(dataSet)
        lineChart.invalidate()
    }

    private fun setupFakePieCharts() {
        // --- Pie Chart 1: BY TYPE ---
        val byTypeEntries = ArrayList<PieEntry>().apply {
            add(PieEntry(60f, "Lost items"))
            add(PieEntry(25f, "Found items"))
            add(PieEntry(15f, "Theft"))
        }
        val byTypeColors = listOf(
            // --- FIX START: Use defined colors from your project's colors.xml ---
            ContextCompat.getColor(requireContext(), R.color.purple_700),
            ContextCompat.getColor(requireContext(), R.color.purple_200),
            ContextCompat.getColor(requireContext(), R.color.teal_200)
            // --- FIX END ---
        )
        val byTypeCenterText = "60%"
        setupStyledPieChart(binding.chartByType, byTypeEntries, byTypeColors, byTypeCenterText)

        // --- Pie Chart 2: BY REPORTER ---
        val byReporterEntries = ArrayList<PieEntry>().apply {
            add(PieEntry(70f, "Student"))
            add(PieEntry(30f, "Security"))
        }
        val byReporterColors = listOf(
            // --- FIX START: Use defined colors from your project's colors.xml ---
            ContextCompat.getColor(requireContext(), R.color.purple_700),
            ContextCompat.getColor(requireContext(), R.color.purple_200)
            // --- FIX END ---
        )
        val byReporterCenterText = "30%"
        setupStyledPieChart(binding.chartByReporter, byReporterEntries, byReporterColors, byReporterCenterText)
    }

    private fun setupStyledPieChart(chart: PieChart, entries: List<PieEntry>, colors: List<Int>, centerText: String) {
        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            setDrawValues(false)
        }

        val pieData = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(chart))
            setValueTextSize(12f)
            setValueTextColor(Color.WHITE)
        }

        chart.apply {
            data = pieData
            description.isEnabled = false
            legend.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 70f
            transparentCircleRadius = 75f

            setDrawCenterText(true)
            setCenterText(centerText)
            setCenterTextSize(20f)
            setCenterTextTypeface(Typeface.DEFAULT_BOLD)
            setCenterTextColor(Color.BLACK)

            animateY(1000, Easing.EaseInOutQuad)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
