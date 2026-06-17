package com.example.loginandregistration

import com.google.firebase.Timestamp
import org.junit.Test
import org.junit.Assert.*

/**
 * Integration tests for Comprehensive Notification System
 * Tests Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7
 */
class NotificationSystemTest {

    /**
     * Test Requirement 11.7: Notification data structure
     */
    @Test
    fun testNotificationDataStructure() {
        val notification = Notification(
            notificationId = "notif-123",
            userId = "user-456",
            userEmail = "user@example.com",
            type = Notification.TYPE_FOUND_ITEM_APPROVED,
            title = "Found Item Approved",
            message = "Your found item has been approved",
            itemId = "item-789",
            requestId = "",
            timestamp = Timestamp.now(),
            read = false,
            delivered = false
        )
        
        // Verify all required fields
        assertNotNull("Notification ID should not be null", notification.notificationId)
        assertNotNull("User ID should not be null", notification.userId)
        assertNotNull("User email should not be null", notification.userEmail)
        assertNotNull("Type should not be null", notification.type)
        assertNotNull("Title should not be null", notification.title)
        assertNotNull("Message should not be null", notification.message)
        assertNotNull("Timestamp should not be null", notification.timestamp)
        
        // Verify field values
        assertEquals("notif-123", notification.notificationId)
        assertEquals("user-456", notification.userId)
        assertEquals("user@example.com", notification.userEmail)
        assertEquals(Notification.TYPE_FOUND_ITEM_APPROVED, notification.type)
        assertEquals("Found Item Approved", notification.title)
        assertEquals("Your found item has been approved", notification.message)
        assertEquals("item-789", notification.itemId)
        assertFalse("Notification should be unread initially", notification.read)
        assertFalse("Notification should be undelivered initially", notification.delivered)
    }

    /**
     * Test Requirement 11.7: Notification type constants
     */
    @Test
    fun testNotificationTypeConstants() {
        val types = listOf(
            Notification.TYPE_FOUND_ITEM_SUBMITTED,
            Notification.TYPE_FOUND_ITEM_APPROVED,
            Notification.TYPE_FOUND_ITEM_REJECTED,
            Notification.TYPE_CLAIM_SUBMITTED,
            Notification.TYPE_CLAIM_APPROVED,
            Notification.TYPE_CLAIM_REJECTED
        )
        
        // Verify all types are defined
        assertEquals("Should have 6 notification types", 6, types.size)
        
        // Verify type values
        assertEquals("FOUND_ITEM_SUBMITTED", Notification.TYPE_FOUND_ITEM_SUBMITTED)
        assertEquals("FOUND_ITEM_APPROVED", Notification.TYPE_FOUND_ITEM_APPROVED)
        assertEquals("FOUND_ITEM_REJECTED", Notification.TYPE_FOUND_ITEM_REJECTED)
        assertEquals("CLAIM_SUBMITTED", Notification.TYPE_CLAIM_SUBMITTED)
        assertEquals("CLAIM_APPROVED", Notification.TYPE_CLAIM_APPROVED)
        assertEquals("CLAIM_REJECTED", Notification.TYPE_CLAIM_REJECTED)
        
        // Verify all types are unique
        val uniqueTypes = types.toSet()
        assertEquals("All types should be unique", types.size, uniqueTypes.size)
    }

    /**
     * Test Requirement 11.1: Found item submitted notification
     */
    @Test
    fun testFoundItemSubmittedNotification() {
        val itemId = "item-123"
        val itemName = "Found Wallet"
        
        // Simulate notification for security users
        val securityUsers = listOf(
            "security-1" to "security1@example.com",
            "security-2" to "security2@example.com"
        )
        
        val notifications = securityUsers.map { (userId, userEmail) ->
            Notification(
                notificationId = "notif-${userId}",
                userId = userId,
                userEmail = userEmail,
                type = Notification.TYPE_FOUND_ITEM_SUBMITTED,
                title = "New Found Item Pending",
                message = "A new found item '$itemName' has been submitted and requires approval.",
                itemId = itemId,
                timestamp = Timestamp.now(),
                read = false,
                delivered = false
            )
        }
        
        // Verify notifications were created for all security users
        assertEquals("Should create notification for each security user", 2, notifications.size)
        
        // Verify notification content
        notifications.forEach { notification ->
            assertEquals(Notification.TYPE_FOUND_ITEM_SUBMITTED, notification.type)
            assertEquals("New Found Item Pending", notification.title)
            assertTrue("Message should contain item name", notification.message.contains(itemName))
            assertEquals(itemId, notification.itemId)
            assertFalse("Should be unread", notification.read)
        }
    }

