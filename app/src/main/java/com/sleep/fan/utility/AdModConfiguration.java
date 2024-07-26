package com.sleep.fan.utility;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.sleep.fan.R;

public class AdModConfiguration {

    public static InterstitialAd minterstitialAd;

    public static void configureBannerAds(){

    }

    public static void configureInterstitialAds(Context context){
        AdRequest adRequestInter = new AdRequest.Builder().build();
        InterstitialAd.load(context, context.getString(R.string.interstitial_ad_id), adRequestInter, new InterstitialAdLoadCallback() {
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
