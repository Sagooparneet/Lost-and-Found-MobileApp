package com.example.loginandregistration.admin.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper object for performance optimization
 * Ensures heavy computations run on appropriate background threads
 * 
 * Requirements: 9.6, 9.7
 */
object PerformanceHelper {
    
    /**
     * Execute heavy computation on Default dispatcher
     * Use this for CPU-intensive operations like:
     * - Data processing
     * - Analytics calculations
     * - Sorting large lists
     * - Complex filtering operations
     * 
     * @param block The computation to execute
     * @return Result of the computation
     */
    suspend fun <T> executeHeavyComputation(block: suspend () -> T): T {
        return withContext(Dispatchers.Default) {
            block()
        }
    }
    
    /**
     * Execute I/O operation on IO dispatcher
     * Use this for I/O operations like:
     * - Network requests
     * - Database queries
     * - File operations
     * 
     * @param block The I/O operation to execute
     * @return Result of the operation
     */
    suspend fun <T> executeIoOperation(block: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            block()
        }
    }
    
    /**
     * Process list with heavy operations on Default dispatcher
     * Optimized for large lists with complex transformations
     * 
     * @param list The list to process
     * @param operation The operation to perform on the list
     * @return Processed result
     */
    suspend fun <T, R> processListHeavy(
        list: List<T>,
        operation: (List<T>) -> R
    ): R {
        return withContext(Dispatchers.Default) {
            operation(list)
        }
    }
    
    /**
     * Filter list with heavy predicate on Default dispatcher
     * 
     * @param list The list to filter
     * @param predicate The filter condition
     * @return Filtered list
     */
    suspend fun <T> filterListHeavy(
        list: List<T>,
        predicate: (T) -> Boolean
    ): List<T> {
        return withContext(Dispatchers.Default) {
            list.filter(predicate)
        }
    }
    
    /**
     * Sort list with heavy comparator on Default dispatcher
     * 
     * @param list The list to sort
     * @param comparator The sorting logic
     * @return Sorted list
     */
    suspend fun <T> sortListHeavy(
        list: List<T>,
        comparator: Comparator<T>
    ): List<T> {
        return withContext(Dispatchers.Default) {
            list.sortedWith(comparator)
        }
    }
    
    /**
     * Group list by key on Default dispatcher
     * 
     * @param list The list to group
     * @param keySelector Function to extract grouping key
     * @return Grouped map
     */
    suspend fun <T, K> groupListHeavy(
        list: List<T>,
        keySelector: (T) -> K
    ): Map<K, List<T>> {
        return withContext(Dispatchers.Default) {
            list.groupBy(keySelector)
        }
    }
    
    /**
     * Calculate statistics on Default dispatcher
     * Use for aggregations, counts, sums, etc.
     * 
     * @param block The calculation to perform
     * @return Calculation result
     */
    suspend fun <T> calculateStatistics(block: suspend () -> T): T {
        return withContext(Dispatchers.Default) {
            block()
        }
    }
}
