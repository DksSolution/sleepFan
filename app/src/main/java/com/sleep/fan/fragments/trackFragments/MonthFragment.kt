package com.sleep.fan.fragments.trackFragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.data.Session
import com.google.android.gms.fitness.request.SessionReadRequest
import com.google.android.gms.fitness.result.SessionReadResponse
import com.google.android.material.snackbar.Snackbar
import com.sleep.fan.R
import com.sleep.fan.TAG
import com.sleep.fan.databinding.FragmentDayMonthBinding
import com.sleep.fan.newdb.DateUtils
import com.sleep.fan.newdb.NewSleepDatabase
import com.sleep.fan.newdb.SleepDao
import com.sleep.fan.newdb.SleepDataGenerator
import com.sleep.fan.newdb.SleepEntity
import com.sleep.fan.utility.ProgressAnimator
import com.sleep.fan.utility.Utils
import com.sleep.fan.utility.Utils.Companion.convertTimeStringToHoursAndMinutes
import com.sleep.fan.utility.Utils.Companion.millisFromRfc339DateString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.ParseException
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit


class MonthFragment : Fragment() {
    private var binding: FragmentDayMonthBinding? = null
    private var progressAnimator: ProgressAnimator? = null
    private var chart: BarChart? = null
    var colorDeepSleep = Color.parseColor("#302e95")
    var colorCoreSleep = Color.parseColor("#0b7072")
    var colorRemSleep = Color.parseColor("#3577f7")
    var colorAwake = Color.parseColor("#ec6951")
    private var sleepDao: SleepDao? = null

    private var currentIndex = 0
    private var allSleepRecords: List<SleepEntity>? = null

    // Sign-In
    enum class FitActionRequestCode {
        INSERT_SLEEP_SESSIONS,
        READ_SLEEP_SESSIONS
    }

    val SLEEP_STAGES = arrayOf(
        "Unused",
        "Awake (during sleep)",
        "Sleep",
        "Out-of-bed",
        "Light sleep",
        "Deep sleep",
        "REM sleep"
    )

//    var PERIOD_START_DATE_TIME = "2024-06-01T12:00:00Z"
//    var PERIOD_END_DATE_TIME = "2024-06-25T12:00:00Z"

//    var PERIOD_START_DATE_TIME = "2024-06-01 12:00:00"
//    var PERIOD_END_DATE_TIME = "2024-06-25 12:00:00"

    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
        .build()

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    // Defines the start and end of the period of interest in this example.
//    private var periodStartMillis = millisFromRfc339DateString(com.sleep.fan.PERIOD_START_DATE_TIME)
//    private var periodEndMillis = millisFromRfc339DateString(com.sleep.fan.PERIOD_END_DATE_TIME)

    private var periodStartMillis : Long = 0
    private var periodEndMillis : Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_day_month,
            container,
            false
        )

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = NewSleepDatabase.getInstance(requireContext())
        sleepDao = db.sleepDao()

        chart = binding!!.chart1
        progressAnimator = ProgressAnimator(binding!!.progressForeground)
        progressAnimator!!.animateProgress(0) // Set your desired progress
        Handler().postDelayed({
            progressAnimator!!.animateProgress(80) // Set your desired progress after the delay
        }, 1000)

        setupChart()
