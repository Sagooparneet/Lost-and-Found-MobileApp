# Test Data Generator for Old Items

## Overview

The `TestDataGenerator` utility creates test items with timestamps from 1+ year ago to facilitate comprehensive testing of the donation workflow system.

## Requirements Addressed

- **3.1**: Calculate item age by comparing current date with item creation timestamp
- **3.2**: Automatically flag items older than 365 days as "donation pending"
- **3.3**: Display items with age indicators and donation status

## Features

### Generated Test Items

The generator creates 12 test items with the following characteristics:

1. **Item exactly 365 days old** - ACTIVE status (should be flagged for donation)
   - Dell XPS Laptop
   - Category: Electronics

2. **Item 400 days old** - ACTIVE status (should be flagged for donation)
   - Seiko Watch
   - Category: Accessories

3. **Item 2 years old (730 days)** - DONATION_PENDING status
   - Nike Backpack
   - Category: Bags

4. **Item 500 days old** - DONATION_READY status
   - Calculus Textbook
   - Category: Books

5. **Item 3 years old (1095 days)** - DONATED status
   - Black Umbrella
   - Category: Accessories

6. **Item 450 days old** - ACTIVE status (Lost item)
   - Toyota Car Keys
   - Category: Keys

7. **Item 600 days old** - DONATION_PENDING status
   - Sony Headphones
   - Category: Electronics

8. **Item 800 days old** - DONATION_READY status
   - Hydro Flask Water Bottle
   - Category: Personal Items

9. **Item 1.5 years old (547 days)** - ACTIVE status
   - North Face Jacket
   - Category: Clothing

10. **Item 2.5 years old (912 days)** - DONATED status
    - TI-84 Calculator
    - Category: Electronics

11. **Item 380 days old** - ACTIVE status
    - Ray-Ban Sunglasses
    - Category: Accessories

12. **Item 700 days old** - DONATION_PENDING status
    - iPhone Charger
    - Category: Electronics

### Status History

Each item includes appropriate status history based on its current status:

- **ACTIVE**: No status history
- **DONATION_PENDING**: One status change from ACTIVE to DONATION_PENDING
- **DONATION_READY**: Two status changes (ACTIVE → DONATION_PENDING → DONATION_READY)
- **DONATED**: Three status changes (ACTIVE → DONATION_PENDING → DONATION_READY → DONATED)

## Usage

### From AdminRepository

```kotlin
// Generate test items
val result = adminRepository.generateOldTestItems()
result.onSuccess { count ->
    Log.d(TAG, "Generated $count test items")
}

// Delete test items
val deleteResult = adminRepository.deleteTestItems()
deleteResult.onSuccess { count ->
    Log.d(TAG, "Deleted $count test items")
}
```

### From AdminDashboardViewModel

```kotlin
// Generate test items
viewModel.generateOldTestItems()

// Delete test items
viewModel.deleteTestItems()
```

### Direct Usage

```kotlin
// Generate test items
val result = TestDataGenerator.generateOldTestItems()

// Delete test items
val deleteResult = TestDataGenerator.deleteTestItems()
```

## Testing Scenarios

### 1. Age Calculation Testing
- Verify items show correct age in days
- Test items at exactly 365 days boundary
- Test items at various ages (400, 500, 730, 1095 days)

### 2. Automatic Flagging Testing
- Items with ACTIVE status and 365+ days should be flagged as DONATION_PENDING
- Verify the automatic flagging system identifies eligible items

### 3. Status Workflow Testing
- Test transitions: ACTIVE → DONATION_PENDING → DONATION_READY → DONATED
- Verify status history is recorded correctly
- Test UI displays correct action buttons for each status

### 4. Visual Indicator Testing
- Verify "365+ days old" badge appears for items 1+ year old
- Test age display formatting
- Verify donation date display for DONATED items

### 5. Filtering and Sorting Testing
- Test filtering by donation status
- Verify items are sorted by age (oldest first)
- Test category filtering with old items

## Cleanup

After testing, use the `deleteTestItems()` method to remove all generated test data:

```kotlin
adminRepository.deleteTestItems()
```

This will delete all items where `userId = "test_admin_user"`.

## Implementation Details

### Timestamp Calculation

```kotlin
val calendar = Calendar.getInstance()
calendar.add(Calendar.DAY_OF_YEAR, -daysOld)
val oldTimestamp = Timestamp(calendar.time)
```

### Item Identification

All test items are created with:
- `userId = "test_admin_user"`
- `userEmail = "testadmin@example.com"`

This allows easy identification and cleanup.

### Categories Included

- Electronics
- Accessories
- Bags
- Books
- Keys
- Personal Items
- Clothing

## Security

- Only admins can generate or delete test items (enforced by `requireAdminAccess()`)
- All operations are logged in the activity log
- Test items are clearly marked with test user ID

## Notes

- Test items are created in the `lostFoundItems` collection
- Each item has a unique Firestore document ID
- Status history timestamps are calculated relative to item age
- All test items are "found" items except one (Car Keys) which is a "lost" item
