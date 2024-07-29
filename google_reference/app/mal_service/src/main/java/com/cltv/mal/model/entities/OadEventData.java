package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;


public class OadEventData implements Parcelable {

    int oadEventValue;
    int progress;
    int version;

    public int getOadEventValue() {
        return oadEventValue;
    }

    public int getProgress() {
        return progress;
    }

    public int getVersion() {
        return version;
    }

    protected OadEventData(Parcel in) {
        oadEventValue = in.readInt();
        progress = in.readInt();
        version = in.readInt();
    }

    public OadEventData(int oadEventValue, int progress, int version) {
        this.oadEventValue = oadEventValue;
        this.progress = progress;
        this.version = version;
    }

    public static final Creator<OadEventData> CREATOR = new Creator<OadEventData>() {
        @Override
        public OadEventData createFromParcel(Parcel in) {
            return new OadEventData(in);
        }

        @Override
        public OadEventData[] newArray(int size) {
            return new OadEventData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeInt(oadEventValue);
        parcel.writeInt(progress);
        parcel.writeInt(version);
    }
}