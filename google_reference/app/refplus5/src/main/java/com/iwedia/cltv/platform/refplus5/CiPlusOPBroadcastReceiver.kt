package com.iwedia.cltv.platform.refplus5

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG
import kotlin.concurrent.thread

class CiPlusOPBroadcastReceiver : BroadcastReceiver() {
    val ACTION_CAM_SCAN = "com.mediatek.dtv.tvinput.framework.intent.action.CAM_PROFILE_INFO_CHANGED"

    val KEY_CAM_PROFILE_NAME = "CAM_PROFILE_NAME"
    val KEY_CAM_PROFILE_TYPE = "CAM_PROFILE_TYPE"
    val KEY_NEED_SERVICE_UPDATE = "NEED_SERVICE_UPDATE"
    val KEY_CAM_DELIVERY_SYSTEM_HINT = "CAM_DELIVERY_SYSTEM_HINT"


    interface CiPlusScanStartListener {
        fun onStartScanReceived(profileType : Int , profileName: String, needServiceUpdate : Int, deliverySystemHint: Int)
    }

    private object WaitingNotification{
        var profileType = 0
        var profileName = ""
        var needServiceUpdate = 0
        var camDeliverySystemHint = 0
        var isWaiting = false
    }

    companion object {
        val TAG = "CiPlusOPBroadcastReceiver"
        var allowProfileInstallation = false
        var listeners : MutableList<CiPlusScanStartListener> = mutableListOf()
        fun registerScanStartListener(listener : CiPlusScanStartListener) {
            listeners.add(listener)

        }

        fun enableProfileInstallation() {
            allowProfileInstallation = true
            Log.d(CLTV_TAG + TAG, "enableProfileInstallation waiting ${WaitingNotification.isWaiting}")
            if(WaitingNotification.isWaiting) {
                WaitingNotification.isWaiting = false
                thread {
                    for(listener in listeners) {
                        Thread.sleep(5000)
                        listener.onStartScanReceived(
                            WaitingNotification.profileType,
                            WaitingNotification.profileName,
                            WaitingNotification.needServiceUpdate,
                            WaitingNotification.camDeliverySystemHint
                        )
                    }
                }
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if(ACTION_CAM_SCAN == intent?.action) {
            thread {
                var mCamProfileType = intent?.getIntExtra(KEY_CAM_PROFILE_TYPE, 0) ?: 0
                var profileName = intent?.getStringExtra(KEY_CAM_PROFILE_NAME) ?: ""
                val needServiceUpdate = intent?.getIntExtra(KEY_NEED_SERVICE_UPDATE, 0) ?: 0
                val camDeliverySystemHint = intent?.getIntExtra(KEY_CAM_DELIVERY_SYSTEM_HINT, 0) ?: 0
                if (TextUtils.isEmpty(profileName)) {
                    profileName = "CAM"
                }

                if(CiPlusInterfaceImpl.getInstance()?.getProfileSupport(context, camDeliverySystemHint) == false) {
                    Log.e(CLTV_TAG + TAG, "onReceive wrong tuner type, expected $camDeliverySystemHint, exiting")
                    return@thread
                }

                Log.d(CLTV_TAG + TAG, "onReceive type: $mCamProfileType profile name: $profileName need update: $needServiceUpdate hint: $camDeliverySystemHint ")
                Log.d(CLTV_TAG + TAG, "onReceive listeners ${listeners.size} allow profile installation $allowProfileInstallation")

                for(listener in listeners) {
                    if(allowProfileInstallation) {
                        listener.onStartScanReceived(mCamProfileType, profileName, needServiceUpdate, camDeliverySystemHint)
                    }
                }

                if((listeners.size == 0) || (!allowProfileInstallation)) {
                    WaitingNotification.profileType = mCamProfileType
                    WaitingNotification.profileName = profileName
                    WaitingNotification.camDeliverySystemHint = camDeliverySystemHint
                    WaitingNotification.needServiceUpdate = needServiceUpdate
                    WaitingNotification.isWaiting = true
                }
            }
        }
    }
}