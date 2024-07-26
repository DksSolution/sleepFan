package com.sleep.fan.newdb

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tbl_sleep")
data class SleepEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var sleep_time: String? = null,
    var awake_time: String? = null,
    var awake_sleep: String? = null,
    var rem_sleep: String? = null,
    var core_sleep: String? = null,
    var deep_sleep: String? = null,
    var total_sleep_time: String? = null,
    var total_awake_time: String? = null,
    var total_rem_time: String? = null,
    var total_core_time: String? = null,
    var total_deep_time: String? = null,
    var bpm: String? = null
)
