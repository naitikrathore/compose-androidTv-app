package com.cltv.mal.model.hbb_tv;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum HbbTvSettingsWithDefaultValue implements Parcelable {
    OFF,      // HbbTV is turned off
    ON,       // HbbTV is turned on
    DEFAULT;   // Use the default HbbTV setting

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(toInteger());
    }

    public static final Creator<HbbTvSettingsWithDefaultValue> CREATOR = new Creator<HbbTvSettingsWithDefaultValue>() {
        @Override
        public HbbTvSettingsWithDefaultValue createFromParcel(Parcel in) {
            return fromInteger(in.readInt());
        }

        @Override
        public HbbTvSettingsWithDefaultValue[] newArray(int size) {
            return new HbbTvSettingsWithDefaultValue[size];
        }
    };

    public int toInteger() {
        return this.ordinal();
    }

    public static HbbTvSettingsWithDefaultValue fromInteger(int value) {
        return values()[value];
    }
}
