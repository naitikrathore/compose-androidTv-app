package com.iwedia.cltv.platform.gretzky

import android.content.Context
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.base.ScheduledInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.TimeInterface

class ScheduledInterfaceImpl (
    private var tvInterfaceImpl: TvInterface,
    private var context: Context,
    timeInterface: TimeInterface
) : ScheduledInterfaceBaseImpl(
    tvInterfaceImpl,
    context,
    timeInterface
) {
}