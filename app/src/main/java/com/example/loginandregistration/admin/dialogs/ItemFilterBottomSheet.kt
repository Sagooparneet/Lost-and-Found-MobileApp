package com.example.loginandregistration.admin.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.ItemStatus
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Bottom sheet for item filtering with status and category
 * Requirements: 1.1, 1.3
 */
class ItemFilterBottomSheet : BottomSheetDialogFragment() {
    
    private lateinit var cgStatus: ChipGroup
    private lateinit var cgCategory: ChipGroup
    private lateinit var btnApply: MaterialButton
    private lateinit var btnClear: MaterialButton
    
    private var listener: FilterListener? = null
    private var onFiltersAppliedCallback: ((FilterCriteria) -> Unit)? = null
    
    interface FilterListener {
        fun onFiltersApplied(criteria: FilterCriteria)
    }
    
    companion object {
        fun newInstance(listener: FilterListener): ItemFilterBottomSheet {
            return ItemFilterBottomSheet().apply {
                this.listener = listener
            }
        }
        
        // Backward compatibility with lambda callback
        fun newInstance(onFiltersApplied: (FilterCriteria) -> Unit): ItemFilterBottomSheet {
            return ItemFilterBottomSheet().apply {
                this.onFiltersAppliedCallback = onFiltersApplied
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_item_filter, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupStatusChips()
        setupCategoryChips()
        setupButtons()
    }
    
    private fun initViews(view: View) {
        cgStatus = view.findViewById(R.id.cgStatus)
        cgCategory = view.findViewById(R.id.cgCategory)
        btnApply = view.findViewById(R.id.btnApply)
        btnClear = view.findViewById(R.id.btnClear)
    }
    
    private fun setupStatusChips() {
        cgStatus.removeAllViews()
        
        // Add "All" chip
        val allChip = Chip(context).apply {
            text = "All"
            isCheckable = true
            isChecked = true
        }
        cgStatus.addView(allChip)
        
        // Add status chips
        ItemStatus.values().forEach { status ->
            val chip = Chip(context).apply {
                text = getStatusDisplayName(status)
                isCheckable = true
                isChecked = false
            }
            cgStatus.addView(chip)
        }
    }
    
    private fun setupCategoryChips() {
        cgCategory.removeAllViews()
        
        val categories = listOf(
            "All",
            "Electronics",
            "Clothing",
            "Books",
            "Accessories",
            "Documents",
            "Keys",
            "Bags",
            "Jewelry",
            "Sports Equipment",
            "Other"
        )
        
        categories.forEach { category ->
            val chip = Chip(context).apply {
                text = category
                isCheckable = true
                isChecked = category == "All"
            }
            cgCategory.addView(chip)
        }
    }
    
    private fun setupButtons() {
        btnApply.setOnClickListener {
            applyFilters()
        }
        
        btnClear.setOnClickListener {
            clearFilters()
        }
    }
    
    private fun applyFilters() {
        // Get selected status
        var selectedStatus: ItemStatus? = null
        for (i in 0 until cgStatus.childCount) {
            val chip = cgStatus.getChildAt(i) as Chip
            if (chip.isChecked && chip.text.toString() != "All") {
                selectedStatus = getStatusFromDisplayName(chip.text.toString())
                break
            }
        }
        
        // Get selected category
        var selectedCategory: String? = null
        for (i in 0 until cgCategory.childCount) {
            val chip = cgCategory.getChildAt(i) as Chip
            if (chip.isChecked && chip.text.toString() != "All") {
                selectedCategory = chip.text.toString()
                break
            }
        }
        
        val criteria = FilterCriteria(
            status = selectedStatus,
            category = selectedCategory
        )
        
        listener?.onFiltersApplied(criteria)
        onFiltersAppliedCallback?.invoke(criteria)
        dismiss()
    }
    
    private fun clearFilters() {
        // Reset status chips
        for (i in 0 until cgStatus.childCount) {
            val chip = cgStatus.getChildAt(i) as Chip
            chip.isChecked = chip.text.toString() == "All"
        }
        
        // Reset category chips
        for (i in 0 until cgCategory.childCount) {
            val chip = cgCategory.getChildAt(i) as Chip
            chip.isChecked = chip.text.toString() == "All"
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
    
    private fun getStatusFromDisplayName(displayName: String): ItemStatus? {
        return when (displayName) {
            "Active" -> ItemStatus.ACTIVE
            "Requested" -> ItemStatus.REQUESTED
            "Returned" -> ItemStatus.RETURNED
            "Donation Pending" -> ItemStatus.DONATION_PENDING
            "Donation Ready" -> ItemStatus.DONATION_READY
            "Donated" -> ItemStatus.DONATED
            else -> null
        }
    }
    
    data class FilterCriteria(
        val status: ItemStatus? = null,
        val category: String? = null
    )
}
