package com.example.loginandregistration.admin.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.adapters.AdminUsersAdapter
import com.example.loginandregistration.admin.models.AdminUser
import com.example.loginandregistration.admin.models.UserRole
import com.example.loginandregistration.admin.viewmodel.AdminDashboardViewModel
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

class AdminUsersFragment : Fragment() {
    
    private val viewModel: AdminDashboardViewModel by activityViewModels()
    private lateinit var usersAdapter: AdminUsersAdapter
    
    // Views
    private lateinit var searchView: SearchView
    private lateinit var rvUsers: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var chipGroupFilters: ChipGroup
    private lateinit var chipAllUsers: Chip
    private lateinit var chipActiveUsers: Chip
    private lateinit var chipBlockedUsers: Chip
    private lateinit var chipAdminRole: Chip
    private lateinit var chipUserRole: Chip
    private lateinit var emptyStateView: LinearLayout
    private lateinit var tvEmptyStateTitle: TextView
    private lateinit var tvEmptyStateMessage: TextView
    private lateinit var btnRetry: MaterialButton
    
    // Filter state
    private var currentSearchQuery: String = ""
    private var statusFilter: StatusFilter = StatusFilter.ALL
    private var roleFilters: MutableSet<UserRole> = mutableSetOf()
    private var allUsersList: List<AdminUser> = emptyList()
    
    companion object {
        private const val TAG = "AdminUsersFragment"
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_users, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d(TAG, "onViewCreated: Initializing AdminUsersFragment")
        
        initViews(view)
        setupRecyclerView()
        setupSearchView()
        setupFilterChips()
        setupSwipeRefresh()
        setupEmptyState()
        observeViewModel()
        
        // Explicitly load all users
        Log.d(TAG, "onViewCreated: Calling loadAllUsers()")
        viewModel.loadAllUsers()
    }
    
    private fun initViews(view: View) {
        searchView = view.findViewById(R.id.searchView)
        rvUsers = view.findViewById(R.id.rvUsers)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        chipGroupFilters = view.findViewById(R.id.chipGroupFilters)
        chipAllUsers = view.findViewById(R.id.chipAllUsers)
        chipActiveUsers = view.findViewById(R.id.chipActiveUsers)
        chipBlockedUsers = view.findViewById(R.id.chipBlockedUsers)
        chipAdminRole = view.findViewById(R.id.chipAdminRole)
        chipUserRole = view.findViewById(R.id.chipUserRole)
        emptyStateView = view.findViewById(R.id.emptyStateView)
        tvEmptyStateTitle = view.findViewById(R.id.tvEmptyStateTitle)
        tvEmptyStateMessage = view.findViewById(R.id.tvEmptyStateMessage)
        btnRetry = view.findViewById(R.id.btnRetry)
    }
    
