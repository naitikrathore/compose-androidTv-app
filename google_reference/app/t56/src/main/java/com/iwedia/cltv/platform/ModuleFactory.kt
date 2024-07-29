package com.iwedia.cltv.platform

import android.app.Application
import android.media.tv.TvView
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.FastFavoriteInterfaceBaseImpl
import com.iwedia.cltv.platform.base.FastUserSettingsInterfaceBaseImpl
import com.iwedia.cltv.platform.base.FavoritesInterfaceBaseImpl
import com.iwedia.cltv.platform.base.GeneralConfigInterfaceBaseImpl
import com.iwedia.cltv.platform.base.InteractiveAppInterfaceBaseImpl
import com.iwedia.cltv.platform.base.OadUpdateInterfaceBaseImpl
import com.iwedia.cltv.platform.base.PromotionInterfaceBaseImpl
import com.iwedia.cltv.platform.base.RecommendationInterfaceBaseImpl
import com.iwedia.cltv.platform.base.ScheduledInterfaceBaseImpl
import com.iwedia.cltv.platform.base.SchedulerInterfaceBaseImpl
import com.iwedia.cltv.platform.base.TvInputInterfaceBaseImpl
import com.iwedia.cltv.platform.base.WatchlistBaseImpl
import com.iwedia.cltv.platform.base.content_provider.FastDataProvider
import com.iwedia.cltv.platform.base.content_provider.RecentlyProvider
import com.iwedia.cltv.platform.base.content_provider.TifEpgDataProvider
import com.iwedia.cltv.platform.base.network.NetworkInterfaceBaseImpl
import com.iwedia.cltv.platform.base.text_to_speech.TTSInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.CategoryInterface
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.`interface`.ClosedCaptionInterface
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.`interface`.FactoryModeInterface
import com.iwedia.cltv.platform.`interface`.FastFavoriteInterface
import com.iwedia.cltv.platform.`interface`.FastUserSettingsInterface
import com.iwedia.cltv.platform.`interface`.FavoritesInterface
import com.iwedia.cltv.platform.`interface`.ForYouInterface
import com.iwedia.cltv.platform.`interface`.GeneralConfigInterface
import com.iwedia.cltv.platform.`interface`.HbbTvInterface
import com.iwedia.cltv.platform.`interface`.InputSourceInterface
import com.iwedia.cltv.platform.`interface`.InteractiveAppInterface
import com.iwedia.cltv.platform.`interface`.NetworkInterface
import com.iwedia.cltv.platform.`interface`.OadUpdateInterface
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.PlatformModuleFactory
import com.iwedia.cltv.platform.`interface`.PlatformOsInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.PreferenceChannelsInterface
import com.iwedia.cltv.platform.`interface`.PreferenceInterface
import com.iwedia.cltv.platform.`interface`.PromotionInterface
import com.iwedia.cltv.platform.`interface`.PvrInterface
import com.iwedia.cltv.platform.`interface`.RecommendationInterface
import com.iwedia.cltv.platform.`interface`.ScheduledInterface
import com.iwedia.cltv.platform.`interface`.SchedulerInterface
import com.iwedia.cltv.platform.`interface`.SearchInterface
import com.iwedia.cltv.platform.`interface`.SubtitleInterface
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TTXInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.TimeshiftInterface
import com.iwedia.cltv.platform.`interface`.TvInputInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.`interface`.WatchlistInterface
import com.iwedia.cltv.platform.t56.CategoryInterfaceImpl
import com.iwedia.cltv.platform.t56.CiPlusInterfaceImpl
import com.iwedia.cltv.platform.t56.ClosedCaptionInterfaceImpl
import com.iwedia.cltv.platform.t56.EpgInterfaceImpl
import com.iwedia.cltv.platform.t56.FactoryModeInterfaceImpl
import com.iwedia.cltv.platform.t56.FavoritesInterfaceImpl
import com.iwedia.cltv.platform.t56.ForYouInterfaceImpl
import com.iwedia.cltv.platform.t56.HbbTvInterfaceImpl
import com.iwedia.cltv.platform.t56.PlatformOsInterfaceImpl
import com.iwedia.cltv.platform.t56.PlayerInterfaceImpl
import com.iwedia.cltv.platform.t56.PreferenceChannelsInterfaceImpl
import com.iwedia.cltv.platform.t56.PreferenceInterfaceImpl
import com.iwedia.cltv.platform.t56.PvrInterfaceImpl
import com.iwedia.cltv.platform.t56.SearchInterfaceImpl
import com.iwedia.cltv.platform.t56.SubtitleInterfaceImpl
import com.iwedia.cltv.platform.t56.TTXInterfaceImpl
import com.iwedia.cltv.platform.t56.TimeInterfaceImpl
import com.iwedia.cltv.platform.t56.TimeshiftInterfaceImpl
import com.iwedia.cltv.platform.t56.TvInterfaceImpl
import com.iwedia.cltv.platform.t56.UtilsInterfaceImpl
import com.iwedia.cltv.platform.t56.parental.ParentalControlSettingsInterfaceImpl
import com.iwedia.cltv.platform.t56.provider.ChannelDataProvider
import java.lang.ref.WeakReference

@RequiresApi(Build.VERSION_CODES.S)
class ModuleFactory(application: Application) : PlatformModuleFactory {

