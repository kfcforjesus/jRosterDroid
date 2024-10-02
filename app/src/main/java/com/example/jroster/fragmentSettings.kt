package com.example.jroster

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import okhttp3.*
import java.io.IOException
import org.json.JSONArray
import org.json.JSONException
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*


class FragmentSettings : Fragment() {

    // Vars
    private lateinit var nameEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var baseLabel: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var exportRadioGroup: RadioGroup
    private lateinit var exportOffButton: RadioButton
    private lateinit var exportOnButton: RadioButton
    private lateinit var baseSpinner: Spinner
    private lateinit var logoutButton: Button
    private lateinit var syncButton: Button
    private lateinit var syncLabel: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize UI components
        nameEditText = view.findViewById(R.id.nameEditText)
        saveButton = view.findViewById(R.id.conditionButton)
        baseLabel = view.findViewById(R.id.baseLabel)
        logoutButton = view.findViewById(R.id.logoutButton)
        syncButton = view.findViewById(R.id.syncRosterButton)
        syncLabel = view.findViewById(R.id.lastSyncedTime)

        // Initialize radio buttons
        exportRadioGroup = view.findViewById(R.id.exportRadioGroup)
        exportOffButton = view.findViewById(R.id.exportOff)
        exportOnButton = view.findViewById(R.id.exportOn)

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)

        // Call updateSyncLabel()
        updateSyncLabel()

        // Inside your Fragment's onCreateView or onViewCreated method
        val instructionsButton = view.findViewById<Button>(R.id.instructionsButton)

        // ---------------------------------------- Setup Listeners  -------------------------------------------------------------------- //

        // Set an OnClickListener for the button
        instructionsButton.setOnClickListener {
            // Get access to SharedPreferences
            val sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)

            // Clear all the stored preferences
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()

            // Notify the user
            Toast.makeText(requireContext(), "All preferences have been cleared!", Toast.LENGTH_SHORT).show()
        }

        // Set a click listener for the logout button
        logoutButton.setOnClickListener {
            // Confirm logout action with an alert dialog
            AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogStyle)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out?  This action will completely reset JRoster")
                .setPositiveButton("Yes") { _, _ ->
                    // Take action
                    logoutAction()
                }
                .setNegativeButton("No", null)
                .show()
        }

        // Set a click listener for the sync roster button
        syncButton.setOnClickListener {
            val userID = sharedPreferences.getString("userID", "") ?: ""
            val passCode = sharedPreferences.getString("passCode", "") ?: ""

            // Store the current timestamp (in milliseconds)
            val currentTime = System.currentTimeMillis()
            sharedPreferences.edit().putLong("lastSyncTime", currentTime).apply()

            // Update the local DB from MySQL
            fetchRosterData(userID, passCode)

            // Update the sync label immediately after sync
            syncLabel.text = "0 min"
        }

        // When exportOnButton is selected, trigger the calendar export process
        exportOnButton.setOnClickListener {
            checkAndRequestCalendarPermission()
        }

        // Add TextWatcher to handle save button visibility
        nameEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.isNotEmpty() == true) {
                    baseLabel.visibility = View.GONE
                    baseSpinner.visibility = View.GONE
                    saveButton.visibility = View.VISIBLE
                } else {
                    saveButton.visibility = View.GONE
                    baseLabel.visibility = View.VISIBLE
                    baseSpinner.visibility = View.VISIBLE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Set up the save button click listener
        saveButton.setOnClickListener {
            saveAction()
        }

        // Set a listener for the radio button group to save changes
        exportRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val editor = sharedPreferences.edit()
            when (checkedId) {
                R.id.exportOn -> {
                    editor.putBoolean("isExportOn", true)
                }
                R.id.exportOff -> {
                    editor.putBoolean("isExportOn", false)
                }
            }
            editor.apply() // Save the changes
        }

        // ---------------------------------------- Var Setup  -------------------------------------------------------------------- //

        // Initialize the EditText
        val nameEditText = view.findViewById<EditText>(R.id.nameEditText)

        // Check for name in SharedPreferences
        val storedName = sharedPreferences.getString("userName", null)

        if (storedName != null) {
            // If name exists in SharedPreferences, set it to the EditText
            nameEditText.setText(storedName)
        } else {
            // If name does not exist, fetch from MySQL
            val userID = sharedPreferences.getString("userID", "") ?: ""
            fetchFriendDataFromMySQL(userID)
        }

        // Set save button visibility to invisible initially3
        saveButton.visibility = View.INVISIBLE



        // Load saved radio button state
        val isExportOn = sharedPreferences.getBoolean("isExportOn", false) // Default is 'false' (Off)

        // Set the radio button state based on saved value
        if (isExportOn) {
            exportOnButton.isChecked = true
        } else {
            exportOffButton.isChecked = true
        }

        // Initialize the base Spinner
        baseSpinner = view.findViewById(R.id.baseSpinner)

        // Create an ArrayAdapter using the custom spinner item layout
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.base_array,
            R.layout.spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            baseSpinner.adapter = adapter
        }

        // Set up SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)

        // Retrieve the saved base value from SharedPreferences
        val savedBase = sharedPreferences.getString("base", "Sydney") // Default to 'Sydney'

        // Set the Spinner to the saved base value
        baseSpinner.setSelection(adapter.getPosition(savedBase))

        // Set a listener for the Spinner to save the selected item in SharedPreferences
        baseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedBase = parent.getItemAtPosition(position).toString()

                // Save the selected base to SharedPreferences
                sharedPreferences.edit().putString("base", selectedBase).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        return view
    }

    // ---------------------------------------- Calendar Functions  -------------------------------------------------------------------- //

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportDutiesToGoogleCalendar()
            } else {
                Toast.makeText(requireContext(), "Calendar permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportDutiesToGoogleCalendar() {
        val eventUriString = "content://com.android.calendar/events"
        val calendarId: Long = getPrimaryCalendarId()

        if (calendarId == -1L) {
            Toast.makeText(requireContext(), "No calendar available", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch all DbData (your flight data) from the Room database
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(requireContext())
            val flights = db.dbDataDao().getAll() // Replace with your actual method for fetching all flights

            withContext(Dispatchers.Main) {
                // Loop through each flight and add events to the calendar
                for (flight in flights) {
                    // Parse ATD (Actual Time of Departure) from string to Date
                    val atdDate = parseDate(flight.atd)
                    val ataDate = parseDate(flight.ata)

                    // Check if ATD exists
                    atdDate?.let { atd ->
                        // Create flight event
                        val flightEventValues = ContentValues().apply {
                            put(CalendarContract.Events.CALENDAR_ID, calendarId)
                            put(CalendarContract.Events.TITLE, flight.activity)
                            put(CalendarContract.Events.DTSTART, atd.time)
                            put(CalendarContract.Events.DTEND, ataDate?.time ?: atd.time + 3600000) // Default to 1-hour duration if ATA is not available
                            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                            put(CalendarContract.Events.DESCRIPTION, "${flight.orig} to ${flight.dest}")
                        }
                        requireContext().contentResolver.insert(Uri.parse(eventUriString), flightEventValues)
                    }

                    // Optional: Add sign-on event
                    val checkInDate = parseDate(flight.checkIn)
                    checkInDate?.let {
                        val signOnValues = ContentValues().apply {
                            put(CalendarContract.Events.CALENDAR_ID, calendarId)
                            put(CalendarContract.Events.TITLE, "Sign On")
                            put(CalendarContract.Events.DTSTART, it.time)
                            put(CalendarContract.Events.DTEND, it.time + 3600000) // 1 hour
                            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                        }
                        requireContext().contentResolver.insert(Uri.parse(eventUriString), signOnValues)
                    }

                    // Optional: Add sign-off event
                    val checkOutDate = parseDate(flight.checkOut)
                    checkOutDate?.let {
                        val signOffValues = ContentValues().apply {
                            put(CalendarContract.Events.CALENDAR_ID, calendarId)
                            put(CalendarContract.Events.TITLE, "Sign Off")
                            put(CalendarContract.Events.DTSTART, it.time)
                            put(CalendarContract.Events.DTEND, it.time + 3600000) // 1 hour
                            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                        }
                        requireContext().contentResolver.insert(Uri.parse(eventUriString), signOffValues)
                    }
                }

                Toast.makeText(requireContext(), "Duties exported to Google Calendar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper function to parse date strings
    private fun parseDate(dateString: String): Date? {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    // Get the primary calendar ID
    private fun getPrimaryCalendarId(): Long {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
        val uri = CalendarContract.Calendars.CONTENT_URI
        val cursor = requireContext().contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(0)  // Return the first calendar ID found
            }
        }
        return -1L
    }

    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("Permission", "${it.key} = ${it.value}")
                if (it.key == Manifest.permission.WRITE_CALENDAR && it.value) {
                    // Permission granted.  Start the show
                    exportDutiesToGoogleCalendar()
                } else {
                    Toast.makeText(requireContext(), "Calendar permission is required to export duties", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun checkAndRequestCalendarPermission() {
        // Check if permissions are already granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            // If permissions are granted, proceed with calendar export
            exportDutiesToGoogleCalendar()
        } else {
            // Otherwise, request the permissions
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR)
            )
        }
    }

    // ---------------------------------------- Sync Roster Functions  -------------------------------------------------------------------- //

    private fun fetchRosterData(userID: String, passCode: String) {
        val call = RetrofitClient.rosterApiService.getRosterData(userID, passCode)

        call.enqueue(object : retrofit2.Callback<List<DbData>> {
            override fun onResponse(call: retrofit2.Call<List<DbData>>, response: retrofit2.Response<List<DbData>>) {
                if (response.isSuccessful) {
                    val rosterData = response.body()

                    rosterData?.let { data ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val db = AppDatabase.getInstance(requireContext())

                            // Only clear the database and insert if we get non-empty data
                            if (data.isNotEmpty()) {
                                db.dbDataDao().deleteAll()
                                db.dbDataDao().insertAll(data)
                            }

                            lifecycleScope.launch(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Roster Synced Successfully", Toast.LENGTH_SHORT).show()
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

            override fun onFailure(call: retrofit2.Call<List<DbData>>, t: Throwable) {
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Offline Mode: Unable to fetch data", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun fetchFriendDataFromMySQL(userID: String) {
        val url = "http://flightschoolms.com/JRoster/getName.php"
        val requestBody = "userID=$userID"
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull()))
            .build()

        // Make the request using OkHttp
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    showAlert("Error", "Failed to fetch friend data: ${e.localizedMessage}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    requireActivity().runOnUiThread {
                        showAlert("Error", "Server error: ${response.code}")
                    }
                    return
                }

                response.body?.string()?.let { responseData ->
                    try {
                        val jsonArray = JSONArray(responseData)

                        // Check if the JSON array is not empty
                        if (jsonArray.length() > 0) {
                            val friendData = jsonArray.getJSONObject(0)
                            val friendName = friendData.optString("name", "")

                            requireActivity().runOnUiThread {
                                if (friendName.isNotEmpty()) {
                                    // Save the name to SharedPreferences
                                    val sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
                                    sharedPreferences.edit().putString("userName", friendName).apply()

                                    // Set the name in the EditText
                                    nameEditText.setText(friendName)
                                } else {
                                    showAlert("Info", "No name found for this user.")
                                }
                            }
                        } else {
                            requireActivity().runOnUiThread {
                                showAlert("Info", "No friend data found.")
                            }
                        }
                    } catch (e: JSONException) {
                        requireActivity().runOnUiThread {
                            showAlert("Error", "Failed to parse JSON: ${e.localizedMessage}")
                        }
                    }
                }
            }
        })
    }

    // ---------------------------------------- Core Functions  -------------------------------------------------------------------- //

    private fun saveAction() {
        // Remove focus from the EditText
        nameEditText.clearFocus()

        val previousName = sharedPreferences.getString("userName", "") ?: ""

        if (nameEditText.text.toString().isNotEmpty()) {
            if (!isInternetAvailable()) {
                // No internet connection; alert the user and restore the previous name
                showAlert("No Internet Connection", "Please check your internet connection and try again.")
                nameEditText.setText(previousName)
                return
            }

            // Save the name in SharedPreferences
            val newName = nameEditText.text.toString()
            sharedPreferences.edit().putString("userName", newName).apply()
            showAlert("Save Success", "$newName will now appear as your name in the friends tab for anyone you have shared your Friend Code with.")

            // Post to server
            postToServer(newName)

            // Hide the button and show the base label
            saveButton.visibility = View.INVISIBLE
            baseLabel.visibility = View.VISIBLE
            baseSpinner.visibility = View.VISIBLE
        } else {
            showAlert("Error", "You are trying to save a blank name. JRoster has a strict non-blank name policy.")
        }
    }

    private fun postToServer(userName: String) {
        val userID = sharedPreferences.getString("userID", "") ?: ""

        val client = OkHttpClient()
        val url = "http://flightschoolms.com/JRoster/updateFriendName.php"
        val requestBody = FormBody.Builder()
            .add("userName", userName)
            .add("userID", userID)
            .build()
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SettingsFragment", "Error occurred: $e")
                activity?.runOnUiThread {
                    showAlert("Error", "Failed to save your name. Please try again later.")
                    nameEditText.setText(sharedPreferences.getString("userName", ""))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("SettingsFragment", "Response: ${response.body?.string()}")
                }
            }
        })
    }

    // ---------------------------------------- Boilerplate  -------------------------------------------------------------------- //

    // Youll never guess.
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Android 23+ >
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            return when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            // Android < 23
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo?.isConnected == true
        }
    }

    private fun showAlert(title: String, message: String) {
        // Create an AlertDialog builder with the context of the fragment
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogStyle)

        // Set the title and message of the alert dialog
        builder.setTitle(title)
        builder.setMessage(message)

        // Add a button to the dialog
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        // Create and display the alert dialog
        val alertDialog = builder.create()
        alertDialog.show()
    }

    // Function to handle the logout action
    private fun logoutAction() {
        // 1. Clear all data from the local database
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(requireContext())
            db.clearAllTables()

            withContext(Dispatchers.Main) {
                // 2. Clear all shared preferences
                sharedPreferences.edit().clear().apply()

                // 3. Return the user to the main activity
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)

                // Optionally, display a toast message
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to calculate and update the sync label
    fun updateSyncLabel() {
        val lastSyncTime = sharedPreferences.getLong("lastSyncTime", 0L)

        if (lastSyncTime > 0) {
            val currentTime = System.currentTimeMillis()
            val timeDiffInMinutes = ((currentTime - lastSyncTime) / 1000 / 60).toInt()

            val days = timeDiffInMinutes / (24 * 60)
            val hours = (timeDiffInMinutes % (24 * 60)) / 60
            val minutes = timeDiffInMinutes % 60

            // Constructing the formatted time string
            val formattedTime = when {
                days > 0 -> "$days Days $hours Hours $minutes Min"
                hours > 0 -> "$hours Hour $minutes Min"
                else -> "$minutes Min"
            }

            syncLabel.text = formattedTime
        } else {
            // If there's no sync time saved yet
            syncLabel.text = "Not Synced"
        }
    }
}