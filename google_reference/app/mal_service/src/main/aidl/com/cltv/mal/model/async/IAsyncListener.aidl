// IAsyncListener.aidl
package com.cltv.mal.model.async;

// Declare any non-default types here with import statements
oneway interface IAsyncListener {
   void onSuccess();
   void onFailed(in String data);
}