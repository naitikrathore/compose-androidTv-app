// IServiceAPI.aidl
package com.cltv.mal;

// Declare any non-default types here with import statements
import com.cltv.mal.model.entities.TvChannel;
import com.cltv.mal.model.entities.TvEvent;
import com.cltv.mal.model.entities.ServiceTvView;
import com.cltv.mal.model.entities.TvChannel;
import com.cltv.mal.model.entities.TvEvent;
import com.cltv.mal.model.async.IAsyncTvEventListener;
import android.media.tv.TvInputInfo;
import com.cltv.mal.model.async.IAsyncListener;
import com.cltv.mal.model.async.IAsyncTvChannelListener;
import com.cltv.mal.model.async.IAsyncRailItemListener;
import com.cltv.mal.interfaces.ILanguageMapperInterface;
import com.cltv.mal.interfaces.ITvInputInterface;

import android.media.tv.TvTrackInfo;
import com.cltv.mal.model.entities.TvChannel;
import com.cltv.mal.model.entities.TvEvent;
import com.cltv.mal.model.entities.CountryPreference;
import com.cltv.mal.model.entities.Region;
import com.cltv.mal.interfaces.ILanguageMapperInterface;
import com.cltv.mal.model.ci_plus.PlatformSpecificOperation;
import com.cltv.mal.model.entities.DateTimeFormat;
import com.cltv.mal.model.async.IAsyncSpeedTestListener;
import com.cltv.mal.model.async.IAsyncFomattingProgressListener;
import com.cltv.mal.model.async.IStringTranslationListener;
import com.cltv.mal.model.entities.AudioTrack;
import com.cltv.mal.model.entities.SubtitleTrack;
import com.cltv.mal.model.content_rating.ContentRatingSystem;
import com.cltv.mal.model.pvr.Recording;

import com.cltv.mal.model.fast.PromotionItem;
import com.cltv.mal.model.fast.RecommendationRow;
import com.cltv.mal.model.fast.FastRatingListItem;
import com.cltv.mal.model.entities.ScheduledReminder;
import com.cltv.mal.model.entities.ScheduledRecording;
import com.cltv.mal.model.async.IAsyncScheduledReminderListener;
import com.cltv.mal.model.async.IAsyncScheduledRecordingListener;
import com.cltv.mal.model.async.IAsyncScheduleRecordingResultListener;
import com.cltv.mal.model.entities.FavoriteItem;
import com.cltv.mal.model.async.IAsyncCategoryListener;
import com.cltv.mal.model.entities.Category;
import com.cltv.mal.model.async.ICiPlusListener;
import com.cltv.mal.model.prefs.PrefType;
import com.cltv.mal.model.prefs.PrefMenu;
import com.cltv.mal.model.prefs.PrefSubMenu;
import com.cltv.mal.model.ci_plus.CamTypePreference;

import com.cltv.mal.model.async.IAsyncRecordingListener;
import com.cltv.mal.model.pvr.RecordingInProgress;
import com.cltv.mal.model.pvr.PvrSeekType;
import com.cltv.mal.model.async.IAsyncIntListener;
import com.cltv.mal.model.pvr.ScheduleRecordingResult;
import android.view.KeyEvent;
import com.cltv.mal.model.hbb_tv.HbbTvKeyEventType;
import com.cltv.mal.model.hbb_tv.HbbTvCookieSettingsValue;
import com.cltv.mal.model.async.IAsyncHbbTvUnusedKeyReplyListener;
import com.cltv.mal.model.content_rating.TvContentRating;
import com.cltv.mal.model.content_rating.ContentRatingSystem;
import com.cltv.mal.model.content_rating.ContentRatingSystemRating;
import com.cltv.mal.model.content_rating.ContentRatingSystemSubRating;
import com.cltv.mal.model.entities.InputSourceData;
import com.cltv.mal.model.entities.InputItem;
import com.cltv.mal.model.entities.InputSourceData;
import com.cltv.mal.model.entities.InputResolutionItem;
import com.cltv.mal.model.entities.LanguageCode;
import com.cltv.mal.model.async.IOadEventListener;
import com.cltv.mal.model.entities.OadEventData;
import com.cltv.mal.model.entities.SystemInfoData;

interface IServiceAPI {

     void initialize();

    void refresh();

    void loadEvents();

    String getVersion();

    //----------------------- EPG Interface API ----------------------------------------------------
    TvEvent getCurrentEvent(in TvChannel tvChannel);
    TvEvent getTvEvent(in int index);
    TvEvent getTvEventById(in int id);
    TvEvent getTvEventByNameAndStartTime(in String name, in long startTime);
    oneway void getEventList(in IAsyncTvEventListener listener);
    oneway void getEventListByChannel(in TvChannel tvChannel, in IAsyncTvEventListener listener);
    oneway void getEventListByChannelAndTime(in TvChannel tvChannel, in long startTime, in long endTime, in IAsyncTvEventListener listener);
    oneway void getAllCurrentEvent(in IAsyncTvEventListener listener);
    oneway void getAllNextEvents(in IAsyncTvEventListener listener);
    void updateEpgData(in int applicationMode);
    TvEvent getUncachedCurrentEvent(in TvChannel tvChannel);
    TvEvent getUncachedNextEvent(in TvChannel tvChannel);