    private val appRef: WeakReference<Application> = WeakReference(application)
    private val channelDataProvider by lazy {
        ChannelDataProvider(appRef.get()!!.applicationContext)
    }
    private val textToSpeechInterface by lazy {
        TTSInterfaceBaseImpl(appRef.get()!!.applicationContext)
    }
    private val epgDataProvider by lazy {
        TifEpgDataProvider(appRef.get()!!.applicationContext)
    }
    private val recentlyProvider by lazy {
        RecentlyProvider(channelDataProvider)
    }
    private val utilsInterfaceImpl by lazy {
        UtilsInterfaceImpl(appRef.get()!!.applicationContext, textToSpeechInterface)
    }

    private val fastDataProvider by lazy {
        FastDataProvider(appRef.get()!!.applicationContext)
    }

    override fun refresh() {
        channelDataProvider.loadChannels()
        epgDataProvider.loadEvents()
        utilsInterfaceImpl.prepareRegion()
        fastDataProvider.init()
        createTimeModule()
    }

    override fun initService(): TvView {
        TODO("Not yet implemented")
    }

    override fun createInteractiveAppModule(): InteractiveAppInterface {
        return InteractiveAppInterfaceBaseImpl(appRef.get()!!.applicationContext)
    }

    override fun createCategoryModule(
        tvInterface: TvInterface,
        favoritesInterface: FavoritesInterface,
        utilsModule: UtilsInterface
    ): CategoryInterface {
        return CategoryInterfaceImpl(
            appRef.get()!!.applicationContext,
            tvInterface,
            favoritesInterface,
            utilsModule,
            fastDataProvider
        )
    }

    override fun createEpgModule(timeInterface: TimeInterface): EpgInterface {
        return EpgInterfaceImpl(
            appRef.get()?.applicationContext,
            epgDataProvider,
            channelDataProvider,
            timeInterface
        )
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
        return PlayerInterfaceImpl(
            appRef.get()!!.applicationContext,
            utilsInterface,
            epgInterface,
            parentalControlSettingsInterface
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

    override fun createTimeshiftModule(playerInterface: PlayerInterface, utilsInterface: UtilsInterface): TimeshiftInterface {
        return TimeshiftInterfaceImpl(playerInterface, utilsInterface, appRef.get()!!.applicationContext)
    }

    override fun createForYouModule(
        epgInterface: EpgInterface,
        watchlistInterface: WatchlistInterface,
        pvrInterface: PvrInterface,
        utilsInterface: UtilsInterface,
        recommendationInterface: RecommendationInterface,
        schedulerInterface: SchedulerInterface
    ): ForYouInterface {
        return ForYouInterfaceImpl(
            appRef.get()!!.applicationContext,
            epgInterface,
            channelDataProvider,
            watchlistInterface,
            pvrInterface,
            utilsInterface,
            recommendationInterface,
            schedulerInterface
        )
    }

    override fun createPreferenceModule(utilsInterface: UtilsInterface, generalConfigInterface: GeneralConfigInterface): PreferenceInterface {
        return PreferenceInterfaceImpl(utilsInterface, generalConfigInterface)
    }

    override fun createParentalControlSettingsModule(): ParentalControlSettingsInterface {
        return ParentalControlSettingsInterfaceImpl(appRef.get()!!.applicationContext, fastDataProvider)
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
        return GeneralConfigInterfaceBaseImpl(appRef.get()!!.applicationContext,utilsInterface)
    }

    override fun createUtilsModule(): UtilsInterface {
        return utilsInterfaceImpl
    }

    override fun createHbbTvModule(timeInterface: TimeInterface): HbbTvInterface {
        return HbbTvInterfaceImpl(appRef.get()?.applicationContext, timeInterface, utilsInterfaceImpl)
    }

    override fun createTTXModule(utilsInterface: UtilsInterface): TTXInterface {
        return TTXInterfaceImpl()
    }

    override fun createScheduledModule(tvInterface: TvInterface): ScheduledInterface {
        return ScheduledInterfaceBaseImpl(
            tvInterface,
            appRef.get()!!.applicationContext,
            createTimeModule()
        )
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

    override fun createPreferenceChannelsModule(tvInterface: TvInterface): PreferenceChannelsInterface {
        return PreferenceChannelsInterfaceImpl(appRef.get()!!.applicationContext, tvInterface)
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
        return FactoryModeInterfaceImpl(utilsModule)
    }

    override fun toString(): String {
        return "[T56] ModuleFactory"
    }

    override fun createClosedCaptionModule(
        utilsInterface: UtilsInterface,
        playerInterface: PlayerInterface
    ): ClosedCaptionInterface {
        return ClosedCaptionInterfaceImpl(
            appRef.get()!!.applicationContext,
            utilsInterface,
            playerInterface
        )
    }

    override fun createCiPlusModule(playerInterface: PlayerInterface, tvInterface: TvInterface): CiPlusInterface {
        return CiPlusInterfaceImpl(appRef.get()!!.applicationContext)
    }

    override fun createTimeModule(): TimeInterface {
        var timeInterface = TimeInterfaceImpl()
        utilsInterfaceImpl.timeInterface = timeInterface
        return timeInterface
    }

    override fun createSubtitleModule(
        utilsInterface: UtilsInterface,
        playerInterface: PlayerInterface
    ): SubtitleInterface {
        return SubtitleInterfaceImpl(
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

}