    private fun setupRecyclerView() {
        usersAdapter = AdminUsersAdapter { user, action ->
            when (action) {
                "block" -> {
                    viewModel.updateUserBlockStatus(user.uid, !user.isBlocked)
                }
                "change_role" -> {
                    showRoleChangeDialog(user)
                }
                "view_details" -> {
                    showUserDetails(user)
                }
            }
        }
        
        rvUsers.apply {
            adapter = usersAdapter
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
    
    private fun setupFilterChips() {
        // Status filter chips
        chipAllUsers.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                statusFilter = StatusFilter.ALL
                chipActiveUsers.isChecked = false
                chipBlockedUsers.isChecked = false
                applyFilters()
            }
        }
        
        chipActiveUsers.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                statusFilter = StatusFilter.ACTIVE
                chipAllUsers.isChecked = false
                chipBlockedUsers.isChecked = false
                applyFilters()
            }
        }
        
        chipBlockedUsers.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                statusFilter = StatusFilter.BLOCKED
                chipAllUsers.isChecked = false
                chipActiveUsers.isChecked = false
                applyFilters()
            }
        }
        
        // Role filter chips
        chipAdminRole.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                roleFilters.add(UserRole.ADMIN)
            } else {
                roleFilters.remove(UserRole.ADMIN)
            }
            applyFilters()
        }
        
        chipUserRole.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                roleFilters.add(UserRole.STUDENT)
            } else {
                roleFilters.remove(UserRole.STUDENT)
            }
            applyFilters()
        }
    }
    
    private fun applyFilters() {
        Log.d(TAG, "applyFilters: Applying filters to ${allUsersList.size} users")
        var filteredList = allUsersList
        
        // Apply status filter
        filteredList = when (statusFilter) {
            StatusFilter.ALL -> filteredList
            StatusFilter.ACTIVE -> filteredList.filter { !it.isBlocked }
            StatusFilter.BLOCKED -> filteredList.filter { it.isBlocked }
        }
        
        // Apply role filters
        if (roleFilters.isNotEmpty()) {
            filteredList = filteredList.filter { user ->
                roleFilters.contains(user.role)
            }
        }
        
        // Apply search query
        if (currentSearchQuery.isNotBlank()) {
            filteredList = filteredList.filter { user ->
                user.email.contains(currentSearchQuery, ignoreCase = true) ||
                user.displayName.contains(currentSearchQuery, ignoreCase = true)
            }
        }
        
        Log.d(TAG, "applyFilters: Filtered list has ${filteredList.size} users")
        
        // Show/hide empty state based on filtered results
        if (filteredList.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
        
        usersAdapter.submitList(filteredList)
    }
    
    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            Log.d(TAG, "setupSwipeRefresh: User triggered refresh")
            viewModel.loadAllUsers()
        }
    }
    
    private fun setupEmptyState() {
        btnRetry.setOnClickListener {
            Log.d(TAG, "setupEmptyState: Retry button clicked")
            hideEmptyState()
            swipeRefresh.isRefreshing = true
            viewModel.loadAllUsers()
        }
    }
    
    private fun showEmptyState(isError: Boolean = false, errorMessage: String = "") {
        Log.d(TAG, "showEmptyState: Displaying empty state, isError = $isError")
        emptyStateView.visibility = View.VISIBLE
        rvUsers.visibility = View.GONE
        
        if (isError) {
            tvEmptyStateTitle.text = getString(R.string.error_loading_users)
            tvEmptyStateMessage.text = errorMessage.ifEmpty { "Unable to load users. Please try again." }
            btnRetry.visibility = View.VISIBLE
        } else {
            tvEmptyStateTitle.text = getString(R.string.no_items_title)
            tvEmptyStateMessage.text = "There are no users matching your criteria"
            btnRetry.visibility = View.GONE
        }
    }
    
    private fun hideEmptyState() {
        Log.d(TAG, "hideEmptyState: Hiding empty state")
        emptyStateView.visibility = View.GONE
        rvUsers.visibility = View.VISIBLE
    }
    
    private fun observeViewModel() {
        viewModel.allUsers.observe(viewLifecycleOwner) { users ->
            Log.d(TAG, "observeViewModel: Received ${users.size} users from ViewModel")
            allUsersList = users
            swipeRefresh.isRefreshing = false
            
            if (users.isEmpty()) {
                Log.d(TAG, "observeViewModel: No users found, showing empty state")
                showEmptyState(isError = false)
            } else {
                Log.d(TAG, "observeViewModel: Users loaded successfully, applying filters")
                applyFilters()
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "observeViewModel: Loading state changed to $isLoading")
            swipeRefresh.isRefreshing = isLoading
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Show error message
            }
        }
    }
    
    private fun showRoleChangeDialog(user: AdminUser) {
        val roles = UserRole.values()
        val roleNames = roles.map { it.name }.toTypedArray()
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Change User Role")
            .setItems(roleNames) { _, which ->
                val selectedRole = roles[which]
                viewModel.updateUserRole(user.uid, selectedRole)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showUserDetails(user: AdminUser) {
        // Navigate to UserDetailsFragment using Navigation component
        val bundle = Bundle().apply {
            putString("user_id", user.uid)
        }
        findNavController().navigate(R.id.userDetailsFragment, bundle)
    }
    
    enum class StatusFilter {
        ALL, ACTIVE, BLOCKED
    }
}