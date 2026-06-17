package com.example.loginandregistration.security.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.loginandregistration.databinding.FragmentSecurityReportsBinding
import com.example.loginandregistration.security.data.viewmodel.SecurityViewModel

class SecurityReportsFragment : Fragment() {

    private var _binding: FragmentSecurityReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SecurityViewModel by viewModels()
    private lateinit var reportsAdapter: SecurityReportsAdapter

    private var fullReportList: List<com.example.loginandregistration.models.Report> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecurityReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        observeViewModel()

        binding.btnAddReport.setOnClickListener {
            // TODO: Navigate to the "Add Report" screen
            Toast.makeText(context, "Add Report Clicked!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        reportsAdapter = SecurityReportsAdapter(
            onApproveClicked = { report ->
                viewModel.updateReportStatus(report.id, "Approved")
            },
            onRejectClicked = { report ->
                viewModel.updateReportStatus(report.id, "Rejected")
            }
        )
        binding.recyclerViewReports.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reportsAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.reports.observe(viewLifecycleOwner, Observer { reports ->
            fullReportList = reports
            reportsAdapter.submitList(reports)
        })

        viewModel.toastMessage.observe(viewLifecycleOwner, Observer { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        })
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString().lowercase()
            val filteredList = fullReportList.filter { report ->
                report.itemName.lowercase().contains(query) ||
                        report.description.lowercase().contains(query) ||
                        report.userEmail.lowercase().contains(query)
            }
            reportsAdapter.submitList(filteredList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
