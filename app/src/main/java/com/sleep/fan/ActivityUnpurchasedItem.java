package com.sleep.fan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.wav.WavExtractor;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.sleep.fan.adapter.UnPurchasedAdapter;
import com.sleep.fan.inapppurchase.BillingManager;
import com.sleep.fan.inapppurchase.iBilling;
import com.sleep.fan.model.DataModel;
import com.sleep.fan.model.ListDataModel;
import com.sleep.fan.utility.Utility;

import java.util.ArrayList;
import java.util.List;

public class ActivityUnpurchasedItem extends AppCompatActivity implements iBilling, View.OnClickListener, Player.EventListener {

    ListDataModel listDataModel;
    ArrayList<DataModel> listUnPurchased;
    BillingManager billingManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unpurchased_item);
        listDataModel = Utility.getData(this);
        initializeControls();
        billingManager = new BillingManager(this, this, this);
    }

    private void initializeControls() {
        try
        {
            Class.forName("android.os.AsyncTask");   //it prevents AdMob from crashing on HTC with Android 4.0.x
            AdView mAdView = findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);

        } catch(Exception ex) {
            ex.printStackTrace();
        }


        RecyclerView recyclerUnpurchased = (RecyclerView) findViewById(R.id.recyclerUnpurchased);
        GridLayoutManager manager = new GridLayoutManager(this, 2);
        recyclerUnpurchased.setLayoutManager(manager);

        if(listDataModel != null && listDataModel.listData!= null && listDataModel.listData.size() >0) {
            listUnPurchased = new ArrayList<DataModel>();
            for (int i = 0; i < listDataModel.listData.size(); i++){
                if(!listDataModel.listData.get(i).isPurchased){
                    listUnPurchased.add(listDataModel.listData.get(i));
                }
            }
//            UnPurchasedAdapter adapter = new UnPurchasedAdapter(this, listUnPurchased);
//            recyclerUnpurchased.setAdapter(adapter);
        }


    }

    int positionModel, positionAdapter;
    Dialog dialog;
    public void openDemoDialog(int posModel, int position) {

        positionModel = posModel;
        positionAdapter = position;

        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_inapppurchase);

        Button btn_purchase = (Button) dialog.findViewById(R.id.btn_purchase);
        btn_purchase.setOnClickListener(this);

        ImageButton ib_close = (ImageButton) dialog.findViewById(R.id.ib_close);
        ib_close.setOnClickListener(this);

        ImageView ivFan = (ImageView) dialog.findViewById(R.id.iv_fan);
        ivFan.setImageResource(getResources().getIdentifier("@drawable/" + listUnPurchased.get(position).icon, null, getPackageName()));

        dialog.show();
        startDemoPlayer(position);

    }

    SimpleExoPlayer player;
    private void startDemoPlayer(int position) {

        TrackSelector trackSelector = new DefaultTrackSelector();
        LoadControl loadControl = new DefaultLoadControl();
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
        player.addListener(this);
        int trackID = getResources().getIdentifier("@raw/"+listUnPurchased.get(position).soundID, null, getPackageName());
        setDataSpec(trackID);
    }

    LoopingMediaSource loopingMediaSource;
    boolean isPlaying;
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

        MediaSource audioSource = new ExtractorMediaSource(rawResourceDataSource.getUri(),
                factory, WavExtractor.FACTORY, null, null);
        loopingMediaSource = new LoopingMediaSource(audioSource);

        player.prepare(loopingMediaSource);
        player.setPlayWhenReady(true);
        player.seekTo(0);
        player.setVolume(0.9f);
        isPlaying = true;
        timerStart(60*1000);
    }

    CountDownTimer countDownTimer;
    public void timerStart(long millisInFuture){
        countDownTimer = new CountDownTimer(millisInFuture, 1000){
            @Override
            public void onFinish()
            {
                stopMediaPlayer();
            }

            @Override
            public void onTick(long millisUntilFinished) {
            }
        };

        countDownTimer.start();

    }

    private void stopMediaPlayer() {
        if (player != null  && isPlaying) {
            player.setPlayWhenReady(false);
            isPlaying = false;
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onPurchasesUpdated(List<Purchase> purchases) {
        if(purchases != null && purchases.size() >0){
            for(Purchase p: purchases){
                if(p.getSkus().get(0).equalsIgnoreCase(listDataModel.listData.get(positionModel-1).inAppID)) {
                    listDataModel.listData.get(positionModel-1).isPurchased = true;
                    Utility.saveData(this,listDataModel);
                    Toast.makeText(ActivityUnpurchasedItem.this, "Your purchases has been successful.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }

    }

    @Override
    public void onRestoreUpdated(List<Purchase> purchases) {

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btn_purchase:
                billingManager.initiatePurchaseFlow(listDataModel.listData.get(positionModel-1).inAppID);
                closeDialog();
                break;
            case R.id.ib_close:
                closeDialog();
                break;

        }
    }

    public void closeDialog(){
        dialog.dismiss();
        stopMediaPlayer();
    }
}
