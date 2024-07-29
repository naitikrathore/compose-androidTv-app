package com.cltv.mal.model.pvr;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.cltv.mal.model.entities.TvChannel;
import com.cltv.mal.model.entities.TvEvent;

import java.util.Date;

public class Recording implements Parcelable {
    int id;
    String name;
    long duration;
    long recordingDate;
    String image;
    String videoUrl;
    TvChannel tvChannel;
    TvEvent tvEvent;
    long recordingStartTime;
    long recordingEndTime;
    String shortDescription;

    boolean isEventLocked = false;

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

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getRecordingDate() {
        return recordingDate;
    }

    public void setRecordingDate(long recordingDate) {
        this.recordingDate = recordingDate;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
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

    public long getRecordingStartTime() {
        return recordingStartTime;
    }

    public void setRecordingStartTime(long recordingStartTime) {
        this.recordingStartTime = recordingStartTime;
    }

    public long getRecordingEndTime() {
        return recordingEndTime;
    }

    public void setRecordingEndTime(long recordingEndTime) {
        this.recordingEndTime = recordingEndTime;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public boolean isInProgress() {
        return duration == 0;
    }

    public boolean isEventLocked() {
        return isEventLocked;
    }

    public void setEventLocked(boolean eventLocked) {
        isEventLocked = eventLocked;
    }

    public Recording(int id, String name, long duration, long recordingDate, String image, String videoUrl, TvChannel tvChannel, TvEvent tvEvent, long recordingStartTime, long recordingEndTime, String shortDescription) {
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.recordingDate = recordingDate;
        this.image = image;
        this.videoUrl = videoUrl;
        this.tvChannel = tvChannel;
        this.tvEvent = tvEvent;
        this.recordingStartTime = recordingStartTime;
        this.recordingEndTime = recordingEndTime;
        this.shortDescription = shortDescription;
    }

    protected Recording(Parcel parcel) {
        id = parcel.readInt();
        name = parcel.readString();
        duration = parcel.readLong();
        recordingDate = parcel.readLong();
        image = parcel.readString();
        videoUrl = parcel.readString();
        tvChannel = parcel.readParcelable(TvChannel.class.getClassLoader());
        tvEvent = parcel.readParcelable(TvEvent.class.getClassLoader());
        recordingStartTime = parcel.readLong();
        recordingEndTime = parcel.readLong();
        shortDescription = parcel.readString();
        isEventLocked = parcel.readByte() != 0;

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeLong(duration);
        dest.writeLong(recordingDate);
        dest.writeString(image);
        dest.writeString(videoUrl);
        dest.writeParcelable(tvChannel, 0);
        dest.writeParcelable(tvEvent, 0);
        dest.writeLong(recordingStartTime);
        dest.writeLong(recordingEndTime);
        dest.writeString(shortDescription);
        dest.writeByte((byte) (isEventLocked ? 1 : 0));
    }

    public static final Creator<Recording> CREATOR = new Creator<Recording>() {
        @Override
        public Recording createFromParcel(Parcel in) {
            return new Recording(in);
        }

        @Override
        public Recording[] newArray(int size) {
            return new Recording[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "Recording = [id = " + id + ", name = " + name + ", duration = " + duration + ", startTime = " + new Date(recordingStartTime) + ", endTime = " + new Date(recordingEndTime) + ", imagePath = " + image + ", uri = " + videoUrl + ", tv channel = " + tvChannel.name + ", event = " + tvEvent.name;
    }

    public static String createRecordingTimeInfo(long recordingDate, long recordingEndTime) {
        if (recordingDate != 0 && recordingEndTime != 0) {
            Date startTime = new Date(recordingDate);
            Date endTime = new Date(recordingEndTime);

            long totalSeconds = (endTime.getTime() - startTime.getTime()) / 1000;
            long hours = (totalSeconds / 3600) % 24;
            long minutes = (totalSeconds / 60) % 60;
            long seconds = totalSeconds % 60;
            if (totalSeconds < 0) {
                hours = 0;
                minutes = 0;
                seconds = 0;
            }
            if (hours > 0) {
                return (hours < 10 ? "0" + hours + " h" : hours + " h") + " " +
                        (minutes < 10 ? "0" + minutes + " min" : minutes + " min") + " " +
                        (seconds < 10 ? "0" + seconds + " sec" : seconds + " sec");
            } else if (minutes > 0) {
                return (minutes < 10 ? "0" + minutes + " min" : minutes + " min") + " " +
                        (seconds < 10 ? "0" + seconds + " sec" : seconds + " sec");
            } else {
                return seconds < 10 ? "0" + seconds + " sec" : seconds + " sec";
            }
        }
        return "No start time defined";
    }


    public static int calculateCurrentProgress(long currentTimeData, Long startTime, Long endTime) {
        if (startTime == null || endTime == null) {
            return 0;
        }
        long currentTime = currentTimeData;
        long currentMinute = (currentTime - startTime);
        long duration = (endTime - startTime);
        int progress = 0;
        if (duration > 0) {
            progress = (int) ((currentMinute * 100) / duration);
        }
        // Progress value
        return progress;
    }

}
