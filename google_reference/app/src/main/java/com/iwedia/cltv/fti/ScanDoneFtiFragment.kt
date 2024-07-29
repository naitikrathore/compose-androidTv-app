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
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.fti.data.Channel
import com.iwedia.cltv.fti.handlers.ConfigHandler
import com.iwedia.cltv.fti.scan_models.DataParams
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import java.io.InputStream

class ScanDoneFtiFragment : Fragment()  {

    val TAG = javaClass.simpleName
    companion object {
        fun newInstance() = ScanDoneFtiFragment()
    }

    var viewModel: View? = null

    @SuppressLint("ResourceAsColor", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = inflater.inflate(R.layout.fti_scan_done_layout, container, false)
        viewModel!!.background = ContextCompat.getDrawable(context!!, R.color.fti_left_black)

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
            constraint_border1!!.background = ContextCompat.getDrawable(context!!, R.color.fti_left_background)
        }else{
            viewModel!!.background = ContextCompat.getDrawable(context!!, R.drawable.fti_bg_gtv)
        }

        val textViewPinText: TextView = viewModel!!.findViewById(R.id.textViewPinText)
        var param = textViewPinText!!.layoutParams as ViewGroup.MarginLayoutParams
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            param.setMargins(50 * context!!.getResources().getDisplayMetrics().density.toInt(), 150 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 50 * context!!.getResources().getDisplayMetrics().density.toInt())
            textViewPinText!!.layoutParams = param
            textViewPinText!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewPinText!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewPinText!!.gravity = Gravity.LEFT
            textViewPinText!!.textSize = 36f
            textViewPinText!!.includeFontPadding = false
        }else{
            param.setMargins(0, 124 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            textViewPinText!!.layoutParams = param
            textViewPinText!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewPinText!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewPinText!!.gravity = Gravity.TOP
            textViewPinText!!.textSize = 32f
        }
        if ((activity as SettingsActivity?)!!.terrestrialCable == 0) {
            textViewPinText.text =
                ConfigStringsManager.getStringById("terrestrial") + " " + ConfigStringsManager.getStringById(
                    "scan_completed"
                ).lowercase()
        }else{
            textViewPinText.text =
                ConfigStringsManager.getStringById("cable") + " " + ConfigStringsManager.getStringById(
                    "scan_completed"
                ).lowercase()
        }

        val textViewDescriptionChannels: TextView = viewModel!!.findViewById(R.id.textViewDescriptionChannels)
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            param = textViewDescriptionChannels!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(50 * context!!.getResources().getDisplayMetrics().density.toInt(), 20 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 50 * context!!.getResources().getDisplayMetrics().density.toInt())
            textViewDescriptionChannels!!.layoutParams = param
            textViewDescriptionChannels!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewDescriptionChannels!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewDescriptionChannels!!.gravity = Gravity.LEFT
            textViewDescriptionChannels!!.includeFontPadding = false
            textViewDescriptionChannels!!.layoutParams
        }else{
            param = textViewDescriptionChannels.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 20 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            textViewDescriptionChannels.layoutParams = param
            textViewDescriptionChannels!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewDescriptionChannels!!.layoutParams.height = 96 * context!!.getResources().getDisplayMetrics().density.toInt()
            textViewDescriptionChannels.gravity = Gravity.TOP
            textViewDescriptionChannels!!.includeFontPadding = false
            textViewDescriptionChannels.translationY = -1.98f
            textViewDescriptionChannels.textSize = 16f
        }
        textViewDescriptionChannels.text = "${(activity as SettingsActivity?)!!.programmesFound} ${ConfigStringsManager.getStringById("programmes_found")}"

        val textViewDescriptionLCN: TextView = viewModel!!.findViewById(R.id.textViewDescriptionLCN)
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            param = textViewDescriptionLCN!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(50 * context!!.getResources().getDisplayMetrics().density.toInt(), 20 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 50 * context!!.getResources().getDisplayMetrics().density.toInt())
            textViewDescriptionLCN!!.layoutParams = param
            textViewDescriptionLCN!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewDescriptionLCN!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewDescriptionLCN!!.gravity = Gravity.LEFT
            textViewDescriptionLCN!!.includeFontPadding = false
            textViewDescriptionLCN!!.layoutParams
        }else{
            param = textViewDescriptionLCN.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 20 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            textViewDescriptionLCN.layoutParams = param
            textViewDescriptionLCN!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewDescriptionLCN!!.layoutParams.height = 96 * context!!.getResources().getDisplayMetrics().density.toInt()
            textViewDescriptionLCN.gravity = Gravity.TOP
            textViewDescriptionLCN!!.includeFontPadding = false
            textViewDescriptionLCN.translationY = -1.98f
            textViewDescriptionLCN.textSize = 16f
        }
        textViewDescriptionLCN.text = ""

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


        val buttonDone: RelativeLayout = viewModel!!.findViewById(R.id.buttonDone)
        val buttonDoneText: TextView = viewModel!!.findViewById(R.id.buttonDoneText)
        val buttonRetry: RelativeLayout = viewModel!!.findViewById(R.id.buttonRetry)
        val buttonRetryText: TextView = viewModel!!.findViewById(R.id.buttonRetryText)

        buttonDone!!.setFocusable(true)
        buttonRetry!!.setFocusable(true)

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            buttonDone!!.gravity = Gravity.CENTER
            param = buttonDone!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonDone!!.layoutParams = param
            buttonDoneText!!.setTextColor(R.color.fti_text_color)
            buttonDone!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    focusOn(buttonDone!!, buttonDoneText!!)
                } else {
                    focusOff(buttonDone!!, buttonDoneText!!)
                }
            }
        }
        else{
            buttonDone!!.gravity = Gravity.CENTER_VERTICAL
            param = buttonDone!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 124 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonDone!!.layoutParams = param
            buttonDone!!.setFocusable(true)
            buttonDone!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_off_gtv)
            buttonDone!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    buttonDone!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_on_gtv)
                    buttonDoneText!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                }else{
                    buttonDone!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_off_gtv)
                    buttonDoneText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                }
            }
            buttonDoneText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            buttonDoneText!!.layoutParams.width = 250 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonDoneText!!.layoutParams.height = 22 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonDoneText!!.includeFontPadding = false
            buttonDoneText!!.isAllCaps = false
            buttonDoneText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            param = buttonDoneText!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(22 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
            buttonDoneText!!.layoutParams = param
        }

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            param = buttonRetry!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonRetry!!.layoutParams = param
            buttonRetry!!.gravity = Gravity.CENTER
            buttonRetryText!!.setTextColor(R.color.fti_text_color)
            buttonRetry!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    focusOn(buttonRetry!!, buttonRetryText!!)
                } else {
                    focusOff(buttonRetry!!, buttonRetryText!!)
                }
            }
        }
        else{
            buttonRetry!!.gravity = Gravity.CENTER_VERTICAL
            param = buttonRetry!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonRetry!!.layoutParams = param
            buttonRetry!!.setFocusable(true)
            buttonRetry!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_off_gtv)
            buttonRetry!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    buttonRetry!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_on_gtv)
                    buttonRetryText!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                }else{
                    buttonRetry!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_off_gtv)
                    buttonRetryText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                }
            }
            buttonRetryText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            buttonRetryText!!.layoutParams.width = 250 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonRetryText!!.layoutParams.height = 22 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonRetryText!!.includeFontPadding = false
            buttonRetryText!!.isAllCaps = false
            buttonRetryText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            param = buttonRetryText!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(22 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
            buttonRetryText!!.layoutParams = param
        }

        buttonRetryText.text  = ConfigStringsManager.getStringById("retry")
        if((activity as SettingsActivity?)!!.programmesFound.toInt() == 0){
            buttonDoneText.text  = ConfigStringsManager.getStringById("failed")
        }else{
            buttonDoneText.text  = ConfigStringsManager.getStringById("proceed")
        }
        if((activity as SettingsActivity?)!!.hasLcnConflicts){
            val duplicates: HashMap<Int, MutableList<Channel>>? = (activity as SettingsActivity?)!!.mChannelsHandler!!.getMapOfDuplicateChannels((activity as SettingsActivity?)!!.mChannels!!)
            buttonDoneText.text  = ConfigStringsManager.getStringById("solve_lcn_manu")
            textViewDescriptionChannels.text = "${textViewDescriptionChannels.text}\n${duplicates!!.size}${ConfigStringsManager.getStringById("lcn_conf_found")}"
            for(i in 0 until duplicates!!.size){
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreateView: ${(activity as SettingsActivity?)!!.conflictFixedList.size}")
                (activity as SettingsActivity?)!!.conflictFixedList.add(i,false)
            }
        }






        buttonDone!!.setOnClickListener {
            if((activity as SettingsActivity?)!!.hasLcnConflicts){
                (activity as SettingsActivity?)!!.goToLcnConflict()
            }else {
                if ((activity as SettingsActivity?)!!.terrestrialCable == 0){
                    var dataParams: DataParams? = (activity as SettingsActivity?)!!.dataParams
                    if (dataParams!!.getCountrydefaultLCN() == 0 || buttonDoneText.text == ConfigStringsManager.getStringById("failed") || !(activity as SettingsActivity?)!!.hasLcnConflicts) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            (activity as SettingsActivity?)!!.finishAffinity()
                            android.os.Process.killProcess(android.os.Process.myPid())
                        } else {
                            (activity as SettingsActivity?)!!.finish()
                            android.os.Process.killProcess(android.os.Process.myPid())
                            System.exit(0)
                        }
                    }
                }else{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        (activity as SettingsActivity?)!!.finishAffinity()
                        android.os.Process.killProcess(android.os.Process.myPid())
                    } else {
                        (activity as SettingsActivity?)!!.finish()
                        android.os.Process.killProcess(android.os.Process.myPid())
                        System.exit(0)
                    }
                }
            }
        }

        buttonRetry!!.setOnClickListener {
            if ((activity as SettingsActivity?)!!.terrestrialCable == 0) {
                (activity as SettingsActivity?)!!.goToTuneOptionFragment()
            }else{
                (activity as SettingsActivity?)!!.goToScanTuneCableOptionFragment()
            }
        }

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            focusOff(buttonDone!!, buttonDoneText!!)
            focusOff(buttonRetry!!, buttonRetryText!!)
        }

        buttonDone!!.requestFocus()

        return viewModel!!
    }

    fun focusOn(button: RelativeLayout, buttonText: TextView){
//        button!!.layoutParams.width = 295 * context!!.getResources().getDisplayMetrics().density.toInt()
//        button!!.layoutParams.height = 66 * context!!.getResources().getDisplayMetrics().density.toInt()
        buttonText!!.setTextColor(ContextCompat.getColor(context!!,R.color.button_selected_text_fti))
        button!!.setBackgroundResource(R.drawable.my_action_button_selected)
    }

    fun focusOff(button: RelativeLayout, buttonText: TextView){
        button!!.layoutParams.width = 268 * context!!.getResources().getDisplayMetrics().density.toInt()
        button!!.layoutParams.height = 60 * context!!.getResources().getDisplayMetrics().density.toInt()
        buttonText!!.setTextColor(ContextCompat.getColor(context!!,R.color.button_not_selected_text_fti))
        button!!.setBackgroundResource(R.drawable.my_action_button)
    }
}