    //----------------------- TV Interface API ----------------------------------------------------
    void setupDefaultService(in TvChannel[] channels, in int applicationMode);
    oneway void getSelectedChannelList(in int applicationMode,in int filterItemType, in String filterMetadata, in IAsyncTvChannelListener listener);
    oneway void nextChannel(in int applicationMode, in IAsyncListener listener);
    oneway void previousChannel(in int applicationMode, in IAsyncListener listener);
    void getLastActiveChannel(in int applicationMode);
    TvChannel getChannelById(in int channelId, in int applicationMode);
    int findChannelPosition(in TvChannel tvChannel, in int applicationMode);
    oneway void changeChannel(in int index, in int applicationMode, in IAsyncListener listener);
    oneway void playChannel(in TvChannel tvChannel, in int applicationMode, in IAsyncListener listener);
    oneway void playNextIndex(in TvChannel[] selectedChannelList, in int applicationMode, in IAsyncListener listener);
    oneway void playPrevIndex(in TvChannel[] selectedChannelList, in int applicationMode, in IAsyncListener listener);
    TvChannel getChannelByDisplayNumber(in String displayNumber, in int applicationMode);
    void enableLcn(in boolean enableLcn, in int applicationMode);
    void updateDesiredChannelIndex(in int applicationMode);
    void updateLaunchOrigin(in int categoryId, in String favGroupName, in String tifCategoryName, in String genreCategoryName, in int applicationMode);
    TvChannel getActiveChannel(in int applicationMode);
    void storeActiveChannel(in TvChannel tvChannel, in int applicationMode);
    void storeLastActiveChannel(in TvChannel tvChannel, in int applicationMode);
    TvChannel getChannelByIndex(in int index, in int applicationMode);
    TvChannel[] getChannelList(in int applicationMode);
    oneway void nextChannelByCategory(in int categoryId, in int applicationMode, in IAsyncListener listener);
    oneway void previousChannelByCategory(in int categoryId, in int applicationMode, in IAsyncListener listener);
    void setRecentChannel(in int channelIndex, in int applicationMode);
    boolean startInitialPlayback(in int applicationMode);
    TvChannel[] getRecentlyWatched(in int applcationMode);
    boolean deleteChannel(in TvChannel tvChannel, in int applicationMode);
    oneway void lockUnlockChannel(in TvChannel tvChannel, in boolean lockUnlock, in int applicationMode, in IAsyncListener listener);
    boolean isChannelLockAvailable(in TvChannel tvChannel, in int applicationMode);
    boolean skipUnskipChannel(in TvChannel tvChannel, in boolean skipUnskip, in int applicationMode);
    int getDesiredChannelIndex(in int applicationMode);
    TvInputInfo[] getTvInputListTvInterface(in int applicationMode);
    TvChannel[] getChannelListByCategories(in int entityCategory, in int applicationMode);
    boolean isParentalEnabledTvInterface(in int applicationMode);
    boolean isTvNavigationBlocked(in int applicationMode);
    boolean isChannelLocked(in int channelId, in int applicationMode);
    void initSkippedChannels(in int applicationMode);
    String getTifChannelSourceLabel(in TvChannel tvChannel, in int applicationMode);
    String getChannelSourceType(in TvChannel tvChannel, in int applicationMode);
    String getAnalogTunerTypeName(in TvChannel tvChannel, in int applicationMode);
    int getAnalogServiceListID(in TvChannel tvChannel, in int applicationMode);
    String getParentalRatingDisplayNameTvInterface(in String parentalRating, in int applicationMode, in TvEvent tvEvent);
    boolean isLcnEnabled(in int applicationMode);
    void getLockedChannelListAfterOta(in int applicationMode);
    String[] getVisuallyImpairedAudioTracks(in int applicationMode);
    boolean isSignalAvailable();
    boolean isChannelsAvailable();
    boolean isWaitingChannel();
    boolean isPlayerTimeout();
    boolean isNetworkAvailable();
    boolean appJustStarted();
    void checkAndRunBarkerChannel(in boolean run);
    void forceChannelsRefresh(in int applicationMode);
    boolean isChannelSelectable(in TvChannel channel);
    void updateTis();

    //----------------------- TV Input Interface API -----------------------------------------------
    TvInputInfo[] getTvInputList();
    TvInputInfo[] getTvInputFilteredList(in String filter);
    boolean startSetupActivity(in TvInputInfo input);
    void triggerScanCallback(in boolean isSuccessful);
    int getChannelCountForInput(in TvInputInfo input);
    boolean isParentalEnabled();
    ContentRatingSystem[] getContentRatingSystems();
    ContentRatingSystem[] getContentRatingSystemsList();
    String getContentRatingSystemDisplayName(in ContentRatingSystem contentRatingSystem);
    String getParentalRatingDisplayName(in String parentalRating, in TvEvent tvEvent);
    String getParentalRating(in String parentalRating);


