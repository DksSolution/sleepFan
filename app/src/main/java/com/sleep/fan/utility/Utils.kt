package com.sleep.fan.utility

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import com.sleep.fan.common.logger.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Utils {

    companion object {
        fun millisFromRfc339DateString(dateString: String): Long{ // retrun 2024-07-12T12:00:00Z
            return android.icu.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                .parse(dateString).time
        }

        fun getDayName(dateString: String): String {

            // Replace various types of spaces with a regular space
            val normalizedDateString = dateString
                .replace('\u00A0', ' ')  // Non-breaking space
                .replace('\u2007', ' ')  // Figure space
                .replace('\u202F', ' ')  // Narrow no-break space
                .trim()

            // Debug print to verify normalization
            Log.e("", "Normalized Date String: '$normalizedDateString'")

            // Define the input date format
            val inputFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a", Locale.getDefault())

            return try {
                // Parse the date string to a Date object
                val date = inputFormat.parse(normalizedDateString)

                // Define the output format to get the day name
                val outputFormat = SimpleDateFormat("EEE", Locale.getDefault())

                // Format the date to get the day name and return it
                outputFormat.format(date)
            } catch (e: Exception) {
                e.printStackTrace()
                "Invalid Date"
            }
        }
        fun separateDateTime(dateTimeStr: String): String {
            // Define the date-time format
            val dateTimeFormat = android.icu.text.SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a", Locale.getDefault())

            // Parse the input date-time string
            val date = dateTimeFormat.parse(dateTimeStr)

            // Define separate date and time formats
            val dateFormat = android.icu.text.SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())

            // Format the parsed date-time into separate date and time strings
            val dateStr = dateFormat.format(date)

            return dateStr
        }

        // Output: 1 hour 20 minutes
        fun formatTime(minutesString: String): String {
            // Convert the string parameter to an integer
            val minutes = minutesString.toInt()

            // Calculate hours and remaining minutes
            val hours = minutes / 60
            val remainingMinutes = minutes % 60

            // Construct the formatted string
            val formattedTime = if (hours > 0) {
                "$hours hour${if (hours > 1) "s" else ""} ${remainingMinutes} minute${if (remainingMinutes != 1) "s" else ""}"
            } else {
                "$remainingMinutes minute${if (remainingMinutes != 1) "s" else ""}"
            }

            return formattedTime
        }

        // Weekly date range < present >

        private val dateFormat =
            android.icu.text.SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        private val calendar = Calendar.getInstance()

        fun moveToCurrentWeek() {
            // Move calendar to the nearest previous Sunday
            while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
            }
        }

        fun showPreviousWeek() {
            calendar.add(Calendar.DAY_OF_MONTH, -7)
            updateDateRangeText()
        }

        fun showNextWeek() {
            calendar.add(Calendar.DAY_OF_MONTH, 7)
            updateDateRangeText()
        }

        fun updateDateRangeText(): String {
            val weekStart = calendar.time
            calendar.add(Calendar.DAY_OF_MONTH, 6)
            val weekEnd = calendar.time
            calendar.add(Calendar.DAY_OF_MONTH, -6) // Reset to the start of the week

            val range = "${dateFormat.format(weekStart)} - ${dateFormat.format(weekEnd)}"
//        binding!!.tvDate.text = range
            return range
        }

        //  Monthly date range < present >
        private val dateFormatMonth = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        private val calendarMonth = Calendar.getInstance()

        fun moveToCurrentMonth() {
            calendarMonth.set(Calendar.DAY_OF_MONTH, 1) // Move calendar to the start of the current month
        }

        fun showPreviousMonth(): String {
            calendarMonth.add(Calendar.MONTH, -1) // Move calendar to the previous month
            return updateDateRangeTextMonth()
        }

        fun showNextMonth(): String {
            calendarMonth.add(Calendar.MONTH, 1) // Move calendar to the next month
            return updateDateRangeTextMonth()
        }

        fun updateDateRangeTextMonth(): String {
            val monthStart = calendarMonth.time // Start of the month
            calendarMonth.add(Calendar.MONTH, 1) // Move to the start of the next month
            calendarMonth.add(Calendar.DAY_OF_MONTH, -1) // Move to the last day of the current month
            val monthEnd = calendarMonth.time // End of the month
            calendarMonth.add(Calendar.DAY_OF_MONTH, 1) // Move back to the start of the next month
            calendarMonth.add(Calendar.MONTH, -1) // Reset to the start of the current month

            return "${dateFormatMonth.format(monthStart)} - ${dateFormatMonth.format(monthEnd)}"
        }

        // Date range
        fun formatDateRange(dateRange: String): Pair<String, String> {

            val inputDateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            val outputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

            // Split the date range into start and end dates
            val dates = dateRange.split(" - ")
            if (dates.size != 2) {
                throw IllegalArgumentException("Invalid date range format")
            }

            // Parse the start and end dates
            val startDate = inputDateFormat.parse(dates[0])
            val endDate = inputDateFormat.parse(dates[1])

            val calendar = Calendar.getInstance()
            calendar.time = endDate
            calendar.add(Calendar.DAY_OF_MONTH, -7)
            val expectedStartDate = calendar.time

            // Adjust the start date if necessary
            val finalStartDate = if (startDate.after(endDate)) {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.time
            } else if (startDate != expectedStartDate) {
                expectedStartDate
            } else {
                startDate
            }

            // Set time to 12:00:00 for both dates
            calendar.time = finalStartDate
            calendar.set(Calendar.HOUR_OF_DAY, 12)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val adjustedStartDate = calendar.time

            calendar.time = endDate
            calendar.set(Calendar.HOUR_OF_DAY, 12)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val adjustedEndDate = calendar.time

            // Format the start and end dates into the desired format
            val formattedStartDate = outputDateFormat.format(adjustedStartDate)
            val formattedEndDate = outputDateFormat.format(adjustedEndDate)

            Log.e("======>","FStart: $formattedStartDate, FEnd: $formattedEndDate")

            return Pair(formattedStartDate, formattedEndDate)
        }

        fun seperateDateFromRange(dateRange: String): Pair<String, String> {
            val inputDateFormat = android.icu.text.SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            val outputDateFormat = android.icu.text.SimpleDateFormat("d-MMM-yyyy", Locale.getDefault())

            // Split the date range into start and end dates
            val parts = dateRange.split(" - ")
            if (parts.size != 2) {
                throw IllegalArgumentException("Invalid date range format")
            }

            // Extract the start and end dates
            val startPart = parts[0].trim()
            val endPart = parts[1].trim()

            // Parse the dates and reformat them
            val startDate = inputDateFormat.parse(startPart)
            val endDate = inputDateFormat.parse(endPart)

            val formattedStartDate = outputDateFormat.format(startDate)
            val formattedEndDate = outputDateFormat.format(endDate)

//            println("Formatted Start Date: $formattedStartDate")
//            println("Formatted End Date: $formattedEndDate")

            return Pair(formattedStartDate, formattedEndDate)
        }

        fun getMonthRangeInFormat(dateRange: String): Pair<String, String> {
            val inputDateFormat =
                android.icu.text.SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            val outputDateFormat =
                android.icu.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

            // Split the date range into start and end dates
            val parts = dateRange.split(" - ")
            if (parts.size != 2) {
                throw IllegalArgumentException("Invalid date range format")
            }

            // Extract the start and end dates
            val startPart = parts[0].trim()
            val endPart = parts[1].trim()

            // Parse the dates
            val startDate = inputDateFormat.parse(startPart)
            val endDate = inputDateFormat.parse(endPart)

            // Set the time to 12:00:00 for both dates
            val startCalendar = Calendar.getInstance().apply {
                time = startDate
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val endCalendar = Calendar.getInstance().apply {
                time = endDate
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Format the dates in the desired output format
            val formattedStartDate = outputDateFormat.format(startCalendar.time)
            val formattedEndDate = outputDateFormat.format(endCalendar.time)

            return Pair(formattedStartDate, formattedEndDate)
        }

        fun formatCustomDateRange(dateRange: String): String {
            val inputDateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            val monthDayFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

            // Split the date range into start and end dates
            val dates = dateRange.split(" - ")
            if (dates.size != 2) {
                throw IllegalArgumentException("Invalid date range format")
            }

            // Parse the start and end dates
            val startDate = inputDateFormat.parse(dates[0])
            val endDate = inputDateFormat.parse(dates[1])

            // Format the start and end dates into the desired output format
            val startMonthDay = monthDayFormat.format(startDate)
            val endMonthDay = monthDayFormat.format(endDate)
            val year = yearFormat.format(endDate)

            // Construct the final output string
            val formattedDateRange = if (startMonthDay.substring(0, 3) == endMonthDay.substring(0, 3)) {
                // Same month
                "${startMonthDay.substring(0, 3)} ${startMonthDay.substring(4)} - ${endMonthDay.substring(4)}, $year"
            }
            else {
                // Different months
                "$startMonthDay - $endMonthDay, $year"
            }

            return formattedDateRange
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
                    spannableString.setSpan(RelativeSizeSpan(0.95f), 0, hours.toString().length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                spannableString.setSpan(RelativeSizeSpan(0.95f), fullText.indexOf(remainingMinutes.toString()), fullText.indexOf(remainingMinutes.toString()) + remainingMinutes.toString().length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                // Styling text parts
                if (hours > 0) {
                    spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#767a80")), hours.toString().length, fullText.indexOf("") + 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannableString.setSpan(RelativeSizeSpan(0.65f), hours.toString().length, fullText.indexOf("") + 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#767a80")), fullText.indexOf("min"), fullText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannableString.setSpan(RelativeSizeSpan(0.65f), fullText.indexOf("min"), fullText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                return spannableString
            } else {
                // Handle cases where the input string is not in the expected format
                return SpannableString("Invalid input format")
            }
        }
    }
}
