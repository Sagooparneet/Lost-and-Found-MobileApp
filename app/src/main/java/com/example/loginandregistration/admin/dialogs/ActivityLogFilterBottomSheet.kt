package com.example.loginandregistration.admin.dialogs

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.ActionType
import com.example.loginandregistration.admin.models.TargetType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.example.loginandregistration.utils.EditTextUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Bottom sheet dialog for filtering activity logs
 * Requirements: 5.4
 * Task: 12.3 Implement activity log filters
 */
class ActivityLogFilterBottomSheet : BottomSheetDialogFragment() {
    
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var etUserEmail: TextInputEditText
    private lateinit var btnClearFilters: Button
    private lateinit var btnCancel: Button
    private lateinit var btnApplyFilters: Button
    
    // Action type checkboxes
    private lateinit var cbUserLogin: CheckBox
    private lateinit var cbUserLogout: CheckBox
    private lateinit var cbUserRegister: CheckBox
    private lateinit var cbItemReport: CheckBox
    private lateinit var cbItemRequest: CheckBox
    private lateinit var cbItemClaim: CheckBox
    private lateinit var cbUserBlock: CheckBox
    private lateinit var cbUserUnblock: CheckBox
    private lateinit var cbUserRoleChange: CheckBox
    private lateinit var cbUserEdit: CheckBox
    private lateinit var cbItemEdit: CheckBox
    private lateinit var cbItemStatusChange: CheckBox
    private lateinit var cbItemDelete: CheckBox
    private lateinit var cbDonationMarkReady: CheckBox
    private lateinit var cbDonationComplete: CheckBox
    private lateinit var cbNotificationSend: CheckBox
    
    // Target type chips
    private lateinit var chipGroupTargetType: ChipGroup
    private lateinit var chipTargetUser: Chip
    private lateinit var chipTargetItem: Chip
    private lateinit var chipTargetDonation: Chip
    private lateinit var chipTargetNotification: Chip
    private lateinit var chipTargetSystem: Chip
    
