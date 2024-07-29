package com.iwedia.cltv.platform.`interface`

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.iwedia.cltv.platform.model.network.NetworkData

interface NetworkInterface {
    val networkStatus: LiveData<NetworkData>
    var anokiServerStatus: MutableLiveData<Boolean>
}