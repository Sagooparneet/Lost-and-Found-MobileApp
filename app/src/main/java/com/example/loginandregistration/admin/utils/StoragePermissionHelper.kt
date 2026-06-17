package com.example.loginandregistration.admin.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Helper class for managing storage permissions for export functionality
 * Requirements: 4.9
 */
class StoragePermissionHelper(private val context: Context) {

    companion object {
        const val STORAGE_PERMISSION_REQUEST_CODE = 1001
        
        /**
         * Gets the required storage permissions based on Android version
         * For image/video selection on Android 13+
         * Requirements: 4.2, 4.3, 4.4, 4.5
         */
        fun getStoragePermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ (API 33): Use granular media permissions
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            } else {
                // Android 12 and below: Use legacy storage permission
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        /**
         * Gets the required storage permissions based on Android version
         * For export functionality (legacy method for backward compatibility)
         */
        fun getRequiredPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ doesn't need WRITE_EXTERNAL_STORAGE for app-specific directories
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10-12
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            } else {
                // Android 9 and below
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    /**
     * Checks if storage permissions are granted
     */
    fun hasStoragePermission(): Boolean {
        // For Android 10+, we use app-specific storage which doesn't require permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        }
        
        // For older versions, check permissions
        val permissions = getRequiredPermissions()
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Requests storage permissions
     */
    fun requestStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No need to request permissions for app-specific storage
            return
        }
        
        val permissions = getRequiredPermissions()
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Checks if permission should show rationale
     */
    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return false
        }
        
        val permissions = getRequiredPermissions()
        return permissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    /**
     * Handles permission request result
     */
    fun onPermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode != STORAGE_PERMISSION_REQUEST_CODE) {
            return false
        }
        
        return grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }

    /**
     * Gets a user-friendly message explaining why storage permission is needed
     */
    fun getPermissionRationaleMessage(): String {
        return "Storage permission is required to save and share export files. " +
                "This allows you to export reports and share them with other apps."
    }

    /**
     * Gets a message for when permission is denied
     */
    fun getPermissionDeniedMessage(): String {
        return "Storage permission is required to export data. " +
                "Please grant the permission in app settings to use this feature."
    }
    
    /**
     * Shows a dialog explaining why storage permission is needed
     * Requirements: 4.9
     */
    fun showPermissionRationaleDialog(
        activity: Activity,
        onPositive: () -> Unit,
        onNegative: () -> Unit
    ) {
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle("Storage Permission Required")
            .setMessage("This app needs storage permission to save export files including:\n\n" +
                    "• PDF reports\n" +
                    "• CSV data exports\n" +
                    "• Comprehensive analytics reports\n\n" +
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
     * Requirements: 4.9
     */
    fun showPermissionDeniedDialog(activity: Activity) {
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle("Storage Permission Denied")
            .setMessage("You have denied storage permission. You won't be able to export data to files.\n\n" +
                    "You can enable storage access anytime from app settings.")
            .setPositiveButton("Open Settings") { dialog, _ ->
                dialog.dismiss()
                openAppSettings(activity)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    /**
     * Opens app settings page
     * Requirements: 4.9
     */
    fun openAppSettings(context: Context) {
        val intent = android.content.Intent().apply {
            action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = android.net.Uri.parse("package:${context.packageName}")
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    /**
     * Checks and requests storage permission if needed with graceful handling
     * Requirements: 4.9
     */
    fun checkAndRequestPermission(
        activity: Activity,
        showRationale: Boolean = true,
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {}
    ): Boolean {
        if (hasStoragePermission()) {
            onGranted()
            return true
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No permission needed for app-specific storage
            onGranted()
            return true
        }
        
        if (showRationale && shouldShowRequestPermissionRationale(activity)) {
            showPermissionRationaleDialog(
                activity,
                onPositive = { requestStoragePermission(activity) },
                onNegative = { onDenied() }
            )
        } else {
            requestStoragePermission(activity)
        }
        
        return false
    }
}