    //-------------------- Utils Interface API --------------------------------------------
    boolean kidsModeEnabled();
    oneway void registerStringTranslationListener(in IStringTranslationListener listener);
    String getStringValue(in String stringId);
    int getUsbStorageTotalSpace();
    int getUsbStorageFreeSize();
    boolean formatUsbStorage();
    boolean isUsbConnected();
    String getPrimaryAudioLanguage(in int prefType);
    String getSecondaryAudioLanguage(in int prefType);
    String getPrimarySubtitleLanguageUtilsInterface(in int prefType);
    String getSecondarySubtitleLanguageUtilsInterface(in int prefType);
    void setPrimaryAudioLanguage(in String language, in int prefType);
    void setSecondaryAudioLanguage(in String language, in int prefType);
    void setPrimarySubtitleLanguageUtilsInterface(in String language, in int prefType);
    void setSecondarySubtitleLanguageUtilsInterface(in String language, in int prefType);
    void setFaderValue(in int newValue);
    void setVisuallyImpairedAudioValue(in int newValue);
    void setVisuallyImpairedValues(in int position, in boolean enabled);
    void setVolumeValue(in int value);
    int getVolumeValue();
    boolean getAudioSpeakerState();
    boolean getAudioHeadphoneState();
    boolean getAudioPaneState();
    int getDefFaderValue();
    int getAudioForVi();
    void enableAudioDescription(in boolean enable);
    boolean getAudioDescriptionState();
    void enableHardOfHearingUtilsInterface(in boolean enable);
    boolean getHardOfHearingStateUtilsInterface();
    void enableSubtitlesUtilsInterface(in boolean enable, in int prefType);
    boolean getSubtitlesStateUtilsInterface(in int prefType);
    void updateAudioTracks();
    void updateSubtitleTracksUtilsInterface();
    String getDolby(in TvTrackInfo tvTrackInfo);
    void setAudioType(in int position);
    int getAudioType();
    void setSubtitlesTypeUtilsInterface(in int position, in boolean updateSwitch);
    String[] getSubtitleTypeDisplayNames(in int prefType);
    int getSubtitleType();
    void setTeletextDigitalLanguage(in int position);
    int getTeletextDigitalLanguage();
    void setTeletextDecodeLanguage(in int position);
    int getTeletextDecodeLanguage();
    void registerFormatProgressListenerUtilsInterface(in IAsyncFomattingProgressListener listener);
    void setDeafaultLanguages();
    void enableTimeshift(in boolean enable);
    void setAspectRatio(in int index);
    int getAspectRatio();
    boolean hasAudioDescription(in TvTrackInfo tvTrackInfo);
    void setAntennaPower(in boolean enable);
    boolean isAntennaPowerEnabled();
    boolean isGretzkyBoard();
    ILanguageMapperInterface getLanguageMapper();
    void runOemCustomization(in String jsonPath);
    //getPrefsValue
    //setPrefsValue
    boolean isCurrentEvent(in TvEvent tvEvent);
    int checkUSBSpace();
    String getCodecDolbySpecificAudioInfo(in TvTrackInfo tvTrackInfo);
    boolean hasHardOfHearingSubtitleInfoUtilsInterface(in TvTrackInfo tvTrackInfo);
    String getCountryCode();
    Region getRegion();
    String getCountry();
    String getParentalPin();
    boolean setParentalPin(in String parentalPin);
    boolean isThirdPartyChannel(in TvChannel tvChannel);
    void enableBlueMute(in boolean enable);
    boolean getBlueMuteState();
    void setBlueMute(in boolean isEnable);
    boolean getIsPowerOffEnabled();
    void noSignalPowerOffChanged(in boolean enable);
    String getPowerOffTime();
    void setPowerOffTime(in int value, in String time);
    boolean getAudioDescriptionEnabled();
    boolean getHearingImpairedEnabled();
    void setAudioDescriptionEnabled(in boolean isEnable);
    void setHearingImpairedEnabled(in boolean isEnable);
    boolean isAccessibilityEnabled();
    double similarity(in String s1, in String s2);
    boolean noSignalPowerOffEnabledOTA();
    int noSignalPowerOffTimeOTA();
    //startScanChannelsIntent
    boolean isParentalPinChanged();
    boolean isUsbFormatEnabled();
    //getSystemInfoData
    SystemInfoData getSystemInfoData(in TvChannel tvChannel);
    DateTimeFormat getDateTimeFormat();
    oneway void startSpeedTest(in String realPath, in IAsyncSpeedTestListener listener);
    Map<String, String> getUsbDevices();
    void setPvrStoragePath(in String path);
    String getPvrStoragePath();
    boolean isUsbFreeSpaceAvailable();
    void setApplicationRunningInBackground(in boolean running);
    boolean isUsbWritableReadable();
    boolean isCorrectUsbFormat();
//    void deleteRecordingFromUsb(in Recording recording, in IAsyncListener listener);
    boolean getCountryPreferences(in CountryPreference countryPreference, in boolean defaultValue);
    String getEWSPostalCode();
    void setEWSPostalCode(in String postalCode);
    int getApplicationMode();
    void setApplicationMode(in int applicationMode);
    void setChannelUnlocked(in boolean unlocked);
    boolean isChannelUnlockedUtilsInterface();
    void setTvInputId(in String inputId);
    String getTvInputId();
    void onEvent(in String inputId, in String eventType, in Bundle eventArgs);
    boolean isSubtitleRegion();
    boolean isTeletextAvailable();

