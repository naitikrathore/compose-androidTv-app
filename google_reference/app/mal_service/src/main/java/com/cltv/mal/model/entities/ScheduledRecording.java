package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.cltv.mal.model.pvr.RepeatFlag;

public class ScheduledRecording implements Parcelable {

    int id;
    String name;
    long scheduledDateStart;
    long scheduledDateEnd;
    int tvChannelId;
    int tvEventId;
    RepeatFlag repeatFreq;
    TvChannel tvChannel;
    TvEvent tvEvent;

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

    public long getScheduledDateStart() {
        return scheduledDateStart;
    }

    public void setScheduledDateStart(long scheduledDateStart) {
        this.scheduledDateStart = scheduledDateStart;
    }

    public long getScheduledDateEnd() {
        return scheduledDateEnd;
    }

    public void setScheduledDateEnd(long scheduledDateEnd) {
        this.scheduledDateEnd = scheduledDateEnd;
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

    public RepeatFlag getRepeatFlag() {
        return repeatFreq;
    }

    public void setRepeatFlag(RepeatFlag repeatFreq) {
        this.repeatFreq = repeatFreq;
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

    public ScheduledRecording(int id, String name, long scheduledDateStart, long scheduledDateEnd, int tvChannelId, int tvEventId, RepeatFlag repeatFreq, TvChannel tvChannel, TvEvent tvEvent) {
        this(id, name, scheduledDateStart, scheduledDateEnd, tvChannelId, tvEventId, repeatFreq);
        this.tvChannel = tvChannel;
        this.tvEvent = tvEvent;
    }

    public ScheduledRecording(int id, String name, long scheduledDateStart, long scheduledDateEnd, int tvChannelId, int tvEventId, RepeatFlag repeatFreq) {
        this.id = id;
        this.name = name;
        this.scheduledDateStart = scheduledDateStart;
        this.scheduledDateEnd = scheduledDateEnd;
        this.tvChannelId = tvChannelId;
        this.tvEventId = tvEventId;
        this.repeatFreq = repeatFreq;
    }

    public ScheduledRecording(Parcel in) {
        id = in.readInt();
        name = in.readString();
        scheduledDateStart = in.readLong();
        scheduledDateEnd = in.readLong();
        tvChannelId = in.readInt();
        tvEventId = in.readInt();
        repeatFreq = in.readParcelable(RepeatFlag.class.getClassLoader());
        tvChannel = in.readParcelable(TvChannel.class.getClassLoader());
        tvEvent = in.readParcelable(TvEvent.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeLong(scheduledDateStart);
        dest.writeLong(scheduledDateEnd);
        dest.writeInt(tvChannelId);
        dest.writeInt(tvEventId);
        dest.writeParcelable(repeatFreq, 0);
        dest.writeParcelable(tvChannel, 0);
        dest.writeParcelable(tvEvent, 0);
    }

    public static final Creator<ScheduledRecording> CREATOR = new Creator<ScheduledRecording>() {
        @Override
        public ScheduledRecording createFromParcel(Parcel in) {
            return new ScheduledRecording(in);
        }

        @Override
        public ScheduledRecording[] newArray(int size) {
            return new ScheduledRecording[size];
        }
    };
}
