package com.cltv.mal.model.fast;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class PromotionItem implements Parcelable {
    String type;
    String banner;
    String logo;
    String callToAction;
    String clickUrl;
    String channelid;

    String contentId;

    public PromotionItem(String type, String banner, String logo, String callToAction, String clickUrl, String channelId) {
        this.type = type;
        this.banner = banner;
        this.logo = logo;
        this.callToAction = callToAction;
        this.clickUrl = clickUrl;
        this.channelid = channelId;
    }

    protected PromotionItem(Parcel in) {
        type = in.readString();
        banner = in.readString();
        logo = in.readString();
        callToAction = in.readString();
        clickUrl = in.readString();
        channelid = in.readString();
        contentId = in.readString();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(String callToAction) {
        this.callToAction = callToAction;
    }

    public String getClickUrl() {
        return clickUrl;
    }

    public void setClickUrl(String clickUrl) {
        this.clickUrl = clickUrl;
    }

    public String getChannelId() {
        return channelid;
    }

    public void setChannelId(String channelId) {
        this.channelid = channelId;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeString(banner);
        dest.writeString(logo);
        dest.writeString(callToAction);
        dest.writeString(clickUrl);
        dest.writeString(channelid);
        dest.writeString(contentId);
    }

    public static final Creator<PromotionItem> CREATOR = new Creator<PromotionItem>() {
        @Override
        public PromotionItem createFromParcel(Parcel in) {
            return new PromotionItem(in);
        }

        @Override
        public PromotionItem[] newArray(int size) {
            return new PromotionItem[size];
        }
    };
}
