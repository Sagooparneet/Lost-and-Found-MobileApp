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
import com.example.loginandregistration.LostFoundItem
import com.example.loginandregistration.R
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class ItemDetailsDialogFragment : DialogFragment() {
    
    companion object {
        private const val ARG_ITEM = "item"
        
        fun newInstance(item: LostFoundItem): ItemDetailsDialogFragment {
            val fragment = ItemDetailsDialogFragment()
            val args = Bundle()
            args.putSerializable(ARG_ITEM, item)
            fragment.arguments = args
            return fragment
        }
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
        
        val item = arguments?.getSerializable(ARG_ITEM) as? LostFoundItem ?: return
        
        val ivItemImage: ImageView = view.findViewById(R.id.ivItemImage)
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val tvItemDescription: TextView = view.findViewById(R.id.tvItemDescription)
        val tvItemCategory: TextView = view.findViewById(R.id.tvItemCategory)
        val tvItemLocation: TextView = view.findViewById(R.id.tvItemLocation)
        val tvItemStatus: TextView = view.findViewById(R.id.tvItemStatus)
        val tvReporterName: TextView = view.findViewById(R.id.tvReporterName)
        val tvReporterEmail: TextView = view.findViewById(R.id.tvReporterEmail)
        val tvReportDate: TextView = view.findViewById(R.id.tvReportDate)
        val btnClose: MaterialButton = view.findViewById(R.id.btnClose)
        
        // Populate data
        tvItemName.text = item.name
        tvItemDescription.text = item.description
        tvItemCategory.text = "Category: Electronics" // Placeholder since category doesn't exist
        tvItemLocation.text = "Location: ${item.location}"
        tvItemStatus.text = "Status: ${if (item.isLost) "Lost" else "Found"}"
        tvReporterName.text = "Reporter: ${item.userEmail}"
        tvReporterEmail.text = "Contact: ${item.contactInfo}"
        
        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        tvReportDate.text = "Reported: ${sdf.format(item.timestamp.toDate())}"
        
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
        
        btnClose.setOnClickListener {
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