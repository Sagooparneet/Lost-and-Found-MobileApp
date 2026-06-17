package com.example.loginandregistration.admin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.adapters.StatusHistoryAdapter
import com.example.loginandregistration.admin.dialogs.EditItemDialog
import com.example.loginandregistration.admin.dialogs.StatusChangeDialog
import com.example.loginandregistration.admin.models.EnhancedLostFoundItem
import com.example.loginandregistration.admin.viewmodel.AdminDashboardViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced ItemDetailsFragment showing comprehensive item information
 * Requirements: 2.3, 2.9
 */
class ItemDetailsFragment : Fragment() {
    
    private val viewModel: AdminDashboardViewModel by activityViewModels()
    private lateinit var statusHistoryAdapter: StatusHistoryAdapter
    
    // Views
    private lateinit var btnBack: android.widget.ImageButton
    private lateinit var ivItemImage: ImageView
    private lateinit var tvItemName: TextView
    private lateinit var tvItemDescription: TextView
    private lateinit var tvItemLocation: TextView
    private lateinit var tvItemCategory: TextView
    private lateinit var tvItemContact: TextView
    private lateinit var tvItemType: TextView
    private lateinit var tvReportedBy: TextView
    private lateinit var tvReportedDate: TextView
    private lateinit var tvLastModified: TextView
    private lateinit var chipStatus: Chip
    private lateinit var tvRequestedBy: TextView
    private lateinit var tvRequestedDate: TextView
    private lateinit var tvReturnedDate: TextView
    private lateinit var rvStatusHistory: RecyclerView
    private lateinit var btnEdit: MaterialButton
    private lateinit var btnChangeStatus: MaterialButton
    private lateinit var btnDelete: MaterialButton
    
    private var itemId: String? = null
    private var currentItem: EnhancedLostFoundItem? = null
    
