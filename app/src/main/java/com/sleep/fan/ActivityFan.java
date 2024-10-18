package com.sleep.fan;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sleep.fan.cutomView.CustomTimePickerView;
import com.sleep.fan.utility.Utility;

import java.util.Objects;

public class ActivityFan extends SoundBaseActivity implements View.OnClickListener {

    private int fanType;
    private RelativeLayout fanParentLayout = null;
    private GameView fanBlade = null;
    private ImageView btnPlay, btnSpin, btnStop;
    private TextView tvTimer = null;
    private ImageButton btnReset = null;
    private LinearLayout btnTime;
    private int hours = 0;
    private int minutes = 0;
    private int seconds = 0;
    private long getcountTimer = 0;
    CardView layout_timer;
    LinearLayout start_time_layout, llOverlayBinaural;
    // Garbage data start

    protected static boolean onTouchDoneBtn = false;
    protected static boolean onTouchStopBtn = false;
    protected static boolean onTouchPauseBtn = false;
    protected static boolean onTouchResumeBtn = false;
    protected static boolean onTimeFinish = false;
    protected static boolean onStartSpin = false;
    protected static boolean onStopSpin = false;
    protected static boolean onTouchZeroSet = false;

    // Garbage data end

    protected static boolean isFanPlaying = true;
    protected static boolean onTouchFastBtn = false;
    protected static boolean onTouchMediumBtn = false;
    protected static boolean onTouchSlowBtn = false;
    protected static boolean isSpinEnabled = true;
    protected static boolean isTimerSet = false;

