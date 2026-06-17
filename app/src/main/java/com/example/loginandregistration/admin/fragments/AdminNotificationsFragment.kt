package com.example.loginandregistration.admin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.adapters.NotificationHistoryAdapter
import com.example.loginandregistration.admin.dialogs.NotificationComposerDialog
import com.example.loginandregistration.admin.dialogs.NotificationDetailsDialog
import com.example.loginandregistration.admin.models.PushNotification
import com.example.loginandregistration.admin.viewmodel.AdminDashboardViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import java.util.Locale

/**
 * Fragment for managing push notifications
 * Requirements: 6.7, 6.9, 8.5
 * Task: 13.1
 */
class AdminNotificationsFragment : Fragment() {
    
    private val viewModel: AdminDashboardViewModel by activityViewModels()
    private lateinit var notificationAdapter: NotificationHistoryAdapter
    
    // Views
    private lateinit var tabLayout: TabLayout
    private lateinit var rvNotificationHistory: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var fabCompose: FloatingActionButton
    private lateinit var statsContainer: View
    private lateinit var swipeRefreshStats: SwipeRefreshLayout
    private lateinit var rvNotificationHistoryStats: RecyclerView
    
    // Statistics views
    private lateinit var tvTotalSent: TextView
    private lateinit var tvDelivered: TextView
    private lateinit var tvOpened: TextView
    private lateinit var tvFailed: TextView
    private lateinit var tvOpenRate: TextView
    
    // Current tab
    private var currentTab = 0 // 0 = Send, 1 = History
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_notifications, container, false)
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
        viewModel.loadNotificationHistory()
    }
    
    private fun initViews(view: View) {
        tabLayout = view.findViewById(R.id.tabLayout)
        rvNotificationHistory = view.findViewById(R.id.rvNotificationHistory)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        fabCompose = view.findViewById(R.id.fabCompose)
        statsContainer = view.findViewById(R.id.statsContainer)
        swipeRefreshStats = view.findViewById(R.id.swipeRefreshStats)
        rvNotificationHistoryStats = view.findViewById(R.id.rvNotificationHistoryStats)
        
        // Statistics views
        tvTotalSent = view.findViewById(R.id.tvTotalSent)
        tvDelivered = view.findViewById(R.id.tvDelivered)
        tvOpened = view.findViewById(R.id.tvOpened)
        tvFailed = view.findViewById(R.id.tvFailed)
        tvOpenRate = view.findViewById(R.id.tvOpenRate)
    }
    
    /**
     * Setup tab navigation for Send and History
     * Requirements: 6.7
     */
    private fun setupTabs() {
        // Add tabs
        tabLayout.addTab(tabLayout.newTab().setText("Send"))
        tabLayout.addTab(tabLayout.newTab().setText("History"))
        
        // Handle tab selection
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        // Send tab - show compose button, hide history
                        currentTab = 0
                        rvNotificationHistory.visibility = View.GONE
                        swipeRefresh.visibility = View.GONE
                        statsContainer.visibility = View.GONE
                        fabCompose.show()
                    }
                    1 -> {
                        // History tab - show history list and stats
                        currentTab = 1
                        rvNotificationHistory.visibility = View.GONE
                        swipeRefresh.visibility = View.GONE
                        statsContainer.visibility = View.VISIBLE
                        fabCompose.hide()
                        viewModel.loadNotificationHistory()
                    }
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        
        // Select first tab by default
        tabLayout.selectTab(tabLayout.getTabAt(0))
    }
    
    /**
     * Setup RecyclerView with adapter
     * Requirements: 6.9
     */
    private fun setupRecyclerView() {
        notificationAdapter = NotificationHistoryAdapter { notification ->
            showNotificationDetails(notification)
        }
        
        // Setup main history RecyclerView
        rvNotificationHistory.apply {
            adapter = notificationAdapter
            layoutManager = LinearLayoutManager(context)
        }
        
        // Setup stats history RecyclerView
        rvNotificationHistoryStats.apply {
            adapter = notificationAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    /**
     * Setup Floating Action Button for composing notifications
     * Requirements: 6.7
     */
    private fun setupFAB() {
        fabCompose.setOnClickListener {
            showNotificationComposer()
        }
    }
    
    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            viewModel.loadNotificationHistory()
        }
        
        swipeRefreshStats.setOnRefreshListener {
            viewModel.loadNotificationHistory()
        }
    }
    
    /**
     * Observe ViewModel data
     * Requirements: 6.9, 8.5
     */
    private fun observeViewModel() {
        // Observe notification history
        viewModel.notificationHistory.observe(viewLifecycleOwner) { notifications ->
            notificationAdapter.submitList(notifications)
            swipeRefresh.isRefreshing = false
            swipeRefreshStats.isRefreshing = false
        }
        
        // Observe notification statistics
        viewModel.notificationStats.observe(viewLifecycleOwner) { stats ->
            updateStatistics(stats)
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefresh.isRefreshing = isLoading
            swipeRefreshStats.isRefreshing = isLoading
        }
        
        // Error display disabled - uncomment to show errors
        // viewModel.error.observe(viewLifecycleOwner) { error ->
        //     error?.let {
        //         Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG).show()
        //     }
        // }
        
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(requireView(), it, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Update delivery statistics display
     * Requirements: 6.9
     */
    private fun updateStatistics(stats: com.example.loginandregistration.admin.models.NotificationStats) {
        tvTotalSent.text = stats.totalSent.toString()
        tvDelivered.text = stats.delivered.toString()
        tvOpened.text = stats.opened.toString()
        tvFailed.text = stats.failed.toString()
        tvOpenRate.text = String.format(Locale.getDefault(), "%.1f%%", stats.getOpenRate())
    }
    
    /**
     * Show notification composer dialog
     * Requirements: 6.7
     */
    private fun showNotificationComposer() {
        val dialog = NotificationComposerDialog.newInstance { notification ->
            viewModel.sendNotification(notification)
        }
        dialog.show(parentFragmentManager, "NotificationComposerDialog")
    }
    
    /**
     * Show notification details dialog
     * Requirements: 6.9
     */
    private fun showNotificationDetails(notification: PushNotification) {
        val dialog = NotificationDetailsDialog.newInstance(notification)
        dialog.show(parentFragmentManager, "NotificationDetailsDialog")
    }
    
    companion object {
        fun newInstance() = AdminNotificationsFragment()
    }
}
