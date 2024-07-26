package com.sleep.fan.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class ListDataModel implements Parcelable {

    public ArrayList<DataModel> listData;

    public ListDataModel(Parcel in) {
        listData = in.createTypedArrayList(DataModel.CREATOR);
    }

    public ListDataModel() {
        listData = new ArrayList<DataModel>();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(listData);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ListDataModel> CREATOR = new Creator<ListDataModel>() {
        @Override
        public ListDataModel createFromParcel(Parcel in) {
            return new ListDataModel(in);
        }

        @Override
        public ListDataModel[] newArray(int size) {
            return new ListDataModel[size];
        }
    };
}

