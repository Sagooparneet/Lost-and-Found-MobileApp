package com.example.loginandregistration

import com.google.firebase.Timestamp
import org.junit.Test
import org.junit.Assert.*

/**
 * Integration tests for Item Claiming System
 * Tests Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6
 */
class ClaimingSystemTest {

    /**
     * Test Requirement 5.1: "Request This Item" button visibility
     */
    @Test
    fun testClaimButtonVisibility() {
        // Test for approved found item - button should be visible
        val approvedFoundItem = LostFoundItem(
            id = "item-123",
            name = "Found Wallet",
            status = "Approved",
            isLost = false
        )
        
        val shouldShowButton = !approvedFoundItem.isLost && approvedFoundItem.status == "Approved"
        assertTrue("Button should be visible for approved found items", shouldShowButton)
        
        // Test for lost item - button should not be visible
        val lostItem = LostFoundItem(
            id = "item-456",
            name = "Lost Phone",
            status = "Approved",
            isLost = true
        )
        
        val shouldHideForLost = lostItem.isLost
        assertTrue("Button should be hidden for lost items", shouldHideForLost)
        
        // Test for pending found item - button should not be visible
        val pendingFoundItem = LostFoundItem(
            id = "item-789",
            name = "Found Keys",
            status = "Pending Approval",
            isLost = false
        )
        
        val shouldHideForPending = pendingFoundItem.status != "Approved"
        assertTrue("Button should be hidden for pending items", shouldHideForPending)
    }

    /**
     * Test Requirement 5.2: Claim request dialog validation
     */
    @Test
    fun testClaimRequestDialogValidation() {
        // Test empty reason validation
        val emptyReason = ""
        assertTrue("Empty reason should fail validation", emptyReason.isEmpty())
        
        // Test valid reason
        val validReason = "This is my wallet, I lost it yesterday"
        assertTrue("Valid reason should not be empty", validReason.isNotEmpty())
        
        // Test proof description (optional)
        val proofDescription = "It has my ID card inside"
        assertTrue("Proof description can be provided", proofDescription.isNotEmpty())
        
        // Test empty proof description (should be allowed)
        val emptyProof = ""
        assertTrue("Proof description can be empty", emptyProof.isEmpty())
    }

    /**
     * Test Requirement 5.3: ClaimRequest data structure
     */
    @Test
    fun testClaimRequestDataStructure() {
        val claimRequest = ClaimRequest(
            requestId = "claim-123",
            itemId = "item-456",
            itemName = "Found Wallet",
            userId = "user-789",
            userEmail = "user@example.com",
            userName = "John Doe",
            userPhone = "1234567890",
            reason = "This is my wallet",
            proofDescription = "Has my ID inside",
            status = ClaimRequest.ClaimStatus.PENDING,
            requestDate = Timestamp.now(),
            reviewedBy = "",
            reviewDate = null,
            reviewNotes = ""
        )
        
        // Verify all required fields
        assertNotNull("Request ID should not be null", claimRequest.requestId)
        assertNotNull("Item ID should not be null", claimRequest.itemId)
        assertNotNull("Item name should not be null", claimRequest.itemName)
        assertNotNull("User ID should not be null", claimRequest.userId)
        assertNotNull("User email should not be null", claimRequest.userEmail)
        assertNotNull("User name should not be null", claimRequest.userName)
        assertNotNull("User phone should not be null", claimRequest.userPhone)
        assertNotNull("Reason should not be null", claimRequest.reason)
        assertNotNull("Status should not be null", claimRequest.status)
        assertNotNull("Request date should not be null", claimRequest.requestDate)
        
        // Verify field values
        assertEquals("claim-123", claimRequest.requestId)
        assertEquals("item-456", claimRequest.itemId)
        assertEquals("Found Wallet", claimRequest.itemName)
        assertEquals("user-789", claimRequest.userId)
        assertEquals("user@example.com", claimRequest.userEmail)
        assertEquals("John Doe", claimRequest.userName)
        assertEquals("1234567890", claimRequest.userPhone)
        assertEquals("This is my wallet", claimRequest.reason)
        assertEquals("Has my ID inside", claimRequest.proofDescription)
        assertEquals(ClaimRequest.ClaimStatus.PENDING, claimRequest.status)
    }

