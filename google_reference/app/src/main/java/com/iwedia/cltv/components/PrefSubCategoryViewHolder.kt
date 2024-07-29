package com.iwedia.cltv.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.downActionBackKeyDone
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.platform.`interface`.TTSSpeakTextForFocusedViewInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.utils.Utils

class PrefSubCategoryViewHolder(
    parent: ViewGroup,
    private val ttsSetterInterface: TTSSetterInterface
) : RecyclerView.ViewHolder (
    LayoutInflater.from(parent.context).inflate(R.layout.pref_sub_category, parent, false)
), PrefComponentInterface<RootItem>, TTSSpeakTextForFocusedViewInterface{

    //Root view
    var rootView: LinearLayout? = null

    //Category text
    var categoryText1: TextView? = null
    var categoryText2: TextView? = null
    var arrowRight: ImageView? = null

    val isCategoryFocus = false
    
    val textColor = Color.parseColor(ConfigColorManager.getColor("color_main_text"))
    val textColorFocused = Color.parseColor(ConfigColorManager.getColor("color_background"))
    val background = Color.parseColor((ConfigColorManager.getColor("color_main_text")).replace("#",ConfigColorManager.alfa_ten_per))
    private val backgroundFocused = Color.parseColor(ConfigColorManager.getColor("color_selector"))
    private val backgroundSelected = Color.parseColor((ConfigColorManager.getColor("color_main_text")).replace("#",ConfigColorManager.alfa_light))

    init {
        //Set references
        rootView = itemView.findViewById(R.id.channel_category_item_root_view)
        categoryText1 = itemView.findViewById(R.id.preference_category_title)
        categoryText2 = itemView.findViewById(R.id.preference_category_subtitle)
        arrowRight = itemView.findViewById(R.id.arrow_right)

        categoryText1!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )
        categoryText2!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_light")
        )
        categoryText1!!.isSingleLine = true
        categoryText2!!.isSingleLine = true

        rootView!!.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->

            categoryText1!!.ellipsize = if(hasFocus) TextUtils.TruncateAt.MARQUEE else TextUtils.TruncateAt.END
            categoryText2!!.ellipsize = if(hasFocus) TextUtils.TruncateAt.MARQUEE else TextUtils.TruncateAt.END

            categoryText1!!.isSelected = hasFocus
            categoryText2!!.isSelected = hasFocus

            if(hasFocus){
                setFocused()
                speakTextForFocusedView()
                Utils.focusAnimation(view!!)
                listener!!.onAction(Action.FOCUSED,id)
            }else{
                setBackground()
                Utils.unFocusAnimation(view!!)
            }
        }

        val keyListener: View.OnKeyListener = object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (!ReferenceApplication.worldHandler?.isEnableUserInteraction!!) {
                    return true
                }
                if (event!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        return if (ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                            listener!!.onAction(Action.LEFT,id)
                        } else {
                            listener!!.onAction(Action.RIGHT,id)
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return if (ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                            listener!!.onAction(Action.RIGHT,id)
                        } else {
                            listener!!.onAction(Action.LEFT,id)
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        return listener!!.onAction(Action.DOWN,id)
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        return listener!!.onAction(Action.UP,id)
                    }
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        downActionBackKeyDone = true
                    }
                    }

                if (event.action == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (!downActionBackKeyDone)return true
                        downActionBackKeyDone = false
                        return listener!!.onAction(Action.BACK,id)
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                        return listener!!.onAction(Action.CLICK,id)
                    }
                }
                return false
            }
        }

        itemView.setOnKeyListener(keyListener)

    }

    fun setSelected() {
        categoryText1!!.setTextColor(textColor)
        categoryText2!!.setTextColor(textColor)
        arrowRight!!.setColorFilter(
            textColor
        )
        val drawable = GradientDrawable()
        drawable.setColor(backgroundSelected)
        drawable.cornerRadius = 17f
        rootView!!.background = drawable
    }

    fun setFocused() {
        val drawable = GradientDrawable()
        drawable.setColor(backgroundFocused)
        drawable.cornerRadius = 17f
        
        rootView!!.background = drawable

        categoryText1!!.setTextColor(textColorFocused)
        categoryText2!!.setTextColor(textColorFocused)
        arrowRight!!.setColorFilter(textColorFocused)
    }


    private fun setBackground() {
        categoryText1!!.setTextColor(textColor)
        categoryText2!!.setTextColor(textColor)
        arrowRight!!.setColorFilter(textColor)

        val drawableChannelLogo = GradientDrawable()
        drawableChannelLogo.cornerRadius = 17F
        drawableChannelLogo.setColor(background)

        rootView!!.background = drawableChannelLogo
    }

    private fun getContext(): Context {
        return ReferenceApplication.applicationContext()
    }

    override fun requestFocus() {
        itemView!!.requestFocus()
    }

    override fun clearFocus() {
        itemView!!.clearFocus()
    }


    var categoryItem : RootItem? = null
    var listener: PrefItemListener? = null
    lateinit var id: Pref

    override fun refresh(data: RootItem, id:Pref, listener: PrefItemListener) {
        this.categoryItem = data
        this.listener = listener
        this.id = id

        //Set root view to be focusable and clickable
        categoryText1!!.text = categoryItem!!.name
        setInfo(categoryItem!!.info)
        arrowRight!!.visibility = if(categoryItem!!.showArrow ) View.VISIBLE else View.GONE
        setBackground()

        rootView!!.isClickable = data.isEnabled
        rootView!!.alpha = if(data.isEnabled )1.0f else 0.4f
        rootView!!.isFocusable = data.isEnabled
    }

    fun updateInfo(){
        if(categoryItem!!.infoListener!=null){
            setInfo(categoryItem!!.infoListener!!.getInfo())
        }
    }

    private fun setInfo(info: String?) {
        categoryItem!!.info = info

        if(categoryItem!!.info == null){
            categoryText2!!.visibility = View.GONE
        }else{
            categoryText2!!.visibility = View.VISIBLE
            categoryText2!!.text = info
        }
    }

    override fun notifyPreferenceUpdated() {
        setInfo(categoryItem!!.info)
    }

    override fun speakTextForFocusedView() {
        ttsSetterInterface.setSpeechText(
            categoryText1!!.text.toString(),
            categoryText2!!.text.toString()
        )
    }
}