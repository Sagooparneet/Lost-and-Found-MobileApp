package com.example.loginandregistration.admin.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.navigation.findNavController
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.fragments.ItemDetailsFragment
import com.example.loginandregistration.admin.fragments.UserDetailsFragment
import com.example.loginandregistration.admin.models.ActivityLog
import com.example.loginandregistration.admin.models.TargetType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for displaying complete activity log details
 * Requirements: 5.6
 * Task: 12.5 Create ActivityDetailDialog
 */
class ActivityDetailDialog : DialogFragment() {
    
    private lateinit var activityLog: ActivityLog
    
    private lateinit var tvActionType: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvActorEmail: TextView
    private lateinit var tvActorRole: TextView
    private lateinit var tvTargetType: TextView
    private lateinit var tvTargetId: TextView
    private lateinit var layoutPreviousValue: LinearLayout
    private lateinit var tvPreviousValue: TextView
    private lateinit var layoutNewValue: LinearLayout
    private lateinit var tvNewValue: TextView
    private lateinit var tvTimestamp: TextView
    private lateinit var layoutDeviceInfo: LinearLayout
    private lateinit var tvDeviceInfo: TextView
    private lateinit var layoutIpAddress: LinearLayout
    private lateinit var tvIpAddress: TextView
    private lateinit var layoutMetadata: LinearLayout
    private lateinit var tvMetadata: TextView
    private lateinit var btnViewRelatedEntity: Button
    private lateinit var btnClose: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog)
        
        // Get activity log from arguments
        arguments?.getSerializable(ARG_ACTIVITY_LOG)?.let {
            activityLog = it as ActivityLog
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_activity_detail, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        populateData()
        setupButtons()
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
        tvActionType = view.findViewById(R.id.tvActionType)
        tvDescription = view.findViewById(R.id.tvDescription)
        tvActorEmail = view.findViewById(R.id.tvActorEmail)
        tvActorRole = view.findViewById(R.id.tvActorRole)
        tvTargetType = view.findViewById(R.id.tvTargetType)
        tvTargetId = view.findViewById(R.id.tvTargetId)
        layoutPreviousValue = view.findViewById(R.id.layoutPreviousValue)
        tvPreviousValue = view.findViewById(R.id.tvPreviousValue)
        layoutNewValue = view.findViewById(R.id.layoutNewValue)
        tvNewValue = view.findViewById(R.id.tvNewValue)
        tvTimestamp = view.findViewById(R.id.tvTimestamp)
        layoutDeviceInfo = view.findViewById(R.id.layoutDeviceInfo)
        tvDeviceInfo = view.findViewById(R.id.tvDeviceInfo)
        layoutIpAddress = view.findViewById(R.id.layoutIpAddress)
        tvIpAddress = view.findViewById(R.id.tvIpAddress)
        layoutMetadata = view.findViewById(R.id.layoutMetadata)
        tvMetadata = view.findViewById(R.id.tvMetadata)
        btnViewRelatedEntity = view.findViewById(R.id.btnViewRelatedEntity)
        btnClose = view.findViewById(R.id.btnClose)
    }
    
    private fun populateData() {
        // Set action type
        tvActionType.text = activityLog.actionType.getDisplayName()
        
        // Set description
        tvDescription.text = activityLog.description
        
        // Set actor information
        tvActorEmail.text = activityLog.actorEmail
        tvActorRole.text = "Role: ${activityLog.actorRole.name}"
        
        // Set target information
        tvTargetType.text = "Type: ${activityLog.targetType.getDisplayName()}"
        tvTargetId.text = if (activityLog.targetId.isNotBlank()) {
            "ID: ${activityLog.targetId}"
        } else {
            "ID: N/A"
        }
        
        // Set previous value if available
        if (activityLog.previousValue.isNotBlank()) {
            layoutPreviousValue.visibility = View.VISIBLE
            tvPreviousValue.text = activityLog.previousValue
        } else {
            layoutPreviousValue.visibility = View.GONE
        }
        
        // Set new value if available
        if (activityLog.newValue.isNotBlank()) {
            layoutNewValue.visibility = View.VISIBLE
            tvNewValue.text = activityLog.newValue
        } else {
            layoutNewValue.visibility = View.GONE
        }
        
        // Set timestamp
        val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        tvTimestamp.text = dateFormat.format(Date(activityLog.timestamp))
        
        // Set device info if available
        if (activityLog.deviceInfo.isNotBlank()) {
            layoutDeviceInfo.visibility = View.VISIBLE
            tvDeviceInfo.text = activityLog.deviceInfo
        } else {
            layoutDeviceInfo.visibility = View.GONE
        }
        
        // Set IP address if available
        if (activityLog.ipAddress.isNotBlank()) {
            layoutIpAddress.visibility = View.VISIBLE
            tvIpAddress.text = activityLog.ipAddress
        } else {
            layoutIpAddress.visibility = View.GONE
        }
        
        // Set metadata if available
        if (activityLog.metadata.isNotEmpty()) {
            layoutMetadata.visibility = View.VISIBLE
            val metadataText = activityLog.metadata.entries.joinToString("\n") { (key, value) ->
                "$key: $value"
            }
            tvMetadata.text = metadataText
        } else {
            layoutMetadata.visibility = View.GONE
        }
    }
    
    private fun setupButtons() {
        btnClose.setOnClickListener {
            dismiss()
        }
        
        // Show "View Related" button only if target ID is available
        if (activityLog.targetId.isNotBlank() && 
            (activityLog.targetType == TargetType.USER || activityLog.targetType == TargetType.ITEM)) {
            btnViewRelatedEntity.visibility = View.VISIBLE
            btnViewRelatedEntity.setOnClickListener {
                navigateToRelatedEntity()
            }
        } else {
            btnViewRelatedEntity.visibility = View.GONE
        }
    }
    
    private fun navigateToRelatedEntity() {
        dismiss()
        
        val bundle = Bundle()
        when (activityLog.targetType) {
            TargetType.USER -> {
                // Navigate to user details using Navigation component
                bundle.putString("user_id", activityLog.targetId)
                try {
                    requireActivity().findNavController(R.id.nav_host_fragment_activity_admin)
                        .navigate(R.id.userDetailsFragment, bundle)
                } catch (e: Exception) {
                    com.google.android.material.snackbar.Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        "Unable to navigate to user details",
                        com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            TargetType.ITEM -> {
                // Navigate to item details using Navigation component
                bundle.putString("item_id", activityLog.targetId)
                try {
                    requireActivity().findNavController(R.id.nav_host_fragment_activity_admin)
                        .navigate(R.id.itemDetailsFragment, bundle)
                } catch (e: Exception) {
                    com.google.android.material.snackbar.Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        "Unable to navigate to item details",
                        com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            else -> {
                // Show message that navigation is not available for this type
                com.google.android.material.snackbar.Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    "Navigation not available for ${activityLog.targetType.getDisplayName()}",
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    companion object {
        private const val ARG_ACTIVITY_LOG = "activity_log"
        
        fun newInstance(activityLog: ActivityLog): ActivityDetailDialog {
            val dialog = ActivityDetailDialog()
            val args = Bundle()
            args.putSerializable(ARG_ACTIVITY_LOG, activityLog)
            dialog.arguments = args
            return dialog
        }
    }
}
