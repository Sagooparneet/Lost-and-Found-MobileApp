package com.example.loginandregistration.utils

import android.widget.EditText
import com.google.android.material.textfield.TextInputEditText

/**
 * Utility class for safely setting text in EditText fields
 * Prevents InputConnection crashes and handles null values
 */
object EditTextUtils {
    
    /**
     * Safely set text in an EditText without triggering InputConnection issues
     * @param editText The EditText to update
     * @param text The text to set (can be null or empty)
     */
    fun safeSetText(editText: EditText?, text: String?) {
        if (editText == null) return
        
        try {
            // Post to message queue to avoid InputConnection issues
            editText.post {
                try {
                    val safeText = text ?: ""
                    editText.setText(safeText)
                    // Move cursor to end
                    editText.setSelection(safeText.length)
                } catch (e: Exception) {
                    // Fallback: try direct set
                    try {
                        editText.setText(text ?: "")
                    } catch (e2: Exception) {
                        // Ignore if still fails
                    }
                }
            }
        } catch (e: Exception) {
            // If post fails, try direct set
            try {
                editText.setText(text ?: "")
            } catch (e2: Exception) {
                // Ignore if fails
            }
        }
    }
    
    /**
     * Safely set text in a TextInputEditText
     */
    fun safeSetText(editText: TextInputEditText?, text: String?) {
        safeSetText(editText as? EditText, text)
    }
    
    /**
     * Safely clear text in an EditText without triggering InputConnection issues
     * @param editText The EditText to clear
     */
    fun safeClear(editText: EditText?) {
        safeSetText(editText, "")
    }
    
    /**
     * Safely clear text in a TextInputEditText
     */
    fun safeClear(editText: TextInputEditText?) {
        safeClear(editText as? EditText)
    }
}
