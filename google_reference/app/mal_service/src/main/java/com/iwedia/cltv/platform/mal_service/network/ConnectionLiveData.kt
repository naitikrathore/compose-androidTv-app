package com.iwedia.cltv.platform.mal_service.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.LiveData
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.fast_backend_utils.FastUrlHelper
import com.iwedia.cltv.platform.model.network.NetworkData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import javax.net.SocketFactory


class ConnectionLiveData(
    private val cm: ConnectivityManager,
    private val connectionCallback: (hasConnection: Boolean) -> Unit
) : LiveData<NetworkData>() {

    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private val validNetworks: HashMap<Long, NetworkData.Network> = HashMap()

    override fun onActive() {
        networkCallback = createNetworkCallback()
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun onInactive() {
        cm.unregisterNetworkCallback(networkCallback)
    }

    private fun addValidNetworks(network: Network) {
        val net =
            NetworkData.Network(cm.getNetworkCapabilities(network), cm.getLinkProperties(network))
        validNetworks[network.networkHandle] = net
        connectionCallback.invoke(true)
        postValue(net)
    }

    private fun removeNetwork(network: Network) {
        validNetworks.remove(network.networkHandle)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onLost: $network, validNetworks.size [${validNetworks.size}]")

        if (validNetworks.isEmpty()) {
            connectionCallback.invoke(false)
            postValue(NetworkData.NoConnection)
        } else {
            connectionCallback.invoke(true)
            postValue(validNetworks.values.first())
        }
    }

    private fun createNetworkCallback() = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            val networkCapabilities = cm.getNetworkCapabilities(network)
            val hasInternetCapability = networkCapabilities?.hasCapability(NET_CAPABILITY_INTERNET)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onAvailable: $network, $hasInternetCapability")
            if (hasInternetCapability == true) {
                checkIfNetworkHasInternet(network)
            }
        }

        private fun checkIfNetworkHasInternet(network: Network) =
            CoroutineScope(Dispatchers.IO).launch {
                val hasInternet = DoesNetworkHaveInternet.execute(network.socketFactory)
                if (hasInternet) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Device has internet")
                    withContext(Dispatchers.Main) {
                        addValidNetworks(network)
                    }
                }
            }

        override fun onLost(network: Network) = removeNetwork(network)
    }

    private object DoesNetworkHaveInternet {

        fun execute(socketFactory: SocketFactory): Boolean {
            return try {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "PINGING google.")
                val address =
                    InetAddress.getByName("www.google.com")
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "address.toString $address")
                address.toString() != ""
            } catch (exception: IOException) {
                //Fallback, try connection to Anoki server
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "No connection to Google DNS. $exception")
                var urlConnection: HttpURLConnection? = null
                try {
                    var baseUrl = URL(FastUrlHelper.BASE_URL)
                    urlConnection = baseUrl.openConnection() as HttpURLConnection
                    urlConnection.connectTimeout = 5 * 1000
                    urlConnection.connect()
                    true
                } catch (exception: IOException) {
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "No connection to Internet. $exception")
                    false
                } finally {
                    urlConnection?.disconnect()
                }
            }
        }
    }

    companion object {
        const val TAG = "NetworkHandler"
    }
}