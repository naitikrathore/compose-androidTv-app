package com.cltv.mal.model.pvr;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum RepeatFlag implements Parcelable {
    NONE,
    DAILY,
    WEEKLY;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(toInteger());
    }

    public static final Creator<RepeatFlag> CREATOR = new Creator<RepeatFlag>() {
        @Override
        public RepeatFlag createFromParcel(Parcel in) {
            return fromInteger(in.readInt());
        }

        @Override
        public RepeatFlag[] newArray(int size) {
            return new RepeatFlag[size];
        }
    };

    public int toInteger() {
        return this.ordinal();
    }

    public static RepeatFlag fromInteger(int value) {
        return values()[value];
    }
}
