package com.cltv.mal.model.content_rating;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ContentRatingSystemOrder implements Parcelable {
    List<ContentRatingSystemRating> mRatingOrder;

    public List<ContentRatingSystemRating> getRatingOrder() {
        return mRatingOrder;
    }

    public ContentRatingSystemOrder(List<ContentRatingSystemRating> ratingOrder) {
        mRatingOrder = ratingOrder;
    }

    private ContentRatingSystemOrder(Parcel in) {
        mRatingOrder = in.createTypedArrayList(ContentRatingSystemRating.CREATOR);
    }

    /**
     * Returns index of the rating in this order. Returns -1 if this order doesn't contain the
     * rating.
     */
    public int getRatingIndex(ContentRatingSystemRating rating) {
        for (int i = 0; i < mRatingOrder.size(); i++) {
            if (mRatingOrder.get(i).getName().equals(rating.getName())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeTypedList(mRatingOrder);
    }

    public static final Creator<ContentRatingSystemOrder> CREATOR = new Creator<ContentRatingSystemOrder>() {
        @Override
        public ContentRatingSystemOrder createFromParcel(Parcel in) {
            return new ContentRatingSystemOrder(in);
        }

        @Override
        public ContentRatingSystemOrder[] newArray(int size) {
            return new ContentRatingSystemOrder[size];
        }
    };

    public static class Builder {
        private final List<String> mRatingNames = new ArrayList<>();

        public Builder() {
        }

        private ContentRatingSystemOrder build(List<ContentRatingSystemRating> ratings) {
            List<ContentRatingSystemRating> ratingOrder = new ArrayList<>();
            for (String ratingName : mRatingNames) {
                boolean found = false;
                for (ContentRatingSystemRating rating : ratings) {
                    if (ratingName.equals(rating.getName())) {
                        found = true;
                        ratingOrder.add(rating);
                        break;
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException(
                            "Unknown rating " + ratingName + " in rating-order tag");
                }
            }
            return new ContentRatingSystemOrder(ratingOrder);
        }

        public void addRatingName(String name) {
            mRatingNames.add(name);
        }
    }
}
