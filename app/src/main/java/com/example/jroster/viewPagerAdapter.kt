// ViewPagerAdapter.kt
package com.example.jroster

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    // Return the total number of fragments
    override fun getItemCount(): Int = 3

    // Return the appropriate Fragment for each position
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> fragmentFriends()   // First Fragment (Friends)
            1 -> fragmentRoster()    // Second Fragment (Roster)
            2 -> fragmentSettings()  // Third Fragment (Settings)
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}
