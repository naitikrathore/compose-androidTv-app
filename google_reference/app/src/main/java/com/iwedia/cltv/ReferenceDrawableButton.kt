package com.iwedia.cltv

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.iwedia.cltv.components.custom_view_base_classes.BaseLinearLayout
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.fast_backend_utils.UserProfileHelper
import java.util.*


class ReferenceDrawableButton : BaseLinearLayout {

    private var mContext: Context? = null

    private var textView: TextView? = null

    var drawableView: ImageView? = null

    private var buttonImageFocused: Int? = null

    private var buttonImageUnfocused: Int? = null

    private var isChecked: Boolean? = false

    private var keepFocus: Boolean? = false

    private var clickAnimationProgress = false

    private var onFocusChanged: (hasFocus: Boolean) -> Unit = {}

    //TODO remove this flag once Zap digit channels adapter implements Custom Button
    //this flag is used to disable animation because animation causes problems in zap digit channels adapter
    var shouldAnimate = true
    val TAG = javaClass.simpleName
    constructor(context: Context) : super(context) {
        mContext = context
        setup()
    }

    constructor(context: Context, focusedDrawable: Int, unfocusedDrawable: Int) : super(context) {
        mContext = context
        buttonImageFocused = focusedDrawable
        buttonImageUnfocused = unfocusedDrawable
        setup()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mContext = context
        setup()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        mContext = context
        setup()
    }


