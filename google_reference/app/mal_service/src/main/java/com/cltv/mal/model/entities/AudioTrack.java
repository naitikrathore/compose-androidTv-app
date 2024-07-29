package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class AudioTrack implements Parcelable {
    public String id;
    public String languageName;
    public String trackName;
    public boolean isAnalogTrack;
    public String analogName;
    public boolean isAd;
    public boolean isDolby;

    public AudioTrack() {
    }

    public AudioTrack(String id, String languageName, String trackName, boolean isAnalogTrack, String analogName, boolean isAd, boolean isDolby) {
        this.id = id;
        this.languageName = languageName;
        this.trackName = trackName;
        this.isAnalogTrack = isAnalogTrack;
        this.analogName = analogName;
        this.isAd = isAd;
        this.isDolby = isDolby;
    }

    protected AudioTrack(Parcel in) {
        id = in.readString();
        languageName = in.readString();
        trackName = in.readString();
        isAnalogTrack = in.readByte() != 0;
        analogName = in.readString();
        isAd = in.readByte() != 0;
        isDolby = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(languageName);
        dest.writeString(trackName);
        dest.writeByte((byte) (isAnalogTrack ? 1 : 0));
        dest.writeString(analogName);
        dest.writeByte((byte) (isAd ? 1 : 0));
        dest.writeByte((byte) (isDolby ? 1 : 0));
    }

    public static final Creator<AudioTrack> CREATOR = new Creator<AudioTrack>() {
        @Override
        public AudioTrack createFromParcel(Parcel in) {
            return new AudioTrack(in);
        }

        @Override
        public AudioTrack[] newArray(int size) {
            return new AudioTrack[size];
        }
    };
}
