package com.cltv.mal.model.content_rating;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

public class ContentRatingSystemSubRating implements Parcelable {
    String mName;
    String mTitle;
    String mDescription;
    Drawable mIcon;

    public String getName() {
        return mName;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public ContentRatingSystemSubRating(String name, String title, String description, Drawable icon) {
        mName = name;
        mTitle = title;
        mDescription = description;
        mIcon = icon;
    }

    public ContentRatingSystemSubRating(Parcel in) {
        mName = in.readString();
        mTitle = in.readString();
        mDescription = in.readString();
        mIcon = null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(mName);
        parcel.writeString(mTitle);
        parcel.writeString(mDescription);
        parcel.writeParcelable(null, 0);
    }

    public static final Creator<ContentRatingSystemSubRating> CREATOR = new Creator<ContentRatingSystemSubRating>() {
        @Override
        public ContentRatingSystemSubRating createFromParcel(Parcel in) {
            return new ContentRatingSystemSubRating(in);
        }

        @Override
        public ContentRatingSystemSubRating[] newArray(int size) {
            return new ContentRatingSystemSubRating[size];
        }
    };

    public static class Builder {
        private String mName;
        private String mTitle;
        private String mDescription;
        private Drawable mIcon;

        public Builder() {
        }

        public void setName(String name) {
            mName = name;
        }

        public void setTitle(String title) {
            mTitle = title;
        }

        public void setDescription(String description) {
            mDescription = description;
        }

        public void setIcon(Drawable icon) {
            mIcon = icon;
        }

        private ContentRatingSystemSubRating build() {
            if (TextUtils.isEmpty(mName)) {
                throw new IllegalArgumentException("A subrating should have non-empty name");
            }
            return new ContentRatingSystemSubRating(mName, mTitle, mDescription, mIcon);
        }
    }

    @Override
    public String toString() {
        return "SubRating{" +
                "mName='" + mName + '\'' +
                ", mTitle='" + mTitle + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mIcon=" + mIcon +
                '}';
    }
}
