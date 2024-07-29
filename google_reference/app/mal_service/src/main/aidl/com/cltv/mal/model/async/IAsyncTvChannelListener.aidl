// IAsyncTvChannelListener.aidl
package com.cltv.mal.model.async;

// Declare any non-default types here with import statements
import com.cltv.mal.model.entities.TvChannel;
oneway interface IAsyncTvChannelListener {
    void onResponse(in TvChannel[] result);
}