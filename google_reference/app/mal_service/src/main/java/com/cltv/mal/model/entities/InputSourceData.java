package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class InputSourceData implements Parcelable {
    String inputSourceName;
    int hardwareId;
    boolean isBlocked;
    String inputMainName;

    public String getInputSourceName() {
        return inputSourceName;
    }

    public void setInputSourceName(String inputSourceName) {
        this.inputSourceName = inputSourceName;
    }

    public int getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(int hardwareId) {
        this.hardwareId = hardwareId;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public String getInputMainName() {
        return inputMainName;
    }

    public void setInputMainName(String inputMainName) {
        this.inputMainName = inputMainName;
    }

    public InputSourceData(String inputSourceName, int hardwareId, String inputMainName) {
        this.inputSourceName = inputSourceName;
        this.hardwareId = hardwareId;
        this.inputMainName = inputMainName;
    }

    protected InputSourceData(Parcel in) {
        inputSourceName = in.readString();
        hardwareId = in.readInt();
        isBlocked = in.readByte() != 0;
        inputMainName = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(inputSourceName);
        dest.writeInt(hardwareId);
        dest.writeByte((byte) (isBlocked ? 1 : 0));
        dest.writeString(inputMainName);
    }

    public static final Creator<InputSourceData> CREATOR = new Creator<InputSourceData>() {
        @Override
        public InputSourceData createFromParcel(Parcel in) {
            return new InputSourceData(in);
        }

        @Override
        public InputSourceData[] newArray(int size) {
            return new InputSourceData[size];
        }
    };
}
