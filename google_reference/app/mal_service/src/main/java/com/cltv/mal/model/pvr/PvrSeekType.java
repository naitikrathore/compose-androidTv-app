package com.cltv.mal.model.pvr;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum PvrSeekType implements Parcelable {
    FF_SEEK, FR_SEEK, REGULAR_SEEK;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(toInteger());
    }

    public static final Creator<PvrSeekType> CREATOR = new Creator<PvrSeekType>() {
        @Override
        public PvrSeekType createFromParcel(Parcel in) {
            return fromInteger(in.readInt());
        }

        @Override
        public PvrSeekType[] newArray(int size) {
            return new PvrSeekType[size];
        }
    };

    public int toInteger() {
        return this.ordinal();
    }

    public static PvrSeekType fromInteger(int value) {
        return values()[value];
    }
}
