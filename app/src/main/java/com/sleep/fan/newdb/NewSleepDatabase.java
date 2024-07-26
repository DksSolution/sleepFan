package com.sleep.fan.newdb;
import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(entities = {SleepEntity.class}, version = 1)
public abstract class NewSleepDatabase extends RoomDatabase {
    private static NewSleepDatabase instance;

    public abstract SleepDao sleepDao();

    public static synchronized NewSleepDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            NewSleepDatabase.class, "sleep_tracker")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}


