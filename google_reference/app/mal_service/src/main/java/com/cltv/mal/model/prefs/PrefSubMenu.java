package com.cltv.mal.model.prefs;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum PrefSubMenu implements Parcelable {

    CHANNEL_SCAN,
    CHANNEL_EDIT,
    DEFAULT_CHANNEL,
    LCN,
    ANTENNA,
    PARENTAL_CONTROL,
    INTERACTION_CHANNEL,
    MHEG_PIN,
    EVALUATION_LICENCE,
    DISPLAY_MODE,
    ASPECT_RATIO,
    ANOKI_PARENTAL_RATING,


    FIRST_LANGUAGE,
    SECOND_LANGUAGE,
    AUDIO_DESCRIPTION,
    AUDIO_DESCRIPTION_KONKA,
    AUDIO_TYPE,

    GENERAL,
    CLOSED_CAPTIONS,
    ANALOG_SUBTITLE,
    DIGITAL_SUBTITLE,

    DIGITAL_LANGUAGE,
    DECODING_PAGE_LANGUAGE,

    HBBTV_SUPPORT,
    TRACK,
    COOKIE_SETTING,
    PERSISTENT_STORAGE,
    BLOCK_TRACKING_SITES,
    DEVICE_ID,
    RESET_DEVICE_ID,

    CAM_MENU,
    USER_PREFERENCE,
    CAM_TYPE_PREFERENCE,
    CAM_PIN,
    CAM_SCAN,
    CAM_OPERATOR,

    PREFERRED_EPG_LANGUAGE,
    BLUE_MUTE,
    OAD_UPDATE,
    NO_SIGNAL_AUTO_POWER_OFF,
    CHANNEL_BLOCK,
    INPUT_BLOCK,
    RATING_SYSTEMS,
    RATING_LOCK,
    BLOCK_UNRATED_PROGRAMS,
    RRT5_LOCK,
    CHANGE_PIN,
    HEARING_IMPAIRED,
    VISUALLY_IMPAIRED,
    VISUALLY_IMPAIRED_MINIMAL,
    DISPLAY_CC,
    CAPTION_SERVICES,
    ADVANCED_SELECTION,
    TEXT_SIZE,
    FONT_FAMILY,
    TEXT_COLOR,
    TEXT_OPACITY,
    EDGE_TYPE,
    EDGE_COLOR,
    BACKGROUND_COLOR,
    BACKGROUND_OPACITY,
    DEVICE_INFO,
    TIMESHIFT_MODE,
    SCREEN_SETTINGS,
    AUTO_SERVICE_UPDATE,
    AUDIO_FORMAT,
    ENABLE_SUBTITLES,
    ENABLE_SUBTITLES_SWITCH,

    SUBTITLE_TYPE,
    PREFERED_TELETEXT_LANGUAGE,
    SET_TIMESHIFT,
    SET_PVR,
    FORMAT,
    SPEED_TEST,
    SIGNAL_QUALITY,
    SIGNAL_LEVEL,
    CHANNEL_EDIT_MENUS,
    DOWNLOADABLE_PARENTAL,

    CHANNELS,
    CHANNELS_SETTING,
    PICTURE,
    SOUND,
    SCREEN,
    POWER,

    ADS_TARGETING,
    POSTAL_CODE;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(toInteger());
    }

    public static final Creator<PrefSubMenu> CREATOR = new Creator<PrefSubMenu>() {
        @Override
        public PrefSubMenu createFromParcel(Parcel in) {
            return fromInteger(in.readInt());
        }

        @Override
        public PrefSubMenu[] newArray(int size) {
            return new PrefSubMenu[size];
        }
    };

    public int toInteger() {
        return this.ordinal();
    }

    public static PrefSubMenu fromInteger(int value) {
        return values()[value];
    }
}