    private var startDateMillis: Long = 0
    private var endDateMillis: Long = 0
    private var currentFilters: Map<String, String> = emptyMap()
    private var onFiltersAppliedListener: ((Map<String, String>) -> Unit)? = null
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_activity_log_filter, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        loadCurrentFilters()
        setupDatePickers()
        setupButtons()
    }
    
    private fun initViews(view: View) {
        etStartDate = view.findViewById(R.id.etStartDate)
        etEndDate = view.findViewById(R.id.etEndDate)
        etUserEmail = view.findViewById(R.id.etUserEmail)
        btnClearFilters = view.findViewById(R.id.btnClearFilters)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnApplyFilters = view.findViewById(R.id.btnApplyFilters)
        
        // Action type checkboxes
        cbUserLogin = view.findViewById(R.id.cbUserLogin)
        cbUserLogout = view.findViewById(R.id.cbUserLogout)
        cbUserRegister = view.findViewById(R.id.cbUserRegister)
        cbItemReport = view.findViewById(R.id.cbItemReport)
        cbItemRequest = view.findViewById(R.id.cbItemRequest)
        cbItemClaim = view.findViewById(R.id.cbItemClaim)
        cbUserBlock = view.findViewById(R.id.cbUserBlock)
        cbUserUnblock = view.findViewById(R.id.cbUserUnblock)
        cbUserRoleChange = view.findViewById(R.id.cbUserRoleChange)
        cbUserEdit = view.findViewById(R.id.cbUserEdit)
        cbItemEdit = view.findViewById(R.id.cbItemEdit)
        cbItemStatusChange = view.findViewById(R.id.cbItemStatusChange)
        cbItemDelete = view.findViewById(R.id.cbItemDelete)
        cbDonationMarkReady = view.findViewById(R.id.cbDonationMarkReady)
        cbDonationComplete = view.findViewById(R.id.cbDonationComplete)
        cbNotificationSend = view.findViewById(R.id.cbNotificationSend)
        
        // Target type chips
        chipGroupTargetType = view.findViewById(R.id.chipGroupTargetType)
        chipTargetUser = view.findViewById(R.id.chipTargetUser)
        chipTargetItem = view.findViewById(R.id.chipTargetItem)
        chipTargetDonation = view.findViewById(R.id.chipTargetDonation)
        chipTargetNotification = view.findViewById(R.id.chipTargetNotification)
        chipTargetSystem = view.findViewById(R.id.chipTargetSystem)
    }
    
    private fun loadCurrentFilters() {
        // Load date range
        currentFilters["startDate"]?.toLongOrNull()?.let { startDate ->
            startDateMillis = startDate
            EditTextUtils.safeSetText(etStartDate, dateFormat.format(Date(startDate)))
        }
        
        currentFilters["endDate"]?.toLongOrNull()?.let { endDate ->
            endDateMillis = endDate
            EditTextUtils.safeSetText(etEndDate, dateFormat.format(Date(endDate)))
        }
        
        // Load user email
        currentFilters["userEmail"]?.let { email ->
            EditTextUtils.safeSetText(etUserEmail, email)
        }
        
        // Load action types
        currentFilters["actionTypes"]?.split(",")?.forEach { actionType ->
            when (actionType.trim()) {
                ActionType.USER_LOGIN.name -> cbUserLogin.isChecked = true
                ActionType.USER_LOGOUT.name -> cbUserLogout.isChecked = true
                ActionType.USER_REGISTER.name -> cbUserRegister.isChecked = true
                ActionType.ITEM_REPORT.name -> cbItemReport.isChecked = true
                ActionType.ITEM_REQUEST.name -> cbItemRequest.isChecked = true
                ActionType.ITEM_CLAIM.name -> cbItemClaim.isChecked = true
                ActionType.USER_BLOCK.name -> cbUserBlock.isChecked = true
                ActionType.USER_UNBLOCK.name -> cbUserUnblock.isChecked = true
                ActionType.USER_ROLE_CHANGE.name -> cbUserRoleChange.isChecked = true
                ActionType.USER_EDIT.name -> cbUserEdit.isChecked = true
                ActionType.ITEM_EDIT.name -> cbItemEdit.isChecked = true
                ActionType.ITEM_STATUS_CHANGE.name -> cbItemStatusChange.isChecked = true
                ActionType.ITEM_DELETE.name -> cbItemDelete.isChecked = true
                ActionType.DONATION_MARK_READY.name -> cbDonationMarkReady.isChecked = true
                ActionType.DONATION_COMPLETE.name -> cbDonationComplete.isChecked = true
                ActionType.NOTIFICATION_SEND.name -> cbNotificationSend.isChecked = true
            }
        }
        
        // Load target types
        currentFilters["targetTypes"]?.split(",")?.forEach { targetType ->
            when (targetType.trim()) {
                TargetType.USER.name -> chipTargetUser.isChecked = true
                TargetType.ITEM.name -> chipTargetItem.isChecked = true
                TargetType.DONATION.name -> chipTargetDonation.isChecked = true
                TargetType.NOTIFICATION.name -> chipTargetNotification.isChecked = true
                TargetType.SYSTEM.name -> chipTargetSystem.isChecked = true
            }
        }
    }
    
    private fun setupDatePickers() {
        etStartDate.setOnClickListener {
            showDatePicker { selectedDate ->
                startDateMillis = selectedDate
                EditTextUtils.safeSetText(etStartDate, dateFormat.format(Date(selectedDate)))
            }
        }
        
        etEndDate.setOnClickListener {
            showDatePicker { selectedDate ->
                endDateMillis = selectedDate
                EditTextUtils.safeSetText(etEndDate, dateFormat.format(Date(selectedDate)))
            }
        }
    }
    
    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    
    private fun setupButtons() {
        btnClearFilters.setOnClickListener {
            clearAllFilters()
        }
        
        btnCancel.setOnClickListener {
            dismiss()
        }
        
        btnApplyFilters.setOnClickListener {
            applyFilters()
        }
    }
    
    private fun clearAllFilters() {
        // Clear date range
        startDateMillis = 0
        endDateMillis = 0
        EditTextUtils.safeSetText(etStartDate, "")
        EditTextUtils.safeSetText(etEndDate, "")
        
        // Clear user email
        EditTextUtils.safeSetText(etUserEmail, "")
        
        // Clear action type checkboxes
        cbUserLogin.isChecked = false
        cbUserLogout.isChecked = false
        cbUserRegister.isChecked = false
        cbItemReport.isChecked = false
        cbItemRequest.isChecked = false
        cbItemClaim.isChecked = false
        cbUserBlock.isChecked = false
        cbUserUnblock.isChecked = false
        cbUserRoleChange.isChecked = false
        cbUserEdit.isChecked = false
        cbItemEdit.isChecked = false
        cbItemStatusChange.isChecked = false
        cbItemDelete.isChecked = false
        cbDonationMarkReady.isChecked = false
        cbDonationComplete.isChecked = false
        cbNotificationSend.isChecked = false
        
        // Clear target type chips
        chipTargetUser.isChecked = false
        chipTargetItem.isChecked = false
        chipTargetDonation.isChecked = false
        chipTargetNotification.isChecked = false
        chipTargetSystem.isChecked = false
    }
    
    private fun applyFilters() {
        val filters = mutableMapOf<String, String>()
        
        // Add date range
        if (startDateMillis > 0) {
            filters["startDate"] = startDateMillis.toString()
        }
        if (endDateMillis > 0) {
            filters["endDate"] = endDateMillis.toString()
        }
        
        // Add user email
        val userEmail = etUserEmail.text.toString().trim()
        if (userEmail.isNotBlank()) {
            filters["userEmail"] = userEmail
        }
        
        // Add action types
        val selectedActionTypes = mutableListOf<String>()
        if (cbUserLogin.isChecked) selectedActionTypes.add(ActionType.USER_LOGIN.name)
        if (cbUserLogout.isChecked) selectedActionTypes.add(ActionType.USER_LOGOUT.name)
        if (cbUserRegister.isChecked) selectedActionTypes.add(ActionType.USER_REGISTER.name)
        if (cbItemReport.isChecked) selectedActionTypes.add(ActionType.ITEM_REPORT.name)
        if (cbItemRequest.isChecked) selectedActionTypes.add(ActionType.ITEM_REQUEST.name)
        if (cbItemClaim.isChecked) selectedActionTypes.add(ActionType.ITEM_CLAIM.name)
        if (cbUserBlock.isChecked) selectedActionTypes.add(ActionType.USER_BLOCK.name)
        if (cbUserUnblock.isChecked) selectedActionTypes.add(ActionType.USER_UNBLOCK.name)
        if (cbUserRoleChange.isChecked) selectedActionTypes.add(ActionType.USER_ROLE_CHANGE.name)
        if (cbUserEdit.isChecked) selectedActionTypes.add(ActionType.USER_EDIT.name)
        if (cbItemEdit.isChecked) selectedActionTypes.add(ActionType.ITEM_EDIT.name)
        if (cbItemStatusChange.isChecked) selectedActionTypes.add(ActionType.ITEM_STATUS_CHANGE.name)
        if (cbItemDelete.isChecked) selectedActionTypes.add(ActionType.ITEM_DELETE.name)
        if (cbDonationMarkReady.isChecked) selectedActionTypes.add(ActionType.DONATION_MARK_READY.name)
        if (cbDonationComplete.isChecked) selectedActionTypes.add(ActionType.DONATION_COMPLETE.name)
        if (cbNotificationSend.isChecked) selectedActionTypes.add(ActionType.NOTIFICATION_SEND.name)
        
        if (selectedActionTypes.isNotEmpty()) {
            filters["actionTypes"] = selectedActionTypes.joinToString(",")
        }
        
        // Add target types
        val selectedTargetTypes = mutableListOf<String>()
        if (chipTargetUser.isChecked) selectedTargetTypes.add(TargetType.USER.name)
        if (chipTargetItem.isChecked) selectedTargetTypes.add(TargetType.ITEM.name)
        if (chipTargetDonation.isChecked) selectedTargetTypes.add(TargetType.DONATION.name)
        if (chipTargetNotification.isChecked) selectedTargetTypes.add(TargetType.NOTIFICATION.name)
        if (chipTargetSystem.isChecked) selectedTargetTypes.add(TargetType.SYSTEM.name)
        
        if (selectedTargetTypes.isNotEmpty()) {
            filters["targetTypes"] = selectedTargetTypes.joinToString(",")
        }
        
        // Notify listener and dismiss
        onFiltersAppliedListener?.invoke(filters)
        dismiss()
    }
    
    fun setOnFiltersAppliedListener(listener: (Map<String, String>) -> Unit) {
        onFiltersAppliedListener = listener
    }
    
    companion object {
        fun newInstance(currentFilters: Map<String, String>): ActivityLogFilterBottomSheet {
            val fragment = ActivityLogFilterBottomSheet()
            fragment.currentFilters = currentFilters
            return fragment
        }
    }
}
