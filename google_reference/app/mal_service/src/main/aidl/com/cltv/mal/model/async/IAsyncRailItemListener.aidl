// IAsyncRailItemListener.aidl
package com.cltv.mal.model.async;

// Declare any non-default types here with import statements
import com.cltv.mal.model.entities.RailItem;
oneway interface IAsyncRailItemListener {
    void onResponse(in RailItem[] result);
}