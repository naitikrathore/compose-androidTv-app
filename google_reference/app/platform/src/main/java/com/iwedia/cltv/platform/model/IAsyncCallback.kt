package com.iwedia.cltv.platform.model

interface IAsyncCallback {
    fun onFailed(error: Error)

    fun onSuccess()
}