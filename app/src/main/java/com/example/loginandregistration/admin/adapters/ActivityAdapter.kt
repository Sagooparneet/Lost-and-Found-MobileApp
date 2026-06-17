package com.example.loginandregistration.admin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.loginandregistration.R
import com.example.loginandregistration.admin.models.ActivityItem
import com.example.loginandregistration.admin.models.ActivityType
import java.text.SimpleDateFormat
import java.util.*

class ActivityAdapter : ListAdapter<ActivityItem, ActivityAdapter.ActivityViewHolder>(ActivityDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity, parent, false)
        return ActivityViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivActivityIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvActivityTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvActivityDescription)
        private val tvTime: TextView = itemView.findViewById(R.id.tvActivityTime)
        private val tvNewBadge: TextView = itemView.findViewById(R.id.tvNewBadge)
        
        fun bind(activity: ActivityItem) {
            // Set icon based on activity type
            val iconRes = when (activity.action) {
                ActivityType.ITEM_REPORTED -> R.drawable.ic_report
                ActivityType.ITEM_FOUND -> R.drawable.ic_check
                ActivityType.ITEM_CLAIMED -> R.drawable.ic_person
                ActivityType.ITEM_VERIFIED -> R.drawable.ic_verified
                ActivityType.USER_REGISTERED -> R.drawable.ic_person_add
                ActivityType.USER_BLOCKED -> R.drawable.ic_block
                ActivityType.USER_UNBLOCKED -> R.drawable.ic_check
                ActivityType.STATUS_CHANGED -> R.drawable.ic_edit
            }
            ivIcon.setImageResource(iconRes)
            
            // Set title based on activity type
            val title = when (activity.action) {
                ActivityType.ITEM_REPORTED -> "${activity.itemName} reported lost"
                ActivityType.ITEM_FOUND -> "${activity.itemName} found and verified"
                ActivityType.ITEM_CLAIMED -> "${activity.itemName} claimed by owner"
                ActivityType.ITEM_VERIFIED -> "${activity.itemName} verified by security"
                ActivityType.USER_REGISTERED -> "New user registered"
                ActivityType.USER_BLOCKED -> "User blocked"
                ActivityType.USER_UNBLOCKED -> "User unblocked"
                ActivityType.STATUS_CHANGED -> "${activity.itemName} status updated"
            }
            tvTitle.text = title
            
            // Set description
            tvDescription.text = if (activity.description.isNotEmpty()) {
                activity.description
            } else {
                "by ${activity.userName}"
            }
            
            // Set time
            tvTime.text = getRelativeTime(activity.timestamp)
            
            // Show/hide new badge
            tvNewBadge.visibility = if (activity.isNew) View.VISIBLE else View.GONE
        }
        
        private fun getRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60 * 1000 -> "Just now"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
                diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
    
    class ActivityDiffCallback : DiffUtil.ItemCallback<ActivityItem>() {
        override fun areItemsTheSame(oldItem: ActivityItem, newItem: ActivityItem): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ActivityItem, newItem: ActivityItem): Boolean {
            return oldItem == newItem
        }
    }
}