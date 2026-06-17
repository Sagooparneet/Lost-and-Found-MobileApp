package com.example.loginandregistration.admin.utils

import com.example.loginandregistration.admin.models.EnhancedLostFoundItem
import com.example.loginandregistration.admin.models.ItemStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import java.util.Calendar
import java.util.Date

/**
 * Utility class for generating test data for old items
 * Requirements: 3.1, 3.2, 3.3
 */
object TestDataGenerator {
    private const val TAG = "TestDataGenerator"
    private const val ITEMS_COLLECTION = "lostFoundItems"
    
    /**
     * Generate test items with timestamps from 1+ year ago
     * Creates items at different ages for comprehensive testing
     * Requirements: 3.1, 3.2, 3.3
     */
    suspend fun generateOldTestItems(firestore: FirebaseFirestore = FirebaseFirestore.getInstance()): Result<Int> {
        return try {
            Log.d(TAG, "Starting generation of old test items...")
            
            val testItems = createTestItemsList()
            var createdCount = 0
            
            testItems.forEach { item ->
                try {
                    val docRef = firestore.collection(ITEMS_COLLECTION).document()
                    val itemWithId = item.copy(id = docRef.id)
                    
                    docRef.set(itemWithId.toMap()).await()
                    createdCount++
                    Log.d(TAG, "Created test item: ${item.name} (${item.getAgeInDays()} days old)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create test item: ${item.name}", e)
                }
            }
            
            Log.d(TAG, "Successfully created $createdCount test items")
            Result.success(createdCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate test items", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create a list of test items with various ages and statuses
     * Requirements: 3.1, 3.2, 3.3
     */
    private fun createTestItemsList(): List<EnhancedLostFoundItem> {
        val currentTime = System.currentTimeMillis()
        
        return listOf(
            // Item exactly 365 days old - ACTIVE status (should be flagged for donation)
            createTestItem(
                name = "Old Laptop - Dell XPS",
                description = "Silver Dell XPS 13 laptop, found in library",
                category = "Electronics",
                location = "Main Library - 3rd Floor",
                daysOld = 365,
                status = ItemStatus.ACTIVE,
                isLost = false
            ),
            
            // Item 400 days old - ACTIVE status (should be flagged for donation)
            createTestItem(
                name = "Vintage Watch - Seiko",
                description = "Classic Seiko watch with leather strap",
                category = "Accessories",
                location = "Student Center",
                daysOld = 400,
                status = ItemStatus.ACTIVE,
                isLost = false
            ),
            
            // Item 2 years old - DONATION_PENDING status
            createTestItem(
                name = "Blue Backpack - Nike",
                description = "Navy blue Nike backpack with laptop compartment",
                category = "Bags",
                location = "Cafeteria",
                daysOld = 730,
                status = ItemStatus.DONATION_PENDING,
                isLost = false
            ),
            
            // Item 500 days old - DONATION_READY status
            createTestItem(
                name = "Textbook - Calculus II",
                description = "Stewart Calculus textbook, 8th edition",
                category = "Books",
                location = "Math Building - Room 201",
                daysOld = 500,
                status = ItemStatus.DONATION_READY,
                isLost = false
            ),
            
            // Item 3 years old - DONATED status
            createTestItem(
                name = "Umbrella - Black",
                description = "Large black umbrella with wooden handle",
                category = "Accessories",
                location = "Main Entrance",
                daysOld = 1095,
                status = ItemStatus.DONATED,
                isLost = false
            ),
            
            // Item 450 days old - ACTIVE status (Lost item)
            createTestItem(
                name = "Car Keys - Toyota",
                description = "Toyota car keys with blue keychain",
                category = "Keys",
                location = "Parking Lot B",
                daysOld = 450,
                status = ItemStatus.ACTIVE,
                isLost = true
            ),
            
            // Item 600 days old - DONATION_PENDING status
            createTestItem(
                name = "Headphones - Sony WH-1000XM4",
                description = "Black Sony noise-cancelling headphones",
                category = "Electronics",
                location = "Library Study Room 5",
                daysOld = 600,
                status = ItemStatus.DONATION_PENDING,
                isLost = false
            ),
            
            // Item 800 days old - DONATION_READY status
            createTestItem(
                name = "Water Bottle - Hydro Flask",
                description = "Blue 32oz Hydro Flask water bottle",
                category = "Personal Items",
                location = "Gym Locker Room",
                daysOld = 800,
                status = ItemStatus.DONATION_READY,
                isLost = false
            ),
            
            // Item 1.5 years old - ACTIVE status
            createTestItem(
                name = "Jacket - North Face",
                description = "Black North Face winter jacket, size M",
                category = "Clothing",
                location = "Lecture Hall A",
                daysOld = 547,
                status = ItemStatus.ACTIVE,
                isLost = false
            ),
            
            // Item 2.5 years old - DONATED status
            createTestItem(
                name = "Calculator - TI-84",
                description = "Texas Instruments TI-84 Plus graphing calculator",
                category = "Electronics",
                location = "Engineering Building",
                daysOld = 912,
                status = ItemStatus.DONATED,
                isLost = false
            ),
            
            // Item 380 days old - ACTIVE status
            createTestItem(
                name = "Sunglasses - Ray-Ban",
                description = "Black Ray-Ban Wayfarer sunglasses",
                category = "Accessories",
                location = "Sports Field",
                daysOld = 380,
                status = ItemStatus.ACTIVE,
                isLost = false
            ),
            
            // Item 700 days old - DONATION_PENDING status
            createTestItem(
                name = "Phone Charger - iPhone",
                description = "Apple Lightning cable and power adapter",
                category = "Electronics",
                location = "Computer Lab",
                daysOld = 700,
                status = ItemStatus.DONATION_PENDING,
                isLost = false
            )
        )
    }
    
    /**
     * Create a single test item with specified age
     * Requirements: 3.1, 3.2, 3.3
     */
    private fun createTestItem(
        name: String,
        description: String,
        category: String,
        location: String,
        daysOld: Int,
        status: ItemStatus,
        isLost: Boolean
    ): EnhancedLostFoundItem {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysOld)
        val oldTimestamp = Timestamp(calendar.time)
        
        val currentTimeMillis = System.currentTimeMillis()
        
        return EnhancedLostFoundItem(
            id = "", // Will be set when adding to Firestore
            name = name,
            description = description,
            location = location,
            contactInfo = "testadmin@example.com",
            isLost = isLost,
            userId = "test_admin_user",
            userEmail = "testadmin@example.com",
            imageUrl = "",
            timestamp = oldTimestamp,
            category = category,
            status = status,
            statusHistory = createStatusHistory(status, daysOld),
            requestedBy = "",
            requestedAt = 0,
            returnedAt = 0,
            donationEligibleAt = if (status.isDonationStatus()) {
                currentTimeMillis - ((daysOld - 365) * 24 * 60 * 60 * 1000L)
            } else 0,
            donatedAt = if (status == ItemStatus.DONATED) currentTimeMillis else 0,
            lastModifiedBy = "System",
            lastModifiedAt = currentTimeMillis
        )
    }
    
    /**
     * Create status history based on current status
     * Requirements: 3.2
     */
    private fun createStatusHistory(currentStatus: ItemStatus, daysOld: Int): List<com.example.loginandregistration.admin.models.StatusChange> {
        val history = mutableListOf<com.example.loginandregistration.admin.models.StatusChange>()
        val currentTimeMillis = System.currentTimeMillis()
        
        when (currentStatus) {
            ItemStatus.DONATION_PENDING -> {
                history.add(
                    com.example.loginandregistration.admin.models.StatusChange(
                        previousStatus = ItemStatus.ACTIVE,
                        newStatus = ItemStatus.DONATION_PENDING,
                        changedBy = "System",
                        changedAt = currentTimeMillis - ((daysOld - 365) * 24 * 60 * 60 * 1000L),
                        reason = "Item is 1+ year old and unclaimed"
                    )
                )
            }
            ItemStatus.DONATION_READY -> {
                history.add(
                    com.example.loginandregistration.admin.models.StatusChange(
                        previousStatus = ItemStatus.ACTIVE,
                        newStatus = ItemStatus.DONATION_PENDING,
                        changedBy = "System",
                        changedAt = currentTimeMillis - ((daysOld - 365) * 24 * 60 * 60 * 1000L),
                        reason = "Item is 1+ year old and unclaimed"
                    )
                )
                history.add(
                    com.example.loginandregistration.admin.models.StatusChange(
                        previousStatus = ItemStatus.DONATION_PENDING,
                        newStatus = ItemStatus.DONATION_READY,
                        changedBy = "Admin",
                        changedAt = currentTimeMillis - (7 * 24 * 60 * 60 * 1000L), // 7 days ago
                        reason = "Marked for donation by admin"
                    )
                )
            }
            ItemStatus.DONATED -> {
                history.add(
                    com.example.loginandregistration.admin.models.StatusChange(
                        previousStatus = ItemStatus.ACTIVE,
                        newStatus = ItemStatus.DONATION_PENDING,
                        changedBy = "System",
                        changedAt = currentTimeMillis - ((daysOld - 365) * 24 * 60 * 60 * 1000L),
                        reason = "Item is 1+ year old and unclaimed"
                    )
                )
                history.add(
                    com.example.loginandregistration.admin.models.StatusChange(
                        previousStatus = ItemStatus.DONATION_PENDING,
                        newStatus = ItemStatus.DONATION_READY,
                        changedBy = "Admin",
                        changedAt = currentTimeMillis - (14 * 24 * 60 * 60 * 1000L), // 14 days ago
                        reason = "Marked for donation by admin"
                    )
                )
                history.add(
                    com.example.loginandregistration.admin.models.StatusChange(
                        previousStatus = ItemStatus.DONATION_READY,
                        newStatus = ItemStatus.DONATED,
                        changedBy = "Admin",
                        changedAt = currentTimeMillis - (1 * 24 * 60 * 60 * 1000L), // 1 day ago
                        reason = "Item donated to charity"
                    )
                )
            }
            else -> {
                // ACTIVE status - no history
            }
        }
        
        return history
    }
    
    /**
     * Delete all test items created by this generator
     * Useful for cleanup after testing
     */
    suspend fun deleteTestItems(firestore: FirebaseFirestore = FirebaseFirestore.getInstance()): Result<Int> {
        return try {
            Log.d(TAG, "Deleting test items...")
            
            val snapshot = firestore.collection(ITEMS_COLLECTION)
                .whereEqualTo("userId", "test_admin_user")
                .get()
                .await()
            
            var deletedCount = 0
            snapshot.documents.forEach { doc ->
                try {
                    doc.reference.delete().await()
                    deletedCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete test item: ${doc.id}", e)
                }
            }
            
            Log.d(TAG, "Successfully deleted $deletedCount test items")
            Result.success(deletedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete test items", e)
            Result.failure(e)
        }
    }
}
