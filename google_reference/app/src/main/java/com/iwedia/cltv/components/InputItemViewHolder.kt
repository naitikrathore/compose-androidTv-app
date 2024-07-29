package com.iwedia.cltv.components

import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R

class InputItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {



    //Category text
    var categoryText: TextView? = null
    var inputLayout: RelativeLayout? = null
    var inputImg: ImageView? = null
    var isLocked: TextView? = null
    var blockedInputLayout: RelativeLayout? =null
    var unlockText: TextView? =null
    var rightArrow: ImageView? = null
    var leftArrow: ImageView? = null


    init {
        inputLayout = view.findViewById(R.id.input_root_layout)
        categoryText = view.findViewById(R.id.input_sub_text)
        inputImg = view.findViewById(R.id.input_image)
        isLocked = view.findViewById(R.id.locked_input)
        blockedInputLayout = view.findViewById(R.id.blocked_inputs_id)
        unlockText = view.findViewById(R.id.unlock_text)
        rightArrow = view.findViewById(R.id.input_right_arrow)
        leftArrow = view.findViewById(R.id.input_left_arrow)

    }
}