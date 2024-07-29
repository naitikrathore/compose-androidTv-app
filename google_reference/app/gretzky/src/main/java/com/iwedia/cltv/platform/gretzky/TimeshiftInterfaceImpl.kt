package com.iwedia.cltv.platform.gretzky

import android.content.Context
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.base.TimeshiftInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.UtilsInterface

internal class TimeshiftInterfaceImpl(private var playerInterface: PlayerInterface, private var  utilsInterface: UtilsInterface, private var context: Context): TimeshiftInterfaceBaseImpl(playerInterface, utilsInterface, context){}