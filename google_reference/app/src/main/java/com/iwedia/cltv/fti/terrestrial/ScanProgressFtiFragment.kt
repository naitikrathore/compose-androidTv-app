package com.iwedia.cltv.fti.terrestrial

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
import android.graphics.Color
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
import com.iwedia.cltv.fti.scan_models.DataParams
import com.iwedia.cltv.fti.scan_models.TerrestrialRangeModel
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.receiver.GlobalAppReceiver
import com.iwedia.cltv.utils.Utils
import listeners.AsyncReceiver
import org.json.JSONArray
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


class ScanProgressFtiFragment: Fragment()  {

    val TAG = javaClass.simpleName
    companion object {
        fun newInstance() = ScanProgressFtiFragment()
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
//    private val mScanStatus: Preference? = null

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

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = ReferenceApplication.applicationContext()
        viewModel = inflater.inflate(R.layout.fti_scan_progresss_layout, container, false)
        viewModel!!.background = ContextCompat.getDrawable(context!!, R.color.fti_left_black)

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
            viewModel!!.background = ContextCompat.getDrawable(context!!, R.drawable.fti_bg_gtv)
        }

        val textViewScanText : TextView= viewModel!!.findViewById(R.id.textViewScanText)
        textViewScanText!!.text = ConfigStringsManager.getStringById("scan_terrestrial")
        var param = textViewScanText!!.layoutParams as ViewGroup.MarginLayoutParams
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            param.setMargins(50 * context!!.getResources().getDisplayMetrics().density.toInt(), 150 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 50 * context!!.getResources().getDisplayMetrics().density.toInt())
            textViewScanText!!.layoutParams = param
            textViewScanText!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewScanText!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewScanText!!.gravity = Gravity.LEFT
            textViewScanText!!.textSize = 36f
            textViewScanText!!.includeFontPadding = false
        }else{
            param.setMargins(0, 124 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
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
            param.setMargins(50 * context!!.getResources().getDisplayMetrics().density.toInt(), 20 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 50 * context!!.getResources().getDisplayMetrics().density.toInt())
            textViewDescription!!.layoutParams = param
            textViewDescription!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewDescription!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewDescription!!.gravity = Gravity.LEFT
            textViewDescription!!.includeFontPadding = false
            textViewDescription!!.layoutParams
        }else{
            param = textViewDescription.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 20 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
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
        if (imagePath.contains("no_image")) {
            imageViewLogo.setBackgroundColor(Color.TRANSPARENT)
        }else if (imagePath.isNotEmpty()) {
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

        val textViewDescriptionChannel : TextView= viewModel!!.findViewById(R.id.textViewDescriptionChannel)
        textViewDescriptionChannel!!.text = ConfigStringsManager.getStringById("scanning_progress")

        val textViewDescriptionScan : TextView= viewModel!!.findViewById(R.id.textViewDescriptionScan)
        textViewDescriptionScan!!.text = ConfigStringsManager.getStringById("scanning_status")

        val textViewDescriptionFreq : TextView= viewModel!!.findViewById(R.id.textViewDescriptionFreq)
        textViewDescriptionFreq!!.text = ConfigStringsManager.getStringById("frequency_cable")

        val textViewDescriptionProgFound : TextView= viewModel!!.findViewById(R.id.textViewDescriptionProgFound)
        textViewDescriptionProgFound!!.text = ConfigStringsManager.getStringById("programmes_found")

        val textViewDescriptionSignalStatus : TextView= viewModel!!.findViewById(R.id.textViewDescriptionSignalStatus)
        textViewDescriptionSignalStatus!!.text = ConfigStringsManager.getStringById("signal_status")

        val textViewDescriptionSignalStrength : TextView= viewModel!!.findViewById(R.id.textViewDescriptionSignalStrength)
        textViewDescriptionSignalStrength!!.text = ConfigStringsManager.getStringById("signal_strength")

        val textViewDescriptionSignalQuality : TextView= viewModel!!.findViewById(R.id.textViewDescriptionSignalQuality)
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
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreateView: ###########3 IS SERVICE RUNNING ${isServiceRunning(
            ReferenceApplication.applicationContext(), ContentAggregatorService::class.java)}")
        //BroadcastReceiver sometimes doesn't start on reboot in that case start it here
        if (!isServiceRunning(ReferenceApplication.applicationContext(), ContentAggregatorService::class.java)) {
            startService(ReferenceApplication.applicationContext(), ContentAggregatorService::class.java)
        }

        buttonScan!!.setTextColor(ContextCompat.getColor(ReferenceApplication.applicationContext(),R.color.button_selected_text_fti))
        buttonScan!!.setBackgroundResource(R.drawable.my_small_action_button_selected)

        buttonScan!!.setFocusable(true)
        buttonScan!!.requestFocus()
        buttonScan!!.setOnClickListener {
            if(buttonScan!!.text == ConfigStringsManager.getStringById("stop") || buttonScan!!.text == ConfigStringsManager.getStringById("scan_back")){
                ReferenceApplication.applicationContext().contentResolver.unregisterContentObserver(contentObserver)
                clearDatabase()
                monitoringThread = null
                if((activity as SettingsActivity?)!!.autoManual == 0){
                    (activity as SettingsActivity?)!!.goToTuneOptionFragment()
                }else {
                    (activity as SettingsActivity?)!!.goToTerManualScan()
                }
            }else if(buttonScan!!.text == ConfigStringsManager.getStringById("complete")){
                (activity as SettingsActivity?)!!.goToScanDone()
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

        buttonScan!!.text = ConfigStringsManager.getStringById("stop")
        if ((activity as SettingsActivity?)!!.autoManual == 1) {
            val freq = (activity as SettingsActivity?)!!.channelFreq
            val band = (activity as SettingsActivity?)!!.channelBand * 1000
            editStatusText!!.text = ConfigStringsManager.getStringById("status")
            startManualScan(freq, band)
        } else if ((activity as SettingsActivity?)!!.autoManual == 0) {
            startAutoScan((activity as SettingsActivity?)!!.dataParams)
            editStatusText!!.text = ConfigStringsManager.getStringById("status")
        }

        onArriveAtMainPanel()
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



    @RequiresApi(Build.VERSION_CODES.R)
    fun onArriveAtMainPanel(){
        if(isScanInProgress){
            return
        }
        buttonScan!!.text = ConfigStringsManager.getStringById("stop")

        if ((activity as SettingsActivity?)!!.autoManual == 1) {
            var channelFreq = (activity as SettingsActivity?)!!.channelFreq
            var channelBand = (activity as SettingsActivity?)!!.channelBand * 1000
            editStatusText!!.text = ConfigStringsManager.getStringById("in_progress")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                isScanInProgress = true
                startManualScan(channelFreq, channelBand)
            }
        } else if ((activity as SettingsActivity?)!!.autoManual == 0) {
            var dataParams: DataParams? = (activity as SettingsActivity?)!!.dataParams
            isScanInProgress = true
            startAutoScan(dataParams)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("Range")
    private fun startManualScan(freq: Int, bandwidth: Int) {
        val mStartFreq = freq
        val mEndFreq = freq
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startManualScan freq: $freq band: $bandwidth")
        var manualScanJson =
            "{\"type\":\"dvb-t2\",\"country\":"+(activity as SettingsActivity?)!!.dataParams!!.getCountryTag()!!+"\",\"custom-scan\":false,\"scan-mode\":"+mFullScanMode+
                    ",\"slots\":[{\"frequency-min\":"+freq+",\"frequency-max\":"+freq+
                    ",\"frequency-step\":"+freq+",\"bandwidth\":"+bandwidth+"}], \"clear-channels\":false}"
        if((activity as SettingsActivity?)!!.moduleProvider.getGeneralConfigModule().getGeneralSettingsInfo("scan_type_switch")) {
            val contentResolver: ContentResolver = ReferenceApplication.applicationContext().contentResolver
            var cursor = contentResolver.query(
                Uri.parse("content://${"com.iwedia.cltv.platform.model.content_provider.ReferenceContentProvider"}/${"oem_customization"}"),
                null,
                null,
                null,
                null
            )
            if (cursor!!.count > 0) {
                cursor.moveToFirst()
                if (cursor.getString(cursor.getColumnIndex("scan_type")) != null) {
                    var scan_type = cursor.getString(cursor.getColumnIndex("scan_type"))
                    if(scan_type.equals("free")){
                        manualScanJson =
                            "{\"type\":\"dvb-t2\",\"country\":"+(activity as SettingsActivity?)!!.dataParams!!.getCountryTag()!!+"\",\"custom-scan\":false,\"scan-mode\":"+mFullScanMode+
                                    ",\"slots\":[{\"frequency-min\":"+freq+",\"frequency-max\":"+freq+
                                    ",\"frequency-step\":"+freq+",\"bandwidth\":"+bandwidth+"}], \"clear-channels\":false,\"scan-scrambled\":false}"
                    }else if(scan_type.equals("all")){
                        manualScanJson =
                            "{\"type\":\"dvb-t2\",\"country\":"+(activity as SettingsActivity?)!!.dataParams!!.getCountryTag()!!+"\",\"custom-scan\":false,\"scan-mode\":"+mFullScanMode+
                                    ",\"slots\":[{\"frequency-min\":"+freq+",\"frequency-max\":"+freq+
                                    ",\"frequency-step\":"+freq+",\"bandwidth\":"+bandwidth+"}], \"clear-channels\":false,\"scan-scrambled\":true}"
                    }
                }
            }
        }
        val startScanIntent = Intent("com.google.android.tv.dtvscan.action.ACTION_START_INSTALL")
        startScanIntent.setPackage("com.google.android.tv.dtvinput")
        startScanIntent.putExtra("params", manualScanJson)
        ReferenceApplication.applicationContext().sendBroadcast(
            startScanIntent,
            "com.google.android.tv.dtvinput.permission.INSTALL_TV_CHANNELS"
        )
        isScanInProgress = true
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("Range")
    private fun createJSONForScanIntent(
        list: ArrayList<TerrestrialRangeModel>,
        countryTag: String
    ): JSONObject? {
        val result = JSONObject()
        val slotsArray = JSONArray()
        return try {
            for (trm in list) {
                val slotsObject = JSONObject()
                slotsObject.put("frequency-min", trm.getRangeMinFrequency())
                slotsObject.put("frequency-max", trm.getRangeMaxFrequency())
                slotsObject.put("frequency-step", trm.getRangeStep())
                slotsObject.put("bandwidth", trm.getRangeBandwidth())
                slotsArray.put(slotsObject)
            }
            result.put("type", "dvb-t2")
            result.put("country", countryTag)
            result.put("slots", slotsArray)
            result.put("scan-mode",mFullScanMode)
            if((activity as SettingsActivity?)!!.moduleProvider.getGeneralConfigModule().getGeneralSettingsInfo("scan_type_switch")) {
                val contentResolver: ContentResolver = ReferenceApplication.applicationContext().contentResolver
                var cursor = contentResolver.query(
                    Uri.parse("content://${"com.iwedia.cltv.platform.model.content_provider.ReferenceContentProvider"}/${"oem_customization"}"),
                    null,
                    null,
                    null,
                    null
                )
                if (cursor!!.count > 0) {
                    cursor.moveToFirst()
                    if (cursor.getString(cursor.getColumnIndex("scan_type")) != null) {
                        var scan_type = cursor.getString(cursor.getColumnIndex("scan_type"))
                        if(scan_type.equals("free")){
                            result.put("scan-scrambled", false)
                        }else if(scan_type.equals("all")){
                            result.put("scan-scrambled", true)
                        }
                    }
                }
            }
            result
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun startAutoScan(dataParams: DataParams?) {
//        ScanHelper.registerReceiver()

        val rangeList: ArrayList<TerrestrialRangeModel> =
            ArrayList<TerrestrialRangeModel>(dataParams!!.getRangeList())
        val terScanParams = createJSONForScanIntent(rangeList, dataParams!!.getCountryTag()!!)

        if(terScanParams != null){
            // Setup scan intent
            isScanInProgress = true
            val startScanIntent = Intent("com.google.android.tv.dtvscan.action.ACTION_START_INSTALL")
            startScanIntent.setPackage("com.google.android.tv.dtvinput")
            startScanIntent.putExtra("params", terScanParams.toString())
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "startAutoScan: #### terScanParams=$terScanParams")

            // Broadcast scan intent
            ReferenceApplication.applicationContext().sendBroadcast(
                startScanIntent,
                "com.google.android.tv.dtvinput.permission.INSTALL_TV_CHANNELS"
            )

        }else{
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "FAILED to start scan: Invalid settings")
        }

    }

    private fun updateSignalStatusPrefs(strength: Int, quality: Int, frequency: Float) {
        try {
            Log.i(
                TAG,
                "updateSignalStatusPrefs strength=$strength quality=$quality frequency=$frequency"
            )
            //prevent inconsistent database data to create glitches
            var freq = java.lang.String.valueOf(frequency)
            val indexOfZeroAfterDecimal = 4
            if (freq.indexOf("0", 3) == indexOfZeroAfterDecimal) {
                freq = freq.substring(0, 3)
            }
            if (strength > 0 && quality > 0) {
                editFrequencyText!!.text ="${freq} MHz"
                signalStrengthBar!!.setProgress(strength)
                signalStrengthBarText!!.text = strength.toString() + " %"
                signalQualityBar!!.setProgress(quality)
                signalQualityBarText!!.text = quality.toString() + " %"
            } else {
                editFrequencyText!!.text ="${freq} MHz"
                signalStrengthBar!!.setProgress(0)
                signalStrengthBarText!!.text = "0 %"
                signalQualityBar!!.setProgress(0)
                signalQualityBarText!!.text = "0 %"
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
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
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateScanProgressPrefs: ###########3 IS SERVICE RUNNING ${isServiceRunning(
                            ReferenceApplication.applicationContext(), ContentAggregatorService::class.java)}")
                        ReferenceApplication.applicationContext().sendBroadcast(intent)
                        val uri: Uri = Uri.parse("content://com.iwedia.cltv.platform.model.content_provider.ReferenceContentProvider/channels")
                        ReferenceApplication.applicationContext().contentResolver.registerContentObserver(uri, true, contentObserver)
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Intent to sync databases after scan has been sent")

                    }catch (E: java.lang.RuntimeException){
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateScanProgressPrefs: Error $E")
                    }
                    isScanInProgress = false
                }
                "cancelled" -> {
                    editStatusText!!.text = ConfigStringsManager.getStringById("cancelled")
//                    mButton.setSummary(R.string.back_button_summary)
                    buttonScan!!.text =  ConfigStringsManager.getStringById("scan_back")
//                    mScanProgress.setTitle(R.string.scanning_progress_before_scan)
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

    private fun startService(context: Context, serviceClass: Class<*>) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startService: ################ START SERVICE FROM MAIN ACTIVITY ")
        val intent = Intent(context, serviceClass)
        context.startService(intent)
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

    internal class SignalStatus {
        var ber = 0
        var snr = 0
        var rssi = 0
        var frequency : Float = 0F
        var locked = false
    }

    @SuppressLint("Range")
    private fun getSignalStatus(): SignalStatus? {
        var retVal: SignalStatus? = null
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
                    Log.d(Constants.LogTag.CLTV_TAG + "#####", "getSignalStatus type=$type")
                    if (type == "TERRESTRIAL" || type == "TERRESTRIAL_2" || type == "CABLE" || type == "SATELLITE") {
                        val state: String = cursor.getString(cursor.getColumnIndex("state"))
                        val signal = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex("signal")))
                        val stats: String = cursor.getString(cursor.getColumnIndex("stats"))
                        val parameters: String = cursor.getString(cursor.getColumnIndex("parameters"))
                        retVal = SignalStatus()
                        try {
                            val jsonParameters = JSONObject(parameters)
                            var tFrequency : Float = jsonParameters.getInt("frequency").toFloat()
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
                            retVal.frequency = tFrequency
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

    private fun runSignalMonitoringThread() {
        monitoringThread = Thread {
            while (isScanInProgress) {
                var status: SignalStatus? = null
                try {
//                    Thread.sleep(200)
                    if(getSignalStatus() == null){
                        updateSignalStatusPrefs(0, 0, 0F)
                    }else{
                        status = getSignalStatus()
                        if (status != null) {
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

    private fun cancelScan() {
        try {
            val scanCancelIntent = Intent("com.google.android.tv.dtvscan.action.ACTION_CANCEL_INSTALL")
            ReferenceApplication.applicationContext().sendBroadcast(scanCancelIntent)
            isScanInProgress = false
        }catch (E: Error){
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
                    val tChannelNum =
                        intent.extras!!.getInt("nb_channels")
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

    var contentObserver: ContentObserver = object : ContentObserver(null) {
        @RequiresApi(Build.VERSION_CODES.R)
        override fun onChange(selfChange: kotlin.Boolean) {
            super.onChange(selfChange)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Databases are synced, continue with operations")
            do{
                Thread.sleep(100)
            }while(GlobalAppReceiver.numInserted == null)
            mChannelsHandler!!.updateChannelList()
            mChannels = mChannelsHandler!!.getListOfChannels()
            val duplicates: HashMap<Int, MutableList<Channel>>? = mChannelsHandler.getMapOfDuplicateChannels(mChannels!!)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onChange: duplicates.size ${duplicates!!.size} duplicates $duplicates")
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

    @RequiresApi(Build.VERSION_CODES.R)
    private fun clearDatabase() {
        var context = ReferenceApplication.applicationContext()
        val contentResolver: ContentResolver = context.contentResolver
        contentResolver.delete(Uri.parse("content://${"com.iwedia.cltv.platform.model.content_provider.ReferenceContentProvider"}/${"channels"}"), null)
    }

}