    companion object {
        private const val ARG_ITEM_ID = "item_id"
        
        fun newInstance(itemId: String): ItemDetailsFragment {
            return ItemDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ITEM_ID, itemId)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Support both manual arguments and navigation args
        itemId = arguments?.getString(ARG_ITEM_ID) 
            ?: arguments?.getString("item_id")
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_item_details, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupStatusHistoryRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Load item details
        itemId?.let { viewModel.loadItemDetails(it) }
    }
    
    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        ivItemImage = view.findViewById(R.id.ivItemImage)
        tvItemName = view.findViewById(R.id.tvItemName)
        tvItemDescription = view.findViewById(R.id.tvItemDescription)
        tvItemLocation = view.findViewById(R.id.tvItemLocation)
        tvItemCategory = view.findViewById(R.id.tvItemCategory)
        tvItemContact = view.findViewById(R.id.tvItemContact)
        tvItemType = view.findViewById(R.id.tvItemType)
        tvReportedBy = view.findViewById(R.id.tvReportedBy)
        tvReportedDate = view.findViewById(R.id.tvReportedDate)
        tvLastModified = view.findViewById(R.id.tvLastModified)
        chipStatus = view.findViewById(R.id.chipStatus)
        tvRequestedBy = view.findViewById(R.id.tvRequestedBy)
        tvRequestedDate = view.findViewById(R.id.tvRequestedDate)
        tvReturnedDate = view.findViewById(R.id.tvReturnedDate)
        rvStatusHistory = view.findViewById(R.id.rvStatusHistory)
        btnEdit = view.findViewById(R.id.btnEdit)
        btnChangeStatus = view.findViewById(R.id.btnChangeStatus)
        btnDelete = view.findViewById(R.id.btnDelete)
    }
    
    private fun setupStatusHistoryRecyclerView() {
        statusHistoryAdapter = StatusHistoryAdapter()
        rvStatusHistory.apply {
            adapter = statusHistoryAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        btnEdit.setOnClickListener {
            currentItem?.let { item ->
                showEditItemDialog(item)
            }
        }
        
        btnChangeStatus.setOnClickListener {
            currentItem?.let { item ->
                showStatusChangeDialog(item)
            }
        }
        
        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }
    
    private fun observeViewModel() {
        viewModel.itemDetails.observe(viewLifecycleOwner) { item ->
            currentItem = item
            displayItemDetails(item)
        }
        
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
                // Reload item details after successful status update
                itemId?.let { viewModel.loadItemDetails(it) }
            }
        }
        
        // Error display disabled - uncomment to show errors
        // viewModel.error.observe(viewLifecycleOwner) { error ->
        //     if (error.isNotEmpty()) {
        //         Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show()
        //     }
        // }
    }
    
    private fun displayItemDetails(item: EnhancedLostFoundItem) {
        // Load image - handle nullable imageUrl properly
        if (!item.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .centerCrop()
                .into(ivItemImage)
        } else {
            ivItemImage.setImageResource(R.drawable.ic_image_placeholder)
        }
        
        // Basic information
        tvItemName.text = item.name
        tvItemDescription.text = item.description
        tvItemLocation.text = item.location
        tvItemCategory.text = item.category.ifEmpty { "Not specified" }
        tvItemContact.text = item.contactInfo.ifEmpty { "Not provided" }
        tvItemType.text = if (item.isLost) "Lost Item" else "Found Item"
        
        // Status chip
        chipStatus.text = getStatusDisplayName(item.status)
        chipStatus.setChipBackgroundColorResource(getStatusColor(item.status))
        
        // Reporter information
        tvReportedBy.text = item.userEmail
        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        tvReportedDate.text = sdf.format(item.timestamp.toDate())
        
        // Last modified
        if (item.lastModifiedAt > 0) {
            tvLastModified.text = "Last modified: ${sdf.format(Date(item.lastModifiedAt))} by ${item.lastModifiedBy}"
            tvLastModified.visibility = View.VISIBLE
        } else {
            tvLastModified.visibility = View.GONE
        }
        
        // Request information
        if (item.requestedBy.isNotEmpty()) {
            tvRequestedBy.text = "Requested by: ${item.requestedBy}"
            tvRequestedBy.visibility = View.VISIBLE
            
            if (item.requestedAt > 0) {
                tvRequestedDate.text = "Requested on: ${sdf.format(Date(item.requestedAt))}"
                tvRequestedDate.visibility = View.VISIBLE
            }
        } else {
            tvRequestedBy.visibility = View.GONE
            tvRequestedDate.visibility = View.GONE
        }
        
        // Return information
        if (item.returnedAt > 0) {
            tvReturnedDate.text = "Returned on: ${sdf.format(Date(item.returnedAt))}"
            tvReturnedDate.visibility = View.VISIBLE
        } else {
            tvReturnedDate.visibility = View.GONE
        }
        
        // Status history
        statusHistoryAdapter.submitList(item.statusHistory)
    }
    
    private fun getStatusDisplayName(status: com.example.loginandregistration.admin.models.ItemStatus): String {
        return when (status) {
            com.example.loginandregistration.admin.models.ItemStatus.ACTIVE -> "Active"
            com.example.loginandregistration.admin.models.ItemStatus.REQUESTED -> "Requested"
            com.example.loginandregistration.admin.models.ItemStatus.RETURNED -> "Returned"
            com.example.loginandregistration.admin.models.ItemStatus.DONATION_PENDING -> "Donation Pending"
            com.example.loginandregistration.admin.models.ItemStatus.DONATION_READY -> "Donation Ready"
            com.example.loginandregistration.admin.models.ItemStatus.DONATED -> "Donated"
        }
    }
    
    private fun getStatusColor(status: com.example.loginandregistration.admin.models.ItemStatus): Int {
        return when (status) {
            com.example.loginandregistration.admin.models.ItemStatus.ACTIVE -> R.color.status_active
            com.example.loginandregistration.admin.models.ItemStatus.REQUESTED -> R.color.status_requested
            com.example.loginandregistration.admin.models.ItemStatus.RETURNED -> R.color.status_returned
            com.example.loginandregistration.admin.models.ItemStatus.DONATION_PENDING -> R.color.status_donation_pending
            com.example.loginandregistration.admin.models.ItemStatus.DONATION_READY -> R.color.status_donation_ready
            com.example.loginandregistration.admin.models.ItemStatus.DONATED -> R.color.status_donated
        }
    }
    
    private fun showEditItemDialog(item: EnhancedLostFoundItem) {
        val dialog = EditItemDialog.newInstance(item)
        dialog.show(parentFragmentManager, "EditItemDialog")
    }
    
    private fun showStatusChangeDialog(item: EnhancedLostFoundItem) {
        val dialog = StatusChangeDialog.newInstance(item) { newStatus ->
            // Callback invoked when status is changed
            viewModel.updateItemStatus(item.id, newStatus)
        }
        dialog.show(parentFragmentManager, "StatusChangeDialog")
    }
    
    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete this item? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                itemId?.let { id ->
                    viewModel.deleteItem(id)
                    // Navigate back after deletion
                    parentFragmentManager.popBackStack()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
