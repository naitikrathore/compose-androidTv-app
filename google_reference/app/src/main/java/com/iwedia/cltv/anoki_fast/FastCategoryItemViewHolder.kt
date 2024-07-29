package com.iwedia.cltv.anoki_fast

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.components.CustomButton
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface

/**
 * Class ChannelListCategoryViewHolder
 *
 * @author Boris Tirkajla
 */
class FastCategoryItemViewHolder(
    view: LinearLayout,
    ttsSetterInterface: TTSSetterInterface
) : RecyclerView.ViewHolder(view) {

    val customButton: CustomButton

    init {

        // Initialize customButton with specified layout parameters
        customButton = CustomButton(context = view.context).apply {
            enableMarqueeEffect()
            setWidthToMatchParent()
            textToSpeechHandler.setupTextToSpeechTextSetterInterface(ttsSetterInterface)
        }

        // Set the layout parameters to ensure that the ViewHolder occupies the maximum available width.
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        // Add customButton to the view
        view.addView(customButton)    }

}