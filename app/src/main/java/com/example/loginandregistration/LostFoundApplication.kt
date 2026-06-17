package com.example.loginandregistration

import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.util.Log
import com.example.loginandregistration.admin.utils.NotificationChannelManager
import com.example.loginandregistration.firebase.FirebaseManager
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LostFoundApplication : Application() {
    
    companion object {
        private const val TAG = "LostFoundApplication"
    }
    
    // Application-level coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            Log.d(TAG, "Application starting...")
            
            // Initialize Firebase App Check (must be done before any Firebase usage)
            initializeAppCheck()
            
            // DISABLED: StrictMode causes excessive logcat noise
            // Only enable when actively debugging specific issues
            // if (BuildConfig.DEBUG) {
            //     enableStrictMode()
            // }
            
            // Create notification channels (lightweight operation, safe on main thread)
            NotificationChannelManager.createNotificationChannels(this)
            
            // Move heavy operations to background thread
            applicationScope.launch(Dispatchers.IO) {
                try {
                    // Initialize Firebase on background thread
                    FirebaseManager.initialize(applicationContext)
                    
                    // REMOVED: Continuous connection testing causes logcat spam
                    // Only test connection when needed
                    
                    Log.d(TAG, "Application initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during background initialization", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during application initialization", e)
            // Don't crash the app, just log the error
        }
    }
    
    /**
     * Initialize Firebase App Check with Play Integrity provider
     * This protects your Firebase resources from abuse
     */
    private fun initializeAppCheck() {
        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "Firebase App Check initialized with Play Integrity provider")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase App Check", e)
        }
    }
    
    /**
     * Enable StrictMode to detect main thread violations in debug builds
     * This helps identify:
     * - Disk reads/writes on main thread
     * - Network operations on main thread
     * - Slow calls that block the main thread
     * - Custom slow code detection
     * 
     * Requirements: 3.6
     */
    private fun enableStrictMode() {
        try {
            // Thread policy - detects disk and network access on main thread
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog() // Log violations to logcat
                    .apply {
                        // On Android P (API 28) and above, detect unbuffered I/O
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            detectUnbufferedIo()
                        }
                    }
                    .build()
            )
            
            // VM policy - detects memory leaks and other VM-level issues
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll() // Detect all VM violations
                    .penaltyLog() // Log violations to logcat
                    .apply {
                        // Detect leaked closable objects (unclosed files, cursors, etc.)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            detectContentUriWithoutPermission()
                            detectUntaggedSockets()
                        }
                        // Detect leaked registration objects (broadcast receivers, etc.)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            detectUnsafeIntentLaunch()
                        }
                    }
                    .build()
            )
            
            Log.d(TAG, "StrictMode enabled for debug build - monitoring main thread violations")
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling StrictMode", e)
        }
    }
}