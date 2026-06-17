package com.example.loginandregistration.admin.utils

import android.util.Log

/**
 * Cache manager for storing and retrieving cached data with timeout
 * Implements a simple in-memory cache with TTL (Time To Live)
 * Requirements: 9.5
 */
class CacheManager {
    private val cache = mutableMapOf<String, CacheEntry<*>>()
    private val cacheTimeout = 5 * 60 * 1000L // 5 minutes default
    
    companion object {
        private const val TAG = "CacheManager"
        
        // Cache keys
        const val KEY_USER_ANALYTICS = "user_analytics"
        const val KEY_USER_STATISTICS = "user_statistics"
        const val KEY_DONATION_STATS = "donation_stats"
        const val KEY_DASHBOARD_STATS = "dashboard_stats"
        const val KEY_ITEM_ANALYTICS = "item_analytics"
        
        @Volatile
        private var instance: CacheManager? = null
        
        fun getInstance(): CacheManager {
            return instance ?: synchronized(this) {
                instance ?: CacheManager().also { instance = it }
            }
        }
    }
    
    /**
     * Cache entry with data and timestamp
     */
    data class CacheEntry<T>(
        val data: T,
        val timestamp: Long,
        val ttl: Long = 5 * 60 * 1000L // 5 minutes default
    )
    
    /**
     * Get cached data if available and not expired
     * @param key Cache key
     * @return Cached data or null if not found or expired
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = cache[key] as? CacheEntry<T> ?: return null
        
        return if (System.currentTimeMillis() - entry.timestamp < entry.ttl) {
            Log.d(TAG, "Cache hit for key: $key")
            entry.data
        } else {
            Log.d(TAG, "Cache expired for key: $key")
            cache.remove(key)
            null
        }
    }
    
    /**
     * Put data into cache with default TTL
     * @param key Cache key
     * @param data Data to cache
     */
    fun <T> put(key: String, data: T) {
        put(key, data, cacheTimeout)
    }
    
    /**
     * Put data into cache with custom TTL
     * @param key Cache key
     * @param data Data to cache
     * @param ttl Time to live in milliseconds
     */
    fun <T> put(key: String, data: T, ttl: Long) {
        val entry = CacheEntry(
            data = data,
            timestamp = System.currentTimeMillis(),
            ttl = ttl
        )
        cache[key] = entry
        Log.d(TAG, "Cached data for key: $key with TTL: ${ttl}ms")
    }
    
    /**
     * Invalidate (remove) cached data for a specific key
     * @param key Cache key to invalidate
     */
    fun invalidate(key: String) {
        cache.remove(key)
        Log.d(TAG, "Invalidated cache for key: $key")
    }
    
    /**
     * Invalidate all cached data matching a pattern
     * @param pattern Regex pattern to match keys
     */
    fun invalidatePattern(pattern: String) {
        val regex = Regex(pattern)
        val keysToRemove = cache.keys.filter { regex.matches(it) }
        keysToRemove.forEach { cache.remove(it) }
        Log.d(TAG, "Invalidated ${keysToRemove.size} cache entries matching pattern: $pattern")
    }
    
    /**
     * Clear all cached data
     */
    fun clearAll() {
        val size = cache.size
        cache.clear()
        Log.d(TAG, "Cleared all cache entries ($size items)")
    }
    
    /**
     * Get cache statistics
     */
    fun getStats(): CacheStats {
        val now = System.currentTimeMillis()
        val validEntries = cache.values.count { 
            now - it.timestamp < it.ttl 
        }
        val expiredEntries = cache.size - validEntries
        
        return CacheStats(
            totalEntries = cache.size,
            validEntries = validEntries,
            expiredEntries = expiredEntries
        )
    }
    
    /**
     * Clean up expired entries
     */
    fun cleanupExpired() {
        val now = System.currentTimeMillis()
        val keysToRemove = cache.filter { (_, entry) ->
            now - entry.timestamp >= entry.ttl
        }.keys
        
        keysToRemove.forEach { cache.remove(it) }
        
        if (keysToRemove.isNotEmpty()) {
            Log.d(TAG, "Cleaned up ${keysToRemove.size} expired cache entries")
        }
    }
    
    /**
     * Check if a key exists in cache and is valid
     */
    fun contains(key: String): Boolean {
        val entry = cache[key] ?: return false
        return System.currentTimeMillis() - entry.timestamp < entry.ttl
    }
    
    /**
     * Get or compute cached value
     * If cache miss, compute the value and cache it
     */
    suspend fun <T> getOrPut(
        key: String,
        ttl: Long = cacheTimeout,
        compute: suspend () -> T
    ): T {
        // Try to get from cache first
        get<T>(key)?.let { return it }
        
        // Cache miss - compute value
        Log.d(TAG, "Cache miss for key: $key, computing value...")
        val value = compute()
        put(key, value, ttl)
        return value
    }
}

/**
 * Cache statistics data class
 */
data class CacheStats(
    val totalEntries: Int,
    val validEntries: Int,
    val expiredEntries: Int
)
