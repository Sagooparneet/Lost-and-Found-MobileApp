package com.example.loginandregistration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ItemDetailsDialog : DialogFragment() {
    
    private lateinit var item: LostFoundItem
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "ItemDetailsDialog"
        private const val ARG_ITEM = "item"
        
        fun newInstance(item: LostFoundItem): ItemDetailsDialog {
            val dialog = ItemDetailsDialog()
            val args = Bundle()
            args.putSerializable(ARG_ITEM, item)
            dialog.arguments = args
            return dialog
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        item = arguments?.getSerializable(ARG_ITEM) as? LostFoundItem 
            ?: throw IllegalArgumentException("Item is required")
        setStyle(STYLE_NORMAL, R.style.Theme_LoginAndRegistration)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_item_details, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val tvItemDescription: TextView = view.findViewById(R.id.tvItemDescription)
        val tvItemType: TextView = view.findViewById(R.id.tvItemType)
        val tvItemCategory: TextView = view.findViewById(R.id.tvItemCategory)
        val tvItemLocation: TextView = view.findViewById(R.id.tvItemLocation)
        val tvItemStatus: TextView = view.findViewById(R.id.tvItemStatus)
        val tvReporterName: TextView = view.findViewById(R.id.tvReporterName)
        val tvReporterEmail: TextView = view.findViewById(R.id.tvReporterEmail)
        val tvReportDate: TextView = view.findViewById(R.id.tvReportDate)
        val btnRequestItem: MaterialButton = view.findViewById(R.id.btnRequestItem)
        val btnClose: MaterialButton = view.findViewById(R.id.btnClose)
        
        // Set item name
        tvItemName.text = item.name
        
        // Set item description
        tvItemDescription.text = if (item.description.isNotEmpty()) {
            item.description
        } else {
            "No description available"
        }
        
        // Set item type badge (Lost/Found/Returned)
        val context = requireContext()
        val itemType = when {
            item.status.equals("Returned", ignoreCase = true) -> "Returned"
            item.isLost -> "Lost"
            else -> "Found"
        }
        tvItemType.text = itemType
        
        // Set background color based on type
        when (itemType) {
            "Returned" -> tvItemType.setBackgroundColor(context.getColor(R.color.status_returned))
            "Lost" -> tvItemType.setBackgroundColor(context.getColor(R.color.lost_tag))
            "Found" -> tvItemType.setBackgroundColor(context.getColor(R.color.found_tag))
        }
        
        // Set item category (without prefix)
        tvItemCategory.text = item.category.ifEmpty { "Not specified" }
        
        // Set item location (without prefix)
        tvItemLocation.text = item.location.ifEmpty { "Not specified" }
        
        // Set item status (without prefix)
        tvItemStatus.text = item.status
        
        // Set reporter information (without prefixes)
        tvReporterName.text = item.userEmail.substringBefore("@")
        tvReporterEmail.text = item.userEmail
        
        // Set report date (without prefix)
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        val reportDate = item.timestamp.toDate()
        tvReportDate.text = dateFormat.format(reportDate)
        
        // Request Item button
        btnRequestItem.setOnClickListener {
            showRequestDialog()
        }
        
        // Close button
        btnClose.setOnClickListener {
            dismiss()
        }
    }
    
    private fun showRequestDialog() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please sign in to request items", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Check if user already has a pending request for this item
        db.collection("itemRequests")
            .whereEqualTo("itemId", item.id)
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("status", ItemRequest.RequestStatus.PENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    Toast.makeText(requireContext(), "You already have a pending request for this item", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                
                // Show simple input dialog to enter reason
                val input = EditText(requireContext())
                input.hint = "Why do you need this item?"
                input.minLines = 3
                input.maxLines = 5
                input.setPadding(50, 30, 50, 30)
                
                AlertDialog.Builder(requireContext())
                    .setTitle("Request Item")
                    .setMessage("Please provide a reason for requesting this item:")
                    .setView(input)
                    .setPositiveButton("Submit") { _, _ ->
                        val reason = input.text.toString().trim()
                        if (reason.isEmpty()) {
                            Toast.makeText(requireContext(), "Please provide a reason", Toast.LENGTH_SHORT).show()
                        } else {
                            submitItemRequest(reason)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error checking existing requests: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun submitItemRequest(reason: String) {
        val currentUser = auth.currentUser ?: return
        
        val requestId = db.collection("itemRequests").document().id
        val itemRequest = ItemRequest(
            requestId = requestId,
            itemId = item.id,
            itemName = item.name,
            userId = currentUser.uid,
            userEmail = currentUser.email ?: "",
            userName = currentUser.email?.substringBefore("@") ?: "",
            reason = reason,
            status = ItemRequest.RequestStatus.PENDING
        )
        
        db.collection("itemRequests")
            .document(requestId)
            .set(itemRequest)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Request submitted successfully", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to submit request: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }
}
