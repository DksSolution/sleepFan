package com.sleep.fan.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.Purchase;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.sleep.fan.ActivityFan;
import com.sleep.fan.ActivityMenu;
import com.sleep.fan.ActivityPurchased;
import com.sleep.fan.ActivityUnpurchasedItem;
import com.sleep.fan.R;
import com.sleep.fan.activities.HomeActivity;
import com.sleep.fan.adapter.MenuAdapter;
import com.sleep.fan.databinding.FragmentFansBinding;
import com.sleep.fan.inapppurchase.BillingManager;
import com.sleep.fan.inapppurchase.iBilling;
import com.sleep.fan.model.DataModel;
import com.sleep.fan.model.ListDataModel;
import com.sleep.fan.utility.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FansFragment extends Fragment implements iBilling {
    public static int screenWidth=0;
    public static int screenHeight=0;
    ListDataModel listDataModel;
    ArrayList<DataModel> listPurchased;
    private FragmentFansBinding binding;
    BillingManager billingManager;
    private boolean isFirstTime = false;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_fans, container, false);
        View view = binding.getRoot();
        loadInterstitialAD();
        initcontrols();

        return view;
    }

    private void initcontrols() {

        GridLayoutManager manager = new GridLayoutManager(getActivity(), 2);
        binding.recyclerMenu.setLayoutManager(manager);
        setDataInRecyclerView();

    }

    @Override
    public void onResume() {
        super.onResume();
        billingManager = new BillingManager(getActivity(), getActivity(), this);
        listDataModel = Utility.getData(getContext());
        Log.e("FansFragment", "FansFragment onResume: " + listDataModel.listData.size() );
        initcontrols();
    }
    public void PrepareFanLaunch(int position){

        if(position >=0 && position <=3) {
            startFanActivity(position + 1);
            showInterstitialAD();
        }
        else {
            startPurchasedActivity(listPurchased.get(position));
            showInterstitialAD();
        }
    }

    InterstitialAd minterstitialAd;
    AdRequest adRequestInter;
    public void showInterstitialAD(){
        if(minterstitialAd != null) {
            minterstitialAd.show(requireActivity());
        }
    }
    private void loadInterstitialAD() {

        if(Utility.isNetworkConnected(requireActivity())) {

            adRequestInter = new com.google.android.gms.ads.AdRequest.Builder().build();
            InterstitialAd.load(requireActivity(), getString(R.string.interstitial_ad_id), adRequestInter, new InterstitialAdLoadCallback() {
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
    public void startFanActivity(int fanType){
        Intent intent = new Intent(requireActivity(), ActivityFan.class);
        intent.putExtra("fanType", fanType);
        startActivity(intent);
    }
    private void startPurchasedActivity(DataModel dataModel) {
        Intent intent = new Intent(requireActivity(), ActivityPurchased.class);
        intent.putExtra("dataModel", dataModel);
        startActivity(intent);
    }

    private void startUnPurchaseActivity() {
        Intent intent = new Intent(requireActivity(), ActivityUnpurchasedItem.class);
        startActivity(intent);
    }
    private void setDataInRecyclerView(){
        if(listDataModel != null && listDataModel.listData!= null && listDataModel.listData.size() >0) {
            listPurchased = new ArrayList<DataModel>();
            for (int i = 0; i < listDataModel.listData.size(); i++){
                if(listDataModel.listData.get(i).isPurchased){
                    listPurchased.add(listDataModel.listData.get(i));
                }
            }
            MenuAdapter adapter = new MenuAdapter(FansFragment.this, listPurchased);
            binding.recyclerMenu.setAdapter(adapter);
        }

    }

    @Override
    public void onPurchasesUpdated(List<Purchase> purchases) {

    }
    public void showDialog(ListDataModel listDataModel){
        new AlertDialog.Builder(getActivity())
            .setTitle("Restore Purchases")
            .setMessage("Do you want to restore your purchases?")
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if(isFirstTime)
                    {
                        isFirstTime = false;
                        Utility.saveData(requireActivity(), listDataModel);
                        setDataInRecyclerView();
                        Toast.makeText(requireActivity(), "Purchases restored successfully.", Toast.LENGTH_LONG).show();
                    }else {
                        billingManager.initiateRestore(requireActivity());
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
                Utility.saveData(requireActivity(), listDataModel);
                setDataInRecyclerView();
                Toast.makeText(requireActivity(), "Purchases restored successfully.", Toast.LENGTH_LONG).show();
            }
        }else{
            if(isFirstTime)
                isFirstTime = false;
            Toast.makeText(requireActivity(), "There are nothing to restore.", Toast.LENGTH_LONG).show();
        }
    }
}
