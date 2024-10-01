package com.example.jroster

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jroster.GlobalVariables.globalFriendCode
import com.example.jroster.GlobalVariables.globalFriendName
import com.example.jroster.GlobalVariables.globalFriendUserID
import com.example.jroster.GlobalVariables.isFriendMode
import com.zires.switchsegmentedcontrol.ZiresSwitchSegmentedControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class FragmentRoster : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var rosterAdapter: RosterAdapter
    private lateinit var rosterTitle: TextView
    private lateinit var segmentSwitch: ZiresSwitchSegmentedControl
    private lateinit var exitButton: Button
    private val wdoDates: MutableSet<String> = mutableSetOf()

    var shouldScrollToClosestDate = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_roster, container, false)

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.rosterRecyclerView)
        rosterTitle = view.findViewById(R.id.rosterTitle)
        segmentSwitch = view.findViewById(R.id.zires_switch)
        exitButton = view.findViewById(R.id.exitButton)

        // Exit Friend Button
        exitButton.setOnClickListener {

            // End friend mode
            disableFriendMode()

            // Load users data
            populateRoster()
        }

        // Divider
        val dividerDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.divider)
        val dividerItemDecoration = DividerItemDecoration(recyclerView.context, LinearLayoutManager.VERTICAL)
        dividerDrawable?.let { dividerItemDecoration.setDrawable(it) }
        recyclerView.addItemDecoration(dividerItemDecoration)

        // Set the LayoutManager for the RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Detect if we are in friend mode using the GlobalVariables object
        if (isFriendMode()) {

            // Fetch and display the friend's roster
            val friendUserID = globalFriendUserID
            val friendCode = globalFriendCode
            val friendName = globalFriendName

            // Set the title
            rosterTitle.setText(friendName)
            rosterTitle.setTextColor(Color.RED)

            // Remove the time toggle
            segmentSwitch.isVisible = false
            exitButton.isVisible = true


            if (friendUserID != null && friendCode != null) {
                fetchFriendRoster(friendUserID, friendCode)
            } else {
                Toast.makeText(requireContext(), "No friend data found", Toast.LENGTH_SHORT).show()
            }
        } else {

           // Load users data
           populateRoster()
        }

        return view
    }

    // ---------------------------------------- Functions to handle the user -------------------------------------------------------------------- //

    // Populate the recycleview with users shit
    private fun populateRoster() {

        // Set the title
        rosterTitle.setText("My Roster")
        rosterTitle.setTextColor(Color.parseColor("#6A5ACD"))

        // Reinstate the time toggle
        segmentSwitch.isVisible = true
        exitButton.isGone = true

        // Fetch and display the current user's roster
        val sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val userID = sharedPreferences.getString("userID", "123")
        val passCode = sharedPreferences.getString("passCode", "456")
        val switchState = sharedPreferences.getBoolean("switch_state", false)

        // Fetch and display data from the database first
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(requireContext())
            val rosterEntriesFromDb = db.dbDataDao().getAll()

            // Process entries for Sign On
            val processedEntries = processEntriesForSignOn(rosterEntriesFromDb)

            // Update the UI on the main thread
            CoroutineScope(Dispatchers.Main).launch {
                updateRecyclerView(processedEntries, switchState)
            }

            // Then fetch new data from the network
            fetchRosterData(userID.toString(), passCode.toString(), switchState)
        }
    }

    // Normal user roster fetching
    private fun fetchRosterData(userID: String, passCode: String, useHomeTime: Boolean) {
        val call = RetrofitClient.rosterApiService.getRosterData(userID, passCode)

        call.enqueue(object : Callback<List<DbData>> {
            override fun onResponse(call: Call<List<DbData>>, response: Response<List<DbData>>) {
                if (response.isSuccessful) {
                    val rosterData = response.body()

                    rosterData?.let { data ->
                        if (view != null && isAdded) {
                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                val processedEntries = processEntriesForSignOn(data)
                                withContext(Dispatchers.Main) {
                                    if (isAdded && view != null) {
                                        updateRecyclerView(processedEntries, useHomeTime)
                                    }
                                }
                            }
                        }
                    } ?: run {
                        if (isAdded && view != null) {
                            Toast.makeText(requireContext(), "No records found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    if (isAdded && view != null) {
                        Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<List<DbData>>, t: Throwable) {
                if (isAdded && view != null) {
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    // ---------------------------------------- Handle Friends  -------------------------------------------------------------------- //

    // Fetch the friend's roster data ROOMS DB
    private fun fetchFriendRoster(userID: String, friendCode: String) {
        val call = RetrofitClient.rosterApiService.getFriendRosterData(userID, friendCode)

        // Queue the API call
        call.enqueue(object : Callback<List<DbData>> {
            override fun onResponse(call: Call<List<DbData>>, response: Response<List<DbData>>) {
                if (response.isSuccessful) {
                    val friendRosterData = response.body()

                    // Check if friendRosterData is not null
                    friendRosterData?.let { data ->
                        if (view != null && isAdded) {
                            // Update the RecyclerView
                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                val processedEntries = processEntriesForSignOn(data)
                                withContext(Dispatchers.Main) {
                                    if (isAdded && view != null) {
                                        updateRecyclerView(processedEntries, useHomeTime = false)
                                    }
                                }
                            }
                        }
                    } ?: run {
                        if (isAdded && view != null) {
                            Toast.makeText(requireContext(), "No records found for this friend", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    if (isAdded && view != null) {
                        Toast.makeText(requireContext(), "Failed to fetch friend's roster data", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<List<DbData>>, t: Throwable) {
                if (isAdded && view != null) {
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }


    // ---------------------------------------- Core Functions  -------------------------------------------------------------------- //

    // Process Sign On duties
    fun processEntriesForSignOn(rosterEntries: List<DbData>): List<DbData> {
        val updatedEntries = mutableListOf<DbData>()
        val seenCheckInTimesByDate = mutableMapOf<String, MutableSet<Date>>()

        val cutoffDate = Calendar.getInstance().apply {
            set(1980, Calendar.JANUARY, 1, 0, 0, 0)
        }.time

        for (entry in rosterEntries) {
            if (seenCheckInTimesByDate[entry.date] == null) {
                seenCheckInTimesByDate[entry.date] = mutableSetOf()
            }

            if (listOf("WDO", "WDA", "WDT", "WDE").contains(entry.activity)) {
                wdoDates.add(entry.date)
                continue
            }

            val checkInDate = entry.checkIn?.let { parseDate(it) }
            if (checkInDate != null && checkInDate.after(cutoffDate)) {
                val atdDate = entry.atd?.let { parseDate(it) }
                if (!seenCheckInTimesByDate[entry.date]!!.contains(checkInDate) && checkInDate != atdDate) {
                    seenCheckInTimesByDate[entry.date]!!.add(checkInDate)
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
            updatedEntries.add(entry)
        }
        return updatedEntries
    }

    // ---------------------------------------- RecyclerView Function  -------------------------------------------------------------------- //

    fun updateRecyclerView(rosterData: List<DbData>, useHomeTime: Boolean) {
        val entriesByDate = rosterData.groupBy { it.date }
        val sortedDates = entriesByDate.keys.sorted()

        val extAirportsInstance = extAirports()

        val sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val savedBase = sharedPreferences.getString("base", "Melbourne") ?: "Melbourne"

        rosterAdapter = RosterAdapter(sortedDates, entriesByDate, extAirportsInstance, useHomeTime, savedBase, wdoDates)
        recyclerView.adapter = rosterAdapter

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
        calendar.add(Calendar.DAY_OF_YEAR, 0)
        val yesterday = calendar.time

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateFormatter.timeZone = TimeZone.getTimeZone("UTC")

        var closestDateIndex: Int? = null
        var smallestTimeInterval: Long = Long.MAX_VALUE
        var currentIndex = 0

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

            currentIndex++
            currentIndex += entriesByDate[sortedDates[i]]?.size ?: 0
        }

        closestDateIndex?.let {
            recyclerView.scrollToPosition(it)
        }
    }

    // ---------------------------------------- Boiler plate  -------------------------------------------------------------------- //

    // Check if we are in friend mode
    private fun isFriendMode(): Boolean {
        return isFriendMode && globalFriendUserID != null && globalFriendCode != null
    }

    // Disable Friend Mode
    private fun disableFriendMode() {
        isFriendMode = false
        globalFriendUserID = null
        globalFriendCode = null
    }

    fun resetFriendMode() {
        GlobalVariables.globalFriendUserID = null
        GlobalVariables.globalFriendCode = null
        GlobalVariables.globalFriendName = null
        GlobalVariables.isFriendMode = false
    }

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

    private fun formatDate(date: Date?): String {
        return date?.let {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            dateFormatter.format(date)
        } ?: ""
    }
}
