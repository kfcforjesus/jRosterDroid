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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class FragmentRoster : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var rosterAdapter: RosterAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_roster, container, false)

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.rosterRecyclerView)

        // Create a divider so i can see where shit is
        val dividerDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.divider)
        val dividerItemDecordation = DividerItemDecoration(recyclerView.context, LinearLayoutManager.VERTICAL)

        dividerDrawable?.let {
            dividerItemDecordation.setDrawable(it)
        }

        recyclerView.addItemDecoration(dividerItemDecordation)

        // Set the LayoutManager for the RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Fetch userID and passCode from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)

        // Retrieve userID and passCode as integers
        val userID = sharedPreferences.getString("userID", "123") // Default to 123 if not found
        val passCode = sharedPreferences.getString("passCode", "456") // Default to 456 if not found

        val recyclerView = view.findViewById<RecyclerView>(R.id.rosterRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Add a divider between items
        val dividerItemDecoration = DividerItemDecoration(recyclerView.context, LinearLayoutManager.VERTICAL)
        recyclerView.addItemDecoration(dividerItemDecoration)

        // Fetch the roster data
        fetchRosterData(userID.toString(), passCode.toString())

        return view
    }

    fun fetchRosterData(userID: String, passCode: String) {
        // Prepare the API call
        val call = RetrofitClient.rosterApiService.getRosterData(userID, passCode)

        // Enqueue the API call
        call.enqueue(object : Callback<List<DbData>> {
            override fun onResponse(call: Call<List<DbData>>, response: Response<List<DbData>>) {
                if (response.isSuccessful) {
                    val rosterData = response.body()

                    // Check if rosterData is not null and show the number of records
                    rosterData?.let {
                        val recordCount = it.size

                        // Log each piece of data
                        for (data in it) {
                            Log.d("RosterData", "Date: ${data.date}, Activity: ${data.activity}, SignOn: ${data.checkIn ?: "N/A"}, ATD: ${data.atd ?: "N/A"}, ATA: ${data.ata ?: "N/A"}, Orig: ${data.orig}, Dest: ${data.dest}, SignOff: ${data.checkOut ?: "N/A"}")
                        }

                        val updatedRosterEntries = processEntriesForSignOn(it)
                        updateRecyclerView(updatedRosterEntries)

                        // Update RecyclerView with the data
                        //updateRecyclerView(it)
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

    // Populate the roster table
    fun updateRecyclerView(rosterData: List<DbData>) {
        // Organize roster data by date
        val entriesByDate = rosterData.groupBy { it.date }
        val sortedDates = entriesByDate.keys.sorted()

        // Set up the adapter with the grouped data
        rosterAdapter = RosterAdapter(sortedDates, entriesByDate)

        // Set the adapter for the RecyclerView
        recyclerView.adapter = rosterAdapter

        // Scroll to the date closest to today
        scrollToClosestDate(sortedDates, entriesByDate, recyclerView)
    }

    // Scroll to yesterday's date. Which is actually today, but Kotlin is retarded
    private fun scrollToClosestDate(sortedDates: List<String>, entriesByDate: Map<String, List<DbData>>, recyclerView: RecyclerView) {
        // Get today's date and subtract one day to get yesterday's date
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -1) // Subtract one day
        val yesterday = calendar.time

        // Date format that matches your date strings
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var closestDateIndex: Int? = null
        var smallestTimeInterval: Long = Long.MAX_VALUE
        var currentIndex = 0 // Keeps track of the overall index (date header + duties)

        // Loop through sortedDates and calculate the index
        for (i in sortedDates.indices) {
            val dateString = sortedDates[i]
            val parsedDate = dateFormatter.parse(dateString)

            parsedDate?.let {
                val timeInterval = it.time - yesterday.time

                // Check if this date is closer to yesterday
                if (Math.abs(timeInterval) < smallestTimeInterval) {
                    smallestTimeInterval = Math.abs(timeInterval)
                    closestDateIndex = currentIndex
                }
            }

            // Add 1 for the date header
            currentIndex++

            // Add the number of duties for this date to currentIndex
            currentIndex += entriesByDate[sortedDates[i]]?.size ?: 0
        }

        // Scroll to the closest date index if found
        closestDateIndex?.let {
            recyclerView.scrollToPosition(it)
        }
    }

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
                continue // Skip adding this entry
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

    // Helper function to parse date string into Date object
    private fun parseDate(dateString: String): Date? {
        return try {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            dateFormatter.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    // Helper function to format Date object into string (yyyy-MM-dd HH:mm:ss)
    private fun formatDate(date: Date?): String {
        return date?.let {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            dateFormatter.format(date)
        } ?: ""
    }







}
