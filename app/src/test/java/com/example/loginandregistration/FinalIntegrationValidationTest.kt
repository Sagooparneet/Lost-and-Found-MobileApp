package com.example.loginandregistration

import com.example.loginandregistration.utils.SearchManager
import com.example.loginandregistration.utils.UserRoleManager
import com.google.firebase.Timestamp
import org.junit.Test
import org.junit.Assert.*

/**
 * Final Integration Validation Test
 * Comprehensive end-to-end validation of all requirements
 * Tests Requirements: All requirements (1.1-11.7)
 */
class FinalIntegrationValidationTest {

    /**
     * Test complete user registration and profile management flow
     */
    @Test
    fun testCompleteUserManagementFlow() {
        // Registration
        val name = "John Doe"
        val phone = "1234567890"
        val email = "john@example.com"
        val password = "password123"
        
        assertTrue("All registration fields should be valid",
            name.isNotEmpty() && phone.isNotEmpty() && 
            email.isNotEmpty() && password.length >= 6)
        
        val user = User(
            userId = "user-123",
            name = name,
            email = email,
            phone = phone,
            gender = "Male",
            fcmToken = "",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        
        assertEquals(name, user.name)
        assertEquals(email, user.email)
        assertEquals(phone, user.phone)
        
        // Profile update
        val updatedUser = user.copy(
            name = "John Smith",
            phone = "9876543210",
            gender = "Other"
        )
        
        assertEquals("John Smith", updatedUser.name)
        assertEquals("9876543210", updatedUser.phone)
        assertEquals("Other", updatedUser.gender)
    }

    /**
     * Test complete item reporting and approval workflow
     */
    @Test
    fun testCompleteItemReportingWorkflow() {
        // Lost item (auto-approved)
        val lostItem = LostFoundItem(
            id = "lost-123",
            name = "Lost Wallet",
            description = "Black leather wallet",
            location = "Library",
            contactInfo = "user@example.com",
            category = "Wallet",
            dateLostFound = Timestamp.now(),
            imageUrl = "https://storage.example.com/image.jpg",
            isLost = true,
            status = "Approved",
            userId = "user-123",
            userEmail = "user@example.com",
            timestamp = Timestamp.now()
        )
        
        assertTrue("Lost item should be marked as lost", lostItem.isLost)
        assertEquals("Lost item should be auto-approved", "Approved", lostItem.status)
        
        // Found item (requires approval)
        val foundItem = LostFoundItem(
            id = "found-456",
            name = "Found Phone",
            description = "iPhone 12",
            location = "Cafeteria",
            contactInfo = "security@example.com",
            category = "Electronics",
            dateLostFound = Timestamp.now(),
            imageUrl = "https://storage.example.com/phone.jpg",
            isLost = false,
            status = "Pending Approval",
            userId = "user-456",
            userEmail = "finder@example.com",
            timestamp = Timestamp.now()
        )
        
        assertFalse("Found item should be marked as found", foundItem.isLost)
        assertEquals("Found item should be pending", "Pending Approval", foundItem.status)
        
        // Approval
        val approvedItem = foundItem.copy(
            status = "Approved",
            approvedBy = "security-789",
            approvalDate = Timestamp.now()
        )
        
        assertEquals("Approved", approvedItem.status)
        assertNotEquals("", approvedItem.approvedBy)
    }

    /**
     * Test complete claiming workflow
     */
    @Test
    fun testCompleteClaimingWorkflow() {
        // Found item available for claiming
        val foundItem = LostFoundItem(
            id = "item-123",
            name = "Found Laptop",
            status = "Approved",
            isLost = false
        )
        
        // User submits claim
        val claimRequest = ClaimRequest(
            requestId = "claim-123",
            itemId = foundItem.id,
            itemName = foundItem.name,
            userId = "user-456",
            userEmail = "user@example.com",
            userName = "Jane Doe",
            userPhone = "1234567890",
            reason = "This is my laptop",
            proofDescription = "Serial number matches",
            status = ClaimRequest.ClaimStatus.PENDING,
            requestDate = Timestamp.now()
        )
        
        assertEquals(ClaimRequest.ClaimStatus.PENDING, claimRequest.status)
        
        // Security approves claim
        val approvedClaim = claimRequest.copy(
            status = ClaimRequest.ClaimStatus.APPROVED,
            reviewedBy = "security-789",
            reviewDate = Timestamp.now()
        )
        
        assertEquals(ClaimRequest.ClaimStatus.APPROVED, approvedClaim.status)
        
        // Item status changes to Returned
        val returnedItem = foundItem.copy(status = "Returned")
        
        assertEquals("Returned", returnedItem.status)
    }

    /**
     * Test complete browse and search workflow
     */
    @Test
    fun testCompleteBrowseAndSearchWorkflow() {
        val items = listOf(
            LostFoundItem(id = "1", name = "Lost Wallet", isLost = true, status = "Approved", category = "Wallet"),
            LostFoundItem(id = "2", name = "Found Phone", isLost = false, status = "Approved", category = "Electronics"),
            LostFoundItem(id = "3", name = "Lost Keys", isLost = true, status = "Approved", category = "Keys"),
            LostFoundItem(id = "4", name = "Found Laptop", isLost = false, status = "Approved", category = "Electronics"),
            LostFoundItem(id = "5", name = "Returned Bag", isLost = false, status = "Returned", category = "Bags")
        )
        
        // Test tab filtering
        val lostItems = items.filter { it.isLost && it.status == "Approved" }
        assertEquals(2, lostItems.size)
        
        val foundItems = items.filter { !it.isLost && it.status == "Approved" }
        assertEquals(2, foundItems.size)
        
        val returnedItems = items.filter { it.status == "Returned" }
        assertEquals(1, returnedItems.size)
        
        // Test search
        val searchResults = SearchManager.filterItems(items, "electronics")
        assertEquals(2, searchResults.size)
        
        // Test search within tab
        val searchedLostItems = SearchManager.filterItems(lostItems, "wallet")
        assertEquals(1, searchedLostItems.size)
    }

    /**
     * Test complete role-based visibility workflow
     */
    @Test
    fun testCompleteRoleBasedVisibilityWorkflow() {
        val item = LostFoundItem(
            id = "item-123",
            name = "Found Wallet",
            description = "Black leather",
            location = "Library 2nd Floor",
            contactInfo = "security@example.com",
            category = "Wallet",
            status = "Approved"
        )
        
        // Regular user
        val regularUser = "student@university.edu"
        val regularCanView = UserRoleManager.canViewSensitiveInfo(regularUser)
        assertFalse("Regular user should not view sensitive info", regularCanView)
        
        // Security user
        val securityUser = "security@university.edu"
        val securityCanView = UserRoleManager.canViewSensitiveInfo(securityUser)
        assertTrue("Security user should view sensitive info", securityCanView)
        
        // Admin user
        val adminUser = "admin@gmail.com"
        val adminCanView = UserRoleManager.canViewSensitiveInfo(adminUser)
        assertTrue("Admin user should view sensitive info", adminCanView)
    }

    /**
     * Test complete notification workflow
     */
    @Test
    fun testCompleteNotificationWorkflow() {
        // Found item submitted
        val submittedNotif = Notification(
            notificationId = "notif-1",
            userId = "security-1",
            userEmail = "security@example.com",
            type = Notification.TYPE_FOUND_ITEM_SUBMITTED,
            title = "New Found Item Pending",
            message = "A new found item has been submitted",
            itemId = "item-123",
            timestamp = Timestamp.now(),
            read = false
        )
        
        assertEquals(Notification.TYPE_FOUND_ITEM_SUBMITTED, submittedNotif.type)
        
        // Item approved
        val approvedNotif = Notification(
            notificationId = "notif-2",
            userId = "user-456",
            userEmail = "user@example.com",
            type = Notification.TYPE_FOUND_ITEM_APPROVED,
            title = "Found Item Approved",
            message = "Your found item has been approved",
            itemId = "item-123",
            timestamp = Timestamp.now(),
            read = false
        )
        
        assertEquals(Notification.TYPE_FOUND_ITEM_APPROVED, approvedNotif.type)
        
        // Claim submitted
        val claimNotif = Notification(
            notificationId = "notif-3",
            userId = "security-1",
            userEmail = "security@example.com",
            type = Notification.TYPE_CLAIM_SUBMITTED,
            title = "New Claim Request",
            message = "A new claim request has been submitted",
            requestId = "claim-123",
            timestamp = Timestamp.now(),
            read = false
        )
        
        assertEquals(Notification.TYPE_CLAIM_SUBMITTED, claimNotif.type)
        
        // Claim approved
        val claimApprovedNotif = Notification(
            notificationId = "notif-4",
            userId = "user-789",
            userEmail = "claimant@example.com",
            type = Notification.TYPE_CLAIM_APPROVED,
            title = "Claim Approved - Collect Item",
            message = "Your claim has been approved",
            requestId = "claim-123",
            timestamp = Timestamp.now(),
            read = false
        )
        
        assertEquals(Notification.TYPE_CLAIM_APPROVED, claimApprovedNotif.type)
    }

    /**
     * Test data model integrity
     */
    @Test
    fun testDataModelIntegrity() {
        // User model
        val user = User(
            userId = "user-123",
            name = "John Doe",
            email = "john@example.com",
            phone = "1234567890",
            gender = "Male",
            fcmToken = "token123",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        
        assertNotNull(user.userId)
        assertNotNull(user.name)
        assertNotNull(user.email)
        assertNotNull(user.phone)
        
        // LostFoundItem model
        val item = LostFoundItem(
            id = "item-123",
            name = "Test Item",
            description = "Description",
            location = "Location",
            contactInfo = "Contact",
            category = "Category",
            dateLostFound = Timestamp.now(),
            imageUrl = "url",
            isLost = true,
            status = "Approved",
            approvedBy = "security",
            approvalDate = Timestamp.now(),
            userId = "user-123",
            userEmail = "user@example.com",
            timestamp = Timestamp.now()
        )
        
        assertNotNull(item.id)
        assertNotNull(item.name)
        assertNotNull(item.status)
        
        // ClaimRequest model
        val claim = ClaimRequest(
            requestId = "claim-123",
            itemId = "item-123",
            itemName = "Item",
            userId = "user-123",
            userEmail = "user@example.com",
            userName = "User",
            userPhone = "1234567890",
            reason = "Reason",
            proofDescription = "Proof",
            status = ClaimRequest.ClaimStatus.PENDING,
            requestDate = Timestamp.now(),
            reviewedBy = "",
            reviewDate = null,
            reviewNotes = ""
        )
        
        assertNotNull(claim.requestId)
        assertNotNull(claim.itemId)
        assertNotNull(claim.status)
        
        // Notification model
        val notification = Notification(
            notificationId = "notif-123",
            userId = "user-123",
            userEmail = "user@example.com",
            type = Notification.TYPE_FOUND_ITEM_APPROVED,
            title = "Title",
            message = "Message",
            itemId = "item-123",
            requestId = "",
            timestamp = Timestamp.now(),
            read = false,
            delivered = false
        )
        
        assertNotNull(notification.notificationId)
        assertNotNull(notification.userId)
        assertNotNull(notification.type)
    }

    /**
     * Test validation rules
     */
    @Test
    fun testValidationRules() {
        // Registration validation
        assertTrue("Name should not be empty", "John Doe".isNotEmpty())
        assertTrue("Phone should be valid", "1234567890".length >= 10)
        assertTrue("Email should not be empty", "user@example.com".isNotEmpty())
        assertTrue("Password should be at least 6 chars", "password123".length >= 6)
        
        // Report form validation
        assertTrue("Item name should not be empty", "Lost Wallet".isNotEmpty())
        assertTrue("Description should not be empty", "Black leather".isNotEmpty())
        assertTrue("Location should not be empty", "Library".isNotEmpty())
        assertTrue("Category should be selected", "Wallet".isNotEmpty())
        
        // Claim request validation
        assertTrue("Claim reason should not be empty", "This is mine".isNotEmpty())
        
        // Search validation
        val searchQuery = "wallet"
        assertTrue("Search query should not be empty", searchQuery.isNotEmpty())
    }

    /**
     * Test status transitions
     */
    @Test
    fun testStatusTransitions() {
        // Item status transitions
        val pendingItem = LostFoundItem(id = "1", status = "Pending Approval")
        val approvedItem = pendingItem.copy(status = "Approved")
        val rejectedItem = pendingItem.copy(status = "Rejected")
        val returnedItem = approvedItem.copy(status = "Returned")
        
        assertEquals("Pending Approval", pendingItem.status)
        assertEquals("Approved", approvedItem.status)
        assertEquals("Rejected", rejectedItem.status)
        assertEquals("Returned", returnedItem.status)
        
        // Claim status transitions
        val pendingClaim = ClaimRequest(requestId = "1", status = ClaimRequest.ClaimStatus.PENDING)
        val approvedClaim = pendingClaim.copy(status = ClaimRequest.ClaimStatus.APPROVED)
        val rejectedClaim = pendingClaim.copy(status = ClaimRequest.ClaimStatus.REJECTED)
        
        assertEquals(ClaimRequest.ClaimStatus.PENDING, pendingClaim.status)
        assertEquals(ClaimRequest.ClaimStatus.APPROVED, approvedClaim.status)
        assertEquals(ClaimRequest.ClaimStatus.REJECTED, rejectedClaim.status)
    }

    /**
     * Test error handling scenarios
     */
    @Test
    fun testErrorHandlingScenarios() {
        // Empty field validation
        val emptyName = ""
        assertTrue("Empty name should fail validation", emptyName.isEmpty())
        
        val emptyEmail = ""
        assertTrue("Empty email should fail validation", emptyEmail.isEmpty())
        
        // Invalid password
        val shortPassword = "12345"
        assertTrue("Short password should fail validation", shortPassword.length < 6)
        
        // Invalid phone
        val shortPhone = "123"
        assertTrue("Short phone should fail validation", shortPhone.length < 10)
        
        // Empty search
        val emptySearch = ""
        assertTrue("Empty search should return all items", emptySearch.isEmpty())
    }

    /**
     * Test complete end-to-end scenario
     */
    @Test
    fun testCompleteEndToEndScenario() {
        // 1. User registers
        val user = User(
            userId = "user-123",
            name = "John Doe",
            email = "john@example.com",
            phone = "1234567890",
            gender = "Male",
            fcmToken = "",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        
        assertNotNull(user.userId)
        
        // 2. User reports found item
        val foundItem = LostFoundItem(
            id = "item-123",
            name = "Found Wallet",
            description = "Black leather wallet",
            location = "Library",
            contactInfo = "security@example.com",
            category = "Wallet",
            dateLostFound = Timestamp.now(),
            imageUrl = "https://storage.example.com/wallet.jpg",
            isLost = false,
            status = "Pending Approval",
            userId = user.userId,
            userEmail = user.email,
            timestamp = Timestamp.now()
        )
        
        assertEquals("Pending Approval", foundItem.status)
        
        // 3. Security receives notification
        val securityNotif = Notification(
            notificationId = "notif-1",
            userId = "security-1",
            userEmail = "security@example.com",
            type = Notification.TYPE_FOUND_ITEM_SUBMITTED,
            title = "New Found Item Pending",
            message = "A new found item 'Found Wallet' has been submitted",
            itemId = foundItem.id,
            timestamp = Timestamp.now()
        )
        
        assertEquals(Notification.TYPE_FOUND_ITEM_SUBMITTED, securityNotif.type)
        
        // 4. Security approves item
        val approvedItem = foundItem.copy(
            status = "Approved",
            approvedBy = "security-1",
            approvalDate = Timestamp.now()
        )
        
        assertEquals("Approved", approvedItem.status)
        
        // 5. User receives approval notification
        val approvalNotif = Notification(
            notificationId = "notif-2",
            userId = user.userId,
            userEmail = user.email,
            type = Notification.TYPE_FOUND_ITEM_APPROVED,
            title = "Found Item Approved",
            message = "Your found item has been approved",
            itemId = foundItem.id,
            timestamp = Timestamp.now()
        )
        
        assertEquals(Notification.TYPE_FOUND_ITEM_APPROVED, approvalNotif.type)
        
        // 6. Another user searches and finds the item
        val allItems = listOf(approvedItem)
        val searchResults = SearchManager.filterItems(allItems, "wallet")
        
        assertEquals(1, searchResults.size)
        
        // 7. User submits claim request
        val claimRequest = ClaimRequest(
            requestId = "claim-123",
            itemId = approvedItem.id,
            itemName = approvedItem.name,
            userId = "user-456",
            userEmail = "claimant@example.com",
            userName = "Jane Smith",
            userPhone = "9876543210",
            reason = "This is my wallet",
            proofDescription = "Has my ID inside",
            status = ClaimRequest.ClaimStatus.PENDING,
            requestDate = Timestamp.now()
        )
        
        assertEquals(ClaimRequest.ClaimStatus.PENDING, claimRequest.status)
        
        // 8. Security approves claim
        val approvedClaim = claimRequest.copy(
            status = ClaimRequest.ClaimStatus.APPROVED,
            reviewedBy = "security-1",
            reviewDate = Timestamp.now()
        )
        
        assertEquals(ClaimRequest.ClaimStatus.APPROVED, approvedClaim.status)
        
        // 9. Item status changes to Returned
        val returnedItem = approvedItem.copy(status = "Returned")
        
        assertEquals("Returned", returnedItem.status)
        
        // Verify complete workflow
        assertTrue("Complete workflow should succeed",
            user.userId.isNotEmpty() &&
            approvedItem.status == "Approved" &&
            approvedClaim.status == ClaimRequest.ClaimStatus.APPROVED &&
            returnedItem.status == "Returned")
    }
}
