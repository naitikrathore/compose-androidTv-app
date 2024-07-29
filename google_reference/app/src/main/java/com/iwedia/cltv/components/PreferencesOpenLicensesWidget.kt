package com.iwedia.cltv.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import android.view.*
import android.view.View.OnFocusChangeListener
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.size
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.anoki_fast.SecretMenu
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.utils.Utils
import world.widget.GWidget
import world.widget.GWidgetListener

class PreferencesOpenSourceLicensesWidget @SuppressLint("InflateParams") @RequiresApi(Build.VERSION_CODES.P) constructor(
    context: Context,
    listener: PreferencesOpenSourceLicensesWidgetListener
) :
    GWidget<ConstraintLayout, PreferencesOpenSourceLicensesWidget.PreferencesOpenSourceLicensesWidgetListener>(
        ReferenceWorldHandler.WidgetId.PREFERENCES_OPEN_SOURCE_LICENSES,
        ReferenceWorldHandler.WidgetId.PREFERENCES_OPEN_SOURCE_LICENSES,
        listener
    ) {

    var context: Context? = context

    private val picassoUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    private val firebaseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    private val gsonUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    private val lottieUrl = "https://lottiefiles.com/page/license"
    private val jetpackUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    private val androidxUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    private val glideUrl = "https://github.com/bumptech/glide/blob/master/LICENSE"
    private val retrofitUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    private val okHttpUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"


    private var linearLayoutForScroll: LinearLayout? = null
    private var scrollViewForLicenses: ScrollView? = null
    private var titleTextView: TextView? = null
    private var focusOnGrid = false
    private var secretMenu: SecretMenu

    var iter = 0 // number of scroll iterations

    //used for secret pin
    private val left ='1'
    private val right ='2'
    private val down= '3'

    private var enteredPin =""
    private val secretPin = "131221"
    private var lastIndex=0

    init {
        view = LayoutInflater.from(ReferenceApplication.applicationContext()).inflate(R.layout.layout_widget_preferences_licenses, null) as ConstraintLayout
        findViews()
        secretMenu = SecretMenu(
            context = context,
            secretMenuListener = object : SecretMenu.SecretMenuListener {
                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener.setSpeechText(text = text, importance = importance)
                }

                override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                    listener.showToast(text, duration)
                }

                override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                    listener.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                }
            }
        ) {
            linearLayoutForScroll?.getChildAt(lastIndex)?.requestFocus()
        }.also {
            it.visibility = ConstraintLayout.GONE
            view?.findViewById<LinearLayout>(R.id.audio_subtitle_container)?.addView(it)
        }
    }

    /**
     * reset the entered pin
     */
    private fun resetEnteredPin(){
        enteredPin =""
    }

    /**
     * checks if the entered pin is matching with the secret pin
     * if yes then show secret menu
     */
    private fun checkPinMatch(enteredNum:Char){
        enteredPin+=enteredNum
        //this will work as queue FIFO(First in first out)
        if (enteredPin.length-1 == secretPin.length){
            enteredPin = enteredPin.removeRange(0,1)
        }
        if (enteredPin==secretPin){
            secretMenu.showUrlList(context)
            resetEnteredPin()
        }
    }
    @RequiresApi(Build.VERSION_CODES.P)
    private fun findViews() {
        linearLayoutForScroll = view!!.findViewById(R.id.ll_main_layout) as LinearLayout
        titleTextView = view!!.findViewById(R.id.title) as TextView
        titleTextView!!.text = ConfigStringsManager.getStringById("open_source")
        linearLayoutForScroll!!.gravity = Gravity.START
        scrollViewForLicenses = view!!.findViewById(R.id.ll_scroll_view)
        scrollViewForLicenses!!.smoothScrollTo(0, linearLayoutForScroll!!.top)
        scrollViewForLicenses!!.isVerticalScrollBarEnabled = false
        scrollViewForLicenses!!.isHorizontalScrollBarEnabled = false

//        scrollViewForLicenses!!.isVerticalFadingEdgeEnabled = true
//        scrollViewForLicenses!!.setFadingEdgeLength(150)

        addLicense(ConfigStringsManager.getStringById("open_source_license_picasso"), picassoUrl, 0)
        addLicense(ConfigStringsManager.getStringById("open_source_license_firebase"), firebaseUrl, 1)
        addLicense(ConfigStringsManager.getStringById("open_source_license_gson"), gsonUrl, 2)
        addLicense(ConfigStringsManager.getStringById("open_source_license_jetpack"), jetpackUrl, 3)
        addLicense(ConfigStringsManager.getStringById("open_source_license_androidx"), androidxUrl, 4)
        addLicense(ConfigStringsManager.getStringById("open_source_license_retrofit"), retrofitUrl, 5)
        addLicense(ConfigStringsManager.getStringById("open_source_license_okhttp"), okHttpUrl, 6)
        addLicense(ConfigStringsManager.getStringById("open_source_license_lottie"), lottieUrl, 7)
        addLicense(ConfigStringsManager.getStringById("open_source_license_glide"), glideUrl,8)
        lastIndex = linearLayoutForScroll?.size?.minus(1) ?: 0

    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun addLicense(licenseName: String, licenseLink: String, index: Int){
        val dynamicTextView = TextView(context)

        dynamicTextView.text = convertToSpanText(licenseName, licenseLink,false)
        dynamicTextView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val dynamicRelativeLayout = RelativeLayout(context)
        dynamicRelativeLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val param: ViewGroup.MarginLayoutParams = dynamicRelativeLayout.layoutParams as ViewGroup.MarginLayoutParams
        dynamicRelativeLayout.layoutParams = param
        dynamicRelativeLayout.isFocusable = true
        dynamicRelativeLayout.gravity = Gravity.START
        dynamicRelativeLayout.setPadding(0,Utils.convertDpToPixel(3.0).toInt(),0,Utils.convertDpToPixel(3.0).toInt())
        dynamicTextView.setPadding(Utils.convertDpToPixel(10.0).toInt(),Utils.convertDpToPixel(0.0).toInt(),Utils.convertDpToPixel(10.0).toInt(),Utils.convertDpToPixel(0.0).toInt())
        dynamicTextView.background =  ConfigColorManager.generateBackground("color_not_selected", 25f, 0.4)
        dynamicRelativeLayout.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (focusOnGrid) {
                if (hasFocus) {
                    listener.setSpeechText(dynamicTextView.text.toString())
                    dynamicTextView.background = (ConfigColorManager.generateBackground("color_main_text", 25f, 1.0))
                    Utils.focusAnimation(dynamicRelativeLayout,true)

                } else {
                    dynamicTextView.background = (ConfigColorManager.generateBackground("color_not_selected", 25f, 0.4))
                    Utils.unFocusAnimation(dynamicRelativeLayout)
                }
                //can't change color of span text so updating text again with different color on focus change
                dynamicTextView.text = convertToSpanText(licenseName, licenseLink,hasFocus)
            } else {
                listener.onPrefsCategoriesRequestFocus()
            }
        }

        dynamicRelativeLayout.setOnKeyListener( object : View.OnKeyListener{
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (event!!.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT ->{
                            if (index == lastIndex){
                            checkPinMatch(left)
                        }
                            return true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (index == lastIndex){
                                checkPinMatch(right)
                            }
                            return true
                        }

                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (index == lastIndex){
                                checkPinMatch(down)
                                return true
                            }
                            return false
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            resetEnteredPin()
                            if (index == 0) {
                                listener.onPrefsCategoriesRequestFocus()
                                focusOnGrid = false
                                true
                            }
                            return false
                        }
                    }
                }
                if (event.action == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        resetEnteredPin()
                        listener.onPrefsCategoriesRequestFocus()
                        focusOnGrid = false
                        return true
                    }
                }
                return true
            }

        })
        dynamicRelativeLayout.addView(dynamicTextView)
        linearLayoutForScroll!!.addView(dynamicRelativeLayout)
    }

    fun setFocusToGrid() {
        focusOnGrid = true
        scrollViewForLicenses!!.smoothScrollTo(0, linearLayoutForScroll!!.top)
        scrollViewForLicenses?.getChildAt(0)?.requestFocus()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun spanLicenseTitle(): CharSequence {
        val spannableStringBuilder = SpannableStringBuilder()
        val spannableTitle: Spannable = SpannableString(ConfigStringsManager.getStringById("open_source_license_title"))
        spannableStringBuilder.append(setSpanStyleMedium(spannableTitle))
        return spannableStringBuilder
    }

    /**
     * To covert a android resource into span.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun convertToSpanText(stringTitle: String, stringDes: String, isFocused :Boolean): CharSequence {
        val spannableStringBuilder = SpannableStringBuilder()
        val spannableTitle: Spannable = SpannableString(stringTitle)
        val spannableDes: Spannable = SpannableString("\n" + stringDes)
        spannableStringBuilder.append(setSpanStyleMedium(spannableTitle,isFocused))
        spannableStringBuilder.append(setSpanStyleRegular(spannableDes,isFocused))
        return spannableStringBuilder
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setSpanStyleMedium(spannable: Spannable, isFocused: Boolean? = false): Spannable {
        val typefaceMedium = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )

        spannable.setSpan(
            TypefaceSpan(typefaceMedium),
            0,
            spannable.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        var fColor = if (isFocused == true)
            Color.parseColor(ConfigColorManager.getColor("color_background"))
        else
            Color.parseColor(ConfigColorManager.getColor("color_main_text"))

        spannable.setSpan(
            ForegroundColorSpan(fColor),
            0,
            spannable.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setSpanStyleRegular(spannable: Spannable,isFocused: Boolean): Spannable {
        val typefaceRegular = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )

        spannable.setSpan(
            TypefaceSpan(typefaceRegular),
            0,
            spannable.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )

        var fColor = if (isFocused)
            Color.parseColor(ConfigColorManager.getColor("color_background"))
        else
            Color.parseColor(ConfigColorManager.getColor("color_text_description"))

        spannable.setSpan(
            ForegroundColorSpan(fColor),
            0,
            spannable.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    interface PreferencesOpenSourceLicensesWidgetListener : GWidgetListener, TTSSetterInterface,
        ToastInterface, TTSSetterForSelectableViewInterface {
        fun onPrefsCategoriesRequestFocus()

    }
}