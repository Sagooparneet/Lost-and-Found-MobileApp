package com.example.loginandregistration.admin.utils

import android.view.View
import android.widget.ProgressBar
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Manager for handling loading states across the admin module
 * Requirements: 8.3
 */
class LoadingStateManager {
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _loadingMessage = MutableLiveData<String>("")
    val loadingMessage: LiveData<String> = _loadingMessage
    
    private var loadingCount = 0
    
    /**
     * Start loading state
     */
    fun startLoading(message: String = "Loading...") {
        loadingCount++
        _isLoading.value = true
        _loadingMessage.value = message
    }
    
    /**
     * Stop loading state
     */
    fun stopLoading() {
        loadingCount = maxOf(0, loadingCount - 1)
        if (loadingCount == 0) {
            _isLoading.value = false
            _loadingMessage.value = ""
        }
    }
    
    /**
     * Force stop all loading states
     */
    fun forceStopLoading() {
        loadingCount = 0
        _isLoading.value = false
        _loadingMessage.value = ""
    }
    
    /**
     * Check if currently loading
     */
    fun isCurrentlyLoading(): Boolean {
        return _isLoading.value == true
    }
    
    /**
     * Execute an operation with loading state
     */
    suspend fun <T> withLoading(
        message: String = "Loading...",
        operation: suspend () -> T
    ): T {
        startLoading(message)
        return try {
            operation()
        } finally {
            stopLoading()
        }
    }
}

/**
 * Sealed class representing different loading states
 */
sealed class LoadingState {
    object Idle : LoadingState()
    data class Loading(val message: String = "Loading...") : LoadingState()
    data class Success<T>(val data: T) : LoadingState()
    data class Error(val message: String, val throwable: Throwable? = null) : LoadingState()
    
    fun isLoading(): Boolean = this is Loading
    fun isSuccess(): Boolean = this is Success<*>
    fun isError(): Boolean = this is Error
    fun isIdle(): Boolean = this is Idle
}

/**
 * Helper class for managing UI loading indicators
 */
object LoadingIndicatorHelper {
    
    /**
     * Show a progress bar
     */
    fun showProgressBar(progressBar: ProgressBar) {
        progressBar.visibility = View.VISIBLE
    }
    
    /**
     * Hide a progress bar
     */
    fun hideProgressBar(progressBar: ProgressBar) {
        progressBar.visibility = View.GONE
    }
    
    /**
     * Show loading overlay
     */
    fun showLoadingOverlay(overlay: View) {
        overlay.visibility = View.VISIBLE
    }
    
    /**
     * Hide loading overlay
     */
    fun hideLoadingOverlay(overlay: View) {
        overlay.visibility = View.GONE
    }
    
    /**
     * Set view enabled state based on loading
     */
    fun setViewsEnabled(enabled: Boolean, vararg views: View) {
        views.forEach { it.isEnabled = enabled }
    }
}

/**
 * Extension functions for View to handle loading states
 */
fun View.showLoading() {
    visibility = View.VISIBLE
}

fun View.hideLoading() {
    visibility = View.GONE
}

fun View.setLoading(isLoading: Boolean) {
    visibility = if (isLoading) View.VISIBLE else View.GONE
}

/**
 * Extension function for ProgressBar
 */
fun ProgressBar.setLoadingState(isLoading: Boolean) {
    visibility = if (isLoading) View.VISIBLE else View.GONE
}
