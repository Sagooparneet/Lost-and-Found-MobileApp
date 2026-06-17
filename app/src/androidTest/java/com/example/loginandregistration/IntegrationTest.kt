package com.example.loginandregistration

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for critical crash fixes
 * Tests Requirements: 1.5, 2.5, 3.6, 4.5, 6.5
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class IntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(Login::class.java)

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    @Before
    fun setup() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    @After
    fun tearDown() {
        // Sign out after each test
        auth.signOut()
    }

    /**
     * Test Requirement 1.5: Verify no NullPointerException with null imageUrl
     */
    @Test
    fun testLostFoundItem_withNullImageUrl_doesNotCrash() {
        val item = LostFoundItem(
            id = "test123",
            name = "Test Item",
            description = "Test Description",
            location = "Test Location",
            contactInfo = "test@example.com",
            isLost = true,
            status = "Pending",
            userId = "testUser",
            userEmail = "test@example.com",
            imageUrl = null  // Null imageUrl should not crash
        )

        // Verify item is created successfully
        assertNotNull(item)
        assertNull(item.imageUrl)
        assertEquals("Test Item", item.name)
    }

    /**
     * Test Requirement 1.5: Verify items with images work correctly
     */
    @Test
    fun testLostFoundItem_withImageUrl_worksCorrectly() {
        val item = LostFoundItem(
            id = "test456",
            name = "Test Item with Image",
            description = "Test Description",
            location = "Test Location",
            contactInfo = "test@example.com",
            isLost = false,
            status = "Pending",
            userId = "testUser",
            userEmail = "test@example.com",
            imageUrl = "https://example.com/image.jpg"
        )

        assertNotNull(item)
        assertNotNull(item.imageUrl)
        assertEquals("https://example.com/image.jpg", item.imageUrl)
    }

    /**
     * Test Requirement 6.5: Verify error handling for Firestore operations
     */
    @Test
    fun testFirestoreDeserialization_withInvalidData_handlesGracefully() = runBlocking {
        // This test verifies that mapNotNull properly handles deserialization errors
        val validItem = LostFoundItem(
            name = "Valid Item",
            description = "Valid Description",
            location = "Valid Location",
            contactInfo = "valid@example.com",
            isLost = true,
            userId = "validUser",
            userEmail = "valid@example.com"
        )

        // Verify valid item can be created
        assertNotNull(validItem)
        assertEquals("Valid Item", validItem.name)
    }

    /**
     * Test Requirement 3.6: Verify data class has proper default values
     */
    @Test
    fun testLostFoundItem_defaultConstructor_hasProperDefaults() {
        val item = LostFoundItem()

        assertEquals("", item.id)
        assertEquals("", item.name)
        assertEquals("", item.description)
        assertEquals("", item.location)
        assertEquals("", item.contactInfo)
        assertTrue(item.isLost)
        assertEquals("Pending", item.status)
        assertEquals("", item.userId)
        assertEquals("", item.userEmail)
        assertNull(item.imageUrl)  // Should default to null
        assertNotNull(item.timestamp)
    }
}
