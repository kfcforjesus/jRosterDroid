package com.example.jroster

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.jroster.databinding.ActivitySettingsBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.util.Log

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("Testing", "onCreate of SettingsActivity called")

        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the initial fragment
        replaceFragment(FragmentSettings())

        // Set up the BottomNavigationView listener
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->

        when (item.itemId) {
                R.id.nav_friends -> {
                    replaceFragment(fragmentFriends())
                    true
                }
                R.id.nav_roster -> {
                    replaceFragment(fragmentRoster())
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
