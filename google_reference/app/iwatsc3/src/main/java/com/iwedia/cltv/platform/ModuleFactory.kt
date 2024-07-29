package com.iwedia.cltv.platform

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.tv.TvView
import android.net.ConnectivityManager
import android.os.IBinder
import com.iwedia.cltv.platform.base.CategoryInterfaceBaseImpl
import com.iwedia.cltv.platform.base.CiPlusInterfaceBaseImpl
import com.iwedia.cltv.platform.base.ClosedCaptionInterfaceBaseImpl
import com.iwedia.cltv.platform.base.EpgInterfaceBaseImpl
import com.iwedia.cltv.platform.base.FactoryModeInterfaceIBasempl
import com.iwedia.cltv.platform.base.FastFavoriteInterfaceBaseImpl
import com.iwedia.cltv.platform.base.FastUserSettingsInterfaceBaseImpl
import com.iwedia.cltv.platform.base.FavoritesInterfaceBaseImpl
import com.iwedia.cltv.platform.base.ForYouInterfaceBaseImpl
import com.iwedia.cltv.platform.base.GeneralConfigInterfaceBaseImpl
import com.iwedia.cltv.platform.base.HbbTvInterfaceBaseImpl
import com.iwedia.cltv.platform.base.InputSourceBaseImpl
import com.iwedia.cltv.platform.base.OadUpdateInterfaceBaseImpl
import com.iwedia.cltv.platform.base.PlatformOsInterfaceBaseImpl
import com.iwedia.cltv.platform.base.PreferenceChannelsInterfaceBaseImpl
import com.iwedia.cltv.platform.base.PreferenceInterfaceBaseImpl
import com.iwedia.cltv.platform.base.PromotionInterfaceBaseImpl
import com.iwedia.cltv.platform.base.PvrInterfaceBaseImpl
import com.iwedia.cltv.platform.base.RecommendationInterfaceBaseImpl
import com.iwedia.cltv.platform.base.ScheduledInterfaceBaseImpl
import com.iwedia.cltv.platform.base.SchedulerInterfaceBaseImpl
import com.iwedia.cltv.platform.base.SearchInterfaceBaseImpl
import com.iwedia.cltv.platform.base.SubtitleInterfaceBaseImpl
import com.iwedia.cltv.platform.base.TTXInterfaceBaseImpl
import com.iwedia.cltv.platform.base.text_to_speech.TTSInterfaceBaseImpl
import com.iwedia.cltv.platform.base.TimeInterfaceBaseImpl
import com.iwedia.cltv.platform.base.TimeshiftInterfaceBaseImpl
import com.iwedia.cltv.platform.base.TvInputInterfaceBaseImpl
import com.iwedia.cltv.platform.base.TvInterfaceBaseImpl
import com.iwedia.cltv.platform.base.UtilsInterfaceBaseImpl
import com.iwedia.cltv.platform.base.WatchlistBaseImpl
import com.iwedia.cltv.platform.base.content_provider.FastDataProvider
import com.iwedia.cltv.platform.base.content_provider.TifChannelDataProvider
import com.iwedia.cltv.platform.base.content_provider.TifEpgDataProvider
import com.iwedia.cltv.platform.base.network.NetworkInterfaceBaseImpl
import com.iwedia.cltv.platform.base.parental.ParentalControlSettingsInterfaceBaseImpl
import com.iwedia.cltv.platform.base.player.PlayerBaseImpl
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
import com.iwedia.cltv.platform.`interface`.TTXInterface
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.TimeshiftInterface
import com.iwedia.cltv.platform.`interface`.TvInputInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.`interface`.WatchlistInterface
import com.iwedia.cltv.platform.iwatsc3.InteractiveAppInterfaceImpl
import com.mediatek.dtv.tvinput.atsc3tuner.common.IIwediaTis
import java.lang.ref.WeakReference

class ModuleFactory(application: Application) : PlatformModuleFactory {

    private val appRef: WeakReference<Application> = WeakReference(application)
    private var context: Context? = null

    private var mService: IIwediaTis? = null
    private var interactiveAppInterface : InteractiveAppInterface? = null

    private var atsc3Supported : Boolean = false

