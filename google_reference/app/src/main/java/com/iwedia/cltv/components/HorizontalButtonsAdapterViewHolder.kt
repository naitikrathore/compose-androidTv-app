package com.iwedia.cltv.components

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface

class HorizontalButtonsAdapterViewHolder(
    view: ConstraintLayout,
    preventClip: Boolean = false,
    ttsSetterInterface: TTSSetterInterface
) : RecyclerView.ViewHolder(view) {

    //CustomButton
    var customButton: CustomButton

    init {
        customButton = CustomButton(view.context, preventClip)
        customButton.textToSpeechHandler.setupTextToSpeechTextSetterInterface(ttsSetterInterface)
        view.addView(customButton)
    }
}
