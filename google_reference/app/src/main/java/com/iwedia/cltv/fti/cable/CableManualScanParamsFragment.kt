package com.iwedia.cltv.fti.cable

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.iwedia.cltv.R
import com.iwedia.cltv.SettingsActivity
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.fti.handlers.ConfigHandler
import com.iwedia.cltv.fti.scan_models.DataParams
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import java.io.InputStream

class CableManualScanParamsFragment: Fragment() {

    val TAG = javaClass.simpleName
    companion object {
        fun newInstance() = CableManualScanParamsFragment()
    }

    var viewModel: View? = null
    var startButton: Button? = null
    var textViewDescription: TextView? = null
    var textViewDescriptionNetworkId: TextView? = null
    var textViewDescriptionFreq: TextView? = null
    var textViewDescriptionSymbolRate: TextView? = null
    var textViewDescriptionChannelQam: TextView? = null
    var textViewDescriptionChannelStandard: TextView? = null

    val numberPickerQamStrings: MutableList<String> = arrayListOf("16","32","64","128","256")
    val numberPickerStandardStrings: MutableList<String> = arrayListOf("1-EUR/CN","2-NAM","3-JP")
    var numberPickerChannelQam: TextView? = null
    var numberPickerChannelStandard: TextView? = null
    var numberPickerChannelQamPos: Int = 0
    var numberPickerChannelStandardPos: Int = 0

    var buttonNetworkId: EditText? = null
    var buttonFreq: EditText? = null
    var buttonSymbolRate: EditText? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = inflater.inflate(R.layout.fti_cable_manual_scan_layout, container, false)
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

        textViewDescription = viewModel!!.findViewById(R.id.textViewDescription)
        textViewDescription!!.text = ""
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
            textViewDescription!!.layoutParams.height = 96 * context!!.getResources().getDisplayMetrics().density.toInt()
            textViewDescription!!.gravity = Gravity.TOP
            textViewDescription!!.includeFontPadding = false
            textViewDescription!!.translationY = -1.98f
            textViewDescription!!.textSize = 16f
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
        if (imagePath.isNotEmpty()) {
            Utils.loadImage(imagePath, imageViewLogo, object : AsyncReceiver {
                override fun onFailed(error: Error?) {}
                override fun onSuccess() {}
            })
        } else {
            val ims: InputStream = (activity as SettingsActivity?)!!.assets.open("company_logo.png")
            val bitmap1 = BitmapFactory.decodeStream(ims)
            imageViewLogo.setImageBitmap(bitmap1)
        }

        textViewDescriptionNetworkId = viewModel!!.findViewById(R.id.textViewDescriptionNetworkId)
        textViewDescriptionNetworkId!!.text = ConfigStringsManager.getStringById("nit_id")

        textViewDescriptionFreq = viewModel!!.findViewById(R.id.textViewDescriptionFreq)
        textViewDescriptionFreq!!.text = ConfigStringsManager.getStringById("frequency_cable")

        textViewDescriptionSymbolRate = viewModel!!.findViewById(R.id.textViewDescriptionSymbolRate)
        textViewDescriptionSymbolRate!!.text = ConfigStringsManager.getStringById("symbolrate_cable")

        textViewDescriptionChannelQam = viewModel!!.findViewById(R.id.textViewDescriptionChannelQam)
        textViewDescriptionChannelQam!!.text = ConfigStringsManager.getStringById("qam")

        textViewDescriptionChannelStandard = viewModel!!.findViewById(R.id.textViewDescriptionChannelStandard)
        textViewDescriptionChannelStandard!!.text = ConfigStringsManager.getStringById("standard")

        startButton = viewModel!!.findViewById(R.id.startButton)
        startButton!!.text = ConfigStringsManager.getStringById("start")

        startButton!!.setOnFocusChangeListener{_, hasFocus ->
            if(hasFocus) {
                textViewHasFocus(startButton!!)
            }else{
                textViewHasNoFocus(startButton!!)
            }
        }
        textViewHasNoFocus(startButton!!)

        numberPickerChannelQam = viewModel!!.findViewById(R.id.numberPickerChannelQam)
        numberPickerChannelStandard = viewModel!!.findViewById(R.id.numberPickerChannelStandard)

