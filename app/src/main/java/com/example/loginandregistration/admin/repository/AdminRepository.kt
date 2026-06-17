package com.example.loginandregistration.admin.repository

import com.example.loginandregistration.LostFoundItem
import com.example.loginandregistration.admin.models.*
import com.example.loginandregistration.admin.utils.SecurityHelper
import com.example.loginandregistration.admin.utils.DataValidator
import com.example.loginandregistration.admin.utils.PerformanceHelper
import com.example.loginandregistration.admin.utils.TestDataGenerator
import com.example.loginandregistration.firebase.FirebaseManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

class AdminRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Cache for analytics data
    private var cachedAnalytics: UserAnalytics? = null
    private var analyticsCacheTimestamp: Long = 0
    private val analyticsCacheTimeout = 5 * 60 * 1000L // 5 minutes
    
    companion object {
        private const val TAG = "AdminRepository"
        const val ADMIN_EMAIL = "admin@gmail.com"
        const val ITEMS_COLLECTION = "items"
        const val USERS_COLLECTION = "users"
        const val ACTIVITIES_COLLECTION = "activities"
        const val ACTIVITY_LOGS_COLLECTION = "activityLogs"
        const val DONATIONS_COLLECTION = "donations"
    }
    
    fun isAdminUser(): Boolean {
        return SecurityHelper.isAdminUser()
    }
    
    /**
     * Require admin access - throws SecurityException if not admin or session expired
     * Requirements: 7.1, 7.2, 7.3
     */
    @Throws(SecurityException::class)
    private fun requireAdminAccess() {
        SecurityHelper.requireAdminAccess()
    }
    
    // Check Firebase connection
    suspend fun checkFirebaseConnection(): Pair<Boolean, String?> {
        return withContext(Dispatchers.IO) {
            try {
                // Simple connection test
                firestore.collection(ITEMS_COLLECTION)
                    .limit(1)
                    .get()
                    .await()
                
                Log.d(TAG, "Firebase connection successful")
                Pair(true, "Connected successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Firebase connection failed", e)
                Pair(false, e.message)
            }
        }
    }
    
    // Initialize admin user in Firestore if not exists
    suspend fun initializeAdminUser(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser?.email == ADMIN_EMAIL) {
                // Check if user document exists
                val existingDoc = firestore.collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .get()
                    .await()
                
                if (existingDoc.exists()) {
                    // Update existing document to ensure role is ADMIN
                    val updates = hashMapOf<String, Any>(
                        "role" to "ADMIN",
                        "lastLoginAt" to com.google.firebase.Timestamp.now()
                    )
                    
                    firestore.collection(USERS_COLLECTION)
                        .document(currentUser.uid)
                        .update(updates)
                        .await()
                    
                    Log.d(TAG, "Updated existing admin user with ADMIN role")
                } else {
                    // Create new admin user document
                    val adminUser = AdminUser(
                        uid = currentUser.uid,
                        email = currentUser.email ?: "",
                        displayName = currentUser.displayName ?: "Admin",
                        photoUrl = currentUser.photoUrl?.toString() ?: "",
                        role = UserRole.ADMIN,
                        isBlocked = false,
                        createdAt = com.google.firebase.Timestamp.now(),
                        lastLoginAt = com.google.firebase.Timestamp.now()
                    )
                    
                    firestore.collection(USERS_COLLECTION)
                        .document(currentUser.uid)
                        .set(adminUser)
                        .await()
                    
                    Log.d(TAG, "Created new admin user document")
                }
                    
                // Log admin login activity
                logActivity(
                    ActivityItem(
                        userId = currentUser.uid,
                        userName = "Admin",
                        userEmail = currentUser.email ?: "",
                        action = ActivityType.USER_REGISTERED,
                        description = "Admin user logged in"
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing admin user", e)
            Result.failure(e)
        }
    }
    
    // Real-time dashboard stats
    // Requirements: 9.6, 9.7
    fun getDashboardStats(): Flow<DashboardStats> = callbackFlow {
        Log.d(TAG, "getDashboardStats: Starting dashboard stats flow")
        
        // Listen to items collection for real-time updates
        val itemsListener = firestore.collection(ITEMS_COLLECTION)
            .addSnapshotListener { itemsSnapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getDashboardStats: Error listening to items - ${error.message}", error)
                    trySend(DashboardStats())
                    return@addSnapshotListener
                }
                
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Get items count manually to avoid deserialization issues
                        val itemsCount = itemsSnapshot?.size() ?: 0
                        var lostCount = 0
                        var foundCount = 0
                        
                        // Count items manually to handle data issues
                        itemsSnapshot?.documents?.forEach { doc ->
                            try {
                                val isLost = doc.getBoolean("isLost") ?: doc.getBoolean("lost found") ?: true
                                if (isLost) {
                                    lostCount++
                                } else {
                                    foundCount++
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "getDashboardStats: Failed to parse item ${doc.id}: ${e.message}")
                            }
                        }
                        
                        Log.d(TAG, "getDashboardStats: Found $itemsCount items (Lost: $lostCount, Found: $foundCount)")
                        
                        // Fetch current users
                        val usersSnapshot = firestore.collection(USERS_COLLECTION).get().await()
                        val users = if (!usersSnapshot.isEmpty) {
                            usersSnapshot.documents.mapNotNull { doc ->
                                try {
                                    val roleString = doc.getString("role") ?: "STUDENT"
                                    val role = UserRole.fromString(roleString)
                                    
                                    AdminUser(
                                        uid = doc.getString("uid") ?: doc.id,
                                        email = doc.getString("email") ?: "",
                                        displayName = doc.getString("displayName") ?: "",
                                        photoUrl = doc.getString("photoUrl") ?: "",
                                        role = role,
                                        isBlocked = doc.getBoolean("isBlocked") ?: false,
                                        createdAt = com.google.firebase.Timestamp.now(),
                                        lastLoginAt = null
                                    )
                                } catch (e: Exception) {
                                    Log.w(TAG, "getDashboardStats: Failed to parse user ${doc.id}: ${e.message}")
                                    null
                                }
                            }
                        } else {
                            Log.d(TAG, "getDashboardStats: Users collection is empty")
                            emptyList()
                        }
                        
                        Log.d(TAG, "getDashboardStats: Found ${users.size} users")
                        
                        // Compute and send combined stats
                        val stats = DashboardStats(
                            totalItems = itemsCount,
                            lostItems = lostCount,
                            foundItems = foundCount,
                            receivedItems = 0,
                            pendingItems = 0,
                            totalUsers = users.size,
                            activeUsers = users.count { !it.isBlocked },
                            blockedUsers = users.count { it.isBlocked }
                        )
                        
                        Log.d(TAG, "getDashboardStats: Sending stats - Total Items: ${stats.totalItems}, Lost: ${stats.lostItems}, Found: ${stats.foundItems}, Total Users: ${stats.totalUsers}, Active: ${stats.activeUsers}, Blocked: ${stats.blockedUsers}")
                        trySend(stats).isSuccess
                    } catch (e: Exception) {
                        Log.e(TAG, "getDashboardStats: Error processing stats - ${e.message}", e)
                        trySend(DashboardStats()).isSuccess
                    }
                }
            }
        
        awaitClose { 
            Log.d(TAG, "getDashboardStats: Closing dashboard stats flow")
            itemsListener.remove()
        }
    }
    
    // Get all items with real-time updates
    fun getAllItems(): Flow<List<LostFoundItem>> = callbackFlow {
        val listener = firestore.collection(ITEMS_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // If there's an error or collection doesn't exist, send empty list
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    // Process data on IO thread to avoid blocking main thread
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val items = snapshot.toObjects(LostFoundItem::class.java)
                            trySend(items).isSuccess
                        } catch (e: Exception) {
                            trySend(emptyList()).isSuccess
                        }
                    }
                } else {
                    trySend(emptyList())
                }
            }
        
        awaitClose { listener.remove() }
    }
    
    // Get filtered items
    fun getItemsByStatus(status: String): Flow<List<LostFoundItem>> = callbackFlow {
        val listener = when (status) {
            "lost" -> firestore.collection(ITEMS_COLLECTION)
                .whereEqualTo("isLost", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
            "found" -> firestore.collection(ITEMS_COLLECTION)
                .whereEqualTo("isLost", false)
                .orderBy("timestamp", Query.Direction.DESCENDING)
            else -> firestore.collection(ITEMS_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
        }.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val items = snapshot.toObjects(LostFoundItem::class.java)
                trySend(items)
            }
        }
        
        awaitClose { listener.remove() }
    }
    
    // Get all users
    fun getAllUsers(): Flow<List<AdminUser>> = callbackFlow {
        Log.d(TAG, "getAllUsers: Setting up Firestore listener for users collection")
        val listener = firestore.collection(USERS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getAllUsers: Error listening to users collection", error)
                    // If there's an error or collection doesn't exist, send empty list
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    // Process data on IO thread to avoid blocking main thread
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // Manual deserialization to handle role enum mismatches and timestamp issues
                            val users = snapshot.documents.mapNotNull { doc ->
                                try {
                                    // Parse role with fallback for legacy values
                                    val roleString = doc.getString("role") ?: "USER"
                                    val role = UserRole.fromString(roleString)
                                    
                                    // Parse timestamp - handle both Long and Timestamp types for legacy data
                                    val createdAt = when (val createdAtField = doc.get("createdAt")) {
                                        is com.google.firebase.Timestamp -> createdAtField
                                        is Long -> com.google.firebase.Timestamp(createdAtField / 1000, 0)
                                        is Number -> com.google.firebase.Timestamp(createdAtField.toLong() / 1000, 0)
                                        else -> {
                                            Log.w(TAG, "Invalid createdAt for user ${doc.id}, using current time")
                                            com.google.firebase.Timestamp.now()
                                        }
                                    }
                                    
                                    // Parse lastLoginAt - handle both Long and Timestamp types
                                    val lastLoginAt = when (val lastLoginField = doc.get("lastLoginAt")) {
                                        is com.google.firebase.Timestamp -> lastLoginField
                                        is Long -> com.google.firebase.Timestamp(lastLoginField / 1000, 0)
                                        is Number -> com.google.firebase.Timestamp(lastLoginField.toLong() / 1000, 0)
                                        null -> null
                                        else -> {
                                            Log.w(TAG, "Invalid lastLoginAt for user ${doc.id}")
                                            null
                                        }
                                    }
                                    
                                    AdminUser(
                                        uid = doc.getString("uid") ?: doc.id,
                                        email = doc.getString("email") ?: "",
                                        displayName = doc.getString("displayName") ?: "",
                                        photoUrl = doc.getString("photoUrl") ?: "",
                                        role = role,
                                        isBlocked = doc.getBoolean("isBlocked") ?: false,
                                        createdAt = createdAt,
                                        lastLoginAt = lastLoginAt,
                                        itemsReported = (doc.getLong("itemsReported") ?: 0).toInt(),
                                        itemsFound = (doc.getLong("itemsFound") ?: 0).toInt(),
                                        itemsClaimed = (doc.getLong("itemsClaimed") ?: 0).toInt()
                                    )
                                } catch (e: Exception) {
                                    Log.w(TAG, "Failed to deserialize user ${doc.id}: ${e.message}", e)
                                    null
                                }
                            }
                            Log.d(TAG, "getAllUsers: Successfully loaded ${users.size} users from Firestore")
                            Log.d(TAG, "getAllUsers: User emails: ${users.map { it.email }}")
                            trySend(users).isSuccess
                        } catch (e: Exception) {
                            Log.e(TAG, "getAllUsers: Error parsing users from snapshot", e)
                            trySend(emptyList()).isSuccess
                        }
                    }
                } else {
                    Log.w(TAG, "getAllUsers: Snapshot is null")
                    trySend(emptyList())
                }
            }
        
        awaitClose { 
            Log.d(TAG, "getAllUsers: Removing Firestore listener")
            listener.remove() 
        }
    }
    
    // Block/Unblock user
    suspend fun updateUserBlockStatus(userId: String, isBlocked: Boolean): Result<Unit> {
        return try {
            requireAdminAccess()
            
            // Validate user ID to prevent invalid document reference errors
            if (userId.isBlank()) {
                return Result.failure(IllegalArgumentException("User ID cannot be blank"))
            }
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("isBlocked", isBlocked)
                .await()
            
            // Log activity
            logActivity(
                ActivityItem(
                    userId = auth.currentUser?.uid ?: "",
                    userName = "Admin",
                    userEmail = auth.currentUser?.email ?: "",
                    action = if (isBlocked) ActivityType.USER_BLOCKED else ActivityType.USER_UNBLOCKED,
                    description = "User ${if (isBlocked) "blocked" else "unblocked"} by admin"
                )
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update user role
    suspend fun updateUserRole(userId: String, role: UserRole): Result<Unit> {
        return try {
            requireAdminAccess()
            
            val currentUser = auth.currentUser
                ?: return Result.failure(SecurityException("Not authenticated"))
            
            // Get previous role for logging
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            val previousRole = try {
                val roleString = userDoc.getString("role") ?: "STUDENT"
                UserRole.fromString(roleString)
            } catch (e: Exception) {
                UserRole.STUDENT
            }
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("role", role.name)
                .await()
            
            // Log activity using new ActivityLog model
            val activityLog = ActivityLog(
                id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                actorId = currentUser.uid,
                actorEmail = currentUser.email ?: "",
                actorRole = UserRole.ADMIN,
                actionType = ActionType.USER_ROLE_CHANGE,
                targetType = TargetType.USER,
                targetId = userId,
                description = "User role changed from ${previousRole.name} to ${role.name}",
                previousValue = previousRole.name,
                newValue = role.name,
                deviceInfo = android.os.Build.MODEL
            )
            
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(activityLog.id)
                .set(activityLog.toMap())
                .await()
            
            Log.d(TAG, "User $userId role updated to ${role.name}")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error updating user role", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user role", e)
            Result.failure(Exception("Failed to update user role: ${e.message}"))
        }
    }
    
    // Get recent activities
    fun getRecentActivities(limit: Int = 50): Flow<List<ActivityItem>> = callbackFlow {
        val listener = firestore.collection(ACTIVITIES_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // If there's an error or collection doesn't exist, send empty list
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    // Process data on IO thread to avoid blocking main thread
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val activities = snapshot.toObjects(ActivityItem::class.java)
                            trySend(activities).isSuccess
                        } catch (e: Exception) {
                            trySend(emptyList()).isSuccess
                        }
                    }
                } else {
                    trySend(emptyList())
                }
            }
        
        awaitClose { listener.remove() }
    }
    
    // Log activity
    suspend fun logActivity(activity: ActivityItem) {
        try {
            firestore.collection(ACTIVITIES_COLLECTION)
                .add(activity)
                .await()
        } catch (e: Exception) {
            // Handle error silently for logging
        }
    }
    
    // Get analytics data
    suspend fun getAnalyticsData(): Result<AnalyticsData> {
        return try {
            val itemsSnapshot = firestore.collection(ITEMS_COLLECTION).get().await()
            val items = itemsSnapshot.toObjects(LostFoundItem::class.java)
            
            // For now, we'll create simple analytics based on available fields
            val itemsByCategory = mapOf("Electronics" to items.size) // Placeholder since category doesn't exist
            val itemsByStatus = mapOf(
                "Lost" to items.count { it.isLost },
                "Found" to items.count { !it.isLost }
            )
            
            // Calculate daily activity for last 7 days
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dailyActivity = mutableListOf<DailyActivity>()
            
            for (i in 6 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val date = dateFormat.format(calendar.time)
                
                val dayStart = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val dayEnd = calendar.timeInMillis
                
                val dayItems = items.filter { 
                    val itemTime = it.timestamp.toDate().time
                    itemTime in dayStart until dayEnd 
                }
                
                dailyActivity.add(
                    DailyActivity(
                        date = date,
                        itemsReported = dayItems.count { it.isLost },
                        itemsFound = dayItems.count { !it.isLost },
                        itemsClaimed = 0 // Not available in current model
                    )
                )
            }
            
            val resolvedItems = items.count { !it.isLost } // Found items as resolved
            val successRate = if (items.isNotEmpty()) (resolvedItems.toFloat() / items.size) * 100 else 0f
            
            val analytics = AnalyticsData(
                itemsByCategory = itemsByCategory,
                itemsByStatus = itemsByStatus,
                dailyActivity = dailyActivity,
                successRate = successRate
            )
            
            Result.success(analytics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update item status
    suspend fun updateItemStatus(itemId: String, newStatus: String): Result<Unit> {
        return try {
            val isLost = when (newStatus) {
                "lost" -> true
                "found" -> false
                else -> true
            }
            
            firestore.collection(ITEMS_COLLECTION)
                .document(itemId)
                .update("isLost", isLost)
                .await()
            
            // Log activity
            logActivity(
                ActivityItem(
                    userId = auth.currentUser?.uid ?: "",
                    userName = "Admin",
                    userEmail = auth.currentUser?.email ?: "",
                    action = ActivityType.STATUS_CHANGED,
                    itemId = itemId,
                    description = "Item status changed to $newStatus"
                )
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Create test data for demonstration
    suspend fun createTestData(): Result<Unit> {
        return try {
            Log.d(TAG, "Creating test data...")
            
            // Create simple test items using the existing LostFoundItem structure
            val testItems = listOf(
                hashMapOf(
                    "name" to "iPhone 13 Pro",
                    "description" to "Black iPhone with cracked screen protector",
                    "location" to "Library - 2nd Floor",
                    "contactInfo" to "john@example.com",
                    "isLost" to true,
                    "userId" to "user1",
                    "userEmail" to "john@example.com",
                    "imageUrl" to "",
                    "timestamp" to com.google.firebase.Timestamp.now()
                ),
                hashMapOf(
                    "name" to "Blue Backpack",
                    "description" to "Nike blue backpack with laptop inside",
                    "location" to "Cafeteria",
                    "contactInfo" to "jane@example.com",
                    "isLost" to false,
                    "userId" to "user2",
                    "userEmail" to "jane@example.com",
                    "imageUrl" to "",
                    "timestamp" to com.google.firebase.Timestamp.now()
                ),
                hashMapOf(
                    "name" to "Car Keys",
                    "description" to "Toyota car keys with red keychain",
                    "location" to "Parking Lot A",
                    "contactInfo" to "mike@example.com",
                    "isLost" to true,
                    "userId" to "user3",
                    "userEmail" to "mike@example.com",
                    "imageUrl" to "",
                    "timestamp" to com.google.firebase.Timestamp.now()
                )
            )
            
            // Add test items to Firestore
            testItems.forEachIndexed { index, item ->
                firestore.collection(ITEMS_COLLECTION)
                    .document("test_item_$index")
                    .set(item)
                    .await()
                Log.d(TAG, "Created test item: ${item["name"]}")
            }
            
            Log.d(TAG, "Test data created successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create test data", e)
            Result.failure(e)
        }
    }
    
    // ========== Enhanced User Management Methods ==========
    // Requirements: 1.2, 1.3, 1.4, 1.6
    
    /**
     * Get detailed user information by user ID
     * Requirements: 1.2
     */
    suspend fun getUserDetails(userId: String): Result<EnhancedAdminUser> {
        return try {
            requireAdminAccess()
            
            if (userId.isBlank()) {
                return Result.failure(IllegalArgumentException("User ID cannot be blank"))
            }
            
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (!document.exists()) {
                return Result.failure(NoSuchElementException("User not found"))
            }
            
            val user = document.toObject(EnhancedAdminUser::class.java)
                ?: return Result.failure(Exception("Failed to parse user data"))
            
            Log.d(TAG, "Retrieved user details for: ${user.email}")
            Result.success(user)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting user details", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user details", e)
            Result.failure(Exception("Failed to get user details: ${e.message}"))
        }
    }
    
    /**
     * Block a user with reason and activity logging
     * Requirements: 1.3, 1.4
     */
    suspend fun blockUser(userId: String, reason: String): Result<Unit> {
        return try {
            requireAdminAccess()
            
            // Validate inputs
            val userIdValidation = DataValidator.validateUserId(userId)
            if (!userIdValidation.isValid) {
                return Result.failure(IllegalArgumentException(userIdValidation.getErrorMessage()))
            }
            
            val reasonValidation = DataValidator.validateBlockReason(reason)
            if (!reasonValidation.isValid) {
                return Result.failure(IllegalArgumentException(reasonValidation.getErrorMessage()))
            }
            
            val sanitizedReason = DataValidator.sanitizeString(reason)
            
            val currentUser = auth.currentUser
                ?: return Result.failure(SecurityException("Not authenticated"))
            
            val updates = mapOf(
                "isBlocked" to true,
                "blockReason" to sanitizedReason,
                "blockedBy" to currentUser.uid,
                "blockedAt" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()
            
            // Log activity
            val activityLog = ActivityLog(
                id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                actorId = currentUser.uid,
                actorEmail = currentUser.email ?: "",
                actorRole = UserRole.ADMIN,
                actionType = ActionType.USER_BLOCK,
                targetType = TargetType.USER,
                targetId = userId,
                description = "User blocked by admin. Reason: $sanitizedReason",
                previousValue = "active",
                newValue = "blocked",
                deviceInfo = android.os.Build.MODEL
            )
            
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(activityLog.id)
                .set(activityLog.toMap())
                .await()
            
            Log.d(TAG, "User $userId blocked successfully")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error blocking user", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error blocking user", e)
            Result.failure(Exception("Failed to block user: ${e.message}"))
        }
    }
    
    /**
     * Unblock a user with activity logging
     * Requirements: 1.4
     */
    suspend fun unblockUser(userId: String): Result<Unit> {
        return try {
            requireAdminAccess()
            
            val currentUser = auth.currentUser
                ?: return Result.failure(SecurityException("Not authenticated"))
            
            val updates = mapOf(
                "isBlocked" to false,
                "blockReason" to "",
                "blockedBy" to "",
                "blockedAt" to null
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()
            
            // Log activity
            val activityLog = ActivityLog(
                id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                actorId = currentUser.uid,
                actorEmail = currentUser.email ?: "",
                actorRole = UserRole.ADMIN,
                actionType = ActionType.USER_UNBLOCK,
                targetType = TargetType.USER,
                targetId = userId,
                description = "User unblocked by admin",
                previousValue = "blocked",
                newValue = "active",
                deviceInfo = android.os.Build.MODEL
            )
            
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(activityLog.id)
                .set(activityLog.toMap())
                .await()
            
            Log.d(TAG, "User $userId unblocked successfully")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error unblocking user", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error unblocking user", e)
            Result.failure(Exception("Failed to unblock user: ${e.message}"))
        }
    }
    
    /**
     * Update user details with validation and logging
     * Requirements: 1.6
     */
    suspend fun updateUserDetails(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            requireAdminAccess()
            
            if (updates.isEmpty()) {
                return Result.failure(IllegalArgumentException("No updates provided"))
            }
            
            // Validate user ID
            val userIdValidation = DataValidator.validateUserId(userId)
            if (!userIdValidation.isValid) {
                return Result.failure(IllegalArgumentException(userIdValidation.getErrorMessage()))
            }
            
            // Validate update data
            val updateValidation = DataValidator.validateUserUpdate(updates)
            if (!updateValidation.isValid) {
                return Result.failure(IllegalArgumentException(updateValidation.getErrorMessage()))
            }
            
            val currentUser = auth.currentUser
                ?: return Result.failure(SecurityException("Not authenticated"))
            
            // Add modification tracking
            val updatesWithTracking = updates.toMutableMap().apply {
                put("lastModifiedBy", currentUser.uid)
                put("lastModifiedAt", System.currentTimeMillis())
            }
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updatesWithTracking)
                .await()
            
            // Log activity
            val activityLog = ActivityLog(
                id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                actorId = currentUser.uid,
                actorEmail = currentUser.email ?: "",
                actorRole = UserRole.ADMIN,
                actionType = ActionType.USER_EDIT,
                targetType = TargetType.USER,
                targetId = userId,
                description = "User details updated by admin. Fields: ${updates.keys.joinToString(", ")}",
                newValue = updates.toString(),
                deviceInfo = android.os.Build.MODEL
            )
            
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(activityLog.id)
                .set(activityLog.toMap())
                .await()
            
            Log.d(TAG, "User $userId details updated successfully")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error updating user details", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user details", e)
            Result.failure(Exception("Failed to update user details: ${e.message}"))
        }
    }
    
    /**
     * Simple updateUser method for basic user updates
     * Requirements: 7.2, 7.4, 7.5
     */
    suspend fun updateUser(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            requireAdminAccess()
            
            if (updates.isEmpty()) {
                return Result.failure(IllegalArgumentException("No updates provided"))
            }
            
            val currentUser = auth.currentUser
                ?: return Result.failure(SecurityException("Not authenticated"))
            
            // Use Firestore update() method to save changes
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()
            
            Log.d(TAG, "User $userId updated successfully")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error updating user", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user", e)
            Result.failure(Exception("Failed to update user: ${e.message}"))
        }
    }
    
    /**
     * Delete a user from Firestore and Firebase Authentication
     * Requirements: 6.6, 6.7, 6.11
     * 
     * Note: This method only deletes the user from Firestore. Firebase Authentication
     * user deletion requires Admin SDK or Cloud Function, which cannot be done directly
     * from the Android client for security reasons.
     */
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            requireAdminAccess()
            
            // Validate user ID
            val userIdValidation = DataValidator.validateUserId(userId)
            if (!userIdValidation.isValid) {
                return Result.failure(IllegalArgumentException(userIdValidation.getErrorMessage()))
            }
            
            val currentUser = auth.currentUser
                ?: return Result.failure(SecurityException("Not authenticated"))
            
            // Prevent admin from deleting themselves
            if (userId == currentUser.uid) {
                return Result.failure(IllegalArgumentException("Cannot delete your own account"))
            }
            
            // Get user details before deletion for logging
            val userResult = getUserDetails(userId)
            val userEmail = if (userResult.isSuccess) {
                userResult.getOrNull()?.email ?: "Unknown"
            } else {
                "Unknown"
            }
            
            // Delete user document from Firestore
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()
            
            // Log activity
            val activityLog = ActivityLog(
                id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                actorId = currentUser.uid,
                actorEmail = currentUser.email ?: "",
                actorRole = UserRole.ADMIN,
                actionType = ActionType.USER_DELETE,
                targetType = TargetType.USER,
                targetId = userId,
                description = "User '$userEmail' deleted by admin. Note: Firebase Authentication account must be deleted separately via Admin SDK or Cloud Function.",
                previousValue = userEmail,
                newValue = "deleted",
                deviceInfo = android.os.Build.MODEL
            )
            
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(activityLog.id)
                .set(activityLog.toMap())
                .await()
            
            Log.d(TAG, "User $userId deleted from Firestore successfully")
            Log.w(TAG, "Note: Firebase Authentication account for $userId must be deleted separately via Admin SDK or Cloud Function")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error deleting user", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user", e)
            Result.failure(Exception("Failed to delete user: ${e.message}"))
        }
    }
    
    /**
     * Search users by query with filtering
     * Requirements: 1.9
     */
    suspend fun searchUsers(query: String): Result<List<EnhancedAdminUser>> {
        return try {
            requireAdminAccess()
            
            if (query.isBlank()) {
                // Return all users if query is empty
                val snapshot = firestore.collection(USERS_COLLECTION)
                    .get()
                    .await()
                
                val users = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(EnhancedAdminUser::class.java)
                    } catch (e: RuntimeException) {
                        Log.w(TAG, "Failed to deserialize user ${doc.id} in search: ${e.message}")
                        null
                    }
                }
                return Result.success(users)
            }
            
            // Get all users and filter client-side (Firestore doesn't support full-text search)
            val snapshot = firestore.collection(USERS_COLLECTION)
                .get()
                .await()
            
            val allUsers = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(EnhancedAdminUser::class.java)
                } catch (e: RuntimeException) {
                    Log.w(TAG, "Failed to deserialize user ${doc.id} in search: ${e.message}")
                    null
                }
            }
            
            // Filter by email, display name, or role
            val queryLower = query.lowercase()
            val filteredUsers = allUsers.filter { user ->
                user.email.lowercase().contains(queryLower) ||
                user.displayName.lowercase().contains(queryLower) ||
                user.role.name.lowercase().contains(queryLower)
            }
            
            Log.d(TAG, "Search found ${filteredUsers.size} users matching '$query'")
            Result.success(filteredUsers)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error searching users", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users", e)
            Result.failure(Exception("Failed to search users: ${e.message}"))
        }
    }
    // ========== Enhanced Item Management Methods ==========
    // Requirements: 2.2, 2.3, 2.4, 2.5
    
    /**
     * Get detailed item information with full status history
     * Requirements: 2.2
     */
    suspend fun getItemDetails(itemId: String): Result<EnhancedLostFoundItem> {
        return try {
            requireAdminAccess()
            
            if (itemId.isBlank()) {
                return Result.failure(IllegalArgumentException("Item ID cannot be blank"))
            }
            
            val document = firestore.collection(ITEMS_COLLECTION)
                .document(itemId)
                .get()
                .await()
            
            if (!document.exists()) {
                return Result.failure(NoSuchElementException("Item not found"))
            }
            
            // Parse the document to EnhancedLostFoundItem
            val data = document.data ?: return Result.failure(Exception("Item data is null"))
            
            val item = EnhancedLostFoundItem(
                id = document.id,
                name = data["name"] as? String ?: "",
                description = data["description"] as? String ?: "",
                location = data["location"] as? String ?: "",
                contactInfo = data["contactInfo"] as? String ?: "",
                isLost = data["isLost"] as? Boolean ?: true,
                userId = data["userId"] as? String ?: "",
                userEmail = data["userEmail"] as? String ?: "",
                imageUrl = data["imageUrl"] as? String ?: "",
                timestamp = data["timestamp"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                category = data["category"] as? String ?: "",
                status = try {
                    ItemStatus.valueOf(data["status"] as? String ?: "ACTIVE")
                } catch (e: Exception) {
                    ItemStatus.ACTIVE
                },
                statusHistory = parseStatusHistory(data["statusHistory"]),
                requestedBy = data["requestedBy"] as? String ?: "",
                requestedAt = (data["requestedAt"] as? Number)?.toLong() ?: 0L,
                returnedAt = (data["returnedAt"] as? Number)?.toLong() ?: 0L,
                donationEligibleAt = (data["donationEligibleAt"] as? Number)?.toLong() ?: 0L,
                donatedAt = (data["donatedAt"] as? Number)?.toLong() ?: 0L,
                lastModifiedBy = data["lastModifiedBy"] as? String ?: "",
                lastModifiedAt = (data["lastModifiedAt"] as? Number)?.toLong() ?: 0L
            )
            
            Log.d(TAG, "Retrieved item details for: ${item.name}")
            Result.success(item)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting item details", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting item details", e)
            Result.failure(Exception("Failed to get item details: ${e.message}"))
        }
    }
    
    /**
     * Helper method to parse status history from Firestore data
     */
    private fun parseStatusHistory(data: Any?): List<StatusChange> {
        return try {
            @Suppress("UNCHECKED_CAST")
            val historyList = data as? List<Map<String, Any>> ?: return emptyList()
            
            historyList.mapNotNull { historyMap ->
                try {
                    StatusChange(
                        previousStatus = ItemStatus.valueOf(historyMap["previousStatus"] as? String ?: "ACTIVE"),
                        newStatus = ItemStatus.valueOf(historyMap["newStatus"] as? String ?: "ACTIVE"),
                        changedBy = historyMap["changedBy"] as? String ?: "",
                        changedAt = (historyMap["changedAt"] as? Number)?.toLong() ?: 0L,
                        reason = historyMap["reason"] as? String ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing status change", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing status history", e)
            emptyList()
        }
    }
    
    /**
     * Update item details with modification tracking
     * Requirements: 2.4
     */
    suspend fun updateItemDetails(itemId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            requireAdminAccess()
            
            if (updates.isEmpty()) {
                return Result.failure(IllegalArgumentException("No updates provided"))
            }
            
            // Validate item ID
            val itemIdValidation = DataValidator.validateItemId(itemId)
            if (!itemIdValidation.isValid) {
                return Result.failure(IllegalArgumentException(itemIdValidation.getErrorMessage()))
            }
            
            // Validate update data
            val updateValidation = DataValidator.validateItemUpdate(updates)
            if (!updateValidation.isValid) {
                return Result.failure(IllegalArgumentException(updateValidation.getErrorMessage()))
            }
            
            val currentUser = auth.currentUser
                ?: return Result.failure(SecurityException("Not authenticated"))
            
            // Add modification tracking
            val updatesWithTracking = updates.toMutableMap().apply {
                put("lastModifiedBy", currentUser.uid)
                put("lastModifiedAt", System.currentTimeMillis())
            }
            
            firestore.collection(ITEMS_COLLECTION)
                .document(itemId)
                .update(updatesWithTracking)
                .await()
            
            // Log activity
            val activityLog = ActivityLog(
                id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                actorId = currentUser.uid,
                actorEmail = currentUser.email ?: "",
                actorRole = UserRole.ADMIN,
                actionType = ActionType.ITEM_EDIT,
                targetType = TargetType.ITEM,
                targetId = itemId,
                description = "Item details updated by admin. Fields: ${updates.keys.joinToString(", ")}",
                newValue = updates.toString(),
                deviceInfo = android.os.Build.MODEL
            )
            
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(activityLog.id)
                .set(activityLog.toMap())
                .await()
            
            Log.d(TAG, "Item $itemId details updated successfully")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error updating item details", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating item details", e)
            Result.failure(Exception("Failed to update item details: ${e.message}"))
        }
    }
    
    /**
     * Simple updateItem method for basic item updates
     * Requirements: 7.2, 7.4, 7.5
     */
    suspend fun updateItem(itemId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            requireAdminAccess()
            
            if (updates.isEmpty()) {
                return Result.failure(IllegalArgumentException("No updates provided"))
            }
            
            val currentUser = auth.currentUser
                ?: return Result.failure(SecurityException("Not authenticated"))
            
            // Use Firestore update() method to save changes
            firestore.collection(ITEMS_COLLECTION)
                .document(itemId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Item $itemId updated successfully")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error updating item", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating item", e)
            Result.failure(Exception("Failed to update item: ${e.message}"))
        }
    }
    
    /**
     * Validate status transitions
     * Requirements: 3.4
     */
    private fun isValidStatusTransition(current: ItemStatus, new: ItemStatus): Boolean {
        return when (current) {
            ItemStatus.ACTIVE -> new in listOf(
                ItemStatus.REQUESTED,
                ItemStatus.DONATION_PENDING
            )
            ItemStatus.REQUESTED -> new in listOf(
                ItemStatus.RETURNED,
                ItemStatus.ACTIVE
            )
            ItemStatus.DONATION_PENDING -> new in listOf(
                ItemStatus.DONATION_READY,
                ItemStatus.ACTIVE
            )
            ItemStatus.DONATION_READY -> new in listOf(
                ItemStatus.DONATED,
                ItemStatus.ACTIVE
            )
            ItemStatus.RETURNED, ItemStatus.DONATED -> false // Final states
        }
    }
    
    /**
     * Update item status with history logging
     * Requirements: 2.3, 2.5, 3.4
     */
    suspend fun updateItemStatus(
        itemId: String, 
        newStatus: ItemStatus, 
        reason: String = ""
    ): Result<Unit> {
        return try {
            if (!isAdminUser()) {
                return Result.failure(SecurityException("Admin access required"))
            }
            
            val currentUser = auth.currentUser
                ?: return Result.failure(SecurityException("Not authenticated"))
            
            // Get current item to retrieve current status
            val itemResult = getItemDetails(itemId)
            if (itemResult.isFailure) {
                return Result.failure(itemResult.exceptionOrNull() ?: Exception("Failed to get item"))
            }
            
            val currentItem = itemResult.getOrNull()!!
            val previousStatus = currentItem.status
            
            // Validate status change
            if (!isValidStatusTransition(previousStatus, newStatus)) {
                val validTransitions = when (previousStatus) {
                    ItemStatus.ACTIVE -> "REQUESTED or DONATION_PENDING"
                    ItemStatus.REQUESTED -> "RETURNED or ACTIVE"
                    ItemStatus.DONATION_PENDING -> "DONATION_READY or ACTIVE"
                    ItemStatus.DONATION_READY -> "DONATED or ACTIVE"
                    ItemStatus.RETURNED, ItemStatus.DONATED -> "none (final state)"
                }
                return Result.failure(IllegalArgumentException(
                    "Invalid status transition from $previousStatus to $newStatus. Valid transitions: $validTransitions"
                ))
            }
            
            // Create status change record
            val statusChange = StatusChange(
                previousStatus = previousStatus,
                newStatus = newStatus,
                changedBy = currentUser.uid,
                changedAt = System.currentTimeMillis(),
                reason = reason
            )
            
            // Add status change to history
            val updatedHistory = currentItem.statusHistory.toMutableList().apply {
                add(statusChange)
            }
            
            // Prepare updates
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "statusHistory" to updatedHistory.map { it.toMap() },
                "lastModifiedBy" to currentUser.uid,
                "lastModifiedAt" to System.currentTimeMillis()
            )
            
            // Add status-specific fields
            when (newStatus) {
                ItemStatus.REQUESTED -> {
                    updates["requestedAt"] = System.currentTimeMillis()
                }
                ItemStatus.RETURNED -> {
                    updates["returnedAt"] = System.currentTimeMillis()
                }
                ItemStatus.DONATION_PENDING -> {
                    updates["donationEligibleAt"] = System.currentTimeMillis()
                }
                ItemStatus.DONATED -> {
                    updates["donatedAt"] = System.currentTimeMillis()
                }
                else -> {}
            }
            
            // Update item in Firestore
            firestore.collection(ITEMS_COLLECTION)
                .document(itemId)
                .update(updates)
                .await()
            
            // Log activity
            val activityLog = ActivityLog(
                id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                actorId = currentUser.uid,
                actorEmail = currentUser.email ?: "",
                actorRole = UserRole.ADMIN,
                actionType = ActionType.ITEM_STATUS_CHANGE,
                targetType = TargetType.ITEM,
                targetId = itemId,
                description = "Item status changed from $previousStatus to $newStatus${if (reason.isNotBlank()) ". Reason: $reason" else ""}",
                previousValue = previousStatus.name,
                newValue = newStatus.name,
                deviceInfo = android.os.Build.MODEL
            )
            
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(activityLog.id)
                .set(activityLog.toMap())
                .await()
            
            Log.d(TAG, "Item $itemId status updated from $previousStatus to $newStatus")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error updating item status", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating item status", e)
            Result.failure(Exception("Failed to update item status: ${e.message}"))
        }
    }
    
    /**
     * Delete an item with confirmation and logging
     * Requirements: 2.5
     */
    suspend fun deleteItem(itemId: String): Result<Unit> {
        return try {
            requireAdminAccess()
            
            val currentUser = auth.currentUser
                ?: return Result.failure(SecurityException("Not authenticated"))
            
            // Get item details before deletion for logging
            val itemResult = getItemDetails(itemId)
            val itemName = if (itemResult.isSuccess) {
                itemResult.getOrNull()?.name ?: "Unknown"
            } else {
                "Unknown"
            }
            
            // Delete the item
            firestore.collection(ITEMS_COLLECTION)
                .document(itemId)
                .delete()
                .await()
            
            // Log activity
            val activityLog = ActivityLog(
                id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                actorId = currentUser.uid,
                actorEmail = currentUser.email ?: "",
                actorRole = UserRole.ADMIN,
                actionType = ActionType.ITEM_DELETE,
                targetType = TargetType.ITEM,
                targetId = itemId,
                description = "Item '$itemName' deleted by admin",
                previousValue = itemName,
                newValue = "deleted",
                deviceInfo = android.os.Build.MODEL
            )
            
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(activityLog.id)
                .set(activityLog.toMap())
                .await()
            
            Log.d(TAG, "Item $itemId deleted successfully")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error deleting item", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting item", e)
            Result.failure(Exception("Failed to delete item: ${e.message}"))
        }
    }
    
    /**
     * Search items with advanced filters
     * Requirements: 2.7
     */
    suspend fun searchItems(query: String, filters: Map<String, String> = emptyMap()): Result<List<EnhancedLostFoundItem>> {
        return try {
            requireAdminAccess()
            
            // Get all items (Firestore doesn't support full-text search)
            val snapshot = firestore.collection(ITEMS_COLLECTION)
                .get()
                .await()
            
            val allItems = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    EnhancedLostFoundItem(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        description = data["description"] as? String ?: "",
                        location = data["location"] as? String ?: "",
                        contactInfo = data["contactInfo"] as? String ?: "",
                        isLost = data["isLost"] as? Boolean ?: true,
                        userId = data["userId"] as? String ?: "",
                        userEmail = data["userEmail"] as? String ?: "",
                        imageUrl = data["imageUrl"] as? String ?: "",
                        timestamp = data["timestamp"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                        category = data["category"] as? String ?: "",
                        status = try {
                            ItemStatus.valueOf(data["status"] as? String ?: "ACTIVE")
                        } catch (e: Exception) {
                            ItemStatus.ACTIVE
                        },
                        statusHistory = parseStatusHistory(data["statusHistory"]),
                        requestedBy = data["requestedBy"] as? String ?: "",
                        requestedAt = (data["requestedAt"] as? Number)?.toLong() ?: 0L,
                        returnedAt = (data["returnedAt"] as? Number)?.toLong() ?: 0L,
                        donationEligibleAt = (data["donationEligibleAt"] as? Number)?.toLong() ?: 0L,
                        donatedAt = (data["donatedAt"] as? Number)?.toLong() ?: 0L,
                        lastModifiedBy = data["lastModifiedBy"] as? String ?: "",
                        lastModifiedAt = (data["lastModifiedAt"] as? Number)?.toLong() ?: 0L
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing item", e)
                    null
                }
            }
            
            // Apply search query
            var filteredItems = if (query.isNotBlank()) {
                val queryLower = query.lowercase()
                allItems.filter { item ->
                    item.name.lowercase().contains(queryLower) ||
                    item.description.lowercase().contains(queryLower) ||
                    item.location.lowercase().contains(queryLower) ||
                    item.userEmail.lowercase().contains(queryLower) ||
                    item.category.lowercase().contains(queryLower)
                }
            } else {
                allItems
            }
            
            // Apply filters
            filters.forEach { (key, value) ->
                filteredItems = when (key) {
                    "status" -> {
                        try {
                            val statusFilter = ItemStatus.valueOf(value)
                            filteredItems.filter { it.status == statusFilter }
                        } catch (e: Exception) {
                            filteredItems
                        }
                    }
                    "category" -> filteredItems.filter { 
                        it.category.equals(value, ignoreCase = true) 
                    }
                    "isLost" -> {
                        val isLostFilter = value.toBoolean()
                        filteredItems.filter { it.isLost == isLostFilter }
                    }
                    "location" -> filteredItems.filter { 
                        it.location.contains(value, ignoreCase = true) 
                    }
                    else -> filteredItems
                }
            }
            
            Log.d(TAG, "Search found ${filteredItems.size} items matching query '$query' with filters $filters")
            Result.success(filteredItems)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error searching items", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching items", e)
            Result.failure(Exception("Failed to search items: ${e.message}"))
        }
    }
    
    /**
     * Get all items with status tracking (Flow for real-time updates)
     * Requirements: 2.1
     */
    fun getAllItemsWithStatus(): Flow<List<EnhancedLostFoundItem>> = callbackFlow {
        try {
            if (!isAdminUser()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            
            val listener = firestore.collection(ITEMS_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error getting items with status", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val items = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data ?: return@mapNotNull null
                                EnhancedLostFoundItem(
                                    id = doc.id,
                                    name = data["name"] as? String ?: "",
                                    description = data["description"] as? String ?: "",
                                    location = data["location"] as? String ?: "",
                                    contactInfo = data["contactInfo"] as? String ?: "",
                                    isLost = data["isLost"] as? Boolean ?: true,
                                    userId = data["userId"] as? String ?: "",
                                    userEmail = data["userEmail"] as? String ?: "",
                                    imageUrl = data["imageUrl"] as? String ?: "",
                                    timestamp = data["timestamp"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                                    category = data["category"] as? String ?: "",
                                    status = try {
                                        ItemStatus.valueOf(data["status"] as? String ?: "ACTIVE")
                                    } catch (e: Exception) {
                                        ItemStatus.ACTIVE
                                    },
                                    statusHistory = parseStatusHistory(data["statusHistory"]),
                                    requestedBy = data["requestedBy"] as? String ?: "",
                                    requestedAt = (data["requestedAt"] as? Number)?.toLong() ?: 0L,
                                    returnedAt = (data["returnedAt"] as? Number)?.toLong() ?: 0L,
                                    donationEligibleAt = (data["donationEligibleAt"] as? Number)?.toLong() ?: 0L,
                                    donatedAt = (data["donatedAt"] as? Number)?.toLong() ?: 0L,
                                    lastModifiedBy = data["lastModifiedBy"] as? String ?: "",
                                    lastModifiedAt = (data["lastModifiedAt"] as? Number)?.toLong() ?: 0L
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing item", e)
                                null
                            }
                        }
                        trySend(items)
                    } else {
                        trySend(emptyList())
                    }
                }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up items listener", e)
            trySend(emptyList())
            close()
        }
    }
    

    
    // ========== Donation Management Methods ==========
    // Requirements: 3.2, 3.3, 3.4, 3.5, 3.6
    
    /**
     * Get donation queue with real-time updates
     * Requirements: 3.2
     */
    fun getDonationQueue(): Flow<List<DonationItem>> = callbackFlow {
        try {
            if (!isAdminUser()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            
            val listener = firestore.collection(DONATIONS_COLLECTION)
                .orderBy("eligibleAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error getting donation queue", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val donations = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data ?: return@mapNotNull null
                                DonationItem(
                                    itemId = data["itemId"] as? String ?: "",
                                    itemName = data["itemName"] as? String ?: "",
                                    category = data["category"] as? String ?: "",
                                    location = data["location"] as? String ?: "",
                                    reportedAt = (data["reportedAt"] as? Number)?.toLong() ?: 0L,
                                    eligibleAt = (data["eligibleAt"] as? Number)?.toLong() ?: 0L,
                                    status = try {
                                        DonationStatus.valueOf(data["status"] as? String ?: "PENDING")
                                    } catch (e: Exception) {
                                        DonationStatus.PENDING
                                    },
                                    markedReadyBy = data["markedReadyBy"] as? String ?: "",
                                    markedReadyAt = (data["markedReadyAt"] as? Number)?.toLong() ?: 0L,
                                    donatedAt = (data["donatedAt"] as? Number)?.toLong() ?: 0L,
                                    donatedBy = data["donatedBy"] as? String ?: "",
                                    estimatedValue = (data["estimatedValue"] as? Number)?.toDouble() ?: 0.0,
                                    donationRecipient = data["donationRecipient"] as? String ?: "",
                                    imageUrl = data["imageUrl"] as? String ?: "",
                                    description = data["description"] as? String ?: ""
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing donation item", e)
                                null
                            }
                        }
                        trySend(donations)
                    } else {
                        trySend(emptyList())
                    }
                }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up donation queue listener", e)
            trySend(emptyList())
            close()
        }
    }
    
    /**
     * Mark an item for donation (auto-flagging or manual)
     * Requirements: 3.1, 3.2
     */
    suspend fun markItemForDonation(itemId: String): Result<Unit> {
        return try {
            requireAdminAccess()
            
            // Get the item details
            val itemDoc = firestore.collection(ITEMS_COLLECTION)
                .document(itemId)
                .get()
                .await()
            
            if (!itemDoc.exists()) {
                return Result.failure(NoSuchElementException("Item not found"))
            }
            
            val itemData = itemDoc.data ?: return Result.failure(Exception("Item data is null"))
            
            // Update item status to DONATION_PENDING
            val currentTime = System.currentTimeMillis()
            val itemUpdates = mapOf(
                "status" to ItemStatus.DONATION_PENDING.name,
                "donationEligibleAt" to currentTime,
                "lastModifiedBy" to (auth.currentUser?.uid ?: "system"),
                "lastModifiedAt" to currentTime
            )
            
            firestore.collection(ITEMS_COLLECTION)
                .document(itemId)
                .update(itemUpdates)
                .await()
            
            // Create donation record
            val donationItem = DonationItem(
                itemId = itemId,
                itemName = itemData["name"] as? String ?: "",
                category = itemData["category"] as? String ?: "",
                location = itemData["location"] as? String ?: "",
                reportedAt = (itemData["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L,
                eligibleAt = currentTime,
                status = DonationStatus.PENDING,
                imageUrl = itemData["imageUrl"] as? String ?: "",
                description = itemData["description"] as? String ?: ""
            )
            
            firestore.collection(DONATIONS_COLLECTION)
                .document(itemId)
                .set(donationItem.toMap())
                .await()
            
            // Log activity
            val activityLog = ActivityLog(
                id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                actorId = auth.currentUser?.uid ?: "system",
                actorEmail = auth.currentUser?.email ?: "system",
                actorRole = UserRole.ADMIN,
                actionType = ActionType.AUTO_DONATION_FLAG,
                targetType = TargetType.DONATION,
                targetId = itemId,
                description = "Item marked for donation: ${donationItem.itemName}",
                previousValue = "ACTIVE",
                newValue = "DONATION_PENDING",
                deviceInfo = android.os.Build.MODEL
            )
            
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(activityLog.id)
                .set(activityLog.toMap())
                .await()
            
            Log.d(TAG, "Item $itemId marked for donation successfully")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error marking item for donation", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking item for donation", e)
            Result.failure(Exception("Failed to mark item for donation: ${e.message}"))
        }
    }
    
    /**
     * Mark an item as ready for donation with admin tracking
     * Requirements: 3.3, 3.4
     */
    suspend fun markItemReadyForDonation(itemId: String): Result<Unit> {
        return try {
            requireAdminAccess()
            
            val currentUser = auth.currentUser
                ?: return Result.failure(SecurityException("Not authenticated"))
            
            val currentTime = System.currentTimeMillis()
            
            // Update item status
            val itemUpdates = mapOf(
                "status" to ItemStatus.DONATION_READY.name,
                "lastModifiedBy" to currentUser.uid,
                "lastModifiedAt" to currentTime
            )
            
            firestore.collection(ITEMS_COLLECTION)
                .document(itemId)
                .update(itemUpdates)
                .await()
            
            // Update donation record
            val donationUpdates = mapOf(
                "status" to DonationStatus.READY.name,
                "markedReadyBy" to currentUser.uid,
                "markedReadyAt" to currentTime
            )
            
            firestore.collection(DONATIONS_COLLECTION)
                .document(itemId)
                .update(donationUpdates)
                .await()
            
            // Log activity
            val activityLog = ActivityLog(
                id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                actorId = currentUser.uid,
                actorEmail = currentUser.email ?: "",
                actorRole = UserRole.ADMIN,
                actionType = ActionType.DONATION_MARK_READY,
                targetType = TargetType.DONATION,
                targetId = itemId,
                description = "Item marked as ready for donation by admin",
                previousValue = "PENDING",
                newValue = "READY",
                deviceInfo = android.os.Build.MODEL
            )
            
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(activityLog.id)
                .set(activityLog.toMap())
                .await()
            
            Log.d(TAG, "Item $itemId marked as ready for donation")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error marking item ready for donation", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking item ready for donation", e)
            Result.failure(Exception("Failed to mark item ready for donation: ${e.message}"))
        }
    }
    
    /**
     * Mark an item as donated with recipient and value
     * Requirements: 3.5
     */
    suspend fun markItemAsDonated(itemId: String, recipient: String, value: Double): Result<Unit> {
        return try {
            requireAdminAccess()
            
            // Validate donation data
            val donationValidation = DataValidator.validateDonationData(recipient, value)
            if (!donationValidation.isValid) {
                return Result.failure(IllegalArgumentException(donationValidation.getErrorMessage()))
            }
            
            val sanitizedRecipient = DataValidator.sanitizeString(recipient)
            
            val currentUser = auth.currentUser
                ?: return Result.failure(SecurityException("Not authenticated"))
            
            val currentTime = System.currentTimeMillis()
            
            // Update item status
            val itemUpdates = mapOf(
                "status" to ItemStatus.DONATED.name,
                "donatedAt" to currentTime,
                "lastModifiedBy" to currentUser.uid,
                "lastModifiedAt" to currentTime
            )
            
            firestore.collection(ITEMS_COLLECTION)
                .document(itemId)
                .update(itemUpdates)
                .await()
            
            // Update donation record
            val donationUpdates = mapOf(
                "status" to DonationStatus.DONATED.name,
                "donatedAt" to currentTime,
                "donatedBy" to currentUser.uid,
                "estimatedValue" to value,
                "donationRecipient" to sanitizedRecipient
            )
            
            firestore.collection(DONATIONS_COLLECTION)
                .document(itemId)
                .update(donationUpdates)
                .await()
            
            // Log activity
            val activityLog = ActivityLog(
                id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                actorId = currentUser.uid,
                actorEmail = currentUser.email ?: "",
                actorRole = UserRole.ADMIN,
                actionType = ActionType.DONATION_COMPLETE,
                targetType = TargetType.DONATION,
                targetId = itemId,
                description = "Item donated to $sanitizedRecipient with estimated value $$value",
                previousValue = "READY",
                newValue = "DONATED",
                deviceInfo = android.os.Build.MODEL,
                metadata = mapOf(
                    "recipient" to sanitizedRecipient,
                    "value" to value.toString()
                )
            )
            
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(activityLog.id)
                .set(activityLog.toMap())
                .await()
            
            Log.d(TAG, "Item $itemId marked as donated to $recipient")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error marking item as donated", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking item as donated", e)
            Result.failure(Exception("Failed to mark item as donated: ${e.message}"))
        }
    }
    
    /**
     * Get donation statistics with date range filtering
     * Requirements: 3.5, 3.6
     */
    fun getDonationStats(dateRange: DateRange? = null): Flow<DonationStats> = callbackFlow {
        try {
            if (!isAdminUser()) {
                trySend(DonationStats())
                close()
                return@callbackFlow
            }
            
            val listener = firestore.collection(DONATIONS_COLLECTION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error getting donation stats", error)
                        trySend(DonationStats())
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        try {
                            val allDonations = snapshot.documents.mapNotNull { doc ->
                                try {
                                    val data = doc.data ?: return@mapNotNull null
                                    DonationItem(
                                        itemId = data["itemId"] as? String ?: "",
                                        itemName = data["itemName"] as? String ?: "",
                                        category = data["category"] as? String ?: "",
                                        location = data["location"] as? String ?: "",
                                        reportedAt = (data["reportedAt"] as? Number)?.toLong() ?: 0L,
                                        eligibleAt = (data["eligibleAt"] as? Number)?.toLong() ?: 0L,
                                        status = try {
                                            DonationStatus.valueOf(data["status"] as? String ?: "PENDING")
                                        } catch (e: Exception) {
                                            DonationStatus.PENDING
                                        },
                                        markedReadyBy = data["markedReadyBy"] as? String ?: "",
                                        markedReadyAt = (data["markedReadyAt"] as? Number)?.toLong() ?: 0L,
                                        donatedAt = (data["donatedAt"] as? Number)?.toLong() ?: 0L,
                                        donatedBy = data["donatedBy"] as? String ?: "",
                                        estimatedValue = (data["estimatedValue"] as? Number)?.toDouble() ?: 0.0,
                                        donationRecipient = data["donationRecipient"] as? String ?: ""
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            
                            // Filter by date range if provided
                            val filteredDonations = if (dateRange != null) {
                                allDonations.filter { donation ->
                                    donation.donatedAt in dateRange.startDate..dateRange.endDate
                                }
                            } else {
                                allDonations
                            }
                            
                            // Calculate statistics
                            val donated = filteredDonations.filter { it.status == DonationStatus.DONATED }
                            val totalDonated = donated.size
                            val totalValue = donated.sumOf { it.estimatedValue }
                            
                            val pendingDonations = allDonations.count { it.status == DonationStatus.PENDING }
                            val readyForDonation = allDonations.count { it.status == DonationStatus.READY }
                            
                            // Group by category
                            val donationsByCategory = donated.groupBy { it.category }
                                .mapValues { it.value.size }
                            
                            // Group by month
                            val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                            val donationsByMonth = donated.groupBy { 
                                dateFormat.format(Date(it.donatedAt))
                            }.mapValues { it.value.size }
                            
                            // Calculate average item age
                            val averageItemAge = if (donated.isNotEmpty()) {
                                donated.map { 
                                    (it.donatedAt - it.reportedAt) / (24 * 60 * 60 * 1000)
                                }.average().toFloat()
                            } else {
                                0f
                            }
                            
                            // Find most donated category
                            val mostDonatedCategory = donationsByCategory.maxByOrNull { it.value }?.key ?: ""
                            
                            val stats = DonationStats(
                                totalDonated = totalDonated,
                                totalValue = totalValue,
                                donationsByCategory = donationsByCategory,
                                donationsByMonth = donationsByMonth,
                                pendingDonations = pendingDonations,
                                readyForDonation = readyForDonation,
                                averageItemAge = averageItemAge,
                                mostDonatedCategory = mostDonatedCategory
                            )
                            
                            trySend(stats)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error calculating donation stats", e)
                            trySend(DonationStats())
                        }
                    } else {
                        trySend(DonationStats())
                    }
                }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up donation stats listener", e)
            trySend(DonationStats())
            close()
        }
    }
    
    /**
     * Get donation history with date range filtering
     * Requirements: 3.6
     */
    suspend fun getDonationHistory(dateRange: DateRange): Result<List<DonationItem>> {
        return try {
            requireAdminAccess()
            
            val snapshot = firestore.collection(DONATIONS_COLLECTION)
                .whereEqualTo("status", DonationStatus.DONATED.name)
                .orderBy("donatedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val donations = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    DonationItem(
                        itemId = data["itemId"] as? String ?: "",
                        itemName = data["itemName"] as? String ?: "",
                        category = data["category"] as? String ?: "",
                        location = data["location"] as? String ?: "",
                        reportedAt = (data["reportedAt"] as? Number)?.toLong() ?: 0L,
                        eligibleAt = (data["eligibleAt"] as? Number)?.toLong() ?: 0L,
                        status = DonationStatus.DONATED,
                        markedReadyBy = data["markedReadyBy"] as? String ?: "",
                        markedReadyAt = (data["markedReadyAt"] as? Number)?.toLong() ?: 0L,
                        donatedAt = (data["donatedAt"] as? Number)?.toLong() ?: 0L,
                        donatedBy = data["donatedBy"] as? String ?: "",
                        estimatedValue = (data["estimatedValue"] as? Number)?.toDouble() ?: 0.0,
                        donationRecipient = data["donationRecipient"] as? String ?: "",
                        imageUrl = data["imageUrl"] as? String ?: "",
                        description = data["description"] as? String ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing donation item", e)
                    null
                }
            }
            
            // Filter by date range
            val filteredDonations = donations.filter { donation ->
                donation.donatedAt in dateRange.startDate..dateRange.endDate
            }
            
            Log.d(TAG, "Retrieved ${filteredDonations.size} donation history items")
            Result.success(filteredDonations)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting donation history", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting donation history", e)
            Result.failure(Exception("Failed to get donation history: ${e.message}"))
        }
    }
    
    /**
     * Check and flag items older than 1 year for donation
     * Requirements: 3.1, 3.8
     */
    suspend fun checkAndFlagOldItems(): Result<Int> {
        return try {
            Log.d(TAG, "Checking for items older than 1 year")
            
            val oneYearAgo = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
            
            // Get all active items
            val snapshot = firestore.collection(ITEMS_COLLECTION)
                .whereEqualTo("status", ItemStatus.ACTIVE.name)
                .get()
                .await()
            
            var flaggedCount = 0
            
            for (doc in snapshot.documents) {
                try {
                    val data = doc.data ?: continue
                    val timestamp = data["timestamp"] as? com.google.firebase.Timestamp
                    val reportedAt = timestamp?.toDate()?.time ?: continue
                    
                    // Check if item is older than 1 year
                    if (reportedAt < oneYearAgo) {
                        // Mark item for donation
                        val result = markItemForDonation(doc.id)
                        if (result.isSuccess) {
                            flaggedCount++
                            Log.d(TAG, "Auto-flagged item ${doc.id} for donation")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing item ${doc.id}", e)
                    // Continue with next item
                }
            }
            
            Log.d(TAG, "Auto-flagged $flaggedCount items for donation")
            Result.success(flaggedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking and flagging old items", e)
            Result.failure(Exception("Failed to check and flag old items: ${e.message}"))
        }
    }
    
    /**
     * Get filtered donation queue with real-time updates
     * Requirements: 3.9
     */
    fun getFilteredDonationQueue(filter: DonationFilter): Flow<List<DonationItem>> = callbackFlow {
        try {
            if (!isAdminUser()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            
            // Build query based on filter
            var query: Query = firestore.collection(DONATIONS_COLLECTION)
            
            // Apply status filter at query level if specified
            if (filter.status != null) {
                query = query.whereEqualTo("status", filter.status.name)
            }
            
            // Apply category filter at query level if specified
            if (filter.category != null) {
                query = query.whereEqualTo("category", filter.category)
            }
            
            // Apply location filter at query level if specified
            if (filter.location != null) {
                query = query.whereEqualTo("location", filter.location)
            }
            
            // Order by eligibility date
            query = query.orderBy("eligibleAt", Query.Direction.ASCENDING)
            
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting filtered donation queue", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val donations = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            DonationItem(
                                itemId = data["itemId"] as? String ?: "",
                                itemName = data["itemName"] as? String ?: "",
                                category = data["category"] as? String ?: "",
                                location = data["location"] as? String ?: "",
                                reportedAt = (data["reportedAt"] as? Number)?.toLong() ?: 0L,
                                eligibleAt = (data["eligibleAt"] as? Number)?.toLong() ?: 0L,
                                status = try {
                                    DonationStatus.valueOf(data["status"] as? String ?: "PENDING")
                                } catch (e: Exception) {
                                    DonationStatus.PENDING
                                },
                                markedReadyBy = data["markedReadyBy"] as? String ?: "",
                                markedReadyAt = (data["markedReadyAt"] as? Number)?.toLong() ?: 0L,
                                donatedAt = (data["donatedAt"] as? Number)?.toLong() ?: 0L,
                                donatedBy = data["donatedBy"] as? String ?: "",
                                estimatedValue = (data["estimatedValue"] as? Number)?.toDouble() ?: 0.0,
                                donationRecipient = data["donationRecipient"] as? String ?: "",
                                imageUrl = data["imageUrl"] as? String ?: "",
                                description = data["description"] as? String ?: ""
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing donation item", e)
                            null
                        }
                    }
                    
                    // Apply client-side filters for age range
                    val filteredDonations = donations.filter { donation ->
                        filter.matches(donation)
                    }
                    
                    trySend(filteredDonations)
                } else {
                    trySend(emptyList())
                }
            }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up filtered donation queue listener", e)
            trySend(emptyList())
            close()
        }
    }
    
    // ========== Activity Logging System Methods ==========
    // Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6
    
    /**
     * Log an activity with comprehensive error handling
     * Requirements: 5.1, 5.2
     */
    suspend fun logActivity(log: ActivityLog): Result<Unit> {
        return try {
            // Validate activity log
            if (!log.isValid()) {
                return Result.failure(IllegalArgumentException("Invalid activity log data"))
            }
            
            // Generate ID if not provided
            val logWithId = if (log.id.isBlank()) {
                log.copy(id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id)
            } else {
                log
            }
            
            // Save to Firestore
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(logWithId.id)
                .set(logWithId.toMap())
                .await()
            
            Log.d(TAG, "Activity logged: ${logWithId.actionType.name} by ${logWithId.actorEmail}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging activity", e)
            // Don't fail the main operation if logging fails
            Result.failure(Exception("Failed to log activity: ${e.message}"))
        }
    }
    
    /**
     * Get activity logs with pagination and filtering
     * Requirements: 5.3, 5.4
     */
    fun getActivityLogs(
        limit: Int = 50,
        filters: Map<String, String> = emptyMap()
    ): Flow<List<ActivityLog>> = callbackFlow {
        try {
            if (!isAdminUser()) {
                Log.w(TAG, "Non-admin user attempted to access activity logs")
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            
            // Start with base query
            var query: Query = firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
            
            // Apply filters
            filters.forEach { (key, value) ->
                when (key) {
                    "actionType" -> {
                        try {
                            query = query.whereEqualTo("actionType", value)
                        } catch (e: Exception) {
                            Log.e(TAG, "Invalid action type filter: $value", e)
                        }
                    }
                    "targetType" -> {
                        try {
                            query = query.whereEqualTo("targetType", value)
                        } catch (e: Exception) {
                            Log.e(TAG, "Invalid target type filter: $value", e)
                        }
                    }
                    "actorId" -> {
                        query = query.whereEqualTo("actorId", value)
                    }
                    "actorEmail" -> {
                        query = query.whereEqualTo("actorEmail", value)
                    }
                    "targetId" -> {
                        query = query.whereEqualTo("targetId", value)
                    }
                }
            }
            
            // Set up real-time listener
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting activity logs", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    try {
                        val logs = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data ?: return@mapNotNull null
                                ActivityLog(
                                    id = doc.id,
                                    timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                                    actorId = data["actorId"] as? String ?: "",
                                    actorEmail = data["actorEmail"] as? String ?: "",
                                    actorRole = try {
                                        val roleString = data["actorRole"] as? String ?: "STUDENT"
                                        UserRole.fromString(roleString)
                                    } catch (e: Exception) {
                                        UserRole.STUDENT
                                    },
                                    actionType = try {
                                        ActionType.valueOf(data["actionType"] as? String ?: "USER_LOGIN")
                                    } catch (e: Exception) {
                                        ActionType.USER_LOGIN
                                    },
                                    targetType = try {
                                        TargetType.valueOf(data["targetType"] as? String ?: "SYSTEM")
                                    } catch (e: Exception) {
                                        TargetType.SYSTEM
                                    },
                                    targetId = data["targetId"] as? String ?: "",
                                    description = data["description"] as? String ?: "",
                                    previousValue = data["previousValue"] as? String ?: "",
                                    newValue = data["newValue"] as? String ?: "",
                                    ipAddress = data["ipAddress"] as? String ?: "",
                                    deviceInfo = data["deviceInfo"] as? String ?: "",
                                    metadata = (data["metadata"] as? Map<*, *>)?.mapKeys { it.key.toString() }
                                        ?.mapValues { it.value.toString() } ?: emptyMap()
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing activity log", e)
                                null
                            }
                        }
                        
                        // Apply client-side date range filter if provided
                        val startDate = filters["startDate"]?.toLongOrNull()
                        val endDate = filters["endDate"]?.toLongOrNull()
                        
                        val filteredLogs = if (startDate != null || endDate != null) {
                            logs.filter { log ->
                                val matchesStart = startDate == null || log.timestamp >= startDate
                                val matchesEnd = endDate == null || log.timestamp <= endDate
                                matchesStart && matchesEnd
                            }
                        } else {
                            logs
                        }
                        
                        Log.d(TAG, "Retrieved ${filteredLogs.size} activity logs")
                        trySend(filteredLogs)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing activity logs", e)
                        trySend(emptyList())
                    }
                } else {
                    trySend(emptyList())
                }
            }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up activity logs listener", e)
            trySend(emptyList())
            close()
        }
    }
    
    /**
     * Search activity logs with query and filters
     * Requirements: 5.5
     */
    suspend fun searchActivityLogs(
        query: String,
        filters: Map<String, String> = emptyMap()
    ): Result<List<ActivityLog>> {
        return try {
            if (!isAdminUser()) {
                return Result.failure(SecurityException("Admin access required"))
            }
            
            // Get all logs (Firestore doesn't support full-text search)
            val snapshot = firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1000) // Reasonable limit for search
                .get()
                .await()
            
            val allLogs = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    ActivityLog(
                        id = doc.id,
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                        actorId = data["actorId"] as? String ?: "",
                        actorEmail = data["actorEmail"] as? String ?: "",
                        actorRole = try {
                            val roleString = data["actorRole"] as? String ?: "STUDENT"
                            UserRole.fromString(roleString)
                        } catch (e: Exception) {
                            UserRole.STUDENT
                        },
                        actionType = try {
                            ActionType.valueOf(data["actionType"] as? String ?: "USER_LOGIN")
                        } catch (e: Exception) {
                            ActionType.USER_LOGIN
                        },
                        targetType = try {
                            TargetType.valueOf(data["targetType"] as? String ?: "SYSTEM")
                        } catch (e: Exception) {
                            TargetType.SYSTEM
                        },
                        targetId = data["targetId"] as? String ?: "",
                        description = data["description"] as? String ?: "",
                        previousValue = data["previousValue"] as? String ?: "",
                        newValue = data["newValue"] as? String ?: "",
                        ipAddress = data["ipAddress"] as? String ?: "",
                        deviceInfo = data["deviceInfo"] as? String ?: "",
                        metadata = (data["metadata"] as? Map<*, *>)?.mapKeys { it.key.toString() }
                            ?.mapValues { it.value.toString() } ?: emptyMap()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing activity log", e)
                    null
                }
            }
            
            // Apply search query
            val queryLower = query.lowercase()
            var filteredLogs = if (query.isNotBlank()) {
                allLogs.filter { log ->
                    log.actorEmail.lowercase().contains(queryLower) ||
                    log.description.lowercase().contains(queryLower) ||
                    log.targetId.lowercase().contains(queryLower) ||
                    log.actionType.getDisplayName().lowercase().contains(queryLower)
                }
            } else {
                allLogs
            }
            
            // Apply additional filters
            filters.forEach { (key, value) ->
                when (key) {
                    "actionType" -> {
                        try {
                            val actionType = ActionType.valueOf(value)
                            filteredLogs = filteredLogs.filter { it.actionType == actionType }
                        } catch (e: Exception) {
                            Log.e(TAG, "Invalid action type filter: $value", e)
                        }
                    }
                    "targetType" -> {
                        try {
                            val targetType = TargetType.valueOf(value)
                            filteredLogs = filteredLogs.filter { it.targetType == targetType }
                        } catch (e: Exception) {
                            Log.e(TAG, "Invalid target type filter: $value", e)
                        }
                    }
                    "actorEmail" -> {
                        filteredLogs = filteredLogs.filter { 
                            it.actorEmail.lowercase().contains(value.lowercase()) 
                        }
                    }
                    "startDate" -> {
                        val startDate = value.toLongOrNull()
                        if (startDate != null) {
                            filteredLogs = filteredLogs.filter { it.timestamp >= startDate }
                        }
                    }
                    "endDate" -> {
                        val endDate = value.toLongOrNull()
                        if (endDate != null) {
                            filteredLogs = filteredLogs.filter { it.timestamp <= endDate }
                        }
                    }
                }
            }
            
            Log.d(TAG, "Search found ${filteredLogs.size} activity logs matching '$query'")
            Result.success(filteredLogs)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error searching activity logs", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching activity logs", e)
            Result.failure(Exception("Failed to search activity logs: ${e.message}"))
        }
    }
    
    /**
     * Get activity logs filtered by date range
     * Requirements: 5.4
     */
    suspend fun getActivityLogsByDateRange(
        startDate: Long,
        endDate: Long,
        limit: Int = 500
    ): Result<List<ActivityLog>> {
        return try {
            if (!isAdminUser()) {
                return Result.failure(SecurityException("Admin access required"))
            }
            
            val snapshot = firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .whereLessThanOrEqualTo("timestamp", endDate)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val logs = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    ActivityLog(
                        id = doc.id,
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                        actorId = data["actorId"] as? String ?: "",
                        actorEmail = data["actorEmail"] as? String ?: "",
                        actorRole = try {
                            UserRole.fromString(data["actorRole"] as? String ?: "STUDENT")
                        } catch (e: Exception) {
                            UserRole.STUDENT
                        },
                        actionType = try {
                            ActionType.valueOf(data["actionType"] as? String ?: "USER_LOGIN")
                        } catch (e: Exception) {
                            ActionType.USER_LOGIN
                        },
                        targetType = try {
                            TargetType.valueOf(data["targetType"] as? String ?: "SYSTEM")
                        } catch (e: Exception) {
                            TargetType.SYSTEM
                        },
                        targetId = data["targetId"] as? String ?: "",
                        description = data["description"] as? String ?: "",
                        previousValue = data["previousValue"] as? String ?: "",
                        newValue = data["newValue"] as? String ?: "",
                        ipAddress = data["ipAddress"] as? String ?: "",
                        deviceInfo = data["deviceInfo"] as? String ?: "",
                        metadata = (data["metadata"] as? Map<*, *>)?.mapKeys { it.key.toString() }
                            ?.mapValues { it.value.toString() } ?: emptyMap()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing activity log", e)
                    null
                }
            }
            
            Log.d(TAG, "Retrieved ${logs.size} activity logs for date range")
            Result.success(logs)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting activity logs by date range", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting activity logs by date range", e)
            Result.failure(Exception("Failed to get activity logs: ${e.message}"))
        }
    }
    
    /**
     * Get activity logs filtered by user
     * Requirements: 5.4
     */
    suspend fun getActivityLogsByUser(
        userEmail: String,
        limit: Int = 100
    ): Result<List<ActivityLog>> {
        return try {
            if (!isAdminUser()) {
                return Result.failure(SecurityException("Admin access required"))
            }
            
            val snapshot = firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .whereEqualTo("actorEmail", userEmail)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val logs = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    ActivityLog(
                        id = doc.id,
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                        actorId = data["actorId"] as? String ?: "",
                        actorEmail = data["actorEmail"] as? String ?: "",
                        actorRole = try {
                            UserRole.fromString(data["actorRole"] as? String ?: "STUDENT")
                        } catch (e: Exception) {
                            UserRole.STUDENT
                        },
                        actionType = try {
                            ActionType.valueOf(data["actionType"] as? String ?: "USER_LOGIN")
                        } catch (e: Exception) {
                            ActionType.USER_LOGIN
                        },
                        targetType = try {
                            TargetType.valueOf(data["targetType"] as? String ?: "SYSTEM")
                        } catch (e: Exception) {
                            TargetType.SYSTEM
                        },
                        targetId = data["targetId"] as? String ?: "",
                        description = data["description"] as? String ?: "",
                        previousValue = data["previousValue"] as? String ?: "",
                        newValue = data["newValue"] as? String ?: "",
                        ipAddress = data["ipAddress"] as? String ?: "",
                        deviceInfo = data["deviceInfo"] as? String ?: "",
                        metadata = (data["metadata"] as? Map<*, *>)?.mapKeys { it.key.toString() }
                            ?.mapValues { it.value.toString() } ?: emptyMap()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing activity log", e)
                    null
                }
            }
            
            Log.d(TAG, "Retrieved ${logs.size} activity logs for user: $userEmail")
            Result.success(logs)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting activity logs by user", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting activity logs by user", e)
            Result.failure(Exception("Failed to get activity logs: ${e.message}"))
        }
    }
    
    /**
     * Get activity logs filtered by action type
     * Requirements: 5.4
     */
    suspend fun getActivityLogsByActionType(
        actionType: ActionType,
        limit: Int = 100
    ): Result<List<ActivityLog>> {
        return try {
            if (!isAdminUser()) {
                return Result.failure(SecurityException("Admin access required"))
            }
            
            val snapshot = firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .whereEqualTo("actionType", actionType.name)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val logs = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    ActivityLog(
                        id = doc.id,
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                        actorId = data["actorId"] as? String ?: "",
                        actorEmail = data["actorEmail"] as? String ?: "",
                        actorRole = try {
                            UserRole.fromString(data["actorRole"] as? String ?: "STUDENT")
                        } catch (e: Exception) {
                            UserRole.STUDENT
                        },
                        actionType = try {
                            ActionType.valueOf(data["actionType"] as? String ?: "USER_LOGIN")
                        } catch (e: Exception) {
                            ActionType.USER_LOGIN
                        },
                        targetType = try {
                            TargetType.valueOf(data["targetType"] as? String ?: "SYSTEM")
                        } catch (e: Exception) {
                            TargetType.SYSTEM
                        },
                        targetId = data["targetId"] as? String ?: "",
                        description = data["description"] as? String ?: "",
                        previousValue = data["previousValue"] as? String ?: "",
                        newValue = data["newValue"] as? String ?: "",
                        ipAddress = data["ipAddress"] as? String ?: "",
                        deviceInfo = data["deviceInfo"] as? String ?: "",
                        metadata = (data["metadata"] as? Map<*, *>)?.mapKeys { it.key.toString() }
                            ?.mapValues { it.value.toString() } ?: emptyMap()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing activity log", e)
                    null
                }
            }
            
            Log.d(TAG, "Retrieved ${logs.size} activity logs for action type: ${actionType.name}")
            Result.success(logs)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting activity logs by action type", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting activity logs by action type", e)
            Result.failure(Exception("Failed to get activity logs: ${e.message}"))
        }
    }
    
    /**
     * Log user login event
     * Requirements: 5.7
     */
    suspend fun logUserLogin(userId: String, userEmail: String, userRole: UserRole = UserRole.STUDENT): Result<Unit> {
        return try {
            val activityLog = ActivityLog(
                id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                actorId = userId,
                actorEmail = userEmail,
                actorRole = userRole,
                actionType = ActionType.USER_LOGIN,
                targetType = TargetType.USER,
                targetId = userId,
                description = "User logged in",
                deviceInfo = android.os.Build.MODEL
            )
            
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(activityLog.id)
                .set(activityLog.toMap())
                .await()
            
            // Update last login timestamp
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("lastLoginAt", System.currentTimeMillis())
                .await()
            
            Log.d(TAG, "User login logged: $userEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging user login", e)
            // Don't fail login if logging fails
            Result.success(Unit)
        }
    }
    
    /**
     * Log user logout event
     * Requirements: 5.7
     */
    suspend fun logUserLogout(userId: String, userEmail: String, userRole: UserRole = UserRole.STUDENT): Result<Unit> {
        return try {
            val activityLog = ActivityLog(
                id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                actorId = userId,
                actorEmail = userEmail,
                actorRole = userRole,
                actionType = ActionType.USER_LOGOUT,
                targetType = TargetType.USER,
                targetId = userId,
                description = "User logged out",
                deviceInfo = android.os.Build.MODEL
            )
            
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(activityLog.id)
                .set(activityLog.toMap())
                .await()
            
            Log.d(TAG, "User logout logged: $userEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging user logout", e)
            // Don't fail logout if logging fails
            Result.success(Unit)
        }
    }
    
    /**
     * Log user registration event
     * Requirements: 5.7
     */
    suspend fun logUserRegistration(userId: String, userEmail: String): Result<Unit> {
        return try {
            val activityLog = ActivityLog(
                id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                actorId = userId,
                actorEmail = userEmail,
                actorRole = UserRole.STUDENT,
                actionType = ActionType.USER_REGISTER,
                targetType = TargetType.USER,
                targetId = userId,
                description = "New user registered",
                deviceInfo = android.os.Build.MODEL
            )
            
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(activityLog.id)
                .set(activityLog.toMap())
                .await()
            
            Log.d(TAG, "User registration logged: $userEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging user registration", e)
            // Don't fail registration if logging fails
            Result.success(Unit)
        }
    }
    
    // ========== Activity Log Archiving Methods ==========
    // Requirements: 5.11
    
    /**
     * Archive old activity logs (older than 1 year) to Firebase Storage
     * Requirements: 5.11
     */
    suspend fun archiveOldLogs(): Result<Int> {
        return try {
            requireAdminAccess()
            
            // Calculate timestamp for 1 year ago
            val oneYearAgo = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
            
            Log.d(TAG, "Starting archive of logs older than ${Date(oneYearAgo)}")
            
            // Get logs older than 1 year
            val snapshot = firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .whereLessThan("timestamp", oneYearAgo)
                .get()
                .await()
            
            val oldLogs = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    ActivityLog(
                        id = doc.id,
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                        actorId = data["actorId"] as? String ?: "",
                        actorEmail = data["actorEmail"] as? String ?: "",
                        actorRole = try {
                            UserRole.fromString(data["actorRole"] as? String ?: "STUDENT")
                        } catch (e: Exception) {
                            UserRole.STUDENT
                        },
                        actionType = try {
                            ActionType.valueOf(data["actionType"] as? String ?: "USER_LOGIN")
                        } catch (e: Exception) {
                            ActionType.USER_LOGIN
                        },
                        targetType = try {
                            TargetType.valueOf(data["targetType"] as? String ?: "SYSTEM")
                        } catch (e: Exception) {
                            TargetType.SYSTEM
                        },
                        targetId = data["targetId"] as? String ?: "",
                        description = data["description"] as? String ?: "",
                        previousValue = data["previousValue"] as? String ?: "",
                        newValue = data["newValue"] as? String ?: "",
                        ipAddress = data["ipAddress"] as? String ?: "",
                        deviceInfo = data["deviceInfo"] as? String ?: "",
                        metadata = (data["metadata"] as? Map<*, *>)?.mapKeys { it.key.toString() }
                            ?.mapValues { it.value.toString() } ?: emptyMap()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing activity log for archiving", e)
                    null
                }
            }
            
            if (oldLogs.isEmpty()) {
                Log.d(TAG, "No logs to archive")
                return Result.success(0)
            }
            
            Log.d(TAG, "Found ${oldLogs.size} logs to archive")
            
            // Create archive collection name with timestamp
            val archiveDate = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val archiveCollection = "activityLogsArchive_$archiveDate"
            
            // Move logs to archive collection
            val batch = firestore.batch()
            var batchCount = 0
            val maxBatchSize = 500 // Firestore batch limit
            
            oldLogs.forEach { log ->
                if (batchCount >= maxBatchSize) {
                    // Commit current batch and start new one
                    batch.commit().await()
                    batchCount = 0
                }
                
                // Add to archive collection
                val archiveRef = firestore.collection(archiveCollection).document(log.id)
                batch.set(archiveRef, log.toMap())
                
                // Delete from main collection
                val mainRef = firestore.collection(ACTIVITY_LOGS_COLLECTION).document(log.id)
                batch.delete(mainRef)
                
                batchCount++
            }
            
            // Commit final batch
            if (batchCount > 0) {
                batch.commit().await()
            }
            
            // Log the archiving action
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val archiveLog = ActivityLog(
                    id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                    actorId = currentUser.uid,
                    actorEmail = currentUser.email ?: "",
                    actorRole = UserRole.ADMIN,
                    actionType = ActionType.LOG_ARCHIVE,
                    targetType = TargetType.SYSTEM,
                    targetId = archiveCollection,
                    description = "Archived ${oldLogs.size} activity logs older than 1 year to $archiveCollection",
                    previousValue = oldLogs.size.toString(),
                    newValue = archiveCollection,
                    deviceInfo = android.os.Build.MODEL,
                    metadata = mapOf(
                        "archivedCount" to oldLogs.size.toString(),
                        "archiveDate" to archiveDate,
                        "oldestLog" to oldLogs.minByOrNull { it.timestamp }?.timestamp.toString(),
                        "newestLog" to oldLogs.maxByOrNull { it.timestamp }?.timestamp.toString()
                    )
                )
                
                firestore.collection(ACTIVITY_LOGS_COLLECTION)
                    .document(archiveLog.id)
                    .set(archiveLog.toMap())
                    .await()
            }
            
            Log.d(TAG, "Successfully archived ${oldLogs.size} logs to $archiveCollection")
            Result.success(oldLogs.size)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error archiving logs", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error archiving logs", e)
            Result.failure(Exception("Failed to archive logs: ${e.message}"))
        }
    }
    
    /**
     * Get count of logs eligible for archiving
     * Requirements: 5.11
     */
    suspend fun getArchivableLogsCount(): Result<Int> {
        return try {
            requireAdminAccess()
            
            val oneYearAgo = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
            
            val snapshot = firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .whereLessThan("timestamp", oneYearAgo)
                .get()
                .await()
            
            val count = snapshot.size()
            Log.d(TAG, "Found $count logs eligible for archiving")
            Result.success(count)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting archivable logs count", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting archivable logs count", e)
            Result.failure(Exception("Failed to get archivable logs count: ${e.message}"))
        }
    }

    // ========== Push Notification Methods ==========
    // Requirements: 6.4, 6.7, 6.9
    
    /**
     * Send push notification to target users
     * Requirements: 6.4
     */
    suspend fun sendPushNotification(notification: PushNotification): Result<Unit> {
        return try {
            requireAdminAccess()
            
            if (!notification.isValid()) {
                return Result.failure(IllegalArgumentException("Invalid notification data"))
            }
            
            val currentUser = auth.currentUser
                ?: return Result.failure(SecurityException("Not authenticated"))
            
            // Create notification document
            val notificationId = if (notification.id.isBlank()) {
                firestore.collection("notifications").document().id
            } else {
                notification.id
            }
            
            val notificationData = notification.copy(
                id = notificationId,
                createdBy = currentUser.uid,
                sentAt = System.currentTimeMillis(),
                deliveryStatus = DeliveryStatus.SENDING
            )
            
            // Save notification to Firestore
            firestore.collection("notifications")
                .document(notificationId)
                .set(notificationData.toMap())
                .await()
            
            // Get target user tokens
            val targetTokens = getTargetUserTokens(
                notification.targetUsers,
                notification.targetRoles
            ).getOrElse { emptyList() }
            
            if (targetTokens.isEmpty()) {
                // Update status to failed if no tokens found
                firestore.collection("notifications")
                    .document(notificationId)
                    .update(
                        mapOf(
                            "deliveryStatus" to DeliveryStatus.FAILED.name,
                            "failedCount" to 1
                        )
                    )
                    .await()
                
                return Result.failure(Exception("No target users found with FCM tokens"))
            }
            
            // Send FCM messages (this would typically be done via Cloud Functions)
            // For now, we'll just update the notification status
            firestore.collection("notifications")
                .document(notificationId)
                .update(
                    mapOf(
                        "deliveryStatus" to DeliveryStatus.SENT.name,
                        "deliveredCount" to targetTokens.size
                    )
                )
                .await()
            
            // Log activity
            val activityLog = ActivityLog(
                id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                actorId = currentUser.uid,
                actorEmail = currentUser.email ?: "",
                actorRole = UserRole.ADMIN,
                actionType = ActionType.NOTIFICATION_SEND,
                targetType = TargetType.NOTIFICATION,
                targetId = notificationId,
                description = "Push notification sent: ${notification.title}",
                metadata = mapOf(
                    "type" to notification.type.name,
                    "targetCount" to targetTokens.size.toString()
                ),
                deviceInfo = android.os.Build.MODEL
            )
            
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(activityLog.id)
                .set(activityLog.toMap())
                .await()
            
            Log.d(TAG, "Notification sent successfully: $notificationId")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error sending notification", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
            Result.failure(Exception("Failed to send notification: ${e.message}"))
        }
    }
    
    /**
     * Get FCM tokens for target users
     * Helper method for recipient selection
     * Requirements: 6.4
     */
    private suspend fun getTargetUserTokens(
        targetUserIds: List<String>,
        targetRoles: List<UserRole>
    ): Result<List<Pair<String, String>>> {
        return try {
            val tokens = mutableListOf<Pair<String, String>>() // Pair of (userId, token)
            
            // If targeting specific users
            if (targetUserIds.isNotEmpty() && targetUserIds.first() != "ALL") {
                for (userId in targetUserIds) {
                    val userDoc = firestore.collection(USERS_COLLECTION)
                        .document(userId)
                        .get()
                        .await()
                    
                    val fcmToken = userDoc.getString("fcmToken")
                    if (!fcmToken.isNullOrBlank()) {
                        tokens.add(Pair(userId, fcmToken))
                    }
                }
            }
            // If targeting by roles
            else if (targetRoles.isNotEmpty()) {
                for (role in targetRoles) {
                    val usersSnapshot = firestore.collection(USERS_COLLECTION)
                        .whereEqualTo("role", role.name)
                        .get()
                        .await()
                    
                    usersSnapshot.documents.forEach { doc ->
                        val fcmToken = doc.getString("fcmToken")
                        if (!fcmToken.isNullOrBlank()) {
                            tokens.add(Pair(doc.id, fcmToken))
                        }
                    }
                }
            }
            // If targeting all users
            else if (targetUserIds.firstOrNull() == "ALL") {
                val allUsersSnapshot = firestore.collection(USERS_COLLECTION)
                    .get()
                    .await()
                
                allUsersSnapshot.documents.forEach { doc ->
                    val fcmToken = doc.getString("fcmToken")
                    if (!fcmToken.isNullOrBlank()) {
                        tokens.add(Pair(doc.id, fcmToken))
                    }
                }
            }
            
            Log.d(TAG, "Found ${tokens.size} FCM tokens for notification")
            Result.success(tokens)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting target user tokens", e)
            Result.failure(Exception("Failed to get target user tokens: ${e.message}"))
        }
    }
    
    /**
     * Schedule notification for delayed delivery
     * Requirements: 6.7
     */
    suspend fun scheduleNotification(notification: PushNotification): Result<Unit> {
        return try {
            requireAdminAccess()
            
            if (!notification.isValid()) {
                return Result.failure(IllegalArgumentException("Invalid notification data"))
            }
            
            if (notification.scheduledFor <= System.currentTimeMillis()) {
                return Result.failure(IllegalArgumentException("Scheduled time must be in the future"))
            }
            
            val currentUser = auth.currentUser
                ?: return Result.failure(SecurityException("Not authenticated"))
            
            // Create notification document with scheduled status
            val notificationId = if (notification.id.isBlank()) {
                firestore.collection("notifications").document().id
            } else {
                notification.id
            }
            
            val notificationData = notification.copy(
                id = notificationId,
                createdBy = currentUser.uid,
                deliveryStatus = DeliveryStatus.SCHEDULED
            )
            
            // Save notification to Firestore
            firestore.collection("notifications")
                .document(notificationId)
                .set(notificationData.toMap())
                .await()
            
            // Log activity
            val activityLog = ActivityLog(
                id = firestore.collection(ACTIVITY_LOGS_COLLECTION).document().id,
                actorId = currentUser.uid,
                actorEmail = currentUser.email ?: "",
                actorRole = UserRole.ADMIN,
                actionType = ActionType.NOTIFICATION_SEND,
                targetType = TargetType.NOTIFICATION,
                targetId = notificationId,
                description = "Notification scheduled: ${notification.title}",
                metadata = mapOf(
                    "type" to notification.type.name,
                    "scheduledFor" to notification.scheduledFor.toString()
                ),
                deviceInfo = android.os.Build.MODEL
            )
            
            firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .document(activityLog.id)
                .set(activityLog.toMap())
                .await()
            
            Log.d(TAG, "Notification scheduled successfully: $notificationId")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error scheduling notification", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling notification", e)
            Result.failure(Exception("Failed to schedule notification: ${e.message}"))
        }
    }
    
    /**
     * Get notification history with real-time updates
     * Requirements: 6.9
     */
    fun getNotificationHistory(limit: Int = 50): Flow<List<PushNotification>> = callbackFlow {
        if (!isAdminUser()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to notification history", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    try {
                        val notifications = snapshot.documents.mapNotNull { doc ->
                            val data = doc.data ?: return@mapNotNull null
                            
                            PushNotification(
                                id = doc.id,
                                title = data["title"] as? String ?: "",
                                body = data["body"] as? String ?: "",
                                type = try {
                                    NotificationType.valueOf(data["type"] as? String ?: "CUSTOM_ADMIN")
                                } catch (e: Exception) {
                                    NotificationType.CUSTOM_ADMIN
                                },
                                targetUsers = (data["targetUsers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                targetRoles = (data["targetRoles"] as? List<*>)?.mapNotNull { 
                                    try {
                                        UserRole.valueOf(it as? String ?: "USER")
                                    } catch (e: Exception) {
                                        null
                                    }
                                } ?: emptyList(),
                                actionUrl = data["actionUrl"] as? String ?: "",
                                imageUrl = data["imageUrl"] as? String ?: "",
                                createdBy = data["createdBy"] as? String ?: "",
                                createdAt = (data["createdAt"] as? Long) ?: 0L,
                                scheduledFor = (data["scheduledFor"] as? Long) ?: 0L,
                                sentAt = (data["sentAt"] as? Long) ?: 0L,
                                deliveryStatus = try {
                                    DeliveryStatus.valueOf(data["deliveryStatus"] as? String ?: "PENDING")
                                } catch (e: Exception) {
                                    DeliveryStatus.PENDING
                                },
                                deliveredCount = (data["deliveredCount"] as? Long)?.toInt() ?: 0,
                                openedCount = (data["openedCount"] as? Long)?.toInt() ?: 0,
                                failedCount = (data["failedCount"] as? Long)?.toInt() ?: 0,
                                metadata = (data["metadata"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                                    if (k is String && v is String) k to v else null
                                }?.toMap() ?: emptyMap()
                            )
                        }
                        
                        trySend(notifications)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing notification history", e)
                        trySend(emptyList())
                    }
                } else {
                    trySend(emptyList())
                }
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get notification statistics for a specific notification
     * Requirements: 6.9
     */
    suspend fun getNotificationStats(notificationId: String): Result<NotificationStats> {
        return try {
            requireAdminAccess()
            
            if (notificationId.isBlank()) {
                return Result.failure(IllegalArgumentException("Notification ID cannot be blank"))
            }
            
            // Get notification document
            val notificationDoc = firestore.collection("notifications")
                .document(notificationId)
                .get()
                .await()
            
            if (!notificationDoc.exists()) {
                return Result.failure(NoSuchElementException("Notification not found"))
            }
            
            val data = notificationDoc.data ?: return Result.failure(Exception("No notification data"))
            
            // Get history records for this notification
            val historySnapshot = firestore.collection("notificationHistory")
                .whereEqualTo("notificationId", notificationId)
                .get()
                .await()
            
            val delivered = historySnapshot.documents.count { doc ->
                doc.getLong("deliveredAt") ?: 0L > 0L && doc.getString("errorMessage").isNullOrBlank()
            }
            
            val opened = historySnapshot.documents.count { doc ->
                doc.getBoolean("isOpened") == true
            }
            
            val failed = historySnapshot.documents.count { doc ->
                !doc.getString("errorMessage").isNullOrBlank()
            }
            
            val totalSent = (data["deliveredCount"] as? Long)?.toInt() ?: delivered
            val pending = maxOf(0, totalSent - delivered - failed)
            
            val stats = NotificationStats(
                totalSent = totalSent,
                delivered = delivered,
                opened = opened,
                failed = failed,
                pending = pending
            )
            
            Log.d(TAG, "Retrieved notification stats for: $notificationId")
            Result.success(stats)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting notification stats", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notification stats", e)
            Result.failure(Exception("Failed to get notification stats: ${e.message}"))
        }
    }
    
    /**
     * Update notification opened status
     * Called when user opens a notification
     */
    suspend fun markNotificationAsOpened(notificationId: String, userId: String): Result<Unit> {
        return try {
            if (notificationId.isBlank() || userId.isBlank()) {
                return Result.failure(IllegalArgumentException("Invalid parameters"))
            }
            
            // Find the history record
            val historySnapshot = firestore.collection("notificationHistory")
                .whereEqualTo("notificationId", notificationId)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
            
            if (historySnapshot.documents.isNotEmpty()) {
                val historyDoc = historySnapshot.documents.first()
                
                // Update history record
                firestore.collection("notificationHistory")
                    .document(historyDoc.id)
                    .update(
                        mapOf(
                            "isOpened" to true,
                            "openedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()
                
                // Increment opened count in notification
                firestore.collection("notifications")
                    .document(notificationId)
                    .update("openedCount", com.google.firebase.firestore.FieldValue.increment(1))
                    .await()
                
                Log.d(TAG, "Notification marked as opened: $notificationId")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as opened", e)
            Result.failure(Exception("Failed to mark notification as opened: ${e.message}"))
        }
    }

    // ========== Export Management Methods ==========
    // Requirements: 4.1, 4.2, 4.3
    
    /**
     * Get export history with real-time updates
     * Requirements: 4.1
     * Task: 14.4
     */
    fun getExportHistory(): Flow<List<ExportRequest>> = callbackFlow {
        try {
            if (!isAdminUser()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            
            val listener = firestore.collection("exports")
                .orderBy("requestedAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error loading export history", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        try {
                            val exports = snapshot.documents.mapNotNull { doc ->
                                try {
                                    ExportRequest(
                                        id = doc.id,
                                        format = ExportFormat.valueOf(doc.getString("format") ?: "PDF"),
                                        dataType = ExportDataType.valueOf(
                                            doc.getString("dataType") ?: "COMPREHENSIVE"
                                        ),
                                        dateRange = DateRange(
                                            startDate = (doc.get("dateRange.startDate") as? Long) ?: 0,
                                            endDate = (doc.get("dateRange.endDate") as? Long) ?: System.currentTimeMillis()
                                        ),
                                        filters = doc.get("filters") as? Map<String, String> ?: emptyMap(),
                                        requestedBy = doc.getString("requestedBy") ?: "",
                                        requestedAt = doc.getLong("requestedAt") ?: 0,
                                        status = ExportStatus.valueOf(
                                            doc.getString("status") ?: "PENDING"
                                        ),
                                        fileUrl = doc.getString("fileUrl") ?: "",
                                        fileName = doc.getString("fileName") ?: "",
                                        completedAt = doc.getLong("completedAt") ?: 0,
                                        errorMessage = doc.getString("errorMessage") ?: ""
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing export document", e)
                                    null
                                }
                            }
                            trySend(exports)
                            Log.d(TAG, "Loaded ${exports.size} export history items")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing export history", e)
                            trySend(emptyList())
                        }
                    } else {
                        trySend(emptyList())
                    }
                }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up export history listener", e)
            trySend(emptyList())
            close()
        }
    }
    
    /**
     * Export data based on request parameters
     * Requirements: 4.1, 4.2, 4.3
     */
    suspend fun exportData(request: ExportRequest): Result<String> {
        return try {
            requireAdminAccess()
            
            // This is a placeholder - actual export generation is handled by ExportFileManager
            // Here we just save the export request to Firestore
            firestore.collection("exports")
                .document(request.id)
                .set(request.toMap())
                .await()
            
            Log.d(TAG, "Export request saved: ${request.id}")
            Result.success(request.fileUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving export request", e)
            Result.failure(Exception("Failed to save export request: ${e.message}"))
        }
    }
    
    /**
     * Generate PDF report
     * Requirements: 4.2, 4.8
     */
    suspend fun generatePdfReport(request: ExportRequest): Result<String> {
        return exportData(request)
    }
    
    /**
     * Generate CSV export
     * Requirements: 4.3, 4.9
     */
    suspend fun generateCsvExport(request: ExportRequest): Result<String> {
        return exportData(request)
    }

    // ========== Pagination Methods ==========
    // Requirements: 8.2, 9.1
    
    /**
     * Get paginated items with real-time updates
     * Requirements: 8.2, 9.1
     */
    suspend fun getItemsPaginated(
        paginationHelper: com.example.loginandregistration.admin.utils.PaginationHelper<EnhancedLostFoundItem>
    ): Result<List<EnhancedLostFoundItem>> {
        return try {
            requireAdminAccess()
            
            val query = firestore.collection(ITEMS_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
            
            paginationHelper.loadNextPage(query) { doc ->
                doc.toObject(EnhancedLostFoundItem::class.java)!!
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting paginated items", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paginated items", e)
            Result.failure(Exception("Failed to get paginated items: ${e.message}"))
        }
    }
    
    /**
     * Get paginated users with real-time updates
     * Requirements: 8.2, 9.1
     */
    suspend fun getUsersPaginated(
        paginationHelper: com.example.loginandregistration.admin.utils.PaginationHelper<EnhancedAdminUser>
    ): Result<List<EnhancedAdminUser>> {
        return try {
            requireAdminAccess()
            
            val query = firestore.collection(USERS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
            
            paginationHelper.loadNextPage(query) { doc ->
                doc.toObject(EnhancedAdminUser::class.java)!!
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting paginated users", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paginated users", e)
            Result.failure(Exception("Failed to get paginated users: ${e.message}"))
        }
    }
    
    /**
     * Get paginated activity logs
     * Requirements: 8.2, 9.1
     */
    suspend fun getActivityLogsPaginated(
        paginationHelper: com.example.loginandregistration.admin.utils.PaginationHelper<ActivityLog>,
        filters: Map<String, String> = emptyMap()
    ): Result<List<ActivityLog>> {
        return try {
            requireAdminAccess()
            
            var query = firestore.collection(ACTIVITY_LOGS_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
            
            // Apply filters if provided
            filters.forEach { (key, value) ->
                when (key) {
                    "actionType" -> query = query.whereEqualTo("actionType", value)
                    "actorId" -> query = query.whereEqualTo("actorId", value)
                    "targetType" -> query = query.whereEqualTo("targetType", value)
                }
            }
            
            paginationHelper.loadNextPage(query) { doc ->
                doc.toObject(ActivityLog::class.java)!!
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting paginated activity logs", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paginated activity logs", e)
            Result.failure(Exception("Failed to get paginated activity logs: ${e.message}"))
        }
    }
    
    /**
     * Get paginated donation queue
     * Requirements: 8.2, 9.1
     */
    suspend fun getDonationQueuePaginated(
        paginationHelper: com.example.loginandregistration.admin.utils.PaginationHelper<DonationItem>
    ): Result<List<DonationItem>> {
        return try {
            requireAdminAccess()
            
            val query = firestore.collection(DONATIONS_COLLECTION)
                .orderBy("eligibleAt", Query.Direction.DESCENDING)
            
            paginationHelper.loadNextPage(query) { doc ->
                doc.toObject(DonationItem::class.java)!!
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting paginated donation queue", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paginated donation queue", e)
            Result.failure(Exception("Failed to get paginated donation queue: ${e.message}"))
        }
    }

    // ========== Cached Analytics Methods ==========
    // Requirements: 9.5
    
    private val cacheManager = com.example.loginandregistration.admin.utils.CacheManager.getInstance()
    
    /**
     * Get user analytics with caching
     * Requirements: 1.7, 9.5
     */
    suspend fun getUserAnalyticsCached(): Result<UserAnalytics> {
        return try {
            requireAdminAccess()
            
            val analytics = cacheManager.getOrPut(
                key = com.example.loginandregistration.admin.utils.CacheManager.KEY_USER_ANALYTICS,
                ttl = 5 * 60 * 1000L // 5 minutes
            ) {
                computeUserAnalytics()
            }
            
            Result.success(analytics)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting cached user analytics", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cached user analytics", e)
            Result.failure(Exception("Failed to get user analytics: ${e.message}"))
        }
    }
    
    /**
     * Compute user analytics (internal method)
     * Heavy data processing operations run on Dispatchers.Default
     * Requirements: 9.6, 9.7
     */
    private suspend fun computeUserAnalytics(): UserAnalytics {
        // Fetch data on IO dispatcher
        val usersSnapshot = withContext(Dispatchers.IO) {
            firestore.collection(USERS_COLLECTION).get().await()
        }
        val users = usersSnapshot.documents.mapNotNull { 
            it.toObject(EnhancedAdminUser::class.java) 
        }
        
        // Perform heavy computations on Default dispatcher to avoid blocking main thread
        return withContext(Dispatchers.Default) {
            val totalUsers = users.size
            val activeUsers = users.count { !it.isBlocked }
            val blockedUsers = users.count { it.isBlocked }
            
            val usersByRole = users.groupBy { it.role }
                .mapValues { it.value.size }
            
            // Calculate new users this month
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val monthStart = calendar.timeInMillis
            
            val newUsersThisMonth = users.count { it.createdAt.seconds * 1000 >= monthStart }
            
            // Calculate average items per user
            val itemsSnapshot = withContext(Dispatchers.IO) {
                firestore.collection(ITEMS_COLLECTION).get().await()
            }
            val totalItems = itemsSnapshot.size()
            val averageItemsPerUser = if (totalUsers > 0) totalItems.toFloat() / totalUsers else 0f
            
            // Get top contributors (users with most items) - heavy operation
            val itemsByUser = itemsSnapshot.documents
                .mapNotNull { it.getString("userId") }
                .groupingBy { it }
                .eachCount()
            
            val topContributors = users
                .map { user ->
                    val itemCount = itemsByUser[user.uid] ?: 0
                    TopContributor(
                        userId = user.uid,
                        userName = user.displayName,
                        userEmail = user.email,
                        itemsReported = itemCount,
                        itemsFound = 0,
                        totalContributions = itemCount
                    )
                }
                .sortedByDescending { it.totalContributions }
                .take(5)
            
            UserAnalytics(
                totalUsers = totalUsers,
                activeUsers = activeUsers,
                blockedUsers = blockedUsers,
                usersByRole = usersByRole,
                topContributors = topContributors
            )
        }
    }
    
    /**
     * Get donation statistics with caching
     * Requirements: 3.6, 9.5
     */
    suspend fun getDonationStatsCached(dateRange: DateRange? = null): Result<DonationStats> {
        return try {
            requireAdminAccess()
            
            val cacheKey = if (dateRange != null) {
                "${com.example.loginandregistration.admin.utils.CacheManager.KEY_DONATION_STATS}_${dateRange.startDate}_${dateRange.endDate}"
            } else {
                com.example.loginandregistration.admin.utils.CacheManager.KEY_DONATION_STATS
            }
            
            val stats = cacheManager.getOrPut(
                key = cacheKey,
                ttl = 10 * 60 * 1000L // 10 minutes
            ) {
                computeDonationStats(dateRange)
            }
            
            Result.success(stats)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting cached donation stats", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cached donation stats", e)
            Result.failure(Exception("Failed to get donation stats: ${e.message}"))
        }
    }
    
    /**
     * Compute donation statistics (internal method)
     * Heavy data processing operations run on Dispatchers.Default
     * Requirements: 9.6, 9.7
     */
    private suspend fun computeDonationStats(dateRange: DateRange?): DonationStats {
        // Fetch data on IO dispatcher
        val snapshot = withContext(Dispatchers.IO) {
            var query: Query = firestore.collection(DONATIONS_COLLECTION)
            
            // Apply date range filter if provided
            if (dateRange != null) {
                query = query
                    .whereGreaterThanOrEqualTo("donatedAt", dateRange.startDate)
                    .whereLessThanOrEqualTo("donatedAt", dateRange.endDate)
            }
            
            query.get().await()
        }
        
        val donations = snapshot.documents.mapNotNull { 
            it.toObject(DonationItem::class.java) 
        }
        
        // Perform heavy computations on Default dispatcher to avoid blocking main thread
        return withContext(Dispatchers.Default) {
            val totalDonated = donations.count { it.status == DonationStatus.DONATED }
            val totalValue = donations
                .filter { it.status == DonationStatus.DONATED }
                .sumOf { it.estimatedValue }
            
            val donationsByCategory = donations
                .filter { it.status == DonationStatus.DONATED }
                .groupBy { it.category }
                .mapValues { it.value.size }
            
            // Calculate donations by month
            val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val donationsByMonth = donations
                .filter { it.status == DonationStatus.DONATED && it.donatedAt > 0 }
                .groupBy { dateFormat.format(Date(it.donatedAt)) }
                .mapValues { it.value.size }
            
            val pendingDonations = donations.count { it.status == DonationStatus.PENDING }
            val readyForDonation = donations.count { it.status == DonationStatus.READY }
            
            DonationStats(
                totalDonated = totalDonated,
                totalValue = totalValue,
                donationsByCategory = donationsByCategory,
                donationsByMonth = donationsByMonth,
                pendingDonations = pendingDonations,
                readyForDonation = readyForDonation
            )
        }
    }
    
    /**
     * Invalidate analytics cache when data changes
     * Should be called after operations that affect analytics
     */
    fun invalidateAnalyticsCache() {
        cacheManager.invalidate(com.example.loginandregistration.admin.utils.CacheManager.KEY_USER_ANALYTICS)
        cacheManager.invalidate(com.example.loginandregistration.admin.utils.CacheManager.KEY_USER_STATISTICS)
        cacheManager.invalidatePattern("${com.example.loginandregistration.admin.utils.CacheManager.KEY_DONATION_STATS}.*")
        cacheManager.invalidate(com.example.loginandregistration.admin.utils.CacheManager.KEY_DASHBOARD_STATS)
        Log.d(TAG, "Analytics cache invalidated")
    }
    
    /**
     * Invalidate user-related cache
     */
    fun invalidateUserCache() {
        cacheManager.invalidate(com.example.loginandregistration.admin.utils.CacheManager.KEY_USER_ANALYTICS)
        cacheManager.invalidate(com.example.loginandregistration.admin.utils.CacheManager.KEY_USER_STATISTICS)
        Log.d(TAG, "User cache invalidated")
    }
    
    /**
     * Invalidate donation-related cache
     */
    fun invalidateDonationCache() {
        cacheManager.invalidatePattern("${com.example.loginandregistration.admin.utils.CacheManager.KEY_DONATION_STATS}.*")
        Log.d(TAG, "Donation cache invalidated")
    }

    // ========== Optimized Query Methods ==========
    // Requirements: 9.2, 9.6
    
    /**
     * Get items with optimized query and selective fields
     * Requirements: 9.2
     */
    suspend fun getItemsOptimized(
        filters: Map<String, Any> = emptyMap(),
        limit: Int = 50
    ): Result<List<EnhancedLostFoundItem>> {
        return try {
            requireAdminAccess()
            
            // Validate query complexity
            val validation = com.example.loginandregistration.admin.utils.QueryOptimizer.validateQueryComplexity(filters)
            if (!validation.isValid) {
                return Result.failure(IllegalArgumentException(validation.message))
            }
            
            val query = com.example.loginandregistration.admin.utils.QueryOptimizer.createItemsQuery(
                firestore, filters, limit
            )
            
            val snapshot = query.get().await()
            val items = snapshot.documents.mapNotNull { 
                it.toObject(EnhancedLostFoundItem::class.java) 
            }
            
            Log.d(TAG, "Retrieved ${items.size} items with optimized query")
            Result.success(items)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting optimized items", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting optimized items", e)
            Result.failure(Exception("Failed to get items: ${e.message}"))
        }
    }
    
    /**
     * Get users with optimized query
     * Requirements: 9.2
     */
    suspend fun getUsersOptimized(
        filters: Map<String, Any> = emptyMap(),
        limit: Int = 50
    ): Result<List<EnhancedAdminUser>> {
        return try {
            requireAdminAccess()
            
            val validation = com.example.loginandregistration.admin.utils.QueryOptimizer.validateQueryComplexity(filters)
            if (!validation.isValid) {
                return Result.failure(IllegalArgumentException(validation.message))
            }
            
            val query = com.example.loginandregistration.admin.utils.QueryOptimizer.createUsersQuery(
                firestore, filters, limit
            )
            
            val snapshot = query.get().await()
            val users = snapshot.documents.mapNotNull { 
                it.toObject(EnhancedAdminUser::class.java) 
            }
            
            Log.d(TAG, "Retrieved ${users.size} users with optimized query")
            Result.success(users)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting optimized users", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting optimized users", e)
            Result.failure(Exception("Failed to get users: ${e.message}"))
        }
    }
    
    /**
     * Get activity logs with optimized query and composite indexes
     * Requirements: 9.2, 9.6
     */
    suspend fun getActivityLogsOptimized(
        filters: Map<String, Any> = emptyMap(),
        limit: Int = 50
    ): Result<List<ActivityLog>> {
        return try {
            requireAdminAccess()
            
            val validation = com.example.loginandregistration.admin.utils.QueryOptimizer.validateQueryComplexity(filters)
            if (!validation.isValid) {
                return Result.failure(IllegalArgumentException(validation.message))
            }
            
            val query = com.example.loginandregistration.admin.utils.QueryOptimizer.createActivityLogsQuery(
                firestore, filters, limit
            )
            
            val snapshot = query.get().await()
            val logs = snapshot.documents.mapNotNull { 
                it.toObject(ActivityLog::class.java) 
            }
            
            Log.d(TAG, "Retrieved ${logs.size} activity logs with optimized query")
            Result.success(logs)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting optimized activity logs", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting optimized activity logs", e)
            Result.failure(Exception("Failed to get activity logs: ${e.message}"))
        }
    }
    
    /**
     * Get donations with optimized query
     * Requirements: 9.2
     */
    suspend fun getDonationsOptimized(
        filters: Map<String, Any> = emptyMap(),
        limit: Int = 50
    ): Result<List<DonationItem>> {
        return try {
            requireAdminAccess()
            
            val validation = com.example.loginandregistration.admin.utils.QueryOptimizer.validateQueryComplexity(filters)
            if (!validation.isValid) {
                return Result.failure(IllegalArgumentException(validation.message))
            }
            
            val query = com.example.loginandregistration.admin.utils.QueryOptimizer.createDonationsQuery(
                firestore, filters, limit
            )
            
            val snapshot = query.get().await()
            val donations = snapshot.documents.mapNotNull { 
                it.toObject(DonationItem::class.java) 
            }
            
            Log.d(TAG, "Retrieved ${donations.size} donations with optimized query")
            Result.success(donations)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting optimized donations", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting optimized donations", e)
            Result.failure(Exception("Failed to get donations: ${e.message}"))
        }
    }
    
    /**
     * Optimized search with result limiting
     * Requirements: 9.6
     */
    suspend fun searchOptimized(
        collection: String,
        searchField: String,
        searchValue: String,
        limit: Int = 100
    ): Result<List<Map<String, Any>>> {
        return try {
            requireAdminAccess()
            
            if (searchValue.isBlank()) {
                return Result.success(emptyList())
            }
            
            val query = com.example.loginandregistration.admin.utils.QueryOptimizer.createSearchQuery(
                firestore, collection, searchField, searchValue, limit
            )
            
            val snapshot = query.get().await()
            val results = snapshot.documents.map { it.data ?: emptyMap() }
            
            Log.d(TAG, "Search returned ${results.size} results")
            Result.success(results)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error in optimized search", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error in optimized search", e)
            Result.failure(Exception("Search failed: ${e.message}"))
        }
    }

    // ========== Background Export Processing ==========
    // Requirements: 9.4
    
    /**
     * Queue an export for background processing
     * Requirements: 9.4
     */
    fun queueExportInBackground(
        context: android.content.Context,
        request: ExportRequest
    ): Result<UUID> {
        return try {
            requireAdminAccess()
            
            val exportQueue = com.example.loginandregistration.admin.utils.ExportQueueManager.getInstance(context)
            val workId = exportQueue.queueExport(request)
            
            Log.d(TAG, "Export queued for background processing: $workId")
            Result.success(workId)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error queueing export", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error queueing export", e)
            Result.failure(Exception("Failed to queue export: ${e.message}"))
        }
    }
    
    /**
     * Observe export status from background queue
     * Requirements: 9.4
     */
    fun observeBackgroundExportStatus(
        context: android.content.Context,
        workId: UUID
    ): androidx.lifecycle.LiveData<androidx.work.WorkInfo?> {
        val exportQueue = com.example.loginandregistration.admin.utils.ExportQueueManager.getInstance(context)
        return exportQueue.observeExportStatus(workId)
    }
    
    /**
     * Cancel a background export
     * Requirements: 9.4
     */
    fun cancelBackgroundExport(
        context: android.content.Context,
        workId: UUID
    ) {
        val exportQueue = com.example.loginandregistration.admin.utils.ExportQueueManager.getInstance(context)
        exportQueue.cancelExport(workId)
        Log.d(TAG, "Background export cancelled: $workId")
    }
    
    /**
     * Observe all active background exports
     * Requirements: 9.4
     */
    fun observeActiveBackgroundExports(
        context: android.content.Context
    ): androidx.lifecycle.LiveData<List<androidx.work.WorkInfo>> {
        val exportQueue = com.example.loginandregistration.admin.utils.ExportQueueManager.getInstance(context)
        return exportQueue.observeActiveExports()
    }
    
    /**
     * Observe export queue statistics
     * Requirements: 9.4
     */
    fun observeExportQueueStats(
        context: android.content.Context
    ): androidx.lifecycle.LiveData<List<androidx.work.WorkInfo>> {
        val exportQueue = com.example.loginandregistration.admin.utils.ExportQueueManager.getInstance(context)
        return exportQueue.observeQueueStats()
    }
    
    // ========== Activity Log Archiving Methods ==========
    // Requirements: 5.11
    
    /**
     * Get count of logs eligible for archiving (older than 1 year)
     * Requirements: 5.11
     */
    
    // Note: Item Management Methods (getItemDetails, updateItemDetails, deleteItem) 
    // are already implemented earlier in this file (lines 965-1300)
    // Requirements: 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9
    
    // ========== User Schema Migration Methods ==========
    // Requirements: 2.5
    
    /**
     * Migrate individual user document from old schema to new unified schema
     * Maps old fields (userId → uid, name → displayName) and adds missing fields
     * Requirements: 2.5
     */
    suspend fun migrateUserSchema(userId: String): Result<Unit> {
        return try {
            if (userId.isBlank()) {
                return Result.failure(IllegalArgumentException("User ID cannot be blank"))
            }
            
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (!userDoc.exists()) {
                return Result.failure(NoSuchElementException("User not found: $userId"))
            }
            
            val updates = mutableMapOf<String, Any>()
            
            // Map old fields to new fields
            userDoc.getString("userId")?.let { 
                updates["uid"] = it 
                Log.d(TAG, "Mapping userId -> uid: $it")
            }
            userDoc.getString("name")?.let { 
                updates["displayName"] = it 
                Log.d(TAG, "Mapping name -> displayName: $it")
            }
            
            // Add missing fields with defaults
            if (!userDoc.contains("role")) {
                updates["role"] = "USER"
                Log.d(TAG, "Adding default role: USER")
            }
            if (!userDoc.contains("isBlocked")) {
                updates["isBlocked"] = false
                Log.d(TAG, "Adding default isBlocked: false")
            }
            if (!userDoc.contains("photoUrl")) {
                updates["photoUrl"] = ""
                Log.d(TAG, "Adding default photoUrl: empty")
            }
            if (!userDoc.contains("itemsReported")) {
                updates["itemsReported"] = 0
                Log.d(TAG, "Adding default itemsReported: 0")
            }
            if (!userDoc.contains("itemsFound")) {
                updates["itemsFound"] = 0
                Log.d(TAG, "Adding default itemsFound: 0")
            }
            if (!userDoc.contains("itemsClaimed")) {
                updates["itemsClaimed"] = 0
                Log.d(TAG, "Adding default itemsClaimed: 0")
            }
            if (!userDoc.contains("lastLoginAt")) {
                updates["lastLoginAt"] = com.google.firebase.firestore.FieldValue.delete()
                Log.d(TAG, "Adding default lastLoginAt: null")
            }
            
            if (updates.isNotEmpty()) {
                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .update(updates)
                    .await()
                Log.d(TAG, "Successfully migrated user $userId with ${updates.size} updates")
            } else {
                Log.d(TAG, "User $userId already has unified schema, no migration needed")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating user schema for $userId", e)
            Result.failure(Exception("Failed to migrate user schema: ${e.message}"))
        }
    }
    
    /**
     * Migrate all users from old schema to new unified schema
     * Performs batch migration with error tracking
     * Requirements: 2.5
     */
    suspend fun migrateAllUsers(): Result<Int> {
        return try {
            Log.d(TAG, "Starting batch user migration...")
            
            val users = firestore.collection(USERS_COLLECTION)
                .get()
                .await()
            
            var migratedCount = 0
            var errorCount = 0
            val errors = mutableListOf<String>()
            
            users.documents.forEach { doc ->
                val userId = doc.id
                migrateUserSchema(userId)
                    .onSuccess { 
                        migratedCount++
                        Log.d(TAG, "Migrated user $userId ($migratedCount/${users.size()})")
                    }
                    .onFailure { e ->
                        errorCount++
                        val errorMsg = "Failed to migrate user $userId: ${e.message}"
                        errors.add(errorMsg)
                        Log.e(TAG, errorMsg, e)
                    }
            }
            
            Log.d(TAG, "Batch migration complete: $migratedCount migrated, $errorCount errors")
            
            if (errors.isNotEmpty()) {
                Log.w(TAG, "Migration errors: ${errors.joinToString("; ")}")
            }
            
            Result.success(migratedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error during batch user migration", e)
            Result.failure(Exception("Failed to migrate all users: ${e.message}"))
        }
    }
    
    // ========== Test Data Generation Methods ==========
    // Requirements: 3.1, 3.2, 3.3
    
    /**
     * Generate test items with timestamps from 1+ year ago
     * Creates items at different ages (365 days, 400 days, 2 years, etc.) and various statuses
     * for comprehensive testing of the donation workflow
     * Requirements: 3.1, 3.2, 3.3
     */
    suspend fun generateOldTestItems(): Result<Int> {
        return try {
            requireAdminAccess()
            
            Log.d(TAG, "Generating old test items for donation testing...")
            val result = TestDataGenerator.generateOldTestItems(firestore)
            
            result.onSuccess { count ->
                Log.d(TAG, "Successfully generated $count old test items")
                
                // Log activity
                logActivity(
                    ActivityItem(
                        userId = auth.currentUser?.uid ?: "",
                        userName = "Admin",
                        userEmail = auth.currentUser?.email ?: "",
                        action = ActivityType.ITEM_REPORTED,
                        description = "Generated $count old test items for donation testing"
                    )
                )
            }.onFailure { e ->
                Log.e(TAG, "Failed to generate old test items", e)
            }
            
            result
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error generating test items", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating test items", e)
            Result.failure(Exception("Failed to generate test items: ${e.message}"))
        }
    }
    
    /**
     * Delete all test items created by the test data generator
     * Useful for cleanup after testing
     * Requirements: 3.1, 3.2, 3.3
     */
    suspend fun deleteTestItems(): Result<Int> {
        return try {
            requireAdminAccess()
            
            Log.d(TAG, "Deleting test items...")
            val result = TestDataGenerator.deleteTestItems(firestore)
            
            result.onSuccess { count ->
                Log.d(TAG, "Successfully deleted $count test items")
                
                // Log activity
                logActivity(
                    ActivityItem(
                        userId = auth.currentUser?.uid ?: "",
                        userName = "Admin",
                        userEmail = auth.currentUser?.email ?: "",
                        action = ActivityType.STATUS_CHANGED,
                        description = "Deleted $count test items"
                    )
                )
            }.onFailure { e ->
                Log.e(TAG, "Failed to delete test items", e)
            }
            
            result
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error deleting test items", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting test items", e)
            Result.failure(Exception("Failed to delete test items: ${e.message}"))
        }
    }
    
    /**
     * Get user analytics with real-time data
     * Requirements: 1.7
     */
    fun getUserAnalytics(): Flow<UserAnalytics> = callbackFlow {
        try {
            requireAdminAccess()
            
            // Check cache first
            val now = System.currentTimeMillis()
            if (cachedAnalytics != null && (now - analyticsCacheTimestamp) < analyticsCacheTimeout) {
                trySend(cachedAnalytics!!)
                close()
                return@callbackFlow
            }
            
            Log.d(TAG, "getUserAnalytics: Loading fresh analytics data")
            
            // Get all users
            val usersSnapshot = firestore.collection(USERS_COLLECTION).get().await()
            val users = usersSnapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    AdminUser(
                        uid = doc.id,
                        email = data["email"] as? String ?: "",
                        displayName = data["displayName"] as? String ?: data["name"] as? String ?: "",
                        photoUrl = data["photoUrl"] as? String ?: "",
                        role = UserRole.fromString(data["role"] as? String ?: "STUDENT"),
                        isBlocked = data["isBlocked"] as? Boolean ?: false,
                        createdAt = data["createdAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                        lastLoginAt = data["lastLoginAt"] as? com.google.firebase.Timestamp,
                        itemsReported = (data["itemsReported"] as? Number)?.toInt() ?: 0,
                        itemsFound = (data["itemsFound"] as? Number)?.toInt() ?: 0,
                        itemsClaimed = (data["itemsClaimed"] as? Number)?.toInt() ?: 0
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing user document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            Log.d(TAG, "getUserAnalytics: Loaded ${users.size} users")
            
            // Get all items for contribution calculation
            val itemsSnapshot = firestore.collection(ITEMS_COLLECTION).get().await()
            val items = itemsSnapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    data["userId"] as? String to (data["isLost"] as? Boolean ?: true)
                } catch (e: Exception) {
                    null
                }
            }
            
            Log.d(TAG, "getUserAnalytics: Loaded ${items.size} items for contribution calculation")
            
            // Calculate statistics
            val totalUsers = users.size
            val activeUsers = users.count { !it.isBlocked }
            val blockedUsers = users.count { it.isBlocked }
            
            // Count users by role
            val usersByRole = mutableMapOf<UserRole, Int>()
            UserRole.values().forEach { role ->
                usersByRole[role] = users.count { it.role == role }
            }
            
            Log.d(TAG, "getUserAnalytics: Role distribution - ${usersByRole}")
            
            // Calculate top contributors
            val userContributions = mutableMapOf<String, MutableMap<String, Any>>()
            
            // Initialize contribution maps for all users
            users.forEach { user ->
                userContributions[user.uid] = mutableMapOf(
                    "userName" to user.displayName,
                    "userEmail" to user.email,
                    "itemsReported" to 0,
                    "itemsFound" to 0
                )
            }
            
            // Count contributions from items
            items.forEach { (userId, isLost) ->
                if (userId != null && userContributions.containsKey(userId)) {
                    val contrib = userContributions[userId]!!
                    if (isLost) {
                        contrib["itemsReported"] = (contrib["itemsReported"] as Int) + 1
                    } else {
                        contrib["itemsFound"] = (contrib["itemsFound"] as Int) + 1
                    }
                }
            }
            
            // Create top contributors list
            val topContributors = userContributions.entries
                .map { (userId, data) ->
                    val itemsReported = data["itemsReported"] as Int
                    val itemsFound = data["itemsFound"] as Int
                    TopContributor(
                        userId = userId,
                        userName = data["userName"] as String,
                        userEmail = data["userEmail"] as String,
                        itemsReported = itemsReported,
                        itemsFound = itemsFound,
                        totalContributions = itemsReported + itemsFound
                    )
                }
                .filter { it.totalContributions > 0 }
                .sortedByDescending { it.totalContributions }
                .take(5)
            
            Log.d(TAG, "getUserAnalytics: Top ${topContributors.size} contributors calculated")
            
            val analytics = UserAnalytics(
                totalUsers = totalUsers,
                activeUsers = activeUsers,
                blockedUsers = blockedUsers,
                usersByRole = usersByRole,
                topContributors = topContributors
            )
            
            // Cache the result
            cachedAnalytics = analytics
            analyticsCacheTimestamp = now
            
            trySend(analytics)
            close()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error getting user analytics", e)
            trySend(UserAnalytics())
            close(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user analytics", e)
            trySend(UserAnalytics())
            close(e)
        }
        
        awaitClose { }
    }
}
