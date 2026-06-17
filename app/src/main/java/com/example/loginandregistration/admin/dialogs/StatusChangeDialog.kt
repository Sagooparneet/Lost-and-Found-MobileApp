package com.example.loginandregistration.admin.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.EnhancedLostFoundItem
import com.example.loginandregistration.admin.models.ItemStatus
import com.example.loginandregistration.admin.viewmodel.AdminDashboardViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

/**
 * Dialog for changing item status with reason tracking
 * Requirements: 2.5
 */
class StatusChangeDialog : DialogFragment() {
    
    private val viewModel: AdminDashboardViewModel by activityViewModels()
    
    private lateinit var rgStatus: RadioGroup
    private lateinit var etReason: TextInputEditText
    
    private var currentItem: EnhancedLostFoundItem? = null
    private var onStatusChanged: ((ItemStatus) -> Unit)? = null
    
    companion object {
        private const val ARG_ITEM = "item"
        
        fun newInstance(item: EnhancedLostFoundItem, onStatusChanged: (ItemStatus) -> Unit): StatusChangeDialog {
            return StatusChangeDialog().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ITEM, item)
                }
                this.onStatusChanged = onStatusChanged
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentItem = arguments?.getSerializable(ARG_ITEM) as? EnhancedLostFoundItem
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_status_change, null)
        
        initViews(view)
        setupStatusOptions()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change Item Status")
            .setView(view)
            .setPositiveButton("Change Status") { _, _ ->
                changeStatus()
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
    
    private fun initViews(view: View) {
        rgStatus = view.findViewById(R.id.rgStatus)
        etReason = view.findViewById(R.id.etReason)
    }
    
    private fun setupStatusOptions() {
        val item = currentItem ?: return
        
        // Add radio buttons for each status
        ItemStatus.values().forEach { status ->
            val radioButton = RadioButton(context).apply {
                id = View.generateViewId()
                text = getStatusDisplayName(status)
                isChecked = status == item.status
                isEnabled = status != item.status // Disable current status
                
                // Add description for each status
                val description = getStatusDescription(status)
                if (description.isNotEmpty()) {
                    text = "${getStatusDisplayName(status)}\n$description"
                }
            }
            rgStatus.addView(radioButton)
        }
    }
    
    private fun getStatusDisplayName(status: ItemStatus): String {
        return when (status) {
            ItemStatus.ACTIVE -> "Active"
            ItemStatus.REQUESTED -> "Requested"
            ItemStatus.RETURNED -> "Returned"
            ItemStatus.DONATION_PENDING -> "Donation Pending"
            ItemStatus.DONATION_READY -> "Donation Ready"
            ItemStatus.DONATED -> "Donated"
        }
    }
    
    private fun getStatusDescription(status: ItemStatus): String {
        return when (status) {
            ItemStatus.ACTIVE -> "Item is active and visible to users"
            ItemStatus.REQUESTED -> "User has requested this item"
            ItemStatus.RETURNED -> "Item has been returned to owner"
            ItemStatus.DONATION_PENDING -> "Item is eligible for donation"
            ItemStatus.DONATION_READY -> "Item is ready to be donated"
            ItemStatus.DONATED -> "Item has been donated"
        }
    }
    
    private fun changeStatus() {
        val item = currentItem ?: return
        
        // Get selected status
        val selectedRadioButtonId = rgStatus.checkedRadioButtonId
        if (selectedRadioButtonId == -1) {
            Snackbar.make(requireView(), "Please select a status", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        val selectedRadioButton = rgStatus.findViewById<RadioButton>(selectedRadioButtonId)
        val selectedStatusText = selectedRadioButton.text.toString().split("\n")[0]
        
        val newStatus = when (selectedStatusText) {
            "Active" -> ItemStatus.ACTIVE
            "Requested" -> ItemStatus.REQUESTED
            "Returned" -> ItemStatus.RETURNED
            "Donation Pending" -> ItemStatus.DONATION_PENDING
            "Donation Ready" -> ItemStatus.DONATION_READY
            "Donated" -> ItemStatus.DONATED
            else -> return
        }
        
        // Get reason
        val reason = etReason.text.toString().trim()
        
        // Validate reason for certain status changes
        if (shouldRequireReason(item.status, newStatus) && reason.isEmpty()) {
            Snackbar.make(requireView(), "Please provide a reason for this status change", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        // Show confirmation dialog
        showConfirmationDialog(item, newStatus, reason)
    }
    
    private fun shouldRequireReason(oldStatus: ItemStatus, newStatus: ItemStatus): Boolean {
        // Require reason for certain critical status changes
        return when {
            newStatus == ItemStatus.DONATED -> true
            newStatus == ItemStatus.DONATION_READY -> true
            oldStatus == ItemStatus.RETURNED && newStatus != ItemStatus.ACTIVE -> true
            else -> false
        }
    }
    
    private fun showConfirmationDialog(item: EnhancedLostFoundItem, newStatus: ItemStatus, reason: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Status Change")
            .setMessage("Are you sure you want to change the status from ${getStatusDisplayName(item.status)} to ${getStatusDisplayName(newStatus)}?")
            .setPositiveButton("Confirm") { _, _ ->
                // Invoke callback to notify fragment
                onStatusChanged?.invoke(newStatus)
                // Also update via ViewModel for backward compatibility
                viewModel.updateItemStatusEnhanced(item.id, newStatus, reason)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
