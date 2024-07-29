package com.cltv.mal.model.prefs;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum PrefMenu implements Parcelable {
    SETUP,
    AUDIO,
    SUBTITLE,
    HBBTV,
    CAMINFO,
    TELETEXT,
    SYSTEMINFO,
    PARENTAL_CONTROL,
    PVR_TIMESHIFT,
    CLOSED_CAPTIONS,
    ADS_TARGETING,
    OPEN_SOURCE_LICENSES;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(toInteger());
    }

    public static final Creator<PrefMenu> CREATOR = new Creator<PrefMenu>() {
        @Override
        public PrefMenu createFromParcel(Parcel in) {
            return fromInteger(in.readInt());
        }

        @Override
        public PrefMenu[] newArray(int size) {
            return new PrefMenu[size];
        }
    };

    public int toInteger() {
        return this.ordinal();
    }

    public static PrefMenu fromInteger(int value) {
        return values()[value];
    }
}
