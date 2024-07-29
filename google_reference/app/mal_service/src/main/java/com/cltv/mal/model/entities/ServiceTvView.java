package com.cltv.mal.model.entities;

import android.content.Context;
import android.media.tv.TvView;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.iwedia.cltv.platform.ModuleFactory;

public class ServiceTvView extends TvView implements Parcelable {

    public ServiceTvView() {
        super(ModuleFactory.Companion.getContext());
    }

    public ServiceTvView(Context context) {
        super(context);
    }

    protected ServiceTvView(Parcel in) {
        this();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
    }

    public static final Creator<ServiceTvView> CREATOR = new Creator<ServiceTvView>() {
        @Override
        public ServiceTvView createFromParcel(Parcel in) {
            return new ServiceTvView(in);
        }

        @Override
        public ServiceTvView[] newArray(int size) {
            return new ServiceTvView[size];
        }
    };
}
