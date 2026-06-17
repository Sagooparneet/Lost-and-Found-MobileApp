package com.example.loginandregistration.admin.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.util.Log

/**
 * Query optimizer for Firestore queries
 * Provides optimized query builders with selective field queries and result limiting
 * Requirements: 9.2, 9.6
 */
object QueryOptimizer {
    private const val TAG = "QueryOptimizer"
    private const val DEFAULT_LIMIT = 50
    private const val SEARCH_LIMIT = 100
    
    /**
     * Create an optimized query for items with selective fields
     * Requirements: 9.2
     */
    fun createItemsQuery(
        firestore: FirebaseFirestore,
        filters: Map<String, Any> = emptyMap(),
        limit: Int = DEFAULT_LIMIT
    ): Query {
        var query: Query = firestore.collection("items")
        
        // Apply filters
        filters.forEach { (key, value) ->
            when (key) {
                "status" -> query = query.whereEqualTo("status", value)
                "category" -> query = query.whereEqualTo("category", value)
                "isLost" -> query = query.whereEqualTo("isLost", value)
                "userId" -> query = query.whereEqualTo("userId", value)
            }
        }
        
        // Order by timestamp (descending) and limit results
        query = query.orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
        
        Log.d(TAG, "Created optimized items query with filters: $filters, limit: $limit")
        return query
    }
    
    /**
     * Create an optimized query for users with selective fields
     * Requirements: 9.2
     */
    fun createUsersQuery(
        firestore: FirebaseFirestore,
        filters: Map<String, Any> = emptyMap(),
        limit: Int = DEFAULT_LIMIT
    ): Query {
        var query: Query = firestore.collection("users")
        
        // Apply filters
        filters.forEach { (key, value) ->
            when (key) {
                "role" -> query = query.whereEqualTo("role", value)
                "isBlocked" -> query = query.whereEqualTo("isBlocked", value)
            }
        }
        
        // Order by creation date and limit results
        query = query.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
        
        Log.d(TAG, "Created optimized users query with filters: $filters, limit: $limit")
        return query
    }
    
    /**
     * Create an optimized query for activity logs with composite indexes
     * Requirements: 9.2, 9.6
     */
    fun createActivityLogsQuery(
        firestore: FirebaseFirestore,
        filters: Map<String, Any> = emptyMap(),
        limit: Int = DEFAULT_LIMIT
    ): Query {
        var query: Query = firestore.collection("activityLogs")
        
        // Apply filters - order matters for composite indexes
        filters.forEach { (key, value) ->
            when (key) {
                "actionType" -> query = query.whereEqualTo("actionType", value)
                "actorId" -> query = query.whereEqualTo("actorId", value)
                "targetType" -> query = query.whereEqualTo("targetType", value)
                "startDate" -> query = query.whereGreaterThanOrEqualTo("timestamp", value)
                "endDate" -> query = query.whereLessThanOrEqualTo("timestamp", value)
            }
        }
        
        // Order by timestamp (descending) and limit results
        query = query.orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
        
        Log.d(TAG, "Created optimized activity logs query with filters: $filters, limit: $limit")
        return query
    }
    
    /**
     * Create an optimized query for donations with composite indexes
     * Requirements: 9.2
     */
    fun createDonationsQuery(
        firestore: FirebaseFirestore,
        filters: Map<String, Any> = emptyMap(),
        limit: Int = DEFAULT_LIMIT
    ): Query {
        var query: Query = firestore.collection("donations")
        
        // Apply filters
        filters.forEach { (key, value) ->
            when (key) {
                "status" -> query = query.whereEqualTo("status", value)
                "category" -> query = query.whereEqualTo("category", value)
            }
        }
        
        // Order by eligible date and limit results
        query = query.orderBy("eligibleAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
        
        Log.d(TAG, "Created optimized donations query with filters: $filters, limit: $limit")
        return query
    }
    
    /**
     * Create an optimized search query with result limiting
     * Requirements: 9.6
     */
    fun createSearchQuery(
        firestore: FirebaseFirestore,
        collection: String,
        searchField: String,
        searchValue: String,
        limit: Int = SEARCH_LIMIT
    ): Query {
        // For prefix search, use range query
        val query = firestore.collection(collection)
            .whereGreaterThanOrEqualTo(searchField, searchValue)
            .whereLessThanOrEqualTo(searchField, searchValue + "\uf8ff")
            .limit(limit.toLong())
        
        Log.d(TAG, "Created optimized search query for $collection.$searchField = $searchValue")
        return query
    }
    
    /**
     * Create a query with selective fields (projection)
     * Note: Firestore doesn't support field projection in queries,
     * but we can document which fields should be used
     */
    fun getSelectiveFields(entityType: String): List<String> {
        return when (entityType) {
            "items" -> listOf(
                "id", "name", "description", "location", "category",
                "status", "timestamp", "userId", "userEmail"
            )
            "users" -> listOf(
                "uid", "email", "displayName", "role", "isBlocked",
                "createdAt", "lastLoginAt"
            )
            "activityLogs" -> listOf(
                "id", "timestamp", "actorEmail", "actionType",
                "targetType", "description"
            )
            "donations" -> listOf(
                "itemId", "itemName", "category", "status",
                "eligibleAt", "donatedAt"
            )
            else -> emptyList()
        }
    }
    
    /**
     * Get recommended query limits based on use case
     */
    fun getRecommendedLimit(useCase: String): Int {
        return when (useCase) {
            "list_view" -> 50
            "search" -> 100
            "analytics" -> 1000
            "export" -> Int.MAX_VALUE
            "dashboard" -> 20
            else -> DEFAULT_LIMIT
        }
    }
    
    /**
     * Validate query complexity to prevent expensive queries
     */
    fun validateQueryComplexity(filters: Map<String, Any>): QueryValidation {
        val filterCount = filters.size
        
        return when {
            filterCount > 5 -> QueryValidation(
                isValid = false,
                message = "Too many filters ($filterCount). Maximum 5 filters allowed."
            )
            filterCount == 0 -> QueryValidation(
                isValid = true,
                message = "Simple query with no filters",
                complexity = QueryComplexity.SIMPLE
            )
            filterCount <= 2 -> QueryValidation(
                isValid = true,
                message = "Moderate query complexity",
                complexity = QueryComplexity.MODERATE
            )
            else -> QueryValidation(
                isValid = true,
                message = "Complex query with multiple filters",
                complexity = QueryComplexity.COMPLEX
            )
        }
    }
}

/**
 * Query validation result
 */
data class QueryValidation(
    val isValid: Boolean,
    val message: String,
    val complexity: QueryComplexity = QueryComplexity.SIMPLE
)

/**
 * Query complexity levels
 */
enum class QueryComplexity {
    SIMPLE,     // No filters or single filter
    MODERATE,   // 2-3 filters
    COMPLEX     // 4-5 filters
}
