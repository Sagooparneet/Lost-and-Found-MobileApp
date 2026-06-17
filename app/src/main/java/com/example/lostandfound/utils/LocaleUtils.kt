package com.example.lostandfound.utils

import java.util.Locale

/**
 * Utility class for locale-aware string operations.
 * Provides helper functions to ensure proper locale handling in string formatting and case conversion.
 */
object LocaleUtils {
    
    /**
     * Formats a string using the default locale.
     * 
     * @param format The format string
     * @param args The arguments to format
     * @return The formatted string using the default locale
     */
    fun formatString(format: String, vararg args: Any): String {
        return String.format(Locale.getDefault(), format, *args)
    }
    
    /**
     * Converts a string to uppercase using the default locale.
     * 
     * @param text The text to convert
     * @return The uppercase string using the default locale
     */
    fun toUpperCaseLocale(text: String): String {
        return text.uppercase(Locale.getDefault())
    }
    
    /**
     * Converts a string to lowercase using the default locale.
     * 
     * @param text The text to convert
     * @return The lowercase string using the default locale
     */
    fun toLowerCaseLocale(text: String): String {
        return text.lowercase(Locale.getDefault())
    }
}
