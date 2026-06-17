package com.example.loginandregistration.admin.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loginandregistration.LostFoundItem
import com.example.loginandregistration.admin.models.*
import com.example.loginandregistration.admin.repository.AdminRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class AdminDashboardViewModel : ViewModel() {
    
    private val repository = AdminRepository()
    
    // Dashboard Stats
    private val _dashboardStats = MutableLiveData<DashboardStats>()
    val dashboardStats: LiveData<DashboardStats> = _dashboardStats
    
    // Items
    private val _allItems = MutableLiveData<List<LostFoundItem>>()
    val allItems: LiveData<List<LostFoundItem>> = _allItems
    
    private val _filteredItems = MutableLiveData<List<LostFoundItem>>()
    val filteredItems: LiveData<List<LostFoundItem>> = _filteredItems
    
    // Enhanced Item Management (Task 8.2)
    private val _itemDetails = MutableLiveData<EnhancedLostFoundItem>()
    val itemDetails: LiveData<EnhancedLostFoundItem> = _itemDetails
    
    private val _allItemsWithStatus = MutableLiveData<List<EnhancedLostFoundItem>>()
    val allItemsWithStatus: LiveData<List<EnhancedLostFoundItem>> = _allItemsWithStatus
    
    // Donation Management (Task 8.3)
    private val _donationQueue = MutableLiveData<List<DonationItem>>()
    val donationQueue: LiveData<List<DonationItem>> = _donationQueue
    
    private val _donationStats = MutableLiveData<DonationStats>()
    val donationStats: LiveData<DonationStats> = _donationStats
    
    // Activity Log Management (Task 8.4)
    private val _activityLogs = MutableLiveData<List<ActivityLog>>()
    val activityLogs: LiveData<List<ActivityLog>> = _activityLogs
    
    private val _activityLogFilters = MutableLiveData<Map<String, String>>()
    val activityLogFilters: LiveData<Map<String, String>> = _activityLogFilters
    
    // Notification Management (Task 8.5)
    private val _notificationHistory = MutableLiveData<List<PushNotification>>()
    val notificationHistory: LiveData<List<PushNotification>> = _notificationHistory
    
    private val _notificationStats = MutableLiveData<NotificationStats>()
    val notificationStats: LiveData<NotificationStats> = _notificationStats
    
    // Export Management (Task 8.6)
    private val _exportProgress = MutableLiveData<Int>()
    val exportProgress: LiveData<Int> = _exportProgress
    
    private val _exportResult = MutableLiveData<String>()
    val exportResult: LiveData<String> = _exportResult
    
    private val _exportHistory = MutableLiveData<List<ExportRequest>>()
    val exportHistory: LiveData<List<ExportRequest>> = _exportHistory
    
    // Users
    private val _allUsers = MutableLiveData<List<AdminUser>>()
    val allUsers: LiveData<List<AdminUser>> = _allUsers
    
    // Enhanced User Management (Task 8.1)
    private val _userDetails = MutableLiveData<EnhancedAdminUser>()
    val userDetails: LiveData<EnhancedAdminUser> = _userDetails
    
    private val _userAnalytics = MutableLiveData<UserAnalytics>()
    val userAnalytics: LiveData<UserAnalytics> = _userAnalytics
    
    private val _filteredUsers = MutableLiveData<List<EnhancedAdminUser>>()
    val filteredUsers: LiveData<List<EnhancedAdminUser>> = _filteredUsers
    
    // Activities
    private val _recentActivities = MutableLiveData<List<ActivityItem>>()
    val recentActivities: LiveData<List<ActivityItem>> = _recentActivities
    
    // Analytics
    private val _analyticsData = MutableLiveData<AnalyticsData>()
    val analyticsData: LiveData<AnalyticsData> = _analyticsData
    
    // Loading states
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage
    
    init {
        loadDashboardData()
    }
    
    private fun loadDashboardData() {
        loadDashboardStats()
        loadAllItems()
        loadAllUsers()
        loadRecentActivities()
        loadAnalyticsData()
    }
    
    private fun loadDashboardStats() {
        viewModelScope.launch {
            repository.getDashboardStats()
                .catch { e -> _error.value = e.message }
                .collect { stats ->
                    _dashboardStats.value = stats
                }
        }
    }
    
    private fun loadAllItems() {
        viewModelScope.launch {
            repository.getAllItems()
                .catch { e -> _error.value = e.message }
                .collect { items ->
                    _allItems.value = items
                }
        }
    }
    
    fun loadItemsByStatus(status: String) {
        viewModelScope.launch {
            repository.getItemsByStatus(status)
                .catch { e -> _error.value = e.message }
                .collect { items ->
                    _filteredItems.value = items
                }
        }
    }
    
    fun loadAllUsers() {
        viewModelScope.launch {
            android.util.Log.d("AdminDashboardViewModel", "loadAllUsers: Starting to load users")
            _isLoading.value = true
            repository.getAllUsers()
                .catch { e -> 
                    android.util.Log.e("AdminDashboardViewModel", "loadAllUsers: Error loading users", e)
                    _error.value = "Failed to load users: ${e.message}"
                    _isLoading.value = false
                }
                .collect { users ->
                    android.util.Log.d("AdminDashboardViewModel", "loadAllUsers: Loaded ${users.size} users")
                    _allUsers.value = users
                    _isLoading.value = false
                }
        }
    }
    
    private fun loadRecentActivities() {
        viewModelScope.launch {
            repository.getRecentActivities()
                .catch { e -> _error.value = e.message }
                .collect { activities ->
                    _recentActivities.value = activities
                }
        }
    }
    
    private fun loadAnalyticsData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAnalyticsData()
                .onSuccess { data ->
                    _analyticsData.value = data
                }
                .onFailure { e ->
                    _error.value = e.message
                }
            _isLoading.value = false
        }
    }
    
    fun updateUserBlockStatus(userId: String, isBlocked: Boolean) {
        viewModelScope.launch {
            repository.updateUserBlockStatus(userId, isBlocked)
                .onFailure { e ->
                    _error.value = "Failed to update user: ${e.message}"
                }
        }
    }
    
    fun updateUserRole(userId: String, role: UserRole) {
        viewModelScope.launch {
            repository.updateUserRole(userId, role)
                .onFailure { e ->
                    _error.value = "Failed to update user role: ${e.message}"
                }
        }
    }
    
    fun updateItemStatus(itemId: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateItemStatus(itemId, newStatus)
                .onFailure { e ->
                    _error.value = "Failed to update item status: ${e.message}"
                }
        }
    }
    
    fun refreshData() {
        loadDashboardData()
    }
    
    fun createTestData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.createTestData()
                .onSuccess {
                    // Test data created successfully
                    loadDashboardData() // Refresh data
                }
                .onFailure { e ->
                    _error.value = "Failed to create test data: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Search items with query - optimized for large lists
     * Requirements: 9.6, 9.7
     */
    fun searchItems(query: String) {
        val currentItems = _allItems.value ?: return
        
        // For large lists, perform filtering on background thread
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val filtered = if (query.isBlank()) {
                currentItems
            } else {
                currentItems.filter { item ->
                    item.name.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    item.location.contains(query, ignoreCase = true) ||
                    item.userEmail.contains(query, ignoreCase = true)
                }
            }
            
            // Update LiveData on main thread
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                _filteredItems.value = filtered
            }
        }
    }
    
    /**
     * Search users with query - optimized for large lists
     * Requirements: 9.6, 9.7
     */
    fun searchUsers(query: String) {
        val currentUsers = _allUsers.value ?: return
        
        // For large lists, perform filtering on background thread
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val filtered = if (query.isBlank()) {
                currentUsers
            } else {
                currentUsers.filter { user ->
                    user.email.contains(query, ignoreCase = true) ||
                    user.displayName.contains(query, ignoreCase = true)
                }
            }
            
            // Update LiveData on main thread (if you add filtered users LiveData)
            // _filteredUsers.value = filtered
        }
    }
    
    // ========== Enhanced User Management Methods (Task 8.1) ==========
    // Requirements: 1.1-1.9
    
    /**
     * Load detailed user information
     * Requirements: 1.2
     */
    fun loadUserDetails(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getUserDetails(userId)
                .onSuccess { user ->
                    _userDetails.value = user
                }
                .onFailure { e ->
                    _error.value = "Failed to load user details: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Block a user with reason
     * Requirements: 1.3, 1.4
     */
    fun blockUser(userId: String, reason: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.blockUser(userId, reason)
                .onSuccess {
                    _successMessage.value = "User blocked successfully"
                    // Refresh user details if currently viewing
                    loadUserDetails(userId)
                    // Refresh all users list
                    loadAllUsers()
                }
                .onFailure { e ->
                    _error.value = "Failed to block user: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Unblock a user
     * Requirements: 1.4
     */
    fun unblockUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.unblockUser(userId)
                .onSuccess {
                    _successMessage.value = "User unblocked successfully"
                    // Refresh user details if currently viewing
                    loadUserDetails(userId)
                    // Refresh all users list
                    loadAllUsers()
                }
                .onFailure { e ->
                    _error.value = "Failed to unblock user: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Update user role
     * Requirements: 1.5
     */
    fun updateUserRoleEnhanced(userId: String, role: UserRole) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateUserRole(userId, role)
                .onSuccess {
                    _successMessage.value = "User role updated successfully"
                    // Refresh user details if currently viewing
                    loadUserDetails(userId)
                    // Refresh all users list
                    loadAllUsers()
                }
                .onFailure { e ->
                    _error.value = "Failed to update user role: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Update user details
     * Requirements: 1.6
     */
    fun updateUserDetailsEnhanced(userId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateUserDetails(userId, updates)
                .onSuccess {
                    _successMessage.value = "User details updated successfully"
                    // Refresh user details if currently viewing
                    loadUserDetails(userId)
                    // Refresh all users list
                    loadAllUsers()
                }
                .onFailure { e ->
                    _error.value = "Failed to update user details: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Simple updateUser method for basic user updates
     * Requirements: 7.2, 7.3, 7.4
     */
    fun updateUser(userId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateUser(userId, updates)
                .onSuccess {
                    _successMessage.value = "User updated successfully"
                    // Refresh all users list
                    loadAllUsers()
                }
                .onFailure { e ->
                    _error.value = "Failed to update user: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Delete a user
     * Requirements: 6.6, 6.7, 6.11
     */
    fun deleteUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteUser(userId)
                .onSuccess {
                    _successMessage.value = "User deleted successfully from Firestore. Note: Firebase Authentication account must be deleted separately."
                    // Refresh all users list
                    loadAllUsers()
                }
                .onFailure { e ->
                    _error.value = "Failed to delete user: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Search users with query
     * Requirements: 1.9
     */
    fun searchUsersEnhanced(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.searchUsers(query)
                .onSuccess { users ->
                    _filteredUsers.value = users
                }
                .onFailure { e ->
                    _error.value = "Failed to search users: ${e.message}"
                    _filteredUsers.value = emptyList()
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Load user analytics
     * Requirements: 1.7
     */
    fun loadUserAnalytics() {
        viewModelScope.launch {
            repository.getUserAnalytics()
                .catch { e -> 
                    _error.value = "Failed to load user analytics: ${e.message}"
                }
                .collect { analytics ->
                    _userAnalytics.value = analytics
                }
        }
    }
    
    // ========== Enhanced Item Management Methods (Task 8.2) ==========
    // Requirements: 2.1-2.9
    
    /**
     * Load detailed item information
     * Requirements: 2.2, 2.3
     */
    fun loadItemDetails(itemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getItemDetails(itemId)
                .onSuccess { item ->
                    _itemDetails.value = item
                }
                .onFailure { e ->
                    _error.value = "Failed to load item details: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Update item details
     * Requirements: 2.4
     */
    fun updateItemDetailsEnhanced(itemId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateItemDetails(itemId, updates)
                .onSuccess {
                    _successMessage.value = "Item details updated successfully"
                    // Refresh item details if currently viewing
                    loadItemDetails(itemId)
                    // Refresh all items list
                    loadAllItemsWithStatus()
                }
                .onFailure { e ->
                    _error.value = "Failed to update item details: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Simple updateItem method for basic item updates
     * Requirements: 7.2, 7.3, 7.4
     */
    fun updateItem(itemId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateItem(itemId, updates)
                .onSuccess {
                    _successMessage.value = "Item updated successfully"
                    // Refresh all items list
                    loadAllItems()
                }
                .onFailure { e ->
                    _error.value = "Failed to update item: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Update item status with history tracking
     * Requirements: 2.5
     */
    fun updateItemStatusEnhanced(itemId: String, newStatus: ItemStatus, reason: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateItemStatus(itemId, newStatus, reason)
                .onSuccess {
                    _successMessage.value = "Item status updated successfully"
                    // Refresh item details if currently viewing
                    loadItemDetails(itemId)
                    // Refresh all items list
                    loadAllItemsWithStatus()
                }
                .onFailure { e ->
                    _error.value = "Failed to update item status: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Update item status (simplified version for callback usage)
     * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5
     */
    fun updateItemStatus(itemId: String, newStatus: ItemStatus, reason: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateItemStatus(itemId, newStatus, reason)
                .onSuccess {
                    _successMessage.value = "Item status updated successfully"
                    // Refresh item details if currently viewing
                    loadItemDetails(itemId)
                    // Refresh all items list
                    loadAllItemsWithStatus()
                }
                .onFailure { e ->
                    _error.value = "Failed to update item status: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Delete an item
     * Requirements: 2.6
     */
    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteItem(itemId)
                .onSuccess {
                    _successMessage.value = "Item deleted successfully"
                    // Refresh all items list
                    loadAllItemsWithStatus()
                }
                .onFailure { e ->
                    _error.value = "Failed to delete item: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Search items with advanced filters
     * Requirements: 2.7
     */
    fun searchItemsEnhanced(query: String, filters: Map<String, String> = emptyMap()) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.searchItems(query, filters)
                .onSuccess { items ->
                    _allItemsWithStatus.value = items
                }
                .onFailure { e ->
                    _error.value = "Failed to search items: ${e.message}"
                    _allItemsWithStatus.value = emptyList()
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Load all items with status information
     * Requirements: 2.1, 2.8
     */
    fun loadAllItemsWithStatus() {
        viewModelScope.launch {
            repository.getAllItemsWithStatus()
                .catch { e -> 
                    _error.value = "Failed to load items: ${e.message}"
                }
                .collect { items ->
                    _allItemsWithStatus.value = items
                }
        }
    }
    
    // ========== Donation Management Methods (Task 8.3) ==========
    // Requirements: 3.1-3.9
    // Note: Methods moved to avoid duplication - see below after Notification Management section
    
    // ========== Activity Log Methods (Task 8.4) ==========
    // Requirements: 5.1-5.11
    
    /**
     * Load activity logs with optional filters
     * Requirements: 5.3, 5.4
     */
    fun loadActivityLogs(limit: Int = 50, filters: Map<String, String> = emptyMap()) {
        viewModelScope.launch {
            repository.getActivityLogs(limit, filters)
                .catch { e -> 
                    _error.value = "Failed to load activity logs: ${e.message}"
                }
                .collect { logs ->
                    _activityLogs.value = logs
                }
        }
    }
    
    /**
     * Search activity logs with query and filters
     * Requirements: 5.5
     */
    fun searchActivityLogs(query: String, filters: Map<String, String> = emptyMap()) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.searchActivityLogs(query, filters)
                .onSuccess { logs ->
                    _activityLogs.value = logs
                }
                .onFailure { e ->
                    _error.value = "Failed to search activity logs: ${e.message}"
                    _activityLogs.value = emptyList()
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Update activity log filters
     * Requirements: 5.4
     */
    fun updateActivityLogFilters(filters: Map<String, String>) {
        _activityLogFilters.value = filters
        // Reload logs with new filters
        loadActivityLogs(50, filters)
    }
    
    /**
     * Clear activity log filters
     * Requirements: 5.4
     */
    fun clearActivityLogFilters() {
        _activityLogFilters.value = emptyMap()
        // Reload logs without filters
        loadActivityLogs(50, emptyMap())
    }
    
    // ========== Notification Management Methods (Task 8.5) ==========
    // Requirements: 6.1-6.12
    
    /**
     * Send push notification
     * Requirements: 6.4, 6.7
     */
    fun sendNotification(notification: PushNotification) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.sendPushNotification(notification)
                .onSuccess {
                    _successMessage.value = "Notification sent successfully"
                    // Refresh notification history
                    loadNotificationHistory()
                }
                .onFailure { e ->
                    _error.value = "Failed to send notification: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Load notification history
     * Requirements: 6.9
     */
    fun loadNotificationHistory(limit: Int = 50) {
        viewModelScope.launch {
            repository.getNotificationHistory(limit)
                .catch { e -> 
                    _error.value = "Failed to load notification history: ${e.message}"
                }
                .collect { notifications ->
                    _notificationHistory.value = notifications
                }
        }
    }
    
    /**
     * Load notification statistics
     * Requirements: 6.9
     */
    fun loadNotificationStats(notificationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getNotificationStats(notificationId)
                .onSuccess { stats ->
                    _notificationStats.value = stats
                }
                .onFailure { e ->
                    _error.value = "Failed to load notification stats: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Schedule notification for later delivery
     * Requirements: 6.7, 6.8
     */
    fun scheduleNotification(notification: PushNotification) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.scheduleNotification(notification)
                .onSuccess {
                    _successMessage.value = "Notification scheduled successfully"
                    // Refresh notification history
                    loadNotificationHistory()
                }
                .onFailure { e ->
                    _error.value = "Failed to schedule notification: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    // ========== Donation Management Methods (Task 8.3) ==========
    // Requirements: 3.1-3.9, 5.1, 5.2, 5.3
    
    /**
     * Load donation queue with real-time updates
     * Requirements: 3.2, 5.1, 5.2, 5.3
     */
    fun loadDonationQueue() {
        viewModelScope.launch {
            android.util.Log.d("AdminDashboardViewModel", "loadDonationQueue: Starting to load donations")
            _isLoading.value = true
            repository.getDonationQueue()
                .catch { e -> 
                    android.util.Log.e("AdminDashboardViewModel", "loadDonationQueue: Error loading donations", e)
                    _error.value = "Failed to load donation queue: ${e.message}"
                    _isLoading.value = false
                }
                .collect { donations ->
                    android.util.Log.d("AdminDashboardViewModel", "loadDonationQueue: Loaded ${donations.size} donations")
                    _donationQueue.value = donations
                    _isLoading.value = false
                }
        }
    }
    
    /**
     * Mark item as ready for donation
     * Requirements: 3.4
     */
    fun markItemReadyForDonation(itemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.markItemReadyForDonation(itemId)
                .onSuccess {
                    _successMessage.value = "Item marked as ready for donation"
                    // Refresh donation queue
                    loadDonationQueue()
                    // Refresh donation stats
                    loadDonationStats(DateRange(0, System.currentTimeMillis()))
                }
                .onFailure { e ->
                    _error.value = "Failed to mark item ready for donation: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Mark item as donated with recipient and value
     * Requirements: 3.5
     */
    fun markItemAsDonated(itemId: String, recipient: String, value: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.markItemAsDonated(itemId, recipient, value)
                .onSuccess {
                    _successMessage.value = "Item marked as donated successfully"
                    // Refresh donation queue
                    loadDonationQueue()
                    // Refresh donation stats
                    loadDonationStats(DateRange(0, System.currentTimeMillis()))
                }
                .onFailure { e ->
                    _error.value = "Failed to mark item as donated: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Load donation statistics
     * Requirements: 3.6, 3.7
     */
    fun loadDonationStats(dateRange: DateRange) {
        viewModelScope.launch {
            repository.getDonationStats(dateRange)
                .catch { e -> 
                    _error.value = "Failed to load donation stats: ${e.message}"
                }
                .collect { stats ->
                    _donationStats.value = stats
                }
        }
    }
    
    /**
     * Automatically flag old items (1+ year old) for donation
     * Requirements: 3.1, 3.2
     */
    fun flagOldItemsForDonation() {
        viewModelScope.launch {
            android.util.Log.d("AdminDashboardViewModel", "flagOldItemsForDonation: Starting automatic flagging")
            _isLoading.value = true
            repository.checkAndFlagOldItems()
                .onSuccess { flaggedCount ->
                    android.util.Log.d("AdminDashboardViewModel", "flagOldItemsForDonation: Flagged $flaggedCount items")
                    _successMessage.value = "Automatically flagged $flaggedCount items for donation"
                    // Refresh donation queue to show newly flagged items
                    loadDonationQueue()
                }
                .onFailure { e ->
                    android.util.Log.e("AdminDashboardViewModel", "flagOldItemsForDonation: Error", e)
                    _error.value = "Failed to flag old items: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    // ========== Export Management Methods (Task 8.6) ==========
    // Requirements: 4.1-4.9
    
    /**
     * Export data with progress tracking
     * Requirements: 4.1, 4.2, 4.3
     */
    fun exportData(request: ExportRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _exportProgress.value = 0
            
            try {
                // Update progress to indicate start
                _exportProgress.value = 10
                
                repository.exportData(request)
                    .onSuccess { fileUrl ->
                        _exportProgress.value = 100
                        _exportResult.value = fileUrl
                        _successMessage.value = "Export completed successfully"
                        // Refresh export history
                        loadExportHistory()
                    }
                    .onFailure { e ->
                        _exportProgress.value = 0
                        _error.value = "Export failed: ${e.message}"
                    }
            } catch (e: Exception) {
                _exportProgress.value = 0
                _error.value = "Export error: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Generate PDF report
     * Requirements: 4.2, 4.8
     */
    fun generatePdfReport(request: ExportRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _exportProgress.value = 0
            
            try {
                _exportProgress.value = 10
                
                repository.generatePdfReport(request)
                    .onSuccess { fileUrl ->
                        _exportProgress.value = 100
                        _exportResult.value = fileUrl
                        _successMessage.value = "PDF report generated successfully"
                        // Refresh export history
                        loadExportHistory()
                    }
                    .onFailure { e ->
                        _exportProgress.value = 0
                        _error.value = "PDF generation failed: ${e.message}"
                    }
            } catch (e: Exception) {
                _exportProgress.value = 0
                _error.value = "PDF generation error: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Generate CSV export
     * Requirements: 4.3, 4.9
     */
    fun generateCsvExport(request: ExportRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _exportProgress.value = 0
            
            try {
                _exportProgress.value = 10
                
                repository.generateCsvExport(request)
                    .onSuccess { fileUrl ->
                        _exportProgress.value = 100
                        _exportResult.value = fileUrl
                        _successMessage.value = "CSV export generated successfully"
                        // Refresh export history
                        loadExportHistory()
                    }
                    .onFailure { e ->
                        _exportProgress.value = 0
                        _error.value = "CSV generation failed: ${e.message}"
                    }
            } catch (e: Exception) {
                _exportProgress.value = 0
                _error.value = "CSV generation error: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Load export history
     * Requirements: 4.1
     */
    fun loadExportHistory() {
        viewModelScope.launch {
            repository.getExportHistory()
                .catch { e -> 
                    _error.value = "Failed to load export history: ${e.message}"
                }
                .collect { history ->
                    _exportHistory.value = history
                }
        }
    }
    
    /**
     * Update export progress manually (for UI feedback)
     * Requirements: 4.1
     */
    fun updateExportProgress(progress: Int) {
        _exportProgress.value = progress.coerceIn(0, 100)
    }
    
    /**
     * Clear export result
     */
    fun clearExportResult() {
        _exportResult.value = ""
        _exportProgress.value = 0
    }
    
    // ========== Test Data Generation Methods ==========
    // Requirements: 3.1, 3.2, 3.3
    
    /**
     * Generate old test items for donation workflow testing
     * Creates items at different ages (365 days, 400 days, 2 years, etc.)
     * with various statuses for comprehensive testing
     * Requirements: 3.1, 3.2, 3.3
     */
    fun generateOldTestItems() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.generateOldTestItems()
                .onSuccess { count ->
                    _successMessage.value = "Successfully generated $count old test items for donation testing"
                    // Refresh data to show new items
                    loadDashboardData()
                }
                .onFailure { e ->
                    _error.value = "Failed to generate old test items: ${e.message}"
                }
            _isLoading.value = false
        }
    }
    
    /**
     * Delete all test items created by the test data generator
     * Useful for cleanup after testing
     * Requirements: 3.1, 3.2, 3.3
     */
    fun deleteTestItems() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteTestItems()
                .onSuccess { count ->
                    _successMessage.value = "Successfully deleted $count test items"
                    // Refresh data to reflect deletion
                    loadDashboardData()
                }
                .onFailure { e ->
                    _error.value = "Failed to delete test items: ${e.message}"
                }
            _isLoading.value = false
        }
    }
}
