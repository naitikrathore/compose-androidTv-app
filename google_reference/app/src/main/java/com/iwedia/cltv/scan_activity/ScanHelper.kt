package com.iwedia.cltv.scan_activity

import android.content.*
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.BuildConfig
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.platform.model.Constants
import core_entities.Error
import listeners.AsyncReceiver
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList


object ScanHelper {

    val ACTION_START_INSTALL = "com.google.android.tv.dtvscan.action.ACTION_START_INSTALL"
    val EXTRA_START_INSTALL_PARAM = "params"
    val ACTION_CANCEL_INSTALL = "com.google.android.tv.dtvscan.action.ACTION_CANCEL_INSTALL"
    val INSTALL_EVENT = "com.google.android.tv.dtvscan.INSTALL_EVENT"
    val EXTRA_PROGRESS_VALUE = "progress"
    val EXTRA_NB_CHANNELS_VALUE = "nb_channels"
    val EXTRA_STATUS_VALUE = "status"
    val ACTION_START_UNINSTALL = "com.google.android.tv.dtvscan.action.ACTION_START_UNINSTALL"
    val EXTRA_SATELLITE_VALUE = "satellites"
    val UNINSTALL_EVENT = "com.google.android.tv.dtvscan.UNINSTALL_EVENT"
    val ACTION_TUNE = "com.google.android.tv.dtvscan.action.ACTION_TUNE"
    val ACTION_CHECK_SIGNAL = "com.google.android.tv.dtvscan.action.ACTION_CHECK_SIGNAL"
    val SIGNAL_EVENT = "com.google.android.tv.dtvscan.SIGNAL_EVENT"
    val EXTRA_SIGNAL_QUALITY = "signal-quality"
    val EXTRA_SIGNAL_LEVEL = "signal-level"
    val EXTRA_SIGNAL_LOSS = "signal-loss"

    val TAG = "ScanHelper: "
    var channelNum = 42

    var activeFragment: com.iwedia.cltv.scan_activity.GenericFragment? = null
    var startFrequency = 0
    var endFrequency = 0
    var clearAllChannels = false

    var packageName: String = ""

    var isReceiverRegistered = false

    fun registerReceiver() {
        isReceiverRegistered = true
        val filter = IntentFilter()
        filter.addAction(INSTALL_EVENT)
        filter.addAction(SIGNAL_EVENT)
        ReferenceApplication.applicationContext()!!.registerReceiver(broadcastReceiver, filter)

        if (BuildConfig.FLAVOR.contains("mtk")) {
            packageName = "com.iwedia.scanmanager"
        } else {
            packageName = "com.google.android.tv.dtvinput"
        }
    }

    fun unregisterReceiver() {
        if (isReceiverRegistered) {
            try {
                ReferenceApplication.applicationContext()!!.unregisterReceiver(broadcastReceiver)
                val stopTuneIntent = Intent(com.iwedia.cltv.scan_activity.IwediaSetupActivity.SIGNAL_EVENT_STOP)
                stopTuneIntent.setPackage("com.iwedia.scanmanager")
                Log.d(Constants.LogTag.CLTV_TAG + "ScanHelper :", "iWedia sending stop tune")
                ReferenceApplication.applicationContext().sendBroadcast(
                    stopTuneIntent,
                    "com.google.android.tv.dtvinput.permission.INSTALL_TV_CHANNELS"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isReceiverRegistered = false
        }
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            val action = intent.action
            if (action == INSTALL_EVENT) {
                val progress = intent.getIntExtra(EXTRA_PROGRESS_VALUE, 0)
                val status = intent.getStringExtra(EXTRA_STATUS_VALUE)
                channelNum = intent.getIntExtra(EXTRA_NB_CHANNELS_VALUE, -1)


                Log.d(Constants.LogTag.CLTV_TAG +
                    TAG,
                    "INSTALL_EVENT, status: $status, progress: $progress, channelNum: $channelNum"
                )

                ReferenceApplication.runOnUiThread(Runnable {
                    activeFragment!!.scanTuneFrequencyChanged(getScanFrequencyInfo(progress))
                    activeFragment!!.updateProgress(progress)
                    activeFragment!!.scanTvServiceNumber(channelNum)
                    if (status == "complete" || status == "failed") {
                        activeFragment!!.scanFinished()
                    }
                })

            } else if (action == SIGNAL_EVENT) {
                Log.d(Constants.LogTag.CLTV_TAG +
                    TAG,
                    "SIGNAL_EVENT"
                )
                val signalQuality = intent.getIntExtra(EXTRA_SIGNAL_QUALITY, 0)
                val signalLevel = intent.getIntExtra(EXTRA_SIGNAL_LEVEL, 0)
                val signalLoss = intent.getBooleanExtra(EXTRA_SIGNAL_LOSS, true)

                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Received signal event:$signalQuality : $signalLevel : $signalLoss")
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Active fragment:$activeFragment")
                activeFragment!!.scanSignalQualityChanged(signalQuality)
                activeFragment!!.scanSignalStrengthChanged(signalLevel)
            }

        }
    }

    fun startTune(frequency: Int) {
        val to = JSONObject()
        try {
            to.put("type", "dvb-t2")
            to.put("frequency", frequency)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val startTuneIntent = Intent(ACTION_TUNE)
        startTuneIntent.setPackage("com.iwedia.scanmanager")
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "iWediaTune sending tune intent:$to")
        startTuneIntent.putExtra(EXTRA_START_INSTALL_PARAM, to.toString())
        ReferenceApplication.applicationContext().sendBroadcast(
            startTuneIntent,
            "com.google.android.tv.dtvinput.permission.INSTALL_TV_CHANNELS"
        )
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "iWediaTune sending check signal intent")
        val checkSignalIntent = Intent(ACTION_CHECK_SIGNAL)
        startTuneIntent.setPackage("com.iwedia.scanmanager")
        ReferenceApplication.applicationContext().sendBroadcast(
            checkSignalIntent,
            "com.google.android.tv.dtvinput.permission.INSTALL_TV_CHANNELS"
        )
    }

