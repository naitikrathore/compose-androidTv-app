package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

/**
 * Tv channel object
 *
 * @author Dejan Nadj
 */
public class TvChannel implements Parcelable, Cloneable {

    public int id = -1;
    public int index = -1;
    public String name = "";
    public String logoImagePath = "";
    public String channelUrl = "";
    public int[] categoryIds;
    //TODO check is this tracks list needed
    //audioTracks
    //subtitleTracks
    public int[] videoQuality;
    public int[] videoType;
    public int[] audioType;
    public long channelId = -1;
    public String inputId = "";
    public String serviceType = "";
    public String displayNumber = "0";
    int lcn = 0;
    public List<String> favListIds;
    public boolean isRadioChannel = false;
    public int tunerType = TunerType.DEFAULT.ordinal();
    public boolean isSkipped = false;
    public boolean isLocked = false;
    public int ordinalNumber = -1;
    public int tsId = 0;
    public int onId = 0;
    public int serviceId = 0;
    public long internalId = -1;
    public boolean isBrowsable = false;
    public String appLinkText = "";
    public String appLinkIntentUri = "";
    public String appLinkIconUri = "";
    public String packageName = "";
    public String[] genres;

    public Object platformSpecific;
    public String type = "";
    public int providerFlag1 = -1;
    public int providerFlag2 = -1;
    public int providerFlag3 = -1;
    public int providerFlag4 = -1;

    public TvChannel(){}

    public TvChannel(int id, int index, String name, String logoImagePath, int[] videoQuality, String channelUrl, long channelId, String inputId, String serviceType, String displayNumber, int lcn, boolean isRadioChannel, int tunerType, boolean isSkipped, boolean isLocked, int ordinalNumber, int tsId, int onId, int serviceId, long internalId, boolean isBrowsable, String appLinkText, String appLinkIntentUri, String appLinkIconUri, String packageName, String[] genres, String type, int providerFlag1, int providerFlag2, int providerFlag3, int providerFlag4) {
        this.id = id;
        this.index = index;
        this.name = name;
        this.logoImagePath = logoImagePath;
        this.videoQuality = videoQuality;
        this.channelUrl = channelUrl;
        this.channelId = channelId;
        this.inputId = inputId;
        this.serviceType = serviceType;
        this.displayNumber = displayNumber;
        this.lcn = lcn;
        this.isRadioChannel = isRadioChannel;
        this.tunerType = tunerType;
        this.isSkipped = isSkipped;
        this.isLocked = isLocked;
        this.ordinalNumber = ordinalNumber;
        this.tsId = tsId;
        this.onId = onId;
        this.serviceId = serviceId;
        this.internalId = internalId;
        this.isBrowsable = isBrowsable;
        this.appLinkText = appLinkText;
        this.appLinkIntentUri = appLinkIntentUri;
        this.appLinkIconUri = appLinkIconUri;
        this.packageName = packageName;
        this.genres = genres;
        this.type = type;
        this.providerFlag1 = providerFlag1;
        this.providerFlag2 = providerFlag2;
        this.providerFlag3 = providerFlag3;
        this.providerFlag4 = providerFlag4;
    }

