package com.sleep.fan.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SleepDataDao {
    @Insert
    void insert(SleepData sleepData);
    @Update
    void update(SleepData sleepData);
    @Query("SELECT * FROM sleep_data ORDER BY id DESC LIMIT 1")
    SleepData getLastSleepRecord();
    @Query("SELECT * FROM sleep_data WHERE date BETWEEN :startDate AND :endDate")
    LiveData<List<SleepData>> getSleepDataBetweenDates(long startDate, long endDate);

    @Query("SELECT * FROM sleep_data WHERE date BETWEEN :startTime AND :endTime")
    List<SleepData> getSleepDataBetween(long startTime, long endTime);
//    @Query("SELECT * FROM sleep_data WHERE timestamp >= :startDate AND timestamp <= :endDate")
//    LiveData<List<SleepData>> getSleepDataBetweenDates(long startDate, long endDate);
}
