package com.cltv.mal.model.content_rating;

import android.content.ContentResolver;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class TvContentRatingSystemInfo implements Parcelable {
    private final Uri mXmlUri;

    private final ApplicationInfo mApplicationInfo;

    /**
     * Creates a TvContentRatingSystemInfo object with given resource ID and receiver info.
     *
     * @param xmlResourceId   The ID of an XML resource whose root element is
     *                        <code> &lt;rating-system-definitions&gt;</code>
     * @param applicationInfo Information about the application that provides the TV content rating
     *                        system definition.
     */
    public static final TvContentRatingSystemInfo createTvContentRatingSystemInfo(int xmlResourceId,
                                                                                  ApplicationInfo applicationInfo) {
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(applicationInfo.packageName)
                .appendPath(String.valueOf(xmlResourceId))
                .build();
        return new TvContentRatingSystemInfo(uri, applicationInfo);
    }

    private TvContentRatingSystemInfo(Uri xmlUri, ApplicationInfo applicationInfo) {
        mXmlUri = xmlUri;
        mApplicationInfo = applicationInfo;
    }

    /**
     * Returns {@code true} if the TV content rating system is defined by a system app,
     * {@code false} otherwise.
     */
    public final boolean isSystemDefined() {
        return (mApplicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    /**
     * Returns the URI to the XML resource that defines the TV content rating system.
     * <p>
     * TODO: Remove. Instead, parse the XML resource and provide an interface to directly access
     * parsed information.
     */
    public final Uri getXmlUri() {
        return mXmlUri;
    }

    /**
     * Used to make this class parcelable.
     */
    public static final Creator<TvContentRatingSystemInfo> CREATOR =
            new Creator<TvContentRatingSystemInfo>() {
                @Override
                public TvContentRatingSystemInfo createFromParcel(Parcel in) {
                    return new TvContentRatingSystemInfo(in);
                }

                @Override
                public TvContentRatingSystemInfo[] newArray(int size) {
                    return new TvContentRatingSystemInfo[size];
                }
            };

    private TvContentRatingSystemInfo(Parcel in) {
        mXmlUri = in.readParcelable(null, Uri.class);
        mApplicationInfo = in.readParcelable(null, ApplicationInfo.class);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mXmlUri, flags);
        dest.writeParcelable(mApplicationInfo, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
