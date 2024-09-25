package com.example.jroster

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zires.switchsegmentedcontrol.ZiresSwitchSegmentedControl
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class FragmentRoster : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var rosterAdapter: RosterAdapter

    var shouldScrollToClosestDate = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_roster, container, false)

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.rosterRecyclerView)

        // Divider
        val dividerDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.divider)
        val dividerItemDecoration = DividerItemDecoration(recyclerView.context, LinearLayoutManager.VERTICAL)
        dividerDrawable?.let { dividerItemDecoration.setDrawable(it) }
        recyclerView.addItemDecoration(dividerItemDecoration)

        // Set the LayoutManager for the RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Fetch userID and passCode from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val userID = sharedPreferences.getString("userID", "123")
        val passCode = sharedPreferences.getString("passCode", "456")

        // Users home base
        val savedBase = sharedPreferences.getString("base", "Melbourne")

        // Retrieve saved switch state
        val switchState = sharedPreferences.getBoolean("switch_state", false)

        // Find and set the initial switch state
        val switchSegmentedControl: ZiresSwitchSegmentedControl = view.findViewById(R.id.zires_switch)
        switchSegmentedControl.setChecked(switchState)

        // Display users base
        switchSegmentedControl.setRightToggleText(savedBase!!)

        // Capture scroll position and offset
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager

        // Set listener to handle switch toggle
        switchSegmentedControl.setOnToggleSwitchChangeListener(object : ZiresSwitchSegmentedControl.OnSwitchChangeListener {
            override fun onToggleSwitchChangeListener(isChecked: Boolean) {
                val correctedState = !isChecked

                // Save the corrected state to SharedPreferences
                sharedPreferences.edit().putBoolean("switch_state", correctedState).apply()

                // Capture the current scroll position and offset
                val currentPosition = layoutManager.findFirstVisibleItemPosition()
                val view = layoutManager.findViewByPosition(currentPosition)
                val offset = view?.top ?: 0

                // Update the adapter's time zone state
                rosterAdapter.useHomeTime = correctedState

                // Notify only the visible items of the change without resetting the entire view
                val visibleItemCount = layoutManager.childCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                // Notify only the range of visible items
                rosterAdapter.notifyItemRangeChanged(firstVisibleItemPosition, visibleItemCount)

                // Restore the scroll position and offset
                recyclerView.post {
                    layoutManager.scrollToPositionWithOffset(currentPosition, offset)
                }
            }
        })

        // Fetch the roster data initially
        fetchRosterData(userID.toString(), passCode.toString(), switchState)

        return view
    }

    fun fetchRosterData(userID: String, passCode: String, useHomeTime: Boolean) {
        // Prepare the API call
        val call = RetrofitClient.rosterApiService.getRosterData(userID, passCode)

        // Enqueue the API call
        call.enqueue(object : Callback<List<DbData>> {
            override fun onResponse(call: Call<List<DbData>>, response: Response<List<DbData>>) {
                if (response.isSuccessful) {
                    val rosterData = response.body()

                    // Check if rosterData is not null and show the number of records
                    rosterData?.let {
                        val updatedRosterEntries = processEntriesForSignOn(it)

                        // Pass the switch state (useSydneyTimeZone) to updateRecyclerView
                        updateRecyclerView(updatedRosterEntries, useHomeTime)
                    } ?: run {
                        // Show a Toast if there are no records
                        Toast.makeText(requireContext(), "No records found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Handle the error case
                    Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<DbData>>, t: Throwable) {
                // Show a Toast if there is a failure
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun updateRecyclerView(rosterData: List<DbData>, useHomeTime: Boolean) {
        // Organize roster data by date
        val entriesByDate = rosterData.groupBy { it.date }
        val sortedDates = entriesByDate.keys.sorted()

        // Create an instance of extAirports
        val extAirportsInstance = extAirports()

        // Retrieve the saved base value from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val savedBase = sharedPreferences.getString("base", "Melbourne") ?: "Melbourne"

        // Set up the adapter with the grouped data, extAirports instance, switch state, and the user's base
        rosterAdapter = RosterAdapter(sortedDates, entriesByDate, extAirportsInstance, useHomeTime, savedBase)

        // Set the adapter for the RecyclerView
        recyclerView.adapter = rosterAdapter

        // Conditionally scroll to the closest date
        if (shouldScrollToClosestDate) {
            scrollToClosestDate(sortedDates, entriesByDate, recyclerView)
        }
    }


    private fun scrollToClosestDate(sortedDates: List<String>, entriesByDate: Map<String, List<DbData>>, recyclerView: RecyclerView) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = Date()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -1) // Subtract one day
        val yesterday = calendar.time

        // Standard formatter
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateFormatter.timeZone = TimeZone.getTimeZone("UTC")

        var closestDateIndex: Int? = null
        var smallestTimeInterval: Long = Long.MAX_VALUE
        var currentIndex = 0 // Keeps track of the overall index (date header + duties)

        for (i in sortedDates.indices) {
            val dateString = sortedDates[i]
            val parsedDate = dateFormatter.parse(dateString)

            parsedDate?.let {
                val timeInterval = it.time - yesterday.time

                if (Math.abs(timeInterval) < smallestTimeInterval) {
                    smallestTimeInterval = Math.abs(timeInterval)
                    closestDateIndex = currentIndex
                }
            }

            currentIndex++  // Add 1 for the date header
            currentIndex += entriesByDate[sortedDates[i]]?.size ?: 0 // Add the number of duties for this date
        }

        // Scroll to the closest date index if found
        closestDateIndex?.let {
            recyclerView.scrollToPosition(it)
        }
    }

    // Create the signon duties out of nothing.  Boss.
    private fun processEntriesForSignOn(rosterEntries: List<DbData>): List<DbData> {
        val updatedEntries = mutableListOf<DbData>()
        val seenCheckInTimesByDate = mutableMapOf<String, MutableSet<Date>>()

        // Define the cutoff date (January 1, 1980)
        val cutoffDate = Calendar.getInstance().apply {
            set(1980, Calendar.JANUARY, 1, 0, 0, 0)
        }.time

        for (entry in rosterEntries) {
            // Ensure there's a set for each date to track seen check-in times
            if (seenCheckInTimesByDate[entry.date] == null) {
                seenCheckInTimesByDate[entry.date] = mutableSetOf()
            }

            // Check for WDO-related activities and skip adding a "Sign on" for them
            if (listOf("WDO", "WDA", "WDT", "WDE").contains(entry.activity)) {
                continue // No sign on required for this
            }

            // Check if check-in is after the cutoff date (January 1, 1980)
            val checkInDate = entry.checkIn?.let { parseDate(it) }
            if (checkInDate != null && checkInDate.after(cutoffDate)) {
                // Check if the check-in time is not already seen for this date and not the same as atd
                val atdDate = entry.atd?.let { parseDate(it) }
                if (!seenCheckInTimesByDate[entry.date]!!.contains(checkInDate) && checkInDate != atdDate) {
                    seenCheckInTimesByDate[entry.date]!!.add(checkInDate)

                    // Create a new "Sign on" entry
                    val signOnEntry = DbData(
                        activity = "Sign on",
                        ata = null.toString(),
                        atd = formatDate(checkInDate),
                        checkIn = null.toString(),
                        checkOut = null.toString(),
                        date = entry.date,
                        dest = entry.dest,
                        orig = entry.orig,
                        ac = null.toString(),
                        dd = null.toString()
                    )
                    updatedEntries.add(signOnEntry)
                }
            }

            // Add the original entry
            updatedEntries.add(entry)
        }

        return updatedEntries
    }

    // Helper function to parse date string into Date object in UTC
    private fun parseDate(dateString: String): Date? {
        return try {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            dateFormatter.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    // Helper function to format Date object into string in UTC (yyyy-MM-dd HH:mm:ss)
    private fun formatDate(date: Date?): String {
        return date?.let {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            dateFormatter.format(date)
        } ?: ""
    }
}
