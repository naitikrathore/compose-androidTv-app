package com.iwedia.cltv.scene.postal_code

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.config.ConfigColorManager

/**
 * Postal code view holder
 *
 * @author Tanvi Raut
 */
class PostalCodeViewHolder<T : TextView>(view: View) : RecyclerView.ViewHolder(view) {

    var rootView: View? = null
    var postalCodeTextViewWrapper: ConstraintLayout? = null
    var postalCodeTextView: TextView? = null

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
        postalCodeTextView = itemView.findViewById(R.id.pin_text_view)
        postalCodeTextView!!.backgroundTintList= ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        postalCodeTextViewWrapper = itemView.findViewById(R.id.value_wrapper)
        postalCodeTextViewWrapper!!.backgroundTintList= ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_not_selected")))
        digitUp = itemView.findViewById(R.id.arrow_up)
        digitUp!!.backgroundTintList= ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        digitDown = itemView.findViewById(R.id.arrow_down)
        digitDown!!.backgroundTintList= ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        rootView!!.isFocusable = false
        rootView!!.isClickable = false
    }

}