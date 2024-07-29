package com.iwedia.cltv.sdk.handlers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.ScanResult
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.iwedia.cltv.sdk.NetworkStateChangedEvent
import com.iwedia.cltv.sdk.ReferenceEvents
import com.iwedia.cltv.sdk.ReferenceNetworkAvailabilityEvent
import core_entities.Error
import core_entities.Ethernet
import core_entities.Wifi
import data_type.GList
import handlers.NetworkHandler
import listeners.AsyncDataReceiver
import listeners.AsyncReceiver
import utils.information_bus.Event
import utils.information_bus.InformationBus
import java.sql.Ref
import java.util.*

class ReferenceNetworkHandler : NetworkHandler<Wifi, Ethernet> {

    /**
     * Context
     */
    private var context: Context? = null

    /**
     * Connectivity manager
     */
    private var connectivityManager: ConnectivityManager? = null

    /**
     * Wifi manager
     */
    private var wifiManager: WifiManager? = null

    /**
     * List of scanned wifi
     */
    private var wifiScanList: List<ScanResult>? = null

    /**
     * Wifi scan receiver
     */
    private var wifiReceiver: WifiScanReceiver? =
        null

    /**
     * Wifi state receiver
     */
    private var wifiStateReceiver: WifiStateReceiver? =
        null

    /**
     * Network state receiver
     */
    private var networkStateReceiver: NetworkStateReceiver? =
        null

    /**
     * Connection receiver
     */
    private var connectionReceiver: ConnectionReceiver? =
        null

    /**
     * Wifi network info
     */
    private var wifiNetworkInfo: NetworkInfo? = null

    /**
     * Ethernet network info
     */
    private var ethernetNetworkInfo: NetworkInfo? = null

    /**
     * Is network available
     */
    private var isNetworkAvailable = false

    /**
     * Was network available before
     */
    private var wasNetworkAvailable = false

    /**
     * Constructor
     */
    constructor(context: Context) {
        this.context = context
        setup()
    }

    override fun waitForNetworkAvailable(callback: AsyncReceiver) {
        super.waitForNetworkAvailable(callback)
    }

    override fun setup() {
        connectivityManager =
            context!!.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager!!.registerDefaultNetworkCallback(ConnectionStateMonitor())
        }

        wifiManager = context!!.getSystemService(WifiManager::class.java) as WifiManager
//        wifiManager!!.isWifiEnabled = true

        wifiScanList = ArrayList<ScanResult>()
        wifiReceiver = WifiScanReceiver()
        networkStateReceiver = NetworkStateReceiver()
        wifiStateReceiver = WifiStateReceiver()

        connectionReceiver = ConnectionReceiver()
        val intentFilterState = IntentFilter()
        intentFilterState.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
        context!!.registerReceiver(connectionReceiver, intentFilterState)

        //wifi receiver registration
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context!!.registerReceiver(wifiReceiver, intentFilter)

        //register wifi state
        val intentFilter1 = IntentFilter()
        intentFilter1.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
        context!!.registerReceiver(wifiStateReceiver, intentFilter1)

        //is network available intent
        val intentFilter2 = IntentFilter()
        intentFilter1.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        context!!.registerReceiver(networkStateReceiver, intentFilter2)

