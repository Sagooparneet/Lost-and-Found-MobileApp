package com.example.loginandregistration.utils

import com.example.loginandregistration.LostFoundItem

/**
 * Utility object for managing search and filtering operations on lost and found items.
 * Provides multi-field search with case-insensitive matching.
 */
object SearchManager {
    
    /**
     * Filters a list of items based on a search query.
     * Searches across multiple fields: name, description, location, and category.
     * Performs case-insensitive matching.
     * 
     * @param items The list of items to filter
     * @param query The search query string
     * @return A filtered list of items matching the search query
     */
    fun filterItems(items: List<LostFoundItem>, query: String): List<LostFoundItem> {
        // Return all items if query is blank
        if (query.isBlank()) {
            return items
        }
        
        val lowerQuery = query.lowercase().trim()
        
        return items.filter { item ->
            item.name.lowercase().contains(lowerQuery) ||
            item.description.lowercase().contains(lowerQuery) ||
            item.location.lowercase().contains(lowerQuery) ||
            item.category.lowercase().contains(lowerQuery)
        }
    }
}
