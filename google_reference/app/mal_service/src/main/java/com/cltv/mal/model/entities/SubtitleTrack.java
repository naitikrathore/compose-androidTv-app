package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class SubtitleTrack implements Parcelable {
    public String id;
    public String languageName;
    public String trackName;
    public boolean isHoh;

    public SubtitleTrack() {
    }

    public SubtitleTrack(String id, String languageName, String trackName, boolean isHoh) {
        this.id = id;
        this.languageName = languageName;
        this.trackName = trackName;
        this.isHoh = isHoh;
    }

    protected SubtitleTrack(Parcel in) {
        id = in.readString();
        languageName = in.readString();
        trackName = in.readString();
        isHoh = in.readByte() != 0;
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
        dest.writeByte((byte) (isHoh ? 1 : 0));
    }

    public static final Creator<SubtitleTrack> CREATOR = new Creator<SubtitleTrack>() {
        @Override
        public SubtitleTrack createFromParcel(Parcel in) {
            return new SubtitleTrack(in);
        }

        @Override
        public SubtitleTrack[] newArray(int size) {
            return new SubtitleTrack[size];
        }
    };
}
