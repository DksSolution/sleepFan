package com.sleep.fan.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sleep_data")
public class SleepData {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private long date;
    private long startTime;
    private long endTime;
    private boolean isSleeping;
    private String awakeTime;
    private String remTime;
    private String coreTime;
    private String deepTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public boolean isSleeping() {
        return isSleeping;
    }

    public void setSleeping(boolean sleeping) {
        isSleeping = sleeping;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getAwakeTime() {
        return awakeTime;
    }

    public void setAwakeTime(String awakeTime) {
        this.awakeTime = awakeTime;
    }

    public String getRemTime() {
        return remTime;
    }

    public void setRemTime(String remTime) {
        this.remTime = remTime;
    }

    public String getCoreTime() {
        return coreTime;
    }

    public void setCoreTime(String coreTime) {
        this.coreTime = coreTime;
    }

    public String getDeepTime() {
        return deepTime;
    }

    public void setDeepTime(String deepTime) {
        this.deepTime = deepTime;
    }
}
