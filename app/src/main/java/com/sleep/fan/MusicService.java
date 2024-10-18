package com.sleep.fan;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.wav.WavExtractor;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.sleep.fan.model.DataModel;

public class MusicService extends Service implements Player.Listener {


    IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public MusicService getServerInstance() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 16){

           // if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

                NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    @SuppressLint("WrongConstant")
                    NotificationChannel notificationChannel = new NotificationChannel("101", "Notification", NotificationManager.IMPORTANCE_MAX);

                    //Configure Notification Channel
                    notificationChannel.setDescription("Game Notifications");
                    notificationChannel.enableLights(true);
                    notificationChannel.setVibrationPattern(new long[]{0, 0, 0, 0});
                    notificationChannel.enableVibration(false);
                    notificationChannel.setShowBadge(false);
                    notificationManager.createNotificationChannel(notificationChannel);
                }

           // }
        }
        Notification notification = new NotificationCompat.Builder(this, "101")
                .setSmallIcon(R.mipmap.app_icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Have a good sleep!")
                .setPriority(Notification.PRIORITY_MAX)
                .setVibrate(new long[]{0L})
                .build();
                //.setContentIntent(pendingIntent).build();
        startForeground(1337, notification);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


}
