package com.iwedia.cltv.platform.base

import android.content.Context
import com.iwedia.cltv.platform.`interface`.InteractiveAppInterface

open class InteractiveAppInterfaceBaseImpl(val context: Context) : InteractiveAppInterface {

    override fun sendKeyToInteractiveApp(keyCode: Int, buttonDown : Boolean) : Boolean {
        return false
    }

}