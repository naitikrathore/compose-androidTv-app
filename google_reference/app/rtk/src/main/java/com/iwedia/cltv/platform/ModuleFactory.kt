package com.iwedia.cltv.platform

import android.app.Application
import android.media.tv.TvView
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.base.*
import com.iwedia.cltv.platform.base.content_provider.FastDataProvider
import com.iwedia.cltv.platform.base.content_provider.TifEpgDataProvider
import com.iwedia.cltv.platform.base.network.NetworkInterfaceBaseImpl
import com.iwedia.cltv.platform.base.text_to_speech.TTSInterfaceBaseImpl
import com.iwedia.cltv.platform.rtk.PlayerInterfaceImpl
import com.iwedia.cltv.platform.rtk.*
import com.iwedia.cltv.platform.rtk.CategoryInterfaceImpl
import com.iwedia.cltv.platform.rtk.parental.ParentalControlSettingsInterfaceImpl
import com.iwedia.cltv.platform.rtk.provider.ChannelDataProvider
import java.lang.ref.WeakReference

@RequiresApi(Build.VERSION_CODES.R)
class ModuleFactory(application: Application) : PlatformModuleFactory {
    private val appRef: WeakReference<Application> = WeakReference(application)
    private val channelDataProvider by lazy {
        ChannelDataProvider(appRef.get()!!.applicationContext)
    }
    private val epgDataProvider by lazy {
        TifEpgDataProvider(appRef.get()!!.applicationContext)
    }
    private val utilsInterfaceImpl by lazy {
        UtilsInterfaceImpl(appRef.get()!!.applicationContext, createTextToSpeechModule())
    }

    private val fastDataProvider by lazy {
        FastDataProvider(appRef.get()!!.applicationContext)
    }

    private val timeInterfaceImpl by lazy {
        TimeInterfaceImpl()
    }


    @RequiresApi(Build.VERSION_CODES.S)
    override fun refresh() {
        channelDataProvider.loadChannels()
        epgDataProvider.loadEvents()
        utilsInterfaceImpl.prepareRegion()
        fastDataProvider.init()
    }

    override fun initService(): TvView {
        TODO("Not yet implemented")
    }

    override fun createInteractiveAppModule(): InteractiveAppInterface {
        return InteractiveAppInterfaceBaseImpl(appRef.get()!!.applicationContext)
    }

    override fun createPreferenceModule(utilsInterface: UtilsInterface, generalConfigInterface: GeneralConfigInterface): PreferenceInterface {
        return PreferenceInterfaceImpl(utilsInterface, channelDataProvider, generalConfigInterface)
    }

    override fun createCategoryModule(tvInterface: TvInterface, favoritesInterface: FavoritesInterface, utilsModule: UtilsInterface): CategoryInterface {
        return CategoryInterfaceImpl(appRef.get()!!.applicationContext, tvInterface, favoritesInterface, utilsModule, fastDataProvider)
    }

    override fun createEpgModule(timeInterface: TimeInterface): EpgInterface {
        return EpgInterfaceImpl(appRef.get()!!.applicationContext, epgDataProvider,channelDataProvider, timeInterface)
    }

    override fun createFavoriteModule(utilsModule: UtilsInterface): FavoritesInterface {
        return FavoritesInterfaceBaseImpl(appRef.get()!!.applicationContext, channelDataProvider, utilsModule)
    }

    override fun createNetworkModule(): NetworkInterface {
        val cm = appRef.get()!!
            .getSystemService(ConnectivityManager::class.java)
        return NetworkInterfaceBaseImpl(cm)
    }

    override fun createPlatformOsModule(): PlatformOsInterface {
        return PlatformOsInterfaceImpl()
    }

