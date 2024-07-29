package com.iwedia.cltv.components

import android.graphics.Color
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceDrawableButton
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.Constants

/**
 * ZapDigitChannelViewHolder
 *
 * @author Aleksandar Milojevic
 */
class ZapDigitChannelViewHolder(view: View, ttsSetterInterface: TTSSetterInterface) : RecyclerView.ViewHolder(view) {

    //Category text
    var referenceDrawableButton: ReferenceDrawableButton? = null
    val TAG = javaClass.simpleName
    init {

        //Set references
        referenceDrawableButton = view.findViewById(R.id.zap_digit_channel_drawable_button)
        referenceDrawableButton!!.textToSpeechHandler.setupTextToSpeechTextSetterInterface(ttsSetterInterface)
        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_text_description"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Constructor: Exception color_context $color_context")
            referenceDrawableButton!!.getDrawable().setColorFilter(
                color_context
            )
        } catch(ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Constructor: Exception color rdb $ex")
        }

        //Set root view to be focusable and clickable
        referenceDrawableButton!!.focusable = View.FOCUSABLE
        referenceDrawableButton!!.isClickable = true
    }
}