    private fun getScanFrequencyInfo(progress: Int): Int {
        val temp = endFrequency - startFrequency
        val p = ((progress.toDouble() / 100) * temp)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "p $p")
        return (startFrequency + p).toInt()
    }

    fun stopScan() {
        val cancelIntent = Intent(ACTION_CANCEL_INSTALL)
        ReferenceApplication.applicationContext()!!.sendBroadcast(cancelIntent)
        unregisterReceiver()
        startFrequency = 0
        endFrequency = 0
    }

    fun startAutoScan() {
        registerReceiver()
        startFrequency = 474000
        endFrequency = 874000
        /*val autoScanJson =
            """{"type":"dvb-t2","country":"","custom-scan":true,"frequency-min":474000,"frequency-max":874000,"frequency-step":8000,"bandwidth":8000}"""*/

        val autoScanJson =
            """{"type":"dvb-t2","country":"","custom-scan":false,"slots":[{"frequency-min":474000,"frequency-max":874000,"frequency-step":8000,"bandwidth":8000}], "clear-channels":$clearAllChannels}}"""
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Start auto scan $autoScanJson")

        // Setup scan intent
        val startScanIntent =
            Intent("com.google.android.tv.dtvscan.action.ACTION_START_INSTALL")
        startScanIntent.setPackage(packageName)
        startScanIntent.putExtra("params", autoScanJson)

        // Broadcast scan intent
        ReferenceApplication.applicationContext()!!.sendBroadcast(
            startScanIntent,
            "com.google.android.tv.dtvinput.permission.INSTALL_TV_CHANNELS"
        )
    }


    @RequiresApi(Build.VERSION_CODES.R)
    fun startManualScan(frequency: Int, bandwidth: Int) {
        registerReceiver()
        var callback = object : AsyncReceiver {
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Manual scan $frequency $bandwidth")

                startFrequency = frequency
                endFrequency = frequency
                val manualScanJson =
                    """{"type":"dvb-t2","country":"","custom-scan":true,"frequency-min":$frequency,"frequency-max":$frequency,"frequency-step":$bandwidth,"bandwidth":$bandwidth, "clear-channels":$clearAllChannels}"""
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Manual scan json $manualScanJson")

                // Setup scan intent
                val startScanIntent =
                    Intent("com.google.android.tv.dtvscan.action.ACTION_START_INSTALL")
                startScanIntent.setPackage(packageName)
                startScanIntent.putExtra("params", manualScanJson)

                // Broadcast scan intent
                ReferenceApplication.applicationContext()!!.sendBroadcast(
                    startScanIntent,
                    "com.google.android.tv.dtvinput.permission.INSTALL_TV_CHANNELS"
                )
            }

            override fun onFailed(error: Error?) {
                TODO("Not yet implemented")
            }
        }
        if (clearAllChannels) {
            clearChannels(callback)
        } else {
            callback.onSuccess()
        }
    }

    fun startDvbCManualScan(networkId: Int, frequency: Int, modulation: String, symbolRate: Int) {
        registerReceiver()

        val manualScanJson =
            """{"type":"dvb-c","country":"","network-id":$networkId,"frequency":$frequency,"modulation":"$modulation","symbol-rate":$symbolRate,"qam-annex":"a"}"""
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Manual scan json $manualScanJson")

        // Setup scan intent
        val startScanIntent =
            Intent("com.google.android.tv.dtvscan.action.ACTION_START_INSTALL")
        startScanIntent.setPackage(packageName)
        startScanIntent.putExtra("params", manualScanJson)

        // Broadcast scan intent
        ReferenceApplication.applicationContext()!!.sendBroadcast(
            startScanIntent,
            "com.google.android.tv.dtvinput.permission.INSTALL_TV_CHANNELS"
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun clearChannels(callback: AsyncReceiver) {

        val inputList = ArrayList<String>()
        //Get all TV inputs
        for (input in (ReferenceApplication.applicationContext().getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager).tvInputList) {
            val inputId = input.id
            inputList.add(inputId)
        }
        if (inputList!!.isNotEmpty()) {
            for (input in inputList) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ clear channels for input id $input")
                val contentResolver: ContentResolver = ReferenceApplication.applicationContext().contentResolver
                contentResolver.delete(TvContract.buildChannelsUriForInput(input), null)
            }
        }
    }
}