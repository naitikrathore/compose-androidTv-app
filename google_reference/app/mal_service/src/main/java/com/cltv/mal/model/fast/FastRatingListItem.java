package com.cltv.mal.model.fast;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class FastRatingListItem implements Parcelable {

    String abbreviation;
    int level;
    String name;

    public FastRatingListItem(String abbreviation, int level, String name) {
        this.abbreviation = abbreviation;
        this.level = level;
        this.name = name;
    }

    protected FastRatingListItem(Parcel in) {
        in.readString();
        in.readInt();
        in.readString();
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(abbreviation);
        dest.writeInt(level);
        dest.writeString(name);
    }

    public static final Creator<FastRatingListItem> CREATOR = new Creator<FastRatingListItem>() {
        @Override
        public FastRatingListItem createFromParcel(Parcel in) {
            return new FastRatingListItem(in);
        }

        @Override
        public FastRatingListItem[] newArray(int size) {
            return new FastRatingListItem[size];
        }
    };
}
