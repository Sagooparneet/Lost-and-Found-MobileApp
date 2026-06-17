package com.example.loginandregistration.admin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.adapters.AdminItemsAdapter
import com.example.loginandregistration.admin.dialogs.ItemFilterBottomSheet
import com.example.loginandregistration.admin.viewmodel.AdminDashboardViewModel
import com.example.loginandregistration.admin.models.ItemStatus
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class AdminItemsFragment : Fragment(), ItemFilterBottomSheet.FilterListener {
    
    private val viewModel: AdminDashboardViewModel by activityViewModels()
    private lateinit var itemsAdapter: AdminItemsAdapter
    
    // Views
    private lateinit var searchView: SearchView
    private lateinit var rvItems: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var fabFilter: FloatingActionButton
    private lateinit var emptyStateView: View
    private lateinit var tvEmptyTitle: android.widget.TextView
    private lateinit var tvEmptyMessage: android.widget.TextView
    private lateinit var btnRetry: com.google.android.material.button.MaterialButton
    
    private var currentStatusFilter: ItemStatus? = null
    private var currentCategoryFilter: String? = null
    private var currentSearchQuery: String = ""
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_items, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupSearchView()
        setupSwipeRefresh()
        observeViewModel()
        
        // Load initial data
        viewModel.loadAllItemsWithStatus()
    }
    
    private fun initViews(view: View) {
        searchView = view.findViewById(R.id.searchView)
        rvItems = view.findViewById(R.id.rvItems)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        fabFilter = view.findViewById(R.id.fabFilter)
        emptyStateView = view.findViewById(R.id.emptyStateView)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage)
        btnRetry = view.findViewById(R.id.btnRetry)
        
        fabFilter.setOnClickListener {
            showFilterBottomSheet()
        }
        
        btnRetry.setOnClickListener {
            hideEmptyState()
            swipeRefresh.isRefreshing = true
            viewModel.loadAllItemsWithStatus()
        }
    }
    
    private fun setupRecyclerView() {
        itemsAdapter = AdminItemsAdapter { item, action ->
            when (action) {
                "view" -> {
                    // Show item details
                    showItemDetails(item)
                }
                "edit" -> {
                    // Show edit item dialog
                    showEditItemDialog(item)
                }
                "delete" -> {
                    // Show delete confirmation
                    showDeleteConfirmation(item)
                }
            }
        }
        
        rvItems.apply {
            adapter = itemsAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query ?: ""
                applyFilters()
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText ?: ""
                applyFilters()
                return true
            }
        })
    }
    

    
    private fun applyFilters() {
        val filters = mutableMapOf<String, String>()
        
        currentStatusFilter?.let {
            filters["status"] = it.name
        }
        
        currentCategoryFilter?.let {
            filters["category"] = it
        }
        
        if (currentSearchQuery.isNotEmpty()) {
            viewModel.searchItemsEnhanced(currentSearchQuery, filters)
        } else {
            viewModel.loadAllItemsWithStatus()
        }
    }
    
    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            viewModel.loadAllItemsWithStatus()
        }
    }
    
    private fun observeViewModel() {
        // Observe all items with status
        viewModel.allItemsWithStatus.observe(viewLifecycleOwner) { items ->
            var filteredItems = items
            
            // Apply status filter
            currentStatusFilter?.let { status ->
                filteredItems = filteredItems.filter { it.status == status }
            }
            
            // Apply category filter
            currentCategoryFilter?.let { category ->
                filteredItems = filteredItems.filter { it.category == category }
            }
            
            // Apply search filter
            if (currentSearchQuery.isNotEmpty()) {
                val query = currentSearchQuery.lowercase()
                filteredItems = filteredItems.filter {
                    it.name.lowercase().contains(query) ||
                    it.description.lowercase().contains(query) ||
                    it.location.lowercase().contains(query) ||
                    it.userEmail.lowercase().contains(query)
                }
            }
            
            // Handle empty state
            if (filteredItems.isEmpty()) {
                showEmptyState(isError = false)
            } else {
                hideEmptyState()
            }
            
            itemsAdapter.submitList(filteredItems)
            swipeRefresh.isRefreshing = false
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefresh.isRefreshing = isLoading
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                swipeRefresh.isRefreshing = false
                
                // Only show user-relevant errors, suppress technical parsing errors
                val isUserRelevantError = !error.contains("convert", ignoreCase = true) &&
                                         !error.contains("parse", ignoreCase = true) &&
                                         !error.contains("cast", ignoreCase = true) &&
                                         !error.contains("number format", ignoreCase = true)
                
                if (isUserRelevantError) {
                    showEmptyState(isError = true, errorMessage = error)
                    
                    // Show Snackbar with retry action
                    Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.retry)) {
                            viewModel.loadAllItemsWithStatus()
                        }
                        .show()
                } else {
                    // Log technical errors but don't show to user
                    android.util.Log.w("AdminItemsFragment", "Technical error suppressed: $error")
                }
            }
        }
        
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showEmptyState(isError: Boolean = false, errorMessage: String = "") {
        emptyStateView.visibility = View.VISIBLE
        rvItems.visibility = View.GONE
        
        if (isError) {
            tvEmptyTitle.text = getString(R.string.error_loading_items)
            tvEmptyMessage.text = errorMessage.ifEmpty { getString(R.string.no_items_message) }
            btnRetry.visibility = View.VISIBLE
        } else {
            tvEmptyTitle.text = getString(R.string.no_items_title)
            tvEmptyMessage.text = getString(R.string.no_items_message)
            btnRetry.visibility = View.GONE
        }
    }
    
    private fun hideEmptyState() {
        emptyStateView.visibility = View.GONE
        rvItems.visibility = View.VISIBLE
    }
    
    private fun showItemDetails(item: com.example.loginandregistration.LostFoundItem) {
        // Navigate to ItemDetailsFragment using Navigation component
        val bundle = Bundle().apply {
            putString("item_id", item.id)
        }
        findNavController().navigate(R.id.itemDetailsFragment, bundle)
    }
    
    private fun showEditItemDialog(item: com.example.loginandregistration.LostFoundItem) {
        // For now, navigate to item details where edit functionality exists
        // In the future, this can be changed to a dedicated edit screen
        val bundle = Bundle().apply {
            putString("item_id", item.id)
            putBoolean("is_edit_mode", true)
        }
        try {
            findNavController().navigate(R.id.itemDetailsFragment, bundle)
        } catch (e: Exception) {
            // If navigation fails, show item details
            showItemDetails(item)
            android.util.Log.w("AdminItemsFragment", "Edit navigation fallback: ${e.message}")
        }
    }
    
    private fun showDeleteConfirmation(item: com.example.loginandregistration.LostFoundItem) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete this item? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteItem(item.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showFilterBottomSheet() {
        val bottomSheet = ItemFilterBottomSheet.newInstance(this)
        bottomSheet.show(parentFragmentManager, "ItemFilterBottomSheet")
    }
    
    // Implement FilterListener interface
    override fun onFiltersApplied(criteria: ItemFilterBottomSheet.FilterCriteria) {
        // Update status filter
        currentStatusFilter = criteria.status
        
        // Update category filter
        currentCategoryFilter = criteria.category
        
        // Apply all filters
        applyFilters()
    }
}