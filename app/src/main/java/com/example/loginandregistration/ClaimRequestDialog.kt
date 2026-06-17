package com.example.loginandregistration

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText

/**
 * Dialog fragment for users to submit claim requests for found items.
 * Requirements: 5.2, 5.3
 */
class ClaimRequestDialog : DialogFragment() {

    private lateinit var etReason: TextInputEditText
    private lateinit var etProofDescription: TextInputEditText
    private lateinit var btnSubmit: Button
    private lateinit var btnCancel: Button

    private var onSubmitListener: ((reason: String, proofDescription: String) -> Unit)? = null

    companion object {
        private const val ARG_ITEM_ID = "item_id"
        private const val ARG_ITEM_NAME = "item_name"

        fun newInstance(itemId: String, itemName: String): ClaimRequestDialog {
            val dialog = ClaimRequestDialog()
            val args = Bundle()
            args.putString(ARG_ITEM_ID, itemId)
            args.putString(ARG_ITEM_NAME, itemName)
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_claim_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etReason = view.findViewById(R.id.et_claim_reason)
        etProofDescription = view.findViewById(R.id.et_proof_description)
        btnSubmit = view.findViewById(R.id.btn_submit)
        btnCancel = view.findViewById(R.id.btn_cancel)

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnSubmit.setOnClickListener {
            submitClaimRequest()
        }
    }

    override fun onStart() {
        super.onStart()
        // Make dialog wider
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /**
     * Validates and submits the claim request
     * Requirements: 5.2, 5.3
     */
    private fun submitClaimRequest() {
        val reason = etReason.text.toString().trim()
        val proofDescription = etProofDescription.text.toString().trim()

        // Validate reason field (required)
        if (reason.isEmpty()) {
            etReason.error = "Please provide a reason for claiming this item"
            etReason.requestFocus()
            return
        }

        if (reason.length < 10) {
            etReason.error = "Please provide a more detailed reason (at least 10 characters)"
            etReason.requestFocus()
            return
        }

        // Call the listener with the validated data
        onSubmitListener?.invoke(reason, proofDescription)
        dismiss()
    }

    /**
     * Sets the listener to be called when the claim request is submitted
     */
    fun setOnSubmitListener(listener: (reason: String, proofDescription: String) -> Unit) {
        this.onSubmitListener = listener
    }
}
