package com.iwedia.cltv.fti.cable

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.SettingsActivity
import com.iwedia.cltv.assistant.ContentAggregatorService
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.fti.data.Channel
import com.iwedia.cltv.fti.handlers.ChannelHandler
import com.iwedia.cltv.fti.handlers.ConfigHandler
import com.iwedia.cltv.fti.terrestrial.ScanProgressFtiFragment
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.utils.Utils
import listeners.AsyncReceiver
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream
import java.lang.Boolean
import kotlin.Error
import kotlin.Exception
import kotlin.Float
import kotlin.Int
import kotlin.String
import kotlin.arrayOf

class CableScanProgressFtiFragment: Fragment()  {

    val TAG = javaClass.simpleName
    companion object {
        fun newInstance() = CableScanProgressFtiFragment()
    }

    var viewModel: View? = null

    var scanningProgressBar: ProgressBar? = null
    var signalStrengthBar: ProgressBar? = null
    var signalQualityBar: ProgressBar? = null
    var scanningProgressBarText: TextView? = null
    var signalQualityBarText: TextView? = null
    var signalStrengthBarText: TextView? = null

    var editStatusText: TextView? = null
    var editFrequencyText: TextView? = null
    var editProgrammesFoundText: TextView? = null

    var buttonScan: Button? = null

    var isScanInProgress = false
    var isScanCompleted = false
    var isReceiverRegistered = false

    var monitoringThread: Thread? = null
    private lateinit var channelListObserver: ContentObserver
    private var hasLcnConflicts = false

    var scanProgress = 0
    var channelsFound = 0

    private lateinit var mChannelsHandler: ChannelHandler
    private var mChannels: List<Channel>? = null

    //test
    private val AUTHORITY = "com.google.android.tv.dtvprovider"
    private val SIGNAL_STATUS_PATH = "streamers"
    private val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$SIGNAL_STATUS_PATH")
    private val projection = arrayOf(
        "id",
        "type",
        "state",
        "signal",
        "stats",
        "parameters"
    )
    var terSelectionClause = "type=? OR type=?"
    var terSelectionArgs = arrayOf(
        "TERRESTRIAL",
        "TERRESTRIAL_2"
    )

    val mManagedManual = "managed_manual"
    val mUnmanagedManual = "unmanaged_manual"
    val mFullScanMode = "full"


    private val FIELD_TYPE = "type"
    private val FIELD_SCAN_TYPE = "scan-type"
    private val FIELD_COUNTRY = "country"
    private val FIELD_NETWORK_ID = "network-id"
    private val FIELD_FREQUENCY = "frequency"
    private val FIELD_MODULATION = "modulation"
    private val FIELD_SYMBOL_RATE = "symbol-rate"
    private val FIELD_QAM_ANNEX = "qam-annex"
    private val FIELD_SCAN_CUSTOM = "custom-scan"
    private val FIELD_HOME_TS_ONLY = "one-ts"
    private val mTypeC = "dvb-c"

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = ReferenceApplication.applicationContext()
        viewModel = inflater.inflate(R.layout.fti_scan_progresss_layout, container, false)
        viewModel!!.background = ContextCompat.getDrawable(context, R.color.fti_left_black)

