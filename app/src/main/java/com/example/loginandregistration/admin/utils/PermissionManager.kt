package com.example.loginandregistration.admin.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Unified permission manager for handling all runtime permissions
 * Requirements: 6.10, 4.9
 */
class PermissionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "PermissionManager"
    }
    
    /**
     * Check if notification permission is granted
     * Requirements: 6.10
     */
    fun hasNotificationPermission(): Boolean {
        return NotificationPermissionHelper.hasNotificationPermission(context)
    }
    
    /**
     * Check if storage permission is granted
     * Requirements: 4.9
     */
    fun hasStoragePermission(): Boolean {
        val helper = StoragePermissionHelper(context)
        return helper.hasStoragePermission()
    }
    
    /**
     * Request notification permission with graceful handling
     * Requirements: 6.10
     */
    fun requestNotificationPermission(
        activity: Activity,
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {}
    ) {
        if (hasNotificationPermission()) {
            Log.d(TAG, "Notification permission already granted")
            onGranted()
            return
        }
        
        Log.d(TAG, "Requesting notification permission")
        NotificationPermissionHelper.checkAndRequestPermission(activity, showRationale = true)
    }
    
    /**
     * Request storage permission with graceful handling
     * Requirements: 4.9
     */
    fun requestStoragePermission(
        activity: Activity,
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {}
    ) {
        val helper = StoragePermissionHelper(context)
        
        if (helper.hasStoragePermission()) {
            Log.d(TAG, "Storage permission already granted")
            onGranted()
            return
        }
        
        Log.d(TAG, "Requesting storage permission")
        helper.checkAndRequestPermission(
            activity,
            showRationale = true,
            onGranted = onGranted,
            onDenied = onDenied
        )
    }
    
    /**
     * Handle permission request result
     * Requirements: 6.10, 4.9
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onNotificationGranted: () -> Unit = {},
        onNotificationDenied: () -> Unit = {},
        onStorageGranted: () -> Unit = {},
        onStorageDenied: () -> Unit = {}
    ) {
        when (requestCode) {
            NotificationPermissionHelper.NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                val granted = NotificationPermissionHelper.handlePermissionResult(
                    requestCode,
                    permissions,
                    grantResults
                )
                if (granted) {
                    Log.d(TAG, "Notification permission granted")
                    onNotificationGranted()
                } else {
                    Log.w(TAG, "Notification permission denied")
                    onNotificationDenied()
                }
            }
            StoragePermissionHelper.STORAGE_PERMISSION_REQUEST_CODE -> {
                val helper = StoragePermissionHelper(context)
                val granted = helper.onPermissionResult(requestCode, permissions, grantResults)
                if (granted) {
                    Log.d(TAG, "Storage permission granted")
                    onStorageGranted()
                } else {
                    Log.w(TAG, "Storage permission denied")
                    onStorageDenied()
                }
            }
        }
    }
    
    /**
     * Check all required permissions for admin functionality
     * Requirements: 6.10, 4.9
     */
    fun checkAllPermissions(): Map<String, Boolean> {
        return mapOf(
            "notification" to hasNotificationPermission(),
            "storage" to hasStoragePermission()
        )
    }
    
    /**
     * Request all missing permissions
     * Requirements: 6.10, 4.9
     */
    fun requestAllMissingPermissions(activity: Activity) {
        val permissions = checkAllPermissions()
        
        if (!permissions["notification"]!!) {
            requestNotificationPermission(activity)
        }
        
        if (!permissions["storage"]!!) {
            requestStoragePermission(activity)
        }
    }
    
    /**
     * Show permission status summary
     * Requirements: 6.10, 4.9
     */
    fun getPermissionStatusSummary(): String {
        val permissions = checkAllPermissions()
        val builder = StringBuilder("Permission Status:\n")
        
        builder.append("• Notifications: ${if (permissions["notification"]!!) "✓ Granted" else "✗ Denied"}\n")
        builder.append("• Storage: ${if (permissions["storage"]!!) "✓ Granted" else "✗ Denied"}")
        
        return builder.toString()
    }
}
