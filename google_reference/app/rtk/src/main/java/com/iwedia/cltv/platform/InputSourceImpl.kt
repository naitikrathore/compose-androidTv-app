package com.iwedia.cltv.platform

import android.content.Context
import com.iwedia.cltv.platform.base.InputSourceBaseImpl
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.google.android.tv.inputplayer.api.LastUsedInput


internal class InputSourceImpl(
    applicationContext: Context,
    utilsModule: UtilsInterface,
    parentalControlSettingsInterface: ParentalControlSettingsInterface
) :
    InputSourceBaseImpl(applicationContext, utilsModule, parentalControlSettingsInterface) {

    override fun setLastUsedInput(inputId: String) {
        LastUsedInput.enterInput(context, "com.cltv.hybrid/com.iwedia.cltv.tis.main.AnokiTvInputService")
    }

    override fun exitTVInput(inputId: String) {
        LastUsedInput.exitInput(context, "com.cltv.hybrid/com.iwedia.cltv.tis.main.AnokiTvInputService")
    }
}