package com.example.loginandregistration

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Browse fragment with tabbed interface for different item categories
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5
 */
class BrowseFragment : Fragment() {
    
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var searchView: SearchView
    private lateinit var pagerAdapter: BrowseViewPagerAdapter
    
    private var currentSearchQuery: String = ""
    
    companion object {
        private const val TAG = "BrowseFragment"
        private const val STATE_SEARCH_QUERY = "search_query"
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_browse, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)
        searchView = view.findViewById(R.id.search_view)
        
        // Restore search query on configuration changes
        // Requirement: 4.3
        savedInstanceState?.let {
            currentSearchQuery = it.getString(STATE_SEARCH_QUERY, "")
        }
        
        setupViewPager()
        setupSearchView()
        
        // Restore search query in SearchView if it was saved
        if (currentSearchQuery.isNotEmpty()) {
            searchView.setQuery(currentSearchQuery, false)
        }
    }
    
    private fun setupViewPager() {
        // Create adapter with four tabs
        // Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
        pagerAdapter = BrowseViewPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        
        // Optimize ViewPager2 for better performance
        viewPager.offscreenPageLimit = 1 // Preload adjacent pages only
        viewPager.isUserInputEnabled = true // Enable swipe gestures
        
        // Connect TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = pagerAdapter.getTabTitle(position)
        }.attach()
        
        // Listen for tab changes to apply search query to new tab
        // Requirement: 7.5
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Apply current search query to newly selected tab
                applySearchToCurrentTab(currentSearchQuery)
            }
        })
    }
    
    private fun setupSearchView() {
        // Implement search filtering with real-time updates
        // Requirements: 7.2, 7.3, 7.4, 7.5
        
        // Set up the SearchView to be always expanded
        searchView.isIconified = false
        searchView.clearFocus() // Don't auto-focus on creation
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Apply search when user submits
                val searchQuery = query ?: ""
                currentSearchQuery = searchQuery
                applySearchToCurrentTab(searchQuery)
                searchView.clearFocus() // Hide keyboard after submit
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                // Apply search in real-time as user types
                // Requirement: 7.2
                val searchQuery = newText ?: ""
                currentSearchQuery = searchQuery
                applySearchToCurrentTab(searchQuery)
                return true
            }
        })
        
        // Clear search when close button is clicked
        searchView.setOnCloseListener {
            currentSearchQuery = ""
            applySearchToCurrentTab("")
            false
        }
    }
    
    /**
     * Applies the search query to the currently visible tab fragment
     * Requirements: 7.3, 7.5
     */
    private fun applySearchToCurrentTab(query: String) {
        val currentPosition = viewPager.currentItem
        
        // Try multiple methods to find the fragment
        // Method 1: ViewPager2 uses a specific tag format: f{containerId}{position}
        var fragment = childFragmentManager.findFragmentByTag("f${viewPager.id}${currentPosition}")
        
        // Method 2: Try to get fragment directly from adapter
        if (fragment == null) {
            try {
                fragment = pagerAdapter.getFragmentAt(currentPosition)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting fragment from adapter: ${e.message}")
            }
        }
        
        // Method 3: Search through all fragments
        if (fragment == null) {
            val fragments = childFragmentManager.fragments
            fragment = fragments.getOrNull(currentPosition)
        }
        
        if (fragment is SearchableFragment) {
            Log.d(TAG, "Applying search '$query' to fragment at position $currentPosition")
            fragment.applySearchFilter(query)
        } else {
            Log.w(TAG, "Fragment at position $currentPosition is not SearchableFragment or is null")
        }
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