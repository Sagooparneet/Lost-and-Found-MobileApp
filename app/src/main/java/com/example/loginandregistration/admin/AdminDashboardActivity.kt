package com.example.loginandregistration.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.loginandregistration.Login
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.repository.AdminRepository
import com.example.loginandregistration.admin.utils.NotificationPermissionHelper
import com.example.loginandregistration.admin.viewmodel.AdminDashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking

class AdminDashboardActivity : AppCompatActivity() {
    
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var viewModel: AdminDashboardViewModel
    private val adminRepository = AdminRepository()
    
    companion object {
        private const val TAG = "AdminDashboardActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate()
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Set theme back to regular theme (for Android 11 and below)
        setTheme(R.style.Theme_LoginAndRegistration)
        
        // Set condition to keep splash screen visible (false = dismiss immediately after app is ready)
        splashScreen.setKeepOnScreenCondition { false }
        
        Log.d(TAG, "AdminDashboardActivity onCreate started")
        
        // Check admin access in background to avoid blocking main thread
        lifecycleScope.launch(Dispatchers.IO) {
            val isAdmin = adminRepository.isAdminUser()
            
            withContext(Dispatchers.Main) {
                if (!isAdmin) {
                    Log.w(TAG, "Access denied - not admin user")
                    Toast.makeText(this@AdminDashboardActivity, "Access denied. Admin privileges required.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@AdminDashboardActivity, Login::class.java))
                    finish()
                    return@withContext
                }
                
                Log.d(TAG, "Admin access confirmed, setting up dashboard")
                setupDashboard()
            }
        }
    }
    
    private fun setupDashboard() {
        setContentView(R.layout.activity_admin_dashboard)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[AdminDashboardViewModel::class.java]
        
        // Test Firebase connection and initialize admin user
        testFirebaseConnection()
        
        try {
            // Setup toolbar as ActionBar
            val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
            setSupportActionBar(toolbar)
            
            // Setup navigation
            val navView: BottomNavigationView = findViewById(R.id.nav_view)
            val navController = findNavController(R.id.nav_host_fragment_activity_admin)
            
            // Enable ActionBar and configure up navigation support
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            
            // Configure AppBarConfiguration with all top-level destinations
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.navigation_dashboard,
                    R.id.navigation_items,
                    R.id.navigation_users,
                    R.id.navigation_donations,
                    R.id.navigation_profile
                )
            )
            
            // Setup ActionBar with NavController for proper back navigation
            setupActionBarWithNavController(navController, appBarConfiguration)
            navView.setupWithNavController(navController)
            
            // Add navigation listener to handle errors
            navController.addOnDestinationChangedListener { _, destination, _ ->
                Log.d(TAG, "Navigated to: ${destination.label}")
            }
            
            // Request notification permission for admin
            NotificationPermissionHelper.checkAndRequestPermission(this, showRationale = true)
            
            Log.d(TAG, "Navigation setup completed successfully")
            
            // Check and run user schema migration if needed
            checkAndRunMigration()
            
            // Handle notification deep linking
            handleNotificationIntent(intent)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Navigation state error during setup", e)
            Toast.makeText(this, "Navigation error. Please restart the app.", Toast.LENGTH_LONG).show()
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up navigation", e)
            Toast.makeText(this, "Error setting up dashboard: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        val granted = NotificationPermissionHelper.handlePermissionResult(
            requestCode,
            permissions,
            grantResults
        )
        
        if (!granted && requestCode == NotificationPermissionHelper.NOTIFICATION_PERMISSION_REQUEST_CODE) {
            // Permission denied - show dialog if permanently denied
            if (!NotificationPermissionHelper.shouldShowPermissionRationale(this)) {
                NotificationPermissionHelper.showPermissionDeniedDialog(this)
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, Login::class.java))
                finish()
                true
            }
            R.id.action_refresh -> {
                viewModel.refreshData()
                Toast.makeText(this, "Data refreshed", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_export -> {
                navigateToExport()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    /**
     * Navigate to export fragment
     * Requirements: 8.1, 14.1
     */
    private fun navigateToExport() {
        try {
            val navController = findNavController(R.id.nav_host_fragment_activity_admin)
            if (navController.currentDestination?.id != R.id.navigation_export) {
                navController.navigate(R.id.navigation_export)
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Navigation error in navigateToExport", e)
            Toast.makeText(this, "Unable to navigate to export", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid navigation destination", e)
            Toast.makeText(this, "Export feature not available", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to export", e)
            Toast.makeText(this, "Error opening export: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Handles deep linking from notifications
     * Requirements: 6.6
     */
    private fun handleNotificationIntent(intent: Intent) {
        val notificationType = intent.getStringExtra("notification_type")
        val actionUrl = intent.getStringExtra("action_url")
        val notificationId = intent.getStringExtra("notification_id")
        
        Log.d(TAG, "Handling notification intent: type=$notificationType, url=$actionUrl")
        
        if (notificationType != null && actionUrl != null) {
            // Mark notification as opened
            if (notificationId != null) {
                markNotificationAsOpened(notificationId)
            }
            
            // Navigate based on action URL
            when {
                actionUrl.startsWith("item/") -> {
                    val itemId = actionUrl.removePrefix("item/")
                    navigateToItemDetails(itemId)
                }
                actionUrl.startsWith("user/") -> {
                    val userId = actionUrl.removePrefix("user/")
                    navigateToUserDetails(userId)
                }
                actionUrl.startsWith("donation/") -> {
                    val donationId = actionUrl.removePrefix("donation/")
                    navigateToDonationDetails(donationId)
                }
                actionUrl == "donations" -> {
                    navigateToDonations()
                }
                actionUrl == "activity_log" -> {
                    navigateToActivityLog()
                }
                // Notifications feature removed
                // actionUrl == "notifications" -> {
                //     navigateToNotifications()
                // }
                actionUrl == "dashboard" -> {
                    navigateToDashboard()
                }
                else -> {
                    Log.w(TAG, "Unknown action URL: $actionUrl")
                }
            }
        }
        
        // Handle deep link URIs (lostfound://admin/...)
        intent.data?.let { uri ->
            Log.d(TAG, "Handling deep link URI: $uri")
            when (uri.host) {
                "admin" -> {
                    val path = uri.path?.removePrefix("/") ?: ""
                    when {
                        path.startsWith("item/") -> {
                            val itemId = path.removePrefix("item/")
                            navigateToItemDetails(itemId)
                        }
                        path.startsWith("user/") -> {
                            val userId = path.removePrefix("user/")
                            navigateToUserDetails(userId)
                        }
                        path == "donations" -> navigateToDonations()
                        path == "activity_log" -> navigateToActivityLog()
                        // Notifications feature removed
                        // path == "notifications" -> navigateToNotifications()
                        else -> navigateToDashboard()
                    }
                }
            }
        }
    }
    
    /**
     * Navigate to item details from notification
     * Requirements: 6.6, 1.1
     */
    private fun navigateToItemDetails(itemId: String) {
        try {
            val navController = findNavController(R.id.nav_host_fragment_activity_admin)
            if (navController.currentDestination?.id != R.id.navigation_items) {
                navController.navigate(R.id.navigation_items)
            }
            // TODO: Pass itemId to fragment to show specific item details
            Log.d(TAG, "Navigated to items for item: $itemId")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Navigation error in navigateToItemDetails", e)
            Toast.makeText(this, "Unable to navigate to item details", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid navigation destination", e)
            Toast.makeText(this, "Navigation destination not found", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to item details", e)
            Toast.makeText(this, "Navigation error occurred", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Navigate to user details from notification
     * Requirements: 6.6, 1.1
     */
    private fun navigateToUserDetails(userId: String) {
        try {
            val navController = findNavController(R.id.nav_host_fragment_activity_admin)
            if (navController.currentDestination?.id != R.id.navigation_users) {
                navController.navigate(R.id.navigation_users)
            }
            // TODO: Pass userId to fragment to show specific user details
            Log.d(TAG, "Navigated to users for user: $userId")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Navigation error in navigateToUserDetails", e)
            Toast.makeText(this, "Unable to navigate to user details", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid navigation destination", e)
            Toast.makeText(this, "Navigation destination not found", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to user details", e)
            Toast.makeText(this, "Navigation error occurred", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Navigate to donation details from notification
     * Requirements: 6.6, 1.1
     */
    private fun navigateToDonationDetails(donationId: String) {
        try {
            val navController = findNavController(R.id.nav_host_fragment_activity_admin)
            if (navController.currentDestination?.id != R.id.navigation_donations) {
                navController.navigate(R.id.navigation_donations)
            }
            // TODO: Pass donationId to fragment to show specific donation details
            Log.d(TAG, "Navigated to donations for donation: $donationId")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Navigation error in navigateToDonationDetails", e)
            Toast.makeText(this, "Unable to navigate to donation details", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid navigation destination", e)
            Toast.makeText(this, "Navigation destination not found", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to donation details", e)
            Toast.makeText(this, "Navigation error occurred", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Navigate to donations tab
     * Requirements: 6.6, 1.1
     */
    private fun navigateToDonations() {
        try {
            val navController = findNavController(R.id.nav_host_fragment_activity_admin)
            if (navController.currentDestination?.id != R.id.navigation_donations) {
                navController.navigate(R.id.navigation_donations)
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Navigation error in navigateToDonations", e)
            Toast.makeText(this, "Unable to navigate to donations", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid navigation destination", e)
            Toast.makeText(this, "Navigation destination not found", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to donations", e)
            Toast.makeText(this, "Navigation error occurred", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Navigate to activity log tab
     * Requirements: 6.6, 1.1
     */
    private fun navigateToActivityLog() {
        try {
            val navController = findNavController(R.id.nav_host_fragment_activity_admin)
            if (navController.currentDestination?.id != R.id.navigation_activity_log) {
                navController.navigate(R.id.navigation_activity_log)
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Navigation error in navigateToActivityLog", e)
            Toast.makeText(this, "Unable to navigate to activity log", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid navigation destination", e)
            Toast.makeText(this, "Navigation destination not found", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to activity log", e)
            Toast.makeText(this, "Navigation error occurred", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Navigate to notifications tab
     * Requirements: 6.6
     * REMOVED: Notifications feature has been removed from admin dashboard
     */
    // private fun navigateToNotifications() {
    //     try {
    //         val navController = findNavController(R.id.nav_host_fragment_activity_admin)
    //         navController.navigate(R.id.navigation_notifications)
    //     } catch (e: Exception) {
    //         Log.e(TAG, "Error navigating to notifications", e)
    //     }
    // }
    
    /**
     * Navigate to dashboard tab
     * Requirements: 6.6, 1.1
     */
    private fun navigateToDashboard() {
        try {
            val navController = findNavController(R.id.nav_host_fragment_activity_admin)
            if (navController.currentDestination?.id != R.id.navigation_dashboard) {
                navController.navigate(R.id.navigation_dashboard)
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Navigation error in navigateToDashboard", e)
            Toast.makeText(this, "Unable to navigate to dashboard", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid navigation destination", e)
            Toast.makeText(this, "Navigation destination not found", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to dashboard", e)
            Toast.makeText(this, "Navigation error occurred", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Mark notification as opened in Firestore
     * Requirements: 6.6
     */
    private fun markNotificationAsOpened(notificationId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        // Use coroutine to update in background
        Thread {
            try {
                runBlocking {
                    adminRepository.markNotificationAsOpened(notificationId, userId)
                    Log.d(TAG, "Marked notification as opened: $notificationId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark notification as opened", e)
            }
        }.start()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return try {
            val navController = findNavController(R.id.nav_host_fragment_activity_admin)
            navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Navigation error in onSupportNavigateUp", e)
            Toast.makeText(this, "Navigation error occurred", Toast.LENGTH_SHORT).show()
            finish()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in onSupportNavigateUp", e)
            Toast.makeText(this, "Navigation error occurred", Toast.LENGTH_SHORT).show()
            finish()
            true
        }
    }
    
    private fun testFirebaseConnection() {
        Log.d(TAG, "Testing Firebase connection...")
        
        // Use lifecycle-aware coroutines instead of callbacks
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val (isConnected, message) = adminRepository.checkFirebaseConnection()
                // Switch to main thread for UI updates
                withContext(Dispatchers.Main) {
                    if (isConnected) {
                        Log.d(TAG, "Firebase connection successful: $message")
                        Toast.makeText(this@AdminDashboardActivity, "Connected to Firebase", Toast.LENGTH_SHORT).show()
                        initializeAdminUser()
                    } else {
                        Log.e(TAG, "Firebase connection failed: $message")
                        Toast.makeText(this@AdminDashboardActivity, "Firebase connection failed: $message", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error testing Firebase connection", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminDashboardActivity, "Connection test failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun initializeAdminUser() {
        // Use lifecycle-aware coroutines for background work
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                adminRepository.initializeAdminUser()
                    .onSuccess {
                        Log.d(TAG, "Admin user initialized successfully")
                    }
                    .onFailure { e ->
                        Log.e(TAG, "Failed to initialize admin user", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing admin user", e)
            }
        }
    }
    
    // Methods called by AdminProfileFragment
    fun refreshData() {
        viewModel.refreshData()
        Toast.makeText(this, "Data refreshed", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Check if user schema migration is needed and run it
     * One-time migration check on admin dashboard initialization
     * Requirements: 2.1, 2.5
     */
    private fun checkAndRunMigration() {
        try {
            val prefs = getSharedPreferences("admin_prefs", MODE_PRIVATE)
            val migrationCompleted = prefs.getBoolean("user_schema_migration_completed", false)
            
            if (!migrationCompleted) {
                Log.d(TAG, "User schema migration not completed, starting migration...")
                
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // Show progress on main thread
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@AdminDashboardActivity,
                                "Migrating user data to new schema...",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        
                        // Run migration
                        val result = adminRepository.migrateAllUsers()
                        
                        // Update UI on main thread
                        withContext(Dispatchers.Main) {
                            result.onSuccess { migratedCount ->
                                Log.d(TAG, "Migration completed successfully: $migratedCount users migrated")
                                Toast.makeText(
                                    this@AdminDashboardActivity,
                                    "User migration completed: $migratedCount users updated",
                                    Toast.LENGTH_LONG
                                ).show()
                                
                                // Store migration completion flag
                                try {
                                    prefs.edit()
                                        .putBoolean("user_schema_migration_completed", true)
                                        .apply()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to save migration completion flag", e)
                                }
                            }.onFailure { e ->
                                Log.e(TAG, "Migration failed", e)
                                val errorMessage = when (e) {
                                    is SecurityException -> "Permission denied for migration"
                                    is java.net.UnknownHostException -> "Network error during migration"
                                    else -> "Migration failed: ${e.message}"
                                }
                                Toast.makeText(
                                    this@AdminDashboardActivity,
                                    errorMessage,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Security error during migration", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@AdminDashboardActivity,
                                "Migration error: Permission denied",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during migration", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@AdminDashboardActivity,
                                "Migration error: ${e.message ?: "Unknown error"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else {
                Log.d(TAG, "User schema migration already completed, skipping")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing migration check", e)
            Toast.makeText(
                this,
                "Failed to check migration status",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}