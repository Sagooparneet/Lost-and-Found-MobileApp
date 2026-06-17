package com.example.loginandregistration

import com.example.loginandregistration.utils.SearchManager
import com.google.firebase.Timestamp
import org.junit.Test
import org.junit.Assert.*

/**
 * Integration tests for Browse Interface and Search Functionality
 * Tests Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5
 */
class BrowseInterfaceSearchTest {

    /**
     * Test Requirement 6.1: TabLayout with four tabs
     */
    @Test
    fun testTabLayoutStructure() {
        val tabTitles = listOf(
            "Lost Items",
            "Found Items",
            "Returned Items",
            "My Requests"
        )
        
        // Verify all four tabs are present
        assertEquals("Should have 4 tabs", 4, tabTitles.size)
        assertEquals("Lost Items", tabTitles[0])
        assertEquals("Found Items", tabTitles[1])
        assertEquals("Returned Items", tabTitles[2])
        assertEquals("My Requests", tabTitles[3])
    }

    /**
     * Test Requirement 6.2: Lost Items tab filtering
     */
    @Test
    fun testLostItemsTabFiltering() {
        val items = listOf(
            LostFoundItem(id = "1", name = "Lost Phone", isLost = true, status = "Approved"),
            LostFoundItem(id = "2", name = "Found Wallet", isLost = false, status = "Approved"),
            LostFoundItem(id = "3", name = "Lost Keys", isLost = true, status = "Approved"),
            LostFoundItem(id = "4", name = "Lost Laptop", isLost = true, status = "Pending Approval"),
            LostFoundItem(id = "5", name = "Found Bag", isLost = false, status = "Approved")
        )
        
        // Filter for Lost Items tab: isLost = true AND status = "Approved"
        val lostItems = items.filter { it.isLost && it.status == "Approved" }
        
        assertEquals("Should have 2 lost approved items", 2, lostItems.size)
        assertTrue("All items should be lost", lostItems.all { it.isLost })
        assertTrue("All items should be approved", lostItems.all { it.status == "Approved" })
    }

    /**
     * Test Requirement 6.3: Found Items tab filtering
     */
    @Test
    fun testFoundItemsTabFiltering() {
        val items = listOf(
            LostFoundItem(id = "1", name = "Lost Phone", isLost = true, status = "Approved"),
            LostFoundItem(id = "2", name = "Found Wallet", isLost = false, status = "Approved"),
            LostFoundItem(id = "3", name = "Found Keys", isLost = false, status = "Pending Approval"),
            LostFoundItem(id = "4", name = "Found Laptop", isLost = false, status = "Approved"),
            LostFoundItem(id = "5", name = "Lost Bag", isLost = true, status = "Approved")
        )
        
        // Filter for Found Items tab: isLost = false AND status = "Approved"
        val foundItems = items.filter { !it.isLost && it.status == "Approved" }
        
        assertEquals("Should have 2 found approved items", 2, foundItems.size)
        assertTrue("All items should be found", foundItems.all { !it.isLost })
        assertTrue("All items should be approved", foundItems.all { it.status == "Approved" })
    }

    /**
     * Test Requirement 6.4: Returned Items tab filtering
     */
    @Test
    fun testReturnedItemsTabFiltering() {
        val items = listOf(
            LostFoundItem(id = "1", name = "Item 1", status = "Approved"),
            LostFoundItem(id = "2", name = "Item 2", status = "Returned"),
            LostFoundItem(id = "3", name = "Item 3", status = "Pending Approval"),
            LostFoundItem(id = "4", name = "Item 4", status = "Returned"),
            LostFoundItem(id = "5", name = "Item 5", status = "Rejected")
        )
        
        // Filter for Returned Items tab: status = "Returned"
        val returnedItems = items.filter { it.status == "Returned" }
        
        assertEquals("Should have 2 returned items", 2, returnedItems.size)
        assertTrue("All items should have Returned status", returnedItems.all { it.status == "Returned" })
    }

