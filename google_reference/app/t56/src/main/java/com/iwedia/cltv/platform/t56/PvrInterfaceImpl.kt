package com.iwedia.cltv.platform.t56

import android.content.Context
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.base.PvrInterfaceBaseImpl

internal class PvrInterfaceImpl(
    private var epgInterfaceImpl: EpgInterface,
    private var playerInterface: PlayerInterface,
    private var tvInterfaceImpl: TvInterface,
    private var utilsInterface: UtilsInterface,
    private var context: Context,
    timeInterface: TimeInterface
) : PvrInterfaceBaseImpl(
    epgInterfaceImpl,
    playerInterface,
    tvInterfaceImpl,
    utilsInterface,
    context,
    timeInterface
) {
}