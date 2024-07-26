package com.sleep.fan.inapppurchase;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesResult;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.sleep.fan.ActivityUnpurchasedItem;
import com.sleep.fan.model.DataModel;
import com.sleep.fan.model.ListDataModel;
import com.sleep.fan.utility.Utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BillingManager implements PurchasesUpdatedListener {
    BillingClient billingClient;
    Context mContext;
    iBilling interfaceBilling;
    Activity mActivity;
    List<Purchase> purchases;
    public BillingManager(Activity activity, Context context, iBilling interfaceBilling){
        mActivity = activity;
        mContext = context;
        this.interfaceBilling = interfaceBilling;
        setUpBillingClient(activity);
    }

    private void setUpBillingClient(Activity activity) {
        billingClient = BillingClient.newBuilder(activity).setListener(this).enablePendingPurchases().build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                    //Toast.makeText(activity, "success", Toast.LENGTH_SHORT).show();
                }else{
                    //Toast.makeText(activity, ""+billingResult.getResponseCode(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
               // Toast.makeText(activity, "Disconnected", Toast.LENGTH_SHORT).show();

            }
        });
    }

    public void initiatePurchaseFlow(String inAppID){
        if(billingClient.isReady()) {
            SkuDetailsParams params = SkuDetailsParams.newBuilder()
                    .setSkusList(Arrays.asList(inAppID))
                    .setType(BillingClient.SkuType.INAPP)
                    .build();

            billingClient.querySkuDetailsAsync(params, new SkuDetailsResponseListener() {
                @Override
                public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                    if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                        BillingFlowParams para= BillingFlowParams.newBuilder()
                                .setSkuDetails(skuDetailsList.get(0)).build();
                        billingClient.launchBillingFlow(mActivity, para);

                    }
                }
            });
        }else{
            Toast.makeText(mActivity, "not ready yet", Toast.LENGTH_SHORT).show();
        }
    }

    public void initiateRestore(Context context){
        if(billingClient.isReady()) {
            billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, new PurchasesResponseListener() {
                @Override
                public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        interfaceBilling.onRestoreUpdated(list);
                    }else{
                        Toast.makeText(context, "Error ocoured while restore", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }



    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        switch (billingResult.getResponseCode()){
            case BillingClient.BillingResponseCode.OK:
                interfaceBilling.onPurchasesUpdated(purchases);
                acknowledgePurchases(purchases);
                break;

            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                interfaceBilling.onPurchasesUpdated(purchases);
                //Toast.makeText(mActivity, "Already owned", Toast.LENGTH_SHORT).show();
                break;

            case BillingClient.BillingResponseCode.USER_CANCELED:
                Log.d("ITEM_ALREADY_OWNED", "" + purchases);
                break;
            case BillingClient.BillingResponseCode.ERROR:
                Toast.makeText(mActivity, "Error ocourd while purchasing", Toast.LENGTH_SHORT).show();
                break;

            default:
                Log.d("OTHER ERROR", billingResult.getResponseCode()+"");
        }

    }

    private void acknowledgePurchases(List<Purchase> purchases) {
        if(purchases != null && purchases.size() >0){
            for(Purchase purchase: purchases){
                if(!purchase.isAcknowledged()) {
                    AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken()).build();
                    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
                        @Override
                        public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                           // Toast.makeText(mActivity, "Acknowledged ", Toast.LENGTH_SHORT).show();
                        }

                    };
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
                }
            }

        }
    }


}
