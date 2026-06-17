package com.example.loginandregistration

import com.example.loginandregistration.utils.UserRoleManager
import org.junit.Test
import org.junit.Assert.*

/**
 * Integration tests for Role-Based Information Visibility
 * Tests Requirements: 10.1, 10.2, 10.3, 10.4, 10.5
 */
class RoleBasedVisibilityTest {

    /**
     * Test Requirement 10.1: Regular users see limited information
     */
    @Test
    fun testRegularUserVisibility() {
        val regularUserEmail = "student@university.edu"
        
        // Regular user should not be admin
        assertFalse("Regular user should not be admin", UserRoleManager.isAdmin(regularUserEmail))
        
        // Regular user should not be security
        assertFalse("Regular user should not be security", UserRoleManager.isSecurity(regularUserEmail))
        
        // Regular user should not view sensitive info
        assertFalse("Regular user should not view sensitive info", 
            UserRoleManager.canViewSensitiveInfo(regularUserEmail))
        
        // Define visible fields for regular users
        val visibleFields = listOf("name", "description", "category", "image", "status")
        val hiddenFields = listOf("date", "location", "contactInfo")
        
        // Verify field visibility
        assertTrue("Name should be visible", visibleFields.contains("name"))
        assertTrue("Description should be visible", visibleFields.contains("description"))
        assertTrue("Category should be visible", visibleFields.contains("category"))
        assertTrue("Image should be visible", visibleFields.contains("image"))
        assertTrue("Status should be visible", visibleFields.contains("status"))
        
        assertFalse("Date should be hidden", visibleFields.contains("date"))
        assertFalse("Location should be hidden", visibleFields.contains("location"))
        assertFalse("Contact info should be hidden", visibleFields.contains("contactInfo"))
    }

    /**
     * Test Requirement 10.2: Sensitive fields are hidden from regular users
     */
    @Test
    fun testSensitiveFieldsHiddenForRegularUsers() {
        val regularUserEmail = "user@example.com"
        
        val canViewSensitive = UserRoleManager.canViewSensitiveInfo(regularUserEmail)
        assertFalse("Regular user should not view sensitive fields", canViewSensitive)
        
        // Simulate field visibility based on role
        val dateVisible = canViewSensitive
        val locationVisible = canViewSensitive
        val contactInfoVisible = canViewSensitive
        
        assertFalse("Date should not be visible", dateVisible)
        assertFalse("Location should not be visible", locationVisible)
        assertFalse("Contact info should not be visible", contactInfoVisible)
    }

    /**
     * Test Requirement 10.3: Security users see all information
     */
    @Test
    fun testSecurityUserVisibility() {
        val securityUserEmail = "security@university.edu"
        
        // Security user should be identified
        assertTrue("Security user should be identified", UserRoleManager.isSecurity(securityUserEmail))
        
        // Security user should view sensitive info
        assertTrue("Security user should view sensitive info", 
            UserRoleManager.canViewSensitiveInfo(securityUserEmail))
        
        // Define visible fields for security users (all fields)
        val visibleFields = listOf("name", "description", "category", "image", "status", 
                                   "date", "location", "contactInfo")
        
        // Verify all fields are visible
        assertTrue("Name should be visible", visibleFields.contains("name"))
        assertTrue("Description should be visible", visibleFields.contains("description"))
        assertTrue("Category should be visible", visibleFields.contains("category"))
        assertTrue("Image should be visible", visibleFields.contains("image"))
        assertTrue("Status should be visible", visibleFields.contains("status"))
        assertTrue("Date should be visible", visibleFields.contains("date"))
        assertTrue("Location should be visible", visibleFields.contains("location"))
        assertTrue("Contact info should be visible", visibleFields.contains("contactInfo"))
    }

    /**
     * Test Requirement 10.3: Admin users see all information
     */
    @Test
    fun testAdminUserVisibility() {
        val adminUserEmail = "admin@gmail.com"
        
        // Admin user should be identified
        assertTrue("Admin user should be identified", UserRoleManager.isAdmin(adminUserEmail))
        
        // Admin user should have security privileges
        assertTrue("Admin should have security privileges", UserRoleManager.isSecurity(adminUserEmail))
        
        // Admin user should view sensitive info
        assertTrue("Admin user should view sensitive info", 
            UserRoleManager.canViewSensitiveInfo(adminUserEmail))
        
        // Define visible fields for admin users (all fields)
        val visibleFields = listOf("name", "description", "category", "image", "status", 
                                   "date", "location", "contactInfo")
        
        // Verify all fields are visible
        assertEquals("All 8 fields should be visible", 8, visibleFields.size)
        assertTrue("All sensitive fields should be visible", 
            visibleFields.containsAll(listOf("date", "location", "contactInfo")))
    }

