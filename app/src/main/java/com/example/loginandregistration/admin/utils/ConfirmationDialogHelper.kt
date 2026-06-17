package com.example.loginandregistration.admin.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog

/**
 * Helper for showing confirmation dialogs for destructive or important actions
 * Requirements: 8.3, 8.6
 */
object ConfirmationDialogHelper {
    
    /**
     * Show a confirmation dialog
     */
    fun showConfirmation(
        context: Context,
        title: String,
        message: String,
        positiveButtonText: String = "Confirm",
        negativeButtonText: String = "Cancel",
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton(negativeButtonText) { dialog, _ ->
                onCancel?.invoke()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Show a delete confirmation dialog
     */
    fun showDeleteConfirmation(
        context: Context,
        itemName: String,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        showConfirmation(
            context = context,
            title = "Delete $itemName?",
            message = "Are you sure you want to delete this $itemName? This action cannot be undone.",
            positiveButtonText = "Delete",
            negativeButtonText = "Cancel",
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }
    
    /**
     * Show a block user confirmation dialog
     */
    fun showBlockUserConfirmation(
        context: Context,
        userName: String,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        showConfirmation(
            context = context,
            title = "Block User?",
            message = "Are you sure you want to block $userName? They will not be able to access the application.",
            positiveButtonText = "Block",
            negativeButtonText = "Cancel",
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }
    
    /**
     * Show an unblock user confirmation dialog
     */
    fun showUnblockUserConfirmation(
        context: Context,
        userName: String,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        showConfirmation(
            context = context,
            title = "Unblock User?",
            message = "Are you sure you want to unblock $userName? They will regain access to the application.",
            positiveButtonText = "Unblock",
            negativeButtonText = "Cancel",
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }
    
    /**
     * Show a role change confirmation dialog
     */
    fun showRoleChangeConfirmation(
        context: Context,
        userName: String,
        newRole: String,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        showConfirmation(
            context = context,
            title = "Change User Role?",
            message = "Are you sure you want to change $userName's role to $newRole?",
            positiveButtonText = "Change Role",
            negativeButtonText = "Cancel",
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }
    
    /**
     * Show a donation confirmation dialog
     */
    fun showDonationConfirmation(
        context: Context,
        itemName: String,
        action: String,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        showConfirmation(
            context = context,
            title = "$action Item?",
            message = "Are you sure you want to $action '$itemName'?",
            positiveButtonText = action,
            negativeButtonText = "Cancel",
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }
    
    /**
     * Show a status change confirmation dialog
     */
    fun showStatusChangeConfirmation(
        context: Context,
        itemName: String,
        newStatus: String,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        showConfirmation(
            context = context,
            title = "Change Status?",
            message = "Are you sure you want to change the status of '$itemName' to $newStatus?",
            positiveButtonText = "Change Status",
            negativeButtonText = "Cancel",
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }
    
    /**
     * Show a generic warning dialog
     */
    fun showWarning(
        context: Context,
        title: String,
        message: String,
        buttonText: String = "OK",
        onDismiss: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(buttonText) { dialog, _ ->
                onDismiss?.invoke()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Show an info dialog
     */
    fun showInfo(
        context: Context,
        title: String,
        message: String,
        buttonText: String = "OK",
        onDismiss: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(buttonText) { dialog, _ ->
                onDismiss?.invoke()
                dialog.dismiss()
            }
            .show()
    }
    
    /**
     * Show a multi-choice confirmation dialog
     */
    fun showMultiChoiceConfirmation(
        context: Context,
        title: String,
        message: String,
        choices: Array<String>,
        onChoice: (Int) -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setItems(choices) { dialog, which ->
                onChoice(which)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

/**
 * Extension functions for Context to show confirmation dialogs
 */
fun Context.showConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    ConfirmationDialogHelper.showConfirmation(
        this, title, message,
        onConfirm = onConfirm,
        onCancel = onCancel
    )
}

fun Context.showDeleteConfirmation(
    itemName: String,
    onConfirm: () -> Unit
) {
    ConfirmationDialogHelper.showDeleteConfirmation(this, itemName, onConfirm)
}

fun Context.showBlockUserConfirmation(
    userName: String,
    onConfirm: () -> Unit
) {
    ConfirmationDialogHelper.showBlockUserConfirmation(this, userName, onConfirm)
}

fun Context.showUnblockUserConfirmation(
    userName: String,
    onConfirm: () -> Unit
) {
    ConfirmationDialogHelper.showUnblockUserConfirmation(this, userName, onConfirm)
}

fun Context.showWarningDialog(
    title: String,
    message: String,
    onDismiss: (() -> Unit)? = null
) {
    ConfirmationDialogHelper.showWarning(this, title, message, onDismiss = onDismiss)
}

fun Context.showInfoDialog(
    title: String,
    message: String,
    onDismiss: (() -> Unit)? = null
) {
    ConfirmationDialogHelper.showInfo(this, title, message, onDismiss = onDismiss)
}
