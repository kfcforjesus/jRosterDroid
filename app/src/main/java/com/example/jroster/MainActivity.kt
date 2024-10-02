package com.bencornett.jroster

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bencornett.jroster.R
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userIDEditText: EditText
    private lateinit var passcodeEditText: EditText
    private lateinit var signInButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)

        // Check if the login token exists
        val loginTokenExists = sharedPreferences.getBoolean("loginToken", false)

        if (loginTokenExists) {
            // If login token exists, proceed to the settings activity
            Log.d("MainActivity", "Login token found. Redirecting to SettingsActivity.")
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish() // Stop back key by dropping from stack
        } else {
            Log.d("MainActivity", "No login token found. Staying on MainActivity.")
            initializeUI()
        }
    }

    // Setup the UI in event of successful login
    private fun initializeUI() {
        // Initialize UI components
        userIDEditText = findViewById(R.id.userIDEditText)
        passcodeEditText = findViewById(R.id.passcodeEditText)
        signInButton = findViewById(R.id.signInButton)

        // Set up the Sign In button listener
        signInButton.setOnClickListener {
            val userID = userIDEditText.text.toString()
            val passCode = passcodeEditText.text.toString()
            performLogin(userID, passCode)
        }
    }

    private fun performLogin(userID: String, passCode: String) {
        val url = "http://flightschoolms.com/JRoster/login.php"

        // Create a client
        val client = OkHttpClient()

        // Create request body
        val requestBody = FormBody.Builder()
            .add("userID", userID)
            .add("passCode", passCode)
            .build()

        // Create a request
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        // Make an asynchronous call
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to connect to server: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Server returned status code ${response.code}", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                val responseData = response.body?.string()
                if (responseData != null) {
                    try {
                        val json = JSONObject(responseData)
                        val status = json.getString("status")
                        val message = json.getString("message")

                        runOnUiThread {
                            if (status == "success") {
                                // Save login info in SharedPreferences
                                val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
                                val editor = sharedPreferences.edit()
                                editor.putBoolean("loginToken", true)
                                editor.putString("userID", userID)
                                editor.putString("passCode", passCode)
                                editor.apply()

                                // Open the SettingsButtonActivity
                                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                                startActivity(intent)
                                finish() // Close the current activity
                            } else {
                                Toast.makeText(this@MainActivity, "Login Error: $message", Toast.LENGTH_LONG).show()
                                // Reset the input fields
                                userIDEditText.text.clear()
                                passcodeEditText.text.clear()
                                userIDEditText.requestFocus()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Failed to parse response: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "No data received from server", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