        numberPickerChannelQamPos = 0
        numberPickerChannelStandardPos = 0
        numberPickerChannelQam!!.text = numberPickerQamStrings[numberPickerChannelQamPos]
        numberPickerChannelStandard!!.text = numberPickerStandardStrings[numberPickerChannelStandardPos]

        numberPickerChannelQam!!.setOnFocusChangeListener{_, hasFocus ->
            if(hasFocus) {
                textViewHasFocusNumber(numberPickerChannelQam!!)
            }else{
                textViewHasNoFocusNumber(numberPickerChannelQam!!)
            }
        }

        numberPickerChannelStandard!!.setOnFocusChangeListener{_, hasFocus ->
            if(hasFocus) {
                textViewHasFocusNumber(numberPickerChannelStandard!!)
            }else{
                textViewHasNoFocusNumber(numberPickerChannelStandard!!)
            }
        }

        buttonNetworkId = viewModel!!.findViewById(R.id.buttonNetworkId)
        buttonFreq = viewModel!!.findViewById(R.id.buttonFreq)
        buttonSymbolRate = viewModel!!.findViewById(R.id.buttonSymbolRate)
        editViewHasNoFocusNumber(buttonNetworkId!!)
        editViewHasNoFocusNumber(buttonFreq!!)
        editViewHasNoFocusNumber(buttonSymbolRate!!)

        buttonNetworkId!!.setOnFocusChangeListener{_, hasFocus ->
            if(hasFocus) {
                editViewHasFocusNumber(buttonNetworkId!!)
            }else{
                editViewHasNoFocusNumber(buttonNetworkId!!)
            }
        }

        buttonFreq!!.setOnFocusChangeListener{_, hasFocus ->
            if(hasFocus) {
                editViewHasFocusNumber(buttonFreq!!)
            }else{
                editViewHasNoFocusNumber(buttonFreq!!)
            }
        }

        buttonSymbolRate!!.setOnFocusChangeListener{_, hasFocus ->
            if(hasFocus) {
                editViewHasFocusNumber(buttonSymbolRate!!)
            }else{
                editViewHasNoFocusNumber(buttonSymbolRate!!)
            }
        }

        startButton!!.setOnClickListener {
            move()
        }

