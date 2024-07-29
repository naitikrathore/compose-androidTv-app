// IAsyncTvEventListener.aidl
package com.cltv.mal.model.async;

import com.cltv.mal.model.entities.TvEvent;
oneway interface IAsyncTvEventListener {
    void onResponse(in TvEvent[] result);
}