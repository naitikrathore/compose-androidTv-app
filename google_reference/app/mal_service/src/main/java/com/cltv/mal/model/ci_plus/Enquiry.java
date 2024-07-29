package com.cltv.mal.model.ci_plus;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Enquiry implements Parcelable {

    int id = 0;
    String title = "";
    String inputText = "";

    Boolean blind = false;

    public Enquiry(int id, String title, String inputText, Boolean blind) {
        this.id = id;
        this.title = title;
        this.inputText = inputText;
        this.blind = blind;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setBlind(Boolean blind) { this.blind = blind; }

    public Boolean getBlind() {  return blind; }

    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }

    protected Enquiry(Parcel in) {
        id = in.readInt();
        title = in.readString();
        inputText = in.readString();
        blind = in.readBoolean();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(title);
        parcel.writeString(inputText);
        parcel.writeBoolean(blind);
    }

    public static final Creator<Enquiry> CREATOR = new Creator<Enquiry>() {
        @Override
        public Enquiry createFromParcel(Parcel in) {
            return new Enquiry(in);
        }

        @Override
        public Enquiry[] newArray(int size) {
            return new Enquiry[size];
        }
    };
}