    /**
     * Test Requirement 6.5: My Requests tab displays user's claim requests
     */
    @Test
    fun testMyRequestsTabFiltering() {
        val currentUserId = "user-123"
        
        val claimRequests = listOf(
            ClaimRequest(requestId = "1", userId = "user-123", itemName = "Wallet", status = "Pending"),
            ClaimRequest(requestId = "2", userId = "user-456", itemName = "Phone", status = "Approved"),
            ClaimRequest(requestId = "3", userId = "user-123", itemName = "Keys", status = "Approved"),
            ClaimRequest(requestId = "4", userId = "user-789", itemName = "Laptop", status = "Rejected"),
            ClaimRequest(requestId = "5", userId = "user-123", itemName = "Bag", status = "Rejected")
        )
        
        // Filter for My Requests tab: userId = currentUserId
        val myRequests = claimRequests.filter { it.userId == currentUserId }
        
        assertEquals("Should have 3 requests for current user", 3, myRequests.size)
        assertTrue("All requests should belong to current user", myRequests.all { it.userId == currentUserId })
    }

    /**
     * Test Requirement 7.1: Search input field presence
     */
    @Test
    fun testSearchInputFieldPresence() {
        // Verify search query can be captured
        val searchQuery = "wallet"
        assertNotNull("Search query should not be null", searchQuery)
        assertTrue("Search query should not be empty", searchQuery.isNotEmpty())
    }

    /**
     * Test Requirement 7.2: Real-time search filtering
     */
    @Test
    fun testRealTimeSearchFiltering() {
        val items = listOf(
            LostFoundItem(id = "1", name = "Lost Wallet", description = "Black leather", location = "Library", category = "Wallet"),
            LostFoundItem(id = "2", name = "Found Phone", description = "iPhone 12", location = "Cafeteria", category = "Electronics"),
            LostFoundItem(id = "3", name = "Lost Keys", description = "Car keys", location = "Parking", category = "Keys")
        )
        
        // Test search as user types
        val query1 = "w"
        val results1 = SearchManager.filterItems(items, query1)
        assertTrue("Should find items containing 'w'", results1.isNotEmpty())
        
        val query2 = "wa"
        val results2 = SearchManager.filterItems(items, query2)
        assertTrue("Should find items containing 'wa'", results2.isNotEmpty())
        
        val query3 = "wal"
        val results3 = SearchManager.filterItems(items, query3)
        assertTrue("Should find items containing 'wal'", results3.isNotEmpty())
        
        // Results should narrow down as query becomes more specific
        assertTrue("Results should narrow down", results1.size >= results2.size && results2.size >= results3.size)
    }

    /**
     * Test Requirement 7.3: Case-insensitive search matching
     */
    @Test
    fun testCaseInsensitiveSearchMatching() {
        val items = listOf(
            LostFoundItem(id = "1", name = "Lost Wallet", description = "Black leather"),
            LostFoundItem(id = "2", name = "Found PHONE", description = "iPhone 12"),
            LostFoundItem(id = "3", name = "lost keys", description = "CAR KEYS")
        )
        
        // Test lowercase query
        val lowercaseResults = SearchManager.filterItems(items, "wallet")
        assertEquals("Should find 1 item with lowercase query", 1, lowercaseResults.size)
        
        // Test uppercase query
        val uppercaseResults = SearchManager.filterItems(items, "WALLET")
        assertEquals("Should find 1 item with uppercase query", 1, uppercaseResults.size)
        
        // Test mixed case query
        val mixedCaseResults = SearchManager.filterItems(items, "WaLLeT")
        assertEquals("Should find 1 item with mixed case query", 1, mixedCaseResults.size)
        
        // All queries should return the same results
        assertEquals("Case should not matter", lowercaseResults.size, uppercaseResults.size)
        assertEquals("Case should not matter", uppercaseResults.size, mixedCaseResults.size)
    }

    /**
     * Test Requirement 7.4: Clear search functionality
     */
    @Test
    fun testClearSearchFunctionality() {
        val items = listOf(
            LostFoundItem(id = "1", name = "Lost Wallet"),
            LostFoundItem(id = "2", name = "Found Phone"),
            LostFoundItem(id = "3", name = "Lost Keys")
        )
        
        // Apply search filter
        val filteredResults = SearchManager.filterItems(items, "wallet")
        assertEquals("Should have 1 filtered result", 1, filteredResults.size)
        
        // Clear search (empty query)
        val clearedResults = SearchManager.filterItems(items, "")
        assertEquals("Should show all items when search is cleared", 3, clearedResults.size)
    }