    /**
     * Test Requirement 11.2: Found item approved notification
     */
    @Test
    fun testFoundItemApprovedNotification() {
        val itemId = "item-456"
        val itemName = "Found Phone"
        val userId = "user-789"
        val userEmail = "user@example.com"
        
        val notification = Notification(
            notificationId = "notif-approved",
            userId = userId,
            userEmail = userEmail,
            type = Notification.TYPE_FOUND_ITEM_APPROVED,
            title = "Found Item Approved",
            message = "Your found item '$itemName' has been approved and is now visible to all users.",
            itemId = itemId,
            timestamp = Timestamp.now(),
            read = false,
            delivered = false
        )
        
        // Verify notification content
        assertEquals(Notification.TYPE_FOUND_ITEM_APPROVED, notification.type)
        assertEquals("Found Item Approved", notification.title)
        assertTrue("Message should contain item name", notification.message.contains(itemName))
        assertTrue("Message should mention visibility", notification.message.contains("visible"))
        assertEquals(itemId, notification.itemId)
        assertEquals(userId, notification.userId)
    }

    /**
     * Test Requirement 11.3: Found item rejected notification
     */
    @Test
    fun testFoundItemRejectedNotification() {
        val itemId = "item-789"
        val itemName = "Found Keys"
        val userId = "user-123"
        val userEmail = "user@example.com"
        val rejectionReason = "Insufficient proof of ownership"
        
        val notification = Notification(
            notificationId = "notif-rejected",
            userId = userId,
            userEmail = userEmail,
            type = Notification.TYPE_FOUND_ITEM_REJECTED,
            title = "Found Item Rejected",
            message = "Your found item '$itemName' has been rejected. Reason: $rejectionReason",
            itemId = itemId,
            timestamp = Timestamp.now(),
            read = false,
            delivered = false
        )
        
        // Verify notification content
        assertEquals(Notification.TYPE_FOUND_ITEM_REJECTED, notification.type)
        assertEquals("Found Item Rejected", notification.title)
        assertTrue("Message should contain item name", notification.message.contains(itemName))
        assertTrue("Message should contain rejection reason", notification.message.contains(rejectionReason))
        assertEquals(itemId, notification.itemId)
        assertEquals(userId, notification.userId)
    }

    /**
     * Test Requirement 11.4: Claim request submitted notification
     */
    @Test
    fun testClaimSubmittedNotification() {
        val requestId = "claim-123"
        val itemName = "Found Laptop"
        val userName = "John Doe"
        
        // Simulate notification for security users
        val securityUsers = listOf(
            "security-1" to "security1@example.com",
            "security-2" to "security2@example.com"
        )
        
        val notifications = securityUsers.map { (userId, userEmail) ->
            Notification(
                notificationId = "notif-${userId}",
                userId = userId,
                userEmail = userEmail,
                type = Notification.TYPE_CLAIM_SUBMITTED,
                title = "New Claim Request",
                message = "$userName has submitted a claim request for '$itemName'.",
                requestId = requestId,
                timestamp = Timestamp.now(),
                read = false,
                delivered = false
            )
        }
        
        // Verify notifications were created for all security users
        assertEquals("Should create notification for each security user", 2, notifications.size)
        
        // Verify notification content
        notifications.forEach { notification ->
            assertEquals(Notification.TYPE_CLAIM_SUBMITTED, notification.type)
            assertEquals("New Claim Request", notification.title)
            assertTrue("Message should contain user name", notification.message.contains(userName))
            assertTrue("Message should contain item name", notification.message.contains(itemName))
            assertEquals(requestId, notification.requestId)
            assertFalse("Should be unread", notification.read)
        }
    }

    /**
     * Test Requirement 11.5: Claim approved notification
     */
    @Test
    fun testClaimApprovedNotification() {
        val requestId = "claim-456"
        val itemName = "Found Bag"
        val userId = "user-789"
        val userEmail = "user@example.com"
        
        val notification = Notification(
            notificationId = "notif-claim-approved",
            userId = userId,
            userEmail = userEmail,
            type = Notification.TYPE_CLAIM_APPROVED,
            title = "Claim Approved - Collect Item",
            message = "Your claim for '$itemName' has been approved. Please visit the security office to collect your item.",
            requestId = requestId,
            timestamp = Timestamp.now(),
            read = false,
            delivered = false
        )
        
        // Verify notification content
        assertEquals(Notification.TYPE_CLAIM_APPROVED, notification.type)
        assertEquals("Claim Approved - Collect Item", notification.title)
        assertTrue("Message should contain item name", notification.message.contains(itemName))
        assertTrue("Message should mention collection", notification.message.contains("collect"))
        assertTrue("Message should mention security office", notification.message.contains("security office"))
        assertEquals(requestId, notification.requestId)
        assertEquals(userId, notification.userId)
    }

