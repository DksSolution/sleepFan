package com.sleep.fan.fragments.trackFragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.DashPathEffect
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.data.Session
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.SessionReadRequest
import com.google.android.gms.fitness.result.SessionReadResponse
import com.google.android.material.snackbar.Snackbar
import com.sleep.fan.R
import com.sleep.fan.TAG
import com.sleep.fan.databinding.FragmentDayWeekBinding
import com.sleep.fan.newdb.NewSleepDatabase
import com.sleep.fan.newdb.SleepDao
import com.sleep.fan.newdb.SleepData
import com.sleep.fan.newdb.SleepDataGenerator
import com.sleep.fan.newdb.SleepEntity
import com.sleep.fan.utility.ProgressAnimator
import com.sleep.fan.utility.Utils
import com.sleep.fan.utility.Utils.Companion.formatCustomDateRange
import com.sleep.fan.utility.Utils.Companion.getDayName
import com.sleep.fan.utility.Utils.Companion.millisFromRfc339DateString
import com.sleep.fan.utility.Utils.Companion.moveToCurrentWeek
import com.sleep.fan.utility.Utils.Companion.showNextWeek
import com.sleep.fan.utility.Utils.Companion.showPreviousWeek
import com.sleep.fan.utility.Utils.Companion.updateDateRangeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit


class WeekFragment : Fragment() {
    private var binding: FragmentDayWeekBinding? = null
    private var progressAnimator: ProgressAnimator? = null
    private var chart: BarChart? = null
    var colorDeepSleep = Color.parseColor("#302e95")
    var colorCoreSleep = Color.parseColor("#0b7072")
    var colorRemSleep = Color.parseColor("#3577f7")
    var colorAwake = Color.parseColor("#ec6951")

    private lateinit var sleepDao: SleepDao

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

    var PERIOD_START_DATE_TIME = ""
    var PERIOD_END_DATE_TIME = ""

    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
        .build()

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    // Defines the start and end of the period of interest in this example.
    var periodStartMillis = millisFromRfc339DateString(com.sleep.fan.PERIOD_START_DATE_TIME)
    var periodEndMillis = millisFromRfc339DateString(com.sleep.fan.PERIOD_END_DATE_TIME)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_day_week,
            container,
            false
        )
        val db = NewSleepDatabase.getInstance(requireContext())
        sleepDao = db.sleepDao()

        return binding?.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chart = binding!!.chart1
        progressAnimator = ProgressAnimator(binding!!.progressForeground)
        progressAnimator!!.animateProgress(0) // Set your desired progress
        Handler().postDelayed({
            progressAnimator!!.animateProgress(80) // Set your desired progress after the delay
        }, 1000)

//        setupChart()

        showRecords()

//        accessGoogleFit()

        // Initialize with the current week range
        moveToCurrentWeek()
        binding!!.tvDate.text = updateDateRangeText()
        setDateRange()

//        checkPermissionsAndRun(com.sleep.fan.FitActionRequestCode.READ_SLEEP_SESSIONS)

        binding!!.ivPrev.setOnClickListener {
//            checkPermissionsAndRun(com.sleep.fan.FitActionRequestCode.READ_SLEEP_SESSIONS)
            showPreviousWeek()
            binding!!.tvDate.text = updateDateRangeText()
            setDateRange()
        }

        binding!!.ivNext.setOnClickListener {
//            checkPermissionsAndRun(com.sleep.fan.FitActionRequestCode.READ_SLEEP_SESSIONS)
            showNextWeek()
            binding!!.tvDate.text = updateDateRangeText()
            setDateRange()
        }

        binding!!.tvProgressPercent.setOnClickListener {
            checkPermissionsAndRun(com.sleep.fan.FitActionRequestCode.READ_SLEEP_SESSIONS)
        }
    }

