package com.sleep.fan;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.Purchase;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.sleep.fan.adapter.MenuAdapter;
import com.sleep.fan.inapppurchase.BillingManager;
import com.sleep.fan.inapppurchase.iBilling;
import com.sleep.fan.model.DataModel;
import com.sleep.fan.model.ListDataModel;
import com.sleep.fan.utility.Utility;

import java.util.ArrayList;
import java.util.List;

import java.util.Timer;
import java.util.TimerTask;

public class ActivityMenu extends AppCompatActivity implements View.OnClickListener, iBilling {

    public static int screenWidth=0;
    public static int screenHeight=0;
    ListDataModel listDataModel;
    ArrayList<DataModel> listPurchased;
    BillingManager billingManager;
    androidx.appcompat.widget.Toolbar mToolbar;
    private int AD_DELAY = 1000*60*60; // 1 hr duration
    private com.google.android.gms.ads.AdRequest adRequestInter;
    private boolean isFirstTime = false;
    private ConsentInformation consentInformation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        ConsentRequestParameters params = new ConsentRequestParameters
                .Builder()
                .build();

        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                (ConsentInformation.OnConsentInfoUpdateSuccessListener) () -> {
                    // TODO: Load and show the consent form.
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                            this,
                            (ConsentForm.OnConsentFormDismissedListener) loadAndShowError -> {
                                if (loadAndShowError != null) {
                                    // Consent gathering failed.
                                    Log.w("TAG", String.format("%s: %s",
                                            loadAndShowError.getErrorCode(),
                                            loadAndShowError.getMessage()));
                                }

                                // Consent has been gathered.
                            }
                    );
                },
                (ConsentInformation.OnConsentInfoUpdateFailureListener) requestConsentError -> {
                    // Consent gathering failed.
                    Log.w("TAG", String.format("%s: %s",
                            requestConsentError.getErrorCode(),
                            requestConsentError.getMessage()));
                });
        mToolbar = (androidx.appcompat.widget.Toolbar) findViewById(R.id.custom_toolbar);
        setSupportActionBar(mToolbar);
        setTimer();
        billingManager = new BillingManager(this, this, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        showRestoreDialog();
    }

    private void showRestoreDialog() {
        if(!Utility.isNetworkConnected(this)){
            Toast.makeText(this, "Please connect to internet to restore", Toast.LENGTH_SHORT).show();
            return;
        }
        if(Utility.firstTimeAlert(this)) {
            isFirstTime = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    billingManager.initiateRestore(ActivityMenu.this);
                }
            }, 2000);

        }
    }

    public void showDialog(ListDataModel listDataModel){
        new AlertDialog.Builder(this)
                .setTitle("Restore Purchases")
                .setMessage("Do you want to restore your purchases?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(isFirstTime)
                        {
                            isFirstTime = false;
                            Utility.saveData(ActivityMenu.this, listDataModel);
                            setDataInRecyclerView();
                            Toast.makeText(ActivityMenu.this, "Purchases restored successfully.", Toast.LENGTH_LONG).show();
                        }else {
                            billingManager.initiateRestore(ActivityMenu.this);
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        billingManager = new BillingManager(this, this, this);
        listDataModel = Utility.getData(this);
        initcontrols();
    }

    Timer timer = null;
    private void setTimer() {
        timer = new Timer ();
        TimerTask hourlyTask = new TimerTask () {
            @Override
            public void run () {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        minterstitialAd = null;
                        loadInterstitialAD();
                    }
                });
            }
        };

        // schedule the task to run starting now and then {AD_DELAY} hours...
        timer.schedule (hourlyTask, 0l, AD_DELAY);
    }

    InterstitialAd minterstitialAd;
    private void loadInterstitialAD() {

        if(Utility.isNetworkConnected(this)) {

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

    private void initcontrols() {

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        ImageButton btnback = (ImageButton) findViewById(R.id.btnback);
        btnback.setVisibility(View.GONE);

        try
        {
            Class.forName("android.os.AsyncTask");   //it prevents AdMob from crashing on HTC with Android 4.0.x
            AdView mAdView = findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);

        } catch(Exception ex) {
            ex.printStackTrace();
        }

        recyclerMenu = (RecyclerView) findViewById(R.id.recyclerMenu);
        GridLayoutManager manager = new GridLayoutManager(this, 2);
        recyclerMenu.setLayoutManager(manager);
        setDataInRecyclerView();

    }

    RecyclerView recyclerMenu;
    private void setDataInRecyclerView(){
        if(listDataModel != null && listDataModel.listData!= null && listDataModel.listData.size() >0) {
            listPurchased = new ArrayList<DataModel>();
            for (int i = 0; i < listDataModel.listData.size(); i++){
                if(listDataModel.listData.get(i).isPurchased){
                    listPurchased.add(listDataModel.listData.get(i));
                }
            }
//            MenuAdapter adapter = new MenuAdapter(this, listPurchased);
//            recyclerMenu.setAdapter(adapter);
        }

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){

            case R.id.ib_close:
                dialog.dismiss();
                break;

            case R.id.btn_purchase:
                dialog.dismiss();
                break;



        }

    }

    public void showInterstitialAD(){
        if(minterstitialAd != null) {
            minterstitialAd.show(ActivityMenu.this);
        }
    }

    public void startFanActivity(int fanType){
        Intent intent = new Intent(this, ActivityFan.class);
        intent.putExtra("fanType", fanType);
        startActivity(intent);
    }

    public void PrepareFanLaunch(int position){

        if(position == listPurchased.size()-1){
            startUnPurchaseActivity();
            return;
        }
        if(position >=0 && position <=3) {
            startFanActivity(position + 1);
            showInterstitialAD();
        }
        else {
            startPurchasedActivity(listPurchased.get(position));
            showInterstitialAD();
        }

    }

    private void startPurchasedActivity(DataModel dataModel) {
        Intent intent = new Intent(this, ActivityPurchased.class);
        intent.putExtra("dataModel", dataModel);
        startActivity(intent);
    }

    private void startUnPurchaseActivity() {
        Intent intent = new Intent(this, ActivityUnpurchasedItem.class);
        startActivity(intent);
    }

    @Override
    public void onPurchasesUpdated(List<Purchase> purchases) {


    }

    @Override
    public void onRestoreUpdated(List<Purchase> purchases) {

        if(purchases != null && purchases.size() >0){
            for(Purchase p: purchases){
                for(int i=0; i<listDataModel.listData.size(); i++) {
                    if (p.getSkus().get(0).equalsIgnoreCase(listDataModel.listData.get(i).inAppID)) {
                        listDataModel.listData.get(i).isPurchased = true;
                        break;
                    }
                }
            }
            if(isFirstTime)
                this.showDialog(listDataModel);
            else {
                Utility.saveData(this, listDataModel);
                setDataInRecyclerView();
                Toast.makeText(this, "Purchases restored successfully.", Toast.LENGTH_LONG).show();
            }
        }else{
            if(isFirstTime)
                isFirstTime = false;
            Toast.makeText(this, "There are nothing to restore.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.restore:
                if(!Utility.isNetworkConnected(this)){
                    Toast.makeText(this, "Please connect to internet to restore", Toast.LENGTH_SHORT).show();
                }else showDialog(null);
                return true;
            case R.id.about:
                openAboutDialog();
                return true;
            case R.id.contact_us:
                sendEmail();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    public void sendEmail() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"feedback@yappyapps.com"});

        try {
            startActivity(Intent.createChooser(intent, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    Dialog dialog;
    public void openAboutDialog() {


        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_inapppurchase);

        Button btn_purchase = (Button) dialog.findViewById(R.id.btn_purchase);
        btn_purchase.setText("OK");
        btn_purchase.setOnClickListener(this);

        ImageButton ib_close = (ImageButton) dialog.findViewById(R.id.ib_close);
        ib_close.setOnClickListener(this);

        ImageView ivFan = (ImageView) dialog.findViewById(R.id.iv_fan);
        ivFan.setImageResource(R.mipmap.app_icon);

        TextView tv_description = (TextView) dialog.findViewById(R.id.tv_description);
        tv_description.setText("Current application version is \n "+BuildConfig.VERSION_NAME);
        tv_description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);

        dialog.show();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }
}
