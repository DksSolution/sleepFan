package com.sleep.fan.model;

import android.os.Parcel;
import android.os.Parcelable;

public class DataModel implements Parcelable {
    public String inAppID, name, icon, soundID;
    public boolean isPurchased;
    public int position;

    public DataModel(Parcel in) {
        inAppID = in.readString();
        name = in.readString();
        icon = in.readString();
        soundID = in.readString();
        isPurchased = in.readByte() != 0;
        position = in.readInt();
    }

    public static final Creator<DataModel> CREATOR = new Creator<DataModel>() {
        @Override
        public DataModel createFromParcel(Parcel in) {
            return new DataModel(in);
        }

        @Override
        public DataModel[] newArray(int size) {
            return new DataModel[size];
        }
    };

    public DataModel() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(inAppID);
        dest.writeString(name);
        dest.writeString(icon);
        dest.writeString(soundID);
        dest.writeByte((byte) (isPurchased ? 1 : 0));
        dest.writeInt(position);
    }
}
