package com.iwedia.cltv

import android.app.Application
import android.media.tv.TvView
import android.os.Build
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.ModuleFactory
import com.iwedia.cltv.platform.`interface`.*

/**
 * Module provider
 * Provides module interface implementations
 *
 * @author Dejan Nadj
 */
@RequiresApi(Build.VERSION_CODES.R)
class ModuleProvider constructor(application: Application) {

    private val moduleFactory: ModuleFactory

    init {
        moduleFactory = ModuleFactory(application)
    }

    private var categoryModule: CategoryInterface? = null
    private var epgModule: EpgInterface? = null
    private var favoriteModule: FavoritesInterface? = null
    private var networkModule: NetworkInterface? = null
    private var platformOsModule: PlatformOsInterface? = null
    private var playerModule: PlayerInterface? = null
    private var pvrModule: PvrInterface? = null
    private var scheduledModule: ScheduledInterface? = null
    private var searchModule: SearchInterface? = null
    private var tvModule: TvInterface? = null
    private var utilsModule: UtilsInterface? = null
    private var generalConfigModule: GeneralConfigInterface? = null
    private var hbbTvModule: HbbTvInterface? = null
    private var ttxModule: TTXInterface? = null
    private var timeshiftModule: TimeshiftInterface? = null
    private var schedulerModule: SchedulerInterface? = null
    private var watchlistModule: WatchlistInterface? = null
    private var forYouModule: ForYouInterface? = null
    private var preferenceModule: PreferenceInterface? = null
    private var tvInputModule: TvInputInterface? = null
    private var parentalControlSettingsModule: ParentalControlSettingsInterface? = null
    private var preferenceChannelsModule: PreferenceChannelsInterface? = null
    private var closedCaptionModule: ClosedCaptionInterface? = null
    private var ciPlusModule: CiPlusInterface? = null
    private var inputSourceModule: InputSourceInterface? = null
    private var factoryModule: FactoryModeInterface? = null
    private var timeModule: TimeInterface? = null
    private var subtitleModule: SubtitleInterface? = null
    private var interactiveAppModule: InteractiveAppInterface? = null
    private var promotionModule: PromotionInterface? = null
    private var recommendationModule: RecommendationInterface? = null
    private var fastFavoriteModule: FastFavoriteInterface? = null
    private var fastUserSettingsModule: FastUserSettingsInterface? = null
    private var textToSpeechModule: TTSInterface? = null
    private var oadUpdateModule: OadUpdateInterface? = null

    fun initService(): TvView? {
        if (BuildConfig.FLAVOR.contains("mal_service")) {
            return moduleFactory.initService()
        }
        return null
    }

    fun refresh() {
        moduleFactory.refresh()
        getTimeModule()
    }

    fun getCategoryModule(): CategoryInterface {
        if (categoryModule == null) {
            categoryModule = moduleFactory.createCategoryModule(
                getTvModule(),
                getFavoriteModule(),
                getUtilsModule()
            )
        }
        return categoryModule!!
    }

    fun getEpgModule(): EpgInterface {
        if (epgModule == null) {
            epgModule = moduleFactory.createEpgModule(getTimeModule())
        }
        return epgModule!!
    }

    fun getFavoriteModule(): FavoritesInterface {
        if (favoriteModule == null) {
            favoriteModule = moduleFactory.createFavoriteModule(getUtilsModule())
        }
        return favoriteModule!!
    }

    fun getNetworkModule(): NetworkInterface {
        if (networkModule == null) {
            networkModule = moduleFactory.createNetworkModule()
        }
        return networkModule!!
    }

    fun getSchedulerModule(): SchedulerInterface {
        if (schedulerModule == null) {
            schedulerModule = moduleFactory.createSchedulerModule(
                getUtilsModule(),
                getEpgModule(),
                getWatchlistModule(),
                getTimeModule()
            )
        }
        return schedulerModule!!
    }

    fun getPlatformOSModule(): PlatformOsInterface {
        if (platformOsModule == null) {
            platformOsModule = moduleFactory.createPlatformOsModule()
        }
        return platformOsModule!!
    }

    fun getPlayerModule(): PlayerInterface {
        if (playerModule == null) {
            playerModule = moduleFactory.createPlayerModule(
                getUtilsModule(),
                getEpgModule(),
                getParentalControlSettingsModule()
            )
        }
        return playerModule!!
    }

    fun getClosedCaptionModule(): ClosedCaptionInterface {
        if (closedCaptionModule == null) {
            closedCaptionModule =
                moduleFactory.createClosedCaptionModule(getUtilsModule(), getPlayerModule())
        }
        return closedCaptionModule!!
    }

    fun getPvrModule(): PvrInterface {
        if (pvrModule == null) {
            pvrModule = moduleFactory.createPvrModule(
                getEpgModule(),
                getPlayerModule(),
                getTvModule(),
                getUtilsModule(),
                getTimeModule()
            )
        }
        return pvrModule!!
    }

    fun getScheduledModule(): ScheduledInterface {
        if (scheduledModule == null) {
            scheduledModule = moduleFactory.createScheduledModule(getTvModule())        }
        return scheduledModule!!
    }

    fun getTimeshiftModule(): TimeshiftInterface {
        if (timeshiftModule == null) {
            timeshiftModule = moduleFactory.createTimeshiftModule(getPlayerModule(), getUtilsModule())
        }
        return timeshiftModule!!
    }

    fun getWatchlistModule(): WatchlistInterface {
        if (watchlistModule == null) {
            watchlistModule =
                moduleFactory.createWatchlistModule(getEpgModule(), getTimeModule())
        }
        return watchlistModule!!
    }

    fun getGeneralConfigModule(): GeneralConfigInterface{
        if(generalConfigModule == null){
            generalConfigModule = moduleFactory.createGeneralConfigModule(getUtilsModule())
        }
        return generalConfigModule!!
    }

