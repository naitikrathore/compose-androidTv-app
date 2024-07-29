package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class FavoriteItem implements Parcelable {
    int id;
    TvChannel tvChannel;
    ArrayList<String> favListIds;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TvChannel getTvChannel() {
        return tvChannel;
    }

    public void setTvChannel(TvChannel tvChannel) {
        this.tvChannel = tvChannel;
    }

    public ArrayList<String> getFavListIds() {
        return favListIds;
    }

    public void setFavListIds(ArrayList<String> favListIds) {
        this.favListIds = favListIds;
    }

    public FavoriteItem(int id, TvChannel tvChannel, ArrayList<String> favListIds) {
        this.id = id;
        this.tvChannel = tvChannel;
        this.favListIds = favListIds;
    }

    protected FavoriteItem(Parcel in) {
        id = in.readInt();
        tvChannel = in.readParcelable(TvChannel.class.getClassLoader());
        favListIds = in.createStringArrayList();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeParcelable(tvChannel, 0);
        dest.writeStringList(favListIds);
    }

    public static final Creator<FavoriteItem> CREATOR = new Creator<FavoriteItem>() {
        @Override
        public FavoriteItem createFromParcel(Parcel in) {
            return new FavoriteItem(in);
        }

        @Override
        public FavoriteItem[] newArray(int size) {
            return new FavoriteItem[size];
        }
    };
}