    protected TvChannel(Parcel in) {
        id = in.readInt();
        index = in.readInt();
        name = in.readString();
        logoImagePath = in.readString();
        videoQuality = in.createIntArray();
        channelUrl = in.readString();
        channelId = in.readLong();
        inputId = in.readString();
        serviceType = in.readString();
        displayNumber = in.readString();
        lcn = in.readInt();
        favListIds = in.createStringArrayList();
        isRadioChannel = in.readByte() != 0;
        isSkipped = in.readByte() != 0;
        isLocked = in.readByte() != 0;
        tunerType = in.readInt();
        ordinalNumber = in.readInt();
        tsId = in.readInt();
        onId = in.readInt();
        serviceId = in.readInt();
        internalId = in.readLong();
        isBrowsable = in.readByte() != 0;
        appLinkText = in.readString();
        appLinkIntentUri = in.readString();
        appLinkIconUri = in.readString();
        packageName = in.readString();
        genres = in.createStringArray();
        type = in.readString();
        providerFlag1 = in.readInt();
        providerFlag2 = in.readInt();
        providerFlag3 = in.readInt();
        providerFlag4 = in.readInt();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getLogoImagePath() {
        return logoImagePath;
    }

    public void setLogoImagePath(String logoImagePath) {
        this.logoImagePath = logoImagePath;
    }

    public String getChannelUrl() {
        return channelUrl;
    }

    public void setChannelUrl(String channelUrl) {
        this.channelUrl = channelUrl;
    }

    public long getChannelId() {
        return channelId;
    }

    public void setChannelId(long channelId) {
        this.channelId = channelId;
    }

    public String getInputId() {
        return inputId;
    }

    public void setInputId(String inputId) {
        this.inputId = inputId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getDisplayNumber() {
        return displayNumber;
    }

    public void setDisplayNumber(String displayNumber) {
        this.displayNumber = displayNumber;
    }

    public int getLcn() {
        return lcn;
    }

    public void setLcn(int lcn) {
        this.lcn = lcn;
    }

    public boolean isRadioChannel() {
        return isRadioChannel;
    }

    public void setRadioChannel(boolean radioChannel) {
        isRadioChannel = radioChannel;
    }

    public boolean isSkipped() {
        return isSkipped;
    }

    public void setSkipped(boolean skipped) {
        isSkipped = skipped;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public int getOrdinalNumber() {
        return ordinalNumber;
    }

    public void setOrdinalNumber(int ordinalNumber) {
        this.ordinalNumber = ordinalNumber;
    }

    public int getTsId() {
        return tsId;
    }

    public void setTsId(int tsId) {
        this.tsId = tsId;
    }

    public int getOnId() {
        return onId;
    }

    public void setOnId(int onId) {
        this.onId = onId;
    }

    public int getServiceId() {
        return serviceId;
    }

    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }

    public long getInternalId() {
        return internalId;
    }

    public void setInternalId(long internalId) {
        this.internalId = internalId;
    }

    public boolean isBrowsable() {
        return isBrowsable;
    }

    public void setBrowsable(boolean browsable) {
        isBrowsable = browsable;
    }

    public String getAppLinkText() {
        return appLinkText;
    }

    public void setAppLinkText(String appLinkText) {
        this.appLinkText = appLinkText;
    }

    public String getAppLinkIntentUri() {
        return appLinkIntentUri;
    }

    public void setAppLinkIntentUri(String appLinkIntentUri) {
        this.appLinkIntentUri = appLinkIntentUri;
    }

    public String getAppLinkIconUri() {
        return appLinkIconUri;
    }

    public void setAppLinkIconUri(String appLinkIconUri) {
        this.appLinkIconUri = appLinkIconUri;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getProviderFlag1() {
        return providerFlag1;
    }

    public void setProviderFlag1(int providerFlag1) {
        this.providerFlag1 = providerFlag1;
    }

    public int getProviderFlag2() {
        return providerFlag2;
    }

    public void setProviderFlag2(int providerFlag2) {
        this.providerFlag2 = providerFlag2;
    }

    public int getProviderFlag3() {
        return providerFlag3;
    }

    public void setProviderFlag3(int providerFlag3) {
        this.providerFlag3 = providerFlag3;
    }

    public int getProviderFlag4() {
        return providerFlag4;
    }

    public void setProviderFlag4(int providerFlag4) {
        this.providerFlag4 = providerFlag4;
    }


    @Override
    public int describeContents() {

        return 0;

    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {

        parcel.writeInt(id);
        parcel.writeInt(index);
        parcel.writeString(name);
        parcel.writeString(logoImagePath);
        parcel.writeIntArray(videoQuality);
        parcel.writeString(channelUrl);
        parcel.writeLong(channelId);
        parcel.writeString(inputId);
        parcel.writeString(serviceType);
        parcel.writeString(displayNumber);
        parcel.writeInt(lcn);
        parcel.writeStringList(favListIds);
        parcel.writeByte((byte) (isRadioChannel ? 1 : 0));
        parcel.writeByte((byte) (isSkipped ? 1 : 0));
        parcel.writeByte((byte) (isLocked ? 1 : 0));
        parcel.writeInt(tunerType);
        parcel.writeInt(ordinalNumber);
        parcel.writeInt(tsId);
        parcel.writeInt(onId);
        parcel.writeInt(serviceId);
        parcel.writeLong(internalId);
        parcel.writeByte((byte) (isBrowsable ? 1 : 0));
        parcel.writeString(appLinkText);
        parcel.writeString(appLinkIntentUri);
        parcel.writeString(appLinkIconUri);
        parcel.writeString(packageName);
        parcel.writeStringArray(genres);
        parcel.writeString(type);
        parcel.writeInt(providerFlag1);
        parcel.writeInt(providerFlag2);
        parcel.writeInt(providerFlag3);
        parcel.writeInt(providerFlag4);
    }

    public static final Creator<TvChannel> CREATOR = new Creator<TvChannel>() {
        @Override
        public TvChannel createFromParcel(Parcel in) {
            return new TvChannel(in);
        }

        @Override
        public TvChannel[] newArray(int size) {
            return new TvChannel[size];
        }
    };

    public boolean isFastChannel() {
        return inputId.contains("Anoki");
    }

    public boolean isBroadcastChannel() {
        return inputId.contains("mediatek");
    }

    /**
     * Generates a unique identifier to identify channel
     *
     * @return A unique String.
     */
    public String getUniqueIdentifier() {
        if (isFastChannel()) {
            return displayNumber;
        } else {
            return onId + "_" + tsId + "_" + serviceId;
        }
    }

    @Override
    public String toString() {
        return "TvChannel{" +
                "id=" + id +
                ", index=" + index +
                ", name='" + name + '\'' +
                ", logoImagePath='" + logoImagePath + '\'' +
                ", channelUrl='" + channelUrl + '\'' +
                ", categoryIds=" + Arrays.toString(categoryIds) +
                ", videoQuality=" + Arrays.toString(videoQuality) +
                ", videoType=" + Arrays.toString(videoType) +
                ", audioType=" + Arrays.toString(audioType) +
                ", channelId=" + channelId +
                ", inputId='" + inputId + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", displayNumber='" + displayNumber + '\'' +
                ", lcn=" + lcn +
                ", favListIds=" + favListIds +
                ", isRadioChannel=" + isRadioChannel +
                ", tunerType=" + tunerType +
                ", isSkipped=" + isSkipped +
                ", isLocked=" + isLocked +
                ", ordinalNumber=" + ordinalNumber +
                ", tsId=" + tsId +
                ", onId=" + onId +
                ", serviceId=" + serviceId +
                ", internalId=" + internalId +
                ", isBrowsable=" + isBrowsable +
                ", appLinkText='" + appLinkText + '\'' +
                ", appLinkIntentUri='" + appLinkIntentUri + '\'' +
                ", appLinkIconUri='" + appLinkIconUri + '\'' +
                ", packageName='" + packageName + '\'' +
                ", genres=" + Arrays.toString(genres) +
                ", type='" + type + '\'' +
                ", providerFlag1=" + providerFlag1 +
                ", providerFlag2=" + providerFlag2 +
                ", providerFlag3=" + providerFlag3 +
                ", providerFlag4=" + providerFlag4 +
                '}';
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static boolean compare(TvChannel channel1, TvChannel channel2) {
        return channel1.onId == channel2.onId &&
                channel1.tsId == channel2.tsId &&
                channel1.serviceId == channel2.serviceId;
    }
}