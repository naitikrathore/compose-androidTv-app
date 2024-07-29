package com.iwedia.cltv.sdk.handlers

import data_type.GLong
import handlers.PlatformHandler
import java.util.*

class ReferencePlatformHandler : PlatformHandler() {

    override fun getCurrentTime(): GLong {
        return GLong(Date().time.toString())
    }
}