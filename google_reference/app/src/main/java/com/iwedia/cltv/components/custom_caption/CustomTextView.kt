package com.iwedia.cltv.components.custom_caption

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
class CustomTextView(context: Context, attributeSet: AttributeSet) :
    androidx.appcompat.widget.AppCompatTextView(context, attributeSet) {
    override fun onDraw(canvas: Canvas) {
        val p: Paint = paint
        p.style = Paint.Style.FILL
        super.onDraw(canvas)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        super.onDraw(canvas)
    }
}
