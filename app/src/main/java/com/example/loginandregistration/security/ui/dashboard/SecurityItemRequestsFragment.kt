package com.example.loginandregistration.security.ui.dashboard

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
import com.example.loginandregistration.ItemRequest
import com.example.loginandregistration.R
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

class SecurityItemRequestsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemRequestAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var emptyMessage: TextView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "SecurityItemRequests"
        
        fun newInstance(): SecurityItemRequestsFragment {
            return SecurityItemRequestsFragment()
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

        adapter = ItemRequestAdapter(
            onApproveClicked = { request -> updateRequestStatus(request, ItemRequest.RequestStatus.APPROVED) },
            onRejectClicked = { request -> updateRequestStatus(request, ItemRequest.RequestStatus.REJECTED) }
        )
        recyclerView.adapter = adapter

        emptyMessage.text = "No item requests"

        loadItemRequests()
    }

    private fun loadItemRequests() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading()

                getItemRequestsFlow()
                    .flowOn(Dispatchers.IO)
                    .catch { e ->
                        hideLoading()
                        handleError(e)
                    }
                    .collect { requests ->
                        withContext(Dispatchers.Main) {
                            hideLoading()
                            adapter.submitList(requests)

                            if (requests.isEmpty()) {
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

    private fun getItemRequestsFlow() = callbackFlow {
        val listener = db.collection("itemRequests")
            .orderBy("requestDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Firestore snapshot listener error: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(ItemRequest::class.java)?.copy(requestId = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deserializing item request ${doc.id}: ${e.message}", e)
                        null
                    }
                } ?: emptyList()

                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    private fun updateRequestStatus(request: ItemRequest, newStatus: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Authentication required", Toast.LENGTH_SHORT).show()
            return
        }

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

    private suspend fun handleError(e: Throwable) {
        Log.e(TAG, "Error loading item requests: ${e.message}", e)

        withContext(Dispatchers.Main) {
            hideLoading()
            Toast.makeText(requireContext(), "Failed to load item requests. Please try again.", Toast.LENGTH_LONG).show()
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
}
