package com.iwedia.cltv.components

import android.annotation.SuppressLint
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R

/**
 * Class PreferenceSubcategoryItemViewHolder
 *
 * @author Gaurav Jain
 */
class PreferenceSubcategoryItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    //Root view
    var rootView: LinearLayout? = null

    //Category text
    var categoryText1: TextView? = null
    var categoryText2: TextView? = null
    var arrowRight: ImageView? = null
    var mainLLSubOptions: LinearLayout? = null
    var llSwitchSubOptions: LinearLayout? = null
    var subOptionSwitchText: TextView ? = null
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    var subOptionSwitchButton: Switch? = null
    var subOptionRadioButtonGridView: VerticalGridView? =null

    init {

        //Set references
        rootView = view.findViewById(R.id.channel_category_item_root_view)
        categoryText1 = view.findViewById(R.id.preference_category_title)
        categoryText2 = view.findViewById(R.id.preference_category_subtitle)
        arrowRight = view.findViewById(R.id.arrow_right)
        mainLLSubOptions = view.findViewById(R.id.subOptionsMainLayout)

        llSwitchSubOptions = view.findViewById(R.id.ll_switch)

        subOptionSwitchText=view.findViewById(R.id.switchText)
        subOptionSwitchButton = view.findViewById(R.id.switchButton)
        subOptionRadioButtonGridView = view.findViewById(R.id.radio_options)

        //Set root view to be focusable and clickable
        rootView!!.focusable = View.FOCUSABLE
        rootView!!.isClickable = true


    }
}