package com.cltv.mal.model.ci_plus;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class CiPlusCamMenu implements Parcelable {
    int id = 0;
    String title = "";
    String subTitle = "";
    String bottom = "";
    String[] menuItems;
    public CiPlusCamMenu(){}
    public CiPlusCamMenu(int id, String title, String subTitle, String bottom, String[] menuItems) {
        this.id = id;
        this.title = title;
        this.subTitle = subTitle;
        this.bottom = bottom;
        this.menuItems = menuItems;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public String getBottom() {
        return bottom;
    }

    public String[] getMenuItems() {
        return menuItems;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public void setBottom(String bottom) {
        this.bottom = bottom;
    }

    public void setMenuItems(String[] menuItems) {
        this.menuItems = menuItems;
    }

    protected CiPlusCamMenu(Parcel in) {
        id = in.readInt();
        title = in.readString();
        subTitle = in.readString();
        bottom = in.readString();
        menuItems = in.createStringArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(title);
        parcel.writeString(subTitle);
        parcel.writeString(bottom);
        parcel.writeStringArray(menuItems);
    }

    public static final Creator<CiPlusCamMenu> CREATOR = new Creator<CiPlusCamMenu>() {
        @Override
        public CiPlusCamMenu createFromParcel(Parcel in) {
            return new CiPlusCamMenu(in);
        }

        @Override
        public CiPlusCamMenu[] newArray(int size) {
            return new CiPlusCamMenu[size];
        }
    };
}
