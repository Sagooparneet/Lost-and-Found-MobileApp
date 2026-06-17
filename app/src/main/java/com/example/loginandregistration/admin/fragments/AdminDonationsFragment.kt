package com.example.loginandregistration.admin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.adapters.DonationQueueAdapter
import com.example.loginandregistration.admin.dialogs.DonationDetailsDialog
import com.example.loginandregistration.admin.dialogs.DonationFilterBottomSheet
import com.example.loginandregistration.admin.dialogs.MarkReadyForDonationDialog
import com.example.loginandregistration.admin.dialogs.MarkAsDonatedDialog
import com.example.loginandregistration.admin.models.DonationItem
import com.example.loginandregistration.admin.models.DonationStatus
import com.example.loginandregistration.admin.models.DonationFilter
import com.example.loginandregistration.admin.viewmodel.AdminDashboardViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

/**
 * Fragment for managing donation queue and workflow
 * Requirements: 1.2, 1.4, 3.2, 8.3
 * Task: 3
 */
class AdminDonationsFragment : Fragment(), DonationFilterBottomSheet.FilterListener {
    
    private val viewModel: AdminDashboardViewModel by activityViewModels()
    private lateinit var donationAdapter: DonationQueueAdapter
    
    // Views
    private lateinit var tabLayout: TabLayout
    private lateinit var rvDonations: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var fabFilter: FloatingActionButton
    private lateinit var emptyStateView: View
    private lateinit var btnRetry: com.google.android.material.button.MaterialButton
    
    // Current filter state
    private var currentFilter = DonationFilter()
    private var currentTab = DonationStatus.PENDING
    private var bottomSheetFilterCriteria: DonationFilterBottomSheet.FilterCriteria? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_donations, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupTabs()
        setupRecyclerView()
        setupFAB()
        setupSwipeRefresh()
        observeViewModel()
        
