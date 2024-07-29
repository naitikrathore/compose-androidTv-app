package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class InputResolutionItem implements Parcelable {
    String iconValue = "HD";
    String pixelValue;
    String hdrValue;

    public String getIconValue() {
        return iconValue;
    }

    public void setIconValue(String iconValue) {
        this.iconValue = iconValue;
    }

    public String getPixelValue() {
        return pixelValue;
    }

    public void setPixelValue(String pixelValue) {
        this.pixelValue = pixelValue;
    }

    public String getHdrValue() {
        return hdrValue;
    }

    public void setHdrValue(String hdrValue) {
        this.hdrValue = hdrValue;
    }

    public InputResolutionItem(String iconValue, String pixelValue, String hdrValue) {
        this.iconValue = iconValue;
        this.pixelValue = pixelValue;
        this.hdrValue = hdrValue;
    }

    protected InputResolutionItem(Parcel in) {
        iconValue = in.readString();
        pixelValue = in.readString();
        hdrValue = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(iconValue);
        dest.writeString(pixelValue);
        dest.writeString(hdrValue);
    }

    public static final Creator<InputResolutionItem> CREATOR = new Creator<InputResolutionItem>() {
        @Override
        public InputResolutionItem createFromParcel(Parcel in) {
            return new InputResolutionItem(in);
        }

        @Override
        public InputResolutionItem[] newArray(int size) {
            return new InputResolutionItem[size];
        }
    };
}