        //todo
        val linear_border1: LinearLayout = viewModel!!.findViewById(R.id.linear_border1)
        val linear_border2: LinearLayout = viewModel!!.findViewById(R.id.linear_border2)
        val linear_border3: LinearLayout = viewModel!!.findViewById(R.id.linear_border3)
//        val constraint_border1: ConstraintLayout = viewModel!!.findViewById(R.id.constraint_border1)
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            linear_border1.visibility = View.GONE
            linear_border2.visibility = View.GONE
            linear_border3.visibility = View.GONE
//            constraint_border1!!.background = ContextCompat.getDrawable(context!!, R.color.fti_left_background)
        }else{
            viewModel!!.background = ContextCompat.getDrawable(context, R.drawable.fti_bg_gtv)
        }

        val textViewScanText : TextView = viewModel!!.findViewById(R.id.textViewScanText)
        textViewScanText!!.text = ConfigStringsManager.getStringById("scan_cable")
        var param = textViewScanText!!.layoutParams as ViewGroup.MarginLayoutParams
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            param.setMargins(50 * context.getResources().getDisplayMetrics().density.toInt(), 150 * context.getResources().getDisplayMetrics().density.toInt(), 0, 50 * context!!.getResources().getDisplayMetrics().density.toInt())
            textViewScanText!!.layoutParams = param
            textViewScanText!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewScanText!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewScanText!!.gravity = Gravity.LEFT
            textViewScanText!!.textSize = 36f
            textViewScanText!!.includeFontPadding = false
        }else{
            param.setMargins(0, 124 * context.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            textViewScanText!!.layoutParams = param
            textViewScanText!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewScanText!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewScanText!!.gravity = Gravity.TOP
            textViewScanText!!.textSize = 32f
        }

        val textViewDescription: TextView = viewModel!!.findViewById(R.id.textViewDescription)
        textViewDescription.text = ConfigStringsManager.getStringById("scan_terrestrial_description")
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            param = textViewDescription!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(50 * context.getResources().getDisplayMetrics().density.toInt(), 20 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 50 * context!!.getResources().getDisplayMetrics().density.toInt())
            textViewDescription!!.layoutParams = param
            textViewDescription!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewDescription!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewDescription!!.gravity = Gravity.LEFT
            textViewDescription!!.includeFontPadding = false
            textViewDescription!!.layoutParams
        }else{
            param = textViewDescription.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 20 * context.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            textViewDescription.layoutParams = param
            textViewDescription!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewDescription!!.layoutParams.height = 96 * context!!.getResources().getDisplayMetrics().density.toInt()
            textViewDescription.gravity = Gravity.TOP
            textViewDescription!!.includeFontPadding = false
            textViewDescription.translationY = -1.98f
            textViewDescription.textSize = 16f
        }

        val imageViewLogo: ImageView = viewModel!!.findViewById(R.id.imageViewLogo)
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            param = imageViewLogo.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(
                50 * context!!.getResources().getDisplayMetrics().density.toInt(),
                50 * context!!.getResources().getDisplayMetrics().density.toInt(),
                0,
                0
            )
            imageViewLogo.layoutParams = param
            imageViewLogo!!.layoutParams.width =
                40 * context!!.getResources().getDisplayMetrics().density.toInt()
            imageViewLogo!!.layoutParams.height =
                40 * context!!.getResources().getDisplayMetrics().density.toInt()
        }else{
            param = imageViewLogo.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 96 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            imageViewLogo.layoutParams = param
            imageViewLogo!!.layoutParams.width = 20 * context!!.getResources().getDisplayMetrics().density.toInt()
            imageViewLogo!!.layoutParams.height = 20 * context!!.getResources().getDisplayMetrics().density.toInt()
        }
        var imagePath = ConfigHandler.getCompanyLogo()
        if (imagePath.isNotEmpty()) {
            Utils.loadImage(imagePath, imageViewLogo, object: AsyncReceiver {
                override fun onFailed(error: core_entities.Error?) {}
                override fun onSuccess() {}
            })
        } else {
            val ims: InputStream = (activity as SettingsActivity?)!!.assets.open("company_logo.png")
            val bitmap1 = BitmapFactory.decodeStream(ims)
            imageViewLogo.setImageBitmap(bitmap1)
        }

        scanningProgressBar = viewModel!!.findViewById(R.id.scanningProgressBar)
        signalStrengthBar = viewModel!!.findViewById(R.id.signalStrengthBar)
        signalQualityBar = viewModel!!.findViewById(R.id.signalQualityBar)
        tintHorizontalProgress(signalStrengthBar!!)
        tintIndeterminateProgress(signalStrengthBar!!)
        tintHorizontalProgress(scanningProgressBar!!)
        tintIndeterminateProgress(scanningProgressBar!!)
        tintHorizontalProgress(signalQualityBar!!)
        tintIndeterminateProgress(signalQualityBar!!)

        scanningProgressBarText = viewModel!!.findViewById(R.id.scanningProgressBarText)
        signalQualityBarText = viewModel!!.findViewById(R.id.signalQualityBarText)
        signalStrengthBarText = viewModel!!.findViewById(R.id.signalStrengthBarText)

        editStatusText = viewModel!!.findViewById(R.id.editStatusText)
        editFrequencyText = viewModel!!.findViewById(R.id.editFrequencyText)
        editProgrammesFoundText = viewModel!!.findViewById(R.id.editProgrammesFoundText)

        val textViewDescriptionChannel : TextView = viewModel!!.findViewById(R.id.textViewDescriptionChannel)
        textViewDescriptionChannel!!.text = ConfigStringsManager.getStringById("scanning_progress")

        val textViewDescriptionScan : TextView = viewModel!!.findViewById(R.id.textViewDescriptionScan)
        textViewDescriptionScan!!.text = ConfigStringsManager.getStringById("scanning_status")

        val textViewDescriptionFreq : TextView = viewModel!!.findViewById(R.id.textViewDescriptionFreq)
        textViewDescriptionFreq!!.text = ConfigStringsManager.getStringById("frequency_cable")

        val textViewDescriptionProgFound : TextView = viewModel!!.findViewById(R.id.textViewDescriptionProgFound)
        textViewDescriptionProgFound!!.text = ConfigStringsManager.getStringById("programmes_found")

        val textViewDescriptionSignalStatus : TextView = viewModel!!.findViewById(R.id.textViewDescriptionSignalStatus)
        textViewDescriptionSignalStatus!!.text = ConfigStringsManager.getStringById("signal_status")

        val textViewDescriptionSignalStrength : TextView = viewModel!!.findViewById(R.id.textViewDescriptionSignalStrength)
        textViewDescriptionSignalStrength!!.text = ConfigStringsManager.getStringById("signal_strength")

        val textViewDescriptionSignalQuality : TextView = viewModel!!.findViewById(R.id.textViewDescriptionSignalQuality)
        textViewDescriptionSignalQuality!!.text = ConfigStringsManager.getStringById("signal_quality")

        buttonScan = viewModel!!.findViewById(R.id.buttonScan)

        (activity as SettingsActivity?)!!.mChannelsHandler = null
        (activity as SettingsActivity?)!!.mChannels = null

        editStatusText!!.text = ""
        editFrequencyText!!.text = ""
        signalStrengthBarText!!.text = ""
        buttonScan!!.text = ConfigStringsManager.getStringById("stop")
        scanningProgressBar!!.setProgress(0)
        scanningProgressBarText!!.text = "${0} %"
        signalStrengthBar!!.setProgress(0)
        signalStrengthBarText!!.text = "${0} %"
        signalQualityBar!!.setProgress(0)
        signalQualityBarText!!.text = "${0} %"
        isReceiverRegistered = false

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreateView: IS SERVICE RUNNING ${isServiceRunning(
                ReferenceApplication.applicationContext(), ContentAggregatorService::class.java)}")
        //BroadcastReceiver sometimes doesn't start on reboot in that case start it here
        if (!isServiceRunning( ReferenceApplication.applicationContext(), ContentAggregatorService::class.java)) {
            startService( ReferenceApplication.applicationContext(), ContentAggregatorService::class.java)
        }

        buttonScan!!.setTextColor(ContextCompat.getColor( ReferenceApplication.applicationContext(),R.color.button_selected_text_fti))
        buttonScan!!.setBackgroundResource(R.drawable.my_small_action_button_selected)

        buttonScan!!.setFocusable(true)
        buttonScan!!.requestFocus()

        buttonScan!!.setOnClickListener {
            if(buttonScan!!.text == ConfigStringsManager.getStringById("stop") || buttonScan!!.text == ConfigStringsManager.getStringById("scan_back")){
                monitoringThread = null
                clearDatabase()
                (activity as SettingsActivity?)!!.goToCableManualScanFragment()
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction("com.google.android.tv.dtvscan.INSTALL_EVENT")
        intentFilter.addAction("com.google.android.tv.dtvscan.SIGNAL_EVENT")
        if (!isReceiverRegistered) {
            ReferenceApplication.applicationContext().registerReceiver(mProgressReceiver, intentFilter, null, null)
            isReceiverRegistered = true
        }
        if (isScanInProgress) {
            return viewModel!!
        }

        if ((activity as SettingsActivity?)!!.manualQuick == 0) {
            var country = (activity as SettingsActivity?)!!.countryName
            val network_id = (activity as SettingsActivity?)!!.buttonNetworkId
            val frequency = (activity as SettingsActivity?)!!.buttonFreq
            val symbol_rate = (activity as SettingsActivity?)!!.buttonSymbolRate
            val qam_mode = (activity as SettingsActivity?)!!.numberPickerChannelQam
            val standard = (activity as SettingsActivity?)!!.numberPickerChannelStandard
            startCableManualScan(country, network_id, frequency, symbol_rate, qam_mode, standard)
        } else if ((activity as SettingsActivity?)!!.manualQuick == 1) {
            var country = (activity as SettingsActivity?)!!.countryName
            val network_id = (activity as SettingsActivity?)!!.buttonNetworkId
            val frequency = (activity as SettingsActivity?)!!.buttonFreq
            val symbol_rate = (activity as SettingsActivity?)!!.buttonSymbolRate
            val qam_mode = (activity as SettingsActivity?)!!.numberPickerChannelQam
            val standard = (activity as SettingsActivity?)!!.numberPickerChannelStandard
            startCableQuickScan(country, network_id, frequency, symbol_rate, qam_mode, standard)
        }

        runSignalMonitoringThread()

        return viewModel!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mChannelsHandler = ChannelHandler(((activity as SettingsActivity?)!!))
        mChannelsHandler!!.updateChannelList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isReceiverRegistered) {
            ReferenceApplication.applicationContext().unregisterReceiver(mProgressReceiver)
            try{
                ReferenceApplication.applicationContext().getContentResolver().unregisterContentObserver(contentObserver)
            }catch (e:Exception){
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onDestroyView: $e")
            }
            cancelScan()
            isReceiverRegistered = false;
            isScanInProgress = false;
        }
    }

    private fun cancelScan() {
        try {
            val scanCancelIntent = Intent("com.google.android.tv.dtvscan.action.ACTION_CANCEL_INSTALL")
            ReferenceApplication.applicationContext().sendBroadcast(scanCancelIntent)
            isScanInProgress = false
        }catch (E: Error){
        }
    }

    private fun startCableQuickScan(
        quick_countryTag: String,
        quick_net_id: Int,
        quick_freq: Int,
        quick_symb: Int,
        quick_qam: Int,
        quick_stand: Int
    ) {
        isScanInProgress = true
        val myObject = JSONObject()
        try {
            myObject.put(FIELD_TYPE, mTypeC)
            myObject.put(FIELD_COUNTRY, quick_countryTag)
            myObject.put(FIELD_HOME_TS_ONLY, false)
            myObject.put(FIELD_SCAN_CUSTOM, true)
            myObject.put(FIELD_NETWORK_ID, quick_net_id)
            myObject.put(FIELD_SYMBOL_RATE, quick_symb)
            myObject.put(FIELD_FREQUENCY, quick_freq)
            if (quick_qam == 16) {
                myObject.put(FIELD_MODULATION, "qam_16")
            } else if (quick_qam == 32) {
                myObject.put(FIELD_MODULATION, "qam_32")
            } else if (quick_qam == 64) {
                myObject.put(FIELD_MODULATION, "qam_64")
            } else if (quick_qam == 128) {
                myObject.put(FIELD_MODULATION, "qam_128")
            } else {
                myObject.put(FIELD_MODULATION, "qam_256")
            }
            if (quick_stand == 1) {
                myObject.put(FIELD_QAM_ANNEX, "a")
            } else if (quick_stand == 2) {
                myObject.put(FIELD_QAM_ANNEX, "b")
            } else {
                myObject.put(FIELD_QAM_ANNEX, "c")
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startCableQuickScan: Start Cable Quick Scan: $myObject")
        val startQuickCableScanIntent: Intent = Intent("com.google.android.tv.dtvscan.action.ACTION_START_INSTALL")
        startQuickCableScanIntent.putExtra(
            "params",
            myObject.toString()
        )
        requireActivity().sendBroadcast(startQuickCableScanIntent)
    }

    private fun startCableManualScan(
        countryTag: String,
        net_id: Int,
        freq: Int,
        symb: Int,
        qam: Int,
        stand: Int
    ) {
        isScanInProgress = true
        val myObject = JSONObject()
        try {
            myObject.put(FIELD_TYPE, mTypeC)
            myObject.put(FIELD_COUNTRY, countryTag)
            myObject.put(FIELD_HOME_TS_ONLY, true)
            myObject.put(FIELD_NETWORK_ID, net_id)
            myObject.put(FIELD_SYMBOL_RATE, symb)
            myObject.put(FIELD_FREQUENCY, freq)
            if (qam == 16) {
                myObject.put(FIELD_MODULATION, "qam_16")
            } else if (qam == 32) {
                myObject.put(FIELD_MODULATION, "qam_32")
            } else if (qam == 64) {
                myObject.put(FIELD_MODULATION, "qam_64")
            } else if (qam == 128) {
                myObject.put(FIELD_MODULATION, "qam_128")
            } else {
                myObject.put(FIELD_MODULATION, "qam_256")
            }
            if (stand == 1) {
                myObject.put(FIELD_QAM_ANNEX, "a")
            } else if (stand == 2) {
                myObject.put(FIELD_QAM_ANNEX, "b")
            } else {
                myObject.put(FIELD_QAM_ANNEX, "c")
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startCableManualScan: Start Cable Manual Scan: $myObject")
        val startCableScanIntent: Intent = Intent("com.google.android.tv.dtvscan.action.ACTION_START_INSTALL")
        startCableScanIntent.putExtra(
            "params",
            myObject.toString()
        )
        requireActivity().sendBroadcast(startCableScanIntent)
    }

    private fun runSignalMonitoringThread() {
        monitoringThread = Thread {
            while (isScanInProgress) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Scan in progress, fetch signal strength and quality")
                var status: ScanProgressFtiFragment.SignalStatus? = null
                try {
//                    Thread.sleep(200)
                    if(getSignalStatus() == null){
                        updateSignalStatusPrefs(0, 0, 0F)
                    }else{
                        status = getSignalStatus()
                        if (status != null) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Status is not null, update")
                            //(strength: Int, quality: Int, frequency: Int)
                            updateSignalStatusPrefs(status.rssi, status.snr, status.frequency)
                        } else {
                            updateSignalStatusPrefs(0, 0, 0F)
                        }
                    }
                }catch (E: Error){
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "runSignalMonitoringThread: Error $E")
                    updateSignalStatusPrefs(0, 0, 0F)
                }
                try {
                    Thread.sleep(200)
                } catch (e: Exception) {
                }
            }
            monitoringThread = null
        }
        monitoringThread!!.start()
    }

    private fun updateSignalStatusPrefs(strength: Int, quality: Int, frequency: Float) {
        try {
            Log.i(
                "######",
                "updateSignalStatusPrefs strength=$strength quality=$quality frequency=$frequency"
            )
            //prevent inconsistent database data to create glitches
            if (strength > 0 && quality > 0) {
                editFrequencyText!!.text ="${frequency} MHz"
                signalStrengthBar!!.setProgress(strength)
                signalStrengthBarText!!.text = strength.toString() + " %"
                signalQualityBar!!.setProgress(quality)
                signalQualityBarText!!.text = quality.toString() + " %"
            } else {
                editFrequencyText!!.text ="${frequency} MHz"
                signalStrengthBar!!.setProgress(0)
                signalStrengthBarText!!.text = "0 %"
                signalQualityBar!!.setProgress(0)
                signalQualityBarText!!.text = "0 %"
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("Range")
    private fun getSignalStatus(): ScanProgressFtiFragment.SignalStatus? {
        var retVal: ScanProgressFtiFragment.SignalStatus? = null
        val cursor: Cursor? = ReferenceApplication.applicationContext().contentResolver.query(
            CONTENT_URI,
            projection,
            terSelectionClause,
            terSelectionArgs,
            null
        )
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val id: Int = cursor.getInt(cursor.getColumnIndex("id"))
                    val type: String = cursor.getString(cursor.getColumnIndex("type"))
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getSignalStatus type=$type")
                    if (type == "TERRESTRIAL" || type == "TERRESTRIAL_2" || type == "CABLE" || type == "SATELLITE") {
                        val state: String = cursor.getString(cursor.getColumnIndex("state"))
                        val signal = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex("signal")))
                        val stats: String = cursor.getString(cursor.getColumnIndex("stats"))
                        val parameters: String = cursor.getString(cursor.getColumnIndex("parameters"))
                        retVal = ScanProgressFtiFragment.SignalStatus()
                        try {
                            val jsonParameters = JSONObject(parameters)
                            var tFrequency = jsonParameters.getInt("frequency")
                            retVal.locked = signal
                            retVal.ber = 0
                            retVal.rssi = 0
                            retVal.snr = 0
                            if (tFrequency > 1000000) {
                                tFrequency = tFrequency / 1000
                            }
                            if (tFrequency > 100000) {
                                tFrequency = tFrequency / 1000
                            }
                            retVal.frequency = tFrequency.toFloat()
                            if (signal) {
                                val jsonStats = JSONObject(stats)
                                val ber = jsonStats.getInt("ber")
                                val snr = jsonStats.getInt("snr")
                                val rssi = jsonStats.getInt("rssi")
                                retVal.ber = ber
                                retVal.rssi = rssi
                                retVal.snr = snr
                            }
                        } catch (e: JSONException) {
                            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +"####", "FREQUENCY Exception e:$e")
                            e.printStackTrace()
                        }
                        break
                    } else {
                        continue
                    }
                } while (cursor.moveToNext())
            }
            cursor!!.close()
        } else {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Cursor is null ")
        }
        return retVal
    }


    fun isServiceRunning(context: Context, serviceClass: Class<*>): kotlin.Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                Log.i("Service status", "Running")
                return true
            }
        }
        Log.i("Service status", "Not running")
        return false
    }

    private fun startService(context: Context, serviceClass: Class<*>) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startService: ################ START SERVICE FROM MAIN ACTIVITY")
        val intent = Intent(context, serviceClass)
        context.startService(intent)
    }

    fun tintIndeterminateProgress(progress: ProgressBar
                                  , @ColorInt color: Int = ContextCompat.getColor(progress.context, R.color.fti_progress)
                                  , @ColorInt colorBackground: Int = ContextCompat.getColor(progress.context, R.color.fti_progress_background)){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            progress.indeterminateTintList = ColorStateList.valueOf(color)

            progress.setProgressTintList(ColorStateList.valueOf(color))
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(colorBackground))
        }
    }

    fun tintHorizontalProgress(progress: ProgressBar
                               , @ColorInt color: Int = ContextCompat.getColor(progress.context, R.color.fti_progress)
                               , @ColorInt colorBackground: Int = ContextCompat.getColor(progress.context, R.color.fti_progress_background)){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            progress.progressTintList = ColorStateList.valueOf(color)

            progress.setProgressTintList(ColorStateList.valueOf(color))
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(colorBackground))
        }
    }

    private fun updateScanProgressPrefs(
        scanProgress: Int,
        channelNum: Int,
        freq: Int,
        status: String?
    ) {
        if (scanProgress > -1) {
            scanningProgressBar!!.setProgress(scanProgress)
            scanningProgressBarText!!.text = "${scanProgress} %"
        }
        if (channelNum > -1 /*&& status != "Cancelled"*/) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateScanProgressPrefs: channelNum = $channelNum")
            editProgrammesFoundText!!.text = "$channelNum"
        }
        if (status != null) {
            when (status) {
                "in-progress" -> editStatusText!!.text = ConfigStringsManager.getStringById("in_progress")
                "is-update" -> editStatusText!!.text = ConfigStringsManager.getStringById("updating")
                "complete" -> {
                    editStatusText!!.text = ConfigStringsManager.getStringById("processing")

                    try{
                        val intent = Intent("scan_completed_sync_databases")
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateScanProgressPrefs: ########### IS SERVICE RUNNING ${isServiceRunning(
                            ReferenceApplication.applicationContext(),
                            ContentAggregatorService::class.java
                        )}")
                        ReferenceApplication.applicationContext().sendBroadcast(intent)
                        val uri: Uri = Uri.parse("content://com.iwedia.cltv.sdk.content_provider.ReferenceContentProvider/channels")
                        ReferenceApplication.applicationContext().contentResolver.registerContentObserver(uri, true, contentObserver)
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Intent to sync databases after scan has been sent")

                    }catch (E: java.lang.RuntimeException){
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateScanProgressPrefs: Error $E")
                    }
                    isScanInProgress = false
                }
                "cancelled" -> {
                    editStatusText!!.text = ConfigStringsManager.getStringById("cancelled")
                    buttonScan!!.text =  ConfigStringsManager.getStringById("scan_back")
                    isScanInProgress = false
                }
                "failed" -> {
                    editStatusText!!.text = ConfigStringsManager.getStringById("failed")
                    buttonScan!!.text = ConfigStringsManager.getStringById("failed")
                    monitoringThread = null

                    //set failed state
                    (activity as SettingsActivity?)!!.programmesFound = editProgrammesFoundText!!.text.toString()
                    (activity as SettingsActivity?)!!.goToScanDone()
                }
                else -> editStatusText!!.text = ConfigStringsManager.getStringById("unknown")
            }
        }
    }

    var contentObserver: ContentObserver = object : ContentObserver(null) {
        override fun onChange(selfChange: kotlin.Boolean) {
            super.onChange(selfChange)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Databases are synced, continue with operations")
            mChannelsHandler!!.updateChannelList()
            mChannels = mChannelsHandler!!.getListOfChannels()
            val duplicates: HashMap<Int, MutableList<Channel>>? = mChannelsHandler.getMapOfDuplicateChannels(mChannels!!)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onChange: duplicates.size=${duplicates!!.size} duplicates = $duplicates")
            if (duplicates!!.size > 0) {
                hasLcnConflicts = true
                isScanInProgress = false
                editStatusText!!.text = ConfigStringsManager.getStringById("lcn_conflict")
                (activity as SettingsActivity?)!!.hasLcnConflicts = true
                (activity as SettingsActivity?)!!.lcnConflictNumber = duplicates!!.size
            } else {
                (activity as SettingsActivity?)!!.hasLcnConflicts = false
                isScanInProgress = false
                hasLcnConflicts = false
                isScanCompleted = true
            }

            (activity as SettingsActivity?)!!.programmesFound = editProgrammesFoundText!!.text.toString()
            (activity as SettingsActivity?)!!.mChannelsHandler = mChannelsHandler
            (activity as SettingsActivity?)!!.mChannels = mChannels
            (activity as SettingsActivity?)!!.goToScanDone()
        }
    }

    private val mProgressReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            if (intent.action == null) {
                return
            } else {
                if (intent.action.equals("com.google.android.tv.dtvscan.INSTALL_EVENT")) {
                    val tStatus = intent.extras!!.getString("status")
                    val tProgress = intent.extras!!.getInt("progress")
                    val tFrequency = intent.extras!!.getInt("frequency")
                    val tChannelNum = intent.extras!!.getInt("nb_channels")
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "INSTALL_EVENT, status: $tStatus progress: $tProgress channelNum: $tChannelNum freq: $tFrequency")
                    updateScanProgressPrefs(tProgress, tChannelNum, tFrequency, tStatus)
                } else if (intent.action.equals("com.google.android.tv.dtvscan.SIGNAL_EVENT")) {
                    val signalQuality = intent.getIntExtra("signal-quality", 0)
                    val signalStrength = intent.getIntExtra("signal-level", 0)
                    val signalLoss = intent.getBooleanExtra("signal-loss", true)
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "SIGNAL_EVENT, signalQuality: $signalQuality signalStrength: $signalStrength signalLoss: $signalLoss")
                }
                if(monitoringThread!= null) {
                    monitoringThread!!.interrupt()
                    monitoringThread = null
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun clearDatabase() {
        var context = ReferenceApplication.applicationContext()
        val contentResolver: ContentResolver = context.contentResolver
        contentResolver.delete(ContentProvider.CHANNELS_URI, null)
    }

}