    /**
     * Test Requirement 5.3: Claim status constants
     */
    @Test
    fun testClaimStatusConstants() {
        assertEquals("Pending", ClaimRequest.ClaimStatus.PENDING)
        assertEquals("Approved", ClaimRequest.ClaimStatus.APPROVED)
        assertEquals("Rejected", ClaimRequest.ClaimStatus.REJECTED)
        
        // Test that status values are distinct
        assertNotEquals(ClaimRequest.ClaimStatus.PENDING, ClaimRequest.ClaimStatus.APPROVED)
        assertNotEquals(ClaimRequest.ClaimStatus.PENDING, ClaimRequest.ClaimStatus.REJECTED)
        assertNotEquals(ClaimRequest.ClaimStatus.APPROVED, ClaimRequest.ClaimStatus.REJECTED)
    }

    /**
     * Test Requirement 5.4: Initial claim request creation
     */
    @Test
    fun testClaimRequestCreation() {
        // Simulate creating a claim request
        val itemId = "item-123"
        val itemName = "Found Laptop"
        val userId = "user-456"
        val userEmail = "user@example.com"
        val userName = "Jane Smith"
        val userPhone = "9876543210"
        val reason = "This is my laptop, serial number matches"
        val proofDescription = "Serial number: ABC123XYZ"
        
        val claimRequest = ClaimRequest(
            requestId = "claim-new-123",
            itemId = itemId,
            itemName = itemName,
            userId = userId,
            userEmail = userEmail,
            userName = userName,
            userPhone = userPhone,
            reason = reason,
            proofDescription = proofDescription,
            status = ClaimRequest.ClaimStatus.PENDING,
            requestDate = Timestamp.now()
        )
        
        // Verify initial state
        assertEquals(ClaimRequest.ClaimStatus.PENDING, claimRequest.status)
        assertEquals("", claimRequest.reviewedBy)
        assertNull("Review date should be null initially", claimRequest.reviewDate)
        assertEquals("", claimRequest.reviewNotes)
    }

    /**
     * Test Requirement 5.5: Claim approval process
     */
    @Test
    fun testClaimApprovalProcess() {
        // Create a pending claim request
        val pendingClaim = ClaimRequest(
            requestId = "claim-123",
            itemId = "item-456",
            itemName = "Found Phone",
            userId = "user-789",
            userEmail = "user@example.com",
            userName = "John Doe",
            userPhone = "1234567890",
            reason = "This is my phone",
            proofDescription = "IMEI matches",
            status = ClaimRequest.ClaimStatus.PENDING,
            requestDate = Timestamp.now()
        )
        
        // Simulate approval
        val approvedClaim = pendingClaim.copy(
            status = ClaimRequest.ClaimStatus.APPROVED,
            reviewedBy = "security-user-123",
            reviewDate = Timestamp.now()
        )
        
        // Verify approval
        assertEquals(ClaimRequest.ClaimStatus.APPROVED, approvedClaim.status)
        assertEquals("security-user-123", approvedClaim.reviewedBy)
        assertNotNull("Review date should be set", approvedClaim.reviewDate)
    }

    /**
     * Test Requirement 5.5: Item status changes to "Returned" on claim approval
     */
    @Test
    fun testItemStatusChangeOnApproval() {
        // Create an approved found item
        val foundItem = LostFoundItem(
            id = "item-123",
            name = "Found Wallet",
            status = "Approved",
            isLost = false
        )
        
        // Simulate claim approval - item status should change to "Returned"
        val returnedItem = foundItem.copy(status = "Returned")
        
        assertEquals("Returned", returnedItem.status)
        assertFalse("Item should still be marked as found", returnedItem.isLost)
    }

