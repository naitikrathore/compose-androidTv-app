package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class SystemInfoData implements Parcelable {
    public Long displayNumber;
    public String displayName;
    public String providerData;
    public String logoImagePath;
    public boolean isRadioChannel;
    public boolean isSkipped;
    public boolean isLocked;
    public int tunerType;
    public int ordinalNumber;
    public int frequency;
    public int tsId;
    public int onId;
    public int serviceId;
    public String bandwidth;
    public int networkId;   // Unsupported
    public String networkName;     // Unsupported
    public String postViterbi;     // Unsupported
    public String attr5s;          // Unsupported
    public int signalQuality;
    public int signalStrength;
    public int signalBer;
    public int signalAGC;   // Unsupported
    public int signalUEC;     // Unsupported


    public SystemInfoData() {

    }

    public SystemInfoData(long id, String displayName,
                          String providerData, String logoImagePath, Boolean isRadioChannel,
                          Boolean isLocked, Boolean isSkipped,
                          int tunerType, int ordinalNumber, int frequency, int tsId, int onId,
                          int serviceId, String bandwidth,
                          int networkId, String networkName, String postViterbi, String attr5s,
                          int signalQuality, int signalStrength,
                          int signalBer, int signalAGC, int signalUEC) {
        this.displayNumber = id;
        this.displayName = displayName;
        this.providerData = providerData;
        this.logoImagePath = logoImagePath;
        this.isRadioChannel = isRadioChannel;
        this.isLocked = isLocked;
        this.isSkipped = isSkipped;

        this.tunerType = tunerType;
        this.ordinalNumber = ordinalNumber;
        this.frequency = frequency;
        this.tsId = tsId;
        this.onId = onId;
        this.serviceId = serviceId;

        this.bandwidth = bandwidth;
        this.networkId = networkId;
        this.networkName = networkName;
        this.postViterbi = postViterbi;
        this.attr5s = attr5s;
        this.signalQuality = signalQuality;

        this.signalStrength = signalStrength;
        this.signalBer = signalBer;
        this.signalAGC = signalAGC;
        this.signalUEC = signalUEC;

    }

    protected SystemInfoData(Parcel in) {
        displayNumber = in.readLong();
        displayName = in.readString();
        providerData = in.readString();
        logoImagePath = in.readString();
        isRadioChannel = in.readByte() != 0;
        isSkipped = in.readByte() != 0;
        isLocked = in.readByte() != 0;
        tunerType = in.readInt();
        ordinalNumber = in.readInt();
        frequency = in.readInt();
        tsId = in.readInt();
        onId = in.readInt();
        serviceId = in.readInt();
        bandwidth = in.readString();
        networkId = in.readInt();
        networkName = in.readString();
        postViterbi = in.readString();
        attr5s = in.readString();
        signalQuality = in.readInt();
        signalStrength = in.readInt();
        signalBer = in.readInt();
        signalAGC = in.readInt();
        signalUEC = in.readInt();
    }

    public Long getDisplayNumber() {
        return displayNumber;
    }

    public void setDisplayNumber(Long displayNumber) {
        this.displayNumber = displayNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProviderData() {
        return providerData;
    }

    public void setProviderData(String providerData) {
        this.providerData = providerData;
    }

    public String getLogoImagePath() {
        return logoImagePath;
    }

    public void setLogoImagePath(String logoImagePath) {
        this.logoImagePath = logoImagePath;
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

    public int getTunerType() {
        return tunerType;
    }

    public void setTunerType(int tunerType) {
        this.tunerType = tunerType;
    }

    public int getOrdinalNumber() {
        return ordinalNumber;
    }

    public void setOrdinalNumber(int ordinalNumber) {
        this.ordinalNumber = ordinalNumber;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
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

    public String getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(String bandwidth) {
        this.bandwidth = bandwidth;
    }

    public int getNetworkId() {
        return networkId;
    }

    public void setNetworkId(int networkId) {
        this.networkId = networkId;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public String getPostViterbi() {
        return postViterbi;
    }

    public void setPostViterbi(String postViterbi) {
        this.postViterbi = postViterbi;
    }

    public String getAttr5s() {
        return attr5s;
    }

    public void setAttr5s(String attr5s) {
        this.attr5s = attr5s;
    }

    public int getSignalQuality() {
        return signalQuality;
    }

    public void setSignalQuality(int signalQuality) {
        this.signalQuality = signalQuality;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public int getSignalBer() {
        return signalBer;
    }

    public void setSignalBer(int signalBer) {
        this.signalBer = signalBer;
    }

    public int getSignalAGC() {
        return signalAGC;
    }

    public void setSignalAGC(int signalAGC) {
        this.signalAGC = signalAGC;
    }

    public int getSignalUEC() {
        return signalUEC;
    }

    public void setSignalUEC(int signalUEC) {
        this.signalUEC = signalUEC;
    }

    public static final Creator<SystemInfoData> CREATOR = new Creator<SystemInfoData>() {
        @Override
        public SystemInfoData createFromParcel(Parcel in) {
            return new SystemInfoData(in);
        }

        @Override
        public SystemInfoData[] newArray(int size) {
            return new SystemInfoData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(displayNumber);
        dest.writeString(displayName);
        dest.writeString(providerData);
        dest.writeString(logoImagePath);
        dest.writeByte((byte) (isRadioChannel ? 1 : 0));
        dest.writeByte((byte) (isSkipped ? 1 : 0));
        dest.writeByte((byte) (isLocked ? 1 : 0));
        dest.writeInt(tunerType);
        dest.writeInt(ordinalNumber);
        dest.writeInt(frequency);
        dest.writeInt(tsId);
        dest.writeInt(onId);
        dest.writeInt(serviceId);
        dest.writeString(bandwidth);
        dest.writeInt(networkId);
        dest.writeString(networkName);
        dest.writeString(postViterbi);
        dest.writeString(attr5s);
        dest.writeInt(signalQuality);
        dest.writeInt(signalStrength);
        dest.writeInt(signalBer);
        dest.writeInt(signalAGC);
        dest.writeInt(signalUEC);
    }
}
