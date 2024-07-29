package com.iwedia.cltv.platform.rtk

import androidx.core.text.isDigitsOnly
import com.iwedia.cltv.platform.`interface`.PvrInterface
import com.iwedia.cltv.platform.`interface`.ScheduledInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.base.SearchInterfaceBaseImpl
import com.iwedia.cltv.platform.base.content_provider.TifEpgDataProvider
import com.iwedia.cltv.platform.`interface`.NetworkInterface
import com.iwedia.cltv.platform.rtk.provider.ChannelDataProvider
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import java.util.*

internal class SearchInterfaceImpl(
    private val channelProvider: ChannelDataProvider,
    private val epgDataProvider: TifEpgDataProvider,
    private val pvrInterface: PvrInterface,
    private val scheduledInterface : ScheduledInterface,
    private val utilsInterface: UtilsInterface,
    private val networkInterface: NetworkInterface
) : SearchInterfaceBaseImpl(channelProvider, epgDataProvider, pvrInterface, scheduledInterface, utilsInterface, networkInterface)