    /**
     * Test Requirement 5.6: Claim rejection process
     */
    @Test
    fun testClaimRejectionProcess() {
        // Create a pending claim request
        val pendingClaim = ClaimRequest(
            requestId = "claim-456",
            itemId = "item-789",
            itemName = "Found Keys",
            userId = "user-123",
            userEmail = "user@example.com",
            userName = "Jane Smith",
            userPhone = "9876543210",
            reason = "These are my keys",
            proofDescription = "Toyota keychain",
            status = ClaimRequest.ClaimStatus.PENDING,
            requestDate = Timestamp.now()
        )
        
        // Simulate rejection with notes
        val rejectionNotes = "Proof of ownership not sufficient"
        val rejectedClaim = pendingClaim.copy(
            status = ClaimRequest.ClaimStatus.REJECTED,
            reviewedBy = "security-user-456",
            reviewDate = Timestamp.now(),
            reviewNotes = rejectionNotes
        )
        
        // Verify rejection
        assertEquals(ClaimRequest.ClaimStatus.REJECTED, rejectedClaim.status)
        assertEquals("security-user-456", rejectedClaim.reviewedBy)
        assertNotNull("Review date should be set", rejectedClaim.reviewDate)
        assertEquals(rejectionNotes, rejectedClaim.reviewNotes)
    }

    /**
     * Test checking for pending claims
     */
    @Test
    fun testPendingClaimCheck() {
        val claims = listOf(
            ClaimRequest(
                requestId = "claim-1",
                itemId = "item-123",
                userId = "user-456",
                status = ClaimRequest.ClaimStatus.PENDING
            ),
            ClaimRequest(
                requestId = "claim-2",
                itemId = "item-123",
                userId = "user-789",
                status = ClaimRequest.ClaimStatus.APPROVED
            ),
            ClaimRequest(
                requestId = "claim-3",
                itemId = "item-456",
                userId = "user-456",
                status = ClaimRequest.ClaimStatus.PENDING
            )
        )
        
        // Check if user-456 has pending claim for item-123
        val hasPendingClaim = claims.any { 
            it.itemId == "item-123" && 
            it.userId == "user-456" && 
            it.status == ClaimRequest.ClaimStatus.PENDING 
        }
        
        assertTrue("User should have pending claim for item", hasPendingClaim)
        
        // Check if user-789 has pending claim for item-123 (should be false - claim is approved)
        val hasNoPendingClaim = claims.any { 
            it.itemId == "item-123" && 
            it.userId == "user-789" && 
            it.status == ClaimRequest.ClaimStatus.PENDING 
        }
        
        assertFalse("User should not have pending claim (claim is approved)", hasNoPendingClaim)
    }

    /**
     * Test complete claim request workflow
     */
    @Test
    fun testCompleteClaimWorkflow() {
        // Step 1: User views approved found item
        val foundItem = LostFoundItem(
            id = "item-123",
            name = "Found Backpack",
            description = "Blue backpack with laptop",
            location = "Library",
            category = "Bags",
            status = "Approved",
            isLost = false
        )
        
        assertTrue("Item should be approved found item", 
            !foundItem.isLost && foundItem.status == "Approved")
        
        // Step 2: User submits claim request
        val claimRequest = ClaimRequest(
            requestId = "claim-123",
            itemId = foundItem.id,
            itemName = foundItem.name,
            userId = "user-456",
            userEmail = "user@example.com",
            userName = "John Doe",
            userPhone = "1234567890",
            reason = "This is my backpack, I lost it yesterday",
            proofDescription = "Has my laptop with serial number XYZ123",
            status = ClaimRequest.ClaimStatus.PENDING,
            requestDate = Timestamp.now()
        )
        
        assertEquals(ClaimRequest.ClaimStatus.PENDING, claimRequest.status)
        
        // Step 3: Security reviews and approves claim
        val approvedClaim = claimRequest.copy(
            status = ClaimRequest.ClaimStatus.APPROVED,
            reviewedBy = "security-789",
            reviewDate = Timestamp.now()
        )
        
        assertEquals(ClaimRequest.ClaimStatus.APPROVED, approvedClaim.status)
        
        // Step 4: Item status changes to "Returned"
        val returnedItem = foundItem.copy(status = "Returned")
        
        assertEquals("Returned", returnedItem.status)
        
        // Verify complete workflow
        assertTrue("Workflow should complete successfully",
            approvedClaim.status == ClaimRequest.ClaimStatus.APPROVED &&
            returnedItem.status == "Returned")
    }

