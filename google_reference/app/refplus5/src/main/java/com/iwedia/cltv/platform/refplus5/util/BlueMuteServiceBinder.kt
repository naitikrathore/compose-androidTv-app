package com.iwedia.cltv.platform.refplus5.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.iwedia.cltv.platform.model.Constants
import com.mediatek.extservice.IRenderService
import com.mediatek.extservice.rendertypes.ST_MUTE_COLOR_Parcel

class BlueMuteServiceBinder(
  private val context: Context,
  private val bindIntent: Intent,
) {
  private var isBlueMuteEnabled: Boolean = false
  private val TAG = javaClass.simpleName

  private val serviceConnection: ServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
     var serviceBinder = IRenderService.Stub.asInterface(service)

     try {
       val color = if (isBlueMuteEnabled) ST_MUTE_COLOR_Parcel(0, 0, 255) else ST_MUTE_COLOR_Parcel(0, 0, 0)
       serviceBinder.setMuteColor(color)
       serviceBinder.enableColorMute(isBlueMuteEnabled)
     } catch (ex: Exception) {}
    }

    override fun onServiceDisconnected(name: ComponentName) {
      Log.d(Constants.LogTag.CLTV_TAG + TAG, "onServiceDisconnected$name")
    }

    override fun onBindingDied(name: ComponentName) {
      Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindingDied ComponentName:$name")
    }

    override fun onNullBinding(name: ComponentName) {
      Log.d(Constants.LogTag.CLTV_TAG + TAG, "onNullBinding ComponentName:$name")
    }
  }

  fun bindToService(enabled: Boolean) {
    isBlueMuteEnabled = enabled
    context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
  }

  /** unbind service on [Activity.onStop]  */
  fun unbind() {
      try {
        context.unbindService(serviceConnection)
      } catch (ex: Exception) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "unbind :$ex")
      }
  }
}