package com.example.loginandregistration.admin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.adapters.ActivityLogAdapter
import com.example.loginandregistration.admin.dialogs.ActivityLogFilterBottomSheet
import com.example.loginandregistration.admin.models.ActivityLog
import com.example.loginandregistration.admin.viewmodel.AdminDashboardViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Fragment for displaying and managing activity logs
 * Requirements: 5.3, 8.4
 * Task: 12.1 Create AdminActivityLogFragment
 */
class AdminActivityLogFragment : Fragment() {
    
    private val viewModel: AdminDashboardViewModel by activityViewModels()
    private lateinit var activityLogAdapter: ActivityLogAdapter
    
    // Views
    private lateinit var searchView: SearchView
    private lateinit var rvActivityLogs: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var chipGroupFilters: ChipGroup
    private lateinit var chipAllActions: Chip
    private lateinit var chipUserActions: Chip
    private lateinit var chipAdminActions: Chip
    private lateinit var chipSystemEvents: Chip
    private lateinit var chipMoreFilters: Chip
    private lateinit var emptyStateLayout: View
    private lateinit var progressBar: ProgressBar
    
    // State
    private var currentSearchQuery: String = ""
    private var currentFilters: MutableMap<String, String> = mutableMapOf()
    private var allLogsList: List<ActivityLog> = emptyList()
    private var isLoadingMore = false
    private var currentPage = 0
    private val pageSize = 50
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_activity_log, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupSearchView()
        setupFilterChips()
        setupSwipeRefresh()
        setupPagination()
        observeViewModel()
        
        // Load initial data
        loadActivityLogs()
    }
    
    private fun initViews(view: View) {
        searchView = view.findViewById(R.id.searchView)
        rvActivityLogs = view.findViewById(R.id.rvActivityLogs)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        chipGroupFilters = view.findViewById(R.id.chipGroupFilters)
        chipAllActions = view.findViewById(R.id.chipAllActions)
        chipUserActions = view.findViewById(R.id.chipUserActions)
        chipAdminActions = view.findViewById(R.id.chipAdminActions)
        chipSystemEvents = view.findViewById(R.id.chipSystemEvents)
        chipMoreFilters = view.findViewById(R.id.chipMoreFilters)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        progressBar = view.findViewById(R.id.progressBar)
    }
    
    private fun setupRecyclerView() {
        activityLogAdapter = ActivityLogAdapter { log ->
            // Show activity detail dialog
            showActivityDetailDialog(log)
        }
        
        rvActivityLogs.apply {
            adapter = activityLogAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }
    
    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query ?: ""
                performSearch()
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText ?: ""
                // Debounce search for better performance
                if (currentSearchQuery.length >= 3 || currentSearchQuery.isEmpty()) {
                    performSearch()
                }
                return true
            }
        })
    }
    
    private fun setupFilterChips() {
        // All Actions filter
        chipAllActions.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilters.remove("actionCategory")
                chipUserActions.isChecked = false
                chipAdminActions.isChecked = false
                chipSystemEvents.isChecked = false
                applyFilters()
            }
        }
        
        // User Actions filter
        chipUserActions.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilters["actionCategory"] = "user"
                chipAllActions.isChecked = false
                chipAdminActions.isChecked = false
                chipSystemEvents.isChecked = false
                applyFilters()
            }
        }
        
        // Admin Actions filter
        chipAdminActions.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilters["actionCategory"] = "admin"
                chipAllActions.isChecked = false
                chipUserActions.isChecked = false
                chipSystemEvents.isChecked = false
                applyFilters()
            }
        }
        
        // System Events filter
        chipSystemEvents.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilters["actionCategory"] = "system"
                chipAllActions.isChecked = false
                chipUserActions.isChecked = false
                chipAdminActions.isChecked = false
                applyFilters()
            }
        }
        
        // More Filters button
        chipMoreFilters.setOnClickListener {
            showFilterBottomSheet()
        }
    }
    
    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            refreshData()
        }
    }
    
    private fun setupPagination() {
        rvActivityLogs.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                
                // Load more when reaching the end
                if (!isLoadingMore && 
                    (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 &&
                    firstVisibleItemPosition >= 0) {
                    loadMoreLogs()
                }
            }
        })
    }
    
    private fun observeViewModel() {
        viewModel.activityLogs.observe(viewLifecycleOwner) { logs ->
            allLogsList = logs
            updateUI(logs)
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoadingMore) {
                swipeRefresh.isRefreshing = isLoading
                progressBar.visibility = if (isLoading && allLogsList.isEmpty()) View.VISIBLE else View.GONE
            }
        }
        
        // Error display disabled - uncomment to show errors
        // viewModel.error.observe(viewLifecycleOwner) { error ->
        //     error?.let {
        //         showError(it)
        //     }
        // }
    }
    
    private fun loadActivityLogs() {
        currentPage = 0
        viewModel.loadActivityLogs(pageSize, currentFilters)
    }
    
    private fun loadMoreLogs() {
        if (isLoadingMore) return
        
        isLoadingMore = true
        currentPage++
        
        // Load next page
        viewModel.loadActivityLogs(pageSize * (currentPage + 1), currentFilters)
        
        // Reset loading flag after a delay
        rvActivityLogs.postDelayed({
            isLoadingMore = false
        }, 1000)
    }
    
    private fun refreshData() {
        currentPage = 0
        loadActivityLogs()
    }
    
    private fun performSearch() {
        if (currentSearchQuery.isBlank()) {
            // If search is empty, reload with filters
            loadActivityLogs()
        } else {
            // Perform search with current filters
            viewModel.searchActivityLogs(currentSearchQuery, currentFilters)
        }
    }
    
    private fun applyFilters() {
        currentPage = 0
        if (currentSearchQuery.isBlank()) {
            loadActivityLogs()
        } else {
            performSearch()
        }
    }
    
    private fun updateUI(logs: List<ActivityLog>) {
        if (logs.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            rvActivityLogs.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            rvActivityLogs.visibility = View.VISIBLE
            activityLogAdapter.submitList(logs)
        }
    }
    
    private fun showFilterBottomSheet() {
        val bottomSheet = ActivityLogFilterBottomSheet.newInstance(currentFilters)
        bottomSheet.setOnFiltersAppliedListener { filters ->
            currentFilters.clear()
            currentFilters.putAll(filters)
            applyFilters()
        }
        bottomSheet.show(parentFragmentManager, "ActivityLogFilterBottomSheet")
    }
    
    private fun showActivityDetailDialog(log: ActivityLog) {
        val dialog = com.example.loginandregistration.admin.dialogs.ActivityDetailDialog.newInstance(log)
        dialog.show(parentFragmentManager, "ActivityDetailDialog")
    }
    
    private fun showError(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            requireView(),
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        ).show()
    }
    
    companion object {
        fun newInstance() = AdminActivityLogFragment()
    }
}
