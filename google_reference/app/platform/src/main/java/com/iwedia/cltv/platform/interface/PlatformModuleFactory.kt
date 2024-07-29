package com.iwedia.cltv.platform.`interface`

import android.media.tv.TvView

interface PlatformModuleFactory {
    fun createCategoryModule(
        tvInterface: TvInterface,
        favoritesInterface: FavoritesInterface,
        utilsModule: UtilsInterface
    ): CategoryInterface

    fun createEpgModule(timeInterface: TimeInterface): EpgInterface
    fun createFavoriteModule(utilsModule: UtilsInterface): FavoritesInterface
    fun createNetworkModule(): NetworkInterface
    fun createPlatformOsModule(): PlatformOsInterface
    fun createPlayerModule(
        utilsInterface: UtilsInterface,
        epgInterface: EpgInterface,
        parentalControlSettingsInterface: ParentalControlSettingsInterface
    ): PlayerInterface

    fun createPvrModule(
        epgInterface: EpgInterface,
        playerInterface: PlayerInterface,
        tvInterface: TvInterface,
        utilsInterface: UtilsInterface,
        timeInterface: TimeInterface
    ): PvrInterface

    fun createSearchModule(
        pvrInterface: PvrInterface,
        scheduledInterface: ScheduledInterface,
        utilsInterface: UtilsInterface,
        networkInterface: NetworkInterface
    ): SearchInterface

    fun createTvModule(
        playerInterface: PlayerInterface,
        networkInterface: NetworkInterface,
        tvInputInterface: TvInputInterface,
        utilsInterface: UtilsInterface,
        epgInterface: EpgInterface,
        timeInterface: TimeInterface,
        parentalControlSettingsInterface: ParentalControlSettingsInterface
    ): TvInterface
    fun createGeneralConfigModule(utilsInterface: UtilsInterface): GeneralConfigInterface

    fun createUtilsModule(): UtilsInterface
    fun createHbbTvModule(timeInterface: TimeInterface): HbbTvInterface
    fun createTTXModule(utilsInterface: UtilsInterface): TTXInterface
    fun createScheduledModule(
        tvInterface: TvInterface
    ): ScheduledInterface

    fun createTimeshiftModule(playerInterface: PlayerInterface, utilsInterface: UtilsInterface): TimeshiftInterface
    fun createSchedulerModule(
        utilsInterface: UtilsInterface,
        epgInterface: EpgInterface,
        watchlistInterface: WatchlistInterface,
        timeInterface: TimeInterface
    ): SchedulerInterface
    fun createWatchlistModule(epgInterfaceImpl: EpgInterface, timeInterface: TimeInterface): WatchlistInterface
    fun createForYouModule(epgInterface: EpgInterface,watchlistInterface: WatchlistInterface,
                           pvrInterface: PvrInterface, utilsInterface: UtilsInterface,
                           recommendationInterface: RecommendationInterface,
                           schedulerInterface: SchedulerInterface): ForYouInterface
    fun initService(): TvView
    fun refresh()
    fun createInteractiveAppModule(): InteractiveAppInterface
    fun createPreferenceModule(utilsInterface: UtilsInterface, generalConfigInterface: GeneralConfigInterface): PreferenceInterface
    fun createTvInputModule(
        parentalControlSettingsInterface: ParentalControlSettingsInterface,
        utilsInterface: UtilsInterface
    ): TvInputInterface

    fun createParentalControlSettingsModule(): ParentalControlSettingsInterface
    fun createPreferenceChannelsModule(tvInterface: TvInterface): PreferenceChannelsInterface
    fun createClosedCaptionModule(
        utilsInterface: UtilsInterface,
        playerInterface: PlayerInterface
    ): ClosedCaptionInterface

    fun createCiPlusModule(playerInterface: PlayerInterface, tvInterface: TvInterface): CiPlusInterface
    fun createInputModule(
        utilsInterface: UtilsInterface,
        parentalControlSettingsInterface: ParentalControlSettingsInterface
    ): InputSourceInterface

    fun createFactoryModeModule(utilsModule: UtilsInterface): FactoryModeInterface
    fun createTimeModule(): TimeInterface
    fun createSubtitleModule(
        utilsInterface: UtilsInterface,
        playerInterface: PlayerInterface
    ): SubtitleInterface

    fun createPromotionModule(): PromotionInterface

    fun createRecommendationModule(): RecommendationInterface

    fun createFastFavoritesModule(): FastFavoriteInterface

    fun createFastUserSettingsModule(): FastUserSettingsInterface

    fun createOadUpdateModule(playerInterface: PlayerInterface) : OadUpdateInterface
    fun createTextToSpeechModule(): TTSInterface
}