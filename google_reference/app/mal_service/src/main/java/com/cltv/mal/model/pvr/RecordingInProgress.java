package com.cltv.mal.model.pvr;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.cltv.mal.model.entities.TvChannel;
import com.cltv.mal.model.entities.TvEvent;

public class RecordingInProgress implements Parcelable {
    int id;
    long recordingStart;
    long recordingEnd;
    TvChannel tvChannel;
    TvEvent tvEvent;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getRecordingStart() {
        return recordingStart;
    }

    public void setRecordingStart(long recordingStart) {
        this.recordingStart = recordingStart;
    }

    public long getRecordingEnd() {
        return recordingEnd;
    }

    public void setRecordingEnd(long recordingEnd) {
        this.recordingEnd = recordingEnd;
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

    public RecordingInProgress(int id, long recordingStart, long recordingEnd, TvChannel tvChannel, TvEvent tvEvent) {
        this.id = id;
        this.recordingStart = recordingStart;
        this.recordingEnd = recordingEnd;
        this.tvChannel = tvChannel;
        this.tvEvent = tvEvent;
    }
    protected RecordingInProgress(Parcel in) {
        id = in.readInt();
        recordingStart = in.readLong();
        recordingEnd = in.readLong();
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
        dest.writeLong(recordingStart);
        dest.writeLong(recordingEnd);
        dest.writeParcelable(tvChannel, 0);
        dest.writeParcelable(tvEvent, 0);
    }

    public static final Creator<RecordingInProgress> CREATOR = new Creator<RecordingInProgress>() {
        @Override
        public RecordingInProgress createFromParcel(Parcel in) {
            return new RecordingInProgress(in);
        }

        @Override
        public RecordingInProgress[] newArray(int size) {
            return new RecordingInProgress[size];
        }
    };
}
