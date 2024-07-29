package com.iwedia.cltv.fti.terrestrial

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.tv.TvContentRating
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.iwedia.cltv.ModuleProvider
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.SettingsActivity
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.fti.handlers.ConfigHandler
import com.iwedia.cltv.fti.scan_models.DataParams
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import java.io.InputStream


class ScanTuneOptionFragment : Fragment()  {

    companion object {
        fun newInstance() = ScanTuneOptionFragment()
    }

    var viewModel: View? = null
    var dataParams: DataParams? = null
    private var mTvInputManager: TvInputManager? = null
    private var mRatings: MutableSet<TvContentRating>? = null
    val configUri: Uri = Uri.parse("content://com.iwedia.cltv.platform.model.content_provider.ReferenceContentProvider/config")
    lateinit var moduleProvider: ModuleProvider
    val oemCutomizationUri = Uri.parse("content://${"com.iwedia.cltv.platform.model.content_provider.ReferenceContentProvider"}/${"oem_customization"}")

    @SuppressLint("ResourceAsColor", "SetTextI18n", "Range")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = inflater.inflate(R.layout.fti_scan_tune_option_layout, container, false)
        viewModel!!.background = ContextCompat.getDrawable(requireContext(), R.color.fti_left_black)
        moduleProvider = ModuleProvider(ReferenceApplication.get())

