package com.example.loginandregistration.admin.models

import com.google.firebase.firestore.PropertyName

data class DashboardStats(
    val totalItems: Int = 0,
    val lostItems: Int = 0,
    val foundItems: Int = 0,
    val receivedItems: Int = 0,
    val pendingItems: Int = 0,
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val blockedUsers: Int = 0
)

data class ActivityItem(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val action: ActivityType = ActivityType.ITEM_REPORTED,
    val itemId: String = "",
    val itemName: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isNew: Boolean = true
) {
    // Ignore the "new" field from Firestore to prevent deserialization errors
    // This field is not used in the current implementation
    @get:PropertyName("new")
    @set:PropertyName("new")
    @Suppress("unused")
    var newField: Boolean = true
}

enum class ActivityType {
    ITEM_REPORTED,
    ITEM_FOUND,
    ITEM_CLAIMED,
    ITEM_VERIFIED,
    USER_REGISTERED,
    USER_BLOCKED,
    USER_UNBLOCKED,
    STATUS_CHANGED
}

data class AnalyticsData(
    val itemsByCategory: Map<String, Int> = emptyMap(),
    val itemsByStatus: Map<String, Int> = emptyMap(),
    val dailyActivity: List<DailyActivity> = emptyList(),
    val monthlyTrends: List<MonthlyTrend> = emptyList(),
    val successRate: Float = 0f,
    val averageResolutionTime: Float = 0f
)

data class DailyActivity(
    val date: String = "",
    val itemsReported: Int = 0,
    val itemsFound: Int = 0,
    val itemsClaimed: Int = 0
)

data class MonthlyTrend(
    val month: String = "",
    val totalItems: Int = 0,
    val resolvedItems: Int = 0
)

/**
 * User analytics data model
 * Requirements: 1.7
 */
data class UserAnalytics(
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val blockedUsers: Int = 0,
    val usersByRole: Map<UserRole, Int> = emptyMap(),
    val topContributors: List<TopContributor> = emptyList()
)

/**
 * Top contributor model
 * Requirements: 1.7
 */
data class TopContributor(
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val itemsReported: Int = 0,
    val itemsFound: Int = 0,
    val totalContributions: Int = 0
)