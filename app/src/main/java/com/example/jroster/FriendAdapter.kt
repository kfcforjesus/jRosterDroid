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

class FriendAdapter(private var friendList: List<Friend>, private val onFriendSelectedCallback: () -> Unit) : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.friend_item, parent, false)
        return FriendViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friendList[holder.adapterPosition]
        holder.bind(friend, holder.adapterPosition == selectedPosition)

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
    }

    override fun getItemCount(): Int = friendList.size

    fun updateData(newFriendList: List<Friend>) {
        friendList = newFriendList
        notifyDataSetChanged()
    }

    fun getSelectedFriend(): Friend? {
        return if (selectedPosition != -1) {
            friendList[selectedPosition]
        } else {
            null
        }
    }

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val friendNameTextView: TextView = itemView.findViewById(R.id.friendName)
        private val deleteIcon: ImageView = itemView.findViewById(R.id.deleteIcon)

        fun bind(friend: Friend, isSelected: Boolean) {
            friendNameTextView.text = friend.name

            // Highlight selection
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
    }
}