    //-------------------- Player Interface API --------------------------------------------
    void setCaptionEnabled(in boolean enabled);
    void selectSubtitle(in SubtitleTrack subtitle, in boolean isAnalogChannel);
    void selectAudioTrack(in AudioTrack audioTrack);
    SubtitleTrack[] getSubtitleTracks();
    AudioTrack[] getAudioTracks();
    SubtitleTrack getActiveSubtitle();
    AudioTrack getActiveAudioTrack();
    void setPlaybackView(in ServiceTvView playbackView);
    void pause();
    void play(in TvChannel tvChannel);
    void reset();
    void resume();
    void stop();
    void mute();
    void unmute();
    void seek(in long positionMs, in boolean isRelative);
    void setSpeed(in int speed);
    int getSpeed();
    void slowDown();
    void speedUp();
    long getDuration();
    oneway void requestUnblockContent(in TvContentRating contentRating, in IAsyncListener listener);
    TvTrackInfo[] getPlaybackTracks(in int type);
    boolean getIsDolby(in int type);
    String getDolbyType(in int type, in String trackId);
    boolean getIsCC(in int type);
    boolean getIsAudioDescription(in int type);
    boolean hasAudioDescriptionPlayerInterface(in TvTrackInfo tvTrackInfo);
    boolean getTeletext(in int type);
    String getVideoResolution();
    int getAudioChannelIndex(in int type);
    boolean unlockChannel();
    boolean playRecording(in Recording recording);
    int getPlayerState();
    boolean isTimeShiftAvailable();
    boolean wasScramble();
    boolean isParentalActive();
    boolean isChannelUnlocked();
    boolean isOnLockScreen();
    int getPlaybackStatus();
    void setPlaybackStatus(in int playbackStatus);
    void switchAudioTrack();
    long getPosition();
    void setQuietTuneEnabled(in boolean enabled);
    boolean getQuietTuneEnabled();


    //-------------------- FAST Data Provider Interface API ----------------------------------------
    PromotionItem[] getPromotionListFastDataProvider();
    RecommendationRow[] getRecommendationRowsFastDataProvider();
    String[] getGenreList();
    String[] getFastFavoriteList();
    void updateFavoriteList(in String channelId, in boolean addToFavorite);
    String getAnokiUID();
    void updateDNT(in boolean enableDNT);
    int getDNT();
    int getTosOptIn();
    void updateTosOptIn(in int value);
    FastRatingListItem[] getFastRatingList();
    void updateRating(in String rating);
    void deleteAllFastData(in String inputId);

    //-------------------- FAST User Setting Interface API -----------------------------------------
    int getDnt();
    void setDnt(in boolean value);
    boolean checkTos();
    boolean isRegionSupported();
    void deleteAllFastDataUserSettings(in String inputId);

    //-------------------- FAST favorites setting interface ----------------------------------------
    String[] getFavorites();
    void updateFavorites(in String channelId, in boolean addFavorite);

    //-------------------- Promotions Interface API ------------------------------------------------
    PromotionItem[] getPromotionList();

    //-------------------- Recommendation Interface API --------------------------------------------
    RecommendationRow[] getRecommendationRows();

    //-------------------- Subtitle Interface API --------------------------------------------------
    boolean hasHardOfHearingSubtitleInfo(in TvTrackInfo tvTrackInfo);
    void enableHardOfHearing(in boolean enable);
    void enableSubtitles(in boolean enable);
    void setSubtitlesType(in int position, in boolean updateSwitch);
    void setPrimarySubtitleLanguage(in String language);
    void setSecondarySubtitleLanguage(in String language);
    boolean getHardOfHearingState();
    boolean getSubtitlesState();
    int getSubtitlesType();
    String getPrimarySubtitleLanguage();
    String getSecondarySubtitleLanguage();
    void updateSubtitleTracks();
    void setAnalogSubtitlesType(in String value);
    String getAnalogSubtitlesType();

    //-------------------- Time Interface API ------------------------------------------------------
    long getCurrentTime();
    long getCurrentTimeByChannel(in TvChannel tvChannel);

    //-------------------- Factory Mode Interface API --------------------------------------------------
    boolean hasSignal();
    void restoreEdidVersion();
    int tuneByChannelNameOrNum(in String channelName, in boolean isName, in ServiceTvView tvView);
    String tuneByUri(in Uri uri, in ServiceTvView tvView);
    int getVendorMtkAutoTest();
    void getFirstChannelAndTune(in ServiceTvView tvView);
    String tuneToActiveChannel(in ServiceTvView tvView);
    void loadChannels();
    String channelUpOrDown(in boolean isUp, in ServiceTvView tvView);
    String getChannelDisplayName();
    void deleteInstance();

