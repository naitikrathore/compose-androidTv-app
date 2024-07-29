package com.iwedia.cltv.scene.parental_control

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.config.ConfigColorManager

/**
 * Enter pin view holder
 *
 * @author Aleksandar Lazic
 */
class EnterPinViewHolder<T : TextView>(view: View) : RecyclerView.ViewHolder(view) {

    var rootView: View? = null
    var pinTextViewWrapper: ConstraintLayout? = null
    var pinTextView: TextView? = null

    /**
     * Arrow up
     */
    var digitUp: TextView? = null

    /**
     * Arrow down
     */
    var digitDown: TextView? = null

    /**
     * Constructor
     *
     * @param itemView item view
     */
    init {
        rootView = itemView
        pinTextView = itemView.findViewById(R.id.pin_text_view)
        pinTextView!!.backgroundTintList= ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        pinTextViewWrapper = itemView.findViewById(R.id.value_wrapper)
        pinTextViewWrapper!!.backgroundTintList= ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_not_selected")))
        digitUp = itemView.findViewById(R.id.arrow_up)
        digitUp!!.backgroundTintList= ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        digitDown = itemView.findViewById(R.id.arrow_down)
        digitDown!!.backgroundTintList= ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        rootView!!.isFocusable = false
        rootView!!.isClickable = false
    }

}