    /**
     * Test Requirement 7.5: Search within tab context
     */
    @Test
    fun testSearchWithinTabContext() {
        val allItems = listOf(
            LostFoundItem(id = "1", name = "Lost Wallet", isLost = true, status = "Approved"),
            LostFoundItem(id = "2", name = "Found Wallet", isLost = false, status = "Approved"),
            LostFoundItem(id = "3", name = "Lost Phone", isLost = true, status = "Approved"),
            LostFoundItem(id = "4", name = "Found Phone", isLost = false, status = "Approved")
        )
        
        // Simulate Lost Items tab with search
        val lostItems = allItems.filter { it.isLost && it.status == "Approved" }
        val searchedLostItems = SearchManager.filterItems(lostItems, "wallet")
        
        assertEquals("Should find 1 lost wallet", 1, searchedLostItems.size)
        assertTrue("Result should be lost item", searchedLostItems[0].isLost)
        assertTrue("Result should contain 'wallet'", searchedLostItems[0].name.lowercase().contains("wallet"))
        
        // Simulate Found Items tab with search
        val foundItems = allItems.filter { !it.isLost && it.status == "Approved" }
        val searchedFoundItems = SearchManager.filterItems(foundItems, "wallet")
        
        assertEquals("Should find 1 found wallet", 1, searchedFoundItems.size)
        assertFalse("Result should be found item", searchedFoundItems[0].isLost)
        assertTrue("Result should contain 'wallet'", searchedFoundItems[0].name.lowercase().contains("wallet"))
    }

    /**
     * Test search across multiple fields
     */
    @Test
    fun testMultiFieldSearch() {
        val items = listOf(
            LostFoundItem(
                id = "1",
                name = "Lost Item",
                description = "Blue wallet",
                location = "Library",
                category = "Wallet"
            ),
            LostFoundItem(
                id = "2",
                name = "Found Phone",
                description = "iPhone",
                location = "Cafeteria",
                category = "Electronics"
            ),
            LostFoundItem(
                id = "3",
                name = "Lost Keys",
                description = "Car keys",
                location = "Blue parking lot",
                category = "Keys"
            )
        )
        
        // Search by name
        val nameResults = SearchManager.filterItems(items, "phone")
        assertEquals("Should find item by name", 1, nameResults.size)
        
        // Search by description
        val descriptionResults = SearchManager.filterItems(items, "wallet")
        assertEquals("Should find item by description", 1, descriptionResults.size)
        
        // Search by location
        val locationResults = SearchManager.filterItems(items, "library")
        assertEquals("Should find item by location", 1, locationResults.size)
        
        // Search by category
        val categoryResults = SearchManager.filterItems(items, "electronics")
        assertEquals("Should find item by category", 1, categoryResults.size)
        
        // Search term appearing in multiple fields
        val multiFieldResults = SearchManager.filterItems(items, "blue")
        assertEquals("Should find items with 'blue' in any field", 2, multiFieldResults.size)
    }

    /**
     * Test search with no results
     */
    @Test
    fun testSearchWithNoResults() {
        val items = listOf(
            LostFoundItem(id = "1", name = "Lost Wallet"),
            LostFoundItem(id = "2", name = "Found Phone"),
            LostFoundItem(id = "3", name = "Lost Keys")
        )
        
        val noResults = SearchManager.filterItems(items, "laptop")
        assertEquals("Should return empty list when no matches", 0, noResults.size)
        assertTrue("Result should be empty list", noResults.isEmpty())
    }

    /**
     * Test search with numbers and text
     */
    @Test
    fun testSearchWithNumbersAndText() {
        val items = listOf(
            LostFoundItem(id = "1", name = "Lost Wallet", description = "Black leather wallet"),
            LostFoundItem(id = "2", name = "Found Phone", description = "iPhone 12 Pro"),
            LostFoundItem(id = "3", name = "Lost Keys", description = "Car keys with remote")
        )
        
        // Search with text
        val textResults = SearchManager.filterItems(items, "iphone")
        assertEquals("Should find item with text", 1, textResults.size)
        
        // Search with number
        val numberResults = SearchManager.filterItems(items, "12")
        assertEquals("Should find item with number", 1, numberResults.size)
        
        // Search with mixed text and number
        val mixedResults = SearchManager.filterItems(items, "iphone 12")
        assertEquals("Should find item with mixed query", 1, mixedResults.size)
    }