        val linear_border1: LinearLayout = viewModel!!.findViewById(R.id.linear_border1)
        val linear_border2: LinearLayout = viewModel!!.findViewById(R.id.linear_border2)
        val linear_border3: LinearLayout = viewModel!!.findViewById(R.id.linear_border3)
        val constraint_border1: ConstraintLayout = viewModel!!.findViewById(R.id.constraint_border1)
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            val right_side_lin_layout: LinearLayout = viewModel!!.findViewById(R.id.right_side_lin_layout)
            right_side_lin_layout.gravity = Gravity.CENTER
            linear_border1.visibility = View.GONE
            linear_border2.visibility = View.GONE
            linear_border3.visibility = View.GONE
            constraint_border1!!.background = ContextCompat.getDrawable(requireContext(), R.color.fti_left_background)
        }else{
            viewModel!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.fti_bg_gtv)
        }

        dataParams = (activity as SettingsActivity?)!!.dataParams
        val textViewPinText: TextView = viewModel!!.findViewById(R.id.textViewPinText)
        textViewPinText.text = "${ConfigStringsManager.getStringById("terrestrial_manual_tuning")} ${dataParams!!.getCountryName()}"

        var param = textViewPinText!!.layoutParams as ViewGroup.MarginLayoutParams

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            param.setMargins(50 * requireContext().getResources().getDisplayMetrics().density.toInt(), 150 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 50 * requireContext().getResources().getDisplayMetrics().density.toInt())
            textViewPinText!!.layoutParams = param
            textViewPinText!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewPinText!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewPinText!!.gravity = Gravity.LEFT
            textViewPinText!!.textSize = 36f
            textViewPinText!!.includeFontPadding = false
        }else{
            param.setMargins(0, 124 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            textViewPinText!!.layoutParams = param
            textViewPinText!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewPinText!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewPinText!!.gravity = Gravity.TOP
            textViewPinText!!.textSize = 32f
        }

        val textViewDescription: TextView = viewModel!!.findViewById(R.id.textViewDescription)
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            param = textViewDescription!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(50 * requireContext().getResources().getDisplayMetrics().density.toInt(), 20 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 50 * requireContext().getResources().getDisplayMetrics().density.toInt())
            textViewDescription!!.layoutParams = param
            textViewDescription!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewDescription!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewDescription!!.gravity = Gravity.LEFT
            textViewDescription!!.includeFontPadding = false
            textViewDescription!!.layoutParams
        }else{
            param = textViewDescription.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 20 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            textViewDescription.layoutParams = param
            textViewDescription!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewDescription!!.layoutParams.height = 96 * requireContext().getResources().getDisplayMetrics().density.toInt()
            textViewDescription.gravity = Gravity.TOP
            textViewDescription!!.includeFontPadding = false
            textViewDescription.translationY = -1.98f
            textViewDescription.textSize = 16f
        }

        val imageViewLogo: ImageView = viewModel!!.findViewById(R.id.imageViewLogo)
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            param = imageViewLogo.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(
                50 * requireContext().getResources().getDisplayMetrics().density.toInt(),
                50 * requireContext().getResources().getDisplayMetrics().density.toInt(),
                0,
                0
            )
            imageViewLogo.layoutParams = param
            imageViewLogo!!.layoutParams.width =
                40 * requireContext().getResources().getDisplayMetrics().density.toInt()
            imageViewLogo!!.layoutParams.height =
                40 * requireContext().getResources().getDisplayMetrics().density.toInt()
        }else{
            param = imageViewLogo.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 96 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            imageViewLogo.layoutParams = param
            imageViewLogo!!.layoutParams.width = 20 * requireContext().getResources().getDisplayMetrics().density.toInt()
            imageViewLogo!!.layoutParams.height = 20 * requireContext().getResources().getDisplayMetrics().density.toInt()
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

        ConfigHandler.setLcnValue(dataParams!!.getCountrydefaultLCN() == 1)
        val temp = dataParams!!.getCountryName()!!.lowercase().replace(" ", "_")

        setPvrEnabled(dataParams!!.getCountrydefaultPvr()!!)
        if(temp == "italy"){
            print("----->>>> Italy")
        }

        val buttonAuto: RelativeLayout = viewModel!!.findViewById(R.id.buttonAuto)
        val buttonAutoText: TextView = viewModel!!.findViewById(R.id.buttonAutoText)
        val buttonManual: RelativeLayout = viewModel!!.findViewById(R.id.buttonManual)
        val buttonManualText: TextView = viewModel!!.findViewById(R.id.buttonManualText)
        val buttonAntena: RelativeLayout = viewModel!!.findViewById(R.id.buttonAntena)
        val buttonAntenaText: TextView = viewModel!!.findViewById(R.id.buttonAntenaText)
        var buttonScrambled: RelativeLayout = viewModel!!.findViewById(R.id.buttonScrambled)
        var buttonScrambledText: TextView = viewModel!!.findViewById(R.id.buttonScrambledText)

        var antena = 0
        var scrambled = 0
        moduleProvider.getUtilsModule().setAntennaPower(false)

        buttonAuto!!.setFocusable(true)
        buttonManual!!.setFocusable(true)
        buttonAntena!!.setFocusable(true)

        buttonAutoText!!.setText(ConfigStringsManager.getStringById("auto_tuning"))
        buttonManualText!!.setText(ConfigStringsManager.getStringById("manual_tuning"))
        buttonAntenaText!!.setText("${ConfigStringsManager.getStringById("antenna")}: ${ConfigStringsManager.getStringById("off")}")
        buttonScrambledText!!.setText("${ConfigStringsManager.getStringById("scan_mode")}: ${ConfigStringsManager.getStringById("free")}")

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            buttonAntena!!.gravity = Gravity.CENTER
            param = buttonAntena!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonAntena!!.layoutParams = param
            buttonAntenaText!!.setTextColor(R.color.fti_text_color)
            buttonAntena!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    focusOn(buttonAntena!!, buttonAntenaText!!)
                } else {
                    focusOff(buttonAntena!!, buttonAntenaText!!)
                }
            }
        }
        else{
            buttonAntena!!.gravity = Gravity.CENTER_VERTICAL
            param = buttonAntena!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 124 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonAntena!!.layoutParams = param
            buttonAntena!!.setFocusable(true)
            buttonAntena!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
            buttonAntena!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    buttonAntena!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_on_gtv)
                    buttonAntenaText!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                }else{
                    buttonAntena!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
                    buttonAntenaText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                }
            }
            buttonAntenaText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            buttonAntenaText!!.layoutParams.width = 250 * requireContext().getResources().getDisplayMetrics().density.toInt()
            buttonAntenaText!!.layoutParams.height = 22 * requireContext().getResources().getDisplayMetrics().density.toInt()
            buttonAntenaText!!.includeFontPadding = false
            buttonAntenaText!!.isAllCaps = false
            buttonAntenaText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            param = buttonAntenaText!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(22 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
            buttonAntenaText!!.layoutParams = param
        }

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            param = buttonAuto!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonAuto!!.layoutParams = param
            buttonAuto!!.gravity = Gravity.CENTER
            buttonAutoText!!.setTextColor(R.color.fti_text_color)
            buttonAuto!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    focusOn(buttonAuto!!, buttonAutoText!!)
                } else {
                    focusOff(buttonAuto!!, buttonAutoText!!)
                }
            }
        }
        else{
            buttonAuto!!.gravity = Gravity.CENTER_VERTICAL
            param = buttonAuto!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonAuto!!.layoutParams = param
            buttonAuto!!.setFocusable(true)
            buttonAuto!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
            buttonAuto!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    buttonAuto!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_on_gtv)
                    buttonAutoText!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                }else{
                    buttonAuto!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
                    buttonAutoText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                }
            }
            buttonAutoText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            buttonAutoText!!.layoutParams.width = 250 * requireContext().getResources().getDisplayMetrics().density.toInt()
            buttonAutoText!!.layoutParams.height = 22 * requireContext().getResources().getDisplayMetrics().density.toInt()
            buttonAutoText!!.includeFontPadding = false
            buttonAutoText!!.isAllCaps = false
            buttonAutoText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            param = buttonAutoText!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(22 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
            buttonAutoText!!.layoutParams = param
        }

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            param = buttonManual!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonManual!!.layoutParams = param
            buttonManual!!.gravity = Gravity.CENTER
            buttonManualText!!.setTextColor(R.color.fti_text_color)
            buttonManual!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    focusOn(buttonManual!!, buttonManualText!!)
                } else {
                    focusOff(buttonManual!!, buttonManualText!!)
                }
            }
        }
        else{
            buttonManual!!.gravity = Gravity.CENTER_VERTICAL
            param = buttonManual!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonManual!!.layoutParams = param
            buttonManual!!.setFocusable(true)
            buttonManual!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
            buttonManual!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    buttonManual!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_on_gtv)
                    buttonManualText!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                }else{
                    buttonManual!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
                    buttonManualText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                }
            }
            buttonManualText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            buttonManualText!!.layoutParams.width = 250 * requireContext().getResources().getDisplayMetrics().density.toInt()
            buttonManualText!!.layoutParams.height = 22 * requireContext().getResources().getDisplayMetrics().density.toInt()
            buttonManualText!!.includeFontPadding = false
            buttonManualText!!.isAllCaps = false
            buttonManualText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            param = buttonManualText!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(22 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
            buttonManualText!!.layoutParams = param
        }

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            param = buttonScrambled!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonScrambled!!.layoutParams = param
            buttonScrambled!!.gravity = Gravity.CENTER
            buttonScrambledText!!.setTextColor(R.color.fti_text_color)
            buttonScrambled!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    focusOn(buttonScrambled!!, buttonScrambledText!!)
                } else {
                    focusOff(buttonScrambled!!, buttonScrambledText!!)
                }
            }
        }
        else{
            buttonScrambled!!.gravity = Gravity.CENTER_VERTICAL
            param = buttonScrambled!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonScrambled!!.layoutParams = param
            buttonScrambled!!.setFocusable(true)
            buttonScrambled!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
            buttonScrambled!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    buttonScrambled!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_on_gtv)
                    buttonScrambledText!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                }else{
                    buttonScrambled!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
                    buttonScrambledText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                }
            }
            buttonScrambledText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            buttonScrambledText!!.layoutParams.width = 250 * requireContext().getResources().getDisplayMetrics().density.toInt()
            buttonScrambledText!!.layoutParams.height = 22 * requireContext().getResources().getDisplayMetrics().density.toInt()
            buttonScrambledText!!.includeFontPadding = false
            buttonScrambledText!!.isAllCaps = false
            buttonScrambledText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            param = buttonScrambledText!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(22 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
            buttonScrambledText!!.layoutParams = param
        }

        buttonManual!!.setOnClickListener {
            (activity as SettingsActivity?)!!.goToTerManualScan()
        }
        buttonAuto!!.setOnClickListener {
            (activity as SettingsActivity?)!!.autoManual = 0
            (activity as SettingsActivity?)!!.goToScanProgressScan()
        }

        buttonAntena!!.setOnClickListener {
           if(antena == 0){
               buttonAntenaText!!.setText("${ConfigStringsManager.getStringById("antenna")}: ${ConfigStringsManager.getStringById("on")}")
               moduleProvider.getUtilsModule().setAntennaPower(true)
               antena = 1
           }else{
               buttonAntenaText!!.setText("${ConfigStringsManager.getStringById("antenna")}: ${ConfigStringsManager.getStringById("off")}")
               moduleProvider.getUtilsModule().setAntennaPower(false)
               antena = 0
           }
        }

        buttonScrambled!!.setOnClickListener {
            if(scrambled == 0){
                buttonScrambledText!!.setText("${ConfigStringsManager.getStringById("scan_mode")}: ${ConfigStringsManager.getStringById("all")}")
                changeScanType("all", configUri)
                changeScanType("all", oemCutomizationUri)
                scrambled = 1
            }else{
                buttonScrambledText!!.setText("${ConfigStringsManager.getStringById("scan_mode")}: ${ConfigStringsManager.getStringById("free")}")
                changeScanType("free", configUri)
                changeScanType("free", oemCutomizationUri)
                scrambled = 0
            }
        }

        //todo
        if((activity as SettingsActivity?)!!.moduleProvider.getGeneralConfigModule().getGeneralSettingsInfo("scan_type_switch")) {
            changeScanType("free", configUri)
            changeScanType("free", oemCutomizationUri)
        }else{
            buttonScrambled!!.visibility = View.GONE
        }

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            focusOff(buttonAntena!!,buttonAntenaText!!)
            focusOff(buttonScrambled!!, buttonScrambledText!!)
            focusOff(buttonAuto!!, buttonAutoText!!)
            focusOff(buttonManual!!, buttonManualText!!)
        }

        buttonAntena!!.requestFocus()

        return viewModel!!
    }

    fun focusOn(button: RelativeLayout, buttonText: TextView){
//        button!!.layoutParams.width = 295 * context!!.getResources().getDisplayMetrics().density.toInt()
//        button!!.layoutParams.height = 66 * context!!.getResources().getDisplayMetrics().density.toInt()
        buttonText!!.setTextColor(ContextCompat.getColor(requireContext(),R.color.button_selected_text_fti))
        button!!.setBackgroundResource(R.drawable.my_action_button_selected)
    }

    fun focusOff(button: RelativeLayout, buttonText: TextView){
        button!!.layoutParams.width = 268 * requireContext().getResources().getDisplayMetrics().density.toInt()
        button!!.layoutParams.height = 60 * requireContext().getResources().getDisplayMetrics().density.toInt()
        buttonText!!.setTextColor(ContextCompat.getColor(requireContext(),R.color.button_not_selected_text_fti))
        button!!.setBackgroundResource(R.drawable.my_action_button)
    }

    @SuppressLint("Range")
    fun setPvrEnabled(valueOfString: Int){
        val configUri: Uri = Uri.parse("content://com.iwedia.cltv.platform.model.content_provider.ReferenceContentProvider/oem_customization")
        val cursor: Cursor = requireContext().getContentResolver().query(configUri, null, null, null, null)!!
        if (cursor.moveToFirst()) {
            val mid = cursor.getInt(cursor.getColumnIndex("_id"))
            val uri: Uri = Uri.withAppendedPath(configUri, "/$mid")
            val cv = ContentValues()
            cv.put("pvr_enabled", valueOfString)
            val where = "_id" + " =?"
            val args = arrayOf("$mid")
            val result: Int = requireContext().getContentResolver().update(uri, cv, where, args)
        } else {
            val cv = ContentValues()
            cv.put("pvr_enabled", valueOfString)
            val uri: Uri = requireContext().getContentResolver().insert(configUri, cv)!!
        }
        cursor.close()
    }

    private fun getInputIds(): java.util.ArrayList<String>? {
        val retList = java.util.ArrayList<String>()
        //Get all TV inputs
        for (input in (requireContext().getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager).tvInputList) {
            val inputId = input.id
            retList.add(inputId)
        }
        return retList
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun clearAllChannels() {
        CoroutineHelper.runCoroutine ({
            clearChannelsForTvInput("content://com.iwedia.cltv.platform.model.content_provider.ReferenceContentProvider/config")
            var inputList = getInputIds()
            if (inputList!!.isNotEmpty()) {
                for (input in inputList) {
                    Log.d(Constants.LogTag.CLTV_TAG + "###", "############ clear channels for input id $input")
                    clearChannelsForTvInput(input)
                }
            }
        })
     }

    @RequiresApi(Build.VERSION_CODES.R)
    fun clearChannelsForTvInput(inputId: String) {
        val contentResolver: ContentResolver = requireContext().contentResolver
    }



    @SuppressLint("Range")
    fun changeScanType(type : String, configUri: Uri){
        val cursor: Cursor = requireContext().getContentResolver().query(configUri, null, null, null, null)!!
        if (cursor.moveToFirst()) {
            Log.d(Constants.LogTag.CLTV_TAG + "ScanTuneOptionFragment", "Update existing row with new pin")
            val mid = cursor.getInt(cursor.getColumnIndex("_id"))
            val uri: Uri = Uri.withAppendedPath(configUri, "/$mid")
            val cv = ContentValues()
            cv.put("scan_type", type)
            val where = "_id" + " =?"
            val args = arrayOf("$mid")
            val result: Int = requireContext().getContentResolver().update(uri, cv, where, args)
        } else {
            Log.d(Constants.LogTag.CLTV_TAG + "ParentalPinFtiFragment", "Insert new element in config table")
            val cv = ContentValues()
            cv.put("scan_type", type)
            val uri: Uri = requireContext().getContentResolver().insert(configUri, cv)!!
        }
        cursor.close()
    }
}