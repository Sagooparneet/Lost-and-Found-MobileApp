package com.example.loginandregistration.admin.utils

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Helper class for implementing pagination in Firestore queries
 * Manages page state and provides methods to load paginated data
 */
class PaginationHelper<T>(
    private val pageSize: Int = 50
) {
    private var lastDocument: DocumentSnapshot? = null
    private var hasMorePages = true
    
    /**
     * Load the next page of data
     * @param query The Firestore query to paginate
     * @param transform Function to transform DocumentSnapshot to model object
     * @return Result containing list of items or error
     */
    suspend fun loadNextPage(
        query: Query,
        transform: (DocumentSnapshot) -> T
    ): Result<List<T>> {
        return try {
            if (!hasMorePages) {
                return Result.success(emptyList())
            }
            
            val queryWithPagination = if (lastDocument != null) {
                query.startAfter(lastDocument!!).limit(pageSize.toLong())
            } else {
                query.limit(pageSize.toLong())
            }
            
            val snapshot = queryWithPagination.get().await()
            
            if (snapshot.documents.isNotEmpty()) {
                lastDocument = snapshot.documents.last()
                hasMorePages = snapshot.documents.size >= pageSize
            } else {
                hasMorePages = false
            }
            
            val items = snapshot.documents.mapNotNull { doc ->
                try {
                    transform(doc)
                } catch (e: Exception) {
                    null // Skip invalid documents
                }
            }
            
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Reset pagination state to start from the beginning
     */
    fun reset() {
        lastDocument = null
        hasMorePages = true
    }
    
    /**
     * Check if there are more pages available
     */
    fun hasMore(): Boolean = hasMorePages
    
    /**
     * Get the current page size
     */
    fun getPageSize(): Int = pageSize
}
