package com.example.loginandregistration.admin.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.DonationItem
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Dialog for marking an item as donated with recipient and value information
 * Requirements: 3.5
 * Task: 11.5
 */
class MarkAsDonatedDialog : DialogFragment() {
    
    private var onConfirm: ((String, Double) -> Unit)? = null
    
    companion object {
        private const val ARG_DONATION_ITEM = "donation_item"
        
        fun newInstance(item: DonationItem, onConfirm: (String, Double) -> Unit): MarkAsDonatedDialog {
            val dialog = MarkAsDonatedDialog()
            dialog.onConfirm = onConfirm
            val args = Bundle()
            args.putSerializable(ARG_DONATION_ITEM, item)
            dialog.arguments = args
            return dialog
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_mark_as_donated, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val item = arguments?.getSerializable(ARG_DONATION_ITEM) as? DonationItem ?: return
        
        // Initialize views
        val ivItemImage: ImageView = view.findViewById(R.id.ivItemImage)
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val tvItemCategory: TextView = view.findViewById(R.id.tvItemCategory)
        val tvItemLocation: TextView = view.findViewById(R.id.tvItemLocation)
        val tilRecipient: TextInputLayout = view.findViewById(R.id.tilRecipient)
        val etRecipient: TextInputEditText = view.findViewById(R.id.etRecipient)
        val tilValue: TextInputLayout = view.findViewById(R.id.tilValue)
        val etValue: TextInputEditText = view.findViewById(R.id.etValue)
        val btnConfirm: MaterialButton = view.findViewById(R.id.btnConfirm)
        val btnCancel: MaterialButton = view.findViewById(R.id.btnCancel)
        
        // Populate item summary
        tvItemName.text = item.itemName
        tvItemCategory.text = item.category
        tvItemLocation.text = item.location
        
        // Load image
        if (item.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .centerCrop()
                .into(ivItemImage)
        } else {
            ivItemImage.setImageResource(R.drawable.ic_image_placeholder)
        }
        
        // Set click listeners
        btnConfirm.setOnClickListener {
            if (validateInputs(tilRecipient, etRecipient, tilValue, etValue)) {
                val recipient = etRecipient.text.toString().trim()
                val value = etValue.text.toString().trim().toDoubleOrNull() ?: 0.0
                
                onConfirm?.invoke(recipient, value)
                dismiss()
            }
        }
        
        btnCancel.setOnClickListener {
            dismiss()
        }
    }
    
    /**
     * Validate form inputs
     * Requirements: 3.5
     */
    private fun validateInputs(
        tilRecipient: TextInputLayout,
        etRecipient: TextInputEditText,
        tilValue: TextInputLayout,
        etValue: TextInputEditText
    ): Boolean {
        var isValid = true
        
        // Validate recipient
        val recipient = etRecipient.text.toString().trim()
        if (recipient.isEmpty()) {
            tilRecipient.error = "Recipient name is required"
            isValid = false
        } else if (recipient.length < 3) {
            tilRecipient.error = "Recipient name must be at least 3 characters"
            isValid = false
        } else {
            tilRecipient.error = null
        }
        
        // Validate value
        val valueStr = etValue.text.toString().trim()
        if (valueStr.isEmpty()) {
            tilValue.error = "Estimated value is required"
            isValid = false
        } else {
            val value = valueStr.toDoubleOrNull()
            if (value == null) {
                tilValue.error = "Please enter a valid number"
                isValid = false
            } else if (value < 0) {
                tilValue.error = "Value cannot be negative"
                isValid = false
            } else if (value > 1000000) {
                tilValue.error = "Value seems too high. Please verify."
                isValid = false
            } else {
                tilValue.error = null
            }
        }
        
        return isValid
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
