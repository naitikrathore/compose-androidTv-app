package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class InputItem implements Parcelable {
    int id;
    String inputMainName;
    String inputSourceName;
    int isAvailable;
    boolean isHidden;
    int hardwareId;
    String inputId;
    String tuneUrl;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInputMainName() {
        return inputMainName;
    }

    public void setInputMainName(String inputMainName) {
        this.inputMainName = inputMainName;
    }

    public String getInputSourceName() {
        return inputSourceName;
    }

    public void setInputSourceName(String inputSourceName) {
        this.inputSourceName = inputSourceName;
    }

    public int getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(int isAvailable) {
        this.isAvailable = isAvailable;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public int getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(int hardwareId) {
        this.hardwareId = hardwareId;
    }

    public String getInputId() {
        return inputId;
    }

    public void setInputId(String inputId) {
        this.inputId = inputId;
    }

    public String getTuneUrl() {
        return tuneUrl;
    }

    public void setTuneUrl(String tuneUrl) {
        this.tuneUrl = tuneUrl;
    }

    public InputItem(int id, String inputMainName, String inputSourceName, int isAvailable, boolean isHidden, int hardwareId, String inputId, String tuneUrl) {
        this.id = id;
        this.inputMainName = inputMainName;
        this.inputSourceName = inputSourceName;
        this.isAvailable = isAvailable;
        this.isHidden = isHidden;
        this.hardwareId = hardwareId;
        this.inputId = inputId;
        this.tuneUrl = tuneUrl;
    }

    public InputItem(int id, String inputMainName, String inputSourceName, boolean type, boolean isHidden, int hardwareId, String inputId, String tuneUrl) {
        this.id = id;
        this.inputMainName = inputMainName;
        this.inputSourceName = inputSourceName;
        if (type) {
            this.isAvailable = 1;
        } else {
            this.isAvailable = 0;
        }
        this.isHidden = isHidden;
        this.hardwareId = hardwareId;
        this.inputId = inputId;
        this.tuneUrl = tuneUrl;
    }

    public InputItem(Parcel in) {
        id = in.readInt();
        inputMainName = in.readString();
        inputSourceName = in.readString();
        isAvailable = in.readInt();
        isHidden = in.readByte() != 0;
        hardwareId = in.readInt();
        inputId = in.readString();
        tuneUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(inputMainName);
        dest.writeString(inputSourceName);
        dest.writeInt(isAvailable);
        dest.writeByte((byte) (isHidden ? 1 : 0));
        dest.writeInt(hardwareId);
        dest.writeString(inputId);
        dest.writeString(tuneUrl);
    }

    public static final Creator<InputItem> CREATOR = new Creator<InputItem>() {
        @Override
        public InputItem createFromParcel(Parcel in) {
            return new InputItem(in);
        }

        @Override
        public InputItem[] newArray(int size) {
            return new InputItem[size];
        }
    };
}
