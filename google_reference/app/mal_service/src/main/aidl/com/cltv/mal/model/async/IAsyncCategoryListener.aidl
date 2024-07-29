// IAsyncCategoryListener.aidl
package com.cltv.mal.model.async;

// Declare any non-default types here with import statements
import com.cltv.mal.model.entities.Category;
oneway interface IAsyncCategoryListener {
    void onResponse(in Category[] result);
}