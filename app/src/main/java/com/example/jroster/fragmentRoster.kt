package com.example.jroster

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
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

        // Set the LayoutManager for the RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Fetch userID and passCode from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)

        // Retrieve userID and passCode as integers
        val userID = sharedPreferences.getString("userID", "123") // Default to 123 if not found
        val passCode = sharedPreferences.getString("passCode", "456") // Default to 456 if not found

        // Test
        Toast.makeText(requireContext(), "UserID: $userID, PassCode: $passCode", Toast.LENGTH_SHORT).show()

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

                        // Update RecyclerView with the data
                        updateRecyclerView(it)
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

    // Scroll to yesterday's date
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






}
