package com.cltv.mal.model.ci_plus;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum CamTypePreference implements Parcelable {
    PCMCIA,
    USB;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(toInteger());
    }

    public static final Parcelable.Creator<CamTypePreference> CREATOR = new Parcelable.Creator<CamTypePreference>() {
        @Override
        public CamTypePreference createFromParcel(Parcel in) {
            return fromInteger(in.readInt());
        }

        @Override
        public CamTypePreference[] newArray(int size) {
            return new CamTypePreference[size];
        }
    };

    public int toInteger() {
        return this.ordinal();
    }

    public static CamTypePreference fromInteger(int value) {
        return values()[value];
    }
}