//    fun setDateRange() {
//        if (!TextUtils.isEmpty(binding!!.tvDate.text.toString())) {
//            var strDate = binding!!.tvDate.text.toString() // 07-Jul-2024 - 13-Jul-2024
//            val (startDate, endDate) = formatDateRange(strDate) // Start date: 2024-07-06T12:00:00Z , End date: 2024-07-13T12:00:00Z
//            periodStartMillis = millisFromRfc339DateString(startDate.replace("\\s+".toRegex(), "")) // Start millie: 1720247400000
//            periodEndMillis = millisFromRfc339DateString(endDate.replace("\\s+".toRegex(), ""))  //  End millie: 1720852200000
//
//            Log.e("", "==> 1. $strDate : $startDate , $endDate")
////            Log.e("", "==> 2. $strDate : $periodStartMillis , $periodEndMillis")
////            periodStartMillis = 1718173800000
////            periodEndMillis = 1720679400000
//
//            binding!!.tvDate.text = formatCustomDateRange(binding!!.tvDate.text.toString()) // Textview text: Jul 07 - 13, 2024
//            retrieveSleepDataWithinDateRange(startDate.replace("\\s+".toRegex(), ""), endDate.replace("\\s+".toRegex(), ""))
//        }
//    }

    // New Approach
    var filteredSleepData: List<SleepEntity>? = null
    var newListData: List<WeeklySleepData>? = null

    @RequiresApi(Build.VERSION_CODES.O)
    fun setDateRange() {
        if (!TextUtils.isEmpty(binding!!.tvDate.text.toString())) {
            var strDate = binding!!.tvDate.text.toString() // 07-Jul-2024 - 13-Jul-2024
            binding!!.tvDate.text =
                formatCustomDateRange(binding!!.tvDate.text.toString()) // Textview text: Jul 07 - 13, 2024

            val (first, secnd) = Utils.seperateDateFromRange(strDate)

            val (startDate, endDate) = Utils.getMonthRangeInFormat(strDate) // Start date: 2024-06-12T12:00:00Z , End date: 2024-07-11T12:00:00Z
            periodStartMillis = millisFromRfc339DateString(startDate)
            periodEndMillis = millisFromRfc339DateString(endDate)

            GlobalScope.launch(Dispatchers.IO) {

                try {
                    val sleepData = withContext(Dispatchers.IO) {
                        sleepDao.allSleepData
                    }

                    // Remove time part from awake_time field
                    val modifiedSleepData = sleepData.map { sleep ->
                        sleep.awake_time = sleep.awake_time?.substringBefore(" ")
                        sleep
                    }

                    // Filter data based on date range
                    filteredSleepData = filterSleepDataByDateRange(modifiedSleepData, first, secnd)

//                    for (sleepEntity in filteredSleepData) { // testing purpose
//                        Log.e("SleepData", "##### >> ${sleepEntity.toString()}")
//                    }

                    // Calculate and log averages
                    withContext(Dispatchers.Main) {
                        calculateAndLogAverages(filteredSleepData!!)
                        // *******************************************
                    }

                } catch (e: Exception) {
                    Log.e("Database Error", "Error querying database: ${e.message}")
                }

                newListData = processSleepData(filteredSleepData!!)
                Log.e("", "=====> NewList Data: ${newListData!!.size}")
                withContext(Dispatchers.Main) {
                    setupChart(newListData!!)
                }
            }
        }
    }

    fun filterSleepDataByDateRange(
        sleepData: List<SleepEntity>,
        startDate: String,
        endDate: String,
    ): List<SleepEntity> {
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


        binding!!.tvTimeInBedHr.text =
            if (avgTotalSleepTime.toInt() == 0) Utils.convertTimeStringToHoursAndMinutes(
                "${avgTotalSleepTime.toInt()} minutes"
            )
            else formatTime2(
                Utils.convertTimeStringToHoursAndMinutes("${avgTotalSleepTime.toInt()} minutes")
                    .toString()
            )
        binding!!.tvTimeAsleepBedHr.text =
            Utils.convertTimeStringToHoursAndMinutes("${avgTotalSleepTime.toInt()} minutes")
        binding!!.tvProgressTimeASleep.text =
            Utils.convertTimeStringToHoursAndMinutes("${avgTotalSleepTime.toInt()} minutes")

        binding!!.tvAwakeHr.text =
            Utils.convertTimeStringToHoursAndMinutes("${avgTotalAwakeTime.toInt()} minutes")
        binding!!.tvRemHr.text =
            Utils.convertTimeStringToHoursAndMinutes("${avgTotalRemTime.toInt()} minutes")
        binding!!.tvCoreHr.text =
            Utils.convertTimeStringToHoursAndMinutes("${avgTotalCoreTime.toInt()} minutes")
        binding!!.tvDeepHr.text =
            Utils.convertTimeStringToHoursAndMinutes("${avgTotalDeepTime.toInt()} minutes")
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
    // end new approach

    private fun checkPermissionsAndRun(fitActionRequestCode: com.sleep.fan.FitActionRequestCode) {
        if (permissionApproved()) {
            fitSignIn(fitActionRequestCode)
        } else {
            requestRuntimePermissions(fitActionRequestCode)
        }
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

    private fun oAuthPermissionsApproved() =
        GoogleSignIn.hasPermissions(getGoogleAccount(), fitnessOptions)

    private fun getGoogleAccount() =
        GoogleSignIn.getAccountForExtension(requireActivity(), fitnessOptions)

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

    private fun calculateSessionDuration(session: Session): Long {
        val total =
            session.getEndTime(TimeUnit.MILLISECONDS) - session.getStartTime(TimeUnit.MILLISECONDS)
        return TimeUnit.MILLISECONDS.toMinutes(total)
    }

    private fun getSessionStartAndEnd(session: Session): Pair<String, String> {
        val dateFormat = DateFormat.getDateTimeInstance()
        val startDateTime = dateFormat.format(session.getStartTime(TimeUnit.MILLISECONDS))
        val endDateTime = dateFormat.format(session.getEndTime(TimeUnit.MILLISECONDS))
        return startDateTime to endDateTime
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

    // ***********************
    private fun accessGoogleFit() {
        // Fetch sleep data from Google Fit
        lifecycleScope.launch {
            val sleepData = fetchSleepData()
            Log.e("", "=================> Data: " + sleepData.toString())
//            Toast.makeText(requireActivity(), sleepData.toString(), Toast.LENGTH_LONG).show()
            // Save data to local database
//            sleepDataRepository.insertSleepData(sleepData)
            // Update UI with the fetched data
//            updateUI(sleepData)
        }
    }

    private suspend fun fetchSleepData(): List<SleepData> {
        return withContext(Dispatchers.IO) {
            val account = GoogleSignIn.getLastSignedInAccount(requireActivity())
                ?: return@withContext emptyList<SleepData>()
            val response = Fitness.getHistoryClient(requireActivity(), account)
                .readData(
                    DataReadRequest.Builder()
                        .read(DataType.TYPE_SLEEP_SEGMENT)
                        .setTimeRange(1, System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .build()
                ).await()
            val sleepDataList = mutableListOf<SleepData>()
            for (dp in response.dataSets[0].dataPoints) {
                val startTime = dp.getStartTime(TimeUnit.MILLISECONDS)
                val endTime = dp.getEndTime(TimeUnit.MILLISECONDS)
                sleepDataList.add(SleepData(0, startTime, endTime))
            }
            sleepDataList
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun showRecords() {
        var strDate: String? = null
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                // Retrieve all sleep records
                allSleepRecords = sleepDao.allSleepRecords

                // Start test code

                // End test code

                // Switch to the main thread to update the UI
                withContext(Dispatchers.Main) {
                    displayCurrentRecord()
                }
            }
        }
    }

    // ******* Start New Chart code ********
    data class WeeklySleepData(
        val day: String,
        val sleep_time: String, // Sleep start time for the day
        val awakeSlots: List<TimeSlot>,
        val remSlots: List<TimeSlot>,
        val coreSlots: List<TimeSlot>,
        val deepSlots: List<TimeSlot>
    )

    data class TimeSlot(
        val start: Float,
        val duration: Float,
    )
    fun extractTime(timeString: String?): String? {
        return try {
            if (timeString != null) {
                val dateTimeFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a", Locale.getDefault())
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val date = dateTimeFormat.parse(timeString)
                date?.let { timeFormat.format(date) }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    //    private lateinit var dateTime: LocalDateTime
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun processSleepData(sleepData: List<SleepEntity>): List<WeeklySleepData> {
        val weeklyData = mutableListOf<WeeklySleepData>()

        for (i in sleepData.indices) {
            val sleepEntity = sleepData[i]

            val day = getDayName(sleepEntity.sleep_time!!)
            val sleep_startTime = extractTime(sleepEntity.sleep_time)
            val awakeSlots = extractTimeSlots(sleepEntity.awake_sleep)
            val remSlots = extractTimeSlots(sleepEntity.rem_sleep)
            val coreSlots = extractTimeSlots(sleepEntity.core_sleep)
            val deepSlots = extractTimeSlots(sleepEntity.deep_sleep)
            Log.e(TAG, "processSleepData: $sleep_startTime", )
            weeklyData.add(
                WeeklySleepData(
                    day = day,
                    sleep_time = sleep_startTime.toString(),
                    awakeSlots = awakeSlots,
                    remSlots = remSlots,
                    coreSlots = coreSlots,
                    deepSlots = deepSlots
                )
            )
        }

        return weeklyData
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun extractTimeSlots(timeSlots: String?): List<TimeSlot> {
        val slots = mutableListOf<TimeSlot>()
        if (timeSlots == null) return slots

        val dateFormatter = DateTimeFormatter.ofPattern("hh:mm a")
        val startOfDay = LocalTime.of(18, 0) // 6 PM

        val slotStrings = timeSlots.split(",")
        for (slot in slotStrings) {
            val times = slot.split("to")
            if (times.size == 2) {
                val startTime = LocalTime.parse(times[0].trim(), dateFormatter)
                val endTime = LocalTime.parse(times[1].trim(), dateFormatter)
                val duration = Duration.between(startTime, endTime).toMinutes() / 60f

                val startFloat = Duration.between(startOfDay, startTime).toMinutes() / 60f
                slots.add(TimeSlot(start = startFloat, duration = duration))
            }
        }

        return slots.sortedBy { it.start }
    }

    private fun setupChart(weeklyData: List<WeeklySleepData>) {
        // Create a list of BarEntry for each day of the week
        val entries = mutableListOf<BarEntry>()

        // Define the days of the week (assuming the days are consecutive from a given start day)
        val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        // Create a map of day to WeeklySleepData for quick lookup
        val dataMap = weeklyData.associateBy { it.day }

        for (i in daysOfWeek.indices) {
            val day = daysOfWeek[i]
            val data = dataMap[day]

            if (data != null) {
                // Calculate the starting empty duration from 6 PM to the start of the first event
                val dateTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val startTime = dateTimeFormat.parse(data.sleep_time)
                val calendar = Calendar.getInstance()
                calendar.time = startTime
                val startHour = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60f
                val startOffset = if (startHour < 18) 6f else startHour - 18f

                // Get durations for each sleep type
                val awakeDurations = data.awakeSlots.map { it.duration }.toFloatArray()
                val remDurations = data.remSlots.map { it.duration }.toFloatArray()
                val coreDurations = data.coreSlots.map { it.duration }.toFloatArray()
                val deepDurations = data.deepSlots.map { it.duration }.toFloatArray()

                // Combine all durations into a single BarEntry
                entries.add(
                    BarEntry(
                        i.toFloat(),
                        floatArrayOf(startOffset) + awakeDurations + remDurations + coreDurations + deepDurations
                    )
                )
            } else {
                // Add an empty BarEntry for days with no data
                entries.add(BarEntry(i.toFloat(), floatArrayOf(6f, 0f, 0f, 0f, 0f)))
            }
        }

        // Create a BarDataSet with the entries
        val barDataSet = BarDataSet(entries, "Sleep Data")
        // Set colors for different sleep types
        barDataSet.setColors(
            Color.TRANSPARENT,        // Empty
            Color.parseColor("#ea674f"),  // Awake
            Color.parseColor("#3577f7"),  // REM
            Color.parseColor("#0c7071"),  // Core
            Color.parseColor("#302e93")   // Deep
        )
        barDataSet.stackLabels = arrayOf("Empty", "Awake", "REM", "Core", "Deep")

        val barData = BarData(barDataSet)
        barData.barWidth = 0.4f

        barDataSet.valueFormatter = object : ValueFormatter() {
            override fun getBarLabel(barEntry: BarEntry?): String {
                return ""
            }

            override fun getFormattedValue(value: Float): String {
                return ""
            }
        }

        // Hide values above bars
        barData.setValueTextColor(Color.TRANSPARENT)
        chart!!.setDrawValueAboveBar(false)
        chart!!.data = barData
        val xAxis = chart!!.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(daysOfWeek)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.WHITE
        xAxis.setDrawGridLines(true) // Ensure grid lines are drawn
        xAxis.gridLineWidth = 1f // Set the width of the grid lines
        xAxis.setGridDashedLine(DashPathEffect(floatArrayOf(10f, 5f), 0f))
        // Configure the y-axis
        val yAxis = chart!!.axisLeft
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = 24f
        yAxis.textColor = Color.WHITE
        yAxis.setLabelCount(8, false)
        yAxis.granularity = 3f // Ensure labels are shown at intervals of 3 hours
        yAxis.setDrawGridLines(false) // Remove horizontal grid lines
        yAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val hour = value.toInt()
                return when (hour) {
                    0, 24 -> "6 PM"
                    3 -> "9 PM"
                    6 -> "12 AM"
                    9 -> "3 AM"
                    12 -> "6 AM"
                    15 -> "9 AM"
                    18 -> "12 PM"
                    21 -> "3 PM"
                    else -> ""
                }
            }
        }

        // Disable the right y-axis
        val rightAxis = chart!!.axisRight
        rightAxis.isEnabled = false
        rightAxis.setDrawGridLines(false) // Ensure no grid lines are drawn on the right axis

        // Disable the description text
        chart!!.description.isEnabled = false
        // Enable the legend
        chart!!.legend.isEnabled = true
        // Refresh the chart
        chart!!.invalidate()
    }





    // ******* End New Chart code ********

    private fun displayCurrentRecord() {
        allSleepRecords?.let { records ->
            if (currentIndex in records.indices) {
                val currentRecord = records[currentIndex]
                // Update the UI with the current record

                binding!!.tvTimeInBedHr.text = currentRecord.sleep_time
                binding!!.tvAwakeHr.text =
                    currentRecord.total_awake_time?.let { convertTimeStringToHoursAndMinutes(it) }
                binding!!.tvRemHr.text =
                    currentRecord.total_rem_time?.let { convertTimeStringToHoursAndMinutes(it) }
                binding!!.tvCoreHr.text =
                    currentRecord.total_core_time?.let { convertTimeStringToHoursAndMinutes(it) }
                binding!!.tvDeepHr.text =
                    currentRecord.total_deep_time?.let { convertTimeStringToHoursAndMinutes(it) }

                binding!!.tvProgressTimeASleep.text =
                    currentRecord.total_sleep_time?.let { formatTime(it) }
                binding!!.tvTimeInBedHr.text =
                    currentRecord.total_sleep_time?.let { formatTime2(it) }
                binding!!.tvTimeAsleepBedHr.text =
                    currentRecord.total_sleep_time?.let { formatTime3(it) }


                binding!!.tvDate.text =
                    currentRecord.awake_time?.let {
                        currentRecord.sleep_time?.let { it1 ->
                            formatDatesRange(
                                it1, it
                            )
                        }
                    }

//                binding!!.tvProgressTimeASleep.text = currentRecord.total_sleep_time
            }
        }
    }

    private fun formatTime(timeString: String): String {
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

        // Construct the formatted time string
        val formattedString = StringBuilder()

        // Append hours part if greater than zero
        if (hours > 0) {
            formattedString.append("$hours hr ")
        }

        // Append minutes part if greater than zero
        if (minutes > 0) {
            formattedString.append("$minutes min")
        }

        // If both hours and minutes are zero, assume it's "0 hr 0 min"
        if (hours == 0 && minutes == 0) {
            formattedString.append("0 hr 0 min")
        }

        // Return the formatted string
        return formattedString.toString().trim()
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

    private fun formatTime3(timeString: String?): SpannableString {
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

        // increa minutes by 20
        val totalMinutes = hours * 60 + minutes
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

    fun formatDatesRange(startDateStr: String, endDateStr: String): String {
        // Define date formats
        val inputFormat = SimpleDateFormat("d-MMM-yyyy hh:mm:ss a", Locale.ENGLISH)
        val monthDayFormat = SimpleDateFormat("MMMM dd", Locale.ENGLISH)
        val dayFormat = SimpleDateFormat("dd", Locale.ENGLISH)
        val monthFormat = SimpleDateFormat("MMMM", Locale.ENGLISH)
        val yearFormat = SimpleDateFormat("yyyy", Locale.ENGLISH)

        // Parse start and end dates
        val startDate = inputFormat.parse(startDateStr)
        val endDate = inputFormat.parse(endDateStr)

        // Get month and day parts
        val startDay = dayFormat.format(startDate!!)
        val endDay = dayFormat.format(endDate!!)
        val startMonth = monthFormat.format(startDate)
        val endMonth = monthFormat.format(endDate)
        val year = yearFormat.format(startDate)

        // Construct the final output
        return if (startMonth == endMonth) {
            "$startMonth $startDay - $endDay, $year"
        } else {
            "$startMonth $startDay - $endMonth $endDay, $year"
        }
    }

    fun convertTimeStringToHoursAndMinutes(timeString: String): SpannableString {
        val regex = """(\d+)\s*(minutes?|seconds?)""".toRegex()
        val matchResult = regex.find(timeString)

        if (matchResult != null) {
            val (value, unit) = matchResult.destructured
            val numericValue = value.toInt()

            // Convert the time to minutes if the unit is seconds
            val totalMinutes = if (unit.startsWith("second")) {
                numericValue / 60
            } else {
                numericValue
            }

            val hours = totalMinutes / 60
            val remainingMinutes = totalMinutes % 60

            val hoursText = if (hours > 0) "$hours hr " else ""
            val minutesText = "$remainingMinutes min"

            val fullText = "$hoursText$minutesText"
            val spannableString = SpannableString(fullText)

            // Styling hours and remaining minutes numbers
            if (hours > 0) {
                spannableString.setSpan(
                    RelativeSizeSpan(0.95f),
                    0,
                    hours.toString().length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            spannableString.setSpan(
                RelativeSizeSpan(0.95f),
                fullText.indexOf(remainingMinutes.toString()),
                fullText.indexOf(remainingMinutes.toString()) + remainingMinutes.toString().length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Styling text parts
            if (hours > 0) {
                spannableString.setSpan(
                    ForegroundColorSpan(Color.parseColor("#767a80")),
                    hours.toString().length,
                    fullText.indexOf("") + 4,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannableString.setSpan(
                    RelativeSizeSpan(0.65f),
                    hours.toString().length,
                    fullText.indexOf("") + 4,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            spannableString.setSpan(
                ForegroundColorSpan(Color.parseColor("#767a80")),
                fullText.indexOf("min"),
                fullText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                RelativeSizeSpan(0.65f),
                fullText.indexOf("min"),
                fullText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            return spannableString
        } else {
            // Handle cases where the input string is not in the expected format
            return SpannableString("Invalid input format")
        }
    }

    class TimeAxisValueFormatter(private val timeValues: Array<String>) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val index = (value / 3).toInt()
            return if (index in timeValues.indices) timeValues[index] else ""
        }
    }


}
