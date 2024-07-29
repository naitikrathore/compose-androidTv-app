package com.iwedia.cltv.fti.terrestrial

import android.annotation.SuppressLint
import android.content.*
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.res.ColorStateList
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.TextView.OnEditorActionListener
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.SettingsActivity
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.fti.handlers.ConfigHandler
import com.iwedia.cltv.fti.scan_models.DataParams
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import org.json.JSONObject
import java.io.InputStream
import kotlin.math.log


class TerManualScanParamsFragment : Fragment() {

    companion object {
        fun newInstance() = TerManualScanParamsFragment()
    }

    var viewModel: View? = null
    val TAG = javaClass.simpleName
    private var buttonFreq: EditText? = null
    private var buttonFreqRelativeLayout: RelativeLayout? = null
    private var buttonVhf: EditText? = null
    private var buttonVhfRelativeLayout: RelativeLayout? = null

    var startButton: Button? = null
    var textViewDescriptionChannel: TextView? = null
    var textViewDescriptionFreq: TextView? = null
    var textViewDescription: TextView? = null

    var freqMap: HashMap<Int, Int> = HashMap<Int, Int>()

    var channelNum = 0
    var channelFreq = 0
    var channelBand = 0
    private var mCountryTag:String? = null
    private var dataParams: DataParams? = null
    private val countryTagWithThirteenVfhUfh: List<String> = ArrayList(
        listOf(
            "DNK",
            "FIN"
        )
    )
    private val countryTagOnlyWithUHF: List<String> = ArrayList<String>(
        listOf(
            "FRA",
            "DEU",
            "ESP",
            "NLD",
            "BGR",
            "HRV",
            "CZE",
            "HUN",
            "POL",
            "PRT",
            "SVK",
            "SVN",
            "SWE",
            "GRC",
            "IRL"
        )
    )
    private var FIRST_UHF_CHANNEL = 21
    private var FIRST_UHF_FREQ = 474000
    private var INDEX_OF_FIRST_UHF = 9
    private val sharedPrefFile = "VfhUfhSharedPref"
    private var mSharedPref: SharedPreferences? = null
    private var signalStrengthBar: ProgressBar? = null
    private var signalQualityBar: ProgressBar? = null
    private var signalQualityBarText: TextView? = null
    private var signalStrengthBarText: TextView? = null
    private var textViewWrongInputDescription: TextView? = null
    private var monitoringThread: Thread? = null
    private var signal: Boolean = false
    private val signalUpdateTimerTimeout = 1000L
    var signalUpdateTimer: CountDownTimer? = null

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
    private var terSelectionClause = "type=? OR type=?"
    private var terSelectionArgs = arrayOf(
        "TERRESTRIAL",
        "TERRESTRIAL_2"
    )

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            viewModel =
                inflater.inflate(R.layout.fti_terrestrial_manual_scan_layout, container, false)
        }else{
            viewModel =
                inflater.inflate(R.layout.fti_ter_manual_scan_layout_gtv, container, false)
            textViewWrongInputDescription = viewModel!!.findViewById(R.id.textViewWrongInputDescription)
        }
        val context = requireContext()
        viewModel!!.background = ContextCompat.getDrawable(context!!, R.color.fti_left_black)

        val linear_border1: LinearLayout = viewModel!!.findViewById(R.id.linear_border1)
        val linear_border2: LinearLayout = viewModel!!.findViewById(R.id.linear_border2)
        val linear_border3: LinearLayout = viewModel!!.findViewById(R.id.linear_border3)
        val constraint_border1: ConstraintLayout = viewModel!!.findViewById(R.id.constraint_border1)
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            linear_border1.visibility = View.GONE
            linear_border2.visibility = View.GONE
            linear_border3.visibility = View.GONE
            constraint_border1!!.background = ContextCompat.getDrawable(context!!, R.color.fti_left_background)
        }else{
            viewModel!!.background = ContextCompat.getDrawable(context!!, R.drawable.fti_bg_gtv)
        }

        textViewDescription = viewModel!!.findViewById(R.id.textViewDescription)
        var param = textViewDescription!!.layoutParams as ViewGroup.MarginLayoutParams

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            param.setMargins(50 * context!!.getResources().getDisplayMetrics().density.toInt(), 20 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 50 * context!!.getResources().getDisplayMetrics().density.toInt())
            textViewDescription!!.layoutParams = param
            textViewDescription!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewDescription!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewDescription!!.gravity = Gravity.LEFT
            textViewDescription!!.includeFontPadding = false
            textViewDescription!!.layoutParams
        }else{
            param = textViewDescription!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 20 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            textViewDescription!!.layoutParams = param
            textViewDescription!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewDescription!!.layoutParams.height = 20 * context!!.getResources().getDisplayMetrics().density.toInt()
            textViewDescription!!.gravity = Gravity.TOP
            textViewDescription!!.includeFontPadding = false
            textViewDescription!!.translationY = -1.98f
            textViewDescription!!.textSize = 16f

            textViewWrongInputDescription = viewModel!!.findViewById(R.id.textViewWrongInputDescription)
            textViewWrongInputDescription!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewWrongInputDescription!!.layoutParams.height = 96 * context!!.getResources().getDisplayMetrics().density.toInt()
            textViewWrongInputDescription!!.gravity = Gravity.TOP
            textViewWrongInputDescription!!.includeFontPadding = false
            textViewWrongInputDescription!!.textSize = 16f
        }

        val textViewScanText: TextView = viewModel!!.findViewById(R.id.textViewScanText)
        textViewScanText.text = ConfigStringsManager.getStringById("channels_scan")
        param = textViewScanText!!.layoutParams as ViewGroup.MarginLayoutParams
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
                override fun onFailed(error: Error?) {}

                override fun onSuccess() {}
            })
        } else {
            val ims: InputStream = (activity as SettingsActivity?)!!.assets.open("company_logo.png")
            val bitmap1 = BitmapFactory.decodeStream(ims)
            imageViewLogo.setImageBitmap(bitmap1)
        }

        startButton = viewModel!!.findViewById(R.id.startButton)

        dataParams = (activity as SettingsActivity?)!!.dataParams
        mCountryTag = dataParams!!.getCountryTag()

        signal = (activity as SettingsActivity?)!!.moduleProvider.getGeneralConfigModule().getGeneralSettingsInfo("signal_status_monitoring_enabled")

        if(signal) {
            signalStrengthBar = viewModel!!.findViewById(R.id.signalStrengthBar)
            signalQualityBar = viewModel!!.findViewById(R.id.signalQualityBar)
            signalStrengthBar!!.visibility = View.VISIBLE
            signalQualityBar!!.visibility = View.VISIBLE

            tintHorizontalProgress(signalStrengthBar!!)
            tintIndeterminateProgress(signalStrengthBar!!)
            if((activity as SettingsActivity?)!!.atvGtvSwitch == 1){
                signalStrengthBar!!.layoutParams.height = 4 * context!!.getResources().getDisplayMetrics().density.toInt()
            }else{
                signalStrengthBar!!.layoutParams.height = 10 * context!!.getResources().getDisplayMetrics().density.toInt()
            }

            tintHorizontalProgress(signalQualityBar!!)
            tintIndeterminateProgress(signalQualityBar!!)
            if((activity as SettingsActivity?)!!.atvGtvSwitch == 1){
                signalQualityBar!!.layoutParams.height = 4 * context!!.getResources().getDisplayMetrics().density.toInt()
            }else{
                signalQualityBar!!.layoutParams.height = 10 * context!!.getResources().getDisplayMetrics().density.toInt()
            }

            signalQualityBarText = viewModel!!.findViewById(R.id.signalQualityBarText)
            signalStrengthBarText = viewModel!!.findViewById(R.id.signalStrengthBarText)
            if((activity as SettingsActivity?)!!.atvGtvSwitch == 1) {
                signalQualityBarText!!.textSize = 16f
                signalStrengthBarText!!.textSize = 16f
            }

            signalStrengthBar!!.setProgress(0)
            signalStrengthBarText!!.text = "${0} %"
            signalQualityBar!!.setProgress(0)
            signalQualityBarText!!.text = "${0} %"

            val textViewDescriptionSignalQuality : TextView= viewModel!!.findViewById(R.id.textViewDescriptionSignalQuality)
            val textViewDescriptionSignalStrength : TextView= viewModel!!.findViewById(R.id.textViewDescriptionSignalStrength)
            if((activity as SettingsActivity?)!!.atvGtvSwitch == 1) {
                textViewDescriptionSignalQuality!!.textSize = 16f
                textViewDescriptionSignalStrength!!.textSize = 16f
            }

            textViewDescriptionSignalStrength!!.text = ConfigStringsManager.getStringById("signal_strength")
            textViewDescriptionSignalQuality!!.text = ConfigStringsManager.getStringById("signal_quality")
        }else{
            val textViewDescriptionSignalQuality : TextView= viewModel!!.findViewById(R.id.textViewDescriptionSignalQuality)
            val textViewDescriptionSignalStrength : TextView= viewModel!!.findViewById(R.id.textViewDescriptionSignalStrength)
            signalStrengthBar = viewModel!!.findViewById(R.id.signalStrengthBar)
            signalQualityBar = viewModel!!.findViewById(R.id.signalQualityBar)
            signalQualityBarText = viewModel!!.findViewById(R.id.signalQualityBarText)
            signalStrengthBarText = viewModel!!.findViewById(R.id.signalStrengthBarText)
            textViewDescriptionSignalQuality!!.visibility = View.GONE
            textViewDescriptionSignalStrength!!.visibility = View.GONE
            signalStrengthBar!!.visibility = View.GONE
            signalQualityBar!!.visibility = View.GONE
            signalQualityBarText!!.visibility = View.GONE
            signalStrengthBarText!!.visibility = View.GONE
        }

        mSharedPref = context!!.getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)

        startButton!!.text = ConfigStringsManager.getStringById("start")
        startButton!!.setOnClickListener {
            move()
        }

        textViewDescriptionChannel = viewModel!!.findViewById(R.id.textViewDescriptionChannel)
        textViewDescriptionFreq = viewModel!!.findViewById(R.id.textViewDescriptionFreq)
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            textViewDescriptionFreq!!.text = ConfigStringsManager.getStringById("manual_scan_freq")
        }

        freqMap = HashMap<Int, Int>()

        if(isMapThirteenNeeded(mCountryTag!!)){
            if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                textViewDescriptionChannel!!.text =
                    ConfigStringsManager.getStringById("manual_scan_ch_with_thirteen")
            }
            var startFreq = 177500
            for (i in 5..13) {
                freqMap.put(i, startFreq)
                startFreq += 7000
            }
            startFreq = 474000
            for (i in 21..69) {
                freqMap.put(i, startFreq)
                startFreq += 8000
            }
        }
        else if(isOnlyUHFNeeded(mCountryTag!!)){
            if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                textViewDescriptionChannel!!.text =
                    ConfigStringsManager.getStringById("manual_scan_ch_only_uhf")
            }
            var startFreq = 474000
            for (i in 21..69) {
                freqMap.put(i, startFreq)
                startFreq += 8000
            }
        }
        else{
            if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                textViewDescriptionChannel!!.text =
                    ConfigStringsManager.getStringById("manual_scan_ch")
            }
            var startFreq = 177500
            for (i in 5..12) {
                freqMap.put(i, startFreq)
                startFreq += 7000
            }
            startFreq = 474000
            for (i in 21..69) {
                freqMap.put(i, startFreq)
                startFreq += 8000
            }
        }

        buttonFreq = viewModel!!.findViewById(R.id.buttonFreq)
        buttonFreq!!.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm: InputMethodManager
                imm = context!!.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                return@OnEditorActionListener true
            }
            false
        })
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            buttonFreq!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    editViewHasFocusNumber(buttonFreq!!)
                } else {
                    editViewHasNoFocusNumber(buttonFreq!!)
                }
            }
        }


        if((activity as SettingsActivity?)!!.atvGtvSwitch == 1){
            buttonFreqRelativeLayout = viewModel!!.findViewById(R.id.buttonFreqRelativeLayout)

            buttonFreqRelativeLayout!!.gravity = Gravity.CENTER_VERTICAL
            param = buttonFreqRelativeLayout!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonFreqRelativeLayout!!.layoutParams = param
            buttonFreqRelativeLayout!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_on_gtv)
            param = buttonFreq!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(16 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
            buttonFreq!!.layoutParams = param
            buttonFreq!!.setTextAppearance(R.style.edit_text_text_style_on_gtv)
            buttonFreq!!.gravity = Gravity.CENTER_VERTICAL
            buttonFreq!!.layoutParams.width = 236 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonFreq!!.layoutParams.height = 22 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonFreq!!.setTextColor(ContextCompat.getColor(context!!, R.color.fti_gtv_relative_layout_edit_text_text_color_on))
            buttonFreq!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    textViewDescription!!.text = ConfigStringsManager.getStringById("manual_scan_freq")
                    buttonFreqRelativeLayout!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_on_gtv)
                    buttonFreq!!.setTextAppearance(R.style.edit_text_text_style_on_gtv)
                    buttonFreq!!.background = ContextCompat.getDrawable(context!!, R.color.fti_gtv_relative_layout_edit_text_on)
                }else{
                    buttonFreqRelativeLayout!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_off_gtv)
                    buttonFreq!!.setTextAppearance(R.style.edit_text_text_style_off_gtv)
                    buttonFreq!!.background = null
                }
            }
            buttonFreqRelativeLayout!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_off_gtv)
        }

        buttonVhf = viewModel!!.findViewById(R.id.buttonVhf)
        buttonVhf!!.doAfterTextChanged {
            buttonFreq!!.setText("")
            if(!buttonVhf!!.text.toString().equals("")) {
                val numberPickerChannelNumber = buttonVhf!!.text.toString().toInt()
                val numberPickerChannelValidVal = (numberPickerChannelNumber in 5..13) || (numberPickerChannelNumber in 21..69)
                if (numberPickerChannelValidVal) {
                    val strNum = freqMap!!.get(numberPickerChannelNumber)
                    if(strNum != null){
                        buttonFreq!!.setText(strNum.toString())
                    }
                }
            }
        }
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            buttonVhf!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    editViewHasFocusNumber(buttonVhf!!)

                } else {
                    editViewHasNoFocusNumber(buttonVhf!!)

                }
            }
        }

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 1){
            val buttonVhfRelativeLayout: RelativeLayout = viewModel!!.findViewById(R.id.buttonVhfRelativeLayout)

            buttonVhfRelativeLayout!!.gravity = Gravity.CENTER_VERTICAL
            param = buttonVhfRelativeLayout!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonVhfRelativeLayout!!.layoutParams = param
            buttonVhfRelativeLayout!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_on_gtv)
            param = buttonVhf!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(16 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
            buttonVhf!!.layoutParams = param
            buttonVhf!!.setTextAppearance(R.style.edit_text_text_style_on_gtv)
            buttonVhf!!.gravity = Gravity.CENTER_VERTICAL
            buttonVhf!!.layoutParams.width = 236 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonVhf!!.layoutParams.height = 22 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonVhf!!.setTextColor(ContextCompat.getColor(context!!, R.color.fti_gtv_relative_layout_edit_text_text_color_on))
            buttonVhf!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    if(isMapThirteenNeeded(mCountryTag!!)){
                        textViewDescription!!.text =
                            ConfigStringsManager.getStringById("manual_scan_ch_with_thirteen")
                    }else if(isOnlyUHFNeeded(mCountryTag!!)){
                        textViewDescription!!.text =
                            ConfigStringsManager.getStringById("manual_scan_ch_only_uhf")
                    }else{
                        textViewDescription!!.text =
                            ConfigStringsManager.getStringById("manual_scan_ch")
                    }
                    buttonVhfRelativeLayout!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_on_gtv)
                    buttonVhf!!.setTextAppearance(R.style.edit_text_text_style_on_gtv)
                    buttonVhf!!.background = ContextCompat.getDrawable(context!!, R.color.fti_gtv_relative_layout_edit_text_on)
                }else{
                    buttonVhfRelativeLayout!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_off_gtv)
                    buttonVhf!!.setTextAppearance(R.style.edit_text_text_style_off_gtv)
                    buttonVhf!!.background = null
                }
            }
            buttonVhfRelativeLayout!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_off_gtv)
        }

        if(signal) {
            val intentFilter = IntentFilter()
            intentFilter.addAction("com.google.android.tv.dtvscan.SIGNAL_EVENT")
            ReferenceApplication.applicationContext().registerReceiver(mProgressReceiver, intentFilter, null, null)

            buttonFreq!!.addTextChangedListener {
                checkSignal()
            }
            runSignalMonitoringThread()
            checkSignal()
        }

        startButton!!.setOnFocusChangeListener{ _, hasFocus ->
            if(hasFocus) {
                if((activity as SettingsActivity?)!!.atvGtvSwitch == 1) {
                    textViewDescription!!.text = ""
                    startButton!!.background = ContextCompat.getDrawable(context!!, R.drawable.fti_button_on_gtv)
                    startButton!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                }else{
                    textViewHasFocus(startButton!!)
                }
            }else{
                if((activity as SettingsActivity?)!!.atvGtvSwitch == 1) {
                    startButton!!.background = ContextCompat.getDrawable(context!!, R.drawable.fti_button_off_gtv)
                    startButton!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                }else{
                    textViewHasNoFocus(startButton!!)
                }
            }
        }

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            editViewHasNoFocusNumber(buttonVhf!!)
            editViewHasNoFocusNumber(buttonFreq!!)
            textViewHasFocus(startButton!!)
        }else{
            buttonVhf!!.setTextAppearance(R.style.edit_text_text_style_off_gtv)
            buttonVhf!!.background = null

            buttonFreq!!.setTextAppearance(R.style.edit_text_text_style_off_gtv)
            buttonFreq!!.background = null

            startButton!!.isAllCaps = false
            startButton!!.background = ContextCompat.getDrawable(context!!, R.drawable.fti_button_on_gtv)
            startButton!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
        }

        startButton!!.setFocusable(true)
        startButton!!.requestFocus()

        return viewModel!!
    }

    @SuppressLint("Range")
    fun checkSignal(){
        signalStrengthBar!!.setProgress(0)
        signalStrengthBarText!!.text = "${0} %"
        signalQualityBar!!.setProgress(0)
        signalQualityBarText!!.text = "${0} %"
        startSignalUpdateTimer()
    }

    private fun stopSignalUpdateTimer() {
        if (signalUpdateTimer != null) {
            signalUpdateTimer!!.cancel()
            signalUpdateTimer = null
        }
    }

    private fun startSignalUpdateTimer() {
        //Cancel timer if it's already started
        stopSignalUpdateTimer()

        //Start new count down timer
        signalUpdateTimer = object :
            CountDownTimer(
                signalUpdateTimerTimeout,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                //com.google.android.tv.dtvscan.action.ACTION_LOCK_TUNER
                var localChannelBand: Int = 0
                var localChannelFreq: Int = 0
                val buttonFreqString = buttonFreq!!.text.toString()
                val localChannelNum = buttonVhf!!.text.toString().toInt()
                val mFullScanMode = "managed_manual"
                if(buttonFreqString != "") {
                    try {
                        localChannelFreq = buttonFreqString.toInt()
                    }catch (E: Exception){
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFinish: ${E.printStackTrace()}")
                    }
                    if (localChannelFreq >= 47000 && localChannelFreq <= 862000) {
                        if(localChannelNum <= 13 && localChannelNum >= 5){
                            localChannelBand = 7 * 1000
                        }else if(localChannelNum <= 69 && localChannelNum >= 21){
                            localChannelBand = 8 * 1000
                        }
                    }
                }

                var manualScanJson =
                    "{\"type\":\"dvb-t2\",\"country\":"+(activity as SettingsActivity?)!!.dataParams!!.getCountryTag()!!+"\",\"custom-scan\":false,\"scan-mode\":"+mFullScanMode+
                            ",\"slots\":[{\"frequency-min\":"+localChannelFreq+",\"frequency-max\":"+localChannelFreq+
                            ",\"frequency-step\":"+localChannelFreq+",\"bandwidth\":"+localChannelBand+"}], \"clear-channels\":false}"

                val startScanIntent = Intent("com.google.android.tv.dtvscan.action.ACTION_LOCK_TUNER")
                startScanIntent.setPackage("com.google.android.tv.dtvinput")
                startScanIntent.putExtra("params", manualScanJson)
                ReferenceApplication.applicationContext().sendBroadcast(
                    startScanIntent,
                    "com.google.android.tv.dtvinput.permission.INSTALL_TV_CHANNELS"
                )
            }
        }
        signalUpdateTimer!!.start()
    }

    private val mProgressReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            if (intent.action == null) {
                return
            } else {
                if (intent.action.equals("com.google.android.tv.dtvscan.SIGNAL_EVENT")) {
                    val signalQuality = intent.getIntExtra("signal-quality", 0)
                    val signalStrength = intent.getIntExtra("signal-level", 0)
                    val signalLoss = intent.getBooleanExtra("signal-loss", true)
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "SIGNAL_EVENT, signalQuality: $signalQuality signalStrength: $signalStrength signalLoss: $signalLoss")
                }
            }
        }
    }

    internal class SignalStatusManual {
        var ber = 0
        var snr = 0
        var rssi = 0
        var frequency : Float = 0F
        var locked = false
    }

    @SuppressLint("Range")
    private fun getSignalStatus(): SignalStatusManual? {
        var retVal: SignalStatusManual? = null
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
                    if (type == "TERRESTRIAL" || type == "TERRESTRIAL_2" || type == "CABLE" || type == "SATELLITE") {
                        val state: String = cursor.getString(cursor.getColumnIndex("state"))
                        val signal = java.lang.Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex("signal")))
                        val stats: String = cursor.getString(cursor.getColumnIndex("stats"))
                        val parameters: String = cursor.getString(cursor.getColumnIndex("parameters"))
                        retVal = SignalStatusManual()
                        retVal.locked = signal
                        retVal.ber = 0
                        retVal.rssi = 0
                        retVal.snr = 0
                        if (signal) {
                            val jsonStats = JSONObject(stats)
                            val ber = jsonStats.getInt("ber")
                            val snr = jsonStats.getInt("snr")
                            val rssi = jsonStats.getInt("rssi")
                            retVal.ber = ber
                            retVal.rssi = rssi
                            retVal.snr = snr
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
            var status: SignalStatusManual? = null
            while (true) {
                try {
                    if (getSignalStatus() == null) {
                        updateSignalStatusPrefs(0, 0)
                    } else {
                        status = getSignalStatus()
                        if (status != null) {
                            //(strength: Int, quality: Int)
                            updateSignalStatusPrefs(status.rssi, status.snr)
                        } else {
                            updateSignalStatusPrefs(0, 0)
                        }
                    }
                } catch (E: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "runSignalMonitoringThread: $E")
                    updateSignalStatusPrefs(0, 0)
                }
                try {
                    Thread.sleep(200)
                } catch (e: Exception) {
                }
                monitoringThread = null
            }
        }
        monitoringThread!!.start()
    }

    private fun updateSignalStatusPrefs(strength: Int, quality: Int) {
        try {
            if (strength > 0 && quality > 0) {
                signalStrengthBar!!.setProgress(strength)
                signalStrengthBarText!!.text = strength.toString() + " %"
                signalQualityBar!!.setProgress(quality)
                signalQualityBarText!!.text = quality.toString() + " %"
            } else {
                signalStrengthBar!!.setProgress(0)
                signalStrengthBarText!!.text = "0 %"
                signalQualityBar!!.setProgress(0)
                signalQualityBarText!!.text = "0 %"
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        if(!buttonVhf!!.text.toString().equals("")) {
            val numberPickerChannelNumber = buttonVhf!!.text.toString().toInt()
            val numberPickerChannelValidVal =
                (numberPickerChannelNumber >= 5 && numberPickerChannelNumber <= 13)
                        || (numberPickerChannelNumber >= 21 && numberPickerChannelNumber <= 69)
            if (numberPickerChannelValidVal) {
                mSharedPref!!.edit().putString(sharedPrefFile, numberPickerChannelNumber.toString()).apply()
            }
        }
    }

    override fun onDestroy() {
        if(signal) {
            monitoringThread = null
            signalStrengthBar!!.setProgress(0)
            signalQualityBar!!.setProgress(0)
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        var strNum: Int = 0
        var mapNum: Int = 0
        try {
            strNum = Integer.parseInt(getVfhUfh())
        }catch (E: Exception){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResume: ${E.printStackTrace()}")
        }
        try {
            mapNum = freqMap!!.get(strNum)!!
        }catch (E: Exception){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResume: ${E.printStackTrace()}")
        }
        buttonFreq!!.setText(mapNum.toString())
        buttonVhf!!.setText(strNum.toString())
    }

    private fun isMapThirteenNeeded(countryTag: String): Boolean {
        if (countryTagWithThirteenVfhUfh.contains(countryTag)) {
            return true
        }
        return false
    }

    private fun isOnlyUHFNeeded (countryTag: String): Boolean {
        if(countryTagOnlyWithUHF.contains(countryTag)){
            return true
        }
        return false
    }


    fun move(){
        val buttonFreqString = buttonFreq!!.text.toString()
        var channelNumBoolean: Boolean = false
        if(buttonFreqString != "" && buttonVhf!!.text.toString() != "") {
            channelNum = buttonVhf!!.text.toString().toInt()
            try {
                channelFreq = buttonFreqString.toInt()
            }catch (E: Exception){
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "move: ${E.printStackTrace()}")
            }

            if(isMapThirteenNeeded(mCountryTag!!)){
                if(channelNum in 5..13){
                    channelNumBoolean = true
                    (activity as SettingsActivity?)!!.channelBand = 7
                }else if (channelNum in 21..69){
                    (activity as SettingsActivity?)!!.channelBand = 8
                    channelNumBoolean = true
                }
            }else if(isOnlyUHFNeeded(mCountryTag!!)){
                if(channelNum in 21..69){
                    channelNumBoolean = true
                    (activity as SettingsActivity?)!!.channelBand = 8
                }
            }else{
                if(channelNum in 5..12){
                    channelNumBoolean = true
                    (activity as SettingsActivity?)!!.channelBand = 7
                }else if (channelNum in 21..69){
                    (activity as SettingsActivity?)!!.channelBand = 8
                    channelNumBoolean = true
                }
            }

            if(channelNumBoolean) {
                if (channelFreq in 47000..862000) {
                    textViewDescription!!.text = ""
                    (activity as SettingsActivity?)!!.autoManual = 1
                    (activity as SettingsActivity?)!!.channelNum = channelNum
                    (activity as SettingsActivity?)!!.channelFreq = channelFreq
                    if (channelNum <= 12 && channelNum >= 5) {
                        (activity as SettingsActivity?)!!.channelBand = 7
                    } else if (channelNum <= 69 && channelNum >= 21) {
                        (activity as SettingsActivity?)!!.channelBand = 8
                    }
                    (activity as SettingsActivity?)!!.goToScanProgressScan()
                } else {
                    if ((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                        textViewDescription!!.text =
                            ConfigStringsManager.getStringById("wrong_frequency")
                    } else {
                        textViewWrongInputDescription!!.text =
                            ConfigStringsManager.getStringById("wrong_frequency")
                    }
                }
            }else{
                textViewWrongInputDescription!!.text =
                    ConfigStringsManager.getStringById("wrong_channel")
            }
        }else if(buttonVhf!!.text.toString() == ""){
            textViewWrongInputDescription!!.text =
                ConfigStringsManager.getStringById("wrong_channel")
        }else if(buttonFreqString == ""){
            textViewWrongInputDescription!!.text =
                ConfigStringsManager.getStringById("wrong_frequency")
        }
    }

    fun editViewHasFocusNumber(textButton: EditText){
        textButton!!.setTextColor(ContextCompat.getColor(requireContext(),R.color.button_selected_text_fti))
        textButton!!.setBackgroundResource(R.drawable.my_action_button_selected)
    }

    fun editViewHasNoFocusNumber(textButton: EditText){
        textButton!!.setTextColor(ContextCompat.getColor(requireContext(),R.color.button_not_selected_text_fti))
        textButton!!.setBackgroundResource(R.drawable.my_action_button)
    }

    fun textViewHasFocus(textButton: Button){
        textButton!!.setTextColor(ContextCompat.getColor(requireContext(),R.color.button_selected_text_fti))
        textButton!!.setBackgroundResource(R.drawable.my_small_action_button_selected)
    }

    fun textViewHasNoFocus(textButton: Button){
        textButton!!.setTextColor(ContextCompat.getColor(requireContext(),R.color.button_not_selected_text_fti))
        textButton!!.setBackgroundResource(R.drawable.my_small_action_button)
    }

    fun textViewHasFocusNumber(textButton: TextView){
        textButton!!.setTextColor(ContextCompat.getColor(requireContext(),R.color.button_selected_text_fti))
        textButton!!.setBackgroundResource(R.drawable.my_action_button_selected)
        textButton!!.width = 132
        textButton!!.height = 44

    }

    fun textViewHasNoFocusNumber(textButton: TextView){
        textButton!!.setTextColor(ContextCompat.getColor(requireContext(),R.color.button_not_selected_text_fti))
        textButton!!.setBackgroundResource(R.drawable.my_action_button)
        textButton!!.width = 120
        textButton!!.height = 40
    }

    private fun getVfhUfh(): String? {
        return mSharedPref!!.getString(sharedPrefFile, null)
    }

    fun tintIndeterminateProgress(progress: ProgressBar){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            var color: Int? = null
            var colorBackground: Int? = null
            if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                color = ContextCompat.getColor(progress.context, R.color.fti_progress)
                colorBackground = ContextCompat.getColor(progress.context, R.color.fti_progress_background)
            }else{
                color = ContextCompat.getColor(progress.context, R.color.fti_progress_front_gtv)
                colorBackground = ContextCompat.getColor(progress.context, R.color.fti_progress_bg_gtv)
            }
            progress.indeterminateTintList = ColorStateList.valueOf(color)

            progress.setProgressTintList(ColorStateList.valueOf(color))
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(colorBackground))
        }
    }

    fun tintHorizontalProgress(progress: ProgressBar){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            var color: Int? = null
            var colorBackground: Int? = null
            if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                color = ContextCompat.getColor(progress.context, R.color.fti_progress)
                colorBackground = ContextCompat.getColor(progress.context, R.color.fti_progress_background)
            }else{
                color = ContextCompat.getColor(progress.context, R.color.fti_progress_front_gtv)
                colorBackground = ContextCompat.getColor(progress.context, R.color.fti_progress_bg_gtv)
            }
            progress.progressTintList = ColorStateList.valueOf(color)

            progress.setProgressTintList(ColorStateList.valueOf(color))
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(colorBackground))
        }
    }


}