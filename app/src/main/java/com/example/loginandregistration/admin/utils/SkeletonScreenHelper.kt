package com.example.loginandregistration.admin.utils

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Helper for implementing skeleton screens during data loading
 * Requirements: 8.3
 */
object SkeletonScreenHelper {
    
    /**
     * Show skeleton screen for a RecyclerView
     */
    fun showSkeleton(
        recyclerView: RecyclerView,
        skeletonLayout: View?,
        itemCount: Int = 5
    ) {
        recyclerView.visibility = View.GONE
        skeletonLayout?.visibility = View.VISIBLE
    }
    
    /**
     * Hide skeleton screen and show actual content
     */
    fun hideSkeleton(
        recyclerView: RecyclerView,
        skeletonLayout: View?
    ) {
        skeletonLayout?.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
    
    /**
     * Show empty state
     */
    fun showEmptyState(
        recyclerView: RecyclerView,
        emptyStateView: View?
    ) {
        recyclerView.visibility = View.GONE
        emptyStateView?.visibility = View.VISIBLE
    }
    
    /**
     * Hide empty state
     */
    fun hideEmptyState(
        emptyStateView: View?
    ) {
        emptyStateView?.visibility = View.GONE
    }
    
    /**
     * Manage list states (loading, empty, content)
     */
    fun manageListState(
        isLoading: Boolean,
        isEmpty: Boolean,
        recyclerView: RecyclerView,
        skeletonLayout: View?,
        emptyStateView: View?
    ) {
        when {
            isLoading -> {
                showSkeleton(recyclerView, skeletonLayout)
                hideEmptyState(emptyStateView)
            }
            isEmpty -> {
                hideSkeleton(recyclerView, skeletonLayout)
                showEmptyState(recyclerView, emptyStateView)
            }
            else -> {
                hideSkeleton(recyclerView, skeletonLayout)
                hideEmptyState(emptyStateView)
            }
        }
    }
}

/**
 * Extension functions for RecyclerView
 */
fun RecyclerView.showWithSkeleton(skeletonLayout: View?) {
    SkeletonScreenHelper.hideSkeleton(this, skeletonLayout)
}

fun RecyclerView.hideWithSkeleton(skeletonLayout: View?) {
    SkeletonScreenHelper.showSkeleton(this, skeletonLayout)
}

/**
 * Data class to hold list state
 */
data class ListState(
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val error: String? = null
) {
    companion object {
        fun loading() = ListState(isLoading = true)
        fun empty() = ListState(isEmpty = true)
        fun content() = ListState()
        fun error(message: String) = ListState(error = message)
    }
}
