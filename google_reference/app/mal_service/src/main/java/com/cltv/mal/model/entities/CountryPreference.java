package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum CountryPreference implements Parcelable {
    HIDE_LOCKED_SERVICES_IN_EPG,
    DISABLE_ZERO_PIN,
    HIDE_LOCKED_RECORDINGS,
    ENABLE_HBBTV_BY_DEFAULT,
    USE_HIDDEN_SERVICE_FLAG,
    SHOW_DEFAULT_PIN_TIP,
    DISABLE_ZERO_DIAL;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(toInteger());
    }

    public static final Creator<CountryPreference> CREATOR = new Creator<CountryPreference>() {
        @Override
        public CountryPreference createFromParcel(Parcel in) {
            return fromInteger(in.readInt());
        }

        @Override
        public CountryPreference[] newArray(int size) {
            return new CountryPreference[size];
        }
    };

    public int toInteger() {
        return this.ordinal();
    }

    public static CountryPreference fromInteger(int value) {
        return values()[value];
    }
}
