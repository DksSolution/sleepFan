package com.sleep.fan.newdb;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SleepDao {
    @Insert
    void insert(SleepEntity sleepEntity);

    @Query("SELECT * FROM tbl_sleep WHERE id = :id")
    SleepEntity getSleepRecordById(int id);

    @Query("SELECT * FROM tbl_sleep")
    List<SleepEntity> getAllSleepRecords();

    @Query("SELECT * FROM tbl_sleep WHERE date(sleep_time) = date('now')")
    List<SleepEntity> getTodaySleepRecords();

    @Query("SELECT * FROM tbl_sleep WHERE strftime('%Y-%W', sleep_time) = strftime('%Y-%W', 'now')")
    List<SleepEntity> getCurrentWeekSleepRecords();

    @Query("SELECT * FROM tbl_sleep WHERE strftime('%Y-%m', sleep_time) = strftime('%Y-%m', 'now')")
    List<SleepEntity> getCurrentMonthSleepRecords();

    @Query("SELECT * FROM tbl_sleep WHERE substr(sleep_time, 1, 10) = :dateOnly")
    SleepEntity findBySleepTime(String dateOnly);

    @Query("SELECT * FROM tbl_sleep WHERE substr(sleep_time, 1, 11) = :dateOnly")
    SleepEntity findByDateOnly(String dateOnly);

    //    @Query("SELECT * FROM tbl_sleep WHERE substr(awake_time, 1, 11) BETWEEN :startDate AND :endDate ORDER BY awake_time DESC")
//    List<SleepEntity> getSleepRecordsWithinDateRange(String startDate, String endDate);
    @Query("SELECT * FROM tbl_sleep WHERE substr(awake_time, 1, 11) BETWEEN :startDate AND :endDate ORDER BY awake_time ASC")
    List<SleepEntity> getSleepRecordsWithinDateRange(String startDate, String endDate);

   // *****************

//    @Query("""
//        SELECT
//            AVG(
//                (CAST(SUBSTR(total_sleep_time, 1, INSTR(total_sleep_time, ' hours') - 1) AS INTEGER) * 60) +
//                CAST(SUBSTR(total_sleep_time, INSTR(total_sleep_time, ' hours') + 7, INSTR(total_sleep_time, ' minutes') - INSTR(total_sleep_time, ' hours') - 7) AS INTEGER)
//            ) AS avg_total_sleep_time,
//            AVG(CAST(SUBSTR(total_awake_time, 1, INSTR(total_awake_time, ' minutes') - 1) AS INTEGER)) AS avg_total_awake_time,
//            AVG(CAST(SUBSTR(total_rem_time, 1, INSTR(total_rem_time, ' minutes') - 1) AS INTEGER)) AS avg_total_rem_time
//        FROM tbl_sleep
//        WHERE date(substr(awake_time, 1, 11)) BETWEEN date(:startDate) AND date(:endDate)
//    """)
//   List<MonthlyAverage> getAveragesWithinDateRange(String startDate, String endDate);

    @Query("SELECT * FROM tbl_sleep")
    List<SleepEntity> getAllSleepData();

}
