package com.example.jroster

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("Testing", "OnCreate MainActivity")

        super.onCreate(savedInstanceState)
        window.setDecorFitsSystemWindows(false)
        setContentView(R.layout.activity_main)

        val userIDEditText = findViewById<EditText>(R.id.userIDEditText)
        val passcodeEditText = findViewById<EditText>(R.id.passcodeEditText)
        val signInButton = findViewById<Button>(R.id.signInButton)

        // Add TextWatcher to userIDEditText
        userIDEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 5) {
                    passcodeEditText.requestFocus() // Automatically move focus to passcode field
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Add TextWatcher to passcodeEditText
        passcodeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 3) {
                    passcodeEditText.clearFocus() // Clear focus from passcode field
                    hideKeyboard(passcodeEditText) // Hide the keyboard
                }
            }
        })

        // Set up the Sign In button to open SettingsActivity
        signInButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java) // Use the correct activity
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Function to hide the keyboard
    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
