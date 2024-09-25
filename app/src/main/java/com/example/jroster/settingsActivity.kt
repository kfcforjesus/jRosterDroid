package com.example.jroster

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.jroster.databinding.ActivitySettingsBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.util.Log

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("Testing", "onCreate of SettingsActivity called")

        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize BottomNavigationView
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Get SharedPreferences
        val sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val loginToken = sharedPreferences.getBoolean("loginToken", false)

        // If loginToken is true, immediately switch to FragmentRoster and set the correct nav item
        if (loginToken) {
            replaceFragment(FragmentRoster())
            bottomNavigationView.selectedItemId = R.id.nav_roster // Highlight the Roster tab
        } else {
            // Set the initial fragment to FragmentSettings if loginToken is false
            replaceFragment(FragmentSettings())
            bottomNavigationView.selectedItemId = R.id.nav_settings // Highlight the Settings tab
        }

        // Set up the BottomNavigationView listener
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_friends -> {
                    replaceFragment(fragmentFriends())
                    true
                }
                R.id.nav_roster -> {
                    replaceFragment(FragmentRoster())
                    true
                }
                R.id.nav_settings -> {
                    replaceFragment(FragmentSettings())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        Log.d("Testing", "SettingsActivity - ReplaceFragmentCalled")
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        // Replace the container with the new fragment
        fragmentTransaction.replace(R.id.fragment_container, fragment)

        // Commit the transaction
        fragmentTransaction.commit()
    }
}
