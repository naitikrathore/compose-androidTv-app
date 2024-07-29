package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

/**
 * Tv event object
 *
 * @author Dejan Nadj
 */
public class TvEvent implements Parcelable {

    int id = -1;
    TvChannel tvChannel;
    public String name = "";
    String shortDescription = "";
    String longDescription = "";
    String imagePath = "";
    long startTime = 0;
    long endTime = 0;
    int[] categories;
    public int parentalRate = 0;
    int rating = 0;
    String parentalRatingString = "";
    boolean isProgramSame = false;
    boolean isInitialChannel = false;
    int providerFlag = -1;
    String genre = "";
    String subGenre = "";

    public TvEvent() {}
    public TvEvent(int id, TvChannel tvChannel, String name, String shortDescription, String longDescription, String imagePath, long startTime, long endTime, int[] categories, int parentalRate, int rating, String parentalRatingString, boolean isProgramSame, boolean isInitialChannel, int providerFlag, String genre, String subGenre) {
        this.id = id;
        this.tvChannel = tvChannel;
        this.name = name;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        this.imagePath = imagePath;
        this.startTime = startTime;
        this.endTime = endTime;
        this.categories = categories;
        this.parentalRate = parentalRate;
        this.rating = rating;
        this.parentalRatingString = parentalRatingString;
        this.isProgramSame = isProgramSame;
        this.isInitialChannel = isInitialChannel;
        this.providerFlag = providerFlag;
        this.genre = genre;
        this.subGenre = subGenre;
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int[] getCategories() {
        return categories;
    }

    public void setCategories(int[] categories) {
        this.categories = categories;
    }

    public int getParentalRate() {
        return parentalRate;
    }

    public void setParentalRate(int parentalRate) {
        this.parentalRate = parentalRate;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getParentalRatingString() {
        return parentalRatingString;
    }

    public void setParentalRatingString(String parentalRatingString) {
        this.parentalRatingString = parentalRatingString;
    }

    public boolean isProgramSame() {
        return isProgramSame;
    }

    public void setProgramSame(boolean programSame) {
        isProgramSame = programSame;
    }

    public boolean isInitialChannel() {
        return isInitialChannel;
    }

    public void setInitialChannel(boolean initialChannel) {
        isInitialChannel = initialChannel;
    }

    public int getProviderFlag() {
        return providerFlag;
    }

    public void setProviderFlag(int providerFlag) {
        this.providerFlag = providerFlag;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getSubGenre() {
        return subGenre;
    }

    public void setSubGenre(String subGenre) {
        this.subGenre = subGenre;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeParcelable(tvChannel, 0);
        parcel.writeString(name);
        parcel.writeString(shortDescription);
        parcel.writeString(longDescription);
        parcel.writeString(imagePath);
        parcel.writeLong(startTime);
        parcel.writeLong(endTime);
        parcel.writeIntArray(categories);
        parcel.writeInt(parentalRate);
        parcel.writeInt(rating);
        parcel.writeString(parentalRatingString);
        parcel.writeByte((byte) (isProgramSame ? 1 : 0));
        parcel.writeByte((byte) (isInitialChannel ? 1 : 0));
        parcel.writeInt(providerFlag);
        parcel.writeString(genre);
        parcel.writeString(subGenre);
    }

    protected TvEvent(Parcel in) {
        id = in.readInt();
        tvChannel = in.readParcelable(TvChannel.class.getClassLoader());
        name = in.readString();
        this.shortDescription = in.readString();
        this.longDescription = in.readString();
        this.imagePath = in.readString();
        this.startTime = in.readLong();
        this.endTime = in.readLong();
        this.categories = in.createIntArray();
        this.parentalRate = in.readInt();
        this.rating = in.readInt();
        this.parentalRatingString = in.readString();
        this.isProgramSame = in.readByte() != 0;
        this.isInitialChannel = in.readByte() != 0;
        this.providerFlag = in.readInt();
        this.genre = in.readString();
        this.subGenre = in.readString();
    }

    public static final Creator<TvEvent> CREATOR = new Creator<TvEvent>() {
        @Override
        public TvEvent createFromParcel(Parcel in) {
            return new TvEvent(in);
        }

        @Override
        public TvEvent[] newArray(int size) {
            return new TvEvent[size];
        }
    };

    @Override
    public String toString() {
        return "TvEvent{" +
                "id=" + id +
                ", tvChannel=" + tvChannel +
                ", name='" + name + '\'' +
                ", shortDescription='" + shortDescription + '\'' +
                ", longDescription='" + longDescription + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", categories=" + Arrays.toString(categories) +
                ", parentalRate=" + parentalRate +
                ", rating=" + rating +
                ", parentalRatingString='" + parentalRatingString + '\'' +
                ", isProgramSame=" + isProgramSame +
                ", isInitialChannel=" + isInitialChannel +
                ", providerFlag=" + providerFlag +
                ", genre='" + genre + '\'' +
                ", subGenre='" + subGenre + '\'' +
                '}';
    }

    public static TvEvent createNoInformationEvent(TvChannel tvChannel, long currentTimeData) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTimeData);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        Date startDate = calendar.getTime();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 50);
        calendar.set(Calendar.SECOND, 59);
        Date endDate = calendar.getTime();
        String noInformation = "No Information";
        TvEvent recordedTvEvent = new TvEvent(
                0,
                tvChannel,
                noInformation,
                noInformation,
                noInformation,
                noInformation,
                startDate.getTime(),
                endDate.getTime(),
                null,
                0,
                0,
                "",
                false,
                false,
                0,
                "",
                ""
        );
        return recordedTvEvent;
    }

    public static TvEvent createNoInformationEvent(
            TvChannel tvChannel,
            long startTime,
            long endTime,
            long currentTimeData
    ) {
        TvEvent event = createNoInformationEvent(tvChannel, currentTimeData);
        event.setStartTime(startTime); // Assuming setter methods for startTime
        event.setEndTime(endTime); // Assuming setter methods for endTime
        return event;
    }
}