//        checkPermissionsAndRun(com.sleep.fan.FitActionRequestCode.READ_SLEEP_SESSIONS)

        // Initialize date range to current month
        DateUtils.moveToCurrentMonth()
        updateDateRangeText()

        binding!!.ivPrev.setOnClickListener {
            DateUtils.showPreviousMonth()
            updateDateRangeText()
        }

        binding!!.ivNext.setOnClickListener {
            DateUtils.showNextMonth()
            updateDateRangeText()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun updateDateRangeText() {
        var strDate = DateUtils.updateDateRangeText() // StrDate: 12-Jun-2024 - 11-Jul-2024

        val (first, secnd) = Utils.seperateDateFromRange(strDate)

        binding!!.tvDate.text = Utils.formatCustomDateRange(strDate)

        val (startDate, endDate) = Utils.getMonthRangeInFormat(strDate) // Start date: 2024-06-12T12:00:00Z , End date: 2024-07-11T12:00:00Z

        Log.e("", "======> $strDate : $startDate , $endDate")
        binding!!.tvDate.text = strDate // Textview text: 12-Jun-2024 - 11-Jul-2024

//        retrieveMonthlyAverages(startDate, endDate)
        periodStartMillis = millisFromRfc339DateString(startDate)
        periodEndMillis = millisFromRfc339DateString(endDate)
        checkPermissionsAndRun(com.sleep.fan.FitActionRequestCode.READ_SLEEP_SESSIONS)

        GlobalScope.launch(Dispatchers.IO) {

            try {
                val sleepData = withContext(Dispatchers.IO) {
                    sleepDao!!.allSleepData
                }

                // Remove time part from awake_time field
                val modifiedSleepData = sleepData.map { sleep ->
                    sleep.awake_time = sleep.awake_time?.substringBefore(" ")
                    sleep
                }

                // Filter data based on date range
                val filteredSleepData = filterSleepDataByDateRange(modifiedSleepData, first, secnd)

                // Calculate and log averages
                withContext(Dispatchers.Main) {
                    calculateAndLogAverages(filteredSleepData)
                }

            } catch (e: Exception) {
                Log.e("Database Error", "Error querying database: ${e.message}")
            }
        }

    }

    fun filterSleepDataByDateRange(sleepData: List<SleepEntity>, startDate: String, endDate: String): List<SleepEntity> {
        val sdf = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        val start = sdf.parse(startDate)
        val end = sdf.parse(endDate)

        return sleepData.filter { sleep ->
            val awakeDate = sdf.parse(sleep.awake_time ?: "")
            awakeDate != null && awakeDate >= start && awakeDate <= end
        }
    }

    private fun calculateAndLogAverages(sleepData: List<SleepEntity>) {
        val totalSleepTimeMinutes = sleepData.map { parseTimeToMinutes(it.total_sleep_time) }
        val totalAwakeTimeMinutes = sleepData.map { parseMinutes(it.total_awake_time) }
        val totalRemTimeMinutes = sleepData.map { parseMinutes(it.total_rem_time) }
        val totalCoreTimeMinutes = sleepData.map { parseMinutes(it.total_core_time) }
        val totalDeepTimeMinutes = sleepData.map { parseMinutes(it.total_deep_time) }

        val avgTotalSleepTime = totalSleepTimeMinutes.average()
        val avgTotalAwakeTime = totalAwakeTimeMinutes.average()
        val avgTotalRemTime = totalRemTimeMinutes.average()
        val avgTotalCoreTime = totalCoreTimeMinutes.average()
        val avgTotalDeepTime = totalDeepTimeMinutes.average()

//        for (sleep in sleepData) {
//            Log.d("SleepData", "ID: ${sleep.id}, Sleep Time: ${sleep.sleep_time}, Awake Time: ${sleep.awake_time}, Total Sleep Time: ${sleep.total_sleep_time}, Total Awake Time: ${sleep.total_awake_time}, Total REM Time: ${sleep.total_rem_time}")
//        }
//        // Log detailed debug output
//        logDetailedOutput("Total Sleep Time", totalSleepTimeMinutes, avgTotalSleepTime)
//        logDetailedOutput("Total Awake Time", totalAwakeTimeMinutes, avgTotalAwakeTime)
//        logDetailedOutput("Total REM Time", totalRemTimeMinutes, avgTotalRemTime)

        binding!!.tvTimeInBedHr.text = if (avgTotalSleepTime.toInt() == 0) convertTimeStringToHoursAndMinutes("${avgTotalSleepTime.toInt()} minutes")
        else formatTime2(convertTimeStringToHoursAndMinutes("${avgTotalSleepTime.toInt()} minutes").toString())
        binding!!.tvTimeAsleepBedHr.text = convertTimeStringToHoursAndMinutes("${avgTotalSleepTime.toInt()} minutes")
        binding!!.tvProgressTimeASleep.text = convertTimeStringToHoursAndMinutes("${avgTotalSleepTime.toInt()} minutes")

        binding!!.tvAwakeHr.text = convertTimeStringToHoursAndMinutes("${avgTotalAwakeTime.toInt()} minutes")
        binding!!.tvRemHr.text = convertTimeStringToHoursAndMinutes("${avgTotalRemTime.toInt()} minutes")
        binding!!.tvCoreHr.text = convertTimeStringToHoursAndMinutes("${avgTotalCoreTime.toInt()} minutes")
        binding!!.tvDeepHr.text = convertTimeStringToHoursAndMinutes("${avgTotalDeepTime.toInt()} minutes")
    }

    private fun parseTimeToMinutes(time: String?): Int {
        if (time.isNullOrBlank()) return 0
        val parts = time.split(" ")
        val hours = parts[0].toIntOrNull() ?: 0
        val minutes = parts[2].toIntOrNull() ?: 0
        return hours * 60 + minutes
    }

    private fun parseMinutes(time: String?): Int {
        return time?.split(" ")?.get(0)?.toIntOrNull() ?: 0
    }

    fun formatTime2(timeString: String?): SpannableString {
        if (timeString.isNullOrEmpty()) {
            return SpannableString("0 hr 0 min")
        }

        // Regular expression to extract hours and minutes
        val regex = """(\d+)\s*(hours?|hr|minutes?|min)""".toRegex(RegexOption.IGNORE_CASE)

        // Find all matches in the input string
        val matches = regex.findAll(timeString)

        var hours = 0
        var minutes = 0

        // Iterate over each match and extract hours or minutes
        for (match in matches) {
            val (value, unit) = match.destructured
            when (unit.toLowerCase()) {
                "hours", "hour", "hr" -> hours += value.toInt()
                "minutes", "minute", "min" -> minutes += value.toInt()
            }
        }

        // Reduce minutes by 20
        val totalMinutes = hours * 60 + minutes - 20
        hours = totalMinutes / 60
        minutes = totalMinutes % 60

        // Construct the formatted string
        val formattedString = "$hours hr $minutes min"

        // Create a SpannableString for styling
        val spannableString = SpannableString(formattedString)

        // Styling hours and minutes numbers
        if (hours > 0) {
            spannableString.setSpan(
                RelativeSizeSpan(0.95f),
                0,
                hours.toString().length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                ForegroundColorSpan(Color.parseColor("#FFFFFF")),
                0,
                hours.toString().length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        spannableString.setSpan(
            RelativeSizeSpan(0.95f),
            formattedString.indexOf(minutes.toString()),
            formattedString.indexOf(minutes.toString()) + minutes.toString().length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            ForegroundColorSpan(Color.parseColor("#FFFFFF")),
            formattedString.indexOf(minutes.toString()),
            formattedString.indexOf(minutes.toString()) + minutes.toString().length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Styling text parts
        spannableString.setSpan(
            RelativeSizeSpan(0.65f),
            formattedString.indexOf("hr"),
            formattedString.indexOf("hr") + 2,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            ForegroundColorSpan(Color.parseColor("#767a80")),
            formattedString.indexOf("hr"),
            formattedString.indexOf("hr") + 2,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            RelativeSizeSpan(0.65f),
            formattedString.indexOf("min"),
            formattedString.indexOf("min") + 3,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            ForegroundColorSpan(Color.parseColor("#767a80")),
            formattedString.indexOf("min"),
            formattedString.indexOf("min") + 3,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannableString
    }
    // ************* end *****

    private fun checkPermissionsAndRun(fitActionRequestCode: com.sleep.fan.FitActionRequestCode) {
        if (permissionApproved()) {
            fitSignIn(fitActionRequestCode)
        } else {
            requestRuntimePermissions(fitActionRequestCode)
        }
    }

    private fun permissionApproved(): Boolean {
        return if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else {
            true
        }
    }

    private fun oAuthPermissionsApproved() =
        GoogleSignIn.hasPermissions(getGoogleAccount(), fitnessOptions)

    private fun getGoogleAccount() =
        GoogleSignIn.getAccountForExtension(requireActivity(), fitnessOptions)

    private fun fitSignIn(requestCode: com.sleep.fan.FitActionRequestCode) {
        if (oAuthPermissionsApproved()) {
            performActionForRequestCode(requestCode)
        } else {
            requestCode.let {
                GoogleSignIn.requestPermissions(
                    this,
                    it.ordinal,
                    getGoogleAccount(), fitnessOptions
                )
            }
        }
    }

    private fun performActionForRequestCode(requestCode: com.sleep.fan.FitActionRequestCode) =
        when (requestCode) {
            com.sleep.fan.FitActionRequestCode.INSERT_SLEEP_SESSIONS -> insertSleepSessions()
            com.sleep.fan.FitActionRequestCode.READ_SLEEP_SESSIONS -> readSleepSessions()
        }

    private fun insertSleepSessions() {

    }

    /**
     * Reads sleep sessions from the Fit API, including any sleep {@code DataSet}s.
     */
    private fun readSleepSessions() {
        val client = Fitness.getSessionsClient(requireActivity(), getGoogleAccount())

//        Log.e("=========>", "==> readSleepSessions: $periodStartMillis, $periodEndMillis")
        val sessionReadRequest = SessionReadRequest.Builder()
            .read(DataType.TYPE_SLEEP_SEGMENT)
            .read(DataType.TYPE_HEART_RATE_BPM)
            // By default, only activity sessions are included, not sleep sessions. Specifying
            // includeSleepSessions also sets the behaviour to *exclude* activity sessions.
            .includeSleepSessions()
            .readSessionsFromAllApps()
            .setTimeInterval(periodStartMillis, periodEndMillis, TimeUnit.MILLISECONDS)
            .build()

        client.readSession(sessionReadRequest)
            .addOnSuccessListener { dumpSleepSessions(it) }
            .addOnFailureListener {
                com.sleep.fan.common.logger.Log.e(
                    TAG,
                    "Unable to read sleep sessions",
                    it
                )
            }
    }

    private fun dumpSleepSessions(response: SessionReadResponse) {
        com.sleep.fan.common.logger.Log.clear()

        for (session in response.sessions) {
            dumpSleepSession(session, response.getDataSet(session))
        }
    }

    private fun dumpSleepSession(session: Session, dataSets: List<DataSet>) {
        dumpSleepSessionMetadata(session)
        dumpSleepDataSets(dataSets)
        dumpHeartRateDataSets(dataSets)
    }

    private fun dumpHeartRateDataSets(dataSets: List<DataSet>) {

        for (dataSet in dataSets) {
            if (dataSet.dataType == DataType.TYPE_HEART_RATE_BPM) {
                for (dataPoint in dataSet.dataPoints) {
                    val bpm = dataPoint.getValue(Field.FIELD_BPM).asFloat()
                    val timestamp = dataPoint.getTimestamp(TimeUnit.MILLISECONDS)
                    Log.e(TAG, "BPM: $bpm")
                    Log.e(TAG, "Heart rate: $bpm BPM at $timestamp")
                }
            }
        }
    }

    private fun dumpSleepDataSets(dataSets: List<DataSet>) {
        for (dataSet in dataSets) {
            for (dataPoint in dataSet.dataPoints) {
                val sleepStageOrdinal = dataPoint.getValue(Field.FIELD_SLEEP_SEGMENT_TYPE).asInt()
                val sleepStage = com.sleep.fan.SLEEP_STAGES[sleepStageOrdinal]

                val durationMillis =
                    dataPoint.getEndTime(TimeUnit.MILLISECONDS) - dataPoint.getStartTime(TimeUnit.MILLISECONDS)
                val duration = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
                Log.e(TAG, "--->\t$sleepStage: $duration (mins)")
            }
        }
    }

    private fun dumpSleepSessionMetadata(session: Session) {
        val (startDateTime, endDateTime) = getSessionStartAndEnd(session)
        val totalSleepForNight = calculateSessionDuration(session)
        Log.e("Weekly", "===> Weekly: $startDateTime to $endDateTime ($totalSleepForNight mins)")

        lifecycleScope.launch {
            SleepDataGenerator.generateAndInsertSleepData(
                requireContext(),
                startDateTime.trim(),
                endDateTime.trim(),
                Utils.formatTime(totalSleepForNight.toString())
            );
        }

    }

    private fun getSessionStartAndEnd(session: Session): Pair<String, String> {
        val dateFormat = DateFormat.getDateTimeInstance()
        val startDateTime = dateFormat.format(session.getStartTime(TimeUnit.MILLISECONDS))
        val endDateTime = dateFormat.format(session.getEndTime(TimeUnit.MILLISECONDS))
        return startDateTime to endDateTime
    }

    private fun calculateSessionDuration(session: Session): Long {
        val total =
            session.getEndTime(TimeUnit.MILLISECONDS) - session.getStartTime(TimeUnit.MILLISECONDS)
        return TimeUnit.MILLISECONDS.toMinutes(total)
    }

    private fun requestRuntimePermissions(requestCode: com.sleep.fan.FitActionRequestCode) {
        val shouldProvideRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACTIVITY_RECOGNITION
            )

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        requestCode.let {
            if (shouldProvideRationale) {
                com.sleep.fan.common.logger.Log.e(
                    TAG,
                    "Displaying permission rationale to provide additional context."
                )
                Snackbar.make(
                    binding!!.mainActivityView,
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.ok) {
                        // Request permission
                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                            requestCode.ordinal
                        )
                    }
                    .show()
            } else {
                com.sleep.fan.common.logger.Log.e(TAG, "Requesting permission")
                // Request permission. It's possible this can be auto answered if device policy
                // sets the permission in a given state or the user denied the permission
                // previously and checked "Never ask again".
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    requestCode.ordinal
                )
            }
        }
    }

//    private fun setDateRange() {
//        if (!TextUtils.isEmpty(binding!!.tvDate.text.toString())) {
//            var strDate = binding!!.tvDate.text.toString()
//            val (startDate, endDate) = Utils.formatDateRange(strDate)
//            retrieveMonthlyAverages(startDate, endDate)
//            Log.e("-->MonthlyAverage", "Start date: $startDate, End date: $endDate")
//            binding!!.tvDate.text = Utils.formatCustomDateRange(binding!!.tvDate.text.toString())
//        }
//    }
//
//    private fun retrieveMonthlyAverages(startDate: String, endDate: String) {
//        CoroutineScope(Dispatchers.IO).launch {
//            val monthlyAverages = sleepDao!!.getMonthWiseAveragesWithinDateRange(startDate, endDate)
//            withContext(Dispatchers.Main) {
//                // Update your UI with the monthly averages here
//                for (average in monthlyAverages) {
//                    Log.e("-->MonthlyAverage", "Month: ${average.month ?: "N/A"}, Avg Sleep: ${average.avg_total_sleep_time ?: 0.0}, Avg Awake: ${average.avg_total_awake_time ?: 0.0}, Avg REM: ${average.avg_total_rem_time ?: 0.0}")
//                }
//            }
//        }
//    }

    private fun setupChart() {
        val entries: MutableList<BarEntry> = ArrayList()

        // Awake intervals
        entries.add(BarEntry(0.167f, 0f)) // 12:10 AM to 12:30 AM
        entries.add(BarEntry(2.333f, 0f)) // 2:20 AM to 2:30 AM
        entries.add(BarEntry(4.167f, 0f)) // 4:10 AM to 4:20 AM
        entries.add(BarEntry(5.667f, 0f)) // 5:40 AM to 5:50 AM
        entries.add(BarEntry(6.5f, 0f)) // 6:30 AM to 6:35 AM

        // REM intervals
        entries.add(BarEntry(2f, 1f)) // 2:00 AM to 2:20 AM
        entries.add(BarEntry(3.75f, 1f)) // 3:45 AM to 4:10 AM
        entries.add(BarEntry(4.75f, 1f)) // 4:45 AM to 5:10 AM
        entries.add(BarEntry(5.417f, 1f)) // 5:25 AM to 5:40 AM
        entries.add(BarEntry(5.833f, 1f)) // 5:50 AM to 6:05 AM
        entries.add(BarEntry(6.333f, 1f)) // 6:20 AM to 6:30 AM

        // Core intervals
        entries.add(BarEntry(23.767f, 2f)) // 11:46 PM to 12:00 AM
        entries.add(BarEntry(24f, 2f)) // 12:00 AM to 12:10 AM
        entries.add(BarEntry(24.5f, 2f)) // 12:30 AM to 1:00 AM
        entries.add(BarEntry(25.75f, 2f)) // 1:45 AM to 2:00 AM
        entries.add(BarEntry(27f, 2f)) // 3:00 AM to 3:45 AM
        entries.add(BarEntry(29.167f, 2f)) // 5:10 AM to 5:25 AM
        entries.add(BarEntry(30.083f, 2f)) // 6:05 AM to 6:20 AM
        //
//        // Deep intervals
        entries.add(BarEntry(25f, 3f)) // 1:00 AM to 1:45 AM
        entries.add(BarEntry(26.5f, 3f)) // 2:30 AM to 3:00 AM
        entries.add(BarEntry(28.333f, 3f)) // 4:20 AM to 4:45 AM

//        entries.add(new BarEntry(23.0f , 23.25f,0));
//        entries.add(new BarEntry(1.5f  , 1.75f ,0));
//        entries.add(new BarEntry(3.167f, 3.333f,1));
//        entries.add(new BarEntry(4.667f, 4.833f,1));
//        entries.add(new BarEntry(5.833f, 6.0f  ,2));
//        entries.add(new BarEntry(0.167f, 0.5f  ,2));
//        entries.add(new BarEntry(2.0f  , 2.333f,3));
//        entries.add(new BarEntry(3.5f  , 3.833f,3));
        val dataSet = BarDataSet(entries, "Sleep Stages")
        dataSet.setColors(
            *intArrayOf(
                Color.parseColor("#ec6951"), Color.parseColor("#3577f7"),
                Color.parseColor("#0b7072"), Color.parseColor("#302e95")
            )
        )
        dataSet.setDrawValues(false)
        val barData = BarData(dataSet)
        barData.barWidth = 0.05f // Adjust the bar width if necessary
        chart!!.data = barData
        chart!!.setFitBars(true)
        chart!!.invalidate()

        // Configure X-Axis (Time)
        val xAxis = chart!!.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f // Adjust granularity as needed
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                var hour = value.toInt()
                val minute = ((value - hour) * 60).toInt()
                var period = "AM"
                if (hour >= 24) {
                    hour -= 24
                }
                if (hour >= 12) {
                    period = "PM"
                    if (hour > 12) hour -= 12
                }
                if (hour == 0) hour = 12
                return String.format("%d:%02d %s", hour, minute, period)
            }
        }

        // Configure Y-Axis (Sleep Stages)
        val yAxis = chart!!.axisLeft
        yAxis.granularity = 1f
        yAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return when (value.toInt()) {
                    0 -> "Awake"
                    1 -> "REM Sleep"
                    2 -> "Core Sleep"
                    3 -> "Deep Sleep"
                    else -> ""
                }
            }
        }
        chart!!.axisRight.isEnabled = false // Disable right Y-axis
    }

    private inner class Event internal constructor(
        var category: Int,
        var startTime: Float,
        var duration: Float,
    )

    private fun addEntries(
        entries: MutableList<BarEntry>,
        duration: String,
        stageIndex: Int,
        dateFormat: SimpleDateFormat,
    ) {
        val times = duration.split(" to ".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        try {
            val start = dateFormat.parse(times[0])
            val end = dateFormat.parse(times[1])
            val startHour = start.hours + start.minutes / 60f
            val endHour = end.hours + end.minutes / 60f
            val midPoint = (startHour + endHour) / 2
            entries.add(BarEntry(midPoint, stageIndex.toFloat()))
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

}