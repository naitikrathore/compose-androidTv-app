package com.iwedia.cltv.platform

import android.app.Application
import android.media.tv.TvView
import android.net.ConnectivityManager
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.base.*
import com.iwedia.cltv.platform.base.content_provider.FastDataProvider
import com.iwedia.cltv.platform.base.content_provider.TifChannelDataProvider
import com.iwedia.cltv.platform.base.content_provider.TifEpgDataProvider
import com.iwedia.cltv.platform.base.network.NetworkInterfaceBaseImpl
import com.iwedia.cltv.platform.base.parental.ParentalControlSettingsInterfaceBaseImpl
import com.iwedia.cltv.platform.base.player.PlayerBaseImpl
import com.iwedia.cltv.platform.base.text_to_speech.TTSInterfaceBaseImpl
import java.lang.ref.WeakReference

class ModuleFactory(application: Application) : PlatformModuleFactory {
    private val appRef: WeakReference<Application> = WeakReference(application)
    private val textToSpeechInterface by lazy {
        TTSInterfaceBaseImpl(appRef.get()!!.applicationContext)
    }

    private val channelDataProvider by lazy {
        TifChannelDataProvider(appRef.get()!!.applicationContext)
    }
    private val epgDataProvider by lazy {
        TifEpgDataProvider(appRef.get()!!.applicationContext)
    }
    private val utilsInterfaceImpl by lazy {
        UtilsInterfaceBaseImpl(appRef.get()!!.applicationContext, textToSpeechInterface)
    }

    private val fastDataProvider by lazy {
        FastDataProvider(appRef.get()!!.applicationContext)
    }

    override fun refresh() {
        channelDataProvider.loadChannels()
        epgDataProvider.loadEvents()
        utilsInterfaceImpl.prepareRegion()
        fastDataProvider.init()
    }

    override fun createInteractiveAppModule(): InteractiveAppInterface {
        return InteractiveAppInterfaceBaseImpl(appRef.get()!!.applicationContext)
    }

    override fun createPreferenceModule(utilsInterface: UtilsInterface, generalConfigInterface: GeneralConfigInterface): PreferenceInterface {
        return PreferenceInterfaceBaseImpl(utilsInterface, generalConfigInterface)
    }

    override fun createClosedCaptionModule(
        utilsInterface: UtilsInterface,
        playerInterface: PlayerInterface
    ): ClosedCaptionInterface {
        return ClosedCaptionInterfaceBaseImpl(
            appRef.get()!!.applicationContext,
            utilsInterface,
            playerInterface
        )
    }

    override fun createCiPlusModule(playerInterface: PlayerInterface, tvInterface: TvInterface): CiPlusInterface {
        return CiPlusInterfaceBaseImpl()
    }

    override fun createCategoryModule(
        tvInterface: TvInterface,
        favoritesInterface: FavoritesInterface,
        utilsModule: UtilsInterface
    ): CategoryInterface {
        return CategoryInterfaceBaseImpl(
            appRef.get()!!.applicationContext,
            tvInterface,
            favoritesInterface,
            utilsModule,
            fastDataProvider
        )
    }

    override fun createEpgModule(timeInterface: TimeInterface): EpgInterface {
        return EpgInterfaceBaseImpl(epgDataProvider, timeInterface)
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
        return PlatformOsInterfaceBaseImpl()
    }

    override fun createPlayerModule(
        utilsInterface: UtilsInterface,
        epgInterface: EpgInterface,
        parentalControlSettingsInterface: ParentalControlSettingsInterface
    ): PlayerInterface {
        return PlayerBaseImpl(utilsInterface, parentalControlSettingsInterface)
    }

    override fun createPvrModule(
        epgInterface: EpgInterface,
        playerInterface: PlayerInterface,
        tvInterface: TvInterface,
        utilsInterface: UtilsInterface,
        timeInterface: TimeInterface
    ): PvrInterface {
        return PvrInterfaceBaseImpl(
            epgInterface,
            playerInterface,
            tvInterface,
            utilsInterface,
            appRef.get()!!.applicationContext,
            timeInterface
        )
    }

