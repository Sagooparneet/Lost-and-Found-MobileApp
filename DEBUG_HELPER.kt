// DEBUG_HELPER.kt - Temporary debugging utilities for testing
// Add these functions to your MainActivity or create a separate DebugHelper class

package com.example.loginandregistration

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class DebugHelper {
    
    companion object {
        private const val TAG = "DEBUG_HELPER"
        
        /**
         * Test Firebase connection and basic operations
         */
        suspend fun testFirebaseConnection() {
            withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "🔥 Starting Firebase connection test...")
                    
                    // Test Firestore write
                    val testData = mapOf(
                        "message" to "Debug test from Android app",
                        "timestamp" to FieldValue.serverTimestamp(),
                        "testType" to "connection_test"
                    )
                    
                    val documentReference = FirebaseFirestore.getInstance()
                        .collection("debug_tests")
                        .add(testData)
                        .await()
                    
                    Log.d(TAG, "✅ Firestore WRITE successful: ${documentReference.id}")
                    
                    // Test Firestore read
                    testFirestoreRead()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Firestore WRITE failed: ${e.message}", e)
                }
            }
        }
        
        private suspend fun testFirestoreRead() {
            try {
                val documents = FirebaseFirestore.getInstance()
                    .collection("debug_tests")
                    .limit(5)
                    .get()
                    .await()
                
                Log.d(TAG, "✅ Firestore READ successful: ${documents.size()} documents")
                for (document in documents) {
                    Log.d(TAG, "Document: ${document.id} => ${document.data}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Firestore READ failed: ${e.message}", e)
            }
        }
        
        /**
         * Test authentication state and user info
         */
        fun testAuthState() {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            
            Log.d(TAG, "🔐 Authentication State Test:")
            if (currentUser != null) {
                Log.d(TAG, "✅ User is logged in")
                Log.d(TAG, "   Email: ${currentUser.email}")
                Log.d(TAG, "   UID: ${currentUser.uid}")
                Log.d(TAG, "   Display Name: ${currentUser.displayName}")
                Log.d(TAG, "   Email Verified: ${currentUser.isEmailVerified}")
                
                // Check if user is admin
                val isAdmin = currentUser.email == "admin@gmail.com"
                Log.d(TAG, "   Is Admin: $isAdmin")
                
            } else {
                Log.d(TAG, "❌ No user is logged in")
            }
        }
        
        /**
         * Test item creation (for testing core functionality)
         */
        suspend fun testItemCreation() {
            withContext(Dispatchers.IO) {
                try {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser == null) {
                        Log.e(TAG, "❌ Cannot test item creation - no user logged in")
                        return@withContext
                    }
                    
                    Log.d(TAG, "📦 Testing item creation...")
                    
                    val testItem = mapOf(
                        "title" to "Debug Test Item",
                        "description" to "This is a test item created by debug helper",
                        "category" to "LOST",
                        "location" to "Test Location",
                        "userId" to currentUser.uid,
                        "userEmail" to currentUser.email,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "status" to "ACTIVE"
                    )
                    
                    val documentReference = FirebaseFirestore.getInstance()
                        .collection("items")
                        .add(testItem)
                        .await()
                    
                    Log.d(TAG, "✅ Test item created successfully: ${documentReference.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Test item creation failed: ${e.message}", e)
                }
            }
        }
        
        /**
         * Test reading items (for testing browse functionality)
         */
        suspend fun testItemReading() {
            withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "📖 Testing item reading...")
                    
                    val documents = FirebaseFirestore.getInstance()
                        .collection("items")
                        .limit(10)
                        .get()
                        .await()
                    
                    Log.d(TAG, "✅ Items read successfully: ${documents.size()} items found")
                    for (document in documents) {
                        val title = document.getString("title") ?: "No title"
                        val category = document.getString("category") ?: "No category"
                        Log.d(TAG, "   Item: $title ($category)")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Items reading failed: ${e.message}", e)
                }
            }
        }
        
        /**
         * Test user profile data
         */
        suspend fun testUserProfile() {
            withContext(Dispatchers.IO) {
                try {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser == null) {
                        Log.e(TAG, "❌ Cannot test user profile - no user logged in")
                        return@withContext
                    }
                    
                    Log.d(TAG, "👤 Testing user profile...")
                    
                    val document = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()
                    
                    if (document.exists()) {
                        Log.d(TAG, "✅ User profile found: ${document.data}")
                    } else {
                        Log.d(TAG, "⚠️ User profile not found in Firestore")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ User profile read failed: ${e.message}", e)
                }
            }
        }
        
        /**
         * Run all debug tests
         */
        suspend fun runAllTests() {
            Log.d(TAG, "🚀 Running all debug tests...")
            testFirebaseConnection()
            testAuthState()
            testUserProfile()
            testItemReading()
            // Note: testItemCreation() should be called manually to avoid spam
        }
        
        /**
         * Clean up debug test data
         */
        suspend fun cleanupDebugData() {
            withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "🧹 Cleaning up debug test data...")
                    
                    val documents = FirebaseFirestore.getInstance()
                        .collection("debug_tests")
                        .get()
                        .await()
                    
                    for (document in documents) {
                        document.reference.delete().await()
                    }
                    Log.d(TAG, "✅ Debug test data cleaned up: ${documents.size()} documents deleted")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Debug cleanup failed: ${e.message}", e)
                }
            }
        }
    }
}

// Usage in MainActivity or any Activity:
// Add this to onCreate() or a button click for testing:
/*
// Test Firebase connection
DebugHelper.testFirebaseConnection()

// Test authentication
DebugHelper.testAuthState()

// Run all tests
DebugHelper.runAllTests()

// Clean up when done testing
DebugHelper.cleanupDebugData()
*/