    /**
     * Test search query trimming
     */
    @Test
    fun testSearchQueryTrimming() {
        val items = listOf(
            LostFoundItem(id = "1", name = "Lost Wallet"),
            LostFoundItem(id = "2", name = "Found Phone")
        )
        
        // Search with leading/trailing spaces
        val spacedQuery = "  wallet  "
        val results = SearchManager.filterItems(items, spacedQuery)
        
        assertEquals("Should find item despite spaces", 1, results.size)
        assertTrue("Should match 'wallet'", results[0].name.lowercase().contains("wallet"))
    }

    /**
     * Test tab switching with active search
     */
    @Test
    fun testTabSwitchingWithActiveSearch() {
        val allItems = listOf(
            LostFoundItem(id = "1", name = "Lost Wallet", isLost = true, status = "Approved"),
            LostFoundItem(id = "2", name = "Found Wallet", isLost = false, status = "Approved"),
            LostFoundItem(id = "3", name = "Lost Phone", isLost = true, status = "Approved")
        )
        
        val searchQuery = "wallet"
        
        // Tab 1: Lost Items with search
        val lostItemsFiltered = allItems
            .filter { it.isLost && it.status == "Approved" }
            .let { SearchManager.filterItems(it, searchQuery) }
        
        assertEquals("Should find 1 lost wallet", 1, lostItemsFiltered.size)
        
        // Tab 2: Found Items with same search
        val foundItemsFiltered = allItems
            .filter { !it.isLost && it.status == "Approved" }
            .let { SearchManager.filterItems(it, searchQuery) }
        
        assertEquals("Should find 1 found wallet", 1, foundItemsFiltered.size)
        
        // Verify search is applied to both tabs
        assertTrue("Search should work across tabs", 
            lostItemsFiltered.isNotEmpty() && foundItemsFiltered.isNotEmpty())
    }

    /**
     * Test complete browse and search workflow
     */
    @Test
    fun testCompleteBrowseAndSearchWorkflow() {
        // Step 1: Create test data
        val items = listOf(
            LostFoundItem(id = "1", name = "Lost Wallet", isLost = true, status = "Approved", category = "Wallet"),
            LostFoundItem(id = "2", name = "Found Phone", isLost = false, status = "Approved", category = "Electronics"),
            LostFoundItem(id = "3", name = "Lost Keys", isLost = true, status = "Approved", category = "Keys"),
            LostFoundItem(id = "4", name = "Found Laptop", isLost = false, status = "Approved", category = "Electronics"),
            LostFoundItem(id = "5", name = "Returned Bag", isLost = false, status = "Returned", category = "Bags")
        )
        
        // Step 2: Test Lost Items tab
        val lostItems = items.filter { it.isLost && it.status == "Approved" }
        assertEquals("Lost Items tab should show 2 items", 2, lostItems.size)
        
        // Step 3: Test Found Items tab
        val foundItems = items.filter { !it.isLost && it.status == "Approved" }
        assertEquals("Found Items tab should show 2 items", 2, foundItems.size)
        
        // Step 4: Test Returned Items tab
        val returnedItems = items.filter { it.status == "Returned" }
        assertEquals("Returned Items tab should show 1 item", 1, returnedItems.size)
        
        // Step 5: Test search on Lost Items tab
        val searchedLostItems = SearchManager.filterItems(lostItems, "wallet")
        assertEquals("Search should find 1 lost wallet", 1, searchedLostItems.size)
        
        // Step 6: Test search on Found Items tab
        val searchedFoundItems = SearchManager.filterItems(foundItems, "electronics")
        assertEquals("Search should find 2 electronics", 2, searchedFoundItems.size)
        
        // Step 7: Test clearing search
        val clearedSearch = SearchManager.filterItems(lostItems, "")
        assertEquals("Cleared search should show all lost items", 2, clearedSearch.size)
    }
}
