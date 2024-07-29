package com.iwedia.cltv.components

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnKeyListener
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.downActionBackKeyDone
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.`interface`.TTSSpeakTextForFocusedViewInterface
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.utils.Utils

class PrefSwitchViewHolder(
    val parent: ViewGroup,
    private val ttsSetterForSelectableViewInterface: TTSSetterForSelectableViewInterface
) : RecyclerView.ViewHolder (
    LayoutInflater.from(parent.context).inflate(R.layout.pref_switch, parent, false)
), PrefComponentInterface<CompoundItem>, TTSSpeakTextForFocusedViewInterface{

    var title: TextView? = null
    var switch: Switch
    var switchTitle:String? =null
    val TAG = javaClass.simpleName
    init {
        //Set references
        title = itemView.findViewById(R.id.title)
        switch = itemView.findViewById(R.id.switch_view)!!

        title!!.isSingleLine = true
        title!!.ellipsize = TextUtils.TruncateAt.END

        // Define a ColorStateList
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )

        // Define track color
        val trackColors = intArrayOf(
            Color.parseColor(
                ConfigColorManager.getColor(
                    "color_preference_checked"
                )
            ),
            Color.parseColor(
                ConfigColorManager.getColor(
                    "color_not_selected"
                )
            ),
        )

        val trackColorStateList = ColorStateList(states, trackColors)

        // Create a Drawable for the track to fix transparency
        val trackDrawable = GradientDrawable()
        trackDrawable.shape = GradientDrawable.RECTANGLE
        trackDrawable.setTintList(trackColorStateList)
        trackDrawable.setStroke(Utils.getDimensInPixelSize(R.dimen.custom_dim_7), Color.TRANSPARENT)
        trackDrawable.cornerRadius = Utils.getDimensInPixelSize(R.dimen.custom_dim_7).toFloat()

        // Set the track drawable
        switch.trackDrawable = trackDrawable

        // Define thumb color
        val thumbColors = intArrayOf(
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor("color_preference_checked"), 0.7
                )
            ),
            Color.TRANSPARENT
        )

        val thumbColorStateList = ColorStateList(states, thumbColors)

        // Set the thumb color state list
        switch.thumbTintList = thumbColorStateList
        switch.thumbTintMode = PorterDuff.Mode.SRC_ATOP

        setFocus(false)
    }

    override fun speakTextForFocusedView() {
        ttsSetterForSelectableViewInterface.setSpeechTextForSelectableView(
            "",
            type = Type.SWITCH,
            isChecked = switch.isChecked
        )
    }

    private fun setFocus(isFocused : Boolean){
        if (isFocused) {
            speakTextForFocusedView()
            itemView.setBackgroundResource(R.drawable.focus_shape)
            itemView.backgroundTintList  = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_selector")))

            title!!.typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )
            try {
                val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "setFocus: Exception color_context $color_context")
                title!!.setTextColor(
                    color_context
                )
            } catch(ex: Exception) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "setFocus: Exception color rdb $ex")
            }

        } else {
            itemView.setBackgroundResource(R.drawable.transparent_shape)

            title!!.typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")

            )
            title!!.setTextColor(
                Color.parseColor(ConfigColorManager.getColor("color_main_text"))
            )
        }
    }

    override fun requestFocus() {
        itemView.requestFocus()
    }

    override fun clearFocus() {
        itemView.clearFocus()
    }

    fun changeSwitchStatus(isChecked :Boolean){
        switch.isChecked = isChecked
        val onText = ConfigStringsManager.getStringById("on")
        val offText = ConfigStringsManager.getStringById("off")
        if (switchTitle!=onText && switchTitle!=offText) {
            title!!.text = switchTitle
        }
        else title!!.text =if(isChecked) onText else offText
    }

    override fun refresh(data: CompoundItem,id:Pref, listener: PrefItemListener) {
        switchTitle = data.title

        changeSwitchStatus(data.isChecked)
        val keyListener: OnKeyListener = object : OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (!ReferenceApplication.worldHandler?.isEnableUserInteraction!!) {
                    return true
                }
                if (event!!.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            return if (ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                listener.onAction(Action.LEFT,id)
                            } else {
                                listener.onAction(Action.RIGHT,id)
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            return if (ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                listener.onAction(Action.RIGHT,id)
                            } else {
                                listener.onAction(Action.LEFT,id)
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            return listener.onAction(Action.DOWN,id)
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            return listener.onAction(Action.UP,id)
                        }
                        KeyEvent.KEYCODE_BACK,KeyEvent.KEYCODE_ESCAPE -> {
                        downActionBackKeyDone = true
                        return true
                        }
                    }
                }

                if (event.action == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (!downActionBackKeyDone)return true
                        downActionBackKeyDone = false
                        return listener.onAction(Action.BACK,id)
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
                        data.compoundListener.onChange(!switch.isChecked,0, id,
                            object :IAsyncDataCallback<Boolean>{
                            override fun onFailed(error: Error) {
                            }
                            override fun onReceive(isChecked: Boolean) {
                                changeSwitchStatus(isChecked)
                                data.isChecked = isChecked
                                listener.onAction(Action.UPDATE, id)
                                speakTextForFocusedView()
                            }
                        })
                        return true
                    }
                }
                return false
            }
        }
        itemView.setOnFocusChangeListener { view, hasFocus ->
            title!!.ellipsize = if(hasFocus) TextUtils.TruncateAt.MARQUEE else TextUtils.TruncateAt.END
            title!!.isSelected = hasFocus
            listener.onAction(Action.FOCUSED,id)

            setFocus(hasFocus)
        }


        itemView.setOnKeyListener(keyListener)

    }

    override fun notifyPreferenceUpdated() {
    }
}