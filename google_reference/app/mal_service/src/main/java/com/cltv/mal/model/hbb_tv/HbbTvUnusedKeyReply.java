package com.cltv.mal.model.hbb_tv;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

public class HbbTvUnusedKeyReply implements Parcelable {

    int keyCode;
    KeyEvent upEvent;
    KeyEvent downEvent;

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public KeyEvent getUpEvent() {
        return upEvent;
    }

    public void setUpEvent(KeyEvent upEvent) {
        this.upEvent = upEvent;
    }

    public KeyEvent getDownEvent() {
        return downEvent;
    }

    public void setDownEvent(KeyEvent downEvent) {
        this.downEvent = downEvent;
    }
    public HbbTvUnusedKeyReply(){}
    public HbbTvUnusedKeyReply(int keyCode, KeyEvent upEvent, KeyEvent downEvent) {
        this.keyCode = keyCode;
        this.upEvent = upEvent;
        this.downEvent = downEvent;
    }

    protected HbbTvUnusedKeyReply(Parcel in) {
        keyCode = in.readInt();
        upEvent = in.readParcelable(KeyEvent.class.getClassLoader());
        downEvent = in.readParcelable(KeyEvent.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(keyCode);
        dest.writeParcelable(upEvent, 0);
        dest.writeParcelable(downEvent, 0);
    }

    public static final Creator<HbbTvUnusedKeyReply> CREATOR = new Creator<HbbTvUnusedKeyReply>() {
        @Override
        public HbbTvUnusedKeyReply createFromParcel(Parcel in) {
            return new HbbTvUnusedKeyReply(in);
        }

        @Override
        public HbbTvUnusedKeyReply[] newArray(int size) {
            return new HbbTvUnusedKeyReply[size];
        }
    };
}