    //-------------------- For You Interface API --------------------------------------------------
    int getAvailableRailSize();
    oneway void getForYouRails(in IAsyncRailItemListener listener);
    void updateRailData();
    void setPvrEnabled(in boolean pvrEnabled);

    //-------------------- TTX Interface API --------------------------------------------------
    void startTTX(in Surface ttxSurface);
    void startTTXChangeSize(in Surface ttxSurface, in boolean changeSize);
    void stopTTX();
    boolean sendKeyToTtx(in int keyCode, in KeyEvent keyEvent);
    boolean isTTXAvailable();
    void setTTXAvailable(in boolean isAvailable);
    boolean isTTXActive();
    void addCallback();
    void disposetTTX();
    void saveDigitalTTXLanguage(in String value);
    void saveDecodingPageLanguage(in int value);
    String getDigitalTTXLanguage();
    int getDecodingPageLanguage();

    //-------------------- Watchlist Interface API -------------------------------------------------
    oneway void scheduleReminder(in ScheduledReminder reminder, in IAsyncListener listener);
    void clearWatchlist();
    oneway void removeScheduledReminder(in ScheduledReminder reminder , in IAsyncListener listener);
    ScheduledReminder[] getWatchList();
    int getWatchListCount();
    ScheduledReminder getWatchlistItem(in int position);
    boolean hasScheduledReminder(in TvEvent tvEvent);
    void removeWatchlistEventsForDeletedChannels();
    void loadScheduledReminders();
    boolean isInWatchlist(in TvEvent tvEvent);
    boolean checkReminderConflict(in int channelId, in long startTime);

    //---------------------- Search Interface API --------------------------------------------------
    TvChannel[] searchForChannels(in String query);
    TvEvent[] searchForEvents(in String query);
    ScheduledReminder[] searchForScheduledReminders(in String query);
    TvChannel getSimilarChannel(in String channelQuery);

    //------------------------ Time Shift Interface API --------------------------------------------
    void setLiveTvView(in ServiceTvView liveTvView);
    boolean timeShiftPause();
    boolean pauseTimeShift();
    boolean resumeTimeShift();
    boolean timeShiftStop();
    boolean timeShiftSeekForward(in long timeMs);
    boolean timeShiftSeekBackward(in long timeMs);
    boolean timeShiftSeekTo(in long timeMs);
    boolean setTimeShiftSpeed(in int speed);
    void setTimeShiftIndication(in boolean show);
    void setTimeShiftPositionCallback();
    boolean resetTimeShiftInterface();
    void setTimeShiftPaused(in boolean isPaused);
    boolean isTimeShiftPaused();
    void setTimeShiftActive(in boolean isActive);
    boolean isTimeShiftActive();
    boolean getTimeShiftIndication();
    void stopTimeshift();
    oneway void realPause(in IAsyncListener listener);
    void registerFormatProgressListener(in IAsyncFomattingProgressListener formatListener);

    //------------------------ Platform OS Interface API -------------------------------------------
    void clearMemory();
    String getPlatformName();
    int getPlatformType();
    String getPlatformOsVersion();
    void writeFile(in String content, in String filePath);
    String readFileContent(in String filePath);
    boolean doesFileExist(in String filePath);
    void sendPlatformPrivateCommand(in String action, in Bundle data);

    //------------------------ Favorites Interface API -------------------------------------------
    oneway void updateFavoriteItem(in FavoriteItem item,in IAsyncListener listener);
    boolean isInFavorites(in FavoriteItem favoriteItem);
    FavoriteItem[] getFavoriteItems();
    FavoriteItem[] getFavoriteListByType(in int type);
    String[] geFavoriteCategories();
    boolean addFavoriteCategory(in String category);
    boolean removeFavoriteCategory(in String category);
    boolean renameFavoriteCategory(in String newName, in String oldName);
    String[] getAvailableCategories();
    FavoriteItem[] getFavoritesForCategory(in String category);
    TvChannel[] getChannelListFavoritesInterface();
    void addFavoriteInfoToChannels();
    void clearFavourites();

    //------------------------ Category Interface API -------------------------------------------
    String getActiveCategory(in int applicationMode);
    void setActiveCategory(in String activeCategory, in int applicationMode);
    int getActiveEpgFilter(in int applicationMode);
    void setActiveEpgFilter(in int filterId, in int applicationMode);
    oneway void getActiveCategoryChannelList(in int activeCategoryId, in int applicationMode, in IAsyncTvChannelListener listener);
    oneway void getAvailableFilters(in int applicationMode, in IAsyncCategoryListener listener);
    oneway void filterChannels(in Category category, in int applicationMode, in IAsyncTvChannelListener listener);

