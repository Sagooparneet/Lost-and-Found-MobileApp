package com.example.loginandregistration.admin.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.loginandregistration.R

/**
 * Dialog for showing export progress with cancel option
 * Requirements: 4.1
 * Task: 14.2
 */
class ExportProgressDialog : DialogFragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var statusText: TextView
    private lateinit var cancelButton: Button

    private var onCancelListener: (() -> Unit)? = null
    private var isCancellable: Boolean = true

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_CANCELLABLE = "cancellable"

        fun newInstance(title: String = "Exporting Data", cancellable: Boolean = true): ExportProgressDialog {
            return ExportProgressDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putBoolean(ARG_CANCELLABLE, cancellable)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
        isCancellable = arguments?.getBoolean(ARG_CANCELLABLE, true) ?: true
        isCancelable = isCancellable
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_export_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBar)
        progressText = view.findViewById(R.id.progressText)
        statusText = view.findViewById(R.id.statusText)
        cancelButton = view.findViewById(R.id.cancelButton)

        // Set title
        val title = arguments?.getString(ARG_TITLE) ?: "Exporting Data"
        view.findViewById<TextView>(R.id.titleText).text = title

        // Setup cancel button
        if (isCancellable) {
            cancelButton.visibility = View.VISIBLE
            cancelButton.setOnClickListener {
                onCancelListener?.invoke()
                dismiss()
            }
        } else {
            cancelButton.visibility = View.GONE
        }

        // Initialize progress
        updateProgress(0, "Preparing export...")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    /**
     * Updates the progress bar and status text
     */
    fun updateProgress(progress: Int, status: String) {
        if (!isAdded) return

        progressBar.progress = progress
        progressText.text = "$progress%"
        statusText.text = status
    }

    /**
     * Shows completion state
     */
    fun showCompletion(message: String = "Export completed successfully!") {
        if (!isAdded) return

        progressBar.progress = 100
        progressText.text = "100%"
        statusText.text = message
        cancelButton.text = "Close"
        cancelButton.visibility = View.VISIBLE
        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    /**
     * Shows error state
     */
    fun showError(message: String) {
        if (!isAdded) return

        statusText.text = message
        statusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        cancelButton.text = "Close"
        cancelButton.visibility = View.VISIBLE
        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    /**
     * Sets the cancel listener
     */
    fun setOnCancelListener(listener: () -> Unit) {
        onCancelListener = listener
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancelListener?.invoke()
    }
}