    override fun createTimeshiftModule(playerInterface: PlayerInterface, utilsModule: UtilsInterface): TimeshiftInterface {
        return TimeshiftInterfaceBaseImpl(playerInterface,utilsInterfaceImpl , appRef.get()!!.applicationContext)
    }

    override fun createSearchModule(
        pvrInterface: PvrInterface,
        scheduledInterface: ScheduledInterface,
        utilsInterface: UtilsInterface,
        networkInterface: NetworkInterface
    ): SearchInterface {
        return SearchInterfaceBaseImpl(
            channelDataProvider,
            epgDataProvider,
            pvrInterface,
            scheduledInterface,
            utilsInterface,
            networkInterface
        )
    }

    override fun createScheduledModule(tvInterface: TvInterface): ScheduledInterface {
        return ScheduledInterfaceBaseImpl(
            tvInterface,
            appRef.get()!!.applicationContext,
            createTimeModule()
        )
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
        return TvInterfaceBaseImpl(
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

    override fun createGeneralConfigModule(utilsInterface: UtilsInterface): GeneralConfigInterface {
        return GeneralConfigInterfaceBaseImpl(appRef.get()!!.applicationContext,utilsInterface)
    }

    override fun createUtilsModule(): UtilsInterface {
        return utilsInterfaceImpl
    }

    override fun createHbbTvModule(timeInterface: TimeInterface): HbbTvInterface {
        return HbbTvInterfaceBaseImpl(utilsInterfaceImpl)
    }

    override fun createTTXModule(utilsInterface: UtilsInterface): TTXInterface {
        return TTXInterfaceBaseImpl(utilsInterface)
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
            channelDataProvider,
            watchlistInterface,
            pvrInterface,
            utilsInterfaceImpl,
            recommendationInterface,
            schedulerInterface
        )
    }

    override fun initService(): TvView {
        TODO("Not yet implemented")
    }

    override fun createTvInputModule(
        parentalControlSettingsInterface: ParentalControlSettingsInterface,
        utilsInterface: UtilsInterface
    ): TvInputInterface {
        return TvInputInterfaceBaseImpl(
            parentalControlSettingsInterface,
            utilsInterface,
            appRef.get()!!.applicationContext
        )
    }

    override fun createParentalControlSettingsModule(): ParentalControlSettingsInterface {
        return ParentalControlSettingsInterfaceBaseImpl(appRef.get()!!.applicationContext, fastDataProvider)
    }

    override fun createPreferenceChannelsModule(tvInterface: TvInterface): PreferenceChannelsInterface {
        return PreferenceChannelsInterfaceBaseImpl(appRef.get()!!.applicationContext, tvInterface)
    }

    override fun createInputModule(
        utilsInterface: UtilsInterface,
        parentalControlSettingsInterface: ParentalControlSettingsInterface
    ): InputSourceInterface {
        return InputSourceBaseImpl(
            appRef.get()!!.applicationContext,
            utilsInterface,
            parentalControlSettingsInterface
        )
    }

    override fun createFactoryModeModule(utilsModule: UtilsInterface): FactoryModeInterface {
        return FactoryModeInterfaceIBasempl()
    }

    override fun createTimeModule(): TimeInterface {
        return TimeInterfaceBaseImpl()
    }

    override fun createSubtitleModule(
        utilsInterface: UtilsInterface,
        playerInterface: PlayerInterface
    ): SubtitleInterface {
        return SubtitleInterfaceBaseImpl(
            appRef.get()!!.applicationContext,
            utilsInterface,
            playerInterface
        )
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
        return textToSpeechInterface
    }

    override fun toString(): String {
        return "[BASE TIF] ModuleFactory"
    }
}