    //------------------------ CiPlus Interface API -------------------------------------------
    String getCiName();
    void selectMenuItem(in int position);
    boolean isCamActive();
    int getMenuListID();
    int getEnqId();
    void setMMICloseDone();
    void enterMMI();
    void cancelCurrMenu();
    boolean isChannelScrambled();
    oneway void registerListener(in ICiPlusListener listener);
    void enquiryAnswer(in boolean abort, in String answer);
    boolean isCamPinEnabled();
    void setCamPin(in String pin);
    String[] getMenu();
    boolean isContentClear();
    void disposeCiPlus();
    void startCAMScan(in boolean isTriggeredByUser, in boolean isCanceled);
    String getProfileName();
    void deleteProfile(in String profileName);
    void enableProfileInstallation();
    void doCAMReconfiguration(in CamTypePreference camTypePreference);
    void platformSpecificOperations(in PlatformSpecificOperation operation, in boolean parameter);

    //------------------------ Closed Caption Interface API ----------------------------------------
    int getDefaultCCValues(in String ccOptions);
    void saveUserSelectedCCOptions(in String ccOptions, in int newValue, in boolean isOtherIntput);
    void resetCC();
    void setCCInfo();
    void disableCCInfo();
    void setCCWithMute(in boolean isEnable);
    void setCCWithMuteInfo();
    boolean isClosedCaptionEnabled();
    int setClosedCaption(in boolean isOtherInput);
    String getClosedCaption(in boolean isOtherInput);
    boolean getSubtitlesStateClosedCaption();
    boolean getDefaultMuteValues();
    boolean isCCTrackAvailable();
    void initializeClosedCaption();
    void disposeClosedCaption();
    void applyClosedCaptionStyle();
    void updateSelectedTrack();

    //------------------------ Preference Interface API -------------------------------------------
    PrefType[] getPreferenceTypes();
    PrefMenu[] getPreferenceMenus(in PrefType type);
    PrefSubMenu[] getPreferenceSubMenus(in PrefMenu prefMenu, in PrefType prefType);
    void initializeEas();
    void disposeEas();
    int getEasChannel();

    //------------------------ InteractiveApp Interface API ----------------------------------------
    boolean sendKeyToInteractiveApp(in int keyCode, in boolean buttonDown);

    //------------------------ Preference Channels Interface API -----------------------------------
    boolean swapChannel(in TvChannel firstChannel, in TvChannel secondChannel, in int previousPosition, in int newPosition);
    boolean moveChannel(in TvChannel[] moveChannelList, in int previousIndex, in int newIndex, in String[] newDisplayNumberList);
    void deleteAllChannels();

    //------------------------ Pvr Interface API -------------------------------------------
    void setPvrSpeed(in int speed);
    void resetPvrInterface();
    void seekPvr(in long positionMs);
    void setupPvr();
    void disposePvr();
    oneway void startRecordingByChannel(in TvChannel tvChannel, in IAsyncListener listener, in boolean isInfiniteRec);
    oneway void startRecording(in TvChannel tvChannel, in TvEvent tvEvent, in long duration, in IAsyncListener listener);
    oneway void stopRecordingByChannel(in TvChannel tvChannel, in IAsyncListener listener);
    oneway void stopRecordingByChannelAndEvent(in TvChannel tvChannel, in TvEvent tvEvent, in IAsyncListener listener);
    oneway void getRecording(in int index, in IAsyncRecordingListener listener);
    oneway void removeRecording(in Recording recording, in IAsyncListener listener);
    oneway void renameRecording(in Recording recording, in String name, in IAsyncListener listener);
    RecordingInProgress getRecordingInProgress();
    oneway void getRecordingList(in IAsyncRecordingListener listener);
    oneway void getRecordingsCount(in IAsyncIntListener listener);
    void updateRecording(in long duration);
    void setRecIndication(in boolean show);
    void setPlaybackPosition(in int recordingId, in long position);
    void setSignalAvailability(in boolean isAvailable);
    long getPlaybackPosition(in int recordingId);
    TvChannel getRecordingInProgressTvChannel();
    boolean isRecordingInProgress();
    oneway void startPlayback(in Recording recording, in IAsyncListener listener);
    oneway void pausePlayback(in IAsyncListener listener);
    oneway void stopPlayback(in IAsyncListener listener);
    oneway void resumePlayback(in IAsyncListener listener);
    oneway void seekPlayback(in long position, in IAsyncListener listener);
    boolean isChannelRecordable(in TvChannel tvChannel);
    void setPvrTvView(in ServiceTvView tvView);
    void setPvrPositionCallback();
    void setPvrState(in PvrSeekType state);
    void reloadRecordings();

