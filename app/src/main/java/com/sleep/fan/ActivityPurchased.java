package com.sleep.fan;

import androidx.annotation.NonNull;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.sleep.fan.model.DataModel;
import com.sleep.fan.utility.Utility;

public class ActivityPurchased extends SoundBaseActivity implements View.OnClickListener {

    DataModel dataModel;
    private ImageButton fastButton = null;
    private ImageButton slowButton = null;
    private ImageButton mediumButton = null;
    private ImageButton btnPlay;
    private ImageButton btnSpin = null;
    private Button btnTime = null;
    RelativeLayout layout_timer;
    private TextView tvTimer = null;
    private int hours = 0;
    private int minutes = 0;
    private int seconds = 0;
    private long getcountTimer = 0;

    protected static boolean isTimerSet = false;
    protected  static boolean isFanPlaying = false;
    protected static boolean isSpinEnabled = false;
    private boolean isBackPressed = false;
    private boolean isStopPressed = false;
    private boolean isActivityForeGround = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchased);
        dataModel = getIntent().getParcelableExtra("dataModel");
        initControls();
    }

    ImageView ivGif;
    private void initControls() {

        try{
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            //}
        }catch(Exception ex){
            ex.printStackTrace();
        }

        ivGif = (ImageView) findViewById(R.id.ivGif);
        if(dataModel != null)
            loadImage("@drawable/" + dataModel.icon + "_1");
            /*Glide.with(this).load(this.getResources().getIdentifier("@drawable/" + dataModel.icon + "_1", null, getPackageName()))
                    .placeholder(this.getResources().getIdentifier("@drawable/" + dataModel.icon + "_1", null, getPackageName()))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivGif);*/

        layout_timer = (RelativeLayout) findViewById(R.id.layout_timer);
        tvTimer = (TextView) findViewById(R.id.tvTimer);
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/digital-7 (mono).ttf");
        tvTimer.setTypeface(font);

        setClickEvents();

        try
        {
            Class.forName("android.os.AsyncTask");   //it prevents AdMob from crashing on HTC with Android 4.0.x
        }
        catch(Throwable ignored)
        {}

        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        isTimerSet = false;
        isFanPlaying = false;
        isSpinEnabled = false;

    }

    private void loadImage(String imageIdentifier){
        Glide.with(this).load(this.getResources().getIdentifier(imageIdentifier, null, getPackageName()))
                .placeholder(this.getResources().getIdentifier("@drawable/" + dataModel.icon + "_1", null, getPackageName()))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(ivGif);
    }

    String speedSelected = "fast";
    private void setClickEvents() {

        ImageButton btnback = (ImageButton) findViewById(R.id.btnback);
        btnback.setOnClickListener(this);

        fastButton = (ImageButton) findViewById(R.id.fastButton);
        fastButton.setOnClickListener(this);

        slowButton = (ImageButton) findViewById(R.id.slowButton);
        slowButton.setOnClickListener(this);

        mediumButton = (ImageButton) findViewById(R.id.mediumButton);
        mediumButton.setOnClickListener(this);

        btnTime = (Button) findViewById(R.id.btnTime);
        btnTime.setOnClickListener(this);

        btnPlay = (ImageButton) findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(this);

        btnSpin = (ImageButton) findViewById(R.id.btnSpin);
        btnSpin.setOnClickListener(this);

        ImageButton btnStop = (ImageButton) findViewById(R.id.btnStop);
        btnStop.setOnClickListener(this);

        isBackPressed = false;
        isStopPressed = false;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnback: onBackPressed();
                break;

            case R.id.mediumButton:
                setFanSpeedToMedium();
                speedSelected = "medium";
                setFanVolume(speedSelected);
                break;

            case R.id.fastButton:
                setFanSpeedToFast();
                speedSelected = "fast";
                setFanVolume(speedSelected);
                break;

            case R.id.slowButton:
                setFanSpeedToSlow();
                speedSelected = "slow";
                setFanVolume(speedSelected);
                break;

            case R.id.btnTime:
                isStopPressed = false;
                prepareSoundPool(dataModel);
                showTimeOption();
                loadInterstitialAD();
                break;

            case R.id.btnCancel:
                // popupWindow.dismiss();
                layout.setVisibility(View.GONE);
                btnTime.setVisibility(View.VISIBLE);
                break;

            case R.id.btnDone:
                getTimerValues();
                //popupWindow.dismiss();
                layout.setVisibility(View.GONE);
                setFanSpeed();
                break;

            case R.id.btnPlay:
                fanPlayAndPause();
                playPauseMediaPlayer();
                break;

            case R.id.btnStop:
                isStopPressed = true;
                stopMediaPlayer();
                finishSession();
                break;

            case R.id.btnSpin:
                configureSpinButton();
                break;

        }
    }

    private void configureSpinButton() {
        if(isSpinEnabled){
            isSpinEnabled = false;
            btnSpin.setImageResource(R.drawable.stopspin);
            loadImage("@drawable/" + dataModel.icon + "_1");
           // Glide.with(this).load(this.getResources().getIdentifier("@drawable/" + dataModel.icon + "_1", null, getPackageName())).into(ivGif);
        }else{
            isSpinEnabled = true;
            btnSpin.setImageResource(R.drawable.startspin);
            setFanSpeed();
        }
    }

    private void fanPlayAndPause() {
        if(isFanPlaying)
            pauseFan();
        else
            playFan();
    }

    private void playFan() {
        isFanPlaying = true;
        btnPlay.setImageResource(R.drawable.pause);
        timerStart(getcountTimer);
        setFanSpeed();

    }

    private void pauseFan(){
        isFanPlaying = false;
        btnPlay.setImageResource(R.drawable.play);
        Glide.with(this).load(this.getResources().getIdentifier("@drawable/" + dataModel.icon + "_1", null, getPackageName())).into(ivGif);
        if(countDownTimer != null)
            countDownTimer.cancel();
    }

    private void finishSession() {
        if(countDownTimer != null){
            countDownTimer.cancel();
            countDownTimer= null;
        }
        getcountTimer = 0;
        timerStart(0);
       // Glide.with(this).load(this.getResources().getIdentifier("@drawable/" + dataModel.icon + "_1", null, getPackageName())).into(ivGif);
    }


    private void setFanSpeed() {
        switch (speedSelected){
            case "medium":
                setFanSpeedToMedium();
                break;

            case "slow":
                setFanSpeedToSlow();
                break;

            case "fast":
                setFanSpeedToFast();
                break;
        }

    }

    TimePicker timePicker;
    View layout;
    private PopupWindow popupWindow;
    private void showTimeOption() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layout = inflater.inflate(R.layout.layout_counter, null);
        RelativeLayout parent = findViewById(R.id.parent);
        parent.addView(layout);
        btnTime.setVisibility(View.GONE);

        Button btnCancel = (Button) layout.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(this);

        Button btnDone = (Button) layout.findViewById(R.id.btnDone);
        btnDone.setOnClickListener(this);

        timePicker = (TimePicker) layout.findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(hours >0 || minutes >0){
                timePicker.setHour(hours);
                timePicker.setMinute(minutes);
            }else{
                timePicker.setHour(8);
                timePicker.setMinute(0);
            }
        }else{
            timePicker.setCurrentHour(8);
            timePicker.setCurrentMinute(0);
        }

    }

    CountDownTimer countDownTimer;
    private void getTimerValues() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hours = timePicker.getHour();
            minutes = timePicker.getMinute();
        }else{
            hours = timePicker.getCurrentHour();
            minutes = timePicker.getCurrentMinute();
        }

        if(hours >0 || minutes >0) {
            layout_timer.setVisibility(View.VISIBLE);
            btnTime.setVisibility(View.GONE);
            isFanPlaying = true;
            isSpinEnabled = true;
            playSoundPool(speedSelected);
            isTimerSet = true;
            int multiply_hour = (hours * 60);
            int multiply_minutes = (minutes + multiply_hour) * 60;
            timerStart(1000 * multiply_minutes);
        }else
            btnTime.setVisibility(View.VISIBLE);

    }

    private boolean isFanFinishedWhenBackground = false;
    public void timerStart(long millisInFuture){
        countDownTimer = new CountDownTimer(millisInFuture, 1000){
            @Override
            public void onFinish()
            {
                isFanPlaying = false;
                isSpinEnabled = false;
                isTimerSet = false;
                layout_timer.setVisibility(View.GONE);
                btnTime.setVisibility(View.VISIBLE);
                tvTimer.setText(""+"00"+ ":" + "00"+ ":"+"00");
                if(timePicker!= null) {
                    timePicker.setCurrentHour(0);
                    timePicker.setCurrentMinute(0);
                }
                hours = 0;
                minutes = 0;
                seconds = 0;
                stopMediaPlayer();
                if(!isBackPressed)
                    loadImage("@drawable/" + dataModel.icon + "_1");
                   // Glide.with(ActivityPurchased.this).load(getResources().getIdentifier("@drawable/" + dataModel.icon + "_1", null, getPackageName())).into(ivGif);
                if(!isBackPressed && minterstitialAd != null && !isStopPressed && isActivityForeGround) {

                    isFanFinishedWhenBackground = false;
                }else{
                    isFanFinishedWhenBackground = true;
                }
            }

            @Override
            public void onTick(long millisUntilFinished) {
                // TODO Auto-generated method stub
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
        if(minterstitialAd != null && !isStopPressed && isActivityForeGround && isFanFinishedWhenBackground){

            isFanFinishedWhenBackground = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityForeGround = false;
    }

    InterstitialAd minterstitialAd;
    private com.google.android.gms.ads.AdRequest adRequestInter;
    private void loadInterstitialAD() {
        if(Utility.isNetworkConnected(this)) {
            minterstitialAd = null;
            adRequestInter = new com.google.android.gms.ads.AdRequest.Builder().build();
            InterstitialAd.load(this, getString(R.string.interstitial_ad_id), adRequestInter, new InterstitialAdLoadCallback() {
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

    private void setFanSpeedToSlow() {
        if(isTimerSet && isFanPlaying && isSpinEnabled) {
            if (dataModel.position == 5 || dataModel.position == 6 || dataModel.position == 7 || dataModel.position == 8)
                loadImage("@raw/" + dataModel.icon + "_fast");
                //Glide.with(this).load(this.getResources().getIdentifier("@raw/" + dataModel.icon + "_slow", null, getPackageName())).into(ivGif);
            else
                loadImage("@raw/" + dataModel.icon);
                //Glide.with(this).load(this.getResources().getIdentifier("@raw/" + dataModel.icon, null, getPackageName())).into(ivGif);
        }
        slowButton.setImageResource(R.drawable.slow_selected);
        mediumButton.setImageResource(R.drawable.medium_unselected);
        fastButton.setImageResource(R.drawable.fast_unselected);

    }

    private void setFanSpeedToFast() {
        if(isTimerSet && isFanPlaying && isSpinEnabled) {
            if (dataModel.position == 5 || dataModel.position == 6 || dataModel.position == 7 || dataModel.position == 8)
                loadImage("@raw/" + dataModel.icon + "_fast");
               // Glide.with(this).load(this.getResources().getIdentifier("@raw/" + dataModel.icon + "_fast", null, getPackageName())).into(ivGif);
            else
                loadImage("@raw/" + dataModel.icon);
                //Glide.with(this).load(this.getResources().getIdentifier("@raw/" + dataModel.icon, null, getPackageName())).into(ivGif);
        }
        fastButton.setImageResource(R.drawable.fast_selected);
        mediumButton.setImageResource(R.drawable.medium_unselected);
        slowButton.setImageResource(R.drawable.slow_unselected);

    }

    private void setFanSpeedToMedium() {
        if(isTimerSet && isFanPlaying && isSpinEnabled) {
            if (dataModel.position == 5 || dataModel.position == 6 || dataModel.position == 7 || dataModel.position == 8)
                loadImage("@raw/" + dataModel.icon + "_fast");
                //Glide.with(this).load(this.getResources().getIdentifier("@raw/" + dataModel.icon + "_fast", null, getPackageName())).into(ivGif);
            else
                loadImage("@raw/" + dataModel.icon);
                //Glide.with(this).load(this.getResources().getIdentifier("@raw/" + dataModel.icon, null, getPackageName())).into(ivGif);
        }
        mediumButton.setImageResource(R.drawable.medium_selected);
        fastButton.setImageResource(R.drawable.fast_unselected);
        slowButton.setImageResource(R.drawable.slow_unselected);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        isBackPressed = true;
        finishSession();
        isFanPlaying = false;
        isSpinEnabled = false;
        isTimerSet = false;
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
