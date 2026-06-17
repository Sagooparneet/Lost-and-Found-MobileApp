package com.example.loginandregistration.admin.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.loginandregistration.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Bottom sheet for donation filtering with category, age range, and location
 * Requirements: 1.2, 1.4
 */
class DonationFilterBottomSheet : BottomSheetDialogFragment() {
    
    private lateinit var cgCategory: ChipGroup
    private lateinit var cgAgeRange: ChipGroup
    private lateinit var cgLocation: ChipGroup
    private lateinit var btnApply: MaterialButton
    private lateinit var btnClear: MaterialButton
    
    private var listener: FilterListener? = null
    
    interface FilterListener {
        fun onFiltersApplied(criteria: FilterCriteria)
    }
    
    companion object {
        fun newInstance(listener: FilterListener): DonationFilterBottomSheet {
            return DonationFilterBottomSheet().apply {
                this.listener = listener
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_donation_filter, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupCategoryChips()
        setupAgeRangeChips()
        setupLocationChips()
        setupButtons()
    }
    
    private fun initViews(view: View) {
        cgCategory = view.findViewById(R.id.cgCategory)
        cgAgeRange = view.findViewById(R.id.cgAgeRange)
        cgLocation = view.findViewById(R.id.cgLocation)
        btnApply = view.findViewById(R.id.btnApply)
        btnClear = view.findViewById(R.id.btnClear)
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
    
    private fun setupAgeRangeChips() {
        cgAgeRange.removeAllViews()
        
        val ageRanges = listOf(
            "All",
            "< 30 days",
            "30-90 days",
            "90-180 days",
            "180-365 days",
            "> 365 days"
        )
        
        ageRanges.forEach { range ->
            val chip = Chip(context).apply {
                text = range
                isCheckable = true
                isChecked = range == "All"
            }
            cgAgeRange.addView(chip)
        }
    }
    
    private fun setupLocationChips() {
        cgLocation.removeAllViews()
        
        val locations = listOf(
            "All",
            "Library",
            "Cafeteria",
            "Gym",
            "Classroom",
            "Parking Lot",
            "Office",
            "Other"
        )
        
        locations.forEach { location ->
            val chip = Chip(context).apply {
                text = location
                isCheckable = true
                isChecked = location == "All"
            }
            cgLocation.addView(chip)
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
        // Get selected category
        var selectedCategory: String? = null
        for (i in 0 until cgCategory.childCount) {
            val chip = cgCategory.getChildAt(i) as Chip
            if (chip.isChecked && chip.text.toString() != "All") {
                selectedCategory = chip.text.toString()
                break
            }
        }
        
        // Get selected age range
        var selectedAgeRange: String? = null
        for (i in 0 until cgAgeRange.childCount) {
            val chip = cgAgeRange.getChildAt(i) as Chip
            if (chip.isChecked && chip.text.toString() != "All") {
                selectedAgeRange = chip.text.toString()
                break
            }
        }
        
        // Get selected location
        var selectedLocation: String? = null
        for (i in 0 until cgLocation.childCount) {
            val chip = cgLocation.getChildAt(i) as Chip
            if (chip.isChecked && chip.text.toString() != "All") {
                selectedLocation = chip.text.toString()
                break
            }
        }
        
        val criteria = FilterCriteria(
            category = selectedCategory,
            ageRange = selectedAgeRange,
            location = selectedLocation
        )
        
        listener?.onFiltersApplied(criteria)
        dismiss()
    }
    
    private fun clearFilters() {
        // Reset category chips
        for (i in 0 until cgCategory.childCount) {
            val chip = cgCategory.getChildAt(i) as Chip
            chip.isChecked = chip.text.toString() == "All"
        }
        
        // Reset age range chips
        for (i in 0 until cgAgeRange.childCount) {
            val chip = cgAgeRange.getChildAt(i) as Chip
            chip.isChecked = chip.text.toString() == "All"
        }
        
        // Reset location chips
        for (i in 0 until cgLocation.childCount) {
            val chip = cgLocation.getChildAt(i) as Chip
            chip.isChecked = chip.text.toString() == "All"
        }
    }
    
    data class FilterCriteria(
        val category: String? = null,
        val ageRange: String? = null,
        val location: String? = null
    )
}