    AdView mAdView;
    private boolean isBackPressed = false;
    private boolean isStopPressed = false;
    private boolean isActivityForeGround = false;
    LinearLayout btn_vol_low, btn_vol_medium, btn_vol_full;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fan);
        getDataFromIntent();
        initControls();
    }

    private void initControls() {

        try {
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            //}
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        btn_vol_low = findViewById(R.id.btn_vol_low);
        btn_vol_medium = findViewById(R.id.btn_vol_medium);
        btn_vol_full = findViewById(R.id.btn_vol_full);
        start_time_layout = findViewById(R.id.start_time_layout);
        fanParentLayout = findViewById(R.id.fan);
        llOverlayBinaural = findViewById(R.id.llOverlayBinaural);
        llOverlayBinaural.setOnClickListener(this);
        fanBlade = new GameView(this, fanType);
        fanParentLayout.addView(fanBlade);
        isFanPlaying = false;
        isSpinEnabled = false;
        isTimerSet = false;

        layout_timer = findViewById(R.id.layout_timer);

        tvTimer = (TextView) findViewById(R.id.tvTimer);
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/digital-7 (mono).ttf");
        tvTimer.setTypeface(font);

        setClick();

        try {
            Class.forName("android.os.AsyncTask");   //it prevents AdMob from crashing on HTC with Android 4.0.x
        } catch (Throwable ignored) {
        }

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    InterstitialAd minterstitialAd;
    private com.google.android.gms.ads.AdRequest adRequestInter;

    private void loadInterstitialAD() {
        if (Utility.isNetworkConnected(this)) {
            minterstitialAd = null;
            AdRequest adRequest = new AdRequest.Builder().build();
            InterstitialAd.load(this, getString(R.string.interstitial_ad_id), adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.e("Interstitial ad failed", loadAdError.toString());
                    super.onAdFailedToLoad(loadAdError);
                }

                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    Log.e("Interstitial ad success", "Loaded");
                    minterstitialAd = interstitialAd;
                }
            });
        }

    }

    private void setClick() {
        onTouchFastBtn = false;
        onTouchMediumBtn = false;
        onTouchSlowBtn = false;
        onTouchDoneBtn = false;
        onTouchStopBtn = false;
        onTouchPauseBtn = false;
        onTouchResumeBtn = false;
        onTimeFinish = false;
        onStartSpin = false;
        onStopSpin = false;
        onTouchZeroSet = false;

        isBackPressed = false;
        isStopPressed = false;

        ImageView btnback = (ImageView) findViewById(R.id.btnback);
        btnback.setOnClickListener(this);

        btnPlay = findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(this);

        btnSpin = findViewById(R.id.btnSpin);
        btnSpin.setOnClickListener(this);

        btnTime = findViewById(R.id.btnTime);
        btnTime.setOnClickListener(this);

        btnStop = findViewById(R.id.btnStop);
        btnStop.setOnClickListener(this);

    }

    private void getDataFromIntent() {

        fanType = getIntent().getIntExtra("fanType", 0);
    }

    @Override
    protected void onRestart() {
        // TODO Auto-generated method stub
        super.onRestart();
        fanBlade.RecycleImage();
        fanBlade = null;

        fanBlade = new GameView(this, fanType);
        fanParentLayout.removeAllViews();
        fanParentLayout.addView(fanBlade);
    }

    String speedSelected = "";
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnback:
                onBackPressed();
                break;

            case R.id.btn_vol_medium:
                btn_vol_medium.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.btn_bg_vol_pressed, null));
                btn_vol_full.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.btn_bg_vol, null));
                btn_vol_low.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.btn_bg_vol, null));
                speedSelected = "medium";
                setFanVolume(speedSelected);
                break;

            case R.id.btn_vol_full:
                btn_vol_medium.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.btn_bg_vol, null));
                btn_vol_full.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.btn_bg_vol_pressed, null));
                btn_vol_low.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.btn_bg_vol, null));
                speedSelected = "fast";
                setFanVolume(speedSelected);
                break;

            case R.id.btn_vol_low:
                btn_vol_medium.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.btn_bg_vol, null));
                btn_vol_full.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.btn_bg_vol, null));
                btn_vol_low.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.btn_bg_vol_pressed, null));
                speedSelected = "slow";
                setFanVolume(speedSelected);
                break;
            case R.id.btnPlay:
                fanPlayAndPause();
                playPauseMediaPlayer();
                break;

            case R.id.btnSpin:
                configureSpinButton();
                break;

            case R.id.btnTime:
                prepareSoundPool(fanType);
                showTimeOption();
                loadInterstitialAD();
                isStopPressed = false;
                break;

            case R.id.btnStop:
                isStopPressed = true;
                stopMediaPlayer();
                finishSession();
                break;
            case R.id.llOverlayBinaural:
                OverlayBinaural();
                break;
        }
    }

    boolean isPlayingBinaural = false;

    private TrackSelector trackBinauralSelector;
    private LoadControl loadBinauralControl;
    LinearLayout relaxation, mood_uplift, cognitive, creativity;
    CardView btnDone;
    MediaPlayer binauralMP;
    ExoPlayer binauralExoPlayer;
    private BottomSheetDialog bottomSheetBinauralDialog;
    private String selectedBinaural = "null";
    private void OverlayBinaural() {
        bottomSheetBinauralDialog = new BottomSheetDialog(ActivityFan.this, R.style.MyBottomSheetDialogTheme);
        Objects.requireNonNull(bottomSheetBinauralDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        bottomSheetBinauralDialog.setContentView(R.layout.dialog_add_binaural_bottom_sheet);
        bottomSheetBinauralDialog.show();
        relaxation = bottomSheetBinauralDialog.findViewById(R.id.relaxation);
        mood_uplift = bottomSheetBinauralDialog.findViewById(R.id.mood_uplift);
        cognitive = bottomSheetBinauralDialog.findViewById(R.id.cognitive);
        creativity = bottomSheetBinauralDialog.findViewById(R.id.creativity);
        btnDone = bottomSheetBinauralDialog.findViewById(R.id.btnDone);

        btnDone.setOnClickListener(view -> {
            if (selectedBinaural.equals("null")){
                Toast.makeText(ActivityFan.this, "Please select a beat", Toast.LENGTH_SHORT).show();
            }else {
                playOverlayBinaural();
            }
        });
        relaxation.setOnClickListener(view -> {
            selectedBinaural = "relaxation";
            relaxation.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_binaural_btn_pressed, null));
            mood_uplift.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_binaural_btn, null));
            cognitive.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.bg_binaural_btn, null));
            creativity.setBackground(ResourcesCompat.getDrawable(getResources() ,R.drawable.bg_binaural_btn, null));
        });
        mood_uplift.setOnClickListener(view -> {
            selectedBinaural = "mood_uplift";
            relaxation.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_binaural_btn, null));
            mood_uplift.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_binaural_btn_pressed, null));
            cognitive.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_binaural_btn, null));
            creativity.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_binaural_btn, null));
        });
        cognitive.setOnClickListener(view -> {
            selectedBinaural = "cognitive";
            relaxation.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_binaural_btn, null));
            mood_uplift.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_binaural_btn, null));
            cognitive.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_binaural_btn_pressed, null));
            creativity.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_binaural_btn, null));
        });
        creativity.setOnClickListener(view -> {
            selectedBinaural = "creativity";
            relaxation.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_binaural_btn, null));
            mood_uplift.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_binaural_btn, null));
            cognitive.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_binaural_btn, null));
            creativity.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_binaural_btn_pressed, null));
        });

    }
    private BottomSheetDialog bottomSheetPlayBinauralDialog;
    private LinearLayout ll_vol_high, ll_vol_medium, ll_vol_low, ll_play_pause;
    private TextView beat_name;
    private ImageView back, iv_binaural, iv_play_pause;
    private void playOverlayBinaural(){
        bottomSheetPlayBinauralDialog = new BottomSheetDialog(ActivityFan.this, R.style.MyBottomSheetDialogTheme);
        Objects.requireNonNull(bottomSheetPlayBinauralDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        bottomSheetPlayBinauralDialog.setContentView(R.layout.dialog_play_binaural_sound_bottom_sheet);
        bottomSheetPlayBinauralDialog.setCancelable(false);
        bottomSheetPlayBinauralDialog.show();
        ll_vol_high = bottomSheetPlayBinauralDialog.findViewById(R.id.ll_vol_high);
        ll_vol_medium = bottomSheetPlayBinauralDialog.findViewById(R.id.ll_vol_medium);
        ll_vol_low = bottomSheetPlayBinauralDialog.findViewById(R.id.ll_vol_low);
        ll_play_pause = bottomSheetPlayBinauralDialog.findViewById(R.id.ll_play_pause);
        beat_name = bottomSheetPlayBinauralDialog.findViewById(R.id.beat_name);
        back = bottomSheetPlayBinauralDialog.findViewById(R.id.back);
        iv_binaural = bottomSheetPlayBinauralDialog.findViewById(R.id.iv_binaural);
        iv_play_pause = bottomSheetPlayBinauralDialog.findViewById(R.id.iv_play_pause);
        back.setOnClickListener(view -> bottomSheetPlayBinauralDialog.dismiss());

        if (selectedBinaural.equals("relaxation")){
            beat_name.setText("Relaxation");
            if (binauralExoPlayer != null) {
                stopBinauralPlayer();
            }
            iv_binaural.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.relaxation, null));
            setBinauralLooping(R.raw.relaxation);
        }else if (selectedBinaural.equals("mood_uplift")){
            beat_name.setText("Mood uplift");
            if (binauralExoPlayer != null) {
                stopBinauralPlayer();
            }
            iv_binaural.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.mood_uplift, null));
            setBinauralLooping(R.raw.mood_uplift);
        }else if (selectedBinaural.equals("cognitive")){
            beat_name.setText("Cognitive improvement");
            if (binauralExoPlayer != null) {
                stopBinauralPlayer();
            }
            iv_binaural.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.cognitive_improvement, null));
            setBinauralLooping(R.raw.cognitive);
        }else if (selectedBinaural.equals("creativity")){
            beat_name.setText("Creativity");
            if (binauralExoPlayer != null) {
                stopBinauralPlayer();
            }
            iv_binaural.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.creativity, null));
            setBinauralLooping(R.raw.creativity);
        }
        binauralExoPlayer.setVolume(1.0f);
        ll_vol_high.setOnClickListener(view -> {
            binauralExoPlayer.setVolume(1.0f);
        });
        ll_vol_medium.setOnClickListener(view -> {
            binauralExoPlayer.setVolume(0.7f);
        });
        ll_vol_low.setOnClickListener(view -> {
            binauralExoPlayer.setVolume(0.3f);
        });
        ll_play_pause.setOnClickListener(view -> {
            if (binauralExoPlayer.isPlaying()){
                pauseAudio();
            }else {
                resumeAudio();
            }
        });
    }

    public void stopBinauralPlayer() {
        if (binauralExoPlayer != null) {
            binauralExoPlayer.setPlayWhenReady(false);
            binauralExoPlayer = null;
        }
    }

    public void setBinauralLooping(int rawID) {
        if (binauralExoPlayer == null) {
            // Create a DefaultTrackSelector
            DefaultTrackSelector trackSelector = new DefaultTrackSelector(this);

            // Create an instance of the ExoPlayer using the new ExoPlayer.Builder
            binauralExoPlayer = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .setLoadControl(new DefaultLoadControl())
                .build();

            // Correct way to build Uri for raw resources
            Uri uri = RawResourceDataSource.buildRawResourceUri(rawID);

            MediaSource mediaSource = buildMediaSource(uri);
            binauralExoPlayer.setMediaSource(mediaSource);
            binauralExoPlayer.prepare();
            binauralExoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
            binauralExoPlayer.setPlayWhenReady(true);
        }
    }
    public void pauseAudio() {
        if (binauralExoPlayer != null) {
            iv_play_pause.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play, null));
            binauralExoPlayer.setPlayWhenReady(false);
        }
    }

    public void resumeAudio() {
        if (binauralExoPlayer != null) {
            iv_play_pause.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause, null));
            binauralExoPlayer.setPlayWhenReady(true);
        }
    }
    private MediaSource buildMediaSource(Uri uri) {
        // Create a MediaItem from the Uri
        MediaItem mediaItem = MediaItem.fromUri(uri);

        // Create a data source factory
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, "user-agent");

        // Create a ProgressiveMediaSource using the MediaItem
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem);

        return mediaSource;
    }

    private void finishSession() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        getcountTimer = 0;
        //int multiply_hour = (hours * 60);
        // int multiply_minutes = (minutes + multiply_hour) * 60;
        timerStart(0);
    }

    CountDownTimer countDownTimer;

    private void getTimerValues() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hours = timePicker.getHour();
            minutes = timePicker.getMinute();
        } else {
            hours = timePicker.getHour();
            minutes = timePicker.getMinute();
        }

        if (hours > 0 || minutes > 0) {
            layout_timer.setVisibility(View.VISIBLE);
            start_time_layout.setVisibility(View.GONE);
            isFanPlaying = true;
            isSpinEnabled = true;
            playSoundPool(speedSelected);
            isTimerSet = true;
            int multiply_hour = (hours * 60);
            int multiply_minutes = (minutes + multiply_hour) * 60;
            timerStart(1000 * multiply_minutes);
        } else {
            start_time_layout.setVisibility(View.VISIBLE);
        }

        //Toast.makeText(this, "hrs :"+hours +" minutes:"+ minutes, Toast.LENGTH_SHORT).show();

    }

    CustomTimePickerView timePicker;
    BottomSheetDialog bottomSheetTimePickerDialog;

    private void showTimeOption() {
        bottomSheetTimePickerDialog = new BottomSheetDialog(ActivityFan.this, R.style.MyBottomSheetDialogTheme);
        Objects.requireNonNull(bottomSheetTimePickerDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        bottomSheetTimePickerDialog.setContentView(R.layout.layout_counter);
        bottomSheetTimePickerDialog.show();
        start_time_layout.setVisibility(View.GONE);
        bottomSheetTimePickerDialog.setOnDismissListener(dialogInterface -> {
            if (!isFanPlaying) {
                start_time_layout.setVisibility(View.VISIBLE);
            }
        });
        ImageView btnCancel = bottomSheetTimePickerDialog.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(view -> {
            bottomSheetTimePickerDialog.dismiss();
            start_time_layout.setVisibility(View.VISIBLE);
        });

        CardView btnDone = bottomSheetTimePickerDialog.findViewById(R.id.btnDone);
        btnDone.setOnClickListener(view -> {
            getTimerValues();
            bottomSheetTimePickerDialog.dismiss();
        });

        timePicker = bottomSheetTimePickerDialog.findViewById(R.id.timePicker);
//        setTimePickerTextColor(timePicker, Color.WHITE);
        if (timePicker != null) {
            // Set initial values or handle values previously selected
            if (hours > 0 || minutes > 0) {
                timePicker.setTime(hours, minutes);
            } else {
                timePicker.setTime(8, 0);  // Default time
            }
        }
    }
    private void configureSpinButton() {
        if (isSpinEnabled) {
            isSpinEnabled = false;
            btnSpin.setImageResource(R.drawable.spinning_fan_stop);
        } else {
            isSpinEnabled = true;
            btnSpin.setImageResource(R.drawable.spinning_fan);
        }
    }
    private void fanPlayAndPause() {
        if (isFanPlaying)
            pauseFan();
        else
            playFan();
    }

    private void playFan() {
        isFanPlaying = true;
        btnPlay.setImageResource(R.drawable.ic_pause);
        // OnTimeChange = true;
        timerStart(getcountTimer);
    }

    private void pauseFan() {
        isFanPlaying = false;
        btnPlay.setImageResource(R.drawable.ic_play);
        // OnTimeChange = false;
        if (countDownTimer != null)
            countDownTimer.cancel();
    }

    private boolean isFanFinishedWhenBackground = false;

    public void timerStart(long millisInFuture) {
        countDownTimer = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onFinish() {
                isFanPlaying = false;
                isSpinEnabled = false;
                isTimerSet = false;
                layout_timer.setVisibility(View.GONE);
                start_time_layout.setVisibility(View.VISIBLE);
                tvTimer.setText("" + "00" + ":" + "00" + ":" + "00");
                if (timePicker != null) {
                    timePicker.setTime(0, 0);
//                    timePicker.setCurrentMinute(0);
                }
                hours = 0;
                minutes = 0;
                seconds = 0;
                stopMediaPlayer();
                if (!isBackPressed && minterstitialAd != null && !isStopPressed && isActivityForeGround) {
                    minterstitialAd.show(ActivityFan.this);
                    isFanFinishedWhenBackground = false;
                } else {
                    isFanFinishedWhenBackground = true;
                }
            }

            @Override
            public void onTick(long millisUntilFinished) {
                getcountTimer = millisUntilFinished;
                seconds = (int) (millisUntilFinished / 1000) % 60;
                minutes = (int) ((millisUntilFinished / (1000 * 60)) % 60);
                hours = (int) ((millisUntilFinished / (1000 * 60 * 60)) % 24);

                String secondValue = String.format("%02d", seconds);
                String minuteValue = String.format("%02d", minutes);
                String hourValue = String.format("%02d", hours);

                tvTimer.setText(hourValue + ":" + minuteValue + ":" + secondValue);
            }
        };

        countDownTimer.start();

    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityForeGround = true;
        if (minterstitialAd != null && !isStopPressed && isFanFinishedWhenBackground) {
            minterstitialAd.show(ActivityFan.this);
            isFanFinishedWhenBackground = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityForeGround = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        isBackPressed = true;
        finishSession();
        isFanPlaying = false;
        isSpinEnabled = false;
        isTimerSet = false;
        binauralExoPlayer.setPlayWhenReady(false);
        binauralExoPlayer = null;
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isBackPressed = true;
        finishSession();
        isFanPlaying = false;
        isSpinEnabled = false;
        isTimerSet = false;
    }
}
