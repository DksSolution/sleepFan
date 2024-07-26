package com.sleep.fan.newdb

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_data")
data class SleepData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,
    val endTime: Long
)
