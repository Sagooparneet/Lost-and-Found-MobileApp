package com.example.loginandregistration.admin.fragments

import android.content.Intent
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
import com.google.android.material.card.MaterialCardView
import android.widget.TextView

class AdminDashboardFragment : Fragment() {
    
    private val viewModel: AdminDashboardViewModel by activityViewModels()
    private lateinit var activityAdapter: ActivityAdapter
    
    // Views
    private lateinit var tvTotalItems: TextView
    private lateinit var tvLostItems: TextView
    private lateinit var tvFoundItems: TextView
    private lateinit var tvReceivedItems: TextView
    private lateinit var rvRecentActivity: RecyclerView
    
    // User Analytics Views
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvActiveUsers: TextView
    private lateinit var tvBlockedUsers: TextView
    private lateinit var tvAdminCount: TextView
    private lateinit var tvSecurityCount: TextView
    private lateinit var tvStudentCount: TextView
    private lateinit var rvTopContributors: RecyclerView
    
    private lateinit var topContributorsAdapter: com.example.loginandregistration.admin.adapters.TopContributorsAdapter
    
    // Cards
    private lateinit var cardTotalItems: MaterialCardView
    private lateinit var cardLostItems: MaterialCardView
    private lateinit var cardFoundItems: MaterialCardView
    private lateinit var cardReceivedItems: MaterialCardView
    private lateinit var cardReviewReports: MaterialCardView
    private lateinit var cardPendingItems: MaterialCardView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Load user analytics
        viewModel.loadUserAnalytics()
    }
    
    private fun initViews(view: View) {
        tvTotalItems = view.findViewById(R.id.tvTotalItems)
        tvLostItems = view.findViewById(R.id.tvLostItems)
        tvFoundItems = view.findViewById(R.id.tvFoundItems)
        tvReceivedItems = view.findViewById(R.id.tvReceivedItems)
        rvRecentActivity = view.findViewById(R.id.rvRecentActivity)
        
        cardTotalItems = view.findViewById(R.id.cardTotalItems)
        cardLostItems = view.findViewById(R.id.cardLostItems)
        cardFoundItems = view.findViewById(R.id.cardFoundItems)
        cardReceivedItems = view.findViewById(R.id.cardReceivedItems)
        cardReviewReports = view.findViewById(R.id.cardReviewReports)
        cardPendingItems = view.findViewById(R.id.cardPendingItems)
        
        // User Analytics Views
        tvTotalUsers = view.findViewById(R.id.tvTotalUsers)
        tvActiveUsers = view.findViewById(R.id.tvActiveUsers)
        tvBlockedUsers = view.findViewById(R.id.tvBlockedUsers)
        tvAdminCount = view.findViewById(R.id.tvAdminCount)
        tvSecurityCount = view.findViewById(R.id.tvSecurityCount)
        tvStudentCount = view.findViewById(R.id.tvStudentCount)
        rvTopContributors = view.findViewById(R.id.rvTopContributors)
    }
    
    private fun setupRecyclerView() {
        activityAdapter = ActivityAdapter()
        rvRecentActivity.apply {
            adapter = activityAdapter
            layoutManager = LinearLayoutManager(context)
            isNestedScrollingEnabled = false
        }
        
        // Setup top contributors RecyclerView
        topContributorsAdapter = com.example.loginandregistration.admin.adapters.TopContributorsAdapter()
        rvTopContributors.apply {
            adapter = topContributorsAdapter
            layoutManager = LinearLayoutManager(context)
            isNestedScrollingEnabled = false
        }
    }
    
    private fun setupClickListeners() {
        cardTotalItems.setOnClickListener {
            navigateToItemsFragment("all")
        }
        
        cardLostItems.setOnClickListener {
            navigateToItemsFragment("lost")
        }
        
        cardFoundItems.setOnClickListener {
            navigateToItemsFragment("found")
        }
        
        cardReceivedItems.setOnClickListener {
            navigateToItemsFragment("received")
        }
        
        cardReviewReports.setOnClickListener {
            navigateToItemsFragment("all")
        }
        
        cardPendingItems.setOnClickListener {
            navigateToItemsFragment("pending")
        }
    }
    
    private fun navigateToItemsFragment(filter: String) {
        // Use Navigation Component to navigate to items fragment
        try {
            val navController = androidx.navigation.Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_admin)
            val bundle = Bundle().apply {
                putString("filter", filter)
            }
            navController.navigate(R.id.navigation_items, bundle)
        } catch (e: Exception) {
            // Fallback: just switch to items tab
            val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.nav_view)
            bottomNav.selectedItemId = R.id.navigation_items
        }
    }
    
    private fun observeViewModel() {
        viewModel.dashboardStats.observe(viewLifecycleOwner) { stats ->
            updateStatsCards(stats)
        }
        
        viewModel.recentActivities.observe(viewLifecycleOwner) { activities ->
            activityAdapter.submitList(activities.take(5)) // Show only 5 recent activities
        }
        
        // Observe user analytics
        viewModel.userAnalytics.observe(viewLifecycleOwner) { analytics ->
            updateUserAnalytics(analytics)
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Show error message
                // You can implement a Snackbar or Toast here
            }
        }
    }
    
    private fun updateStatsCards(stats: com.example.loginandregistration.admin.models.DashboardStats) {
        tvTotalItems.text = stats.totalItems.toString()
        tvLostItems.text = stats.lostItems.toString()
        tvFoundItems.text = stats.foundItems.toString()
        tvReceivedItems.text = stats.receivedItems.toString()
    }
    
    /**
     * Update user analytics display
     * Requirements: 1.7
     */
    private fun updateUserAnalytics(analytics: com.example.loginandregistration.admin.models.UserAnalytics) {
        // Update user counts
        tvTotalUsers.text = analytics.totalUsers.toString()
        tvActiveUsers.text = analytics.activeUsers.toString()
        tvBlockedUsers.text = analytics.blockedUsers.toString()
        
        // Update users by role - Requirement 10.4
        tvAdminCount.text = (analytics.usersByRole[com.example.loginandregistration.admin.models.UserRole.ADMIN] ?: 0).toString()
        tvSecurityCount.text = (analytics.usersByRole[com.example.loginandregistration.admin.models.UserRole.SECURITY] ?: 0).toString()
        tvStudentCount.text = (analytics.usersByRole[com.example.loginandregistration.admin.models.UserRole.STUDENT] ?: 0).toString()
        
        // Update top contributors (show top 5)
        topContributorsAdapter.submitList(analytics.topContributors.take(5))
    }
}