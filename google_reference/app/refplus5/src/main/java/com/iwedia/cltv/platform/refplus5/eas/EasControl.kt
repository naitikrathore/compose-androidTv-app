package com.iwedia.cltv.platform.refplus5.eas

import android.content.Context
import android.net.Uri
import android.os.Bundle

import com.iwedia.cltv.platform.model.eas.EasEventInfo
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.refplus5.SaveValue

import com.mediatek.dtv.tvinput.framework.tifextapi.common.eas.Constants

import java.util.ArrayList

class EasControl(private val context: Context) {
    private val TAG = javaClass.simpleName

    companion object {
        const val AUTHORITY = "com.mediatek.tv.internal.data"
        private const val GLOBAL_PROVIDER_ID = "global_value"
        const val GLOBAL_VALUE_KEY = "key"
        const val GLOBAL_VALUE_VALUE = "value"
        const val GLOBAL_VALUE_STORED = "stored"
        val GLOBAL_PROVIDER_URI: Uri = Uri.parse("content://$AUTHORITY/$GLOBAL_PROVIDER_ID")

        //Columns
        const val EVENT_EAS_START = Constants.EVENT_EAS_START
        const val EVENT_EAS_STOP = Constants.EVENT_EAS_STOP
        const val KEY_EAS_INFO = Constants.KEY_EAS_INFO
        const val KEY_EAS_IS_CHANNEL_CHANGE = Constants.KEY_EAS_IS_CHANNEL_CHANGE
        const val KEY_EAS_CHANNEL_CHANGE = Constants.KEY_EAS_CHANNEL_CHANGE_URI

        const val EAS_STATUS = "EAS_STATUS"
        const val EAS_KEY_IS_CHANNEL_CHANGE = "EAS_KEY_IS_CHANNEL_CHANGE"
    }

    fun onEvent(inputId: String, eventType: String, eventArgs: Bundle) {
        when (eventType) {
            EVENT_EAS_START -> {
                SaveValue.saveWorldValue(context, EAS_STATUS, "true", false)
                SaveValue.saveWorldValue(context,
                    EAS_KEY_IS_CHANNEL_CHANGE,
                    (eventArgs.getBoolean(KEY_EAS_IS_CHANNEL_CHANGE) ?: false).toString(),
                    false)

                sendEASNotification(1, eventArgs)
            }
            EVENT_EAS_STOP -> {
                SaveValue.saveWorldValue(context, EAS_STATUS, "false", false)
                SaveValue.saveWorldValue(
                    context,
                    EAS_KEY_IS_CHANNEL_CHANGE,
                    (eventArgs.getBoolean(KEY_EAS_IS_CHANNEL_CHANGE) ?: false).toString(),
                    false)

                sendEASNotification(0, eventArgs)
            }
        }
    }

    private fun sendEASNotification(msgType: Int, eventArgs: Bundle) {
        val list = ArrayList<Any>()
        list.add(msgType)
        list.add(
            EasEventInfo(
                eventArgs.getString(KEY_EAS_INFO),
                "",
                false,
                eventArgs?.getString(KEY_EAS_CHANNEL_CHANGE),
                eventArgs?.getBoolean(KEY_EAS_IS_CHANNEL_CHANGE)
            )
        )

        InformationBus.informationBusEventListener.submitEvent(
            Events.IS_EAS_PLAYING, list
        )
    }
}