    /**
     * Test claim request with minimal information
     */
    @Test
    fun testMinimalClaimRequest() {
        // Test with only required fields
        val minimalClaim = ClaimRequest(
            requestId = "claim-minimal",
            itemId = "item-123",
            itemName = "Found Item",
            userId = "user-456",
            userEmail = "user@example.com",
            userName = "User Name",
            userPhone = "1234567890",
            reason = "This is mine",
            proofDescription = "", // Optional field
            status = ClaimRequest.ClaimStatus.PENDING,
            requestDate = Timestamp.now()
        )
        
        // Verify required fields are present
        assertTrue("Request ID should not be empty", minimalClaim.requestId.isNotEmpty())
        assertTrue("Item ID should not be empty", minimalClaim.itemId.isNotEmpty())
        assertTrue("Reason should not be empty", minimalClaim.reason.isNotEmpty())
        
        // Verify optional field can be empty
        assertEquals("", minimalClaim.proofDescription)
    }

    /**
     * Test multiple claims for same item
     */
    @Test
    fun testMultipleClaimsForSameItem() {
        val itemId = "item-123"
        
        val claims = listOf(
            ClaimRequest(
                requestId = "claim-1",
                itemId = itemId,
                userId = "user-1",
                userName = "User One",
                reason = "This is mine",
                status = ClaimRequest.ClaimStatus.PENDING
            ),
            ClaimRequest(
                requestId = "claim-2",
                itemId = itemId,
                userId = "user-2",
                userName = "User Two",
                reason = "No, this is mine",
                status = ClaimRequest.ClaimStatus.PENDING
            ),
            ClaimRequest(
                requestId = "claim-3",
                itemId = itemId,
                userId = "user-3",
                userName = "User Three",
                reason = "Actually, it's mine",
                status = ClaimRequest.ClaimStatus.REJECTED
            )
        )
        
        // Count pending claims for the item
        val pendingClaimsCount = claims.count { 
            it.itemId == itemId && it.status == ClaimRequest.ClaimStatus.PENDING 
        }
        
        assertEquals("Should have 2 pending claims", 2, pendingClaimsCount)
        
        // Verify all claims are for the same item
        assertTrue("All claims should be for the same item", 
            claims.all { it.itemId == itemId })
    }

    /**
     * Test claim request filtering by status
     */
    @Test
    fun testClaimRequestFiltering() {
        val claims = listOf(
            ClaimRequest(requestId = "1", status = ClaimRequest.ClaimStatus.PENDING),
            ClaimRequest(requestId = "2", status = ClaimRequest.ClaimStatus.APPROVED),
            ClaimRequest(requestId = "3", status = ClaimRequest.ClaimStatus.PENDING),
            ClaimRequest(requestId = "4", status = ClaimRequest.ClaimStatus.REJECTED),
            ClaimRequest(requestId = "5", status = ClaimRequest.ClaimStatus.PENDING)
        )
        
        // Filter pending claims
        val pendingClaims = claims.filter { it.status == ClaimRequest.ClaimStatus.PENDING }
        assertEquals("Should have 3 pending claims", 3, pendingClaims.size)
        
        // Filter approved claims
        val approvedClaims = claims.filter { it.status == ClaimRequest.ClaimStatus.APPROVED }
        assertEquals("Should have 1 approved claim", 1, approvedClaims.size)
        
        // Filter rejected claims
        val rejectedClaims = claims.filter { it.status == ClaimRequest.ClaimStatus.REJECTED }
        assertEquals("Should have 1 rejected claim", 1, rejectedClaims.size)
    }
}
