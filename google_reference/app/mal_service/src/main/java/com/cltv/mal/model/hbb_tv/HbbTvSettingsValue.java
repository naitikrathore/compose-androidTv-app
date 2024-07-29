package com.cltv.mal.model.hbb_tv;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum HbbTvSettingsValue implements Parcelable {
    OFF, // HbbTV is turned off
    ON;  // HbbTV is turned on

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(toInteger());
    }

    public static final Creator<HbbTvSettingsValue> CREATOR = new Creator<HbbTvSettingsValue>() {
        @Override
        public HbbTvSettingsValue createFromParcel(Parcel in) {
            return fromInteger(in.readInt());
        }

        @Override
        public HbbTvSettingsValue[] newArray(int size) {
            return new HbbTvSettingsValue[size];
        }
    };

    public int toInteger() {
        return this.ordinal();
    }

    public static HbbTvSettingsValue fromInteger(int value) {
        return values()[value];
    }
}
