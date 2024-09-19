package com.example.jroster

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// The data type for the roster list, which can be either a String (date header) or a Duty (for duties).
class RosterAdapter(private val rosterList: List<Any>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // View type constants
    private val VIEW_TYPE_DATE_HEADER = 0
    private val VIEW_TYPE_DUTY_ROW = 1

    // Determine if the current item is a date header or a duty row
    override fun getItemViewType(position: Int): Int {
        return if (rosterList[position] is String) {
            VIEW_TYPE_DATE_HEADER // It's a date header
        } else {
            VIEW_TYPE_DUTY_ROW // It's a duty row
        }
    }

    // Create the appropriate ViewHolder based on the view type
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_DATE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.date_header_layout, parent, false)
            DateHeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.duty_row_layout, parent, false)
            DutyRowViewHolder(view)
        }
    }

    // Bind data to the ViewHolder based on the item type
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DateHeaderViewHolder) {
            val date = rosterList[position] as String
            holder.bind(date)
        } else if (holder is DutyRowViewHolder) {
            val duty = rosterList[position] as Duty
            holder.bind(duty)
        }
    }

    override fun getItemCount(): Int {
        return rosterList.size
    }

    // ViewHolder for Date Header
    inner class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rosterDate: TextView = itemView.findViewById(R.id.rosterDate)

        fun bind(date: String) {
            rosterDate.text = date
        }
    }

    // ViewHolder for Duty Row
    inner class DutyRowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val flightRoute: TextView = itemView.findViewById(R.id.flightRoute)
        private val flightTimes: TextView = itemView.findViewById(R.id.flightTimes)
        private val flightIcon: ImageView = itemView.findViewById(R.id.flightIcon)

        fun bind(duty: Duty) {
            flightRoute.text = "${duty.orig} - ${duty.dest}"
            flightTimes.text = "${duty.atd} - ${duty.ata}"
            flightIcon.setImageResource(R.drawable.plane) // Set the icon for the duty
        }
    }
}

// Your duty model (create this class in a separate file)
data class Duty(
    val orig: String,
    val dest: String,
    val atd: String, // Actual Time of Departure
    val ata: String  // Actual Time of Arrival
)
