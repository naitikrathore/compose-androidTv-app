package com.cltv.mal.model.fast;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class RecommendationRow implements Parcelable {
    String name;
    ArrayList<RecommendationItem> items;

    public RecommendationRow(String name, ArrayList<RecommendationItem> items) {
        this.name = name;
        this.items = items;
    }

    protected RecommendationRow(Parcel in) {
        name = in.readString();
        items = in.createTypedArrayList(RecommendationItem.CREATOR);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<RecommendationItem> getItems() {
        return items;
    }

    public void setItems(ArrayList<RecommendationItem> items) {
        this.items = items;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeTypedList(items);
    }

    public static final Creator<RecommendationRow> CREATOR = new Creator<RecommendationRow>() {
        @Override
        public RecommendationRow createFromParcel(Parcel in) {
            return new RecommendationRow(in);
        }

        @Override
        public RecommendationRow[] newArray(int size) {
            return new RecommendationRow[size];
        }
    };
}
