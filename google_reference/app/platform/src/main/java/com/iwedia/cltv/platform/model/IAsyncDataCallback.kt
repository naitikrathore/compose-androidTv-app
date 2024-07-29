package com.iwedia.cltv.platform.model

interface IAsyncDataCallback<T> {
    fun onFailed(error: Error)

    fun onReceive(data: T)
}