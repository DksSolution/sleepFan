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
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
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
import com.sleep.fan.databinding.FragmentDayBinding
import com.sleep.fan.newdb.NewSleepDatabase
import com.sleep.fan.newdb.SleepDao
import com.sleep.fan.newdb.SleepData
import com.sleep.fan.newdb.SleepDataGenerator
import com.sleep.fan.newdb.SleepEntity
import com.sleep.fan.utility.ProgressAnimator
import com.sleep.fan.utility.RoundedBarChart
import com.sleep.fan.utility.Utils
import com.sleep.fan.utility.Utils.Companion.convertTimeStringToHoursAndMinutes
import com.sleep.fan.utility.Utils.Companion.millisFromRfc339DateString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class DayFragment : Fragment() {
    private var binding: FragmentDayBinding? = null
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

    var PERIOD_START_DATE_TIME = "2024-05-01T12:00:00Z"
    var PERIOD_END_DATE_TIME = "2024-07-31T12:00:00Z"

    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
        .build()

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    // Defines the start and end of the period of interest in this example.
    private val periodStartMillis = millisFromRfc339DateString(com.sleep.fan.PERIOD_START_DATE_TIME)
    private val periodEndMillis = millisFromRfc339DateString(com.sleep.fan.PERIOD_END_DATE_TIME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_day,
            container,
            false
        )

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = NewSleepDatabase.getInstance(requireContext())
        sleepDao = db.sleepDao()

        chart = binding!!.barChart
        progressAnimator = ProgressAnimator(binding!!.progressForeground)
        progressAnimator!!.animateProgress(0) // Set your desired progress
        Handler().postDelayed({
            progressAnimator!!.animateProgress(80) // Set your desired progress after the delay
        }, 1000)


        showRecords()

//        accessGoogleFit()
        checkPermissionsAndRun(com.sleep.fan.FitActionRequestCode.READ_SLEEP_SESSIONS)
//        startActivity(Intent(requireActivity(), MainActivity::class.java))

        binding!!.ivPrev.setOnClickListener {
            allSleepRecords?.let { records ->
                if (currentIndex > 0) {
                    currentIndex--
                    displayCurrentRecord()
                } else {
                    println("No previous records.")
                }
            }
        }

        binding!!.ivNext.setOnClickListener {
            allSleepRecords?.let { records ->
                if (currentIndex < records.size - 1) {
                    currentIndex++
                    displayCurrentRecord()
                } else {
                    println("No more records.")
                }
            }
        }

        binding!!.tvProgressPercent.setOnClickListener {
            checkPermissionsAndRun(com.sleep.fan.FitActionRequestCode.READ_SLEEP_SESSIONS)
        }

    }

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
//                    Log.e(TAG, "BPM: $bpm")
//                    Log.e(TAG, "Heart rate: $bpm BPM at $timestamp")
                }
            }
        }
    }

    private fun dumpSleepSessionMetadata(session: Session) {
        val (startDateTime, endDateTime) = getSessionStartAndEnd(session)
        val totalSleepForNight = calculateSessionDuration(session)
//        Log.e(TAG, "===> $startDateTime to $endDateTime ($totalSleepForNight mins)")

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
//                Log.e(TAG, "--->\t$sleepStage: $duration (mins)")
            }
        }
    }

    // ***********************
    private fun accessGoogleFit() {
        // Fetch sleep data from Google Fit
        lifecycleScope.launch {
            val sleepData = fetchSleepData()
//            Log.e("","=================> Data: "+sleepData.toString())

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


    private fun showRecords() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                // Retrieve all sleep records
                allSleepRecords = sleepDao!!.allSleepRecords
                // Switch to the main thread to update the UI
                withContext(Dispatchers.Main) {
                    displayCurrentRecord()
                }
            }
        }
    }
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private fun processSleepData(sleepData: String?, category: Int, events: MutableList<Event>) {
        if (sleepData.isNullOrEmpty()) {
            return
        }

        val timeSlots = sleepData.split(",").map { it.trim() }
        for (slot in timeSlots) {
            val times = slot.split(" to ").map { it.trim() }
            val start = timeFormat.parse(times[0]) ?: continue
            val end = timeFormat.parse(times[1]) ?: continue

            val startTime = convertToFloat(start)
            val endTime = convertToFloat(end)
            var duration = endTime - startTime

            if (duration < 0) {
                // Handle the case where the time crosses midnight
                duration += 24.0f
            }

            events.add(Event(category, startTime, duration))
        }
    }

    private fun convertToFloat(date: Date): Float {
        val calendar = Calendar.getInstance().apply { time = date }
        val hours = calendar.get(Calendar.HOUR_OF_DAY).toFloat()
        val minutes = calendar.get(Calendar.MINUTE).toFloat()
        return hours + minutes / 60.0f
    }
    private fun displayCurrentRecord() {
        allSleepRecords?.let { records ->
            if (currentIndex in records.indices) {
                val currentRecord = records[currentIndex]
                // Update the UI with the current record

                Log.e(
                    "=====> See",
                    "Awake: ${currentRecord.awake_sleep}, \n REM: ${currentRecord.rem_sleep} \n Core: ${currentRecord.core_sleep} \n Deep: ${currentRecord.deep_sleep}"
                )
                val events = mutableListOf<Event>()

                processSleepData(currentRecord.awake_sleep, 0, events)
                processSleepData(currentRecord.rem_sleep, 1, events)
                processSleepData(currentRecord.core_sleep, 2, events)
                processSleepData(currentRecord.deep_sleep, 3, events)

                events.sortBy { it.time }

                // Print sorted events
                for (event in events) {
                    Log.e("checkSleepRecords","==> Category: ${event.category}, Start Time: ${event.time}, Duration: ${event.duration}")
                }


                setupChart(events)

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
            }
        }
    }

    private fun formatTime(timeString: String): String {
        val regex = """(\d+)\s*(hours?|hr|minutes?|min)""".toRegex(RegexOption.IGNORE_CASE)

        val matches = regex.findAll(timeString)

        var hours = 0
        var minutes = 0

        for (match in matches) {
            val (value, unit) = match.destructured
            when (unit.toLowerCase()) {
                "hours", "hour", "hr" -> hours += value.toInt()
                "minutes", "minute", "min" -> minutes += value.toInt()
            }
        }
        val formattedString = StringBuilder()
        if (hours > 0) {
            formattedString.append("$hours hr ")
        }
        if (minutes > 0) {
            formattedString.append("$minutes min")
        }
        if (hours == 0 && minutes == 0) {
            formattedString.append("0 hr 0 min")
        }
        return formattedString.toString().trim()
    }

    private fun formatTime2(timeString: String?): SpannableString {
        if (timeString.isNullOrEmpty()) {
            return SpannableString("0 hr 0 min")
        }
        val regex = """(\d+)\s*(hours?|hr|minutes?|min)""".toRegex(RegexOption.IGNORE_CASE)
        val matches = regex.findAll(timeString)

        var hours = 0
        var minutes = 0
        for (match in matches) {
            val (value, unit) = match.destructured
            when (unit.toLowerCase()) {
                "hours", "hour", "hr" -> hours += value.toInt()
                "minutes", "minute", "min" -> minutes += value.toInt()
            }
        }
        val totalMinutes = hours * 60 + minutes - 20
        hours = totalMinutes / 60
        minutes = totalMinutes % 60
        val formattedString = "$hours hr $minutes min"
        val spannableString = SpannableString(formattedString)
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
        val regex = """(\d+)\s*(hours?|hr|minutes?|min)""".toRegex(RegexOption.IGNORE_CASE)
        val matches = regex.findAll(timeString)

        var hours = 0
        var minutes = 0
        for (match in matches) {
            val (value, unit) = match.destructured
            when (unit.toLowerCase()) {
                "hours", "hour", "hr" -> hours += value.toInt()
                "minutes", "minute", "min" -> minutes += value.toInt()
            }
        }

        val totalMinutes = hours * 60 + minutes
        hours = totalMinutes / 60
        minutes = totalMinutes % 60
        val formattedString = "$hours hr $minutes min"
        val spannableString = SpannableString(formattedString)
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
        val inputFormat = SimpleDateFormat("d-MMM-yyyy hh:mm:ss a", Locale.ENGLISH)
        val monthDayFormat = SimpleDateFormat("MMMM dd", Locale.ENGLISH)
        val dayFormat = SimpleDateFormat("dd", Locale.ENGLISH)
        val monthFormat = SimpleDateFormat("MMMM", Locale.ENGLISH)
        val yearFormat = SimpleDateFormat("yyyy", Locale.ENGLISH)
        val startDate = inputFormat.parse(startDateStr)
        val endDate = inputFormat.parse(endDateStr)

        val startDay = dayFormat.format(startDate!!)
        val endDay = dayFormat.format(endDate!!)
        val startMonth = monthFormat.format(startDate)
        val endMonth = monthFormat.format(endDate)
        val year = yearFormat.format(startDate)

        return if (startMonth == endMonth) {
            "$startMonth $startDay - $endDay, $year"
        } else {
            "$startMonth $startDay - $endMonth $endDay, $year"
        }
    }

    open fun setupChart(events: MutableList<Event>) {
        val categoryColors: MutableList<Int> = ArrayList()
        categoryColors.add(Color.parseColor("#f2674f"))
        categoryColors.add(Color.parseColor("#3577f7"))
        categoryColors.add(Color.parseColor("#087172"))
        categoryColors.add(Color.parseColor("#302e95"))
        val typedArray =
            requireContext().obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        val bgColor = typedArray.getColor(0, 0)
        typedArray.recycle()
        val chart: HorizontalBarChart = binding!!.barChart
        val barChartRender = RoundedBarChart(chart, chart.animator, chart.viewPortHandler, 20)
        barChartRender.setRadius(15)
        chart.renderer = barChartRender
        val dataSets: MutableList<IBarDataSet> = ArrayList()
        val vals = FloatArray(events.size)
        for (i in events.indices) {
            vals[i] = events[i].duration
        }
        for (i in 0..3) {
            val colors: MutableList<Int> = ArrayList()
            for (j in events.indices) {
                if (events[j].category == i) {
                    colors.add(categoryColors[i])
                } else {
                    colors.add(bgColor)
                }
            }
            val entry = BarEntry(i.toFloat(), vals)
            val dataSet = BarDataSet(object : ArrayList<BarEntry?>() {
                init {
                    add(entry)
                }
            }, getStageLabel(i))
            dataSet.stackLabels = arrayOf(null)
            dataSet.colors = colors
            dataSet.isHighlightEnabled = false
            dataSet.setDrawValues(false)
            dataSets.add(dataSet)
        }
        val barData = BarData(dataSets)
        barData.barWidth = 0.3f
        chart.data = barData
        chart.description.isEnabled = false

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase): String {
                return getStageLabel(value.toInt())
            }
        }
        xAxis.textColor = Color.WHITE
        xAxis.setDrawGridLines(false)

        val yAxisRight = chart.axisRight
        yAxisRight.setDrawLabels(false)
        val yAxisLeft = chart.axisLeft
        yAxisLeft.setDrawGridLines(true)
        yAxisLeft.enableGridDashedLine(10f, 10f, 0f)
        yAxisLeft.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase): String {
                return getTimeLabel(value)
            }
        }
        yAxisLeft.textColor = Color.WHITE
        yAxisLeft.granularity = 1f
        chart.legend.isEnabled = false
        chart.invalidate()
    }

    private fun getStageLabel(value: Int): String {
        return when (value) {
            0 -> "Awake"
            1 -> "REM"
            2 -> "Core"
            3 -> "Deep"
            else -> ""
        }
    }

    private fun getTimeLabel(value: Float): String {
        var hours = value.toInt()
        val minutes = ((value - hours) * 60).toInt()
        var period = "AM"
        if (hours >= 12) {
            period = "PM"
            if (hours > 12) {
                hours -= 12
            }
        } else if (hours == 0) {
            hours = 12
        }
        return String.format("%d:%02d %s", hours, minutes, period)
    }


    final class Event(var category: Int, var time: Float, var duration: Float)

}
