package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class LanguageCode implements Parcelable {
    String languageCodeISO6392 = "";
    String languageCodeISO6391 = "";
    String englishName = "";
    String germanName = "";
    String frenchName = "";
    boolean hideInAudioPref = false;
    boolean hideInSubtitlePref = false;

    public LanguageCode(String languageCodeISO6392, String languageCodeISO6391, String englishName, String germanName, String frenchName, boolean hideInAudioPref, boolean hideInSubtitlePref) {
        this.languageCodeISO6392 = languageCodeISO6392;
        this.languageCodeISO6391 = languageCodeISO6391;
        this.englishName = englishName;
        this.germanName = germanName;
        this.frenchName = frenchName;
        this.hideInAudioPref = hideInAudioPref;
        this.hideInSubtitlePref = hideInSubtitlePref;
    }

    public LanguageCode(String languageCodeISO6392, String languageCodeISO6391, String englishName, String germanName, String frenchName) {
        this.languageCodeISO6392 = languageCodeISO6392;
        this.languageCodeISO6391 = languageCodeISO6391;
        this.englishName = englishName;
        this.germanName = germanName;
        this.frenchName = frenchName;
        this.hideInAudioPref = false;
        this.hideInSubtitlePref = false;
    }

    public String getLanguageCodeISO6392() {
        return languageCodeISO6392;
    }

    public void setLanguageCodeISO6392(String languageCodeISO6392) {
        this.languageCodeISO6392 = languageCodeISO6392;
    }

    public String getLanguageCodeISO6391() {
        return languageCodeISO6391;
    }

    public void setLanguageCodeISO6391(String languageCodeISO6391) {
        this.languageCodeISO6391 = languageCodeISO6391;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public String getGermanName() {
        return germanName;
    }

    public void setGermanName(String germanName) {
        this.germanName = germanName;
    }

    public String getFrenchName() {
        return frenchName;
    }

    public void setFrenchName(String frenchName) {
        this.frenchName = frenchName;
    }

    public boolean isHideInAudioPref() {
        return hideInAudioPref;
    }

    public void setHideInAudioPref(boolean hideInAudioPref) {
        this.hideInAudioPref = hideInAudioPref;
    }

    public boolean isHideInSubtitlePref() {
        return hideInSubtitlePref;
    }

    public void setHideInSubtitlePref(boolean hideInSubtitlePref) {
        this.hideInSubtitlePref = hideInSubtitlePref;
    }

    protected LanguageCode(Parcel in) {
        languageCodeISO6392 = in.readString();
        languageCodeISO6391 = in.readString();
        englishName = in.readString();
        germanName = in.readString();
        frenchName = in.readString();
        hideInAudioPref = in.readByte() != 0;
        hideInSubtitlePref = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeString(languageCodeISO6392);
        parcel.writeString(languageCodeISO6391);
        parcel.writeString(englishName);
        parcel.writeString(germanName);
        parcel.writeString(frenchName);
        parcel.writeByte((byte) (hideInAudioPref ? 1 : 0));
        parcel.writeByte((byte) (hideInSubtitlePref ? 1 : 0));
    }

    public static final Creator<LanguageCode> CREATOR = new Creator<LanguageCode>() {
        @Override
        public LanguageCode createFromParcel(Parcel in) {
            return new LanguageCode(in);
        }

        @Override
        public LanguageCode[] newArray(int size) {
            return new LanguageCode[size];
        }
    };
}