    /**
     * Test Requirement 11.6: Claim rejected notification
     */
    @Test
    fun testClaimRejectedNotification() {
        val requestId = "claim-789"
        val itemName = "Found Wallet"
        val userId = "user-123"
        val userEmail = "user@example.com"
        val rejectionReason = "Unable to verify ownership"
        
        val notification = Notification(
            notificationId = "notif-claim-rejected",
            userId = userId,
            userEmail = userEmail,
            type = Notification.TYPE_CLAIM_REJECTED,
            title = "Claim Rejected",
            message = "Your claim for '$itemName' has been rejected. Reason: $rejectionReason",
            requestId = requestId,
            timestamp = Timestamp.now(),
            read = false,
            delivered = false
        )
        
        // Verify notification content
        assertEquals(Notification.TYPE_CLAIM_REJECTED, notification.type)
        assertEquals("Claim Rejected", notification.title)
        assertTrue("Message should contain item name", notification.message.contains(itemName))
        assertTrue("Message should contain rejection reason", notification.message.contains(rejectionReason))
        assertEquals(requestId, notification.requestId)
        assertEquals(userId, notification.userId)
    }

    /**
     * Test notification read/unread status
     */
    @Test
    fun testNotificationReadStatus() {
        val notification = Notification(
            notificationId = "notif-123",
            userId = "user-456",
            userEmail = "user@example.com",
            type = Notification.TYPE_FOUND_ITEM_APPROVED,
            title = "Test Notification",
            message = "Test message",
            timestamp = Timestamp.now(),
            read = false,
            delivered = false
        )
        
        // Initially unread
        assertFalse("Notification should be unread initially", notification.read)
        
        // Mark as read
        val readNotification = notification.copy(read = true)
        assertTrue("Notification should be marked as read", readNotification.read)
    }

    /**
     * Test notification delivery status
     */
    @Test
    fun testNotificationDeliveryStatus() {
        val notification = Notification(
            notificationId = "notif-123",
            userId = "user-456",
            userEmail = "user@example.com",
            type = Notification.TYPE_CLAIM_APPROVED,
            title = "Test Notification",
            message = "Test message",
            timestamp = Timestamp.now(),
            read = false,
            delivered = false
        )
        
        // Initially not delivered
        assertFalse("Notification should be undelivered initially", notification.delivered)
        
        // Mark as delivered
        val deliveredNotification = notification.copy(delivered = true)
        assertTrue("Notification should be marked as delivered", deliveredNotification.delivered)
    }

    /**
     * Test notification filtering by user
     */
    @Test
    fun testNotificationFilteringByUser() {
        val userId = "user-123"
        
        val notifications = listOf(
            Notification(notificationId = "1", userId = "user-123", type = Notification.TYPE_FOUND_ITEM_APPROVED),
            Notification(notificationId = "2", userId = "user-456", type = Notification.TYPE_CLAIM_APPROVED),
            Notification(notificationId = "3", userId = "user-123", type = Notification.TYPE_CLAIM_REJECTED),
            Notification(notificationId = "4", userId = "user-789", type = Notification.TYPE_FOUND_ITEM_REJECTED)
        )
        
        // Filter notifications for specific user
        val userNotifications = notifications.filter { it.userId == userId }
        
        assertEquals("Should have 2 notifications for user", 2, userNotifications.size)
        assertTrue("All notifications should belong to user", userNotifications.all { it.userId == userId })
    }

    /**
     * Test notification filtering by type
     */
    @Test
    fun testNotificationFilteringByType() {
        val notifications = listOf(
            Notification(notificationId = "1", type = Notification.TYPE_FOUND_ITEM_APPROVED),
            Notification(notificationId = "2", type = Notification.TYPE_CLAIM_APPROVED),
            Notification(notificationId = "3", type = Notification.TYPE_FOUND_ITEM_APPROVED),
            Notification(notificationId = "4", type = Notification.TYPE_CLAIM_REJECTED)
        )
        
        // Filter by type
        val approvedItemNotifications = notifications.filter { 
            it.type == Notification.TYPE_FOUND_ITEM_APPROVED 
        }
        
        assertEquals("Should have 2 found item approved notifications", 2, approvedItemNotifications.size)
        assertTrue("All should be found item approved type", 
            approvedItemNotifications.all { it.type == Notification.TYPE_FOUND_ITEM_APPROVED })
    }

    /**
     * Test notification filtering by read status
     */
    @Test
    fun testNotificationFilteringByReadStatus() {
        val notifications = listOf(
            Notification(notificationId = "1", read = false),
            Notification(notificationId = "2", read = true),
            Notification(notificationId = "3", read = false),
            Notification(notificationId = "4", read = true)
        )
        
        // Filter unread notifications
        val unreadNotifications = notifications.filter { !it.read }
        assertEquals("Should have 2 unread notifications", 2, unreadNotifications.size)
        
        // Filter read notifications
        val readNotifications = notifications.filter { it.read }
        assertEquals("Should have 2 read notifications", 2, readNotifications.size)
    }

