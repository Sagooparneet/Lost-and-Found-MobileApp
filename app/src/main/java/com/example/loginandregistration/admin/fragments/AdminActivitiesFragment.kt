package com.example.loginandregistration.admin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.adapters.ActivityAdapter
import com.example.loginandregistration.admin.viewmodel.AdminDashboardViewModel
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class AdminActivitiesFragment : Fragment() {
    
    private val viewModel: AdminDashboardViewModel by activityViewModels()
    private lateinit var activityAdapter: ActivityAdapter
    
    // Views
    private lateinit var rvActivities: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_activities, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
    }
    
    private fun initViews(view: View) {
        rvActivities = view.findViewById(R.id.rvActivities)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
    }
    
    private fun setupRecyclerView() {
        activityAdapter = ActivityAdapter()
        rvActivities.apply {
            adapter = activityAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
            swipeRefresh.isRefreshing = false
        }
    }
    
    private fun observeViewModel() {
        viewModel.recentActivities.observe(viewLifecycleOwner) { activities ->
            activityAdapter.submitList(activities)
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefresh.isRefreshing = isLoading
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Show error message
            }
        }
    }
}