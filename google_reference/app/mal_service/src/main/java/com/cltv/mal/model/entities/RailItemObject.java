package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class RailItemObject extends Object implements Parcelable {

    protected RailItemObject(Parcel in) {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {

    }

    public final Parcelable.Creator<RailItemObject> CREATOR = new Parcelable.Creator<RailItemObject>() {
        @Override
        public RailItemObject createFromParcel(Parcel in) {
            return new RailItemObject(in);
        }

        @Override
        public RailItemObject[] newArray(int size) {
            return new RailItemObject[size];
        }
    };
}
