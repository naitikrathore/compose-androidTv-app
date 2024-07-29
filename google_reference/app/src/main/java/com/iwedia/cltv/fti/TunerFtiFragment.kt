package com.iwedia.cltv.fti

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
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
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.iwedia.cltv.R
import com.iwedia.cltv.SettingsActivity
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.fti.handlers.ConfigHandler
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import java.io.InputStream

class TunerFtiFragment: Fragment()  {

    val TAG = javaClass.simpleName
    companion object {
        fun newInstance() = TunerFtiFragment()
    }

    var viewModel: View? = null
    var buttonTerrestrial: RelativeLayout? = null
    var buttonTerrestrialText: TextView? = null
    var buttonCable: RelativeLayout? = null
    var buttonCableText: TextView? = null
    var buttonCancel: RelativeLayout? = null
    var buttonCancelText: TextView? = null

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("ResourceAsColor")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = inflater.inflate(R.layout.fti_tuner_layout, container, false)
        viewModel!!.background = ContextCompat.getDrawable(requireContext(), R.color.fti_left_black)

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

        val textViewScanText: TextView = viewModel!!.findViewById(R.id.textViewScanText)
        textViewScanText.text = ConfigStringsManager.getStringById("channels_scan_select")

        var param = textViewScanText!!.layoutParams as ViewGroup.MarginLayoutParams

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            param.setMargins(50 * requireContext().getResources().getDisplayMetrics().density.toInt(), 150 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 50 * requireContext().getResources().getDisplayMetrics().density.toInt())
            textViewScanText!!.layoutParams = param
            textViewScanText!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewScanText!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewScanText!!.gravity = Gravity.LEFT
            textViewScanText!!.textSize = 36f
            textViewScanText!!.includeFontPadding = false
        }else{
            param.setMargins(0, 124 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            textViewScanText!!.layoutParams = param
            textViewScanText!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewScanText!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewScanText!!.gravity = Gravity.TOP
            textViewScanText!!.textSize = 32f
        }

        val textViewDescription: TextView = viewModel!!.findViewById(R.id.textViewDescription)
        textViewDescription.text = ConfigStringsManager.getStringById("channels_scan_select_description")

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

        buttonTerrestrial = viewModel!!.findViewById(R.id.buttonTerrestrial)
        buttonTerrestrialText = viewModel!!.findViewById(R.id.buttonTerrestrialText)

        buttonCable = viewModel!!.findViewById(R.id.buttonCable)
        buttonCableText = viewModel!!.findViewById(R.id.buttonCableText)

        buttonCancel = viewModel!!.findViewById(R.id.buttonCancel)
        buttonCancelText = viewModel!!.findViewById(R.id.buttonCancelText)

        buttonTerrestrialText!!.text = ConfigStringsManager.getStringById("terrestrial_scan")
        buttonCableText!!.text = ConfigStringsManager.getStringById("cable_scan")
        buttonCancelText!!.text = ConfigStringsManager.getStringById("cancel_scan")

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            buttonTerrestrial!!.gravity = Gravity.CENTER
            param = buttonTerrestrial!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonTerrestrial!!.layoutParams = param
            buttonTerrestrialText!!.setTextColor(R.color.fti_text_color)
            buttonTerrestrial!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    focusOn(buttonTerrestrial!!, buttonTerrestrialText!!)
                } else {
                    focusOff(buttonTerrestrial!!, buttonTerrestrialText!!)
                }
            }
        }
        else{
            buttonTerrestrial!!.gravity = Gravity.CENTER_VERTICAL
            param = buttonTerrestrial!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 124 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonTerrestrial!!.layoutParams = param
            buttonTerrestrial!!.setFocusable(true)
            buttonTerrestrial!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
            buttonTerrestrial!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    buttonTerrestrial!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_on_gtv)
                    buttonTerrestrialText!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                }else{
                    buttonTerrestrial!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
                    buttonTerrestrialText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                }
            }
            buttonTerrestrialText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            buttonTerrestrialText!!.layoutParams.width = 250 * requireContext().getResources().getDisplayMetrics().density.toInt()
            buttonTerrestrialText!!.layoutParams.height = 22 * requireContext().getResources().getDisplayMetrics().density.toInt()
            buttonTerrestrialText!!.includeFontPadding = false
            buttonTerrestrialText!!.isAllCaps = false
            buttonTerrestrialText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            param = buttonTerrestrialText!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(22 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
            buttonTerrestrialText!!.layoutParams = param
        }

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            param = buttonCable!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonCable!!.layoutParams = param
            buttonCable!!.gravity = Gravity.CENTER
            buttonCableText!!.setTextColor(R.color.fti_text_color)
            buttonCable!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    focusOn(buttonCable!!, buttonCableText!!)
                } else {
                    focusOff(buttonCable!!, buttonCableText!!)
                }
            }
        }
        else{
            buttonCable!!.gravity = Gravity.CENTER_VERTICAL
            param = buttonCable!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonCable!!.layoutParams = param
            buttonCable!!.setFocusable(true)
            buttonCable!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
            buttonCable!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    buttonCable!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_on_gtv)
                    buttonCableText!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                }else{
                    buttonCable!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
                    buttonCableText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                }
            }
            buttonCableText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            buttonCableText!!.layoutParams.width = 250 * requireContext().getResources().getDisplayMetrics().density.toInt()
            buttonCableText!!.layoutParams.height = 22 * requireContext().getResources().getDisplayMetrics().density.toInt()
            buttonCableText!!.includeFontPadding = false
            buttonCableText!!.isAllCaps = false
            buttonCableText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            param = buttonCableText!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(22 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
            buttonCableText!!.layoutParams = param
        }

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            param = buttonCancel!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonCancel!!.layoutParams = param
            buttonCancel!!.gravity = Gravity.CENTER
            buttonCancelText!!.setTextColor(R.color.fti_text_color)
            buttonCancel!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    focusOn(buttonCancel!!, buttonCancelText!!)
                } else {
                    focusOff(buttonCancel!!, buttonCancelText!!)
                }
            }
        }
        else{
            buttonCancel!!.gravity = Gravity.CENTER_VERTICAL
            param = buttonCancel!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonCancel!!.layoutParams = param
            buttonCancel!!.setFocusable(true)
            buttonCancel!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
            buttonCancel!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    buttonCancel!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_on_gtv)
                    buttonCancelText!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                }else{
                    buttonCancel!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
                    buttonCancelText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                }
            }
            buttonCancelText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            buttonCancelText!!.layoutParams.width = 250 * requireContext().getResources().getDisplayMetrics().density.toInt()
            buttonCancelText!!.layoutParams.height = 22 * requireContext().getResources().getDisplayMetrics().density.toInt()
            buttonCancelText!!.includeFontPadding = false
            buttonCancelText!!.isAllCaps = false
            buttonCancelText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            param = buttonCancelText!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(22 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
            buttonCancelText!!.layoutParams = param
        }


        buttonTerrestrial!!.setFocusable(true)
        buttonCable!!.setFocusable(true)
        buttonCancel!!.setFocusable(true)

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            focusOff(buttonCancel!!, buttonCancelText!!)
            focusOff(buttonCable!!, buttonCableText!!)
            focusOff(buttonTerrestrial!!, buttonTerrestrialText!!)
        }

        buttonTerrestrial!!.requestFocus()

        buttonTerrestrial!!.setOnClickListener {
            (activity as SettingsActivity?)!!.terrestrialCable = 0
            (activity as SettingsActivity?)!!.goToTuneOptionFragment()
        }

        buttonCable!!.setOnClickListener {
            (activity as SettingsActivity?)!!.terrestrialCable = 1
            (activity as SettingsActivity?)!!.goToCableSelectProviderFragment()
        }

        buttonCancel!!.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                (activity as SettingsActivity?)!!.finishAffinity()
                android.os.Process.killProcess(android.os.Process.myPid())
            } else{
                (activity as SettingsActivity?)!!.finish()
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(0)
            }
        }

        //todo treba ispraviti dodati logiku kad je sta kad ukljuceno
        if(!(activity as SettingsActivity?)!!.moduleProvider.getGeneralConfigModule().getGeneralSettingsInfo("cable_scan_enabled")){
            buttonCable!!.visibility = View.GONE
        }else{
            if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                focusOff(buttonCable!!, buttonCableText!!)
            }else{
                buttonCable!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
                buttonCableText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            }
        }
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

    fun myOnKeyDown(key_code: Int) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "myOnKeyDown: ")
    }

}