package com.cltv.mal.model.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum Region implements Parcelable {
    CN,
    US,
    SA,
    EU,
    PA;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(toInteger());
    }

    public static final Creator<Region> CREATOR = new Creator<Region>() {
        @Override
        public Region createFromParcel(Parcel in) {
            return fromInteger(in.readInt());
        }

        @Override
        public Region[] newArray(int size) {
            return new Region[size];
        }
    };

    public int toInteger() {
        return this.ordinal();
    }

    public static Region fromInteger(int value) {
        return values()[value];
    }

    public static List<Region> getRegionList() {
        List<Region> list = new ArrayList<>();
        list.add(CN);
        list.add(US);
        list.add(SA);
        list.add(EU);
        list.add(PA);
        return list;
    }

    public static List<String> getCountryList(Region region) {
        switch (region) {
            case CN: {
                return Arrays.asList(
                        "AF",
                        "AM",
                        "AZ",
                        "BH",
                        "BD",
                        "BT",
                        "IO",
                        "BN",
                        "KH",
                        "CN",
                        "CX",
                        "CC",
                        "CY",
                        "GE",
                        "HK",
                        "IN",
                        "ID",
                        "IR",
                        "IQ",
                        "IL",
                        "JP",
                        "JO",
                        "KZ",
                        "KP",
                        "KR",
                        "KW",
                        "KG",
                        "LA",
                        "LB",
                        "MO",
                        "MY",
                        "MV",
                        "MN",
                        "MM",
                        "NP",
                        "OM",
                        "PK",
                        "PS",
                        "PH",
                        "QA",
                        "SA",
                        "SG",
                        "LK",
                        "SY",
                        "TW",
                        "TJ",
                        "TH",
                        "TL",
                        "TM",
                        "AE",
                        "UZ",
                        "VN",
                        "YE"
                );
            }
            case US: {
                return Arrays.asList(
                        "AI",
                        "AG",
                        "AW",
                        "BS",
                        "BB",
                        "BZ",
                        "BM",
                        "BQ",
                        "CA",
                        "KY",
                        "CR",
                        "CU",
                        "CW",
                        "DM",
                        "DO",
                        "SV",
                        "GL",
                        "GD",
                        "GP",
                        "GT",
                        "HT",
                        "HN",
                        "JM",
                        "MQ",
                        "MX",
                        "MS",
                        "NI",
                        "PA",
                        "PR",
                        "BL",
                        "KN",
                        "LC",
                        "MF",
                        "PM",
                        "VC",
                        "SX",
                        "TT",
                        "TC",
                        "US",
                        "VG",
                        "VI");
            }
            case SA: {
                return Arrays.asList(
                        "AR",
                        "BO",
                        "BR",
                        "CL",
                        "CO",
                        "EC",
                        "FK",
                        "GF",
                        "GY",
                        "PY",
                        "PE",
                        "SR",
                        "UY",
                        "VE"
                );
            }
            case EU: {
                return Arrays.asList(
                        "AX",
                        "AL",
                        "AD",
                        "AT",
                        "BY",
                        "BE",
                        "BA",
                        "BG",
                        "HR",
                        "CZ",
                        "DK",
                        "EE",
                        "FO",
                        "FI",
                        "FR",
                        "DE",
                        "GI",
                        "GR",
                        "GG",
                        "VA",
                        "HU",
                        "IS",
                        "IE",
                        "IM",
                        "IT",
                        "JE",
                        "LV",
                        "LI",
                        "LT",
                        "LU",
                        "MK",
                        "MT",
                        "MD",
                        "MC",
                        "ME",
                        "NL",
                        "NO",
                        "PL",
                        "PT",
                        "RO",
                        "RU",
                        "SM",
                        "RS",
                        "SK",
                        "SI",
                        "ES",
                        "SJ",
                        "SE",
                        "CH",
                        "TR",
                        "UA",
                        "GB"
                );
            }
            case PA: {
                return Arrays.asList(
                        "ID", "MY", "NZ", "AU", "SG", "TH", "VN", "MM", "TW", "LV", "LT", "IN", "AE", "GH"
                );
            }
        }
        return new ArrayList<>();
    }
}
