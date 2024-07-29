package com.iwedia.cltv.platform.iwatsc3

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import com.iwedia.cltv.platform.`interface`.InteractiveAppInterface
import com.mediatek.dtv.tvinput.atsc3tuner.common.IDefines
import com.mediatek.dtv.tvinput.atsc3tuner.common.IIntrAppControl
import com.mediatek.dtv.tvinput.atsc3tuner.common.IIntrAppStatusCallback
import com.mediatek.dtv.tvinput.atsc3tuner.common.IIwediaTis

open class InteractiveAppInterfaceImpl(val context: Context) : InteractiveAppInterface {

    val TAG = "InteractiveAppInterfaceImpl: "

    private var mService: IIwediaTis? = null
    private var mIAppService: IIntrAppControl? = null
    private var iAppIndex : Int = -1

    var interactiveAppStatus : Boolean = false
    var greenButtonPressed : Boolean = false

    fun setService(iwediaTisService: IIwediaTis) {
        mService = iwediaTisService

        mIAppService = IIntrAppControl.Stub.asInterface(mService!!.queryBinder(IDefines.BINDER_TYPE_BROWSER_STATUS));

        iAppIndex = mIAppService!!.registerIntrAppStatusCallback(object :
            IIntrAppStatusCallback.Stub() {
            override fun asBinder(): IBinder {
                return this
            }

            override fun onReceiveIntrAppStatus(status: Boolean) {
                interactiveAppStatus = status
            }

            override fun onReceiveIntrAppKeyList(bundleAcquiredKeys: Bundle?) {
            }
        } )
    }

    override fun sendKeyToInteractiveApp(keyCode: Int, buttonDown : Boolean) : Boolean {

        if(buttonDown == true) {
            if (keyCode == KeyEvent.KEYCODE_PROG_GREEN) {
                if (greenButtonPressed == true) {
                    Toast.makeText(context, "All buttons to the main LiveTV app", Toast.LENGTH_LONG).show()
                    greenButtonPressed = false
                } else {
                    Toast.makeText(context, "All buttons to the interactive app", Toast.LENGTH_LONG).show()
                    greenButtonPressed = true
                }
            }

            if (greenButtonPressed) {
                if (keyCode != KeyEvent.KEYCODE_PROG_GREEN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        return true
                    } else {
                        val intent = Intent("com.iwedia.cltv.keyFromLiveTv")
                        intent.putExtra("KEYCODE", keyCode)
                        context.sendBroadcast(intent)

                        return true
                    }
                }
            }
        }
        else {
            if (greenButtonPressed) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        val intentEnter = Intent("com.iwedia.cltv.keyFromLiveTv")
                        intentEnter.putExtra("KEYCODE", keyCode)
                        context.sendBroadcast(intentEnter)

                        return true
                    }

                    KeyEvent.KEYCODE_BACK -> {
                        val intentBack = Intent("com.iwedia.cltv.keyFromLiveTv")
                        intentBack.putExtra("KEYCODE", keyCode)
                        context.sendBroadcast(intentBack)

                        return true
                    }

                    else -> {
                        return false
                    }
                }
            }
        }

        return false
    }
}