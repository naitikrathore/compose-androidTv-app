package com.cltv.mal.model.pvr;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum ScheduleRecordingResult implements Parcelable {
    SCHEDULE_RECORDING_SUCCESS,
    SCHEDULE_RECORDING_ERROR,
    SCHEDULE_RECORDING_CONFLICTS,
    SCHEDULE_RECORDING_ALREADY_PRESENT;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(toInteger());
    }

    public static final Creator<ScheduleRecordingResult> CREATOR = new Creator<ScheduleRecordingResult>() {
        @Override
        public ScheduleRecordingResult createFromParcel(Parcel in) {
            return fromInteger(in.readInt());
        }

        @Override
        public ScheduleRecordingResult[] newArray(int size) {
            return new ScheduleRecordingResult[size];
        }
    };

    public int toInteger() {
        return this.ordinal();
    }

    public static ScheduleRecordingResult fromInteger(int value) {
        return values()[value];
    }
}
