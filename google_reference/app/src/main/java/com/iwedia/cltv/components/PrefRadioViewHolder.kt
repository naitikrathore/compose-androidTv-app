package com.iwedia.cltv.components

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnKeyListener
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.downActionBackKeyDone
import com.iwedia.cltv.ReferenceDrawableButton
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.platform.`interface`.TTSSpeakTextForFocusedViewInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.text_to_speech.Type

class PrefRadioViewHolder(
    val parent: ViewGroup,
    private val ttsSetterForSelectableViewInterface: TTSSetterForSelectableViewInterface
) : RecyclerView.ViewHolder (
    LayoutInflater.from(parent.context).inflate(R.layout.pref_radio, parent, false)
), PrefComponentInterface<CompoundItem>, TTSSpeakTextForFocusedViewInterface {

    //List item
    var listItem: ReferenceDrawableButton

    var isChecked = false
    var isFocused = false

    init {
        //Set references
        listItem = itemView.findViewById(R.id.radio_item)!!

        listItem.getTextView().gravity = Gravity.START

        //background
        listItem!!.background =
            ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.transparent_shape
            )


        listItem!!.getTextView().typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )


        val textSize =
            ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_13)
        listItem!!.getTextView().setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            textSize
        )

        itemView.setOnFocusChangeListener { view, hasFocus ->
            setFocus(hasFocus)
        }

        setFocus(isFocused)
        changeSwitchStatus(isChecked)
    }

    private fun setFocus(isFocused : Boolean){
        this.isFocused = isFocused
        if (isFocused) {
            speakTextForFocusedView()
            listItem!!.getTextView().animate().scaleY(1.06f).scaleX(1.06f).duration =
                0
            listItem!!.background = ConfigColorManager.generateButtonBackground()

            if (isChecked) {

                val draw = ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.ic_small_radio_filled
                )
                draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_preference_checked")))
                listItem!!.setDrawable(
                    draw
                )

            } else {
                val draw = ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.ic_small_radio_empty
                )
                draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_background")))
                listItem!!.setDrawable(
                    draw
                )
            }

            listItem!!.getTextView().setTextColor(
                Color.parseColor(ConfigColorManager.Companion.getColor("color_background"))
            )
        } else {

            if (isChecked) {
                val draw = ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.ic_small_radio_filled
                )
                draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_preference_checked")))
                listItem!!.setDrawable(
                    draw
                )
            } else {
                val draw = ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.ic_small_radio_empty
                )
                draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                listItem!!.setDrawable(
                    draw
                )
            }
            listItem!!.getTextView().setTextColor(
                Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"))
            )

            listItem!!.getTextView().animate().scaleY(1f).scaleX(1f).duration = 0
            listItem!!.background =
                ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.transparent_shape
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
        this.isChecked = isChecked
        if (isChecked) {
            val draw = ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.ic_small_radio_filled
            )
            draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_preference_checked")))
            listItem!!.setDrawable(
                draw
            )
        } else {
            val draw = ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.ic_small_radio_empty
            )
            if (isFocused) {
                draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_background")))
            } else {
                draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            }
            listItem!!.setDrawable(
                draw
            )
        }

    }

    var data: CompoundItem? = null
    var listener: PrefItemListener? = null

    override fun refresh(data: CompoundItem, id:Pref, listener: PrefItemListener) {
        this.data = data
        this.listener = listener
        changeSwitchStatus(data.isChecked)
        listItem.setText(data.title)
        val keyListener: OnKeyListener = object : OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (!ReferenceApplication.worldHandler?.isEnableUserInteraction!!) {
                    return true
                }
                if (event!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        return if (ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                            listener.onAction(Action.LEFT,id)
                        } else {
                            listener.onAction(Action.RIGHT,id)
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return if (ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                            listener.onAction(Action.RIGHT,id)
                        } else {
                            listener.onAction(Action.LEFT,id)
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        return listener.onAction(Action.DOWN,id)
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        return listener.onAction(Action.UP,id)
                    }
                    if (keyCode == KeyEvent.KEYCODE_BACK){
                        downActionBackKeyDone = true
                    }
                }

                if (event.action == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (!downActionBackKeyDone)return true
                        downActionBackKeyDone = false
                        return listener.onAction(Action.BACK,id)
                    }
                    if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
                        data.compoundListener.onChange(true, data.id, id,
                            object : IAsyncDataCallback<Boolean> {
                                override fun onFailed(error: Error) {
                                    TODO("Not yet implemented")
                                }
                                override fun onReceive(data: Boolean) {
                                    changeSwitchStatus(true)
                                    this@PrefRadioViewHolder.data!!.isChecked = true
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

        itemView.setOnKeyListener(keyListener)
    }

    var infoListener: PreferenceSubMenuAdapter.Listener? = null

    fun setListener(listener: PreferenceSubMenuAdapter.Listener) {
        infoListener = listener
    }

    override fun notifyPreferenceUpdated() {
        changeSwitchStatus(this@PrefRadioViewHolder.data!!.isChecked)
    }

    override fun speakTextForFocusedView() {
        ttsSetterForSelectableViewInterface.setSpeechTextForSelectableView(
            listItem.getTextView().text.toString(),
            type = Type.CHECK,
            isChecked = isChecked
        )
    }

}