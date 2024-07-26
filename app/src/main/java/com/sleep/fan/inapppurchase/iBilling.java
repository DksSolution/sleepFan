package com.sleep.fan.inapppurchase;

import com.android.billingclient.api.Purchase;

import java.util.List;

public interface iBilling {

    public void onPurchasesUpdated(List<Purchase> purchases);
    public void onRestoreUpdated(List<Purchase> purchases);
}
