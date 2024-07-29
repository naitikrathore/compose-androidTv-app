package com.iwedia.cltv.platform

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.media.tv.TvView
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.cltv.mal.IServiceAPI
import com.cltv.mal.model.entities.ServiceTvView
import com.cltv.mal.model.entities.TvChannel
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
import com.iwedia.cltv.platform.mal_service.CategoryInterfaceImpl
import com.iwedia.cltv.platform.mal_service.CiPlusInterfaceImpl
import com.iwedia.cltv.platform.mal_service.ClosedCaptionInterfaceImpl
import com.iwedia.cltv.platform.mal_service.FactoryModeInterfaceImpl
import com.iwedia.cltv.platform.mal_service.FastFavoriteInterfaceImpl
import com.iwedia.cltv.platform.mal_service.FastUserSettingsInterfaceImpl
import com.iwedia.cltv.platform.mal_service.FavoritesInterfaceImpl
import com.iwedia.cltv.platform.mal_service.GeneralConfigInterfaceImpl
import com.iwedia.cltv.platform.mal_service.HbbTvInterfaceImpl
import com.iwedia.cltv.platform.mal_service.InputSourceInterfaceImpl
import com.iwedia.cltv.platform.mal_service.InteractiveAppInterfaceImpl
import com.iwedia.cltv.platform.mal_service.OadUpdateInterfaceImpl
import com.iwedia.cltv.platform.mal_service.ParentalControlSettingsInterfaceImpl
import com.iwedia.cltv.platform.mal_service.PlatformOsInterfaceImpl
import com.iwedia.cltv.platform.mal_service.PreferenceChannelsInterfaceImpl
import com.iwedia.cltv.platform.mal_service.PreferenceInterfaceImpl
import com.iwedia.cltv.platform.mal_service.PromotionInterfaceImpl
import com.iwedia.cltv.platform.mal_service.PvrInterfaceImpl
import com.iwedia.cltv.platform.mal_service.RecommendationInterfaceImpl
import com.iwedia.cltv.platform.mal_service.ScheduledInterfaceImpl
import com.iwedia.cltv.platform.mal_service.SchedulerInterfaceImpl
import com.iwedia.cltv.platform.mal_service.SearchInterfaceImpl
import com.iwedia.cltv.platform.mal_service.SubtitleInterfaceImpl
import com.iwedia.cltv.platform.mal_service.TeletextInterfaceImpl
import com.iwedia.cltv.platform.mal_service.TimeInterfaceImpl
import com.iwedia.cltv.platform.mal_service.TimeshiftInterfaceImpl
import com.iwedia.cltv.platform.mal_service.TvInputInterfaceImpl
import com.iwedia.cltv.platform.mal_service.TvInterfaceImpl
import com.iwedia.cltv.platform.mal_service.UtilsInterfaceImpl
import com.iwedia.cltv.platform.mal_service.WatchlistInterfaceImpl
import com.iwedia.cltv.platform.mal_service.common.ForYouInterfaceImpl
import com.iwedia.cltv.platform.mal_service.epg.EpgDataProvider
import com.iwedia.cltv.platform.mal_service.epg.EpgInterfaceBaseImpl
import com.iwedia.cltv.platform.mal_service.fromServiceChannel
import com.iwedia.cltv.platform.mal_service.fromServiceScheduledReminder
import com.iwedia.cltv.platform.mal_service.network.NetworkInterfaceImpl
import com.iwedia.cltv.platform.mal_service.player.PlayerInterfaceImpl
import com.iwedia.cltv.platform.mal_service.text_to_speech.TTSInterfaceBaseImpl
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("UnspecifiedRegisterReceiverFlag")
class ModuleFactory(application: Application) : PlatformModuleFactory {

    companion object {
        var context: Context? = null
    }

    private val appRef: WeakReference<Application> = WeakReference(application)

    private val utilsInterfaceImpl by lazy {
        UtilsInterfaceImpl(appRef.get()!!.applicationContext, serviceImpl!!)
    }

    private val hbbTvInterfaceImpl by lazy {
        HbbTvInterfaceImpl(serviceImpl!!)
    }

    private lateinit var playerInterfaceImpl: PlayerInterface
    private lateinit var tvInterfaceImpl: TvInterface

