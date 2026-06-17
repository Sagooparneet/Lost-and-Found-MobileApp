package com.example.loginandregistration.admin.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Helper class for managing notification permissions
 * Requirements: 6.10
 */
object NotificationPermissionHelper {
    
    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    
    /**
     * Checks if notification permission is granted
     * Requirements: 6.10
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Notifications are allowed by default on Android 12 and below
            true
        }
    }
    
    /**
     * Requests notification permission on Android 13+
     * Requirements: 6.10
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    
    /**
     * Checks if the user should be shown a rationale for notification permission
     */
    fun shouldShowPermissionRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            false
        }
    }
    
    /**
     * Handles the permission request result
     * Returns true if permission was granted, false otherwise
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                return true
            }
        }
        return false
    }
    
    /**
     * Checks if notifications are enabled in system settings
     * This is different from permission - user might have granted permission but disabled notifications
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) 
                as android.app.NotificationManager
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
    }
    
    /**
     * Opens app notification settings
     */
    fun openNotificationSettings(context: Context) {
        val intent = android.content.Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = android.net.Uri.parse("package:${context.packageName}")
            }
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    /**
     * Shows a dialog explaining why notification permission is needed
     */
    fun showPermissionRationaleDialog(
        activity: Activity,
        onPositive: () -> Unit,
        onNegative: () -> Unit
    ) {
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle("Notification Permission Required")
            .setMessage("This app needs notification permission to send you important updates about your lost and found items, including:\n\n" +
                    "• Item match notifications\n" +
                    "• Request approval/denial updates\n" +
                    "• Donation status updates\n" +
                    "• Important announcements\n\n" +
                    "Would you like to grant this permission?")
            .setPositiveButton("Grant Permission") { dialog, _ ->
                dialog.dismiss()
                onPositive()
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
                onNegative()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Shows a dialog when permission is denied permanently
     */
    fun showPermissionDeniedDialog(activity: Activity) {
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle("Notification Permission Denied")
            .setMessage("You have denied notification permission. You won't receive important updates about your items.\n\n" +
                    "You can enable notifications anytime from app settings.")
            .setPositiveButton("Open Settings") { dialog, _ ->
                dialog.dismiss()
                openNotificationSettings(activity)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    /**
     * Checks and requests notification permission if needed
     * Returns true if permission is already granted or request was initiated
     */
    fun checkAndRequestPermission(
        activity: Activity,
        showRationale: Boolean = true
    ): Boolean {
        if (hasNotificationPermission(activity)) {
            return true
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (showRationale && shouldShowPermissionRationale(activity)) {
                showPermissionRationaleDialog(
                    activity,
                    onPositive = { requestNotificationPermission(activity) },
                    onNegative = { /* User declined */ }
                )
            } else {
                requestNotificationPermission(activity)
            }
        }
        
        return false
    }
}
