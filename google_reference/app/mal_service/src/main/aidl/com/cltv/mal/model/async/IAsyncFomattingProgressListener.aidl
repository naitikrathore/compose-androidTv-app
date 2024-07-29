// IAsyncFomattingProgressListener.aidl
package com.cltv.mal.model.async;
interface IAsyncFomattingProgressListener {
     void onFinished(in boolean isSuccess);
     void reportProgress(in boolean visible, in String statusText);
}