    private val TAG = "ServiceMalModuleFactory"
    private lateinit var liveTvView: ServiceTvView
    var serviceImpl: IServiceAPI? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "MAL Service connected")
            serviceImpl = IServiceAPI.Stub.asInterface(service)
            InformationBus.serviceConnectionCallback.invoke()
        }

        override fun onServiceDisconnected(
            className: ComponentName
        ) {
            serviceImpl = null
        }
    }

    /**
     * Scan broadcast receiver
     */
    private val serviceReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onReceive(context: Context, intent: Intent) {

            if (intent != null && intent.action == "ServiceIntent") {

                var intentId = intent.getIntExtra("intentId", 0)
                if (InformationBus.isListenerInitialized()) {
                    if (intentId == Events.TIME_CHANGED) {
                        val time = System.currentTimeMillis()
                        InformationBus.informationBusEventListener.submitEvent(
                            intentId,
                            arrayListOf(time)
                        )
                    } else if (intentId == Events.PLAYBACK_STOPPED) {
                        playerInterfaceImpl.stop()
                    } else if (intentId == Events.CHANNEL_CHANGED) {
                        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra("intentData", TvChannel::class.java)
                        } else {
                            intent.getParcelableExtra<TvChannel>("intentData")
                        }
                        if (channel != null) {
                            var tvChannel = fromServiceChannel(channel)
                            if (::playerInterfaceImpl.isInitialized) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    playerInterfaceImpl.play(tvChannel)
                                }
                            }
                        } else {
                            if (serviceImpl != null) {
                                val applicationMode = serviceImpl!!.applicationMode
                                val channel = fromServiceChannel(
                                    serviceImpl!!.getActiveChannel(applicationMode)
                                )
                                if (::playerInterfaceImpl.isInitialized) {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        playerInterfaceImpl.play(channel)
                                    }
                                }
                            }
                        }
                    } else if (intentId == Events.FAVORITE_LIST_UPDATED) {
                        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra("intentData", ArrayList::class.java)
                        } else {
                            intent.getParcelableExtra("intentData")
                        }
                        if (data != null) {
                            var arrayList = arrayListOf<Any>()
                            arrayList.addAll(data)
                            InformationBus.informationBusEventListener.submitEvent(
                                intentId, arrayList
                            )
                        }
                    } else if (intentId == Events.SCHEDULED_REMINDER_NOTIFICATION) {
                        val data = intent.getParcelableExtra(
                            "intentData",
                            com.cltv.mal.model.entities.ScheduledReminder::class.java
                        )
                        if (data != null) {
                            InformationBus.informationBusEventListener.submitEvent(
                                intentId, arrayListOf(
                                    fromServiceScheduledReminder(data)
                                )
                            )
                        }
                    } else if (intentId == Events.MAL_SERVICE_HBB_TV_SET_SURFACE) {
                        val data =
                            intent.getParcelableExtra<android.view.SurfaceControlViewHost.SurfacePackage>(
                                "intentData"
                            )
                        hbbTvInterfaceImpl.onSetSurface(data as android.view.SurfaceControlViewHost.SurfacePackage)
                    } else if (intentId == Events.MAL_SERVICE_HBB_TV_REGION_CHANGE) {
                        val data = intent.getDoubleArrayExtra(
                            "intentData"
                        )
                        if (data != null && data.size == 4) {
                            hbbTvInterfaceImpl.setTvViewPosition(data[0], data[1], data[2], data[3])
                        }
                    } else if (intentId == Events.MAL_SERVICE_HBB_TV_VISIBILITY_CHANGE) {
                        val data = intent.getBooleanExtra(
                            "intentData",
                            false
                        )
                        if (data != null) {
                            var visibility = if (data) 1 else 0
                            hbbTvInterfaceImpl.onVisibilityChange(visibility)
                        }
                    } else {
                        InformationBus.informationBusEventListener.submitEvent(intentId)
                    }
                }
            }
        }
    }

    init {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "init ")
        context = appRef.get()!!.applicationContext
        val serviceIntent =
            Intent("com.cltv.mal.ServiceImpl")
        serviceIntent.setPackage("com.cltv.mal")
        context!!.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        context!!.registerReceiver(serviceReceiver, IntentFilter("ServiceIntent"))
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun refresh() {
        serviceImpl?.refresh()
        epgDataProvider.loadEvents()
    }

    override fun initService(): TvView {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "initService")
        return TvView(appRef.get()!!.applicationContext)
    }

    override fun createInteractiveAppModule(): InteractiveAppInterface {
        return InteractiveAppInterfaceImpl(serviceImpl!!)
    }

    override fun createPreferenceModule(
        utilsInterface: UtilsInterface,
        generalConfigInterface: GeneralConfigInterface
    ): PreferenceInterface {
        return PreferenceInterfaceImpl(serviceImpl!!)
    }

    override fun createClosedCaptionModule(
        utilsInterface: UtilsInterface,
        playerInterface: PlayerInterface
    ): ClosedCaptionInterface {
        return ClosedCaptionInterfaceImpl(serviceImpl!!,playerInterface as PlayerInterfaceImpl)
    }

    override fun createCiPlusModule(
        playerInterface: PlayerInterface,
        tvInterface: TvInterface
    ): CiPlusInterface {
        return CiPlusInterfaceImpl(serviceImpl!!)
    }

    override fun createCategoryModule(
        tvInterface: TvInterface,
        favoritesInterface: FavoritesInterface,
        utilsModule: UtilsInterface
    ): CategoryInterface {
        return CategoryInterfaceImpl(serviceImpl!!)
    }

    private val epgDataProvider by lazy {
        EpgDataProvider(appRef.get()!!.applicationContext)
    }

    override fun createEpgModule(timeInterface: TimeInterface): EpgInterface {
        return EpgInterfaceBaseImpl(epgDataProvider, serviceImpl!!)
    }

    override fun createFavoriteModule(utilsInterface: UtilsInterface): FavoritesInterface {
        return FavoritesInterfaceImpl(serviceImpl!!, utilsInterface)
    }

    override fun createNetworkModule(): NetworkInterface {
        val cm = appRef.get()!!
            .getSystemService(ConnectivityManager::class.java)
        return NetworkInterfaceImpl(cm)
    }

    override fun createPlatformOsModule(): PlatformOsInterface {
        return PlatformOsInterfaceImpl(serviceImpl!!)
    }

    override fun createPlayerModule(
        utilsInterface: UtilsInterface,
        epgInterface: EpgInterface,
        parentalControlSettingsInterface: ParentalControlSettingsInterface
    ): PlayerInterface {
        if (!::playerInterfaceImpl.isInitialized) {
            playerInterfaceImpl = PlayerInterfaceImpl(
                appRef.get()!!.applicationContext,
                serviceImpl!!,
                utilsInterface,
                parentalControlSettingsInterface
            )
        }
        return playerInterfaceImpl
        /*return PlayerInterfaceImpl(serviceImpl!!, utilsInterface)*/
    }

    override fun createPvrModule(
        epgInterface: EpgInterface,
        playerInterface: PlayerInterface,
        tvInterface: TvInterface,
        utilsInterface: UtilsInterface,
        timeInterface: TimeInterface
    ): PvrInterface {
        return PvrInterfaceImpl(appRef.get()!!.applicationContext, serviceImpl!!, playerInterface, epgInterface, tvInterface)
    }

    override fun createSearchModule(
        pvrInterface: PvrInterface,
        scheduledInterface: ScheduledInterface,
        utilsInterface: UtilsInterface,
        networkInterface: NetworkInterface
    ): SearchInterface {
        return SearchInterfaceImpl(epgDataProvider, serviceImpl!!)
    }

    override fun createTimeshiftModule(playerInterface: PlayerInterface, utilsInterface: UtilsInterface): TimeshiftInterface {
        return TimeshiftInterfaceImpl(serviceImpl!!, playerInterfaceImpl, utilsInterface, null)
    }

    override fun createScheduledModule(tvInterface: TvInterface): ScheduledInterface {
        return ScheduledInterfaceImpl(appRef.get()!!.applicationContext,serviceImpl!!,tvInterface)
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
        tvInterfaceImpl = TvInterfaceImpl(serviceImpl!!, playerInterface, networkInterface, utilsInterface)
        return TvInterfaceImpl(serviceImpl!!, playerInterface, networkInterface, utilsInterface)
    }

    override fun createGeneralConfigModule(utilsInterface: UtilsInterface): GeneralConfigInterface {
        return GeneralConfigInterfaceImpl(appRef.get()!!.applicationContext, serviceImpl!!)
    }

    override fun createSchedulerModule(
        utilsInterface: UtilsInterface,
        epgInterface: EpgInterface,
        watchlistInterface: WatchlistInterface,
        timeInterface: TimeInterface
    ): SchedulerInterface {
        return SchedulerInterfaceImpl(
            appRef.get()!!.applicationContext,
            serviceImpl!!,
            epgInterface,
            watchlistInterface)
    }

    override fun createWatchlistModule(
        epgInterfaceImpl: EpgInterface, timeInterface: TimeInterface
    ): WatchlistInterface {
        return WatchlistInterfaceImpl(
            serviceImpl!!,
            epgInterfaceImpl,
            appRef.get()!!.applicationContext,
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
        if (::tvInterfaceImpl.isInitialized) {
            return ForYouInterfaceImpl(
                serviceImpl!!,
                tvInterfaceImpl,
                epgInterface,
                epgDataProvider,
                watchlistInterface,
                pvrInterface,
                utilsInterface,
                recommendationInterface,
                schedulerInterface
            )
        } else {
            return com.iwedia.cltv.platform.mal_service.ForYouInterfaceImpl(serviceImpl!!)
        }
    }

    override fun createUtilsModule(): UtilsInterface {
        return utilsInterfaceImpl
    }

    override fun createHbbTvModule(timeInterface: TimeInterface): HbbTvInterface {
        return hbbTvInterfaceImpl
    }

    override fun createTTXModule(utilsInterface: UtilsInterface): TTXInterface {
        return TeletextInterfaceImpl(serviceImpl!!)
    }

    override fun createTvInputModule(
        parentalControlSettingsInterface: ParentalControlSettingsInterface,
        utilsInterface: UtilsInterface
    ): TvInputInterface {
        return TvInputInterfaceImpl(appRef.get()!!.applicationContext, serviceImpl!!)
    }

    override fun createParentalControlSettingsModule(): ParentalControlSettingsInterface {
        return ParentalControlSettingsInterfaceImpl(
            appRef.get()!!.applicationContext,
            serviceImpl!!
        )
    }

    override fun createPreferenceChannelsModule(tvInterface: TvInterface): PreferenceChannelsInterface {
        return PreferenceChannelsInterfaceImpl(serviceImpl!!)
    }

    override fun createInputModule(
        utilsInterface: UtilsInterface,
        parentalControlSettingsInterface: ParentalControlSettingsInterface
    ): InputSourceInterface {
        return InputSourceInterfaceImpl(serviceImpl!!)
    }

    override fun createFactoryModeModule(utilsModule: UtilsInterface): FactoryModeInterface {
        return FactoryModeInterfaceImpl(serviceImpl!!)
    }

    override fun createTimeModule(): TimeInterface {
        return TimeInterfaceImpl(serviceImpl!!)
    }

    override fun createSubtitleModule(
        utilsInterface: UtilsInterface,
        playerInterface: PlayerInterface
    ): SubtitleInterface {
        return SubtitleInterfaceImpl(serviceImpl!!)
    }

    override fun createPromotionModule(): PromotionInterface {
        return PromotionInterfaceImpl(serviceImpl!!)
    }

    override fun createRecommendationModule(): RecommendationInterface {
        return RecommendationInterfaceImpl(serviceImpl!!)
    }

    override fun createFastFavoritesModule(): FastFavoriteInterface {
        return FastFavoriteInterfaceImpl(serviceImpl!!)
    }

    override fun createFastUserSettingsModule(): FastUserSettingsInterface {
        return FastUserSettingsInterfaceImpl(serviceImpl!!)
    }

    override fun createOadUpdateModule(playerInterface: PlayerInterface): OadUpdateInterface {
        return OadUpdateInterfaceImpl(playerInterface,serviceImpl!!)

    }

    override fun createTextToSpeechModule(): TTSInterface {
        return TTSInterfaceBaseImpl(appRef.get()!!.applicationContext)
    }

    override fun toString(): String {
        return "[mal_service] ModuleFactory"
    }
}