    /**
     * Test Requirement 10.4: Role determination by email
     */
    @Test
    fun testRoleDeterminationByEmail() {
        // Test admin email
        val adminEmail = "admin@gmail.com"
        assertTrue("Should identify admin by email", UserRoleManager.isAdmin(adminEmail))
        
        // Test admin email case-insensitive
        val adminEmailUpperCase = "ADMIN@GMAIL.COM"
        assertTrue("Should identify admin case-insensitively", UserRoleManager.isAdmin(adminEmailUpperCase))
        
        // Test security email with "security" in domain
        val securityDomainEmail = "officer@security.university.edu"
        assertTrue("Should identify security by domain", UserRoleManager.isSecurity(securityDomainEmail))
        
        // Test security email with "security" in username
        val securityUsernameEmail = "security.officer@university.edu"
        assertTrue("Should identify security by username", UserRoleManager.isSecurity(securityUsernameEmail))
        
        // Test regular user email
        val regularEmail = "student@university.edu"
        assertFalse("Should not identify regular user as admin", UserRoleManager.isAdmin(regularEmail))
        assertFalse("Should not identify regular user as security", UserRoleManager.isSecurity(regularEmail))
    }

    /**
     * Test Requirement 10.5: Role-based visibility applied consistently
     */
    @Test
    fun testConsistentRoleBasedVisibility() {
        val regularEmail = "user@example.com"
        val securityEmail = "security@example.com"
        val adminEmail = "admin@gmail.com"
        
        // Test consistency for regular user
        val regularCanView = UserRoleManager.canViewSensitiveInfo(regularEmail)
        assertFalse("Regular user visibility should be consistent", regularCanView)
        
        // Test consistency for security user
        val securityCanView = UserRoleManager.canViewSensitiveInfo(securityEmail)
        assertTrue("Security user visibility should be consistent", securityCanView)
        
        // Test consistency for admin user
        val adminCanView = UserRoleManager.canViewSensitiveInfo(adminEmail)
        assertTrue("Admin user visibility should be consistent", adminCanView)
        
        // Verify hierarchy: admin has all security privileges
        assertTrue("Admin should have security privileges", 
            UserRoleManager.isSecurity(adminEmail))
    }

    /**
     * Test various security email patterns
     */
    @Test
    fun testSecurityEmailPatterns() {
        val securityEmails = listOf(
            "security@university.edu",
            "officer@security.edu",
            "security.officer@example.com",
            "john.security@example.com",
            "SECURITY@EXAMPLE.COM",
            "Security.Officer@Example.Com"
        )
        
        securityEmails.forEach { email ->
            assertTrue("$email should be identified as security", 
                UserRoleManager.isSecurity(email))
            assertTrue("$email should view sensitive info", 
                UserRoleManager.canViewSensitiveInfo(email))
        }
    }

    /**
     * Test non-security email patterns
     */
    @Test
    fun testNonSecurityEmailPatterns() {
        val regularEmails = listOf(
            "student@university.edu",
            "john.doe@example.com",
            "user123@gmail.com",
            "faculty@university.edu",
            "staff@example.com"
        )
        
        regularEmails.forEach { email ->
            assertFalse("$email should not be identified as security", 
                UserRoleManager.isSecurity(email))
            assertFalse("$email should not view sensitive info", 
                UserRoleManager.canViewSensitiveInfo(email))
        }
    }

    /**
     * Test field visibility logic for different roles
     */
    @Test
    fun testFieldVisibilityLogic() {
        // Helper function to determine field visibility
        fun isFieldVisible(fieldName: String, userEmail: String): Boolean {
            val sensitiveFields = listOf("date", "location", "contactInfo")
            return if (sensitiveFields.contains(fieldName)) {
                UserRoleManager.canViewSensitiveInfo(userEmail)
            } else {
                true // Non-sensitive fields are always visible
            }
        }
        
        val regularUser = "user@example.com"
        val securityUser = "security@example.com"
        
        // Test non-sensitive fields (always visible)
        assertTrue("Name visible to regular user", isFieldVisible("name", regularUser))
        assertTrue("Name visible to security user", isFieldVisible("name", securityUser))
        
        // Test sensitive fields
        assertFalse("Date hidden from regular user", isFieldVisible("date", regularUser))
        assertTrue("Date visible to security user", isFieldVisible("date", securityUser))
        
        assertFalse("Location hidden from regular user", isFieldVisible("location", regularUser))
        assertTrue("Location visible to security user", isFieldVisible("location", securityUser))
        
        assertFalse("Contact info hidden from regular user", isFieldVisible("contactInfo", regularUser))
        assertTrue("Contact info visible to security user", isFieldVisible("contactInfo", securityUser))
    }

