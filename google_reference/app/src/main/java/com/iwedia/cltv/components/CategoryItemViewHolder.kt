package com.iwedia.cltv.components

import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R

/**
 * Class ChannelListCategoryViewHolder
 *
 * @author Aleksandar Milojevic
 */
class CategoryItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    //Root view
    var rootView: RelativeLayout? = null

    //Category text
    var categoryText: TextView? = null

    init {

        //Set references
        rootView = view.findViewById(R.id.channel_category_item_root_view)
        categoryText = view.findViewById(R.id.channel_category_item_category_text)

        //Set root view to be focusable and clickable
        rootView!!.focusable = View.FOCUSABLE
        rootView!!.isClickable = true
    }



    /**
     * Retrieves the text content intended for Text-to-Speech (TTS) when this custom view is in focus.
     *
     * The purpose of this method is to provide the text that should be read aloud using Text-to-Speech
     * functionality when the associated custom view is focused. The text is typically associated
     * with the current state or content of the view, and it can be dynamically updated as needed.
     *
     * @return The text content for Text-to-Speech when this view is focused.
     */
    fun getSpeechText() = categoryText!!.text.toString()
}