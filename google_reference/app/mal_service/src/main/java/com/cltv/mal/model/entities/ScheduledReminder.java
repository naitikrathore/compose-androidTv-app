package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class ScheduledReminder implements Parcelable {

    int id;
    String name;
    TvChannel tvChannel;
    TvEvent tvEvent;
    long startTime;
    int tvChannelId;
    int tvEventId;

    public ScheduledReminder(int id, String name, TvChannel tvChannel, TvEvent tvEvent, long startTime, int tvChannelId, int tvEventId) {
        this.id = id;
        this.name = name;
        this.tvChannel = tvChannel;
        this.tvEvent = tvEvent;
        this.startTime = startTime;
        this.tvChannelId = tvChannelId;
        this.tvEventId = tvEventId;
    }

    public ScheduledReminder(int id, String name, TvChannel tvChannel, TvEvent tvEvent) {
        this.id = id;
        this.name = name;
        this.tvChannel = tvChannel;
        this.tvEvent = tvEvent;
    }

    protected ScheduledReminder(Parcel in) {
        id = in.readInt();
        name = in.readString();
        tvChannel = in.readParcelable(TvChannel.class.getClassLoader());
        tvEvent = in.readParcelable(TvEvent.class.getClassLoader());
        startTime = in.readLong();
        tvChannelId = in.readInt();
        tvEventId = in.readInt();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TvChannel getTvChannel() {
        return tvChannel;
    }

    public void setTvChannel(TvChannel tvChannel) {
        this.tvChannel = tvChannel;
    }

    public TvEvent getTvEvent() {
        return tvEvent;
    }

    public void setTvEvent(TvEvent tvEvent) {
        this.tvEvent = tvEvent;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public int getTvChannelId() {
        return tvChannelId;
    }

    public void setTvChannelId(int tvChannelId) {
        this.tvChannelId = tvChannelId;
    }

    public int getTvEventId() {
        return tvEventId;
    }

    public void setTvEventId(int tvEventId) {
        this.tvEventId = tvEventId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeParcelable(tvChannel, 0);
        dest.writeParcelable(tvEvent, 0);
        dest.writeLong(startTime);
        dest.writeInt(tvChannelId);
        dest.writeInt(tvEventId);
    }

    public static final Creator<ScheduledReminder> CREATOR = new Creator<ScheduledReminder>() {
        @Override
        public ScheduledReminder createFromParcel(Parcel in) {
            return new ScheduledReminder(in);
        }

        @Override
        public ScheduledReminder[] newArray(int size) {
            return new ScheduledReminder[size];
        }
    };
}
