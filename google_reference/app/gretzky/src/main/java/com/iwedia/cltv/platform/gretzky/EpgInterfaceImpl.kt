package com.iwedia.cltv.platform.gretzky

import com.iwedia.cltv.platform.`interface`.EpgDataProviderInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.base.EpgInterfaceBaseImpl

internal class EpgInterfaceImpl constructor(epgDataProvider: EpgDataProviderInterface, timeInterface: TimeInterface) :
    com.iwedia.cltv.platform.base.EpgInterfaceBaseImpl(
        epgDataProvider,
        timeInterface
    ) {
}