    override fun createPlayerModule(
        utilsInterface: UtilsInterface,
        epgInterface: EpgInterface,
        parentalControlSettingsInterface: ParentalControlSettingsInterface
    ): PlayerInterface {
        return PlayerInterfaceImpl(appRef.get()!!.applicationContext, utilsInterface,epgInterface,parentalControlSettingsInterface, timeInterfaceImpl,
            createHbbTvModule(timeInterfaceImpl) as HbbTvInterfaceImpl, channelDataProvider
        )
    }

    override fun createPvrModule(
        epgInterface: EpgInterface,
        playerInterface: PlayerInterface,
        tvInterface: TvInterface,
        utilsInterface: UtilsInterface,
        timeInterface: TimeInterface
    ): PvrInterface {
        return PvrInterfaceImpl(
            epgInterface,
            playerInterface,
            tvInterface,
            utilsInterface,
            appRef.get()!!.applicationContext,
            timeInterface
        )
    }

    override fun createTimeshiftModule(playerInterface: PlayerInterface, utilsInterface: UtilsInterface): TimeshiftInterface {
        return TimeshiftInterfaceImpl(playerInterface,utilsInterface, appRef.get()!!.applicationContext)
    }

    override fun createSearchModule(
        pvrInterface: PvrInterface,
        scheduledInterface: ScheduledInterface,
        utilsInterface: UtilsInterface,
        networkInterface: NetworkInterface
    ): SearchInterface {
        return SearchInterfaceImpl(
            channelDataProvider,
            epgDataProvider,
            pvrInterface,
            scheduledInterface,
            utilsInterface,
            networkInterface
        )
    }

    override fun createSchedulerModule(
        utilsInterface: UtilsInterface,
        epgInterface: EpgInterface,
        watchlistInterface: WatchlistInterface,
        timeInterface: TimeInterface
    ): SchedulerInterface {
        return SchedulerInterfaceBaseImpl(
            utilsInterface,
            channelDataProvider,
            epgInterface,
            watchlistInterface,
            appRef.get()!!.applicationContext,
            timeInterface
        )
    }

    override fun createWatchlistModule(
        epgInterfaceImpl: EpgInterface,
        timeInterface: TimeInterface
    ): WatchlistInterface {
        return WatchlistBaseImpl(
            epgInterfaceImpl,
            channelDataProvider,
            appRef.get()!!.applicationContext,
            timeInterface
        )
    }

    override fun createForYouModule(
        epgInterface: EpgInterface,
        watchlistInterface: WatchlistInterface,
        pvrInterface: PvrInterface,
        utilsInterface: UtilsInterface,
        recommendationInterface: RecommendationInterface,
        schedulerInterface: SchedulerInterface
    ): ForYouInterface {
        return ForYouInterfaceBaseImpl(
            appRef.get()!!.applicationContext,
            epgInterface,
            channelDataProvider, watchlistInterface, pvrInterface, utilsInterface,recommendationInterface, schedulerInterface
        )
    }

    override fun createParentalControlSettingsModule(): ParentalControlSettingsInterface {
        return ParentalControlSettingsInterfaceImpl(appRef.get()!!.applicationContext, fastDataProvider,utilsInterfaceImpl)
    }

    override fun createPreferenceChannelsModule(tvInterface: TvInterface): PreferenceChannelsInterface {
        return PreferenceChannelsInterfaceImpl(appRef.get()!!.applicationContext, tvInterface)
    }

    override fun createTvModule(
        playerInterface: PlayerInterface,
        networkInterface: NetworkInterface,
        tvInputInterface: TvInputInterface,
        utilsInterface: UtilsInterface,
        epgInterface: EpgInterface,
        timeInterface: TimeInterface,
        parentalControlSettingsInterface: ParentalControlSettingsInterface
    ): TvInterface {
        return TvInterfaceImpl(
            playerInterface,
            networkInterface,
            channelDataProvider,
            tvInputInterface,
            utilsInterface,
            epgInterface,
            appRef.get()!!.applicationContext,
            timeInterface,
            parentalControlSettingsInterface
        )
    }

