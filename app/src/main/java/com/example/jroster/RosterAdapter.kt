package com.example.jroster

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RosterAdapter(
    private val sortedDates: List<String>,
    private val entriesByDate: Map<String, List<DbData>>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_DATE_HEADER = 0
    private val VIEW_TYPE_ROSTER_ENTRY = 1

    override fun getItemViewType(position: Int): Int {
        return if (isPositionDateHeader(position)) VIEW_TYPE_DATE_HEADER else VIEW_TYPE_ROSTER_ENTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_DATE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.date_header_layout, parent, false)
            DateViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.duty_row_layout, parent, false)
            RosterEntryViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DateViewHolder) {
            holder.bind(getDateForPosition(position))
        } else if (holder is RosterEntryViewHolder) {
            val entry = getEntryForPosition(position)
            entry?.let {
                holder.bind(it, ::formatTime)
            }
        }
    }

    // Function to format date (2024-12-18 -> Tue 24 Sep 2024)
    fun formatDate(date: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEE dd MMM yyyy", Locale.getDefault())

            val parsedDate = inputFormat.parse(date)
            parsedDate?.let { outputFormat.format(it) } ?: date
        } catch (e: Exception) {
            date
        }
    }

    // Function to format the time (hh:mm)
    fun formatTime(dateTime: String?): String {
        if (dateTime == null || dateTime.isEmpty()) return "N/A"

        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        return try {
            val date = inputFormat.parse(dateTime)
            if (date != null) outputFormat.format(date) else "N/A"
        } catch (e: Exception) {
            "N/A"
        }
    }

    override fun getItemCount(): Int {
        return sortedDates.sumOf { 1 + (entriesByDate[it]?.size ?: 0) }
    }

    private fun isPositionDateHeader(position: Int): Boolean {
        var currentPos = 0
        for (date in sortedDates) {
            if (position == currentPos) return true
            currentPos += 1 + (entriesByDate[date]?.size ?: 0)
        }
        return false
    }

    private fun getDateForPosition(position: Int): String {
        var currentPos = 0
        for (date in sortedDates) {
            if (position == currentPos) return date
            currentPos += 1 + (entriesByDate[date]?.size ?: 0)
        }
        return ""
    }

    private fun getEntryForPosition(position: Int): DbData? {
        var currentPos = 0
        for (date in sortedDates) {
            val entries = entriesByDate[date]
            if (position > currentPos && position <= currentPos + (entries?.size ?: 0)) {
                return entries?.get(position - currentPos - 1)
            }
            currentPos += 1 + (entries?.size ?: 0)
        }
        return null
    }

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.rosterDate)

        // Modify the bind function to properly format and display the date
        fun bind(date: String) {
            // Date formatter for parsing the date string
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayedDateFormatter = SimpleDateFormat("EEE dd MMM yyyy", Locale.getDefault())

            // Get today's date at midnight
            val today = Calendar.getInstance()
            today.time = Date()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)
            val todayDate = today.time

            // Parse the current date from the string
            val parsedDate = dateFormatter.parse(date)

            // Check if the parsed date is valid
            parsedDate?.let {
                // Format the date to display
                var formattedDate = displayedDateFormatter.format(it)

                // Check if this date is today
                if (parsedDate == todayDate) {
                    formattedDate += " (Today)"
                }

                // Set the formatted date text to the TextView
                dateTextView.text = formattedDate
            } ?: run {
                // In case of an error with parsing, show an invalid date text
                dateTextView.text = "Invalid Date"
            }
        }
    }

    class RosterEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val flightIcon: ImageView = itemView.findViewById(R.id.flightIcon)
        private val flightRouteTextView: TextView = itemView.findViewById(R.id.flightRoute)
        private val flightDataTextView: TextView = itemView.findViewById(R.id.flightData)
        private val flightTimesTextView: TextView = itemView.findViewById(R.id.flightTimes)

        @SuppressLint("SetTextI18n")
        fun bind(entry: DbData, formatTime: (String?) -> String) {
            // Special mapping of activities for customization

            val activityMapping = mapOf(
                "PCK" to "Pickup",
                "OFF" to "Day Off",
                "STB" to "Standby",
                "DBF" to "Debrief",
                "FDO" to "Fixed Day Off",
                "UFD" to "UFD",
                "LVE" to "Leave",
                "AOF" to "Leave",
                "OWN" to "Own",
                "DFD" to "DFD",
                "HTC" to "HTL to Training",
                "XSB" to "STBY Callout",
                "STR" to "Star Day",
                "ESB" to "Extended STBY Period",
                "A2B" to "Av Aus to BNE",
                "A2H" to "Av Aus to HTL",
                "A2S" to "Airport to SIM #1",
                "A3S" to "Airport to SIM #2",
                "ATC" to "Airport to TC",
                "B2A" to "BNE to Av Aus",
                "B2T" to "BNE to Alteon",
                "G2H" to "Garden Dr to HTL",
                "G2M" to "Garden Dr to MEL",
                "H22" to "HTL to Headoffice",
                "H2A" to "HTL to Av Aus",
                "H2G" to "HTL to Garden Dr",
                "H2J" to "HTL to Jet Base",
                "H2Q" to "HTL to QCC Building",
                "H2S" to "HTL to SIM #1",
                "H2T" to "HTL to Alteon",
                "H2W" to "HTL to Airport West",
                "H3S" to "HTL to SIM #2",
                "J2H" to "Jet Base to HTL",
                "J2S" to "Jet Base to SYD",
                "M22" to "MEL to Headoffice",
                "M2G" to "MEL to Garden Dr",
                "M2W" to "MEL to Airport West",
                "Q2H" to "QCC to HTL",
                "Q2S" to "QCC to SYD",
                "S2A" to "SIM to Airport",
                "S2H" to "SIM to HTL",
                "S2J" to "SYD to Jet Base",
                "S2Q" to "SYD to QCC",
                "S3A" to "SIM to Airport",
                "S3H" to "SIM to HTL",
                "SAK" to "Security to AKL",
                "T2B" to "Alteon to BNE",
                "T2H" to "Alteon to HTL",
                "TCA" to "TC to Airport",
                "TCH" to "TC to HTL",
                "W2H" to "Airport West to HTL",
                "W2M" to "Airport West to MEL",
                "ALV" to "Low Vis Sim",
                "A01" to "Narrow Runway & Approach Operations",
                "A02" to "PBN (RNP Simulator Training)",
                "A04" to "Undesired Aircraft State",
                "A05" to "Simulator SOP Training",
                "A06" to "Simulator Proficiency Check",
                "A07" to "A320 TR Flight Test",
                "A08" to "Sim Cadet Base Training",
                "A09" to "CDT Intro to Phase 2 Training",
                "AAS" to "ATPL Day2 - Sim",
                "A03" to "Training Simulator",
                "AB1" to "Type Rating FBS 1",
                "AB2" to "Type Rating FBS 2",
                "AB3" to "Type Rating FBS 3",
                "AB4" to "Type Rating FBS 4",
                "AB5" to "Type Rating FBS 5",
                "AB6" to "Type Rating FBS 6",
                "AB7" to "Type Rating FBS 7",
                "AB8" to "Type Rating FBS 8",
                "AB9" to "Type Rating FBS 9",
                "AF1" to "Type Rating FFS 1",
                "AF2" to "Type Rating FFS 2",
                "AF3" to "Type Rating FFS 3",
                "AF4" to "Type Rating FFS 4",
                "AF5" to "Type Rating FFS 5",
                "AF6" to "Type Rating FFS 6",
                "AF7" to "Type Rating FFS 7",
                "AF8" to "Type Rating FFS 8",
                "AF9" to "Type Rating FFS 9",
                "AFL" to "Low Visibility Operations FO",
                "AT1" to "Training Simulator 1",
                "AT2" to "Training Simulator 2",
                "AT3" to "Training Simulator 3",
                "AT4" to "Training Simulator 4",
                "B02" to "PBN, No Slope, LAHSO, PRM",
                "B05" to "SIM SOP Training",
                "B06" to "Simulator Proficiency Check",
                "B07" to "B787 TR Flight Test",
                "BAS" to "ATPL Test Day2 - Sim",
                "BCL" to "LVO & Contaminated RWY CP",
                "BF1" to "Type Rating FFS 1",
                "BF2" to "Type Rating FFS 2",
                "BF3" to "Type Rating FFS 3",
                "BF4" to "Type Rating FFS 4",
                "BF5" to "Type Rating FFS 5",
                "BF6" to "Type Rating FFS 6",
                "BF7" to "Type Rating FFS 7",
                "BF8" to "Type Rating FFS 8",
                "BFL" to "LVO & Contaminated RWY FO",
                "BST" to "BOOST SIM Training",
                "BT1" to "Training Simulator 1",
                "BT2" to "Training Simulator 2",
                "BT3" to "Training Simulator 3",
                "BT4" to "Training Simulator 4",
                "CMC" to "SIM Command Check",
                "CMM" to "Command Upgrade SIM",
                "CMP" to "SIM Command Progress",
                "CPS" to "Cadet Progress Sim",
                "CR2" to "Recurrent Conversion Sim Training",
                "CT3" to "TRANSITION 4 - LVO",
                "CTC" to "LVO",
                "FF5" to "787 FFS5",
                "FF9" to "787 FFS9 Final Prof Check Sim",
                "FFP" to "320 FFS10 Final Prof Check Sim",
                "FFS" to "Full Flight Sim",
                "FSE" to "Failed Sim EBT",
                "ILC" to "Initial Line Check SIM",
                "IP1" to "787 IPT10 Interim Prof Check Sim",
                "ITM" to "SIM Interim",
                "LOT" to "LOFT Sim Training",
                "MPS" to "Mgt Prof Sim",
                "PCS" to "Pre-Command SIM Assessment",
                "RIS" to "Remedial Simulator - Incident",
                "RNV" to "SIM RNAV (GNSS) Approach",
                "RTE" to "Remedial Training EBT sim",
                "RTS" to "Remedial Training Sim",
                "SD2" to "SIM Day 2 Cyclic D 320/321",
                "SD3" to "SIM Day 2 Cyclic D 332",
                "SD7" to "SIM Day 2 Cyclic D 787",
                "SI1" to "SIM S/O Cyclic 1 - TRNG",
                "SI2" to "SIM S/O Cyclic 2 - CHK",
                "SI3" to "SIM S/O Cyclic 3 - TRNG",
                "SI4" to "SIM S/O Cyclic 4 - CHK",
                "SID" to "NOT IN USE",
                "SIT" to "TRANSITION 2",
                "SR2" to "SIM right hand seat 320",
                "SR3" to "SIM Right hand seat 332",
                "SR7" to "SIM Right hand seat 787",
                "SRV" to "Simulator Revalidation",
                "ST2" to "TRANSITION 3 - IPC",
                "ST3" to "SIM Transition 332",
                "ST7" to "SIM Transition 787",
                "STP" to "Senior Training Pilot Simulator Training",
                "SIM" to "Obsolete - SIM non Specific",
                "SB7" to "SIM Day 2 Cyclic B 787",
                "TVL" to "Avail for Training",
                "EPT" to "Emergency Procedures Training",
                "EP" to "Emergency Procedures",
                "EPC" to "Emergency Procedures Check",
                "AAG" to "ATPL Test Day1 - Ground",
                "BAG" to "ATPL Test Day1 - Ground",
                "RNG" to "RNP-AR Ground Training",
                "GSI" to "Gnd School Instructor",
                "GT1" to "Gnd Training Course 1",
                "GT2" to "Gnd Training Course 2",
                "GT3" to "Gnd Training Course 3",
                "GT4" to "Gnd Training Course 4",
                "GA4" to "GS - Previous A320 Experience",
                "GA5" to "GS - NO Previous A320 Experience",
                "GA7" to "Gnd Induction INTAKE",
                "GB3" to "GS (Component A)",
                "GB8" to "Gnd Induction - INTAKE",
                "SGT" to "Sick For Ground Training",
                "CPG" to "Command Upgrade Gnd School",
                "ETG" to "ETOP Gnd Training",
                "FGT" to "Failed Gnd Training",
                "G03" to "Command Upgrade GS",
                "GS" to "Ground School",
                "GA3" to "GA3 - Gnd Training",
                "ADM" to "Admin"
            )

            // Icons mapping for duties
            val iconMapping = mapOf(
                "Sign On" to R.drawable.clock,
                "Pickup" to R.drawable.bus,
                "UFD" to R.drawable.sick,
                "Leave" to R.drawable.off,
                "DFD" to R.drawable.house,
                "Day Off" to R.drawable.house,
                "Standby" to R.drawable.phone_callback_24dp_5f6368_fill0_wght400_grad0_opsz24,
                "Fixed Day Off" to R.drawable.house,
                "EPT" to R.drawable.school_24dp_5f6368_fill0_wght400_grad0_opsz24,
                "TCA" to R.drawable.school_24dp_5f6368_fill0_wght400_grad0_opsz24,
                "OFF" to R.drawable.house,
                "HTL to Training" to R.drawable.bus,
                "STBY Callout" to R.drawable.phone_callback_24dp_5f6368_fill0_wght400_grad0_opsz24,
                "Standby" to R.drawable.phone_callback_24dp_5f6368_fill0_wght400_grad0_opsz24,
                "Star Day" to R.drawable.hotel_class_24dp_5f6368_fill0_wght400_grad0_opsz24,
                "Extended STBY Period" to R.drawable.phone_callback_24dp_5f6368_fill0_wght400_grad0_opsz24,
                "Avail for Training" to R.drawable.phone_callback_24dp_5f6368_fill0_wght400_grad0_opsz24,
                "Debrief" to R.drawable.swords,
                "Admin" to R.drawable.smiley,
            )

            // Handle special activities where the route should be blank and the times adjusted
            val specialActivities = listOf( "PCK", "OFF", "UFD", "LVE", "AOF", "EPT", "OWN", "DFD", "HTC", "XSB", "STR", "ESB",
                "A2B", "A2H", "A2S", "A3S", "ATC", "B2A", "B2T", "G2H", "G2M", "H22", "H2A", "H2G",
                "H2J", "H2Q", "H2S", "H2T", "H2W", "H3S", "J2H", "J2S", "M22", "M2G", "M2W", "Q2H",
                "Q2S", "S2A", "S2H", "S2J", "S2Q", "S3A", "S3H", "SAK", "T2B", "T2H", "TCA", "TCH",
                "W2H", "W2M", "ST7", "TRT", "ZRS", "ZRI", "ZNI", "STP", "SIM", "SB7", "ST2", "ST3",
                "SIC", "SIA", "SB2", "CVA", "A06", "SD7", "L03", "L02", "L01", "A01", "A02", "A04",
                "A05", "A07", "A08", "A09", "AAS", "AB1", "AB2", "AB3", "AB4", "AB5", "AB6", "AB7",
                "AB8", "AB9", "AF1", "AF2", "AF3", "AF4", "AF5", "AF6", "AF7", "AF8", "AF9", "AFL",
                "AT1", "AT2", "AT3", "AT4", "B02", "B05", "B06", "B07", "BAS", "BCL", "BF1", "BF2",
                "BF3", "BF4", "BF5", "BF6", "BF7", "BF8", "BFL", "BST", "BT1", "BT2", "BT3", "BT4",
                "CMC", "CMM", "CMP", "CPS", "CR2", "CT3", "CTC", "FF5", "FF9", "FFP", "FFS", "FSE",
                "ILC", "IP1", "ITM", "LOT", "MPS", "PCS", "RIS", "RNV", "RTE", "RTS", "SD2", "SD3",
                "SI1", "SI2", "SI3", "SI4", "SID", "SIT", "SR2", "SR3", "SR7", "SRV", "TVL", "EPT",
                "EP","EPC", "FDO", "ALV", "STB", "DBF", "GA4", "GA5", "GA7", "AAG","BAG","RGD",
                "RNG","GS","GSI","GT1","GT2","GT3","GT4","GA4","GA5","GA7","GB3","GB8","SGT","CPG",
                "ETG","FGT","G03", "GA3", "ADM")

            // Duty Mapping
            val dutyMapping = mapOf(
                "P" to "(Paxing)",
                "D" to "(Deadhead)",
                "" to " "
            )

            Log.d("Cunt", entry.activity)

            if (listOf("OFF", "LVE", "UFD", "DFD", "AOF", "XSB", "FDO").contains(entry.activity)) {

                // Move flightRouteTextView down by 8 pixels
                val layoutParams = flightRouteTextView.layoutParams as ViewGroup.MarginLayoutParams
                val layoutIcon = flightIcon.layoutParams as ViewGroup.MarginLayoutParams

                layoutIcon.topMargin = 12
                layoutParams.topMargin = 18

                flightRouteTextView.layoutParams = layoutParams

                // Set specific behavior for "Sign on" activity
                flightRouteTextView.text = entry.activity
                flightDataTextView.text = ""  // Clear any other activity details
                flightTimesTextView.text = ""

                iconMapping[entry.activity]?.let {
                    flightIcon.setImageResource(it)
                } ?: run {
                    // Fallback option if no matching icon is found
                    flightIcon.setImageResource(R.drawable.house) // Replace with your default icon
                }

                flightDataTextView.isGone = true



            } else if (entry.activity == "Sign on") {
                flightRouteTextView.text = "Sign On"
                flightDataTextView.text = ""
                flightTimesTextView.text = formatTime(entry.atd) // Only check-in time is shown
                flightIcon.setImageResource(R.drawable.clock) // Icon for Sign On

                flightDataTextView.isGone = true

                // Move the flightRouteTextView down by 8 pixels
                val layoutParams = flightRouteTextView.layoutParams as ViewGroup.MarginLayoutParams
                val layoutFlight = flightTimesTextView.layoutParams as ViewGroup.MarginLayoutParams
                val layoutIcon = flightIcon.layoutParams as ViewGroup.MarginLayoutParams

                layoutIcon.topMargin = 18
                layoutParams.topMargin = 20
                layoutFlight.topMargin = 15

            } else {
                // Handle other activities based on mapping
                val activityText = activityMapping[entry.activity] ?: entry.activity
                flightRouteTextView.text = "${entry.orig} - ${entry.dest}"
                flightDataTextView.text = activityText

                // Reset top margin for non-"Sign On" duties
                val layoutParams = flightRouteTextView.layoutParams as ViewGroup.MarginLayoutParams
                val layoutFlight = flightTimesTextView.layoutParams as ViewGroup.MarginLayoutParams
                val layoutIcon = flightIcon.layoutParams as ViewGroup.MarginLayoutParams

                layoutIcon.topMargin = 28
                layoutParams.topMargin = 0
                layoutFlight.topMargin = 14

                flightDataTextView.isGone = false

                Log.d("Cunt", "Sequence 3")

                // Handle duty timings (ata, atd)
                val atd = formatTime(entry.atd)
                val ata = formatTime(entry.ata)
                flightTimesTextView.text = "$atd - $ata"

                // Set the appropriate icon
                val iconRes = iconMapping[activityText] ?: R.drawable.plane // Default airplane icon
                flightIcon.setImageResource(iconRes)


            }

            /*if (specialActivities.contains(entry.activity)) {
                val activityText = activityMapping[entry.activity] ?: entry.activity
                flightRouteTextView.text = activityText // Adjust route for special activities
                flightTimesTextView.text = formatTime(entry.atd)

                // Optionally, adjust the icon for special activities
                flightIcon.setImageResource(iconMapping[activityText] ?: R.drawable.plane)
            }*/
        }
    }
}
