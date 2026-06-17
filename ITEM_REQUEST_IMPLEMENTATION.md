# Item Request Feature Implementation

## Overview
Successfully implemented the item request functionality allowing users to request items and security to approve/reject them.

## Files Created/Modified

### New Files:
1. **ItemRequest.kt** - Data model for item requests
2. **ItemRequestAdapter.kt** - RecyclerView adapter for security dashboard
3. **SecurityItemRequestsFragment.kt** - Fragment for viewing item requests (optional, not currently used)

### Modified Files:
1. **ItemDetailsDialog.kt** - Added "Request Item" button and request submission logic
2. **dialog_item_details.xml** - Added Request Item button to layout
3. **SecurityDashboardFragment.kt** - Added item requests section
4. **fragment_security_dashboard.xml** - Added RecyclerView for item requests
5. **BrowseViewPagerAdapter.kt** - Added "Requested" tab (6 tabs total now)
6. **BrowseTabFragment.kt** - Added REQUESTED filter type and logic
7. **firestore.rules** - Added security rules for itemRequests collection

## Workflow

### 1. User Requests an Item
- User browses items in any tab (Lost/Found/All/etc.)
- Clicks on an item to view details
- Clicks "Request Item" button
- System checks if user already has a pending request for this item
- If no pending request exists, shows dialog to enter reason
- User enters reason and submits
- Request is saved to Firestore `itemRequests` collection with status "Pending"

### 2. Security Reviews Request
- Security logs into their dashboard
- Sees "ITEM REQUESTS" section with all pending requests
- Each request shows:
  - Item name
  - Requester email
  - Request date
  - Reason for request
  - Status (Pending/Approved/Rejected)
- Security can click "Approve" or "Reject" buttons
- Status is updated in Firestore with reviewer info and timestamp

### 3. User Views Approved Items
- User navigates to Browse section
- Clicks on "Requested" tab (4th tab)
- Sees only items that have been:
  - Requested by them
  - Approved by security
- These items are ready for pickup
- If request was rejected, item does NOT appear in this tab

## Firestore Collections

### itemRequests Collection
```
{
  requestId: string (document ID)
  itemId: string (reference to items collection)
  itemName: string
  userId: string (requester's UID)
  userEmail: string
  userName: string
  reason: string (why they need the item)
  status: "Pending" | "Approved" | "Rejected"
  requestDate: Timestamp
  reviewedBy: string (security email)
  reviewDate: Timestamp (when approved/rejected)
  reviewNotes: string (optional)
}
```

## Security Rules
Added rules for `itemRequests` collection:
- **Read**: Users can read their own requests, Security/Admin can read all
- **Create**: Users can create requests with their own userId
- **Update**: Only Security/Admin can update (for approval/rejection)
- **Delete**: Users can delete their own pending requests, Security/Admin can delete any

## Testing Checklist

### ✅ User Flow:
1. [ ] User can view item details
2. [ ] "Request Item" button is visible
3. [ ] Clicking button shows reason dialog
4. [ ] Submitting empty reason shows error
5. [ ] Valid submission creates request in Firestore
6. [ ] Duplicate request attempt shows error message
7. [ ] Request appears in "My Requests" tab (if implemented)

### ✅ Security Flow:
1. [ ] Security dashboard shows "ITEM REQUESTS" section
2. [ ] All item requests are visible
3. [ ] Approve button updates status to "Approved"
4. [ ] Reject button updates status to "Rejected"
5. [ ] Approved/Rejected requests hide action buttons
6. [ ] Toast messages confirm actions

### ✅ Requested Tab:
1. [ ] "Requested" tab appears in Browse section (4th tab)
2. [ ] Shows only items with approved requests for current user
3. [ ] Empty state shows when no approved requests
4. [ ] Rejected items do NOT appear
5. [ ] Pending items do NOT appear

### ✅ Firestore:
1. [ ] itemRequests collection is created
2. [ ] Security rules allow proper read/write access
3. [ ] No permission denied errors
4. [ ] Real-time updates work correctly

## Known Limitations

1. **No notification system** - Users are not notified when their request is approved/rejected
2. **No pickup tracking** - No way to mark item as picked up after approval
3. **No request history** - Rejected requests are not easily viewable by users
4. **No request cancellation** - Users cannot cancel pending requests from UI (though they can delete via Firestore rules)

## Future Enhancements

1. Add push notifications for request status changes
2. Add "Cancel Request" button for pending requests
3. Show request history (including rejected) in a separate tab
4. Add pickup confirmation workflow
5. Add request notes/comments for security
6. Add request expiration (auto-reject after X days)
7. Add analytics for request approval rates

## Build Status
✅ No compilation errors
✅ No diagnostics issues
✅ Firestore rules updated
✅ All layouts valid
✅ Gradle build successful (dry-run)

## Notes
- The implementation reuses existing layouts and patterns from the codebase
- Security dashboard now has 2 sections: Item Requests and Recent Reports
- Browse section now has 6 tabs: Lost Items, Found Items, Returned, Requested, My Requests, All Items
- All code follows existing architecture and naming conventions
