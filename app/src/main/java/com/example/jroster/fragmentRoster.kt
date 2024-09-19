package com.example.jroster

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FragmentRoster : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_roster, container, false)

        // Fetch userID and passCode from SharedPreferences as Integers
        val sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)

        // Retrieve userID and passCode as integers
        val userID = sharedPreferences.getString("userID", "123") // Default to 123 if not found
        val passCode = sharedPreferences.getString("passCode", "456") // Default to 456 if not found

        // Test
        Toast.makeText(requireContext(), "UserID: $userID, PassCode: $passCode", Toast.LENGTH_SHORT).show()

        // Convert the integers to strings for API call
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



                    // Check if rosterData is not null
                    rosterData?.let {
                        val recordCount = it.size
                        // Log the number of records
                        Log.d("RosterData", "Number of records: $recordCount")

                        // Loop through each entry and log the data
                        for (data in it) {
                            Log.d("RosterData", "Date: ${data.date}, Activity: ${data.activity}, SignOn: ${data.CI}, ATD: ${data.ATD}, ATA: ${data.ATA}, Orig: ${data.orig}, Dest: ${data.dest}, SignOff: ${data.CO}")
                        }

                        // You can also update the RecyclerView here with the retrieved data
                        updateRecyclerView(it)
                    } ?: run {
                        // Log if no records were found
                        Log.d("RosterData", "No records found")
                    }
                } else {
                    // Log the error
                    Log.d("RosterData", "Failed to fetch data")
                }
            }

            override fun onFailure(call: Call<List<DbData>>, t: Throwable) {
                // Log the failure
                Log.e("RosterData", "Error: ${t.message}")
            }
        })
    }


    fun updateRecyclerView(rosterData: List<DbData>) {
        // Update your RecyclerView adapter with the data here
        //rosterAdapter.submitList(rosterData)
    }

}
