package com.sleep.fan.utility;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.view.Display;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.sleep.fan.model.DataModel;
import com.sleep.fan.model.ListDataModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class Utility {

    /**
     * Returns the network state of the device
     *
     * @param context Context of the activity from where it has been called
     * @return Returns true if network is available and connected else false.
     */
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static String loadJSONFromAsset(Activity activity) {
        String json = null;
        try {
            InputStream is = activity.getAssets().open("data.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public static void parseData(Activity activity){
        ListDataModel listDataModel = new ListDataModel();
        //ArrayList<DataModel> listData = new ArrayList<DataModel>();
        try{
            JSONArray jsonArray = new JSONArray(loadJSONFromAsset(activity));
            for (int i = 0; i < jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                DataModel model = new DataModel();
                model.position = jsonObject.getInt("position");
                model.inAppID = jsonObject.getString("inAppID");
                model.name = jsonObject.getString("name");
                model.isPurchased = jsonObject.getBoolean("isPurchased");
                model.icon = jsonObject.getString("icon");
                model.soundID = jsonObject.getString("soundID");
                listDataModel.listData.add(model);
            }

        }catch (Exception ex){

        }
        saveData(activity, listDataModel);
    }

    public static void saveData(Context context, @NonNull Object object) {
        SharedPreferences sharedPref = context.getSharedPreferences("prefName", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String json = new Gson().toJson(object);
        editor.putString("Obj_data", json);
        editor.apply();
    }

    public static ListDataModel getData(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences("prefName", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String json = sharedPref.getString("Obj_data", "");
        ListDataModel listData = new Gson().fromJson(json, ListDataModel.class);
        return listData;
    }

    public static boolean firstTimeAlert(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences("prefName", Context.MODE_PRIVATE);
        if(!sharedPref.contains("dontshow")){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("dontshow", true);
            editor.commit();
            return true;

        }else
            return false;
    }

    /**
     * Is the screen of the device on.
     * @param context the context
     * @return true when (at least one) screen is on
     */
    public static boolean isScreenOn(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            boolean screenOn = false;
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    screenOn = true;
                }
            }
            return screenOn;
        } else {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            //noinspection deprecation
            return pm.isScreenOn();
        }

    }

}
