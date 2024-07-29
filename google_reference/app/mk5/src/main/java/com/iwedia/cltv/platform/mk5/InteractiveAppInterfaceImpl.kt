package com.iwedia.cltv.platform.mk5

import android.content.Context
import com.iwedia.cltv.platform.`interface`.InteractiveAppInterface

open class InteractiveAppInterfaceImpl(val context: Context) : InteractiveAppInterface {

    override fun sendKeyToInteractiveApp(keyCode: Int, buttonDown : Boolean) : Boolean {
        return false
    }

}