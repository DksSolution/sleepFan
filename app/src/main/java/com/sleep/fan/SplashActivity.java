package com.sleep.fan;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.sleep.fan.activities.HomeActivity;
import com.sleep.fan.inapppurchase.BillingManager;
import com.sleep.fan.utility.AdModConfiguration;
import com.sleep.fan.utility.Utility;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Timer;

public class SplashActivity extends AppCompatActivity {

    private int SPLASh_DELAY = 3000;
    private int AD_DURATION = 10000;
    private Timer timerAd;
    private boolean loaded=false;
    private com.google.android.gms.ads.AdRequest adRequestInter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if(Utility.getData(this) == null){
            Utility.parseData(this);
        }

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {}
        });
        
        setSplashDelay();
       // configureInterstitialAd();

    }

    private void configureInterstitialAd() {

        try
        {
            Class.forName("android.os.AsyncTask");   //it prevents AdMob from crashing on HTC with Android 4.0.x
        }
        catch(Throwable ignored)
        {}

       // timerAd=new Timer();

        try{
            if(Utility.isNetworkConnected(this)){
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(!loaded){
                            launchMenuScreen();
                            loaded = true;
                        }
                    }
                }, AD_DURATION);

                launchInterstitialAd();

            }else{
                setSplashDelay();
            }

        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void launchInterstitialAd() {
        adRequestInter = new com.google.android.gms.ads.AdRequest.Builder().build();
        final InterstitialAd[] minterstitialAd = new InterstitialAd[1];
        InterstitialAd.load(this, getString(R.string.interstitial_ad_id), adRequestInter, new InterstitialAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e("Interstitial ad failed", loadAdError.toString());
                super.onAdFailedToLoad(loadAdError);
            }

            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                Log.e("Interstitial ad success", "Loaded");
                minterstitialAd[0] = interstitialAd;
            }
        });


        //interstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");

    }

    private void setSplashDelay() {

        /*if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }*/
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                launchMenuScreen();
            }
        }, SPLASh_DELAY);


    }

    private void launchMenuScreen(){
        Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}
