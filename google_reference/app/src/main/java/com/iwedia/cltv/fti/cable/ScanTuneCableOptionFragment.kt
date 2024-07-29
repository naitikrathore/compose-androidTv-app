package com.iwedia.cltv.fti.cable

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import com.iwedia.cltv.fti.handlers.ConfigHandler
import com.iwedia.cltv.fti.scan_models.DataParams
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import java.io.InputStream

class ScanTuneCableOptionFragment : Fragment()  {

    companion object {
        fun newInstance() = ScanTuneCableOptionFragment()
    }

    var viewModel: View? = null

    @SuppressLint("ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = inflater.inflate(R.layout.fti_scan_tune_cable_options_layout, container, false)
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

        val textViewDescription: TextView = viewModel!!.findViewById(R.id.textViewDescription)
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

        val dataParams: DataParams = (activity as SettingsActivity?)!!.dataParams!!
        var temp = dataParams.getCountryName()!!.lowercase().replace(" ", "_")
        textViewPinText.text = "${ConfigStringsManager.getStringById("cable_manual_tuning")} ${ConfigStringsManager.getStringById(temp)}"
        //ReferenceSdk.setLcn((activity as SettingsActivity?)!!.dataParams!!.getCountrydefaultLCN() == 1)
        setPvrEnabled((activity as SettingsActivity?)!!.dataParams!!.getCountrydefaultPvr()!!)

        val buttonQuick: RelativeLayout = viewModel!!.findViewById(R.id.buttonQuick)
        val buttonQuickText: TextView = viewModel!!.findViewById(R.id.buttonQuickText)
        val buttonManual: RelativeLayout = viewModel!!.findViewById(R.id.buttonManual)
        val buttonManualText: TextView = viewModel!!.findViewById(R.id.buttonManualText)

        buttonQuick!!.setFocusable(true)
        buttonManual!!.setFocusable(true)

        buttonQuickText!!.setText(ConfigStringsManager.getStringById("quick_tuning"))
        buttonManualText!!.setText(ConfigStringsManager.getStringById("manual_tuning"))

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            buttonQuick!!.gravity = Gravity.CENTER
            param = buttonQuick!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonQuick!!.layoutParams = param
            buttonQuickText!!.setTextColor(R.color.fti_text_color)
            buttonQuick!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    focusOn(buttonQuick!!, buttonQuickText!!)
                } else {
                    focusOff(buttonQuick!!, buttonQuickText!!)
                }
            }
        }
        else{
            buttonQuick!!.gravity = Gravity.CENTER_VERTICAL
            param = buttonQuick!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 124 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonQuick!!.layoutParams = param
            buttonQuick!!.setFocusable(true)
            buttonQuick!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_off_gtv)
            buttonQuick!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    buttonQuick!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_on_gtv)
                    buttonQuickText!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                }else{
                    buttonQuick!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_off_gtv)
                    buttonQuickText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                }
            }
            buttonQuickText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            buttonQuickText!!.layoutParams.width = 250 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonQuickText!!.layoutParams.height = 22 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonQuickText!!.includeFontPadding = false
            buttonQuickText!!.isAllCaps = false
            buttonQuickText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            param = buttonQuickText!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(22 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
            buttonQuickText!!.layoutParams = param
        }

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            param = buttonManual!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 10 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
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
            param.setMargins(0, 10 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonManual!!.layoutParams = param
            buttonManual!!.setFocusable(true)
            buttonManual!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_off_gtv)
            buttonManual!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    buttonManual!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_on_gtv)
                    buttonManualText!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                }else{
                    buttonManual!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_off_gtv)
                    buttonManualText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                }
            }
            buttonManualText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            buttonManualText!!.layoutParams.width = 250 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonManualText!!.layoutParams.height = 22 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonManualText!!.includeFontPadding = false
            buttonManualText!!.isAllCaps = false
            buttonManualText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            param = buttonManualText!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(22 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
            buttonManualText!!.layoutParams = param
        }

        buttonManual!!.setOnClickListener {
            (activity as SettingsActivity?)!!.manualQuick = 0
            (activity as SettingsActivity?)!!.goToCableManualScanFragment()
        }
        buttonQuick!!.setOnClickListener {
            (activity as SettingsActivity?)!!.manualQuick = 1
            (activity as SettingsActivity?)!!.goToCableManualScanFragment()
        }

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            focusOff(buttonQuick!!, buttonQuickText!!)
            focusOff(buttonManual!!, buttonManualText!!)
        }

        buttonQuick!!.requestFocus()

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


    @SuppressLint("Range")
    fun setPvrEnabled(valueOfString: Int){
        val configUri: Uri = Uri.parse("content://com.iwedia.cltv.platform.model.content_provider.ReferenceContentProvider/oem_customization")
        val cursor: Cursor = context!!.getContentResolver().query(configUri, null, null, null, null)!!
        if (cursor.moveToFirst()) {
            val mid = cursor.getInt(cursor.getColumnIndex("_id"))
            val uri: Uri = Uri.withAppendedPath(configUri, "/$mid")
            val cv = ContentValues()
            cv.put("pvr_enabled", valueOfString)
            val where = "_id" + " =?"
            val args = arrayOf("$mid")
            val result: Int = context!!.getContentResolver().update(uri, cv, where, args)
        } else {
            val cv = ContentValues()
            cv.put("pvr_enabled", valueOfString)
            val uri: Uri = context!!.getContentResolver().insert(configUri, cv)!!
        }
        cursor.close()
    }

}