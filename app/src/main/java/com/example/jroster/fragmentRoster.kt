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

                    // Check if rosterData is not null and show the number of records
                    rosterData?.let {
                        val recordCount = it.size
                        // Show the number of records using a Toast
                        Toast.makeText(requireContext(), "Number of records: $recordCount", Toast.LENGTH_SHORT).show()

                        Log.d("LOL", "$rosterData")


                        // Log each piece of data
                        for (data in it) {
                            Log.d("RosterData", "Date: ${data.date}, Activity: ${data.activity}, SignOn: ${data.checkIn ?: "N/A"}, ATD: ${data.atd ?: "N/A"}, ATA: ${data.ata ?: "N/A"}, Orig: ${data.orig}, Dest: ${data.dest}, SignOff: ${data.checkOut ?: "N/A"}")
                        }

                        // Update RecyclerView if needed
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



    fun updateRecyclerView(rosterData: List<DbData>) {
        // Update your RecyclerView adapter with the data here
        //rosterAdapter.submitList(rosterData)
    }

}
