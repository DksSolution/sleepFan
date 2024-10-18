package com.sleep.fan.fragments;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.Purchase;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
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
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.sleep.fan.ActivityUnpurchasedItem;
import com.sleep.fan.R;
import com.sleep.fan.activities.HomeActivity;
import com.sleep.fan.adapter.UnPurchasedAdapter;
import com.sleep.fan.databinding.FragmentFansBinding;
import com.sleep.fan.inapppurchase.BillingManager;
import com.sleep.fan.inapppurchase.iBilling;
import com.sleep.fan.model.DataModel;
import com.sleep.fan.model.ListDataModel;
import com.sleep.fan.utility.Utility;

import java.util.ArrayList;
import java.util.List;

public class SoundsFragment extends Fragment implements iBilling, Player.Listener, View.OnClickListener {
    ListDataModel listDataModel;
    ArrayList<DataModel> listUnPurchased;
    BillingManager billingManager;
    private FragmentFansBinding binding;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_fans, container, false);
        View view = binding.getRoot();
        listDataModel = Utility.getData(requireActivity());
        initializeControls();
        billingManager = new BillingManager(requireActivity(), requireActivity(), this);

        return view;
    }

    private void initializeControls() {
        LinearLayoutManager manager = new LinearLayoutManager(requireContext());
        binding.recyclerMenu.setLayoutManager(manager);

        if(listDataModel != null && listDataModel.listData!= null && listDataModel.listData.size() >0) {
            listUnPurchased = new ArrayList<DataModel>();
            for (int i = 0; i < listDataModel.listData.size(); i++){
                if(!listDataModel.listData.get(i).isPurchased){
                    listUnPurchased.add(listDataModel.listData.get(i));
                }
            }
            UnPurchasedAdapter adapter = new UnPurchasedAdapter(this, listUnPurchased);
            binding.recyclerMenu.setAdapter(adapter);
        }


    }

    int positionModel, positionAdapter;
    Dialog dialog;
    public void openDemoDialog(int posModel, int position) {

        positionModel = posModel;
        positionAdapter = position;

        dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_inapppurchase);

        Button btn_purchase = (Button) dialog.findViewById(R.id.btn_purchase);
        btn_purchase.setOnClickListener(this);

        ImageButton ib_close = (ImageButton) dialog.findViewById(R.id.ib_close);
        ib_close.setOnClickListener(this);

        ImageView ivFan = (ImageView) dialog.findViewById(R.id.iv_fan);
        ivFan.setImageResource(getResources().getIdentifier("@drawable/" + listUnPurchased.get(position).icon, null, getContext().getPackageName()));

        dialog.show();
        startDemoPlayer(position);

    }

    ExoPlayer player;
    private void startDemoPlayer(int position) {

        TrackSelector trackSelector = new DefaultTrackSelector();
        LoadControl loadControl = new DefaultLoadControl();
        player = new ExoPlayer.Builder(requireContext())
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build();

// Add the listener
        player.addListener(this);
        int trackID = getResources().getIdentifier("@raw/"+listUnPurchased.get(position).soundID, null, getContext().getPackageName());
        setDataSpec(trackID);
    }

    LoopingMediaSource loopingMediaSource;
    boolean isPlaying;
    public void setDataSpec(int trackID){
        DataSpec dataSpec;
        dataSpec = new DataSpec(RawResourceDataSource.buildRawResourceUri(trackID));
        final RawResourceDataSource rawResourceDataSource = new RawResourceDataSource(requireContext());
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

        Uri uri = rawResourceDataSource.getUri();
        MediaItem mediaItem = MediaItem.fromUri(uri);

// Create a MediaSource using the ProgressiveMediaSource.Factory for extracting audio
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(requireContext());
        MediaSource audioSource = new ProgressiveMediaSource.Factory(dataSourceFactory, WavExtractor.FACTORY)
            .createMediaSource(mediaItem);

// Prepare the player with the media source
        player.setMediaSource(audioSource);

// Set repeat mode (equivalent to LoopingMediaSource)
        player.setRepeatMode(Player.REPEAT_MODE_ALL);

// Prepare and start playback
        player.prepare();
        player.setPlayWhenReady(true);
        player.seekTo(0);
        player.setVolume(0.9f);

// Set playing state and start timer
        isPlaying = true;
        timerStart(60 * 1000);
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
    public void onPurchasesUpdated(List<Purchase> purchases) {
        if(purchases != null && purchases.size() >0){
            for(Purchase p: purchases){
                if(p.getSkus().get(0).equalsIgnoreCase(listDataModel.listData.get(positionModel-1).inAppID)) {
                    listDataModel.listData.get(positionModel-1).isPurchased = true;
                    Utility.saveData(requireContext(),listDataModel);
                    Toast.makeText(requireActivity(), "Your purchases has been successful.", Toast.LENGTH_SHORT).show();
//                    finish();
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