    @SuppressLint("ResourceType")
    private fun setup() {

        drawableView = ImageView(mContext)
        val layoutParams = LayoutParams(
            setDimension(R.dimen.custom_dim_20),
            setDimension(R.dimen.custom_dim_20)
        )
        if (buttonImageUnfocused != null) {
            drawableView!!.background =
                ContextCompat.getDrawable(context, buttonImageUnfocused!!)
            drawableView!!.background.setTint(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        } else {
            drawableView!!.visibility = GONE
        }


        var startMargin =
            if (buttonImageUnfocused == null) R.dimen.custom_dim_10 else R.dimen.custom_dim_12_5
        layoutParams.setMargins(
            setDimension(startMargin),
            setDimension(R.dimen.custom_dim_7),
            setDimension(R.dimen.custom_dim_10),
            setDimension(R.dimen.custom_dim_7)
        )

        //duo to RtL layouts we have to have start and end margins
        layoutParams.marginStart = setDimension(startMargin)
        layoutParams.marginEnd = setDimension(R.dimen.custom_dim_10)

        drawableView!!.layoutParams = layoutParams

        textView = TextView(mContext)
        textView!!.isAllCaps = false
        textView!!.includeFontPadding = false
        val startPadding =
            if (buttonImageUnfocused == null) R.dimen.custom_dim_15 else R.dimen.custom_dim_0
        textView!!.setPaddingRelative(
            setDimension(startPadding),
            setDimension(R.dimen.custom_dim_8),
            setDimension(R.dimen.custom_dim_15),
            setDimension(R.dimen.custom_dim_8)
        )

        textView!!.gravity = Gravity.CENTER
        textView!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )

        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_text_description"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setup: Exception color_context $color_context")
            textView!!.setTextColor(color_context)
        } catch(ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setup: Exception color rdb $ex")
        }

        background = ContextCompat.getDrawable(context, R.drawable.reference_button_non_focus_shape)

        background.setColorFilter(
            Color.parseColor(ConfigColorManager.getColor("color_not_selected")),
            PorterDuff.Mode.SRC_OVER
        )

        addView(drawableView)
        addView(textView)

        orientation = HORIZONTAL

        //Set default key listener
        super.setOnKeyListener(object: OnKeyListener{
            override fun onKey(p0: View?, keyCode: Int, keyEvent: KeyEvent): Boolean {
                return clickAnimationProgress
            }
        })
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        onFocusChange(focused)
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
    }

    @SuppressLint("ResourceType")
    fun onFocusChange(hasFocus: Boolean) {
        if (keepFocus!!) {
            return
        }
        if (hasFocus) {

            try {
                textToSpeechHandler.setSpeechText(textView!!.text.toString())
            } catch (e: NullPointerException) {
                e.printStackTrace()
                // Handle the case when textToSpeechTextSetterInterface is not implemented.
                // You can use setupTextToSpeechTextSetterInterface() method from the place where this error occurred.
                Log.d(Constants.LogTag.CLTV_TAG + UserProfileHelper.TAG, "Error: textToSpeechTextSetterInterface is not implemented. Make sure to call setupTextToSpeechTextSetterInterface() to set the interface.\"")
            }

            try {
                val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color_context $color_context")
                textView!!.setTextColor(color_context)
            } catch(ex: Exception) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color rdb $ex")
            }
            if (buttonImageFocused != null) {
                drawableView!!.background = ContextCompat.getDrawable(context, buttonImageFocused!!)
                drawableView!!.background.setTint(Color.parseColor(ConfigColorManager.getColor("color_background")))
            }
            background.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_selector")), PorterDuff.Mode.SRC_OVER)
        } else {
            try {
                val color_context = Color.parseColor(ConfigColorManager.getColor("color_text_description"))
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color_context $color_context")
                textView!!.setTextColor(color_context)
            } catch(ex: Exception) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color rdb $ex")
            }
            if (buttonImageUnfocused != null) {
                drawableView!!.background =
                    ContextCompat.getDrawable(context, buttonImageUnfocused!!)
                drawableView!!.background.setTint(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
            }
            background.setColorFilter(
                Color.parseColor(ConfigColorManager.getColor("color_not_selected")),
                PorterDuff.Mode.SRC_OVER
            )
        }
        onFocusChanged.invoke(hasFocus)
    }

    private fun setDimension(dimension: Int): Int {
        return context.resources.getDimensionPixelSize(dimension)
    }

    /**
     * used to set lambda function for OnFocusChangedListener.
     *
     * Passed lambda function will be executed every time when [ReferenceDrawableButton] changes focus.
     *
     * By default onFocusChanged variable, which is used to store lambda expression, is set to empty lambda - when [ReferenceDrawableButton] is focused it won't have any additional functionality beside automatic animation and border color changing.
     */
    fun setOnFocusChanged(onFocusChanged: (Boolean) -> Unit) {
        this.onFocusChanged = onFocusChanged
    }
    fun setText(text: String) {
        textView!!.text = text
    }

    fun setText(text: String, channelType: String?) {

        var spannable: SpannableString? = null
        var channelTypeData = ""
        if(channelType!!.lowercase().contains("analog")){
            channelTypeData = "[" + channelType.substring(0..("analog").length-1) + "]" + channelType.substring(6 until channelType.length)
            spannable = SpannableString("$text $channelTypeData")
        }
        else{
            spannable = SpannableString("$text $channelType")
        }

        spannable.setSpan(
            RelativeSizeSpan(0.8f),
            text.length + 1,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textView!!.text = spannable
    }

    fun setSpannableText(text: String) {

        var spannable: SpannableString? = null

        spannable = SpannableString(text)

        spannable.setSpan(
            RelativeSizeSpan(0.8f),
            0,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textView!!.text = spannable
    }

    fun setTextColor(color: Int) {
        textView!!.setTextColor(color)
    }

    fun setDrawable(drawable: Drawable?) {
        if (drawable == null) {
            drawableView!!.visibility = View.GONE
            textView!!.setPaddingRelative(
                setDimension(R.dimen.custom_dim_15),
                setDimension(R.dimen.custom_dim_8),
                setDimension(R.dimen.custom_dim_15),
                setDimension(R.dimen.custom_dim_8)
            )
            return
        }
        drawableView!!.visibility = View.VISIBLE
        textView!!.setPaddingRelative(
            setDimension(R.dimen.custom_dim_0),
            setDimension(R.dimen.custom_dim_8),
            setDimension(R.dimen.custom_dim_15),
            setDimension(R.dimen.custom_dim_8)
        )

        val params = LayoutParams(
            setDimension(R.dimen.custom_dim_20),
            setDimension(R.dimen.custom_dim_20)
        )

        params.setMargins(
            setDimension(R.dimen.custom_dim_12_5),
            setDimension(R.dimen.custom_dim_7),
            setDimension(R.dimen.custom_dim_10),
            setDimension(R.dimen.custom_dim_7)
        )
        drawableView!!.layoutParams = params

        drawableView!!.setImageDrawable(drawable)
    }

    fun getDrawable(): ImageView {
        return drawableView!!
    }

    fun setBackgroundColor(backgroundColor: Drawable) {
        background.setColorFilter(
            Color.parseColor(ConfigColorManager.getColor("color_not_selected")),
            PorterDuff.Mode.SRC_OVER
        )
    }

    fun setButtonImage(drawableFocused: Int, drawableUnfocused: Int) {
        buttonImageFocused = drawableFocused
        buttonImageUnfocused = drawableUnfocused
    }

    fun buttonTitle(): String {
        return textView!!.text as String
    }

    fun getTextView(): TextView {
        return textView!!
    }

    fun isChecked(): Boolean {
        return isChecked!!
    }

    fun setChecked() {
        isChecked = true
        setDrawable(ContextCompat.getDrawable(context, R.drawable.ic_checkbox))
    }

    fun setUnchecked() {
        isChecked = false
        setDrawable(ContextCompat.getDrawable(context, R.drawable.ic_field))
    }

    fun setKeepFocus(keepFocus: Boolean) {
        this.keepFocus = keepFocus
    }

    override fun onDetachedFromWindow() {
        mContext = null
        super.onDetachedFromWindow()
    }

    override fun setOnFocusChangeListener(onFocusChangeListener: OnFocusChangeListener?) {
        super.setOnFocusChangeListener(object : View.OnFocusChangeListener {
            override fun onFocusChange(view: View?, hasFocus: Boolean) {
                if(hasFocus) {
                    if(shouldAnimate) {
                        setAllParentsClip(view!!, false)
                        view.animate().scaleX(1.05f).scaleY(1.05f).setDuration(100).start();
                    }
                }
                else{
                    if(shouldAnimate) {
                        view!!.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                        view.clearAnimation()
                    }

                }
                onFocusChangeListener?.onFocusChange(view, hasFocus)
            }
        })
    }

    override fun setOnClickListener(clickListener: OnClickListener?) {
        super.setOnClickListener(object : OnClickListener {
            override fun onClick(view: View?) {
                clickAnimationProgress = true
                clickAnimation(view, clickListener)
            }
        })

    }

    override fun setOnKeyListener(onKeyListener: OnKeyListener?) {
        super.setOnKeyListener(object: OnKeyListener{
            override fun onKey(p0: View?, keyCode: Int, keyEvent: KeyEvent): Boolean {
                if (clickAnimationProgress){
                    return true
                } else {
                    return onKeyListener?.onKey(p0, keyCode, keyEvent) ?: false
                }
            }
        })
    }


    fun setAllParentsClip(view: View, enabled: Boolean) {
        var view = view
        while (view.parent != null && view.parent is ViewGroup) {
            val viewGroup = view.parent as ViewGroup
            viewGroup.clipChildren = enabled
            viewGroup.clipToPadding = enabled
            view = viewGroup
        }
    }

    fun clickAnimation(view: View?, clickListener: OnClickListener?) {
        view!!.clearAnimation()

        if(shouldAnimate){
            val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f)
            val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f)

            scaleDownY.repeatCount = 1
            scaleDownX.repeatCount = 1

            scaleDownY.repeatMode = ValueAnimator.REVERSE
            scaleDownX.repeatMode = ValueAnimator.REVERSE

            scaleDownX.duration = 200
            scaleDownY.duration = 200

            val scaleDownAnim = AnimatorSet()
            scaleDownAnim.play(scaleDownX).with(scaleDownY)
            scaleDownAnim.start()

            scaleDownAnim.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(scaleDown: Animator) {

                }

                override fun onAnimationEnd(scaleDown: Animator) {
                    if (clickListener != null) {
                        clickListener.onClick(view)
                        view!!.clearAnimation()
                        clickAnimationProgress = false
                    }
                }

                override fun onAnimationCancel(scaleDown: Animator) {
                    clickListener?.onClick(view)
                }

                override fun onAnimationRepeat(scaleDown: Animator) {
                    scaleDownAnim.play(scaleDownX).with(scaleDownY)
                }
            })
        }else{
            clickListener?.onClick(view)
        }
    }
}