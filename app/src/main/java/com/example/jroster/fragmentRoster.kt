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
        val sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val switchState = sharedPreferences.getBoolean("switch_state", false)
        val userBase = sharedPreferences.getString("base", "Melbourne")

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.rosterRecyclerView)
        rosterTitle = view.findViewById(R.id.rosterTitle)
        segmentSwitch = view.findViewById(R.id.zires_switch)
        exitButton = view.findViewById(R.id.exitButton)

        // Set state
        segmentSwitch.setRightToggleText(userBase.toString())
        segmentSwitch.setChecked(switchState)

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

        // Set the listener to toggle between local time and flight time
        segmentSwitch.setOnToggleSwitchChangeListener(object : ZiresSwitchSegmentedControl.OnSwitchChangeListener {
            override fun onToggleSwitchChangeListener(isChecked: Boolean) {
                val correctedState = !isChecked
                val editor = sharedPreferences.edit()
                editor.putBoolean("switch_state", correctedState)
                editor.apply()

                // Get userID and passCode from sharedPreferences
                val userID = sharedPreferences.getString("userID", "123")
                val passCode = sharedPreferences.getString("passCode", "456")

                // Fetch the roster data and update the UI based on the switch state
                fetchRosterData(userID.toString(), passCode.toString(), correctedState)
            }
        })

        // Detect if we are in friend mode using the GlobalVariables object
        if (isFriendMode()) {
            // Fetch and display the friend's roster
            val friendUserID = globalFriendUserID
            val friendCode = globalFriendCode
            val friendName = globalFriendName

            rosterTitle.text = friendName
            rosterTitle.setTextColor(Color.RED)
            segmentSwitch.isVisible = false
            exitButton.isVisible = true

            if (friendUserID != null && friendCode != null) {
                fetchAndUpdateFriendRoster(friendUserID, friendCode)
            } else {
                Toast.makeText(requireContext(), "No friend data found", Toast.LENGTH_SHORT).show()
            }
        } else {
            populateRoster()  // Load user's roster
        }

        return view
    }

    // ---------------------------------------- Functions to handle the user -------------------------------------------------------------------- //

    private fun populateRoster() {
        rosterTitle.text = "My Roster"
        rosterTitle.setTextColor(Color.parseColor("#6A5ACD"))

        // Reinstate the time toggle
        segmentSwitch.isVisible = true
        exitButton.isGone = true

        val sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val userID = sharedPreferences.getString("userID", "123")
        val passCode = sharedPreferences.getString("passCode", "456")
        val switchState = sharedPreferences.getBoolean("switch_state", false)

        // Load data from local database first
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(requireContext())
            val rosterEntriesFromDb = db.dbDataDao().getAll()  // Access database on the IO thread

            // Process entries for Sign On
            val processedEntries = processEntriesForSignOn(rosterEntriesFromDb)

            withContext(Dispatchers.Main) {
                // Update the UI on the main thread
                updateRecyclerView(processedEntries, switchState)
            }

            // Fetch new data from the network after displaying local data
            fetchRosterData(userID.toString(), passCode.toString(), useHomeTime = true)
        }
    }

    private fun fetchRosterData(userID: String, passCode: String, useHomeTime: Boolean) {
        val call = RetrofitClient.rosterApiService.getRosterData(userID, passCode)

        call.enqueue(object : Callback<List<DbData>> {
            override fun onResponse(call: Call<List<DbData>>, response: Response<List<DbData>>) {
                if (response.isSuccessful) {
                    val rosterData = response.body()

                    rosterData?.let { data ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val db = AppDatabase.getInstance(requireContext())

                            // Only clear the database and insert if we get non-empty data
                            if (data.isNotEmpty()) {
                                db.dbDataDao().deleteAll()  // Clear old data
                                db.dbDataDao().insertAll(data)  // Insert fetched data into the database
                            }

                            val processedEntries = processEntriesForSignOn(data)
                            withContext(Dispatchers.Main) {
                                updateRecyclerView(processedEntries, useHomeTime)
                            }
                        }
                    } ?: run {
                        lifecycleScope.launch(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "No records found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<List<DbData>>, t: Throwable) {
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Offline Mode: Unable to fetch data", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    // Function to get roster data from local database asynchronously (for switch toggle)
    private suspend fun getRosterDataFromDb(): List<DbData> = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(requireContext())
        return@withContext db.dbDataDao().getAll()  // Database access on background thread
    }


    // ---------------------------------------- Handle Friends  -------------------------------------------------------------------- //

    private fun fetchAndUpdateFriendRoster(friendUserID: String, friendCode: String) {
        val call = RetrofitClient.rosterApiService.getFriendRosterData(friendUserID, friendCode)

        // Queue the API call
        call.enqueue(object : Callback<List<FriendsFlights>> {
            override fun onResponse(call: Call<List<FriendsFlights>>, response: Response<List<FriendsFlights>>) {
                if (response.isSuccessful) {
                    val fetchedEntries = response.body()

                    // Only update if data exists
                    fetchedEntries?.let { fetchedList ->
                        if (fetchedList.isNotEmpty()) {
                            compareAndUpdateFriendRosterData(fetchedList, friendCode)

                            // Convert FriendsFlights to DbData and update the RecyclerView
                            val dbDataList = convertFriendsFlightsToDbData(fetchedList)

                            // Add sign on duties and display
                            val processedEntries = processEntriesForSignOn(dbDataList)
                            updateRecyclerView(processedEntries, useHomeTime = false)
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch friend's roster data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<FriendsFlights>>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Function to compare and update friend's flights from the fetched data
    private fun compareAndUpdateFriendRosterData(fetchedEntries: List<FriendsFlights>, friendCode: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(requireContext())
            val existingEntries = db.friendsFlightsDao().getFlightsByFriendCode(friendCode)

            var shouldUpdate = false

            for (entry in existingEntries) {
                // Skip "Sign on" duties from being saved (since they're artificially generated)
                if (entry.activity == "Sign on") continue

                val existingFlight = existingEntries.firstOrNull { it.atd == entry.atd && it.activity == entry.activity }

                if (existingFlight != null) {
                    // Compare the fetched entry with the existing one
                    if (existingFlight.ata != entry.ata ||
                        existingFlight.checkIn != entry.checkIn ||
                        existingFlight.checkOut != entry.checkOut ||
                        existingFlight.date != entry.date ||
                        existingFlight.dd != entry.dd ||
                        existingFlight.dest != entry.dest ||
                        existingFlight.orig != entry.orig
                    ) {
                        // Update existing entry (ensure the friendCode is retained)
                        val updatedFlight = existingFlight.copy(
                            ata = entry.ata,
                            checkIn = entry.checkIn,
                            checkOut = entry.checkOut,
                            date = entry.date,
                            dd = entry.dd,
                            dest = entry.dest,
                            orig = entry.orig,
                            friendCode = friendCode // Ensure friendCode is set properly
                        )

                        db.friendsFlightsDao().insertAll(listOf(updatedFlight))
                        shouldUpdate = true
                    }
                } else {
                    // Insert new flight if it doesn't exist in the database
                    val newFlight = entry.copy(friendCode = friendCode)
                    db.friendsFlightsDao().insertAll(listOf(newFlight))
                    shouldUpdate = true
                }
            }

            // If updates were made, refresh the UI
            if (shouldUpdate) {
                withContext(Dispatchers.Main) {
                    // Convert FriendsFlights to DbData before displaying

                    val dbDataEntries = convertFriendsFlightsToDbData(existingEntries)

                    updateRecyclerView(dbDataEntries, useHomeTime = false)
                    Toast.makeText(requireContext(), "Friend roster updated", Toast.LENGTH_SHORT).show()
                }
            }
        }
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

    // Convert FriendsFlights to DbData.  What a fuckin pain.
    private fun convertFriendsFlightsToDbData(friendsFlights: List<FriendsFlights>): List<DbData> {
        return friendsFlights.map { friendFlight ->
            DbData(
                id = 0,  // Use 0 or any default as the ID will be autogenerated
                dd = friendFlight.dd ?: "",
                date = friendFlight.date,
                activity = friendFlight.activity,
                checkIn = friendFlight.checkIn ?: "",
                atd = friendFlight.atd,
                dest = friendFlight.dest,
                orig = friendFlight.orig,
                ata = friendFlight.ata ?: "",
                checkOut = friendFlight.checkOut ?: "",
                ac = ""
            )
        }
    }

}
