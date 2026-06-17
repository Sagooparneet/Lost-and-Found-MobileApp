package com.example.loginandregistration

/**
 * Interface for fragments that support search filtering
 * Requirement: 7.5
 */
interface SearchableFragment {
    /**
     * Apply a search filter to the fragment's displayed items
     * @param query The search query string (empty string clears the filter)
     */
    fun applySearchFilter(query: String)
}
