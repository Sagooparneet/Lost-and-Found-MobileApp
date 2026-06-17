package com.example.loginandregistration.admin.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.DonationItem
import com.example.loginandregistration.admin.models.DonationStatus
import com.example.loginandregistration.admin.viewmodel.AdminDashboardViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for displaying complete donation item information
 * Requirements: 3.3
 * Task: 11.3
 */
class DonationDetailsDialog : DialogFragment() {
    
    private val viewModel: AdminDashboardViewModel by activityViewModels()
    
    companion object {
        private const val ARG_DONATION_ITEM = "donation_item"
        
        fun newInstance(item: DonationItem): DonationDetailsDialog {
            val dialog = DonationDetailsDialog()
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
        return inflater.inflate(R.layout.dialog_donation_details, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val item = arguments?.getSerializable(ARG_DONATION_ITEM) as? DonationItem ?: return
        
        // Initialize views
        val ivItemImage: ImageView = view.findViewById(R.id.ivItemImage)
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val tvItemDescription: TextView = view.findViewById(R.id.tvItemDescription)
        val tvItemCategory: TextView = view.findViewById(R.id.tvItemCategory)
        val tvItemLocation: TextView = view.findViewById(R.id.tvItemLocation)
        val tvItemAge: TextView = view.findViewById(R.id.tvItemAge)
        val tvReportedDate: TextView = view.findViewById(R.id.tvReportedDate)
        val tvEligibilityDate: TextView = view.findViewById(R.id.tvEligibilityDate)
        val chipStatus: Chip = view.findViewById(R.id.chipStatus)
        val tvMarkedReadyInfo: TextView = view.findViewById(R.id.tvMarkedReadyInfo)
        val tvDonatedInfo: TextView = view.findViewById(R.id.tvDonatedInfo)
        val btnMarkReady: MaterialButton = view.findViewById(R.id.btnMarkReady)
        val btnMarkDonated: MaterialButton = view.findViewById(R.id.btnMarkDonated)
        val btnClose: MaterialButton = view.findViewById(R.id.btnClose)
        
        // Populate basic information
        tvItemName.text = item.itemName
        tvItemDescription.text = item.description.ifEmpty { "No description available" }
        tvItemCategory.text = "Category: ${item.category}"
        tvItemLocation.text = "Location: ${item.location}"
        
        // Calculate and display item age
        val ageInDays = item.getAgeInDays()
        val ageText = when {
            ageInDays < 365 -> "$ageInDays days"
            ageInDays < 730 -> "${ageInDays / 365} year, ${ageInDays % 365} days"
            else -> "${ageInDays / 365} years, ${ageInDays % 365} days"
        }
        tvItemAge.text = "Item Age: $ageText"
        
        // Format dates
        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        tvReportedDate.text = "Reported: ${sdf.format(Date(item.reportedAt))}"
        tvEligibilityDate.text = "Eligible for Donation: ${sdf.format(Date(item.eligibleAt))}"
        
        // Set status chip
        chipStatus.text = getStatusDisplayName(item.status)
        chipStatus.setChipBackgroundColorResource(getStatusColor(item.status))
        
        // Show status-specific information
        when (item.status) {
            DonationStatus.PENDING -> {
                tvMarkedReadyInfo.visibility = View.GONE
                tvDonatedInfo.visibility = View.GONE
                btnMarkReady.visibility = View.VISIBLE
                btnMarkDonated.visibility = View.GONE
            }
            DonationStatus.READY -> {
                tvMarkedReadyInfo.visibility = View.VISIBLE
                tvMarkedReadyInfo.text = "Marked Ready by: ${item.markedReadyBy}\n" +
                        "On: ${sdf.format(Date(item.markedReadyAt))}"
                tvDonatedInfo.visibility = View.GONE
                btnMarkReady.visibility = View.GONE
                btnMarkDonated.visibility = View.VISIBLE
            }
            DonationStatus.DONATED -> {
                tvMarkedReadyInfo.visibility = View.VISIBLE
                tvMarkedReadyInfo.text = "Marked Ready by: ${item.markedReadyBy}\n" +
                        "On: ${sdf.format(Date(item.markedReadyAt))}"
                tvDonatedInfo.visibility = View.VISIBLE
                tvDonatedInfo.text = "Donated to: ${item.donationRecipient}\n" +
                        "Value: $${String.format(Locale.getDefault(), "%.2f", item.estimatedValue)}\n" +
                        "Donated by: ${item.donatedBy}\n" +
                        "On: ${sdf.format(Date(item.donatedAt))}"
                btnMarkReady.visibility = View.GONE
                btnMarkDonated.visibility = View.GONE
            }
        }
        
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
        
        // Set click listeners for action buttons
        btnMarkReady.setOnClickListener {
            dismiss()
            showMarkReadyDialog(item)
        }
        
        btnMarkDonated.setOnClickListener {
            dismiss()
            showMarkDonatedDialog(item)
        }
        
        btnClose.setOnClickListener {
            dismiss()
        }
    }
    
    private fun showMarkReadyDialog(item: DonationItem) {
        val dialog = MarkReadyForDonationDialog.newInstance(item) {
            viewModel.markItemReadyForDonation(item.itemId)
        }
        dialog.show(parentFragmentManager, "MarkReadyForDonationDialog")
    }
    
    private fun showMarkDonatedDialog(item: DonationItem) {
        val dialog = MarkAsDonatedDialog.newInstance(item) { recipient, value ->
            viewModel.markItemAsDonated(item.itemId, recipient, value)
        }
        dialog.show(parentFragmentManager, "MarkAsDonatedDialog")
    }
    
    private fun getStatusDisplayName(status: DonationStatus): String {
        return when (status) {
            DonationStatus.PENDING -> "Pending Review"
            DonationStatus.READY -> "Ready for Donation"
            DonationStatus.DONATED -> "Donated"
        }
    }
    
    private fun getStatusColor(status: DonationStatus): Int {
        return when (status) {
            DonationStatus.PENDING -> R.color.status_donation_pending
            DonationStatus.READY -> R.color.status_donation_ready
            DonationStatus.DONATED -> R.color.status_donated
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
