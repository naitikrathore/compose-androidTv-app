package com.iwedia.cltv.platform

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.media.tv.TvView
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.InputSourceBaseImpl
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface


internal class InputSourceImpl(
    val applicationContext: Context,
    utilsModule: UtilsInterface,
    parentalControlSettingsInterface: ParentalControlSettingsInterface
) :
    InputSourceBaseImpl(applicationContext, utilsModule, parentalControlSettingsInterface) {
}