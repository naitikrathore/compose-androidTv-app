package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class DateTimeFormat implements Parcelable {

    String datePattern;
    String timePattern;
    String dateTimePattern;

    public DateTimeFormat(String datePattern, String timePattern, String dateTimePattern) {
        this.datePattern = datePattern;
        this.timePattern = timePattern;
        this.dateTimePattern = dateTimePattern;
    }

    public String getDatePattern() {
        return datePattern;
    }

    public void setDatePattern(String datePattern) {
        this.datePattern = datePattern;
    }

    public String getTimePattern() {
        return timePattern;
    }

    public void setTimePattern(String timePattern) {
        this.timePattern = timePattern;
    }

    public String getDateTimePattern() {
        return dateTimePattern;
    }

    public void setDateTimePattern(String dateTimePattern) {
        this.dateTimePattern = dateTimePattern;
    }

    protected DateTimeFormat(Parcel in) {
        datePattern = in.readString();
        timePattern = in.readString();
        dateTimePattern = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(datePattern);
        dest.writeString(timePattern);
        dest.writeString(dateTimePattern);
    }

    public static final Creator<DateTimeFormat> CREATOR = new Creator<DateTimeFormat>() {
        @Override
        public DateTimeFormat createFromParcel(Parcel in) {
            return new DateTimeFormat(in);
        }

        @Override
        public DateTimeFormat[] newArray(int size) {
            return new DateTimeFormat[size];
        }
    };
}
