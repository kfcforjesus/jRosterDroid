package com.example.jroster

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

class FriendAdapter(
    private var friendList: List<Any>,
    private val onFriendSelectedCallback: () -> Unit,
    private val onDeleteFriendCallback: (Friend) -> Unit
) : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    private var selectedPosition = -1
    private var isDisplayingDaysOff = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.friend_item, parent, false)
        return FriendViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val item = friendList[holder.adapterPosition] // Friend or MDO
        holder.bind(item, holder.adapterPosition == selectedPosition, isDisplayingDaysOff)

        // Set click listener for the item
        holder.itemView.setOnClickListener {
            val previousSelectedPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            // Notify the adapter of the previous and current selection changes
            if (previousSelectedPosition != -1) {
                notifyItemChanged(previousSelectedPosition)
            }
            notifyItemChanged(selectedPosition)

            // Callback to notify FragmentFriends of selection
            onFriendSelectedCallback()
        }

        // Set the visibility of the delete icon based on selection status
        if (!isDisplayingDaysOff && item is Friend) {
            if (holder.adapterPosition == selectedPosition) {
                holder.deleteIcon.isVisible = true
            } else {
                holder.deleteIcon.isVisible = false
            }

            // Set click listener
            holder.deleteIcon.setOnClickListener {
                onDeleteFriendCallback(item)
            }
        } else {
            holder.deleteIcon.isVisible = false
        }
    }

    override fun getItemCount(): Int = friendList.size

    // Update function to handle switching between friends and mutual days off
    fun updateData(newList: List<Any>, isDaysOff: Boolean) {
        friendList = newList
        isDisplayingDaysOff = isDaysOff
        selectedPosition = -1 // Reset selection when switching between modes
        notifyDataSetChanged()
    }

    // Get the selected Friend, or null if not a Friend
    fun getSelectedFriend(): Friend? {
        return if (selectedPosition != -1 && friendList[selectedPosition] is Friend) {
            friendList[selectedPosition] as Friend
        } else {
            null
        }
    }

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val friendNameTextView: TextView = itemView.findViewById(R.id.friendName)
        val deleteIcon: ImageView = itemView.findViewById(R.id.deleteIcon) // Make it accessible

        // Updated bind function to handle either Friend object or String for mutual days off
        fun bind(item: Any, isSelected: Boolean, isDisplayingDaysOff: Boolean) {
            when (item) {
                is Friend -> {
                    friendNameTextView.text = item.name

                    // Highlight selection for friend
                    if (isSelected) {
                        itemView.setBackgroundColor(Color.parseColor("#6A5ACD"))
                        friendNameTextView.setTextColor(Color.parseColor("#FFFFFF"))
                        deleteIcon.isVisible = true
                    } else {
                        itemView.setBackgroundColor(Color.parseColor("#FFFFFF"))
                        friendNameTextView.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))
                        deleteIcon.isVisible = false
                    }
                }
                is String -> {
                    // Handle mutual day off as a simple string
                    friendNameTextView.text = item

                    // No selection highlighting for mutual days off
                    itemView.setBackgroundColor(Color.parseColor("#FFFFFF"))
                    friendNameTextView.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))

                    // Hide delete icon for mutual days off
                    deleteIcon.isVisible = false
                }
            }
        }
    }

}
