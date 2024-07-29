package com.cltv.mal.model.entities;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.cltv.mal.model.fast.RecommendationItem;

import java.util.List;

public class RailItem implements Parcelable {

    /**
     * used to distinguish Rail types in RailAdapter.
     */
    enum RailItemType {
        EVENT,
        CHANNEL,
        RECORDING,
        SCHEDULED_RECORDING;
    }

    int id = -1;
    String railName = "";
    List<TvEvent> rail;
    int type = 0;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRailName() {
        return railName;
    }

    public void setRailName(String railName) {
        this.railName = railName;
    }

    public List<TvEvent> getRail() {
        return rail;
    }

    public void setRail(List<TvEvent> rail) {
        this.rail = rail;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public RailItem(int id, String railName, List<TvEvent> rail, int type) {
        this.id = id;
        this.railName = railName;
        this.rail = rail;
        this.type = type;
    }

    protected RailItem(Parcel in) {
        id = in.readInt();
        railName = in.readString();
        rail = in.createTypedArrayList(TvEvent.CREATOR);
        type = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(railName);
        dest.writeTypedList(rail);
        dest.writeInt(type);
    }

    public static final Creator<RailItem> CREATOR = new Creator<RailItem>() {
        @Override
        public RailItem createFromParcel(Parcel in) {
            return new RailItem(in);
        }

        @Override
        public RailItem[] newArray(int size) {
            return new RailItem[size];
        }
    };
}