        // Load initial data
        viewModel.loadDonationQueue()
    }
    
    private fun initViews(view: View) {
        tabLayout = view.findViewById(R.id.tabLayout)
        rvDonations = view.findViewById(R.id.rvDonations)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        fabFilter = view.findViewById(R.id.fabFilter)
        emptyStateView = view.findViewById(R.id.emptyStateView)
        btnRetry = view.findViewById(R.id.btnRetry)
        
        // Setup retry button
        btnRetry.setOnClickListener {
            btnRetry.visibility = View.GONE
            viewModel.loadDonationQueue()
        }
    }
    
    /**
     * Setup tab navigation for donation statuses
     * Requirements: 3.2
     */
    private fun setupTabs() {
        // Add tabs for each donation status
        tabLayout.addTab(tabLayout.newTab().setText("Pending"))
        tabLayout.addTab(tabLayout.newTab().setText("Ready"))
        tabLayout.addTab(tabLayout.newTab().setText("Donated"))
        
        // Handle tab selection
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        currentTab = DonationStatus.PENDING
                        currentFilter = currentFilter.withStatus(DonationStatus.PENDING)
                    }
                    1 -> {
                        currentTab = DonationStatus.READY
                        currentFilter = currentFilter.withStatus(DonationStatus.READY)
                    }
                    2 -> {
                        currentTab = DonationStatus.DONATED
                        currentFilter = currentFilter.withStatus(DonationStatus.DONATED)
                    }
                }
                applyFilters()
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    /**
     * Setup RecyclerView with adapter
     * Requirements: 3.2
     */
    private fun setupRecyclerView() {
        donationAdapter = DonationQueueAdapter { item, action ->
            when (action) {
                "view_details" -> showDonationDetails(item)
                "mark_ready" -> showMarkReadyDialog(item)
                "mark_donated" -> showMarkDonatedDialog(item)
            }
        }
        
        rvDonations.apply {
            adapter = donationAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    /**
     * Setup FAB for advanced filters
     * Requirements: 1.2, 1.4
     */
    private fun setupFAB() {
        fabFilter.setOnClickListener {
            showFilterBottomSheet()
        }
    }
    
    /**
     * Show filter bottom sheet
     * Requirements: 1.4
     */
    private fun showFilterBottomSheet() {
        val bottomSheet = DonationFilterBottomSheet.newInstance(this)
        bottomSheet.show(parentFragmentManager, "DonationFilterBottomSheet")
    }
    
    /**
     * Handle filter criteria from bottom sheet
     * Requirements: 1.2, 1.4
     */
    override fun onFiltersApplied(criteria: DonationFilterBottomSheet.FilterCriteria) {
        bottomSheetFilterCriteria = criteria
        applyFilters()
    }
    
    /**
     * Filter donation eligible items (1+ year old)
     * Handles null/invalid timestamps gracefully
     * Requirements: 3.1, 3.2, 3.3
     */
    private fun filterDonationEligibleItems(items: List<DonationItem>): List<DonationItem> {
        val oneYearInDays = 365L
        
        return try {
            items.filter { item ->
                try {
                    val ageInDays = item.getAgeInDays()
                    // Filter out items with invalid timestamps (age = -1)
                    ageInDays >= 0 && ageInDays >= oneYearInDays
                } catch (e: Exception) {
                    android.util.Log.e("AdminDonations", "Error calculating age for item ${item.itemId}", e)
                    false
                }
            }.sortedByDescending { 
                try {
                    it.getAgeInDays()
                } catch (e: Exception) {
                    android.util.Log.e("AdminDonations", "Error sorting by age for item ${it.itemId}", e)
                    0L
                }
            } // Sort by age, oldest first
        } catch (e: Exception) {
            android.util.Log.e("AdminDonations", "Error filtering donation eligible items", e)
            Snackbar.make(requireView(), "Error filtering items: ${e.message}", Snackbar.LENGTH_LONG).show()
            emptyList()
        }
    }
    
    /**
     * Apply current filters to donation queue
     * Combines tab status with bottom sheet criteria
     * Requirements: 1.2, 1.4, 3.9, 5.1, 5.2, 5.3
     */
    private fun applyFilters() {
        try {
            val donations = viewModel.donationQueue.value ?: emptyList()
            
            android.util.Log.d("AdminDonations", "applyFilters: Total donations = ${donations.size}, Current tab = $currentTab")
            
            // First filter by tab status
            var filteredDonations = try {
                donations.filter { it.status == currentTab }
            } catch (e: Exception) {
                android.util.Log.e("AdminDonations", "Error filtering by status", e)
                Snackbar.make(requireView(), "Error filtering by status", Snackbar.LENGTH_SHORT).show()
                donations
            }
            
            android.util.Log.d("AdminDonations", "applyFilters: After tab filter = ${filteredDonations.size}")
            
            // Apply age filtering for donation eligible items (1+ year old)
            filteredDonations = filterDonationEligibleItems(filteredDonations)
            
            android.util.Log.d("AdminDonations", "applyFilters: After age filter (1+ year) = ${filteredDonations.size}")
            
            // Then apply bottom sheet filters if any
            bottomSheetFilterCriteria?.let { criteria ->
                try {
                    filteredDonations = filteredDonations.filter { donation ->
                        var matches = true
                        
                        // Filter by category
                        if (criteria.category != null) {
                            try {
                                matches = matches && donation.category.equals(criteria.category, ignoreCase = true)
                            } catch (e: Exception) {
                                android.util.Log.e("AdminDonations", "Error filtering by category", e)
                            }
                        }
                        
                        // Filter by age range
                        if (criteria.ageRange != null) {
                            try {
                                val ageInDays = donation.getAgeInDays()
                                // Skip items with invalid age
                                if (ageInDays >= 0) {
                                    matches = matches && when (criteria.ageRange) {
                                        "< 30 days" -> ageInDays < 30
                                        "30-90 days" -> ageInDays in 30..90
                                        "90-180 days" -> ageInDays in 90..180
                                        "180-365 days" -> ageInDays in 180..365
                                        "> 365 days" -> ageInDays > 365
                                        else -> true
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("AdminDonations", "Error filtering by age range", e)
                            }
                        }
                        
                        // Filter by location
                        if (criteria.location != null) {
                            try {
                                matches = matches && donation.location.contains(criteria.location, ignoreCase = true)
                            } catch (e: Exception) {
                                android.util.Log.e("AdminDonations", "Error filtering by location", e)
                            }
                        }
                        
                        matches
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AdminDonations", "Error applying bottom sheet filters", e)
                    Snackbar.make(requireView(), "Error applying filters: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
            
            android.util.Log.d("AdminDonations", "applyFilters: After all filters = ${filteredDonations.size}")
            
            // Handle empty state
            if (filteredDonations.isEmpty()) {
                showEmptyState()
            } else {
                hideEmptyState()
            }
            
            donationAdapter.submitList(filteredDonations)
        } catch (e: Exception) {
            android.util.Log.e("AdminDonations", "Critical error in applyFilters", e)
            Snackbar.make(requireView(), "Error loading donations: ${e.message}", Snackbar.LENGTH_LONG).show()
            showEmptyState(isError = true, errorMessage = "Failed to load donations: ${e.message}")
        }
    }
    
    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            viewModel.loadDonationQueue()
        }
    }
    
    /**
     * Observe ViewModel data
     * Requirements: 3.2, 5.1, 5.2, 5.3, 8.3
     */
    private fun observeViewModel() {
        // Observe donation queue with logging
        viewModel.donationQueue.observe(viewLifecycleOwner) { donations ->
            android.util.Log.d("AdminDonations", "observeViewModel: Received ${donations.size} donations")
            applyFilters()
            swipeRefresh.isRefreshing = false
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefresh.isRefreshing = isLoading
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Show error message
            }
        }
        
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(requireView(), it, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Show donation details dialog
     * Requirements: 3.3
     */
    private fun showDonationDetails(item: DonationItem) {
        val dialog = DonationDetailsDialog.newInstance(item)
        dialog.show(parentFragmentManager, "DonationDetailsDialog")
    }
    
    /**
     * Show mark ready for donation dialog
     * Requirements: 3.4
     */
    private fun showMarkReadyDialog(item: DonationItem) {
        val dialog = MarkReadyForDonationDialog.newInstance(item) {
            viewModel.markItemReadyForDonation(item.itemId)
        }
        dialog.show(parentFragmentManager, "MarkReadyForDonationDialog")
    }
    
    /**
     * Show mark as donated dialog
     * Requirements: 3.5
     */
    private fun showMarkDonatedDialog(item: DonationItem) {
        val dialog = MarkAsDonatedDialog.newInstance(item) { recipient, value ->
            viewModel.markItemAsDonated(item.itemId, recipient, value)
        }
        dialog.show(parentFragmentManager, "MarkAsDonatedDialog")
    }
    
    /**
     * Show empty state view
     * Requirements: 5.1, 5.2, 5.3
     */
    private fun showEmptyState(isError: Boolean = false, errorMessage: String = "") {
        emptyStateView.visibility = View.VISIBLE
        rvDonations.visibility = View.GONE
        
        val tvEmptyTitle = emptyStateView.findViewById<android.widget.TextView>(R.id.tvEmptyTitle)
        val tvEmptyMessage = emptyStateView.findViewById<android.widget.TextView>(R.id.tvEmptyMessage)
        
        if (isError) {
            tvEmptyTitle.text = getString(R.string.error_loading_donations)
            tvEmptyMessage.text = errorMessage.ifEmpty { getString(R.string.no_donations_message) }
            btnRetry.visibility = View.VISIBLE
        } else {
            tvEmptyTitle.text = getString(R.string.no_donations_title)
            tvEmptyMessage.text = getString(R.string.no_donations_message)
            btnRetry.visibility = View.GONE
        }
        
        android.util.Log.d("AdminDonations", "showEmptyState: isError = $isError")
    }
    
    /**
     * Hide empty state view
     * Requirements: 5.1, 5.2, 5.3
     */
    private fun hideEmptyState() {
        emptyStateView.visibility = View.GONE
        rvDonations.visibility = View.VISIBLE
        btnRetry.visibility = View.GONE
        
        android.util.Log.d("AdminDonations", "hideEmptyState: Showing donations list")
    }
    
    companion object {
        fun newInstance() = AdminDonationsFragment()
    }
}
