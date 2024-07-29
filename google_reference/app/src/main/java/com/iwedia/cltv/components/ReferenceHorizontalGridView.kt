package com.iwedia.cltv.components

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import androidx.leanback.widget.HorizontalGridView
import android.widget.LinearLayout

/**
 * Reference horizontal grid view
 *
 * @author Dejan Nadj
 */
class ReferenceHorizontalGridView constructor(context: Context, attrs: AttributeSet):
    HorizontalGridView(context, attrs) {

    var focusSearchListener: FocusSearchListener ?= null
    var focusPosition = 0
    override fun focusSearch(focused: View?, direction: Int): View {
        var next = focusPosition
        next = if (direction == FOCUS_RIGHT) {
            if (focusPosition + 1 < layoutManager!!.itemCount) focusPosition + 1 else focusPosition
        } else if (direction == FOCUS_LEFT) {
            if (focusPosition - 1 < 0) focusPosition else focusPosition - 1
        } else {
            focusPosition
        }
        focusPosition = next

        val retValue = if (layoutManager!!.findViewByPosition(next) != null) {
            layoutManager!!.findViewByPosition(next)!!
        } else {
            super.focusSearch(focused, direction)
        }

        if (focusSearchListener != null) {
            focusSearchListener?.onFocusSearch(focusPosition)
        }
        return retValue
    }

    override fun requestChildFocus(child: View?, focused: View?) {
        val position = layoutManager!!.getPosition(child!!)
        if (focusPosition == 0) {
            super.requestChildFocus(child, focused)
        } else if (focusPosition == position) {
            super.requestChildFocus(child, focused)
        }
    }

    /**
     * Focus search listener
     */
    interface FocusSearchListener {
        fun onFocusSearch(position: Int)
    }
}