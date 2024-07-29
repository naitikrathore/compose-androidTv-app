package com.iwedia.cltv.scene.home_scene.guideVertical

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager

/**
 * Vertical guide timeline view holder
 *
 * @author Thanvandh Natarajan
 */
class VerticalGuideTimelineViewHolder (view: View) : RecyclerView.ViewHolder(view) {

    //Root view
    var rootView: ConstraintLayout? = null

    //Time text view
    var timeTextView: TextView? = null

    init {
        //Set references
        rootView = view.findViewById(R.id.guide_timeline_root_view)
        timeTextView = view.findViewById(R.id.guide_timeline_text)
        timeTextView!!.setTextColor(Color.parseColor(ConfigColorManager.getColor(ConfigColorManager.getColor("color_text_description"), 0.8)))

        //Set typeface
        timeTextView!!.typeface =
            TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))

        rootView?.focusable = View.FOCUSABLE
        rootView!!.isClickable = true
        //rootView!!.requestFocus()
    }
}