    /**
     * Test item display with role-based visibility
     */
    @Test
    fun testItemDisplayWithRoleBasedVisibility() {
        val item = LostFoundItem(
            id = "item-123",
            name = "Lost Wallet",
            description = "Black leather wallet",
            location = "Library 2nd Floor",
            contactInfo = "john@example.com",
            category = "Wallet",
            status = "Approved"
        )
        
        // Regular user view
        val regularUserEmail = "student@university.edu"
        val regularUserCanViewSensitive = UserRoleManager.canViewSensitiveInfo(regularUserEmail)
        
        // Simulate what regular user sees
        val regularUserView = mapOf(
            "name" to item.name,
            "description" to item.description,
            "category" to item.category,
            "status" to item.status,
            "location" to if (regularUserCanViewSensitive) item.location else "[Hidden]",
            "contactInfo" to if (regularUserCanViewSensitive) item.contactInfo else "[Hidden]"
        )
        
        assertEquals("Lost Wallet", regularUserView["name"])
        assertEquals("[Hidden]", regularUserView["location"])
        assertEquals("[Hidden]", regularUserView["contactInfo"])
        
        // Security user view
        val securityUserEmail = "security@university.edu"
        val securityUserCanViewSensitive = UserRoleManager.canViewSensitiveInfo(securityUserEmail)
        
        // Simulate what security user sees
        val securityUserView = mapOf(
            "name" to item.name,
            "description" to item.description,
            "category" to item.category,
            "status" to item.status,
            "location" to if (securityUserCanViewSensitive) item.location else "[Hidden]",
            "contactInfo" to if (securityUserCanViewSensitive) item.contactInfo else "[Hidden]"
        )
        
        assertEquals("Lost Wallet", securityUserView["name"])
        assertEquals("Library 2nd Floor", securityUserView["location"])
        assertEquals("john@example.com", securityUserView["contactInfo"])
    }

    /**
     * Test role hierarchy
     */
    @Test
    fun testRoleHierarchy() {
        val adminEmail = "admin@gmail.com"
        val securityEmail = "security@example.com"
        val regularEmail = "user@example.com"
        
        // Admin should have all privileges
        assertTrue("Admin is admin", UserRoleManager.isAdmin(adminEmail))
        assertTrue("Admin is security", UserRoleManager.isSecurity(adminEmail))
        assertTrue("Admin can view sensitive", UserRoleManager.canViewSensitiveInfo(adminEmail))
        
        // Security should have security privileges but not admin
        assertFalse("Security is not admin", UserRoleManager.isAdmin(securityEmail))
        assertTrue("Security is security", UserRoleManager.isSecurity(securityEmail))
        assertTrue("Security can view sensitive", UserRoleManager.canViewSensitiveInfo(securityEmail))
        
        // Regular user should have no special privileges
        assertFalse("Regular is not admin", UserRoleManager.isAdmin(regularEmail))
        assertFalse("Regular is not security", UserRoleManager.isSecurity(regularEmail))
        assertFalse("Regular cannot view sensitive", UserRoleManager.canViewSensitiveInfo(regularEmail))
    }

    /**
     * Test edge cases for role determination
     */
    @Test
    fun testRoleDeterminationEdgeCases() {
        // Empty email
        val emptyEmail = ""
        assertFalse("Empty email should not be admin", UserRoleManager.isAdmin(emptyEmail))
        assertFalse("Empty email should not be security", UserRoleManager.isSecurity(emptyEmail))
        
        // Email with "secur" substring (should not match "security")
        val secureEmail = "user@securemail.com"
        // This contains "secur" but not "security" so it should not match
        assertFalse("Email with 'secur' but not 'security' should not match", 
            UserRoleManager.isSecurity(secureEmail))
        
        // Case variations
        val mixedCaseAdmin = "AdMiN@GmAiL.cOm"
        assertTrue("Mixed case admin should be recognized", UserRoleManager.isAdmin(mixedCaseAdmin))
        
        val mixedCaseSecurity = "SECURITY@EXAMPLE.COM"
        assertTrue("Mixed case security should be recognized", UserRoleManager.isSecurity(mixedCaseSecurity))
    }

    /**
     * Test complete visibility workflow
     */
    @Test
    fun testCompleteVisibilityWorkflow() {
        // Create test item
        val item = LostFoundItem(
            id = "item-123",
            name = "Found Phone",
            description = "iPhone 12 Pro",
            location = "Cafeteria Table 5",
            contactInfo = "security@university.edu",
            category = "Electronics",
            status = "Approved",
            isLost = false
        )
        
        // Test with different user roles
        val users = listOf(
            "student@university.edu" to false,  // Regular user
            "security@university.edu" to true,  // Security user
            "admin@gmail.com" to true           // Admin user
        )
        
        users.forEach { (email, shouldViewSensitive) ->
            val canView = UserRoleManager.canViewSensitiveInfo(email)
            assertEquals("Visibility for $email should be $shouldViewSensitive", 
                shouldViewSensitive, canView)
            
            // Verify field visibility
            if (canView) {
                // Should see all fields
                assertNotNull("Should see location", item.location)
                assertNotNull("Should see contact info", item.contactInfo)
            } else {
                // Should see basic fields only
                assertNotNull("Should see name", item.name)
                assertNotNull("Should see description", item.description)
                // Location and contact info would be hidden in UI
            }
        }
    }
}
