package com.cltv.mal.model.hbb_tv;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum HbbTvKeyEventType implements Parcelable {
    KEY_DOWN,  // Key down event
    KEY_UP;     // Key up event

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(toInteger());
    }

    public static final Creator<HbbTvKeyEventType> CREATOR = new Creator<HbbTvKeyEventType>() {
        @Override
        public HbbTvKeyEventType createFromParcel(Parcel in) {
            return fromInteger(in.readInt());
        }

        @Override
        public HbbTvKeyEventType[] newArray(int size) {
            return new HbbTvKeyEventType[size];
        }
    };

    public int toInteger() {
        return this.ordinal();
    }

    public static HbbTvKeyEventType fromInteger(int value) {
        return values()[value];
    }
}
