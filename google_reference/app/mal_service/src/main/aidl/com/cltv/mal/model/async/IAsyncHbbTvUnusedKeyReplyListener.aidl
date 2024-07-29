// IAsyncHbbTvUnusedKeyReplyListener.aidl
package com.cltv.mal.model.async;

// Declare any non-default types here with import statements
import com.cltv.mal.model.hbb_tv.HbbTvUnusedKeyReply;
interface IAsyncHbbTvUnusedKeyReplyListener {
    void onResponse(in HbbTvUnusedKeyReply result);
}