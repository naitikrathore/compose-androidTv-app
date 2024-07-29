package com.iwedia.cltv.platform.gretzky

import android.content.Context
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.base.PvrInterfaceBaseImpl
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.recording.ScheduledRecording

internal class PvrInterfaceImpl(
    private var epgInterfaceImpl: EpgInterface,
    private var playerInterface: PlayerInterface,
    private var tvInterfaceImpl: TvInterface,
    private var utilsInterface: UtilsInterface,
    private var context: Context,
    private var timeInterface: TimeInterface
) : PvrInterfaceBaseImpl(
    epgInterfaceImpl,
    playerInterface,
    tvInterfaceImpl,
    utilsInterface,
    context,
    timeInterface
) {
}