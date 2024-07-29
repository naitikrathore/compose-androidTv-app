package com.cltv.mal.model.ci_plus;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;


public enum CachedPinResult implements Parcelable {
    CACHED_PIN_OK,
    CACHED_PIN_NOT_SUPPORTED,
    CACHED_PIN_RETRY,
    CACHED_PIN_FAIL;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(toInteger());
    }

    public static final Parcelable.Creator<CachedPinResult> CREATOR = new Parcelable.Creator<CachedPinResult>() {
        @Override
        public CachedPinResult createFromParcel(Parcel in) {
            return fromInteger(in.readInt());
        }

        @Override
        public CachedPinResult[] newArray(int size) {
            return new CachedPinResult[size];
        }
    };

    public int toInteger() {
        return this.ordinal();
    }

    public static CachedPinResult fromInteger(int value) {
        return values()[value];
    }
}
