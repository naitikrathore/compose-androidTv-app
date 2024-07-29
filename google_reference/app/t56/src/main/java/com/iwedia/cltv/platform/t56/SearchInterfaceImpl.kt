package com.iwedia.cltv.platform.t56

import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.base.SearchInterfaceBaseImpl

internal class SearchInterfaceImpl(
    channelProvider: ChannelDataProviderInterface,
    epgDataProvider: EpgDataProviderInterface,
    pvrInterface: PvrInterface,
    scheduledInterface: ScheduledInterface,
    utilsInterface: UtilsInterface,
    networkInterface: NetworkInterface
) : SearchInterfaceBaseImpl(
    channelProvider,
    epgDataProvider,
    pvrInterface,
    scheduledInterface,
    utilsInterface,
    networkInterface
)