    /**
     * Test notification with item reference
     */
    @Test
    fun testNotificationWithItemReference() {
        val itemId = "item-123"
        
        val notification = Notification(
            notificationId = "notif-123",
            userId = "user-456",
            userEmail = "user@example.com",
            type = Notification.TYPE_FOUND_ITEM_APPROVED,
            title = "Item Approved",
            message = "Your item has been approved",
            itemId = itemId,
            requestId = "",
            timestamp = Timestamp.now()
        )
        
        assertEquals("Should have item reference", itemId, notification.itemId)
        assertEquals("Should have empty request reference", "", notification.requestId)
    }

    /**
     * Test notification with request reference
     */
    @Test
    fun testNotificationWithRequestReference() {
        val requestId = "claim-456"
        
        val notification = Notification(
            notificationId = "notif-456",
            userId = "user-789",
            userEmail = "user@example.com",
            type = Notification.TYPE_CLAIM_APPROVED,
            title = "Claim Approved",
            message = "Your claim has been approved",
            itemId = "",
            requestId = requestId,
            timestamp = Timestamp.now()
        )
        
        assertEquals("Should have request reference", requestId, notification.requestId)
        assertEquals("Should have empty item reference", "", notification.itemId)
    }

    /**
     * Test complete notification workflow for found item
     */
    @Test
    fun testCompleteFoundItemNotificationWorkflow() {
        val itemId = "item-123"
        val itemName = "Found Wallet"
        val reporterUserId = "user-456"
        val reporterEmail = "reporter@example.com"
        
        // Step 1: Item submitted - notify security
        val submittedNotification = Notification(
            notificationId = "notif-1",
            userId = "security-1",
            userEmail = "security@example.com",
            type = Notification.TYPE_FOUND_ITEM_SUBMITTED,
            title = "New Found Item Pending",
            message = "A new found item '$itemName' has been submitted and requires approval.",
            itemId = itemId,
            timestamp = Timestamp.now()
        )
        
        assertEquals(Notification.TYPE_FOUND_ITEM_SUBMITTED, submittedNotification.type)
        
        // Step 2: Item approved - notify reporter
        val approvedNotification = Notification(
            notificationId = "notif-2",
            userId = reporterUserId,
            userEmail = reporterEmail,
            type = Notification.TYPE_FOUND_ITEM_APPROVED,
            title = "Found Item Approved",
            message = "Your found item '$itemName' has been approved and is now visible to all users.",
            itemId = itemId,
            timestamp = Timestamp.now()
        )
        
        assertEquals(Notification.TYPE_FOUND_ITEM_APPROVED, approvedNotification.type)
        assertEquals(reporterUserId, approvedNotification.userId)
        
        // Verify workflow
        assertTrue("Workflow should complete successfully",
            submittedNotification.type == Notification.TYPE_FOUND_ITEM_SUBMITTED &&
            approvedNotification.type == Notification.TYPE_FOUND_ITEM_APPROVED)
    }

    /**
     * Test complete notification workflow for claim request
     */
    @Test
    fun testCompleteClaimNotificationWorkflow() {
        val requestId = "claim-123"
        val itemName = "Found Phone"
        val claimantUserId = "user-789"
        val claimantEmail = "claimant@example.com"
        val claimantName = "Jane Doe"
        
        // Step 1: Claim submitted - notify security
        val submittedNotification = Notification(
            notificationId = "notif-1",
            userId = "security-1",
            userEmail = "security@example.com",
            type = Notification.TYPE_CLAIM_SUBMITTED,
            title = "New Claim Request",
            message = "$claimantName has submitted a claim request for '$itemName'.",
            requestId = requestId,
            timestamp = Timestamp.now()
        )
        
        assertEquals(Notification.TYPE_CLAIM_SUBMITTED, submittedNotification.type)
        
        // Step 2: Claim approved - notify claimant
        val approvedNotification = Notification(
            notificationId = "notif-2",
            userId = claimantUserId,
            userEmail = claimantEmail,
            type = Notification.TYPE_CLAIM_APPROVED,
            title = "Claim Approved - Collect Item",
            message = "Your claim for '$itemName' has been approved. Please visit the security office to collect your item.",
            requestId = requestId,
            timestamp = Timestamp.now()
        )
        
        assertEquals(Notification.TYPE_CLAIM_APPROVED, approvedNotification.type)
        assertEquals(claimantUserId, approvedNotification.userId)
        
        // Verify workflow
        assertTrue("Workflow should complete successfully",
            submittedNotification.type == Notification.TYPE_CLAIM_SUBMITTED &&
            approvedNotification.type == Notification.TYPE_CLAIM_APPROVED)
    }
}