    fun getUtilsModule(): UtilsInterface {
        if (utilsModule == null) {
            utilsModule = moduleFactory.createUtilsModule()
        }
        return utilsModule!!
    }

    fun getSearchModule(): SearchInterface {
        if (searchModule == null) {
            searchModule = moduleFactory.createSearchModule(
                getPvrModule(),
                getScheduledModule(),
                getUtilsModule(),
                getNetworkModule()
            )
        }
        return searchModule!!
    }

    fun getTvModule(): TvInterface {
        if (tvModule == null) {
            tvModule = moduleFactory.createTvModule(
                getPlayerModule(),
                getNetworkModule(),
                getTvInputModule(),
                getUtilsModule(),
                getEpgModule(),
                getTimeModule(),
                getParentalControlSettingsModule()
            )
        }
        return tvModule!!
    }

    fun getHbbTvModule(): HbbTvInterface {
        if (hbbTvModule == null) {
            hbbTvModule = moduleFactory.createHbbTvModule(getTimeModule())
        }
        return hbbTvModule!!
    }

    fun getTTXModule(): TTXInterface {
        if (ttxModule == null) {
            ttxModule = moduleFactory.createTTXModule(getUtilsModule())
        }
        return ttxModule!!
    }

    fun getForYouModule(): ForYouInterface {
        if (forYouModule == null) {
            forYouModule = moduleFactory.createForYouModule(
                getEpgModule(),
                getWatchlistModule(),
                getPvrModule(),
                getUtilsModule(),
                getRecommendationModule(),
                getSchedulerModule()
            )
        }
        return forYouModule!!
    }

    fun getPreferenceModule(): PreferenceInterface {
        if (preferenceModule == null) {
            preferenceModule = moduleFactory.createPreferenceModule(getUtilsModule(), getGeneralConfigModule())
        }
        return preferenceModule!!
    }

    fun getTvInputModule(): TvInputInterface {
        if (tvInputModule == null) {
            tvInputModule = moduleFactory.createTvInputModule(
                getParentalControlSettingsModule(),
                getUtilsModule()
            )
        }
        return tvInputModule!!
    }

    fun getOadUpdateModule(): OadUpdateInterface {
        if (oadUpdateModule == null) {
            oadUpdateModule = moduleFactory.createOadUpdateModule(getPlayerModule())
        }

        return oadUpdateModule!!
    }

    fun getParentalControlSettingsModule(): ParentalControlSettingsInterface {
        if (parentalControlSettingsModule == null) {
            parentalControlSettingsModule = moduleFactory.createParentalControlSettingsModule()
            parentalControlSettingsModule?.setTvInputInterface(getTvInputModule())
        }
        return parentalControlSettingsModule!!
    }

    fun getEasModule(): PreferenceInterface {
        if (preferenceModule == null) {
            preferenceModule = moduleFactory.createPreferenceModule(getUtilsModule(), getGeneralConfigModule())
        }
        return preferenceModule!!
    }

    fun getPreferenceChannelsModule(): PreferenceChannelsInterface {
        if (preferenceChannelsModule == null) {
            preferenceChannelsModule = moduleFactory.createPreferenceChannelsModule(getTvModule())
        }
        return preferenceChannelsModule!!
    }

    fun getCiPlusModule(): CiPlusInterface {
        if (ciPlusModule == null) {
            ciPlusModule = moduleFactory.createCiPlusModule(getPlayerModule(), getTvModule())
        }
        return ciPlusModule!!
    }

    fun getInputSourceMoudle(): InputSourceInterface {
        if (inputSourceModule == null) {
            inputSourceModule = moduleFactory.createInputModule(
                getUtilsModule(),
                getParentalControlSettingsModule()
            )
        }
        return inputSourceModule!!
    }

    fun getFactoryModule(): FactoryModeInterface {
        if (factoryModule == null) {
            factoryModule = moduleFactory.createFactoryModeModule(getUtilsModule())
        }
        return factoryModule!!
    }

    fun getTimeModule(): TimeInterface {
        if (timeModule == null) {
            timeModule = moduleFactory.createTimeModule()
        }
        return timeModule!!
    }

    fun getSubtitleModule(): SubtitleInterface {
        if (subtitleModule == null) {
            subtitleModule = moduleFactory.createSubtitleModule(getUtilsModule(), getPlayerModule())
        }
        return subtitleModule!!
    }

    fun getInteractiveAppModule(): InteractiveAppInterface {
        if (interactiveAppModule == null) {
            interactiveAppModule = moduleFactory.createInteractiveAppModule()
        }
        return interactiveAppModule!!
    }

    fun getPromotionModule(): PromotionInterface {
        if (promotionModule == null) {
            promotionModule = moduleFactory.createPromotionModule()
        }
        return promotionModule!!
    }

    fun getRecommendationModule(): RecommendationInterface {
        if (recommendationModule == null) {
            recommendationModule = moduleFactory.createRecommendationModule()
        }
        return recommendationModule!!
    }

    fun getFastFavoriteModule(): FastFavoriteInterface {
        if (fastFavoriteModule == null) {
            fastFavoriteModule = moduleFactory.createFastFavoritesModule()
        }
        return fastFavoriteModule!!
    }

    fun getFastUserSettingsModule(): FastUserSettingsInterface {
        if (fastUserSettingsModule == null) {
            fastUserSettingsModule = moduleFactory.createFastUserSettingsModule()
        }
        return fastUserSettingsModule!!
    }

    fun getTextToSpeechModule(): TTSInterface {
        if (textToSpeechModule == null) {
            textToSpeechModule = moduleFactory.createTextToSpeechModule()
        }
        return textToSpeechModule!!
    }
}