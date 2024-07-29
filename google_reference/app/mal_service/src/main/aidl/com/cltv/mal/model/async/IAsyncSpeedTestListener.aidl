// IAsyncSpeedTestListener.aidl
package com.cltv.mal.model.async;

// Declare any non-default types here with import statements
oneway interface IAsyncSpeedTestListener {
    void reportProgress(in int progress);
    void onFinished(in boolean isSuccess, in float speedRate);
}