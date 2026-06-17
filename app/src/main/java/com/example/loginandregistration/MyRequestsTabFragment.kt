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
import kotlinx.coroutines.withContext

/**
 * Fragment for displaying user's claim requests
 * Requirements: 6.5, 7.5
 */
class MyRequestsTabFragment : Fragment(), SearchableFragment {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClaimRequestAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var emptyMessage: TextView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var allRequests: List<ClaimRequest> = emptyList()
    private var currentSearchQuery: String = ""
    
    companion object {
        private const val TAG = "MyRequestsTabFragment"
        
        fun newInstance(): MyRequestsTabFragment {
            return MyRequestsTabFragment()
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
        
        adapter = ClaimRequestAdapter()
        recyclerView.adapter = adapter
        
        emptyMessage.text = "No claim requests yet"
        
        loadClaimRequests()
    }
    
    private fun loadClaimRequests() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading()
                
                getClaimRequestsFlow()
                    .flowOn(Dispatchers.IO)
                    .catch { e ->
                        hideLoading()
                        handleError(e)
                    }
                    .collect { requests ->
                        withContext(Dispatchers.Main) {
                            hideLoading()
                            
                            // Store all requests for search filtering
                            // Requirement: 7.5
                            allRequests = requests
                            
                            // Apply current search filter
                            val displayRequests = if (currentSearchQuery.isBlank()) {
                                requests
                            } else {
                                filterClaimRequests(requests, currentSearchQuery)
                            }
                            
                            adapter.submitList(displayRequests)
                            
                            if (displayRequests.isEmpty()) {
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
    
    private fun getClaimRequestsFlow() = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val userId = currentUser.uid
        
        // Log the current user ID for debugging
        Log.d(TAG, "Loading claim requests for userId: $userId")
        
        // Query only the current user's claim requests
        // Requirements: 6.5
        val listener = db.collection("claimRequests")
            .whereEqualTo("userId", userId)
            .orderBy("requestDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Firestore snapshot listener error: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(ClaimRequest::class.java)?.copy(requestId = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deserializing claim request ${doc.id}: ${e.message}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(requests)
            }
        
        awaitClose { listener.remove() }
    }
    
    private suspend fun handleError(e: Throwable) {
        Log.e(TAG, "Error loading claim requests: ${e.message}", e)
        
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
                            "Error loading requests: ${e.message}"
                    }
                }
                else -> "Failed to load claim requests. Please try again."
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
     * Filters claim requests based on search query
     * Searches across item name, reason, and status fields
     * Requirements: 7.2, 7.3, 7.4
     */
    private fun filterClaimRequests(requests: List<ClaimRequest>, query: String): List<ClaimRequest> {
        if (query.isBlank()) {
            return requests
        }
        
        val lowerQuery = query.lowercase().trim()
        
        return requests.filter { request ->
            request.itemName.lowercase().contains(lowerQuery) ||
            request.reason.lowercase().contains(lowerQuery) ||
            request.status.lowercase().contains(lowerQuery)
        }
    }
    
    /**
     * Applies search filter to displayed claim requests
     * Requirements: 7.2, 7.3, 7.4, 7.5
     */
    override fun applySearchFilter(query: String) {
        currentSearchQuery = query
        
        // Apply search filter
        val filteredRequests = if (query.isBlank()) {
            allRequests
        } else {
            filterClaimRequests(allRequests, query)
        }
        
        // Update adapter with filtered requests
        adapter.submitList(filteredRequests)
        
        // Update empty state
        if (filteredRequests.isEmpty()) {
            // Show different message when search returns no results
            if (query.isNotBlank()) {
                emptyMessage.text = "No requests match your search"
            } else {
                emptyMessage.text = "No claim requests yet"
            }
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }
}
