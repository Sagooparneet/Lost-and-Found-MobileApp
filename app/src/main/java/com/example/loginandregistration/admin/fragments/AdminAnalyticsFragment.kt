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
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.DateRange
import com.example.loginandregistration.admin.viewmodel.AdminDashboardViewModel
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for displaying analytics and statistics
 * Enhanced with donation statistics
 * Requirements: 3.6, 3.7
 * Task: 11.6
 */
class AdminAnalyticsFragment : Fragment() {
    
    private val viewModel: AdminDashboardViewModel by activityViewModels()
    
    // Donation statistics views
    private lateinit var cardDonationMetrics: MaterialCardView
    private lateinit var tvTotalDonated: TextView
    private lateinit var tvPendingDonations: TextView
    private lateinit var tvReadyForDonation: TextView
    private lateinit var tvTotalDonationValue: TextView
    private lateinit var tvAverageItemAge: TextView
    private lateinit var tvDonationRate: TextView
    private lateinit var tvMostDonatedCategory: TextView
    private lateinit var rvDonationsByCategory: RecyclerView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_analytics, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        observeViewModel()
        
        // Load donation statistics with date range (last 12 months)
        val endDate = System.currentTimeMillis()
        val startDate = endDate - (365L * 24 * 60 * 60 * 1000) // 1 year ago
        viewModel.loadDonationStats(DateRange(startDate, endDate))
    }
    
    private fun initViews(view: View) {
        // Donation metrics card
        cardDonationMetrics = view.findViewById(R.id.cardDonationMetrics)
        tvTotalDonated = view.findViewById(R.id.tvTotalDonated)
        tvPendingDonations = view.findViewById(R.id.tvPendingDonations)
        tvReadyForDonation = view.findViewById(R.id.tvReadyForDonation)
        tvTotalDonationValue = view.findViewById(R.id.tvTotalDonationValue)
        tvAverageItemAge = view.findViewById(R.id.tvAverageItemAge)
        tvDonationRate = view.findViewById(R.id.tvDonationRate)
        tvMostDonatedCategory = view.findViewById(R.id.tvMostDonatedCategory)
        rvDonationsByCategory = view.findViewById(R.id.rvDonationsByCategory)
        
        // Setup RecyclerView for donations by category
        rvDonationsByCategory.layoutManager = LinearLayoutManager(context)
    }
    
    /**
     * Observe ViewModel data
     * Requirements: 3.6, 3.7
     */
    private fun observeViewModel() {
        // Observe donation statistics
        viewModel.donationStats.observe(viewLifecycleOwner) { stats ->
            updateDonationMetrics(stats)
        }
        
        // Observe analytics data (existing)
        viewModel.analyticsData.observe(viewLifecycleOwner) { analytics ->
            // TODO: Implement other analytics visualization
        }
    }
    
    /**
     * Update donation metrics display
     * Requirements: 3.6, 3.7
     */
    private fun updateDonationMetrics(stats: com.example.loginandregistration.admin.models.DonationStats) {
        // Display total donated count
        tvTotalDonated.text = stats.totalDonated.toString()
        
        // Display pending donations count
        tvPendingDonations.text = stats.pendingDonations.toString()
        
        // Display ready for donation count
        tvReadyForDonation.text = stats.readyForDonation.toString()
        
        // Display total donation value
        tvTotalDonationValue.text = "$${String.format(Locale.getDefault(), "%.2f", stats.totalValue)}"
        
        // Display average item age
        tvAverageItemAge.text = "${String.format(Locale.getDefault(), "%.1f", stats.averageItemAge)} days"
        
        // Display donation rate
        tvDonationRate.text = "${String.format(Locale.getDefault(), "%.1f", stats.getDonationRate())}%"
        
        // Display most donated category
        tvMostDonatedCategory.text = stats.mostDonatedCategory.ifEmpty { "N/A" }
        
        // Update donations by category chart/list
        updateDonationsByCategoryChart(stats.donationsByCategory)
    }
    
    /**
     * Update donations by category visualization
     * Requirements: 3.6
     */
    private fun updateDonationsByCategoryChart(donationsByCategory: Map<String, Int>) {
        // Create a simple adapter to display category breakdown
        val categoryList = donationsByCategory.entries
            .sortedByDescending { it.value }
            .map { "${it.key}: ${it.value} items" }
        
        val adapter = CategoryBreakdownAdapter(categoryList)
        rvDonationsByCategory.adapter = adapter
    }
    
    /**
     * Simple adapter for displaying category breakdown
     */
    private class CategoryBreakdownAdapter(
        private val items: List<String>
    ) : RecyclerView.Adapter<CategoryBreakdownAdapter.ViewHolder>() {
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvCategory: TextView = view.findViewById(R.id.tvCategoryItem)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category_breakdown, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvCategory.text = items[position]
        }
        
        override fun getItemCount() = items.size
    }
}