    init {
        val mServiceConnection: ServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                mService = IIwediaTis.Stub.asInterface(binder)

                atsc3Supported = mService!!.isAtsc3Supported()

                (createInteractiveAppModule() as InteractiveAppInterfaceImpl).setService(mService!!)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                mService = null
            }
        }

        val intent = Intent()
        intent.setComponent(
            ComponentName(
                "com.iwedia.tvinput",
                "com.iwedia.tvinput.Atsc3SupportTIS"
            )
        )

        val context = appRef.get()!!.applicationContext
        context!!.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private val textToSpeechInterface by lazy {
        TTSInterfaceBaseImpl(appRef.get()!!.applicationContext)
    }

    //private val appRef: WeakReference<Application> = WeakReference(application)
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

    override fun initService(): TvView {
        TODO("Not yet implemented")
    }

    override fun createInteractiveAppModule() : InteractiveAppInterface {
        if(interactiveAppInterface == null) {
            interactiveAppInterface = InteractiveAppInterfaceImpl(appRef.get()!!.applicationContext)
        }
        return interactiveAppInterface!!
    }

    override fun createPreferenceModule(utilsInterface: UtilsInterface, generalConfigInterface: GeneralConfigInterface): PreferenceInterface {
        return PreferenceInterfaceBaseImpl(utilsInterface,generalConfigInterface)
    }

    override fun createClosedCaptionModule(utilsInterface: UtilsInterface, playerInterface: PlayerInterface) : ClosedCaptionInterface {
        return ClosedCaptionInterfaceBaseImpl(
            appRef.get()!!.applicationContext,
            utilsInterface,
            playerInterface
        )
    }

    override fun createCiPlusModule(playerInterface: PlayerInterface, tvInterface: TvInterface): CiPlusInterface {
        return CiPlusInterfaceBaseImpl()
    }

    override fun createCategoryModule(tvInterface: TvInterface, favoritesInterface: FavoritesInterface, utilsModule: UtilsInterface): CategoryInterface {
        return CategoryInterfaceBaseImpl(appRef.get()!!.applicationContext, tvInterface, favoritesInterface, utilsModule, fastDataProvider)
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

    override fun createPlayerModule(utilsInterface: UtilsInterface, epgInterface: EpgInterface,parentalControlSettingsInterface:ParentalControlSettingsInterface): PlayerInterface {
        return PlayerBaseImpl(utilsInterface,parentalControlSettingsInterface)
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

    override fun createTimeshiftModule(playerInterface: PlayerInterface, utilsInterface: UtilsInterface): TimeshiftInterface {
        return TimeshiftInterfaceBaseImpl(playerInterface, utilsInterface , appRef.get()!!.applicationContext)
    }

    override fun createScheduledModule(tvInterface: TvInterface): ScheduledInterface {
        return ScheduledInterfaceBaseImpl(
            tvInterface,
            appRef.get()!!.applicationContext,
            createTimeModule()
        )
    }

    override fun createTvModule(playerInterface: PlayerInterface, networkInterface: NetworkInterface, tvInputInterface: TvInputInterface, utilsInterface: UtilsInterface, epgInterface: EpgInterface, timeInterface: TimeInterface, parentalControlSettingsInterface: ParentalControlSettingsInterface): TvInterface {
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

    override fun createGeneralConfigModule(utilsInterface: UtilsInterface): GeneralConfigInterface {
        return GeneralConfigInterfaceBaseImpl(appRef.get()!!.applicationContext,utilsInterface)
    }

    override fun createSchedulerModule(utilsInterface: UtilsInterface, epgInterface: EpgInterface, watchlistInterface: WatchlistInterface, timeInterface: TimeInterface): SchedulerInterface {
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
            channelDataProvider, watchlistInterface, pvrInterface, utilsInterfaceImpl, recommendationInterface, schedulerInterface
        )
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

    override fun createTvInputModule(parentalControlSettingsInterface: ParentalControlSettingsInterface, utilsInterface: UtilsInterface): TvInputInterface {
        return TvInputInterfaceBaseImpl(
            parentalControlSettingsInterface,
            utilsInterface,
            appRef.get()!!.applicationContext)
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
        return InputSourceBaseImpl(appRef.get()!!.applicationContext,utilsInterface, parentalControlSettingsInterface)
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
        return SubtitleInterfaceBaseImpl(appRef.get()!!.applicationContext, utilsInterface, playerInterface)
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
        return "[iwatsc3] ModuleFactory"
    }
}