package com.iwedia.cltv.platform.gretzky

import android.content.Context
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.base.TvInterfaceBaseImpl

internal class TvInterfaceImpl (
    playerInterface: PlayerInterface,
    networkInterface: NetworkInterface,
    dataProvider: ChannelDataProviderInterface,
    tvInputInterface: TvInputInterface,
    utilsInterface: UtilsInterface,
    epgInterface: EpgInterface,
    context: Context,
    timeInterface: TimeInterface,
    parentalControlSettingsInterface: ParentalControlSettingsInterface
) : TvInterfaceBaseImpl(playerInterface, networkInterface, dataProvider, tvInputInterface, utilsInterface, epgInterface, context, timeInterface, parentalControlSettingsInterface) {
}