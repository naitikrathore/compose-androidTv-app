package com.iwedia.cltv.platform.mal_service.network

import android.net.ConnectivityManager
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.iwedia.cltv.platform.`interface`.NetworkInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.fast_backend_utils.FastUrlHelper
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

open class NetworkInterfaceImpl(
    private val connectivityManager: ConnectivityManager
) : NetworkInterface {

    val TAG = "NetworkInterfaceImpl"
    override var anokiServerStatus = MutableLiveData<Boolean>(false)
    override val networkStatus by lazy {
        ConnectionLiveData(connectivityManager) { hasConnection ->
            if (hasConnection) {
                CoroutineScope(Dispatchers.IO).launch {
                    var baseUrl = URL(FastUrlHelper.BASE_URL)
                    try {
                        var urlConnection: HttpURLConnection =
                            baseUrl.openConnection() as HttpURLConnection
                        urlConnection.setConnectTimeout(5 * 1000)
                        urlConnection.connect()
                        //here we reconnected to fast server
                        if (anokiServerStatus.value == false) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Connection ok, sending PROVIDER_RESTORED_EVENT")
                            anokiServerStatus.postValue(true)
                            InformationBus.informationBusEventListener.submitEvent(Events.ANOKI_SERVER_REACHABLE)
                        }
                    } catch (e: IOException) {
                        if (anokiServerStatus.value == true) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Connection failed, sending NO_PROVIDER_EVENT")
                            anokiServerStatus.postValue(false)
                            InformationBus.informationBusEventListener.submitEvent(Events.ANOKI_SERVER_NOT_REACHABLE)
                        }
                    }
                }
            } else {
                anokiServerStatus.postValue(false)
                InformationBus.informationBusEventListener.submitEvent(Events.ANOKI_SERVER_NOT_REACHABLE)
            }
        }
    }
}