package com.iwedia.cltv.platform.refplus5

import com.iwedia.cltv.platform.`interface`.PvrInterface
import com.iwedia.cltv.platform.`interface`.ScheduledInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.base.SearchInterfaceBaseImpl
import com.iwedia.cltv.platform.base.content_provider.TifEpgDataProvider
import com.iwedia.cltv.platform.`interface`.NetworkInterface
import com.iwedia.cltv.platform.refplus5.provider.ChannelDataProvider

internal class SearchInterfaceImpl(
    private val channelProvider: ChannelDataProvider,
    private val epgDataProvider: TifEpgDataProvider,
    private val pvrInterface: PvrInterface,
    private val scheduledInterface : ScheduledInterface,
    private val utilsInterface: UtilsInterface,
    private val networkInterface: NetworkInterface
) : SearchInterfaceBaseImpl(channelProvider, epgDataProvider, pvrInterface, scheduledInterface, utilsInterface, networkInterface)

