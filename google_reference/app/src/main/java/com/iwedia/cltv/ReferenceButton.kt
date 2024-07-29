package com.iwedia.cltv

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager

class ReferenceButton : TextView {

    var mContext: Context? = null

    constructor(context: Context?) : super(context) {
        mContext = context
        setup()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        mContext = context

        setup()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        mContext = context
        setup()
    }


    private fun setup() {

        isAllCaps = false
        includeFontPadding = false
//        setPaddingRelative(
//            setDimension(R.dimen.custom_dim_15),
//            setDimension(R.dimen.custom_dim_7),
//            setDimension(R.dimen.custom_dim_15),
//            setDimension(R.dimen.custom_dim_7)
//        )

        text = "All"
        gravity = Gravity.CENTER
        typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_medium"))
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {

        if (focused) {

            typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_bold"))
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                getContext().resources.getDimensionPixelSize(R.dimen.font_15).toFloat()
            )

            setTextColor(Color.parseColor(ConfigColorManager.getColor("color_color_background")))
        } else {

            typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))

            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                getContext().resources.getDimensionPixelSize(R.dimen.font_15).toFloat()
            )
            setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
    }

    private fun setDimension(dimension: Int): Int {
        return context.resources.getDimensionPixelSize(dimension)
    }
}