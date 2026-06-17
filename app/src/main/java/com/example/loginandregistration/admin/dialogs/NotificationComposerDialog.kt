package com.example.loginandregistration.admin.dialogs

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.NotificationType
import com.example.loginandregistration.admin.models.PushNotification
import com.example.loginandregistration.admin.models.UserRole
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for composing and sending push notifications
 * Requirements: 6.4, 6.7, 6.8
 * Task: 13.2
 */
class NotificationComposerDialog : DialogFragment() {
    
    // Views
    private lateinit var tilTitle: TextInputLayout
    private lateinit var etTitle: TextInputEditText
    private lateinit var tilBody: TextInputLayout
    private lateinit var etBody: TextInputEditText
    private lateinit var rgNotificationType: RadioGroup
    private lateinit var rbCustomAdmin: RadioButton
    private lateinit var rbSystemAnnouncement: RadioButton
    private lateinit var rbSecurityAlert: RadioButton
    private lateinit var rgRecipientType: RadioGroup
    private lateinit var rbAllUsers: RadioButton
    private lateinit var rbRoleBased: RadioButton
    private lateinit var rbIndividual: RadioButton
    private lateinit var llRoleSelection: LinearLayout
    private lateinit var chipGroupRoles: ChipGroup
    private lateinit var chipUser: Chip
    private lateinit var chipAdmin: Chip
    private lateinit var tilUserEmail: TextInputLayout
    private lateinit var etUserEmail: TextInputEditText
    private lateinit var tilActionUrl: TextInputLayout
    private lateinit var etActionUrl: TextInputEditText
    private lateinit var cbSchedule: CheckBox
    private lateinit var llScheduleDateTime: LinearLayout
    private lateinit var btnSelectDate: MaterialButton
    private lateinit var btnSelectTime: MaterialButton
    private lateinit var tvScheduledDateTime: TextView
    private lateinit var btnPreview: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSend: MaterialButton
    
    // Callback
    private var onSendCallback: ((PushNotification) -> Unit)? = null
    
    // Schedule date/time
    private var scheduledCalendar: Calendar? = null
    
