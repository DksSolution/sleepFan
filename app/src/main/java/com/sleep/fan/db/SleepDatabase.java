package com.sleep.fan.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SleepData.class}, version = 1, exportSchema = false)
public abstract class SleepDatabase extends RoomDatabase {
    private static volatile SleepDatabase INSTANCE;

    public abstract SleepDataDao sleepDataDao();

    public static SleepDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (SleepDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    SleepDatabase.class, "sleep_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
