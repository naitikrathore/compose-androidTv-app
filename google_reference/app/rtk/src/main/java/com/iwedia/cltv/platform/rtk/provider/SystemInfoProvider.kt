package com.iwedia.cltv.platform.rtk.provider
import android.os.Build
import androidx.annotation.RequiresApi
import android.content.Context
import android.util.Log
import com.iwedia.cltv.platform.base.content_provider.TifSystemInfoProvider
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.SystemInfoData
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.rtk.UtilsInterfaceImpl
import java.util.*

class SystemInfoProvider internal constructor(var context: Context, utilsInterface: UtilsInterfaceImpl) : TifSystemInfoProvider() {
    val TAG = javaClass.simpleName

    private var utilsInterfaceImpl :UtilsInterfaceImpl? = null
    init {
        utilsInterfaceImpl = utilsInterface
    }


    @RequiresApi(Build.VERSION_CODES.P)
    override fun getSystemInfoData(tvChannel: TvChannel, callback: IAsyncDataCallback<SystemInfoData>) {
        CoroutineHelper.runCoroutine({
            val retVal = SystemInfoData()
            val tvSettings = utilsInterfaceImpl?.getTvSetting()
            val tvChannel = tvSettings?.dtvCurChInfo

            if (tvSettings != null) {
                retVal.signalStrength =tvSettings.signalStrength
                try {
                    retVal.signalQuality = tvSettings.signalQuality
                    retVal.networkId = tvChannel?.networkId!!
                    retVal.frequency = tvChannel.frequency
                    retVal.bandwidth = tvChannel.bandWidth.toString()
                    retVal.networkName = tvSettings.currentNetworkName
                    retVal.signalBer  = tvSettings.signalBer.toInt()

                    retVal.postViterbi = String.format(Locale.ENGLISH, "%3.2e", retVal.signalBer.toDouble() / (100 * 1000))
                    retVal.attr5s = String.format(Locale.ENGLISH, "%3.2e", retVal.signalBer.toDouble() / (100 * 1000))
                }catch (e:Exception){
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getSystemInfoData: $e")
                }
            }

            callback.onReceive(retVal)
        })
    }

}