package com.example.jroster

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import com.zires.switchsegmentedcontrol.ZiresSwitchSegmentedControl
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class RosterAdapter(
    private val sortedDates: List<String>,
    private val entriesByDate: Map<String, List<DbData>>,
    private val extAirports: extAirports,
    var useHomeTime: Boolean,
    private val userBase: String
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
            val date = getDateForPosition(position)
            // Retrieve the first entry's ATD for that date
            val firstEntry = entriesByDate[date]?.firstOrNull()
            firstEntry?.let {
                holder.bind(
                    it.atd, // Use the ATD from the first entry for that date
                    extAirports, // Pass the extAirports instance
                    useHomeTime, // Pass whether to use home time
                    userBase, // Pass the user's base
                    it.orig // Pass the origin IATA code
                )
            }
        } else if (holder is RosterEntryViewHolder) {
            val entry = getEntryForPosition(position)
            entry?.let {
                holder.bind(it, extAirports, ::formatTime, useHomeTime, userBase)
            }
        }
    }

    // Function to format the time (hh:mm)
    fun formatTime(dateTime: String?): String {
        if (dateTime == null || dateTime.isEmpty()) return "N/A"

        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        inputFormat.timeZone =  TimeZone.getTimeZone("UTC")
        outputFormat.timeZone = TimeZone.getTimeZone("UTC")

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

    // Handle date display on the Table
    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.rosterDate)

        // Modify the bind function to properly format and display the date based on ATD
        fun bind(atd: String?, extAirports: extAirports, useHomeTime: Boolean, userBase: String, origIata: String) {
            // Parse the ATD string from MySQL
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC") // Assuming ATD is stored in UTC in MySQL
            }
            val atdDate: Date? = atd?.let { dateFormat.parse(it) }

            //  Determine the time zone (either home time or origin airport time zone)
            val origAirport = extAirports.airports.find { it.iata == origIata }
            val timeZoneForATD = if (useHomeTime) {
                TimeZone.getTimeZone("Australia/Melbourne")
            } else {
                TimeZone.getTimeZone(origAirport?.timeZone ?: "UTC")
            }

            // Convert ATD to local time
            val localDateFormat = SimpleDateFormat("EEE dd MMM yyyy", Locale.getDefault()).apply {
                timeZone = timeZoneForATD
            }
            val localDate = atdDate?.let { localDateFormat.format(it) } ?: "Invalid Date"

            // Get today's date in local time for comparison
            val today = Calendar.getInstance(timeZoneForATD).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            // Check if the local ATD is today or in the past
            val isPastDate = atdDate?.before(today) == true

            // Set text color based on whether the date is in the past
            dateTextView.setTextColor(if (isPastDate) Color.GRAY else Color.parseColor("#3F51B5")) // Indigo for future/today, grey for past

            // Check if the local ATD is today and adjust the display text accordingly
            val finalDateText = if (atdDate != null && atdDate.time >= today.time && atdDate.time < today.time + 24 * 60 * 60 * 1000) {
                "$localDate (Today)"
            } else {
                localDate
            }

            // Set the formatted local date text to the TextView
            dateTextView.text = finalDateText
        }
    }



    // Handle Duty display on the table
    class RosterEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val flightIcon: ImageView = itemView.findViewById(R.id.flightIcon)
        private val flightRouteTextView: TextView = itemView.findViewById(R.id.flightRoute)
        private val flightDataTextView: TextView = itemView.findViewById(R.id.flightData)
        private val flightTimesTextView: TextView = itemView.findViewById(R.id.flightTimes)

        @SuppressLint("SetTextI18n")
        fun bind(entry: DbData, extAirports: extAirports, formatTime: (String?) -> String, useHomeTime: Boolean, userBase: String) {
            // Special mapping of activities for customization

            val activityMapping = mapOf(
                "PCK" to "Pickup",
                "OFF" to "Day Off",
                "STB" to "Standby",
                "DBF" to "Debrief",
                "FTG" to "Fatigue",
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
                "DBF" to R.drawable.swords,
                "Admin" to R.drawable.smiley,
                "FTG" to R.drawable.fatigue,
                "XSB" to R.drawable.phone_callback_24dp_5f6368_fill0_wght400_grad0_opsz24
            )

            // Duty Mapping
            val dutyMapping = mapOf(
                "P" to "(Paxing)",
                "D" to "(Deadhead)",
                "" to " "
            )

            // Home base mapping
            val baseMapping = listOf(
                "Sydney" to "Australia/Sydney",
                "Melbourne" to "Australia/Melbourne",
                "Brisbane" to "Australia/Brisbane",
                "Cairns" to "Australia/Brisbane",
                "Gold Coast" to "Australia/Brisbane",
                "Adelaide" to "Australia/Adelaide",
                "Perth" to "Australia/Perth",
                "Avalon" to "Australia/Melbourne",
                "SEQ" to "Australia/Brisbane",
                "" to "Australia/Melbourne"
            )

            val iataMapping = listOf(
                "Sydney" to "SYD",
                "Melbourne" to "MEL",
                "Brisbane" to "BNE",
                "Cairns" to "CNS",
                "Gold Coast" to "OOL",
                "Adelaide" to "ADL",
                "Perth" to "PER",
                "Avalon" to "AVV",
                "SEQ" to "SEQ",
                "" to "MEL"
            )

            // Base map
            val baseMap = baseMapping.toMap()
            val iataMap = iataMapping.toMap()

            // Paxing?
            val dutyDesignator = dutyMapping[entry.dd]

            // Get the origin and destination time zones using the airport IATA codes
            val origAirport = extAirports.airports.find { it.iata == entry.orig }
            val destAirport = extAirports.airports.find { it.iata == entry.dest }

            // Define the expected date format
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")

            // Decide which time zone to use based on switch state
            val timeZoneForATD = if (useHomeTime) {
                TimeZone.getTimeZone(baseMap[userBase] ?: "Australia/Melbourne")
            } else {
                origAirport?.timeZone?.let { TimeZone.getTimeZone(it) } ?: TimeZone.getDefault()
            }

            val timeZoneForATA = if (useHomeTime) {
                TimeZone.getTimeZone(baseMap[userBase] ?: "Australia/Melbourne")
            } else {
                destAirport?.timeZone?.let { TimeZone.getTimeZone(it) } ?: TimeZone.getDefault()
            }

            // Get today's date in the same time zone
            val today = Calendar.getInstance(timeZoneForATD).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            // Return the date
            val atdDate: Date? = entry.atd?.let { dateFormat.parse(it) }

            // Check if the ATD is in the past
            val isPastDate = atdDate?.before(today) ?: false

            // Set the text color based on whether the duty is in the past
            val textColor = if (isPastDate) Color.GRAY else Color.BLACK
            flightRouteTextView.setTextColor(textColor)
            flightDataTextView.setTextColor(textColor)
            flightTimesTextView.setTextColor(textColor)

            // Convert ATD to selected time zone (local or Sydney)
            val localAtd = if (entry.atd != null && origAirport != null) {
                try {
                    entry.atd?.let {
                        dateFormat.parse(it)?.let { parsedDate ->
                            extAirports.convertToLocalTime(parsedDate, timeZoneForATD.id)
                        }
                    } ?: "N/A"
                } catch (e: Exception) {
                    Log.e("RosterAdapter", "Error parsing ATD: ${entry.atd}", e)
                    "N/A"
                }
            } else {
                "N/A"
            }

            // Convert ATA to selected time zone (local or Sydney)
            val localAta = if (entry.ata != null && destAirport != null) {
                try {
                    entry.ata?.let {
                        dateFormat.parse(it)?.let { parsedDate ->
                            extAirports.convertToLocalTime(parsedDate, timeZoneForATA.id)
                        }
                    } ?: "N/A"
                } catch (e: Exception) {
                    Log.e("RosterAdapter", "Error parsing ATA: ${entry.ata}", e)
                    "N/A"
                }
            } else {
                "N/A"
            }

            // Format the Roster Duties
            if (listOf("OFF", "LVE", "UFD", "DFD", "AOF", "XSB", "FDO", "FTG", "STR").contains(entry.activity)) {

                // Move flightRouteTextView down by 8 pixels
                val layoutParams = flightRouteTextView.layoutParams as ViewGroup.MarginLayoutParams
                val layoutIcon = flightIcon.layoutParams as ViewGroup.MarginLayoutParams

                layoutIcon.topMargin = 12
                layoutParams.topMargin = 18

                flightRouteTextView.layoutParams = layoutParams

                // Set specific behavior for "Sign on" activity
                flightRouteTextView.text = activityMapping[entry.activity]
                flightDataTextView.text = ""  // Clear any other activity details
                flightTimesTextView.text = ""

                iconMapping[entry.activity]?.let {
                    flightIcon.setImageResource(it)
                } ?: run {
                    // Fallback option if no matching icon is found
                    flightIcon.setImageResource(R.drawable.house) // Replace with your default icon
                }

                flightDataTextView.isGone = true

            // ------------- HANDLE DUTIES REQUIRE TIMES BUT ONLY ONE LINE ---------------------
            } else if (listOf("STR", "STB", "DBF", "ADM").contains(entry.activity)) {

                flightRouteTextView.text = activityMapping[entry.activity]
                flightDataTextView.text = ""

                // Handle duty timings (ata, atd)
                flightTimesTextView.text = "$localAtd - $localAta"

                iconMapping[entry.activity]?.let {
                    flightIcon.setImageResource(it)
                } ?: run {
                    // Fallback option if no matching icon is found
                    flightIcon.setImageResource(R.drawable.house) // Replace with your default icon
                }

                flightDataTextView.isGone = true

                // Move the flightRouteTextView down by 8 pixels
                val layoutParams = flightRouteTextView.layoutParams as ViewGroup.MarginLayoutParams
                val layoutFlight = flightTimesTextView.layoutParams as ViewGroup.MarginLayoutParams
                val layoutIcon = flightIcon.layoutParams as ViewGroup.MarginLayoutParams

                layoutIcon.topMargin = 18
                layoutParams.topMargin = 20
                layoutFlight.topMargin = 15

            // ------------- HANDLE PICKUP AND TRANSPORT DUTIES -------------------------
            } else if (listOf("PCK", "HTC","A2B", "A2H", "A2S", "A3S", "ATC", "B2A", "B2T", "G2H", "G2M", "H22", "H2A", "H2G",
                    "H2J", "H2Q", "H2S", "H2T", "H2W", "H3S", "J2H", "J2S", "M22", "M2G", "M2W", "Q2H",
                    "Q2S", "S2A", "S2H", "S2J", "S2Q", "S3A", "S3H", "SAK", "T2B", "T2H", "TCA", "TCH",
                    "W2H", "W2M").contains(entry.activity)) {

                var timeText = ""
                val localText = iataMap[userBase] ?: userBase

                // Correctly access the map for `userBase`
                if (!useHomeTime)  {
                    timeText = " "+entry.orig
                } else {
                    timeText = " "+localText
                }

                flightRouteTextView.text = activityMapping[entry.activity]
                flightDataTextView.text = ""
                flightTimesTextView.text = localAtd+timeText // Only check-in time is shown

                // Set the appropriate icon
                val iconRes = R.drawable.bus // Default airplane icon

                flightIcon.setImageResource(iconRes)

                flightDataTextView.isGone = true

                // Move the flightRouteTextView down by 8 pixels
                val layoutParams = flightRouteTextView.layoutParams as ViewGroup.MarginLayoutParams
                val layoutFlight = flightTimesTextView.layoutParams as ViewGroup.MarginLayoutParams
                val layoutIcon = flightIcon.layoutParams as ViewGroup.MarginLayoutParams

                layoutIcon.topMargin = 18
                layoutParams.topMargin = 20
                layoutFlight.topMargin = 15

            // ------------- HANDLE SIM DUTIES -------------------------
            } else if (listOf("SIC", "SIA", "SB2", "CVA", "A06", "SD7", "L03", "L02", "L01", "A01", "A02", "A04",
                "A05", "A07", "A08", "A09", "AAS", "AB1", "AB2", "AB3", "AB4", "AB5", "AB6", "AB7",
                "AB8", "AB9", "AF1", "AF2", "AF3", "AF4", "AF5", "AF6", "AF7", "AF8", "AF9", "AFL",
                "AT1", "AT2", "AT3", "AT4", "B02", "B05", "B06", "B07", "BAS", "BCL", "BF1", "BF2",
                "BF3", "BF4", "BF5", "BF6", "BF7", "BF8", "BFL", "BST", "BT1", "BT2", "BT3", "BT4",
                "CMC", "CMM", "CMP", "CPS", "CR2", "CT3", "CTC", "FF5", "FF9", "FFP", "FFS", "FSE",
                "ILC", "IP1", "ITM", "LOT", "MPS", "PCS", "RIS", "RNV", "RTE", "RTS", "SD2", "SD3",
                "SI1", "SI2", "SI3", "SI4", "SID", "SIT", "SR2", "SR3", "SR7", "SRV", "ST2", "ST3",
                "ST7", "TRT", "ZRS", "ZRI", "ZNI", "STP", "SIM", "SB7", "ALV", "A03").contains(entry.activity)) {

                // Handle other activities based on mapping
                val activityText = activityMapping[entry.activity] ?: entry.activity
                flightRouteTextView.text = "Sim"
                flightDataTextView.text = activityText

                // Reset top margin for non-"Sign On" duties
                val layoutParams = flightRouteTextView.layoutParams as ViewGroup.MarginLayoutParams
                val layoutFlight = flightTimesTextView.layoutParams as ViewGroup.MarginLayoutParams
                val layoutIcon = flightIcon.layoutParams as ViewGroup.MarginLayoutParams

                layoutIcon.topMargin = 28
                layoutParams.topMargin = 0
                layoutFlight.topMargin = 14

                flightDataTextView.isGone = false

                // Handle duty timings (ata, atd)
                flightTimesTextView.text = "$localAtd - $localAta"

                // Set the appropriate icon
                val iconRes = iconMapping[activityText] ?: R.drawable.sim2 // Default airplane icon
                flightIcon.setImageResource(iconRes)


            // -------------- HANDLE SIGN ON ONLY --------------------------------------
            } else if (entry.activity == "Sign on") {

                var timeText = ""
                val localText = iataMap[userBase] ?: userBase

                // Correctly access the map for `userBase`
                if (!useHomeTime)  {
                    timeText = " "+entry.orig
                } else {
                    timeText = " "+localText
                }

                flightRouteTextView.text = "Sign On"
                flightDataTextView.text = ""
                flightTimesTextView.text = localAtd+timeText // Only check-in time is shown
                flightIcon.setImageResource(R.drawable.clock) // Icon for Sign On

                flightDataTextView.isGone = true

                // Move the flightRouteTextView down by 8 pixels
                val layoutParams = flightRouteTextView.layoutParams as ViewGroup.MarginLayoutParams
                val layoutFlight = flightTimesTextView.layoutParams as ViewGroup.MarginLayoutParams
                val layoutIcon = flightIcon.layoutParams as ViewGroup.MarginLayoutParams

                layoutIcon.topMargin = 18
                layoutParams.topMargin = 20
                layoutFlight.topMargin = 15


            // ------------------- ALL OTHER DUTIES -------------------------------------
            } else {
                // Handle other activities based on mapping
                val activityText = activityMapping[entry.activity] ?: entry.activity
                val extraString = "- ${dutyDesignator}"

                var timeText = ""

                if (!useHomeTime)  {
                    timeText = "L"
                }

                flightRouteTextView.text = "${entry.orig} - ${entry.dest}"

                // Reset top margin for non-"Sign On" duties
                val layoutParams = flightRouteTextView.layoutParams as ViewGroup.MarginLayoutParams
                val layoutFlight = flightTimesTextView.layoutParams as ViewGroup.MarginLayoutParams
                val layoutIcon = flightIcon.layoutParams as ViewGroup.MarginLayoutParams

                layoutIcon.topMargin = 28
                layoutParams.topMargin = 0
                layoutFlight.topMargin = 8

                flightDataTextView.isGone = false

                // Handle duty timings (ata, atd)
                val atd = formatTime(entry.atd)
                val ata = formatTime(entry.ata)
                flightTimesTextView.text = "$localAtd$timeText - $localAta$timeText"

                // Set the appropriate icon
                if (dutyDesignator == "(Paxing)" || dutyDesignator == "(Deadhead)") {
                    flightIcon.setImageResource(R.drawable.paxing)
                    flightDataTextView.text = "${activityText} ${extraString}"
                } else {
                    flightIcon.setImageResource(R.drawable.plane)
                    flightDataTextView.text = activityText
                }

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
