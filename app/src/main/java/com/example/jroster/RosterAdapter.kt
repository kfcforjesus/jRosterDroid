package com.example.jroster

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RosterAdapter(
    private val sortedDates: List<String>,
    private val entriesByDate: Map<String, List<DbData>>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_DATE_HEADER = 0
    private val VIEW_TYPE_ROSTER_ENTRY = 1

    override fun getItemViewType(position: Int): Int {
        return if (isPositionDateHeader(position)) VIEW_TYPE_DATE_HEADER else VIEW_TYPE_ROSTER_ENTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_DATE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.date_header_layout, parent, false)
            DateViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.duty_row_layout, parent, false)
            RosterEntryViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DateViewHolder) {
            holder.bind(getDateForPosition(position))
        } else if (holder is RosterEntryViewHolder) {
            val entry = getEntryForPosition(position)
            entry?.let {
                holder.bind(it, ::formatTime)
            }
        }
    }

    // Function to format date (2024-12-18 -> Tue 24 Sep 2024)
    fun formatDate(date: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEE dd MMM yyyy", Locale.getDefault())

            val parsedDate = inputFormat.parse(date)
            parsedDate?.let { outputFormat.format(it) } ?: date
        } catch (e: Exception) {
            date
        }
    }

    // Function to format the time (hh:mm)
    fun formatTime(dateTime: String?): String {
        if (dateTime == null || dateTime.isEmpty()) return "N/A"

        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        return try {
            val date = inputFormat.parse(dateTime)
            if (date != null) outputFormat.format(date) else "N/A"
        } catch (e: Exception) {
            "N/A"
        }
    }

    override fun getItemCount(): Int {
        return sortedDates.sumOf { 1 + (entriesByDate[it]?.size ?: 0) }
    }

    private fun isPositionDateHeader(position: Int): Boolean {
        var currentPos = 0
        for (date in sortedDates) {
            if (position == currentPos) return true
            currentPos += 1 + (entriesByDate[date]?.size ?: 0)
        }
        return false
    }

    private fun getDateForPosition(position: Int): String {
        var currentPos = 0
        for (date in sortedDates) {
            if (position == currentPos) return date
            currentPos += 1 + (entriesByDate[date]?.size ?: 0)
        }
        return ""
    }

    private fun getEntryForPosition(position: Int): DbData? {
        var currentPos = 0
        for (date in sortedDates) {
            val entries = entriesByDate[date]
            if (position > currentPos && position <= currentPos + (entries?.size ?: 0)) {
                return entries?.get(position - currentPos - 1)
            }
            currentPos += 1 + (entries?.size ?: 0)
        }
        return null
    }

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.rosterDate)

        // Modify the bind function to properly format and display the date
        fun bind(date: String) {
            // Date formatter for parsing the date string
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayedDateFormatter = SimpleDateFormat("EEE dd MMM yyyy", Locale.getDefault())

            // Get today's date at midnight
            val today = Calendar.getInstance()
            today.time = Date()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)
            val todayDate = today.time

            // Parse the current date from the string
            val parsedDate = dateFormatter.parse(date)

            // Check if the parsed date is valid
            parsedDate?.let {
                // Format the date to display
                var formattedDate = displayedDateFormatter.format(it)

                // Check if this date is today
                if (parsedDate == todayDate) {
                    formattedDate += " (Today)"
                }

                // Set the formatted date text to the TextView
                dateTextView.text = formattedDate
            } ?: run {
                // In case of an error with parsing, show an invalid date text
                dateTextView.text = "Invalid Date"
            }
        }
    }


    // ViewHolder for the Roster Entry (Duty)
    class RosterEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val flightIcon: ImageView = itemView.findViewById(R.id.flightIcon)
        private val flightRouteTextView: TextView = itemView.findViewById(R.id.flightRoute)
        private val flightDataTextView: TextView = itemView.findViewById(R.id.flightData)
        private val flightTimesTextView: TextView = itemView.findViewById(R.id.flightTimes)

        fun bind(entry: DbData, formatTime: (String?) -> String) {
            flightRouteTextView.text = "${entry.orig} - ${entry.dest}"
            flightDataTextView.text = entry.activity

            val atd = formatTime(entry.atd)
            val ata = formatTime(entry.ata)
            flightTimesTextView.text = "$atd - $ata"

            flightIcon.setImageResource(R.drawable.plane)
        }
    }
}
