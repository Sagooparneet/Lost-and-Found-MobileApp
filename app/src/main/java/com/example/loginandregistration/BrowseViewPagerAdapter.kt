package com.example.loginandregistration

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.lang.ref.WeakReference

/**
 * ViewPager2 adapter for browse tabs
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 */
class BrowseViewPagerAdapter(private val parentFragment: Fragment) : FragmentStateAdapter(parentFragment) {
    
    // Keep weak references to created fragments for search functionality
    private val fragmentRefs = mutableMapOf<Int, WeakReference<Fragment>>()
    
    companion object {
        const val TAB_COUNT = 6
        const val TAB_LOST = 0
        const val TAB_FOUND = 1
        const val TAB_RETURNED = 2
        const val TAB_REQUESTED = 3
        const val TAB_MY_REQUESTS = 4
        const val TAB_ALL = 5
    }
    
    override fun getItemCount(): Int = TAB_COUNT
    
    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            TAB_LOST -> BrowseTabFragment.newInstance(BrowseTabFragment.TabFilterType.LOST)
            TAB_FOUND -> BrowseTabFragment.newInstance(BrowseTabFragment.TabFilterType.FOUND)
            TAB_RETURNED -> BrowseTabFragment.newInstance(BrowseTabFragment.TabFilterType.RETURNED)
            TAB_REQUESTED -> BrowseTabFragment.newInstance(BrowseTabFragment.TabFilterType.REQUESTED)
            TAB_MY_REQUESTS -> MyRequestsTabFragment.newInstance()
            TAB_ALL -> BrowseTabFragment.newInstance(BrowseTabFragment.TabFilterType.ALL)
            else -> BrowseTabFragment.newInstance(BrowseTabFragment.TabFilterType.LOST)
        }
        
        // Store weak reference to the fragment
        fragmentRefs[position] = WeakReference(fragment)
        
        return fragment
    }
    
    /**
     * Get fragment at a specific position if it exists
     * Returns null if fragment hasn't been created yet or has been garbage collected
     */
    fun getFragmentAt(position: Int): Fragment? {
        // First try to get from weak reference cache
        fragmentRefs[position]?.get()?.let { return it }
        
        // Try to find in parent fragment's child fragment manager
        val tag = "f${parentFragment.view?.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.view_pager)?.id}${position}"
        return parentFragment.childFragmentManager.findFragmentByTag(tag)
    }
    
    fun getTabTitle(position: Int): String {
        return when (position) {
            TAB_LOST -> "Lost Items"
            TAB_FOUND -> "Found Items"
            TAB_RETURNED -> "Returned"
            TAB_REQUESTED -> "Requested"
            TAB_MY_REQUESTS -> "My Requests"
            TAB_ALL -> "All Items"
            else -> ""
        }
    }
}