    companion object {
        fun newInstance(onSend: (PushNotification) -> Unit): NotificationComposerDialog {
            val dialog = NotificationComposerDialog()
            dialog.onSendCallback = onSend
            return dialog
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_notification_composer, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupListeners()
    }
    
    override fun onStart() {
        super.onStart()
        // Make dialog full width
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    
    private fun initViews(view: View) {
        tilTitle = view.findViewById(R.id.tilTitle)
        etTitle = view.findViewById(R.id.etTitle)
        tilBody = view.findViewById(R.id.tilBody)
        etBody = view.findViewById(R.id.etBody)
        rgNotificationType = view.findViewById(R.id.rgNotificationType)
        rbCustomAdmin = view.findViewById(R.id.rbCustomAdmin)
        rbSystemAnnouncement = view.findViewById(R.id.rbSystemAnnouncement)
        rbSecurityAlert = view.findViewById(R.id.rbSecurityAlert)
        rgRecipientType = view.findViewById(R.id.rgRecipientType)
        rbAllUsers = view.findViewById(R.id.rbAllUsers)
        rbRoleBased = view.findViewById(R.id.rbRoleBased)
        rbIndividual = view.findViewById(R.id.rbIndividual)
        llRoleSelection = view.findViewById(R.id.llRoleSelection)
        chipGroupRoles = view.findViewById(R.id.chipGroupRoles)
        chipUser = view.findViewById(R.id.chipUser)
        chipAdmin = view.findViewById(R.id.chipAdmin)
        tilUserEmail = view.findViewById(R.id.tilUserEmail)
        etUserEmail = view.findViewById(R.id.etUserEmail)
        tilActionUrl = view.findViewById(R.id.tilActionUrl)
        etActionUrl = view.findViewById(R.id.etActionUrl)
        cbSchedule = view.findViewById(R.id.cbSchedule)
        llScheduleDateTime = view.findViewById(R.id.llScheduleDateTime)
        btnSelectDate = view.findViewById(R.id.btnSelectDate)
        btnSelectTime = view.findViewById(R.id.btnSelectTime)
        tvScheduledDateTime = view.findViewById(R.id.tvScheduledDateTime)
        btnPreview = view.findViewById(R.id.btnPreview)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnSend = view.findViewById(R.id.btnSend)
    }
    
    /**
     * Setup all listeners
     * Requirements: 6.4, 6.7, 6.8
     */
    private fun setupListeners() {
        // Recipient type selection
        rgRecipientType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbAllUsers -> {
                    llRoleSelection.visibility = View.GONE
                    tilUserEmail.visibility = View.GONE
                }
                R.id.rbRoleBased -> {
                    llRoleSelection.visibility = View.VISIBLE
                    tilUserEmail.visibility = View.GONE
                }
                R.id.rbIndividual -> {
                    llRoleSelection.visibility = View.GONE
                    tilUserEmail.visibility = View.VISIBLE
                }
            }
        }
        
        // Schedule checkbox
        cbSchedule.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                llScheduleDateTime.visibility = View.VISIBLE
                btnSend.text = "Schedule"
            } else {
                llScheduleDateTime.visibility = View.GONE
                tvScheduledDateTime.visibility = View.GONE
                btnSend.text = "Send"
                scheduledCalendar = null
            }
        }
        
        // Date picker
        btnSelectDate.setOnClickListener {
            showDatePicker()
        }
        
        // Time picker
        btnSelectTime.setOnClickListener {
            showTimePicker()
        }
        
        // Preview button
        btnPreview.setOnClickListener {
            showPreview()
        }
        
        // Cancel button
        btnCancel.setOnClickListener {
            dismiss()
        }
        
        // Send button
        btnSend.setOnClickListener {
            sendNotification()
        }
    }
    
    /**
     * Show date picker dialog
     * Requirements: 6.8
     */
    private fun showDatePicker() {
        val calendar = scheduledCalendar ?: Calendar.getInstance()
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                if (scheduledCalendar == null) {
                    scheduledCalendar = Calendar.getInstance()
                }
                scheduledCalendar?.set(Calendar.YEAR, year)
                scheduledCalendar?.set(Calendar.MONTH, month)
                scheduledCalendar?.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateScheduledDateTimeDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }
    
    /**
     * Show time picker dialog
     * Requirements: 6.8
     */
    private fun showTimePicker() {
        val calendar = scheduledCalendar ?: Calendar.getInstance()
        
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                if (scheduledCalendar == null) {
                    scheduledCalendar = Calendar.getInstance()
                }
                scheduledCalendar?.set(Calendar.HOUR_OF_DAY, hourOfDay)
                scheduledCalendar?.set(Calendar.MINUTE, minute)
                scheduledCalendar?.set(Calendar.SECOND, 0)
                updateScheduledDateTimeDisplay()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        
        timePickerDialog.show()
    }
    
    /**
     * Update scheduled date/time display
     */
    private fun updateScheduledDateTimeDisplay() {
        scheduledCalendar?.let { calendar ->
            val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            tvScheduledDateTime.text = "Scheduled for: ${dateFormat.format(calendar.time)}"
            tvScheduledDateTime.visibility = View.VISIBLE
        }
    }
    
    /**
     * Show notification preview
     * Requirements: 6.5
     */
    private fun showPreview() {
        val title = etTitle.text?.toString()?.trim() ?: ""
        val body = etBody.text?.toString()?.trim() ?: ""
        
        if (title.isBlank() || body.isBlank()) {
            tilTitle.error = if (title.isBlank()) "Title is required" else null
            tilBody.error = if (body.isBlank()) "Message is required" else null
            return
        }
        
        // Clear errors
        tilTitle.error = null
        tilBody.error = null
        
        // Create preview notification
        val notification = buildNotification()
        
        // Show preview dialog
        val previewDialog = NotificationPreviewDialog.newInstance(notification)
        previewDialog.show(parentFragmentManager, "NotificationPreviewDialog")
    }
    
    /**
     * Validate and send notification
     * Requirements: 6.4, 6.7
     */
    private fun sendNotification() {
        val title = etTitle.text?.toString()?.trim() ?: ""
        val body = etBody.text?.toString()?.trim() ?: ""
        
        // Validate title
        if (title.isBlank()) {
            tilTitle.error = "Title is required"
            return
        }
        
        if (title.length < 3) {
            tilTitle.error = "Title must be at least 3 characters"
            return
        }
        
        // Validate body
        if (body.isBlank()) {
            tilBody.error = "Message is required"
            return
        }
        
        if (body.length < 10) {
            tilBody.error = "Message must be at least 10 characters"
            return
        }
        
        // Validate recipient
        if (rbIndividual.isChecked) {
            val email = etUserEmail.text?.toString()?.trim() ?: ""
            if (email.isBlank()) {
                tilUserEmail.error = "User email is required"
                return
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilUserEmail.error = "Invalid email address"
                return
            }
        }
        
        // Validate schedule
        if (cbSchedule.isChecked) {
            if (scheduledCalendar == null) {
                btnSelectDate.error = "Please select date and time"
                return
            }
            if (scheduledCalendar!!.timeInMillis <= System.currentTimeMillis()) {
                btnSelectDate.error = "Scheduled time must be in the future"
                return
            }
        }
        
        // Clear all errors
        tilTitle.error = null
        tilBody.error = null
        tilUserEmail.error = null
        
        // Build notification
        val notification = buildNotification()
        
        // Send through callback
        onSendCallback?.invoke(notification)
        
        // Dismiss dialog
        dismiss()
    }
    
    /**
     * Build notification object from form inputs
     * Requirements: 6.4, 6.7, 6.8
     */
    private fun buildNotification(): PushNotification {
        val title = etTitle.text?.toString()?.trim() ?: ""
        val body = etBody.text?.toString()?.trim() ?: ""
        val actionUrl = etActionUrl.text?.toString()?.trim() ?: ""
        
        // Determine notification type
        val type = when (rgNotificationType.checkedRadioButtonId) {
            R.id.rbSystemAnnouncement -> NotificationType.SYSTEM_ANNOUNCEMENT
            R.id.rbSecurityAlert -> NotificationType.SECURITY_ALERT
            else -> NotificationType.CUSTOM_ADMIN
        }
        
        // Determine recipients
        val targetUsers = mutableListOf<String>()
        val targetRoles = mutableListOf<UserRole>()
        
        when (rgRecipientType.checkedRadioButtonId) {
            R.id.rbAllUsers -> {
                targetUsers.add("ALL")
            }
            R.id.rbRoleBased -> {
                if (chipUser.isChecked) targetRoles.add(UserRole.STUDENT)
                if (chipAdmin.isChecked) targetRoles.add(UserRole.ADMIN)
            }
            R.id.rbIndividual -> {
                val email = etUserEmail.text?.toString()?.trim() ?: ""
                if (email.isNotBlank()) {
                    targetUsers.add(email)
                }
            }
        }
        
        // Get scheduled time
        val scheduledFor = if (cbSchedule.isChecked && scheduledCalendar != null) {
            scheduledCalendar!!.timeInMillis
        } else {
            0L
        }
        
        // Get current admin user
        val currentUser = FirebaseAuth.getInstance().currentUser
        val createdBy = currentUser?.email ?: "admin@gmail.com"
        
        return PushNotification(
            id = "", // Will be generated by Firestore
            title = title,
            body = body,
            type = type,
            targetUsers = targetUsers,
            targetRoles = targetRoles,
            actionUrl = actionUrl,
            createdBy = createdBy,
            createdAt = System.currentTimeMillis(),
            scheduledFor = scheduledFor
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        onSendCallback = null
    }
}