        setup()
        return viewModel!!
    }

    override fun onResume() {
        setup()
        super.onResume()
    }

    override fun onPause() {
        setup()
        super.onPause()
    }

    fun move(){
        if(!buttonNetworkId!!.text.toString().equals("") && !buttonFreq!!.text.toString().equals("") && !buttonSymbolRate!!.text.toString().equals("")){
            (activity as SettingsActivity?)!!.buttonNetworkId = buttonNetworkId!!.text.toString().toInt()
            (activity as SettingsActivity?)!!.buttonFreq = buttonFreq!!.text.toString().toInt()
            (activity as SettingsActivity?)!!.buttonSymbolRate = buttonSymbolRate!!.text.toString().toInt()
            (activity as SettingsActivity?)!!.numberPickerChannelQam = numberPickerChannelQam!!.text.toString().toInt()
            (activity as SettingsActivity?)!!.numberPickerChannelStandard = numberPickerChannelStandard!!.text.toString().split("-")[0].toInt()
            (activity as SettingsActivity?)!!.goToCableScanProgressFragment()
        }
    }

    fun setup(){
        if((activity as SettingsActivity?)!!.manualQuick == 1){
            var dataParams: DataParams? = (activity as SettingsActivity?)!!.dataParams
            var cableProviderName: String = (activity as SettingsActivity?)!!.cableProviderName
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setup: cableProviderName $cableProviderName")

            if((activity as SettingsActivity?)!!.cableProviderName.equals("none")){
                buttonNetworkId!!.setText("999")
                buttonFreq!!.setText("306000")
                buttonSymbolRate!!.setText("64")
                numberPickerChannelQamPos = 2
                numberPickerChannelStandardPos = 0
                numberPickerChannelQam!!.text = numberPickerQamStrings[numberPickerChannelQamPos]
                numberPickerChannelStandard!!.text = numberPickerStandardStrings[numberPickerChannelStandardPos]
            }else {
                var operatorList = dataParams!!.getOperatorList()
                operatorList!!.forEach { it ->
                    if (it.operatorName.equals(cableProviderName)) {
                        buttonNetworkId!!.setText(it.operatorNetworkId.toString())
                        buttonFreq!!.setText(it.operatorFrequency.toString())
                        buttonSymbolRate!!.setText(it.operatorSymbolRate.toString())
                        var i = 0
                        numberPickerQamStrings.forEach { it2 ->
                            if (it2.equals(it.operatorModulation.toString())) {
                                numberPickerChannelQamPos = i
                                numberPickerChannelQam!!.setText(it.operatorModulation.toString())
                            }
                            i++
                        }
                    }
                }
            }
        }else{
            buttonNetworkId!!.setText("0")
            buttonFreq!!.setText("0")
            buttonSymbolRate!!.setText("0")
            numberPickerChannelQamPos = 0
            numberPickerChannelStandardPos = 0
            numberPickerChannelQam!!.text = numberPickerQamStrings[numberPickerChannelQamPos]
            numberPickerChannelStandard!!.text = numberPickerStandardStrings[numberPickerChannelStandardPos]
        }
        buttonNetworkId!!.requestFocus()
    }

    fun myOnKeyDown(key_code: Int) {

        if (key_code == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if(numberPickerChannelQam!!.hasFocus()){
                if(numberPickerChannelQamPos < numberPickerQamStrings.size - 1){
                    numberPickerChannelQamPos++
                    numberPickerChannelQam!!.text = numberPickerQamStrings[numberPickerChannelQamPos]
                }
            }else if(numberPickerChannelStandard!!.hasFocus()){
                if(numberPickerChannelStandardPos < numberPickerStandardStrings.size - 1){
                    numberPickerChannelStandardPos++
                    numberPickerChannelStandard!!.text = numberPickerStandardStrings[numberPickerChannelStandardPos]
                }
            }
        }else if(key_code == KeyEvent.KEYCODE_DPAD_LEFT) {
            if(numberPickerChannelQam!!.hasFocus()){
                if(numberPickerChannelQamPos > 0){
                    numberPickerChannelQamPos--
                    numberPickerChannelQam!!.text = numberPickerQamStrings[numberPickerChannelQamPos]
                }
            }else if(numberPickerChannelStandard!!.hasFocus()){
                if(numberPickerChannelStandardPos > 0){
                    numberPickerChannelStandardPos--
                    numberPickerChannelStandard!!.text = numberPickerStandardStrings[numberPickerChannelStandardPos]
                }
            }
        }
    }

    fun editViewHasFocusNumber(textButton: EditText){
        textButton!!.setTextColor(ContextCompat.getColor(context!!,R.color.button_selected_text_fti))
        textButton!!.setBackgroundResource(R.drawable.my_action_button_selected)
    }

    fun editViewHasNoFocusNumber(textButton: EditText){
        textButton!!.setTextColor(ContextCompat.getColor(context!!,R.color.button_not_selected_text_fti))
        textButton!!.setBackgroundResource(R.drawable.my_action_button)
    }

    fun textViewHasFocusNumber(textButton: TextView){
        textButton!!.setTextColor(ContextCompat.getColor(context!!,R.color.button_selected_text_fti))
        textButton!!.setBackgroundResource(R.drawable.my_action_button_selected)
    }

    fun textViewHasNoFocusNumber(textButton: TextView){
        textButton!!.setTextColor(ContextCompat.getColor(context!!,R.color.button_not_selected_text_fti))
        textButton!!.setBackgroundResource(R.drawable.my_action_button)
    }

    fun textViewHasFocus(textButton: Button){
        textButton!!.setTextColor(ContextCompat.getColor(context!!,R.color.button_selected_text_fti))
        textButton!!.setBackgroundResource(R.drawable.my_small_action_button_selected)
        textButton!!.width = 132
        textButton!!.height = 44
    }

    fun textViewHasNoFocus(textButton: Button){
        textButton!!.setTextColor(ContextCompat.getColor(context!!,R.color.button_not_selected_text_fti))
        textButton!!.setBackgroundResource(R.drawable.my_small_action_button)
        textButton!!.width = 120
        textButton!!.height = 40
    }
}