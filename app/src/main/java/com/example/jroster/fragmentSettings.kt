package com.example.jroster

import android.app.AlertDialog
import android.content.Context
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody


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

        // Initialize radio buttons
        exportRadioGroup = view.findViewById(R.id.exportRadioGroup)
        exportOffButton = view.findViewById(R.id.exportOff)
        exportOnButton = view.findViewById(R.id.exportOn)

        // Inside your Fragment's onCreateView or onViewCreated method
        val instructionsButton = view.findViewById<Button>(R.id.instructionsButton)

        // Set an OnClickListener for the button
        instructionsButton.setOnClickListener {
            // Get access to SharedPreferences
            val sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)

            // Clear all the stored preferences
            val editor = sharedPreferences.edit()
            editor.clear() // This will remove all saved data
            editor.apply() // Apply the changes

            // Notify the user
            Toast.makeText(requireContext(), "All preferences have been cleared!", Toast.LENGTH_SHORT).show()
        }

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)

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

        // Add TextWatcher to handle save button visibility
        nameEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.isNotEmpty() == true) {
                    baseLabel.visibility = View.GONE
                    baseSpinner.visibility = View.GONE
                    saveButton.visibility = View.VISIBLE

                    Log.d("VisibilityCheck", "Name entered: Save button visible, Spinner gone")
                } else {
                    saveButton.visibility = View.GONE
                    baseLabel.visibility = View.VISIBLE
                    baseSpinner.visibility = View.VISIBLE
                    Log.d("VisibilityCheck", "Name cleared: Save button hidden, Spinner visible")
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Set up the save button click listener
        saveButton.setOnClickListener {
            saveAction()
        }

        // Load saved radio button state
        val isExportOn = sharedPreferences.getBoolean("isExportOn", false) // Default is 'false' (Off)

        // Set the radio button state based on saved value
        if (isExportOn) {
            exportOnButton.isChecked = true
        } else {
            exportOffButton.isChecked = true
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

    // Save the users name to user prefs
    private fun saveNameToPreferences(name: String) {
        val sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("userName", name)
        editor.apply()
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

    private fun isInternetAvailable(): Boolean {
        // Implement network connectivity check here
        return true
    }

    private fun showAlert(title: String, message: String) {
        // Create an AlertDialog builder with the context of the fragment
        val builder = AlertDialog.Builder(requireContext()) // Use 'requireContext()' for Fragment

        // Set the title and message of the alert dialog
        builder.setTitle(title)
        builder.setMessage(message)

        // Add a button to the dialog
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss() // Close the dialog when the button is pressed
        }

        // Create and display the alert dialog
        val alertDialog = builder.create()
        alertDialog.show()
    }
}