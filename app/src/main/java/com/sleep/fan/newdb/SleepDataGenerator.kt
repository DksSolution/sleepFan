package com.sleep.fan.newdb

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
//import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

object SleepDataGenerator {

    private val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    suspend fun generateAndInsertSleepData(
        context: Context,
        sleepStart: String,
        sleepEnd: String,
        formatTime: String
    ) {
        try {
            Log.e("SleepDataGenerator", "Generating sleep data")
            val sleepData = generateSleepData(sleepStart, sleepEnd, formatTime)

            if (sleepData == null) {
                Log.e("SleepDataGenerator", "Failed to generate sleep data")
                return
            }

            // Extract date part from sleepStart (ignore time part)
            val dateOnly = sleepStart.substring(0, 11) // Adjust based on the exact format length

            // Use Kotlin coroutine with IO dispatcher for database operation
            withContext(Dispatchers.IO) {
                val db = NewSleepDatabase.getInstance(context)
                val sleepDao = db.sleepDao()

                // Check if sleep time already exists in the database by date part
                val existingData = sleepDao.findByDateOnly(dateOnly)
                if (existingData != null) {
                    Log.d("SleepDataGenerator", "Sleep data already exists for date: $dateOnly")
                    return@withContext  // Exit coroutine early if data exists
                }

                // Insert sleep data into the database
                val sleepEntity = SleepEntity().apply {
                    sleep_time = sleepStart
                    awake_time = sleepEnd
                    awake_sleep = sleepData.awakeSleep
                    rem_sleep = sleepData.remSleep
                    core_sleep = sleepData.coreSleep
                    deep_sleep = sleepData.deepSleep
                    total_sleep_time = sleepData.totalSleepTime
                    total_awake_time = sleepData.totalAwakeTime
                    total_rem_time = sleepData.totalRemTime
                    total_core_time = sleepData.totalCoreTime
                    total_deep_time = sleepData.totalDeepTime
                    bpm = sleepData.bpm
                }

//                Log.d("SleepDataGenerator", "Inserting sleep data: $sleepEntity")
                sleepDao.insert(sleepEntity)
                Log.d("SleepDataGenerator", "Insertion complete")
            }

        } catch (e: Exception) {
            Log.e("SleepDataGenerator", "Error inserting sleep data", e)
        }
    }



    private fun generateSleepData(sleepStart: String, sleepEnd: String, formatTime: String): SleepData? {
        return try {
            // Parsing the sleep start and end times with the correct format
            val sdf = SimpleDateFormat("d-MMM-yyyy hh:mm:ss a", Locale.getDefault())
            val sleepStartTime = sdf.parse(sleepStart)
            val sleepEndTime = sdf.parse(sleepEnd)

            // Print the parsed dates for debugging
//            Log.e("","Parsed Sleep Start Time: $sleepStartTime")
//            Log.e("","Parsed Sleep End Time: $sleepEndTime")

            if (sleepStartTime != null && sleepEndTime != null) {
                // Generating random durations
                val awakeTimes = generateRandomDurations(sleepStartTime, sleepEndTime, 4, 6)
                val remTimes = generateRandomDurations(sleepStartTime, sleepEndTime, 4, 6)
                val coreTimes = generateRandomDurations(sleepStartTime, sleepEndTime, 4, 6)
                val deepTimes = generateRandomDurations(sleepStartTime, sleepEndTime, 2, 4)

                // Calculating total times
                val totalAwakeTime = calculateTotalTime(awakeTimes)
                val totalRemTime = calculateTotalTime(remTimes)
                val totalCoreTime = calculateTotalTime(coreTimes)
                val totalDeepTime = calculateTotalTime(deepTimes)
                val totalSleepTime = formatTime // Example value

                // Creating SleepData object
                SleepData(
                    awakeSleep = awakeTimes.joinToString(", "),
                    remSleep = remTimes.joinToString(", "),
                    coreSleep = coreTimes.joinToString(", "),
                    deepSleep = deepTimes.joinToString(", "),
                    totalSleepTime = totalSleepTime,
                    totalAwakeTime = totalAwakeTime,
                    totalRemTime = totalRemTime,
                    totalCoreTime = totalCoreTime,
                    totalDeepTime = totalDeepTime,
                    bpm = "60" // Example value
                )
            } else {
                null
            }

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateRandomDurations(start: Date, end: Date, minDurations: Int, maxDurations: Int): List<String> {
        val durations = ArrayList<String>()
        val random = Random()

        // Convert start and end times to milliseconds
        val startTime = start.time
        val endTime = end.time

        // Generate random time intervals
        val durationCount = random.nextInt((maxDurations - minDurations) + 1) + minDurations
        repeat(durationCount) {
            var randomStartTime = startTime + (random.nextDouble() * (endTime - startTime)).toLong()
            var randomEndTime = randomStartTime + (10 + random.nextInt(30)) * 60 * 1000 // Random duration between 10 to 40 minutes

            if (randomEndTime > endTime) {
                randomEndTime = endTime
            }

            val duration = "${dateFormat.format(Date(randomStartTime))} to ${dateFormat.format(Date(randomEndTime))}"
            durations.add(duration)
        }

        return durations
    }

    private fun calculateTotalTime(durations: List<String>): String {
        val dateFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        var totalMinutes = 0

        durations.forEach { duration ->
            val times = duration.split(" to ")
            if (times.size == 2) {
                try {
                    val start = dateFormat.parse(times[0])
                    val end = dateFormat.parse(times[1])

                    if (start != null && end != null) {
                        var durationInMinutes = ((end.time - start.time) / (1000 * 60)).toInt()

                        // Handle crossing midnight
                        if (durationInMinutes < 0) {
                            durationInMinutes += 24 * 60 // Add 24 hours in minutes
                        }

                        totalMinutes += durationInMinutes
//                        Log.d("calculateTotalTime", "Parsed duration: $duration -> $durationInMinutes minutes")
                    } else {
                        Log.e("calculateTotalTime", "Failed to parse start or end time for duration: $duration")
                    }
                } catch (e: Exception) {
                    Log.e("calculateTotalTime", "Exception parsing duration: $duration", e)
                }
            } else {
                Log.e("calculateTotalTime", "Invalid duration format: $duration")
            }
        }

//        Log.d("calculateTotalTime", "===> Total minutes calculated: $totalMinutes")
        return "$totalMinutes minutes"
    }


    data class SleepData(
        val awakeSleep: String,
        val remSleep: String,
        val coreSleep: String,
        val deepSleep: String,
        val totalSleepTime: String,
        val totalAwakeTime: String,
        val totalRemTime: String,
        val totalCoreTime: String,
        val totalDeepTime: String,
        val bpm: String
    )
}
