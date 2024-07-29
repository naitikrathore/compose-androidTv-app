// IAsyncRecordingListener.aidl
package com.cltv.mal.model.async;

// Declare any non-default types here with import statements
import com.cltv.mal.model.pvr.Recording;
oneway interface IAsyncRecordingListener {
    void onResponse(in Recording[] result);
}