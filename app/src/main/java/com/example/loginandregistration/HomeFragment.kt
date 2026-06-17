package com.example.loginandregistration

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loginandregistration.utils.SearchManager
import com.example.loginandregistration.utils.UserRoleManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(), SearchableFragment {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemsAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var btnLoadMore: Button
    private lateinit var fabReport: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var searchView: SearchView
    private lateinit var tvAllItems: android.widget.TextView
    private lateinit var tvLostItems: android.widget.TextView
    private lateinit var tvFoundItems: android.widget.TextView
    private lateinit var tvItemsCount: android.widget.TextView
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Pagination state variables
    private var lastVisibleItem: DocumentSnapshot? = null
    private var isLoadingMore = false
    
    // Search state variables
    private var allItems: List<LostFoundItem> = emptyList()
    private var currentSearchQuery: String = ""
    
    // Filter state
    private var currentFilter: FilterType = FilterType.ALL
    
    enum class FilterType {
        ALL, LOST, FOUND
    }
    
    companion object {
        private const val TAG = "HomeFragment"
        private const val ITEMS_PER_PAGE = 20
    }
    
    /**
     * Show dialog to select report type (Lost or Found)
     * Requirements: 4.2, 4.3, 4.4
     */
    private fun showReportTypeDialog() {
        val dialog = ReportTypeDialog.newInstance { reportType ->
            navigateToReportWithType(reportType)
        }
        dialog.show(parentFragmentManager, "ReportTypeDialog")
    }
    
    /**
     * Navigate to ReportFragment with pre-selected type
     * Requirements: 3.1, 3.2, 4.4, 4.5
     */
    private fun navigateToReportWithType(reportType: String) {
        val bundle = Bundle().apply {
            putString(ReportFragment.ARG_REPORT_TYPE, reportType)
        }
        
        val reportFragment = ReportFragment().apply {
            arguments = bundle
        }
        
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, reportFragment)
            .addToBackStack(null)
            .commit()
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.recycler_view_items)
        progressBar = view.findViewById(R.id.progress_bar)
        btnLoadMore = view.findViewById(R.id.btn_load_more)
        fabReport = view.findViewById(R.id.fab_report)
        searchView = view.findViewById(R.id.search_view)
        tvAllItems = view.findViewById(R.id.tv_all_items)
        tvLostItems = view.findViewById(R.id.tv_lost_items)
        tvFoundItems = view.findViewById(R.id.tv_found_items)
        tvItemsCount = view.findViewById(R.id.tv_items_count)
        
        // Optimize RecyclerView for better performance
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true) // Items have fixed size
        recyclerView.setItemViewCacheSize(20) // Cache more items
        recyclerView.isNestedScrollingEnabled = true
        
        // Get current user email for role-based visibility
        // Requirement: 10.5
        val currentUserId = auth.currentUser?.uid ?: ""
        val userEmail = auth.currentUser?.email ?: ""
        
        adapter = ItemsAdapter(
            currentUserId = currentUserId,
            userEmail = userEmail,
            onItemClickListener = { item ->
                showItemDetailsDialog(item)
            }
        )
        recyclerView.adapter = adapter
        
        // Set up Load More button
        btnLoadMore.setOnClickListener {
            loadMoreItems()
        }
        
        // Set up FAB to show report type dialog
        fabReport.setOnClickListener {
            showReportTypeDialog()
        }
        
        // Set up search functionality
        setupSearchView()
        
        // Set up filter buttons
        setupFilterButtons()
        
        loadRecentItems()
    }
    
    private fun setupSearchView() {
        // Set up real-time search on the dashboard
        // Search filters items by name, category, location, and description
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val searchQuery = query ?: ""
                currentSearchQuery = searchQuery
                applySearchFilter(searchQuery)
                searchView.clearFocus() // Hide keyboard after submit
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                // Apply search in real-time as user types
                val searchQuery = newText ?: ""
                currentSearchQuery = searchQuery
                applySearchFilter(searchQuery)
                return true
            }
        })
        
        searchView.setOnCloseListener {
            currentSearchQuery = ""
            applySearchFilter("")
            false
        }
    }
    
    /**
     * Set up click listeners for filter buttons
     * Requirements: 2.1, 2.2, 2.4, 2.5
     */
    private fun setupFilterButtons() {
        // All items filter
        tvAllItems.setOnClickListener {
            currentFilter = FilterType.ALL
            updateButtonStates()
            applyFilters()
        }
        
        // Lost items filter - filter items where isLost equals true
        // Requirement 2.1: Filter items where isLost equals true
        tvLostItems.setOnClickListener {
            currentFilter = FilterType.LOST
            updateButtonStates()
            applyFilters()
        }
        
        // Found items filter - filter items where isLost equals false
        // Requirement 2.2: Filter items where isLost equals false
        tvFoundItems.setOnClickListener {
            currentFilter = FilterType.FOUND
            updateButtonStates()
            applyFilters()
        }
    }
    
    /**
     * Update visual feedback for filter buttons
     * Requirement 2.5: Provide visual feedback when a filter button is selected
     */
    private fun updateButtonStates() {
        val context = requireContext()
        
        // Reset all buttons to unselected state
        tvAllItems.setBackgroundResource(R.drawable.tab_unselected_background)
        tvAllItems.setTextColor(context.getColor(R.color.text_secondary))
        
        tvLostItems.setBackgroundResource(R.drawable.tab_unselected_background)
        tvLostItems.setTextColor(context.getColor(R.color.text_secondary))
        
        tvFoundItems.setBackgroundResource(R.drawable.tab_unselected_background)
        tvFoundItems.setTextColor(context.getColor(R.color.text_secondary))
        
        // Set selected button state
        when (currentFilter) {
            FilterType.ALL -> {
                tvAllItems.setBackgroundResource(R.drawable.tab_selected_background)
                tvAllItems.setTextColor(context.getColor(R.color.white))
            }
            FilterType.LOST -> {
                tvLostItems.setBackgroundResource(R.drawable.tab_selected_background)
                tvLostItems.setTextColor(context.getColor(R.color.white))
            }
            FilterType.FOUND -> {
                tvFoundItems.setBackgroundResource(R.drawable.tab_selected_background)
                tvFoundItems.setTextColor(context.getColor(R.color.white))
            }
        }
    }
    
    /**
     * Apply both filter and search to items
     * Requirements: 2.1, 2.2, 2.4
     */
    private fun applyFilters() {
        // First apply filter based on type
        val filteredByType = when (currentFilter) {
            FilterType.ALL -> allItems
            FilterType.LOST -> allItems.filter { it.isLost }
            FilterType.FOUND -> allItems.filter { !it.isLost && !it.status.equals("Returned", ignoreCase = true) }
        }
        
        // Then apply search filter if there's a search query
        val finalItems = if (currentSearchQuery.isBlank()) {
            filteredByType
        } else {
            SearchManager.filterItems(filteredByType, currentSearchQuery)
        }
        
        // Update adapter with filtered items
        adapter.submitList(finalItems)
        
        // Update counts in filter buttons
        updateFilterCounts()
        
        Log.d(TAG, "Applied filters - Type: $currentFilter, Search: '$currentSearchQuery', Results: ${finalItems.size}")
    }
    
    /**
     * Update the count labels on filter buttons with actual item counts
     */
    private fun updateFilterCounts() {
        val allCount = allItems.size
        val lostCount = allItems.count { it.isLost }
        val foundCount = allItems.count { !it.isLost && !it.status.equals("Returned", ignoreCase = true) }
        
        tvAllItems.text = "All ($allCount)"
        tvLostItems.text = "Lost ($lostCount)"
        tvFoundItems.text = "Found ($foundCount)"
        
        // Update the items count display based on current filter
        val displayCount = when (currentFilter) {
            FilterType.ALL -> allCount
            FilterType.LOST -> lostCount
            FilterType.FOUND -> foundCount
        }
        tvItemsCount.text = "$displayCount items"
        
        Log.d(TAG, "Updated counts - All: $allCount, Lost: $lostCount, Found: $foundCount, Display: $displayCount")
    }
    
    private fun loadRecentItems() {
        // Use lifecycleScope to launch coroutine tied to fragment lifecycle
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Show loading indicator on Main thread
                withContext(Dispatchers.Main) {
                    showLoading()
                }
                
                val currentUser = auth.currentUser
                val userEmail = currentUser?.email ?: ""
                val isSecurity = UserRoleManager.canViewSensitiveInfo(userEmail)
                
                // Fetch ALL items from Firestore without pagination limit
                // Requirement 1.1: Remove pagination limit to fetch all items initially
                val querySnapshot = db.collection("items")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()  // Removed .limit() to fetch all items
                    .await()
                
                val finalSnapshot = querySnapshot
                
                // Log item count from Firestore
                // Requirement 1.3: Add logging to track item count through data flow
                Log.d(TAG, "Firestore query returned ${finalSnapshot.documents.size} documents")
                
                // Store last visible item for pagination (kept for potential future use)
                if (finalSnapshot.documents.isNotEmpty()) {
                    lastVisibleItem = finalSnapshot.documents.last()
                }
                
                // Map documents to LostFoundItem objects, skipping problematic items
                val fetchedItems = finalSnapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(LostFoundItem::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse document ${doc.id}: ${e.message}")
                        null  // Skip problematic items silently
                    }
                }
                
                // Log successfully parsed items count
                Log.d(TAG, "Successfully parsed ${fetchedItems.size} items from Firestore")
                
                // Requirement 5.1, 5.2: All authenticated users can read items with any status
                // No client-side filtering by status - show all items
                val filteredItems = fetchedItems
                
                // Log items count
                Log.d(TAG, "Displaying ${filteredItems.size} items (all statuses included)")
                
                // Update UI on Main thread
                withContext(Dispatchers.Main) {
                    hideLoading()
                    
                    if (filteredItems.isEmpty()) {
                        // Show empty state instead of adding sample data
                        Log.d(TAG, "No items to display - showing empty state")
                        adapter.submitList(emptyList())
                        showLoadMoreButton(false)
                    } else {
                        // Store all items for search filtering
                        allItems = filteredItems
                        
                        // Apply current filters (both type and search)
                        applyFilters()
                        
                        // Hide Load More button since we're loading all items at once
                        showLoadMoreButton(false)
                    }
                }
                
            } catch (e: FirebaseFirestoreException) {
                // Handle Firestore-specific errors silently for better UX
                Log.e(TAG, "Firestore error loading items: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    hideLoading()
                    adapter.submitList(emptyList())
                }
            } catch (e: Exception) {
                // Handle generic errors silently
                Log.e(TAG, "Error loading items: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    hideLoading()
                    adapter.submitList(emptyList())
                }
            }
        }
    }
    
    private fun loadMoreItems() {
        // Prevent multiple simultaneous loads
        if (isLoadingMore || lastVisibleItem == null) {
            return
        }
        
        isLoadingMore = true
        btnLoadMore.isEnabled = false
        
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser
                val userEmail = currentUser?.email ?: ""
                val isSecurity = UserRoleManager.canViewSensitiveInfo(userEmail)
                
                // Fetch next page from Firestore
                val querySnapshot = db.collection("items")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .startAfter(lastVisibleItem!!)
                    .limit(ITEMS_PER_PAGE.toLong())
                    .get()
                    .await()
                
                // Update last visible item for next pagination
                if (querySnapshot.documents.isNotEmpty()) {
                    lastVisibleItem = querySnapshot.documents.last()
                }
                
                // Map documents to LostFoundItem objects
                val newItems = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(LostFoundItem::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                // Requirement 5.1, 5.2: All authenticated users can read items with any status
                // No client-side filtering by status - show all items
                val filteredNewItems = newItems
                
                // Update UI on Main thread
                withContext(Dispatchers.Main) {
                    isLoadingMore = false
                    btnLoadMore.isEnabled = true
                    
                    if (filteredNewItems.isNotEmpty()) {
                        // Update all items list
                        val updatedAllItems = allItems.toMutableList()
                        updatedAllItems.addAll(filteredNewItems)
                        allItems = updatedAllItems
                        
                        // Apply current filters (both type and search)
                        applyFilters()
                        
                        // Show/hide Load More button based on items count
                        showLoadMoreButton(filteredNewItems.size == ITEMS_PER_PAGE)
                    } else {
                        // No more items to load
                        showLoadMoreButton(false)
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoadingMore = false
                    btnLoadMore.isEnabled = true
                }
            }
        }
    }
    

    
    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }
    
    private fun hideLoading() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
    
    private fun showLoadMoreButton(show: Boolean) {
        btnLoadMore.visibility = if (show && !isLoadingMore) View.VISIBLE else View.GONE
    }
    

    
    /**
     * Show item details dialog when an item is clicked
     */
    private fun showItemDetailsDialog(item: LostFoundItem) {
        val dialog = ItemDetailsDialog.newInstance(item)
        dialog.show(parentFragmentManager, "ItemDetailsDialog")
    }
    
    /**
     * Applies search filter to displayed items
     * Implementation of SearchableFragment interface
     */
    override fun applySearchFilter(query: String) {
        currentSearchQuery = query
        applyFilters()
    }
}