    override fun createGeneralConfigModule(utilsInterface: UtilsInterface): GeneralConfigInterface {
        return GeneralConfigInterfaceImpl(appRef.get()!!.applicationContext,utilsInterface)
    }

    override fun createUtilsModule(): UtilsInterface {
        return utilsInterfaceImpl
    }

    override fun createHbbTvModule(timeInterface: TimeInterface): HbbTvInterface {
        val hbbTvInterfaceImpl = HbbTvInterfaceImpl(appRef.get()?.applicationContext, utilsInterfaceImpl)
        utilsInterfaceImpl.hbbTvInterface = hbbTvInterfaceImpl
        return hbbTvInterfaceImpl
    }

    override fun createTTXModule(utilsInterface: UtilsInterface): TTXInterface {
        return TTXInterfaceImpl(utilsInterface)
    }

    override fun createScheduledModule(tvInterface: TvInterface): ScheduledInterface {
        return ScheduledInterfaceBaseImpl(
            tvInterface,
            appRef.get()!!.applicationContext,
            createTimeModule()
        )
    }

    override fun createTvInputModule(parentalControlSettingsInterface: ParentalControlSettingsInterface, utilsInterface: UtilsInterface): TvInputInterface {
        return TvInputInterfaceImpl(
            parentalControlSettingsInterface,
            utilsInterface,
            appRef.get()!!.applicationContext,
            fastDataProvider)
    }

    override fun createClosedCaptionModule(
        utilsInterface: UtilsInterface,
        playerInterface: PlayerInterface
    ): ClosedCaptionInterface {
        return ClosedCaptionInterfaceImpl(appRef.get()!!.applicationContext, utilsInterface, playerInterface)
    }

    override fun createCiPlusModule(playerInterface: PlayerInterface, tvInterface: TvInterface): CiPlusInterface {
        return CiPlusInterfaceImpl(appRef.get()!!.applicationContext, utilsInterfaceImpl, playerInterface, tvInterface)
    }

    override fun createInputModule(
        utilsModule: UtilsInterface,
        parentalControlSettingsInterface: ParentalControlSettingsInterface
    ): InputSourceInterface {
        return InputSourceImpl(
            appRef.get()!!.applicationContext,
            utilsModule,
            parentalControlSettingsInterface
        )
    }

    override fun createFactoryModeModule(utilsModule: UtilsInterface): FactoryModeInterface {
        return FactoryModeInterfaceIBasempl()
    }

    override fun createTimeModule(): TimeInterface {
        utilsInterfaceImpl.timeInterface = timeInterfaceImpl
        return timeInterfaceImpl
    }

    override fun createSubtitleModule(
        utilsInterface: UtilsInterface,
        playerInterface: PlayerInterface
    ): SubtitleInterface {
        return SubtitleInterfaceImpl(appRef.get()!!.applicationContext, utilsInterface, playerInterface)
    }

    override fun createPromotionModule(): PromotionInterface {
        return PromotionInterfaceBaseImpl(fastDataProvider)
    }

    override fun createRecommendationModule(): RecommendationInterface {
        return RecommendationInterfaceBaseImpl(fastDataProvider)
    }

    override fun createFastFavoritesModule(): FastFavoriteInterface {
        return FastFavoriteInterfaceBaseImpl(fastDataProvider)
    }

    override fun createFastUserSettingsModule(): FastUserSettingsInterface {
        return FastUserSettingsInterfaceBaseImpl(fastDataProvider)
    }

    override fun createOadUpdateModule(playerInterface: PlayerInterface): OadUpdateInterface {
        return OadUpdateInterfaceBaseImpl(playerInterface)
    }

    override fun createTextToSpeechModule(): TTSInterface {
        return TTSInterfaceBaseImpl(appRef.get()!!.applicationContext)
    }

    override fun toString() = "[RTK] ModuleFactory"
}
