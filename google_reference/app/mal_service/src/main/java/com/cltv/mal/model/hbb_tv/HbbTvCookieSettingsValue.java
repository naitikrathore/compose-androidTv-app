package com.cltv.mal.model.hbb_tv;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum HbbTvCookieSettingsValue implements Parcelable {
    BLOCK_ALL(0),         // Block all HbbTV cookies
    BLOCK_3RD_PARTY(1),   // Block 3rd-party HbbTV cookies
    DEFAULT(2);           // Use the default HbbTV cookie settings

    public int value = -1;

    private HbbTvCookieSettingsValue(int value) {
        this.value = value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(toInteger());
    }

    public static final Creator<HbbTvCookieSettingsValue> CREATOR = new Creator<HbbTvCookieSettingsValue>() {
        @Override
        public HbbTvCookieSettingsValue createFromParcel(Parcel in) {
            return fromInteger(in.readInt());
        }

        @Override
        public HbbTvCookieSettingsValue[] newArray(int size) {
            return new HbbTvCookieSettingsValue[size];
        }
    };

    public int toInteger() {
        return this.ordinal();
    }

    public static HbbTvCookieSettingsValue fromInteger(int value) {
        return values()[value];
    }
}
