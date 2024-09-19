package com.example.jroster

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FragmentRoster : Fragment() {
    private lateinit var rosterRecyclerView: RecyclerView
    private lateinit var rosterAdapter: RosterAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_roster, container, false)

        // Initialize RecyclerView
        rosterRecyclerView = view.findViewById(R.id.rosterRecyclerView)
        rosterRecyclerView.layoutManager = LinearLayoutManager(context)

        // Sample data for demonstration
        val rosterList: List<Any> = listOf(
            "Wed 11 Sept 2024",
            Duty("SYD", "RAR", "07:15L", "13:45L"),
            Duty("RAR", "SYD", "14:15L", "20:45L"),
            "Thu 12 Sept 2024",
            Duty("SYD", "BNE", "08:00L", "09:30L")
        )

        rosterAdapter = RosterAdapter(rosterList)
        rosterRecyclerView.adapter = rosterAdapter

        // Add divider between rows
        val dividerItemDecoration = DividerItemDecoration(
            rosterRecyclerView.context,
            DividerItemDecoration.VERTICAL
        )
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider)!!)
        rosterRecyclerView.addItemDecoration(dividerItemDecoration)

        return view
    }
}
