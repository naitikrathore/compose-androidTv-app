package com.iwedia.cltv.platform.gretzky

import android.content.Context
import com.iwedia.cltv.platform.base.InputSourceBaseImpl
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface


internal class InputSourceImpl(
    applicationContext: Context,
    utilsModule: UtilsInterface,
    parentalControlSettingsInterface: ParentalControlSettingsInterface
) :
    InputSourceBaseImpl(applicationContext,utilsModule, parentalControlSettingsInterface) {
}