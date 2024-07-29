package com.iwedia.cltv.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import androidx.leanback.widget.HorizontalGridView
import com.iwedia.cltv.R
import com.iwedia.cltv.utils.Utils

/**
 * Adding a fading edge effect on the right & left side of the HorizontalGridView.
 */
class FadingEdgeHorizontalGridView : HorizontalGridView {
    private var leftFadingEdgePaint: Paint? = null
    private var rightFadingEdgePaint: Paint? = null
    private val fadingEdgeWidth =
        Utils.getDimensInPixelSize(R.dimen.custom_dim_50).toFloat() // Width of the fading edge

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    private fun init() {
        // Initialize the paint for the fading edge
        leftFadingEdgePaint = Paint()
        rightFadingEdgePaint = Paint()
        leftFadingEdgePaint!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        rightFadingEdgePaint!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

        leftFadingEdgePaint!!.shader = LinearGradient(
            0f,
            0f,
            fadingEdgeWidth,
            0f,
            Color.TRANSPARENT,
            Color.WHITE,
            Shader.TileMode.CLAMP
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rightFadingEdgePaint!!.shader = LinearGradient(
            (w - fadingEdgeWidth),
            0f,
            w.toFloat(),
            0f,
            Color.WHITE,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
    }

    override fun dispatchDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        val saveCount =
            canvas.saveLayer(0f, 0f, w, h, null, Canvas.ALL_SAVE_FLAG)
        super.dispatchDraw(canvas)

        // Draw the fading edge effect on the left side
        if (!Utils.containsView(this, layoutManager!!.findViewByPosition(0)))
            canvas.drawRect(
                0f,
                0f,
                fadingEdgeWidth,
                h,
                leftFadingEdgePaint!!
            )

        // Draw the fading edge effect on the right side
        if (selectedPosition != adapter!!.itemCount - 1)
            canvas.drawRect(
                w - fadingEdgeWidth,
                0f,
                w,
                h,
                rightFadingEdgePaint!!
            )

        canvas.restoreToCount(saveCount)
    }
}