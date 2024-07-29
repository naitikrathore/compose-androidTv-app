package com.cltv.mal.model.ci_plus;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum PlatformSpecificOperation implements Parcelable {
    PSO_RESET_MMI_INTERFACE;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(toInteger());
    }

    public static final Parcelable.Creator<PlatformSpecificOperation> CREATOR = new Parcelable.Creator<PlatformSpecificOperation>() {
        @Override
        public PlatformSpecificOperation createFromParcel(Parcel in) {
            return fromInteger(in.readInt());
        }

        @Override
        public PlatformSpecificOperation[] newArray(int size) {
            return new PlatformSpecificOperation[size];
        }
    };

    public int toInteger() {
        return this.ordinal();
    }

    public static PlatformSpecificOperation fromInteger(int value) {
        return values()[value];
    }
}