    //------------------------ Scheduler Interface API -------------------------------------------
    oneway void  storeScheduledReminder(in ScheduledReminder schedulerReminder, in IAsyncListener listener);
    oneway void  removeScheduledReminderScheduler(in ScheduledReminder schedulerReminder, in IAsyncListener listener);
    oneway  void getScheduledRemindersData(in IAsyncScheduledReminderListener listener);
    void clearReminderList();
    oneway void storeScheduledRecording(in ScheduledRecording scheduledRecording, in IAsyncListener listener);
    oneway void getRecordingId(in ScheduledRecording scheduledRecording, in IAsyncIntListener listener);
    oneway void removeScheduledRecording(in ScheduledRecording scheduledRecording, in IAsyncListener listener);
    oneway void getScheduledRecordingData(in IAsyncScheduledRecordingListener listener);
    void clearRecordingList();
    ScheduleRecordingResult scheduleRecording(in ScheduledRecording scheduledRecording);
    ScheduledRecording[] findConflictedRecordings(in ScheduledRecording scheduledRecording);
    ScheduledRecording[] findConflictedRecordingsByTime(in long recordingStartTime, in long recordingEndTime);
    void schedule(in long durationToStartRecording, in ScheduledRecording scheduledRecording);
    oneway void removeAllScheduledRecording(in ScheduledRecording scheduledRecording, in IAsyncListener listener);
    void scheduleWithDailyRepeat(in long durationToStartRecording, in ScheduledRecording scheduledRecordingPrevious);
    void scheduleWithWeeklyRepeat(in long durationToStartRecording, in ScheduledRecording scheduledRecordingPrevious);
    boolean checkRecordingConflict(in long startTime);
    int getId(in ScheduledRecording scheduledRecording);
    void clearRecordingListPvr();
    TvChannel getChannelByIdScheduler(in int channelId);
    void updateConflictRecordings(in ScheduledRecording recording, in boolean addRemove);
    oneway void getRecList(in IAsyncScheduledRecordingListener listener);
    oneway void hasScheduledRec(in TvEvent tvEvent, in IAsyncListener listener);
    oneway void getEventId(in TvChannel tvChannel, in int eventId, in IAsyncTvEventListener listener);
    boolean isInReclist(in int channelId, in long startTime);
    boolean isInConflictedList(in ScheduledRecording scheduledRecording);
    oneway void getScheduledRecordingsList(in IAsyncScheduledRecordingListener listener);
    void removeScheduledRecordingForDeletedChannels();
    void reload();
    void loadScheduledRecording();
    oneway void getScheduledRecListCount(in IAsyncIntListener listener);
    ScheduledRecording getNewRec();

    //------------------------ Scheduled Interface API -------------------------------------------
    oneway void getScheduledRecordingsListScheduledInterface(in IAsyncScheduledRecordingListener listener);
    oneway void getScheduledRemindersListScheduledInterface(in IAsyncScheduledReminderListener listener);

    //------------------------ HbbTv Interface API -------------------------------------------
    void enableHbbTv();
    boolean sendKeyToHbbTvEngine(in int keyCode, in KeyEvent event, in HbbTvKeyEventType type);
    boolean isHbbTvActive();
    void supportHbbTv(in boolean isEnabled);
    void disableHbbTvTracking(in boolean isEnabled);
    void cookieSettingsHbbTv(in HbbTvCookieSettingsValue value);
    void persistentStorageHbbTv(in boolean isEnabled);
    void blockTrackingSitesHbbTv(in boolean isEnabled);
    void deviceIdHbbTv(in boolean isEnabled);
    void resetDeviceIdHbbTv();
    void registerHbbTvUnusedKeyReply(in IAsyncHbbTvUnusedKeyReplyListener listener);
    void setHbbTvSurfaceView(in IBinder binder, in int displayId, in int sessionId);
    void setTvViewHbbTv(in ServiceTvView tvView, in float windowWidth, in float windowHeight);
    boolean getHbbtvFunctionSwitch();
    boolean getHbbtvDoNotTrack();

