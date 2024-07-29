package com.iwedia.cltv.scene.fti.selectInput

import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R

/**
 * Select input item view holder
 *
 * @author Aleksandar Lazic
 */
class SelectInputItemViewHolder(view : View) : RecyclerView.ViewHolder(view){
    var rootView : ConstraintLayout? = null
    var itemTitle : TextView? = null

    init {
        rootView = view.findViewById(R.id.select_input_root_view)
        itemTitle = view.findViewById(R.id.inputTitle)

        rootView!!.focusable = View.FOCUSABLE
        rootView!!.isClickable = true
        rootView!!.requestFocus()
    }
}