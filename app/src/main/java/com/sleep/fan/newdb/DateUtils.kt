package com.sleep.fan.newdb

import android.icu.text.SimpleDateFormat
import com.sleep.fan.common.logger.Log
//import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {
    // updateDateRangeText
    private val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    // parseDateRange
//    private val inputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
//    private val outputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'12:00:00'Z'", Locale.getDefault())




    fun moveToCurrentMonth() {
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, if (currentDay >= 12) 12 else 12 - 1)
        updateToNearestMonthStart()
    }

    fun showPreviousMonth() {
        calendar.add(Calendar.MONTH, -1)
        updateToNearestMonthStart()
    }

    fun showNextMonth() {
        calendar.add(Calendar.MONTH, 1)
        updateToNearestMonthStart()
    }

    fun updateDateRangeText(): String {
        val monthStart = calendar.time
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val monthEnd = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MONTH, -1)

        return "${dateFormat.format(monthStart)} - ${dateFormat.format(monthEnd)}"
    }

    private fun updateToNearestMonthStart() {
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        if (currentDay >= 12) {
            calendar.set(Calendar.DAY_OF_MONTH, 12)
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, 12)
            calendar.add(Calendar.MONTH, -1)
        }
    }


}