    //------------------------ Parental Control Settings Interface API -----------------------------
    void setTvInputInterface(in ITvInputInterface tvInputInterface);
    boolean isParentalControlsEnabled();
    void setParantalControlsEnabled(in boolean enabled);
    boolean isAnokiParentalControlsEnabled();
    void setAnokiParentalControlsEnabled(in boolean enabled);
    void setContentRatingSystemEnabled(in ITvInputInterface tvInputInterface, in ContentRatingSystem contentRatingSystem, in boolean enabled);
    boolean isContentRatingSystemEnabled(in ContentRatingSystem contentRatingSystem);
    void setTvContentRatingSystemEnabled(in String tvContentRating, in boolean enabled);
    boolean isTvContentRatingSystemEnabled(in String tvContentRating);
    boolean hasContentRatingSystemSet();
    void loadRatings();
    TvContentRating[] getRatings();
    void clearRatingBeforeSetRating();
    void setContentRatingLevelWithTvInputInterface(in int level);
    boolean setRatingBlocked(in ContentRatingSystem contentRatingSystem, in ContentRatingSystemRating rating, in boolean blocked);
    boolean isRatingBlocked(in TvContentRating[] ratings);
    boolean isRatingSystemRatingBlocked(in ContentRatingSystem contentRatingSystem, in ContentRatingSystemRating rating);
    boolean isTvContentRatingBlocked(in String tvContentRating);
    boolean setTvContentRatingBlocked(in String tvContentRating, in boolean blocked);
    void setTvContentRatingRelativeRatingsEnabled(in String tvContentRating, in boolean enabled);
    void setTvContentRatingRelativeRating2SubRatingEnabled(in String tvContentRating, in boolean enabled);
    boolean setSubRatingBlocked(in ContentRatingSystem contentRatingSystem, in ContentRatingSystemRating rating, in ContentRatingSystemSubRating subRating, in boolean blocked);
    boolean isSubRatingEnabled(in ContentRatingSystem contentRatingSystem, in ContentRatingSystemRating rating, in ContentRatingSystemSubRating subRating);
    int getBlockedStatus(in ContentRatingSystem contentRatingSystem, in ContentRatingSystemRating rating);
    void setRelativeRatingsEnabled(in ContentRatingSystem contentRatingSystem, in ContentRatingSystemRating rating, in boolean enabled);
    void setRelativeRating2SubRatingEnabled(in ContentRatingSystem contentRatingSystem, in boolean enabled, in ContentRatingSystemRating relativeRating, in ContentRatingSystemSubRating subRating);
    void removeContentRatingSystem();
    boolean isContentRatingSystemSet();
    int getContentRatingLevel();
    int getContentRatingLevelIndex();
    void setContentRatingLevel(in int level);
    String getGlobalRestrictionValue(in int value);
    String[] getGlobalRestrictionsArray();
    Map<String, String> getRatingsPerRatingLevelMap();
    Map<String, String> getRatingsSubratingsPerRatingLevelMap();
    int blockTvInputCount();
    void firstTimeParental();
    void blockInput(in boolean selected, in InputSourceData item);
    boolean isBlockSource(in int hardwareId);
    int getBlockUnrated();
    void setBlockUnrated(in boolean isBlockUnrated);
    String[] getRRT5Regions();
    String[] getRRT5Dim(in int index);
    String[] getRRT5Level(in int countryIndex, in int dimIndex);
//    int[20][20] getSelectedItemsForRRT5Level();
  //  int[20][20] rrt5BlockedList(in int regionPosition, in int position);
    void setSelectedItemsForRRT5Level(in int regionIndex, in int dimIndex, in int levelIndex);
    String[] getRRT5LevelInfo();
    void setAnokiRatingLevel(in int level, in boolean temporary);
    int getAnokiRatingLevel();
    String[] getAnokiRatingList();
    boolean isEventLocked(in TvEvent tvEvent);
    void resetRRT5();

    //------------------------ Input Source Interface API ------------------------------------------
    void setup(in boolean isFactoryMode);
    void disposeInputSource();
    InputItem[] getInputList();
    int[] getAvailableInpuList();
    void setValueChanged(in boolean show);
    void setTvViewInputSource(in ServiceTvView tvView);
    void handleInputSource(in String inputSelected, in String inputUrl);
    String getDefaultValue();
    String getDefaultURLValue();
    boolean isFactoryMode();
    int getHardwareID(in String inputName);
    void unblockInput();
    void setInputActiveName(in String activeName);
    String getInputActiveName();
    void getUserMode();
    boolean isBasicMode();
    void onApplicationStart();
    void onApplicationStop();
    boolean isParentalEnabledInputSource();
    void setResolutionDetails(in int hdrIndex, in int hdrGamingIndex);
    boolean isCECControlSinkActive();
    boolean isBlock(in String inputName);
    void blockInputInputSource(in boolean selected, in String inputName);
    boolean isUserSetUpComplete();
    int blockInputCount(in InputSourceData[] blockedInputs);
    void dispatchCECKeyEvent(in KeyEvent event);
    InputResolutionItem getResolutionDetailsForUI();
    void handleCecTune(in String inputId);
    void handleCecData(in String hdmiData);
    void requestUnblockContentInputSource();
    void setLastUsedInput(String inputId);
    void exitTVInput(String inputId);

    //------------------------ Language Mapper Interface API ---------------------------------------
    String getLanguageName(in String languageCode);
    String getPreferredLanguageName(in String languageCode);
    String getLanguageCodeByCountryCode(in String countryCode);
    int getTxtDigitalLanguageMapByCountryCode(in String countryCode);
    int getTxtDigitalLanguageMapByPosition(in int position);
    LanguageCode[] getLanguageCodes();
    LanguageCode getDefaultLanguageCode();

    //------------------------ Oad Update Interface API -------------------------------------------
    void enableOad(in boolean enable);
    void startScan();
    void stopScan();
    void startDetect();
    void stopDetect();
    void startDownload();
    void stopDownload();
    void applyOad();
    int getSoftwareVersion();
    oneway void registerListenerOad(in IOadEventListener listener);
    oneway void unregisterListenerOad(in IOadEventListener listener);
    OadEventData checkOdaUpdate(in String eventType, in Bundle eventArgs);

    //------------------------ General Config Interface API ----------------------------------------
    void setupGenralConfig();
    boolean getGeneralSettingsInfo(in String generalParam);
    String getCountryThatIsSelected();
    boolean getEpgMergeStatus();
}