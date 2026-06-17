package com.example.loginandregistration.firebase

import android.content.Context
import android.net.TrafficStats
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private const val FIREBASE_TRAFFIC_TAG = 0xF00D // Tag for Firebase network traffic
    
    fun initialize(context: Context) {
        try {
            // Initialize Firebase if not already initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Log.d(TAG, "Firebase initialized")
                
                // Enable Firestore offline persistence for better performance
                val firestore = FirebaseFirestore.getInstance()
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
                firestore.firestoreSettings = settings
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
        }
    }
    
    fun isFirebaseConnected(): Boolean {
        return try {
            FirebaseApp.getInstance() != null
        } catch (e: Exception) {
            false
        }
    }
    
    fun getCurrentUser() = FirebaseAuth.getInstance().currentUser
    
    fun getFirestore() = FirebaseFirestore.getInstance()
    
    fun getStorage() = FirebaseStorage.getInstance()
    
    /**
     * Test Firestore connection with proper socket tagging
     * This is a suspending function that should be called from a background thread
     */
    suspend fun testFirestoreConnection() {
        try {
            // Tag network traffic for monitoring
            TrafficStats.setThreadStatsTag(FIREBASE_TRAFFIC_TAG)
            
            val firestore = FirebaseFirestore.getInstance()
            Log.d(TAG, "Testing Firestore connection...")
            
            // Test with a simple operation using coroutines
            val querySnapshot = firestore.collection("items")
                .limit(1)
                .get()
                .await()
            
            Log.d(TAG, "Firestore connection test successful - found ${querySnapshot.size()} items")
            
        } catch (e: Exception) {
            Log.e(TAG, "Firestore connection test failed", e)
            // Still log success if it's just a permission error
            if (e.message?.contains("permission") == true) {
                Log.i(TAG, "Connected to Firestore (permission check needed)")
            }
        } finally {
            // Always clear the traffic tag
            TrafficStats.clearThreadStatsTag()
        }
    }
    
    /**
     * Legacy callback-based method for backward compatibility
     * Prefer using the suspending version above
     */
    fun testFirestoreConnection(callback: (Boolean, String?) -> Unit) {
        try {
            // Tag network traffic for monitoring
            TrafficStats.setThreadStatsTag(FIREBASE_TRAFFIC_TAG)
            
            val firestore = FirebaseFirestore.getInstance()
            Log.d(TAG, "Testing Firestore connection...")
            
            // Test with a simple operation
            firestore.collection("items")
                .limit(1)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    Log.d(TAG, "Firestore connection test successful - found ${querySnapshot.size()} items")
                    callback(true, "Connected to Firestore successfully")
                    TrafficStats.clearThreadStatsTag()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firestore connection test failed", e)
                    // Still consider it successful if it's just a permission error
                    if (e.message?.contains("permission") == true) {
                        callback(true, "Connected to Firestore (permission check needed)")
                    } else {
                        callback(false, "Failed to connect to Firestore: ${e.message}")
                    }
                    TrafficStats.clearThreadStatsTag()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing Firestore connection", e)
            callback(false, "Error testing connection: ${e.message}")
            TrafficStats.clearThreadStatsTag()
        }
    }
}