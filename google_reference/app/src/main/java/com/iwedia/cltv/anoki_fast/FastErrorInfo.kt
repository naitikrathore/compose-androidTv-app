package com.iwedia.cltv.anoki_fast

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.components.custom_view_base_classes.BaseConstraintLayout
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText

/**
 * Fast error info
 *
 * @author Dejan Nadj
 */
class FastErrorInfo: BaseConstraintLayout {

    enum class InfoType {
        NO_INTERNET,
        ANOKI_SERVER_DOWN,
        REGION_NOT_SUPPORTED
    }

    /**
     *  Constructor used for home data that is set in xml
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialisation()
    }

    /**
     *  Constructor used for home data that is set in xml
     */
    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int) : super(
        context,
        attrs,
        defStyleAttrs
    ) {
        initialisation()
    }

    private fun initialisation() {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        LayoutInflater.from(context).inflate(R.layout.layout_fast_no_internet_info, this, true)


        findViewById<TextView>(R.id.title).apply {
            typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), "work_sans_regular.ttf")
            setTextColor(Color.parseColor("#eeeeee"))
            text = ConfigStringsManager.getStringById("no_internet_connection")
        }

        findViewById<TextView>(R.id.message).apply {
            typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), "work_sans_regular.ttf")
            setTextColor(Color.parseColor("#cccccc"))
            text = ConfigStringsManager.getStringById("please_try_to_connect_message")
        }
    }

    fun setTextInfo(infoType: InfoType) {
        findViewById<ImageView>(R.id.image).apply {
            if (infoType == InfoType.REGION_NOT_SUPPORTED) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
            }
        }
        findViewById<TextView>(R.id.title).apply {
            if (infoType == InfoType.NO_INTERNET) {
                text = ConfigStringsManager.getStringById("no_internet_connection")
            } else if (infoType == InfoType.ANOKI_SERVER_DOWN) {
                text = resources.getString(R.string.no_provider_connection)
            } else {
                text = resources.getString(R.string.region_not_supported)
            }
        }
        findViewById<TextView>(R.id.message).apply {
            if (infoType == InfoType.NO_INTERNET) {
                text = ConfigStringsManager.getStringById("please_try_to_connect_message")
            } else if (infoType == InfoType.ANOKI_SERVER_DOWN) {
                text = resources.getString(R.string.please_check_connection)
            } else {
                text = ""
            }
        }
    }

    override fun setVisibility(visibility: Int) {
        if (visibility == View.VISIBLE) {
            try {
                textToSpeechHandler.setSpeechText(
                    ConfigStringsManager.getStringById("no_internet_connection"),
                    ConfigStringsManager.getStringById("please_try_to_connect_message"),
                    importance = SpeechText.Importance.HIGH
                )

            } catch (e: Exception) {
                throw Exception("Make sure that textToSpeechTextSetter is initialised. If this error occur find place where FastErrorInfo class is used and set it's textToSpeechTextSetter using updateTextToSpeechTextSetter() method")
            }
        }
        super.setVisibility(visibility)
    }
}