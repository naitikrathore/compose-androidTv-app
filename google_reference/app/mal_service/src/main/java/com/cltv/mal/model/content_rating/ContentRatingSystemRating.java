package com.cltv.mal.model.content_rating;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ContentRatingSystemRating implements Parcelable {
    String mName;
    String mTitle;
    String mDescription;
    Drawable mIcon;
    int mContentAgeHint;
    List<ContentRatingSystemSubRating> mSubRatings;

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

    public int getAgeHint() {
        return mContentAgeHint;
    }

    public List<ContentRatingSystemSubRating> getSubRatings() {
        return mSubRatings;
    }

    public ContentRatingSystemRating(
            String name,
            String title,
            String description,
            Drawable icon,
            int contentAgeHint,
            List<ContentRatingSystemSubRating> subRatings) {
        mName = name;
        mTitle = title;
        mDescription = description;
        mIcon = icon;
        mContentAgeHint = contentAgeHint;
        mSubRatings = subRatings;
    }

    public ContentRatingSystemRating(
            Parcel in) {
        mName = in.readString();
        mTitle = in.readString();
        mDescription = in.readString();
        mIcon = null;
        mContentAgeHint = in.readInt();
        mSubRatings = in.createTypedArrayList(ContentRatingSystemSubRating.CREATOR);
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
        parcel.writeInt(mContentAgeHint);
        parcel.writeTypedList(mSubRatings);
    }

    public static final Creator<ContentRatingSystemRating> CREATOR = new Creator<ContentRatingSystemRating>() {
        @Override
        public ContentRatingSystemRating createFromParcel(Parcel in) {
            return new ContentRatingSystemRating(in);
        }

        @Override
        public ContentRatingSystemRating[] newArray(int size) {
            return new ContentRatingSystemRating[size];
        }
    };

    public static class Builder {
        private String mName;
        private String mTitle;
        private String mDescription;
        private Drawable mIcon;
        private int mContentAgeHint = -1;
        private final List<String> mSubRatingNames = new ArrayList<>();

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

        public void setContentAgeHint(int contentAgeHint) {
            mContentAgeHint = contentAgeHint;
        }

        public void addSubRatingName(String subRatingName) {
            mSubRatingNames.add(subRatingName);
        }

        private ContentRatingSystemRating build(List<ContentRatingSystemSubRating> allDefinedSubRatings) {
            if (TextUtils.isEmpty(mName)) {
                throw new IllegalArgumentException("A rating should have non-empty name");
            }
            if (allDefinedSubRatings == null && mSubRatingNames.size() > 0) {
                throw new IllegalArgumentException("Invalid subrating for rating " + mName);
            }
            if (mContentAgeHint < 0) {
                throw new IllegalArgumentException(
                        "Rating " + mName + " should define " + "non-negative contentAgeHint");
            }
            List<ContentRatingSystemSubRating> subRatings = new ArrayList<>();
            for (String subRatingId : mSubRatingNames) {
                boolean found = false;
                for (ContentRatingSystemSubRating subRating : allDefinedSubRatings) {
                    if (subRatingId.equals(subRating.getName())) {
                        found = true;
                        subRatings.add(subRating);
                        break;
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException(
                            "Unknown subrating name " + subRatingId + " in rating " + mName);
                }
            }
            return new ContentRatingSystemRating(mName, mTitle, mDescription, mIcon, mContentAgeHint, subRatings);
        }
    }

    @Override
    public String toString() {
        return "Rating{" +
                "mName='" + mName + '\'' +
                ", mTitle='" + mTitle + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mIcon=" + mIcon +
                ", mContentAgeHint=" + mContentAgeHint +
                ", mSubRatings=" + mSubRatings +
                '}';
    }
}
