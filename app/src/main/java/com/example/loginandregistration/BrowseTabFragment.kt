package com.example.loginandregistration

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loginandregistration.utils.SearchManager
import com.example.loginandregistration.utils.UserRoleManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Fragment for displaying filtered items in browse tabs
 * Requirements: 6.2, 6.3, 6.4, 7.5
 */
class BrowseTabFragment : Fragment(), SearchableFragment {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemsAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var emptyMessage: TextView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var filterType: TabFilterType = TabFilterType.LOST
    private var allItems: List<LostFoundItem> = emptyList()
    private var currentSearchQuery: String = ""
    
    companion object {
        private const val TAG = "BrowseTabFragment"
        private const val ARG_FILTER_TYPE = "filter_type"
        private const val STATE_SEARCH_QUERY = "search_query"
        
        fun newInstance(filterType: TabFilterType): BrowseTabFragment {
            return BrowseTabFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FILTER_TYPE, filterType.name)
                }
            }
        }
    }
    
    enum class TabFilterType {
        LOST,    // Lost items with status "Approved"
        FOUND,   // Found items with status "Approved"
        RETURNED, // Items with status "Returned"
        REQUESTED, // Items that have been requested and approved by security
        ALL      // All items with status "Approved" or "Returned"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val filterTypeName = it.getString(ARG_FILTER_TYPE)
            filterType = TabFilterType.valueOf(filterTypeName ?: TabFilterType.LOST.name)
        }
        
        // Restore search query on configuration changes
        // Requirement: 4.3
        savedInstanceState?.let {
            currentSearchQuery = it.getString(STATE_SEARCH_QUERY, "")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_browse_tab, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.recycler_view_items)
        progressBar = view.findViewById(R.id.progress_bar)
        emptyState = view.findViewById(R.id.empty_state)
        emptyMessage = view.findViewById(R.id.tv_empty_message)
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        // Get current user ID and email for role-based visibility
        // Requirement: 10.5
        val currentUserId = auth.currentUser?.uid ?: ""
        val userEmail = auth.currentUser?.email ?: ""
        
        // Initialize adapter with claim click listener
        adapter = ItemsAdapter(
            currentUserId = currentUserId,
            userEmail = userEmail,
            onClaimClickListener = { item ->
                // Handle claim button click - will be implemented in task 6
                Toast.makeText(requireContext(), "Claim functionality coming soon", Toast.LENGTH_SHORT).show()
            },
            pendingClaimItemIds = emptySet(), // Will be populated when claim system is implemented
            onItemClickListener = { item ->
                showItemDetailsDialog(item)
            }
        )
        recyclerView.adapter = adapter
        
        // Set empty message based on filter type
        emptyMessage.text = when (filterType) {
            TabFilterType.LOST -> "No lost items found"
            TabFilterType.FOUND -> "No found items available"
            TabFilterType.RETURNED -> "No returned items"
            TabFilterType.REQUESTED -> "No requested items"
            TabFilterType.ALL -> "No items found"
        }
        
        loadItems()
    }
    
    private fun loadItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading()
                
                getItemsFlow()
                    .flowOn(Dispatchers.IO)
                    .catch { e ->
                        hideLoading()
                        handleError(e)
                    }
                    .collect { items ->
                        withContext(Dispatchers.Main) {
                            hideLoading()
                            
                            // Store all items for search filtering
                            // Requirement: 7.5
                            allItems = items
                            
                            // Apply current search filter
                            val displayItems = if (currentSearchQuery.isBlank()) {
                                items
                            } else {
                                SearchManager.filterItems(items, currentSearchQuery)
                            }
                            
                            adapter.submitList(displayItems)
                            
                            // Show empty state if no items
                            if (displayItems.isEmpty()) {
                                showEmptyState()
                            } else {
                                hideEmptyState()
                            }
                        }
                    }
            } catch (e: Exception) {
                hideLoading()
                handleError(e)
            }
        }
    }
    
    private fun getItemsFlow() = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated")
            close(Exception("User not authenticated"))
            return@callbackFlow
        }
        
        val userEmail = currentUser.email ?: ""
        val userId = currentUser.uid
        Log.d(TAG, "Loading items for filter: $filterType, user: $userEmail")
        
        // For REQUESTED tab, we need to get approved item requests first
        if (filterType == TabFilterType.REQUESTED) {
            val requestListener = db.collection("itemRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", ItemRequest.RequestStatus.APPROVED)
                .addSnapshotListener { requestSnapshot, requestError ->
                    if (requestError != null) {
                        Log.e(TAG, "Error loading item requests: ${requestError.message}", requestError)
                        close(requestError)
                        return@addSnapshotListener
                    }
                    
                    val approvedItemIds = requestSnapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(ItemRequest::class.java)?.itemId
                    } ?: emptyList()
                    
                    Log.d(TAG, "Found ${approvedItemIds.size} approved item requests")
                    
                    if (approvedItemIds.isEmpty()) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    // Now get the actual items
                    db.collection("items")
                        .get()
                        .addOnSuccessListener { itemSnapshot ->
                            val requestedItems = itemSnapshot.documents.mapNotNull { doc ->
                                try {
                                    val item = doc.toObject(LostFoundItem::class.java)?.copy(id = doc.id)
                                    if (item != null && approvedItemIds.contains(item.id)) {
                                        item
                                    } else {
                                        null
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error deserializing item ${doc.id}: ${e.message}", e)
                                    null
                                }
                            }
                            trySend(requestedItems)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error loading items: ${e.message}", e)
                            close(e)
                        }
                }
            
            awaitClose { 
                Log.d(TAG, "Closing requested items flow listener")
                requestListener.remove() 
            }
            return@callbackFlow
        }
        
        val listener = db.collection("items")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Firestore snapshot listener error: ${e.message}", e)
                    if (e is FirebaseFirestoreException) {
                        Log.e(TAG, "Firestore error code: ${e.code}")
                    }
                    close(e)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    Log.w(TAG, "Snapshot is null")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                Log.d(TAG, "Received ${snapshot.documents.size} documents from Firestore")
                
                val allItems = snapshot.documents.mapNotNull { doc ->
                    try {
                        val item = doc.toObject(LostFoundItem::class.java)?.copy(id = doc.id)
                        if (item != null) {
                            Log.d(TAG, "Item: ${item.name}, Status: ${item.status}, IsLost: ${item.isLost}")
                        }
                        item
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deserializing item ${doc.id}: ${e.message}", e)
                        null
                    }
                }
                
                Log.d(TAG, "Successfully deserialized ${allItems.size} items")
                
                // Apply filter based on tab type
                // Requirements: 5.1, 5.2, 5.3, 6.2, 6.3, 6.4
                // All authenticated users can read items with any status (Approved, Pending, Returned, Rejected)
                val filteredItems = when (filterType) {
                    TabFilterType.LOST -> {
                        // Show all lost items regardless of status
                        allItems.filter { it.isLost }
                    }
                    TabFilterType.FOUND -> {
                        // Show found items that are not returned
                        allItems.filter { !it.isLost && !it.status.equals("Returned", ignoreCase = true) }
                    }
                    TabFilterType.RETURNED -> {
                        // Show returned items (regardless of lost/found type)
                        allItems.filter { it.status.equals("Returned", ignoreCase = true) }
                    }
                    TabFilterType.ALL -> {
                        // Show all items regardless of status or isLost
                        allItems
                    }
                    TabFilterType.REQUESTED -> {
                        // This case is handled above
                        emptyList()
                    }
                }
                
                Log.d(TAG, "Filtered to ${filteredItems.size} items for tab $filterType")
                trySend(filteredItems)
            }
        
        awaitClose { 
            Log.d(TAG, "Closing items flow listener")
            listener.remove() 
        }
    }
    
    private suspend fun handleError(e: Throwable) {
        Log.e(TAG, "Error loading items: ${e.message}", e)
        
        withContext(Dispatchers.Main) {
            hideLoading()
            
            val message = when (e) {
                is FirebaseFirestoreException -> {
                    when (e.code) {
                        FirebaseFirestoreException.Code.PERMISSION_DENIED -> 
                            "Access denied. Please check your permissions."
                        FirebaseFirestoreException.Code.UNAVAILABLE -> 
                            "Network error. Please check your connection."
                        FirebaseFirestoreException.Code.UNAUTHENTICATED -> 
                            "Authentication required. Please sign in again."
                        else -> 
                            "Error loading items: ${e.message}"
                    }
                }
                else -> "Failed to load items. Please try again."
            }
            
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.GONE
    }
    
    private fun hideLoading() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
    
    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }
    
    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
    
    /**
     * Applies search filter to displayed items
     * Requirements: 7.2, 7.3, 7.4, 7.5
     */
    override fun applySearchFilter(query: String) {
        currentSearchQuery = query
        
        // Apply search filter using SearchManager
        // Requirement: 7.2, 7.3, 7.4
        val filteredItems = if (query.isBlank()) {
            allItems
        } else {
            SearchManager.filterItems(allItems, query)
        }
        
        // Update adapter with filtered items
        adapter.submitList(filteredItems)
        
        // Update empty state
        if (filteredItems.isEmpty()) {
            // Show different message when search returns no results
            if (query.isNotBlank()) {
                emptyMessage.text = "No items match your search"
            } else {
                emptyMessage.text = when (filterType) {
                    TabFilterType.LOST -> "No lost items found"
                    TabFilterType.FOUND -> "No found items available"
                    TabFilterType.RETURNED -> "No returned items"
                    TabFilterType.REQUESTED -> "No requested items"
                    TabFilterType.ALL -> "No items found"
                }
            }
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }
    
    /**
     * Show item details dialog when an item is clicked
     */
    private fun showItemDetailsDialog(item: LostFoundItem) {
        val dialog = ItemDetailsDialog.newInstance(item)
        dialog.show(parentFragmentManager, "ItemDetailsDialog")
    }
    
    /**
     * Save search query state on configuration changes
     * Requirement: 4.3
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SEARCH_QUERY, currentSearchQuery)
    }
}
