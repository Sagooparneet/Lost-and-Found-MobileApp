package com.example.loginandregistration.admin.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.loginandregistration.R

/**
 * Helper for showing progress dialogs for long-running operations
 * Requirements: 8.3
 */
class ProgressDialogHelper(private val context: Context) {
    
    private var progressDialog: AlertDialog? = null
    private var progressBar: ProgressBar? = null
    private var messageTextView: TextView? = null
    
    /**
     * Show a progress dialog with message
     */
    fun show(
        message: String = "Please wait...",
        cancelable: Boolean = false
    ) {
        dismiss() // Dismiss any existing dialog
        
        val builder = AlertDialog.Builder(context)
        val dialogView = createProgressView(message)
        
        builder.setView(dialogView)
        builder.setCancelable(cancelable)
        
        progressDialog = builder.create()
        progressDialog?.show()
    }
    
    /**
     * Show a progress dialog with determinate progress
     */
    fun showWithProgress(
        message: String = "Please wait...",
        progress: Int = 0,
        max: Int = 100,
        cancelable: Boolean = false
    ) {
        dismiss()
        
        val builder = AlertDialog.Builder(context)
        val dialogView = createProgressView(message, determinate = true)
        
        builder.setView(dialogView)
        builder.setCancelable(cancelable)
        
        progressDialog = builder.create()
        progressDialog?.show()
        
        updateProgress(progress, max)
    }
    
    /**
     * Update the progress message
     */
    fun updateMessage(message: String) {
        messageTextView?.text = message
    }
    
    /**
     * Update the progress value (for determinate progress)
     */
    fun updateProgress(progress: Int, max: Int = 100) {
        progressBar?.apply {
            this.max = max
            this.progress = progress
        }
    }
    
    /**
     * Dismiss the progress dialog
     */
    fun dismiss() {
        progressDialog?.dismiss()
        progressDialog = null
        progressBar = null
        messageTextView = null
    }
    
    /**
     * Check if dialog is showing
     */
    fun isShowing(): Boolean {
        return progressDialog?.isShowing == true
    }
    
    private fun createProgressView(message: String, determinate: Boolean = false): View {
        // Create a simple layout programmatically since we don't have the layout XML
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
        }
        
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = !determinate
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32
            }
        }
        
        messageTextView = TextView(context).apply {
            text = message
            textSize = 16f
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        layout.addView(progressBar)
        layout.addView(messageTextView)
        
        return layout
    }
}

/**
 * Extension function for Context to create ProgressDialogHelper
 */
fun Context.createProgressDialog(): ProgressDialogHelper {
    return ProgressDialogHelper(this)
}

/**
 * Execute an operation with progress dialog
 */
suspend fun <T> Context.withProgressDialog(
    message: String = "Please wait...",
    cancelable: Boolean = false,
    operation: suspend () -> T
): T {
    val progressDialog = createProgressDialog()
    progressDialog.show(message, cancelable)
    return try {
        operation()
    } finally {
        progressDialog.dismiss()
    }
}
