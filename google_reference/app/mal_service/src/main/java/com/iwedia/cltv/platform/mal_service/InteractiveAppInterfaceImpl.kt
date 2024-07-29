package com.iwedia.cltv.platform.mal_service

import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.InteractiveAppInterface

class InteractiveAppInterfaceImpl(private val serviceImpl: IServiceAPI) : InteractiveAppInterface {
    override fun sendKeyToInteractiveApp(keyCode: Int, buttonDown: Boolean): Boolean {
        return serviceImpl.sendKeyToInteractiveApp(keyCode, buttonDown)
    }
}