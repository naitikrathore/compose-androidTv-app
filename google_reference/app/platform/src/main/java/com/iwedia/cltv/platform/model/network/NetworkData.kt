package com.iwedia.cltv.platform.model.network

import android.net.LinkProperties
import android.net.NetworkCapabilities

/* The NetworkCapabilities and LinkProperties objects provide information
   about all attributes that the system knows about a network */
sealed class NetworkData {
    object NoConnection: NetworkData()
    data class Network(
        val networkCapabilities: NetworkCapabilities?,
        val linkProperties: LinkProperties?
    ): NetworkData()
}