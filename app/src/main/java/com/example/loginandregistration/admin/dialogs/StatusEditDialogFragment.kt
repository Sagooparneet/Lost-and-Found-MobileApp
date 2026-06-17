package com.example.loginandregistration.admin.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.loginandregistration.LostFoundItem
import com.example.loginandregistration.R
import com.google.android.material.button.MaterialButton

class StatusEditDialogFragment : DialogFragment() {
    
    private var onStatusChanged: ((String) -> Unit)? = null
    
    companion object {
        private const val ARG_ITEM = "item"
        
        fun newInstance(item: LostFoundItem, onStatusChanged: (String) -> Unit): StatusEditDialogFragment {
            val fragment = StatusEditDialogFragment()
            fragment.onStatusChanged = onStatusChanged
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
        return inflater.inflate(R.layout.dialog_status_edit, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val item = arguments?.getSerializable(ARG_ITEM) as? LostFoundItem ?: return
        
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val rgStatus: RadioGroup = view.findViewById(R.id.rgStatus)
        val rbLost: RadioButton = view.findViewById(R.id.rbLost)
        val rbFound: RadioButton = view.findViewById(R.id.rbFound)
        val rbReceived: RadioButton = view.findViewById(R.id.rbReceived)
        val rbPending: RadioButton = view.findViewById(R.id.rbPending)
        val btnCancel: MaterialButton = view.findViewById(R.id.btnCancel)
        val btnUpdate: MaterialButton = view.findViewById(R.id.btnUpdate)
        
        tvItemName.text = "Update status for: ${item.name}"
        
        // Set current status
        if (item.isLost) {
            rbLost.isChecked = true
        } else {
            rbFound.isChecked = true
        }
        
        btnCancel.setOnClickListener {
            dismiss()
        }
        
        btnUpdate.setOnClickListener {
            val selectedId = rgStatus.checkedRadioButtonId
            val newStatus = when (selectedId) {
                R.id.rbLost -> "lost"
                R.id.rbFound -> "found"
                R.id.rbReceived -> "received"
                R.id.rbPending -> "pending"
                else -> if (item.isLost) "lost" else "found"
            }
            
            val currentStatus = if (item.isLost) "lost" else "found"
            if (newStatus != currentStatus) {
                onStatusChanged?.invoke(newStatus)
            }
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