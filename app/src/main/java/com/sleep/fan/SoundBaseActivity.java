package com.sleep.fan;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.wav.WavExtractor;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.internal.Constants;
import com.sleep.fan.model.DataModel;

public class SoundBaseActivity extends AppCompatActivity implements Player.Listener {

    //private int fanType;
    boolean isPlaying = false;

    private TrackSelector trackSelector;
    private LoadControl loadControl;
    ExoPlayer player;

    MusicService musicService;
    boolean mBounded;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        trackSelector = new DefaultTrackSelector();
        loadControl = new DefaultLoadControl();
        player = new ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build();

// Add the listener
        player.addListener(this);
    }


    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    public void prepareSoundPool(int fanType){
        int trackID = 0;
        if(fanType == 1)
            trackID = R.raw.box_fan_clean_loop;
        else if(fanType == 2)
            trackID = R.raw.air_conditioner_clean_loop;
        else if(fanType == 3)
            trackID = R.raw.fire_furnace_clean_loop;
        else if(fanType == 4)
            trackID = R.raw.ovan_fan_clean_loop;

        setDataSpec(trackID);

    }

    public void prepareSoundPool(DataModel dataModel) {
        int trackID = 0;
        trackID = getResources().getIdentifier("@raw/"+dataModel.soundID, null, getPackageName());
        setDataSpec(trackID);
    }

    public void setDataSpec(int trackID){
        DataSpec dataSpec;
        dataSpec = new DataSpec(RawResourceDataSource.buildRawResourceUri(trackID));
        final RawResourceDataSource rawResourceDataSource = new RawResourceDataSource(this);
        try {
            rawResourceDataSource.open(dataSpec);
        } catch (RawResourceDataSource.RawResourceDataSourceException e) {
            e.printStackTrace();
        }
        DataSource.Factory factory = new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return rawResourceDataSource;
            }
        };

        MediaItem mediaItem = MediaItem.fromUri(rawResourceDataSource.getUri());
        audioSource = new ProgressiveMediaSource.Factory(factory, WavExtractor.FACTORY).createMediaSource(mediaItem);
    }

    MediaSource audioSource;
    LoopingMediaSource loopingMediaSource;
    public void playSoundPool(String speedSelected) {

        player.prepare(audioSource);
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        player.setPlayWhenReady(true);
        player.seekTo(0);
        setFanVolume(speedSelected);
        isPlaying = true;
        startService();
    }

    public void setFanVolume(String speedSelected){
        if(player != null){
        if(speedSelected.equalsIgnoreCase("slow"))
            player.setVolume(0.5f);
        else if(speedSelected.equalsIgnoreCase("medium"))
            player.setVolume(0.7f);
        else
            player.setVolume(1.0f);
        }
    }

    public void playPauseMediaPlayer(){
        if(player != null && isPlaying) {
            player.setPlayWhenReady(false);
            isPlaying = false;
        }
        else{
            isPlaying = true;
            player.setPlayWhenReady(true);
        }

    }

    public void stopMediaPlayer(){
        if (player != null  && isPlaying) {
            player.setPlayWhenReady(false);
            isPlaying = false;
            stopService();
        }

    }

    public void startService() {
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
    }

    public void stopService(){
        Intent intent = new Intent(this, MusicService.class);
        stopService(intent);
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(player != null)
            player.release();

        stopService();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopMediaPlayer();
        if (player != null) {
            player.release();
            player = null;
        }

        if (trackSelector != null)
            trackSelector = null;

        if (loadControl != null)
            loadControl = null;

        finish();
    }

    // exo player overrriden method start
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }
}
