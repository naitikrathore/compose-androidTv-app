package com.iwedia.cltv.anoki_fast

import android.graphics.drawable.GradientDrawable
import android.view.View

class Utils {
    companion object{
        fun makeGradient(
            view: View,
            type: Int,
            orientation: GradientDrawable.Orientation?,
            listOfColor: IntArray
        ) {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.gradientType = type
            gradientDrawable.orientation = orientation
            gradientDrawable.colors = listOfColor
            view.background = gradientDrawable
        }

    }
}