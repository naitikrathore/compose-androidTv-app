package com.iwedia.cltv.platform.t56

import android.content.Context
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.`interface`.PvrInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.`interface`.WatchlistInterface
import com.iwedia.cltv.platform.base.ForYouInterfaceBaseImpl
import com.iwedia.cltv.platform.base.content_provider.TifChannelDataProvider
import com.iwedia.cltv.platform.`interface`.RecommendationInterface
import com.iwedia.cltv.platform.`interface`.SchedulerInterface


internal class ForYouInterfaceImpl(
    applicationContext: Context,
    epgInterfaceImpl: EpgInterface,
    channelDataProvider: TifChannelDataProvider,
    watchlistModule: WatchlistInterface,
    pvrModule: PvrInterface,
    utilsModule: UtilsInterface,
    recommendationInterface: RecommendationInterface,
    schedulerInterface: SchedulerInterface
) :
    ForYouInterfaceBaseImpl(applicationContext, epgInterfaceImpl, channelDataProvider,watchlistModule,pvrModule, utilsModule,recommendationInterface, schedulerInterface) {
}