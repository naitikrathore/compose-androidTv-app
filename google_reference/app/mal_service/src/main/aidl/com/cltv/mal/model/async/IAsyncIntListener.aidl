// IAsyncIntListener.aidl
package com.cltv.mal.model.async;

// Declare any non-default types here with import statements

oneway interface IAsyncIntListener {
    void onResponse(int result);
}