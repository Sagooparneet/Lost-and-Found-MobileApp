package com.example.loginandregistration.security.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.loginandregistration.ItemRequest
import com.example.loginandregistration.R
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class ItemRequestAdapter(
    private val onApproveClicked: (ItemRequest) -> Unit,
    private val onRejectClicked: (ItemRequest) -> Unit
) : ListAdapter<ItemRequest, ItemRequestAdapter.ItemRequestViewHolder>(ItemRequestDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemRequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_security_report, parent, false)
        return ItemRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemRequestViewHolder, position: Int) {
        val request = getItem(position)
        holder.bind(request, onApproveClicked, onRejectClicked)
    }

    class ItemRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemName: TextView = itemView.findViewById(R.id.tv_item_name_security)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_report_status_security)
        private val btnApprove: MaterialButton = itemView.findViewById(R.id.btn_approve)
        private val btnReject: MaterialButton = itemView.findViewById(R.id.btn_reject)
        
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(request: ItemRequest, onApprove: (ItemRequest) -> Unit, onReject: (ItemRequest) -> Unit) {
            tvItemName.text = request.itemName
            val statusText = "Status: ${request.status} | Requested by: ${request.userEmail} on ${dateFormat.format(request.requestDate.toDate())}\nReason: ${request.reason}"
            tvStatus.text = statusText

            // Show/hide buttons based on status
            if (request.status == ItemRequest.RequestStatus.PENDING) {
                btnApprove.visibility = View.VISIBLE
                btnReject.visibility = View.VISIBLE
                btnApprove.setOnClickListener { onApprove(request) }
                btnReject.setOnClickListener { onReject(request) }
            } else {
                btnApprove.visibility = View.GONE
                btnReject.visibility = View.GONE
            }
        }
    }

    class ItemRequestDiffCallback : DiffUtil.ItemCallback<ItemRequest>() {
        override fun areItemsTheSame(oldItem: ItemRequest, newItem: ItemRequest): Boolean {
            return oldItem.requestId == newItem.requestId
        }

        override fun areContentsTheSame(oldItem: ItemRequest, newItem: ItemRequest): Boolean {
            return oldItem == newItem
        }
    }
}
