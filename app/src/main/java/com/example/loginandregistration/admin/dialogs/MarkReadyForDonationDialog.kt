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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for confirming marking an item as ready for donation
 * Requirements: 3.4, 3.12
 * Task: 11.4
 */
class MarkReadyForDonationDialog : DialogFragment() {
    
    private var onConfirm: (() -> Unit)? = null
    
    companion object {
        private const val ARG_DONATION_ITEM = "donation_item"
        
        fun newInstance(item: DonationItem, onConfirm: () -> Unit): MarkReadyForDonationDialog {
            val dialog = MarkReadyForDonationDialog()
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
        return inflater.inflate(R.layout.dialog_mark_ready_for_donation, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val item = arguments?.getSerializable(ARG_DONATION_ITEM) as? DonationItem ?: return
        
        // Initialize views
        val ivItemImage: ImageView = view.findViewById(R.id.ivItemImage)
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val tvItemCategory: TextView = view.findViewById(R.id.tvItemCategory)
        val tvItemLocation: TextView = view.findViewById(R.id.tvItemLocation)
        val tvItemAge: TextView = view.findViewById(R.id.tvItemAge)
        val tvConfirmationMessage: TextView = view.findViewById(R.id.tvConfirmationMessage)
        val tvNotificationNote: TextView = view.findViewById(R.id.tvNotificationNote)
        val btnConfirm: MaterialButton = view.findViewById(R.id.btnConfirm)
        val btnCancel: MaterialButton = view.findViewById(R.id.btnCancel)
        
        // Populate item summary
        tvItemName.text = item.itemName
        tvItemCategory.text = item.category
        tvItemLocation.text = item.location
        
        // Calculate and display item age
        val ageInDays = item.getAgeInDays()
        val ageText = when {
            ageInDays < 365 -> "$ageInDays days old"
            ageInDays < 730 -> "${ageInDays / 365} year old"
            else -> "${ageInDays / 365} years old"
        }
        tvItemAge.text = ageText
        
        // Set confirmation message
        tvConfirmationMessage.text = "Are you sure you want to mark this item as ready for donation?\n\n" +
                "This action will:\n" +
                "• Update the item status to 'Ready for Donation'\n" +
                "• Make the item available for donation processing\n" +
                "• Send a notification to the original reporter"
        
        // Set notification note
        tvNotificationNote.text = "Note: A notification will be automatically sent to the original reporter " +
                "informing them that their item is being prepared for donation."
        
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
            onConfirm?.invoke()
            dismiss()
        }
        
        btnCancel.setOnClickListener {
            dismiss()
        }
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
