package com.sleep.fan.newdb

data class MonthlyAverage(
    val month: String?,
    val avg_total_sleep_time: Double?,
    val avg_total_awake_time: Double?,
    val avg_total_rem_time: Double?
)