        if (isInternetAvailable(context!!)) {
            InformationBus.submitEvent(ReferenceNetworkAvailabilityEvent(true))
        } else {
            InformationBus.submitEvent(ReferenceNetworkAvailabilityEvent(false))
        }
    }

    override fun dispose() {
        context!!.unregisterReceiver(wifiReceiver)
        context!!.unregisterReceiver(wifiStateReceiver)
        context!!.unregisterReceiver(networkStateReceiver)
        context!!.unregisterReceiver(connectionReceiver)
    }

    override fun isEthernetConnected(): Boolean {
        val connectivityManager =
            context!!.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo != null && networkInfo.type == ConnectivityManager.TYPE_ETHERNET && networkInfo.isConnected) {
            return true
        }
        return false
    }

    fun getMyEthernetInfo(callback: AsyncDataReceiver<NetworkInfo>) {
        ethernetNetworkInfo =
            connectivityManager!!.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET)
        if (ethernetNetworkInfo != null) {
            callback.onReceive(ethernetNetworkInfo!!)
        } else {
            callback.onFailed(Error(501, "There is no information about ethernet"))
        }
    }

    fun getMyConnectedWiFiInfo(callback: AsyncDataReceiver<NetworkInfo>?) {
        wifiNetworkInfo = connectivityManager!!.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        if (wifiNetworkInfo!!.isConnected) {
            callback!!.onReceive(wifiNetworkInfo!!)
        } else {
            callback!!.onFailed(Error(502, "Wifi is not connected"))
        }
    }


    override fun isNetworkAvailable(callback: AsyncDataReceiver<Any>) {
        wifiNetworkInfo = connectivityManager!!.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        ethernetNetworkInfo =
            connectivityManager!!.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET)

        if (wifiNetworkInfo != null) {
            callback.onReceive(wifiNetworkInfo!!)
            wasNetworkAvailable = false
            isNetworkAvailable = true
        } else if (ethernetNetworkInfo != null) {
            callback.onReceive(ethernetNetworkInfo!!)
            wasNetworkAvailable = false
            isNetworkAvailable = true
        } else {
            isNetworkAvailable = false
            wasNetworkAvailable = false
            callback.onFailed(Error(501, "There are no available networks"))
        }
    }

    fun isInternetAvailable(context: Context): Boolean {

        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivityManager.activeNetworkInfo?.run {
                result = when (type) {
                    ConnectivityManager.TYPE_WIFI -> true
                    ConnectivityManager.TYPE_MOBILE -> true
                    ConnectivityManager.TYPE_ETHERNET -> true
                    else -> false
                }

            }
        }

        return result
    }

    fun isEthernetAvailable(context: Context): Boolean {
        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            connectivityManager.activeNetworkInfo?.run {
                result = type == ConnectivityManager.TYPE_ETHERNET
            }
        }
        return result
    }

    fun isWifiAvailable(context: Context): Boolean {
        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            connectivityManager.activeNetworkInfo?.run {
                result = type == ConnectivityManager.TYPE_WIFI
            }
        }
        return result
    }


    override fun isWifiConnected(): Boolean {
        val connectivityManager =
            context!!.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        if (connectivityManager != null) {
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null && networkInfo.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected) {
                return true
            }
        }
        return false
    }


    override fun registerNetworkAvailableCallback(callback: AsyncReceiver?) {
        super.registerNetworkAvailableCallback(callback)
    }

    override fun registerWifiStateCallback(callback: AsyncDataReceiver<Any>?) {
        if (wifiNetworkInfo != null) {
            callback!!.onReceive(wifiNetworkInfo!!)
        } else {
            callback!!.onFailed(Error(508, "Could not register wifi state"))
        }
    }

    /**
     * Wifi Scan Receiver
     */
    inner class WifiScanReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //scan wifi networks
            wifiScanList = wifiManager!!.getScanResults()

            //todo
            //            InformationBus
//                .submitEvent(AvailableNetworksListChangedEvent(wifiScanList))
        }
    }

    /**
     * Network state receiver
     */
    inner class NetworkStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            wifiNetworkInfo = connectivityManager!!.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            ethernetNetworkInfo =
                connectivityManager!!.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET)
            isNetworkAvailable = ethernetNetworkInfo != null || wifiNetworkInfo != null
            registerNetworkStateChanged(isNetworkAvailable)


//            NeonSdk.get().getListener()
//                .showNotification(NeonNotification("Network available $isNetworkAvailable"))
        }
    }

    /**
     * Wifi state receiver
     */
    inner class WifiStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //register wifi state (parameter inside wifiNetworkInfo)
            wifiNetworkInfo = connectivityManager!!.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        }
    }

    private class ConnectionStateMonitor : NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            InformationBus.submitEvent(ReferenceNetworkAvailabilityEvent(true))
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            InformationBus.submitEvent(ReferenceNetworkAvailabilityEvent(false))
        }
    }

    /**
     * If network status changed, this method submit Event
     *
     * @param state
     */
    fun registerNetworkStateChanged(state: Boolean) {
        if (state) {
            if (!wasNetworkAvailable) {
                InformationBus.submitEvent(Event(ReferenceEvents.NETWORK_AVAILABILITY))
            }
        } else {
            if (wasNetworkAvailable) {
                InformationBus.submitEvent(Event(ReferenceEvents.NETWORK_UNAVAILABILITY))
            }
        }
    }

    /**
     * Connection receiver
     */
    class ConnectionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SUPPLICANT_STATE_CHANGED_ACTION) {
                val supl_state =
                    intent.getParcelableExtra<SupplicantState>(WifiManager.EXTRA_NEW_STATE)
                when (supl_state) {
                    SupplicantState.COMPLETED -> InformationBus
                        .submitEvent(NetworkStateChangedEvent(WifiStateType.COMPLETED))
                    else -> {

                    }
                }
                val supl_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1)
                if (supl_error == WifiManager.ERROR_AUTHENTICATING) {
                    InformationBus
                        .submitEvent(NetworkStateChangedEvent(WifiStateType.ERROR_AUTHENTICATING))
                }
            }
        }
    }

    /**
     * WiFi state type
     */
    enum class WifiStateType {
        COMPLETED, ERROR_AUTHENTICATING
    }
}