package com.iwedia.cltv.scene.channel_list

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceDrawableButton
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface

/**
 * ChannelListSortViewHolder
 *
 * @author Aleksandar Milojevic
 */
class ChannelListSortViewHolder(
    view: View,
    textToSpeechTextSetterListener: TTSSetterInterface
) : RecyclerView.ViewHolder(view) {

    //Root view
    var rootView: ConstraintLayout? = null

    //Sort item
    var sortItemDrawableButton: ReferenceDrawableButton? = null

    init {

        //Set references
        rootView = view.findViewById(R.id.sort_by_item_root_view)
        sortItemDrawableButton = view.findViewById(R.id.sort_item_drawable_button)

        //Set root view to be focusable and clickable
        rootView!!.focusable = View.FOCUSABLE
        rootView!!.isClickable = true

        sortItemDrawableButton!!.textToSpeechHandler.setupTextToSpeechTextSetterInterface(textToSpeechTextSetterListener)
    }

}