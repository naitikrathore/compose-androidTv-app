package com.iwedia.cltv.anoki_fast

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.components.ButtonType
import com.iwedia.cltv.components.CustomButton
import com.iwedia.cltv.components.custom_view_base_classes.BaseConstraintLayout
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.utils.Utils

/**
 * Fast Tos More Info Layout
 *
 * Used in Discover and Live tabs in case that TOS is not accepted on appication start
 * @author Dejan Nadj
 */
class FastTosMoreInfo: BaseConstraintLayout {
    private var listener: FastTosMoreInfoListener?= null
    private var moreInfoButton : CustomButton? =null
    private var isButtonClicked = false
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
        LayoutInflater.from(context).inflate(R.layout.layout_fast_tos_more_info, this, true)

        findViewById<TextView>(R.id.message).apply {
            typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), "work_sans_regular.ttf")
            setTextColor(Color.parseColor("#f8f9fa"))
            text = ConfigStringsManager.getStringById("tos_more_info_text")
        }

        moreInfoButton = findViewById(R.id.more_info_btn)
        moreInfoButton?.update(ButtonType.CUSTOM_TEXT, ConfigStringsManager.getStringById("enable_streaming_tv"))
        moreInfoButton?.setOnClickListener {
            Utils.viewClickAnimation(it, object :
                com.iwedia.cltv.utils.AnimationListener {
                override fun onAnimationEnd() {
                    isButtonClicked = true
                    listener?.showTos()
                    it.clearAnimation()
                }
            })
        }

        moreInfoButton?.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (event!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        listener?.requestFocusOnRail()
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        listener?.requestFocusOnTopMenu()
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        isButtonClicked = false
                    }
                } else if (event!!.action == KeyEvent.ACTION_UP) {
                    //To block the focus going to broadcast tab when returned from channel scan scene
                    if (keyCode == KeyEvent.KEYCODE_BACK && isButtonClicked) {
                        return true
                    }
                }
                return false
            }
        })
    }

    fun setFocusToMoreInfoButton() {
        moreInfoButton?.requestFocus()
    }

    fun setListener(moreInfoListener: FastTosMoreInfoListener) {
        this.listener = moreInfoListener
    }
}

interface FastTosMoreInfoListener {
    fun showTos()
    fun requestFocusOnTopMenu()
    fun requestFocusOnRail()
}