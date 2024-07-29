package com.cltv.mal.model.fast;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class RecommendationItem implements Parcelable {

    public String type;
    public String title;
    public String thumbnail;
    public String description;
    public String playbackUrl;
    public String channelId;
    public long startTimeEpoch;
    public long durationSec;
    public String rating;
    public String genre;
    public String language;
    public String previewUrl;
    public long previewUrlSkipSec;
    public String contentId;

    public RecommendationItem(String type, String title, String thumbnail, String description, String playbackUrl, String channelId, long startTimeEpoch, long durationSec, String rating, String genre, String language, String previewUrl, long previewUrlSkipSec, String contentId) {
        this.type = type;
        this.title = title;
        this.thumbnail = thumbnail;
        this.description = description;
        this.playbackUrl = playbackUrl;
        this.channelId = channelId;
        this.startTimeEpoch = startTimeEpoch;
        this.durationSec = durationSec;
        this.rating = rating;
        this.genre = genre;
        this.language = language;
        this.previewUrl = previewUrl;
        this.previewUrlSkipSec = previewUrlSkipSec;
        this.contentId = contentId;
    }

    protected RecommendationItem(Parcel in) {
        type = in.readString();
        title = in.readString();
        thumbnail = in.readString();
        description = in.readString();
        playbackUrl = in.readString();
        channelId = in.readString();
        startTimeEpoch = in.readLong();
        durationSec = in.readLong();
        rating = in.readString();
        genre = in.readString();
        language = in.readString();
        previewUrl = in.readString();
        previewUrlSkipSec = in.readLong();
        contentId = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeString(title);
        dest.writeString(thumbnail);
        dest.writeString(description);
        dest.writeString(playbackUrl);
        dest.writeString(channelId);
        dest.writeLong(startTimeEpoch);
        dest.writeLong(durationSec);
        dest.writeString(rating);
        dest.writeString(genre);
        dest.writeString(language);
        dest.writeString(previewUrl);
        dest.writeLong(previewUrlSkipSec);
        dest.writeString(contentId);
    }

    public static final Creator<RecommendationItem> CREATOR = new Creator<RecommendationItem>() {
        @Override
        public RecommendationItem createFromParcel(Parcel in) {
            return new RecommendationItem(in);
        }

        @Override
        public RecommendationItem[] newArray(int size) {
            return new RecommendationItem[size];
        }
    };
}
