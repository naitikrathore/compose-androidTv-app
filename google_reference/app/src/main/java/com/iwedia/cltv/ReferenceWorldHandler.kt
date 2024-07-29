package com.iwedia.cltv

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.ReferenceApplication.Companion.moduleProvider
import com.iwedia.cltv.anoki_fast.epg.BackFromPlayback
import com.iwedia.cltv.anoki_fast.vod.details.series.SeriesDetailsSceneManager
import com.iwedia.cltv.anoki_fast.vod.details.single_work.SingleWorkDetailsSceneManager
import com.iwedia.cltv.anoki_fast.vod.player.VodBannerSceneManager
import com.iwedia.cltv.manager.*
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.ForYouInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.fast_backend_utils.IpAddressHelper
import com.iwedia.cltv.scene.custom_webview.CustomWebViewManager
import com.iwedia.cltv.scene.player_scene.AudioSubtitlesHelper
import com.iwedia.cltv.utils.TvViewScaler
import com.iwedia.google_reference.manager.CiPopUpManager
import com.iwedia.guide.android.tools.debug.ActionTracker
import com.iwedia.guide.android.tools.debug.debugAdapter.SceneStateTrackerAdapter
import world.SceneData
import world.SceneManager
import world.WorldHandler
import kotlin.concurrent.thread


/**
 * Custom reference world handler
 *
 * @author Veljko Ilkic
 */
class ReferenceWorldHandler(var context: MainActivity) : WorldHandler() {

    object SceneId {
        const val DUMMY = 999
        const val WALKTHROUGH = 9999
        const val INTRO = 0
        const val LIVE = 1
        const val ZAP_BANNER = 2
        const val CHANNEL_SCENE = 3
        const val FTI_SELECT_INPUT_SCAN = 4
        const val FTI_FINISH_SCAN = 5
        const val HOME_SCENE = 6
        const val INFO_BANNER = 7
        const val DIALOG_SCENE = 8
        const val SEARCH = 9
        const val DIGIT_ZAP = 11
        const val DIGIT_ZAP_CONFLICT = 12
        const val PLAYER_SCENE = 13
        const val DETAILS_SCENE = 14
        const val VOD_SINGLE_WORK_DETAILS_SCENE = 26
        const val PVR_BANNER_SCENE = 15
        const val PARENTAL_PIN = 16
        const val VOD_SERIES_DETAILS_SCENE = 17
        const val FAVOURITES_MODIFICATION_SCENE = 18
        const val PREFERENCES_STATUS_SCENE = 19
        const val PREFERENCES_INFO_SCENE = 20
        const val CUSTOM_RECORDING = 21
        const val CHOOSE_PVR_TYPE = 22
        const val PIN_SCENE = 455
        const val AUDIO_SUBTITLE_SCENE = 23
        const val TIMESHIFT_SCENE = 24
        const val CUSTOM_WEB_VIEW_SCENE = 25
        const val VOD_BANNER_SCENE = 27
        const val RCU_SCENE = 181
        const val RENAME_RECORDING_SCENE = 182
        const val EVALUATION_SCENE = 183
        const val EVAL_LICENSE_EXPIRY_SCENE = 184
        const val OPEN_SOURCE_LICENSE_SCENE = 185
        const val RECORDING_CONFLICT_SCENE = 186
        const val REMINDER_CONFLICT_SCENE = 187
        const val RECORDING_WATCHLIST_CONFLICT_SCENE = 188
        const val DEVICE_OPTION_SCENE = 189
        const val INPUT_SELECTED_SCENE = 190
        const val INPUT_PREF_SCENE = 191
        const val INPUT_OR_CHANNEL_LOCKED_SCENE = 192
        const val NO_INTERNET_DIALOG_SCENE = 193
        const val CI_ENCRYPTED_SCENE = 194
        const val MMI_MENU_SCENE = 195
        const val CI_POPUP = 196
        const val CAM_PIN_SCENE = 197
        const val POSTAL_SCENE = 198
        const val OAD_POPUP = 199
        const val FAVOURITE_SCENE = 59
        const val TIMESHIFT_BUFFER_LIMIT_SCENE = 60
    }

    val TAG = javaClass.simpleName
    object WidgetId {
        const val FOR_YOU = 201
        const val ZAP_BANNER = 202
        const val FAVOURITES = 203
        const val RECORDINGS = 204
        const val PVR_BANNER = 205
        const val INFO_BANNER = 206
        const val CHANNEL_LIST = 207
        const val PREFERENCES = 208
        const val PREFERENCES_SETUP = 209
        const val PREFERENCES_AUDIO = 210
        const val PREFERENCES_SUBTITLES = 211
        const val PREFERENCES_CAM_INFO = 212
        const val PREFERENCES_HBBTV_SETTINGS = 213
        const val PREFERENCES_TELETEXT = 214
        const val PREFERENCES_SYSTEM_INFORMATION = 215
        const val PREFERENCES_CUSTOM_RECORDING = 216
        const val PREFERENCES_PARENTAL_CONTROL = 217
        const val PREFERENCES_CLOSED_CAPTIONS = 218
        const val PREFERENCES_PVR_TIMESHIFT = 219
        const val MIN_RCU = 81
        const val SPEED_TEST = 220
        const val PREFERENCES_ADS_TARGETING = 221
        const val PREFERENCES_OPEN_SOURCE_LICENSES = 222
        const val CI_ENCRYPTED = 223
        const val MMI_MENU = 224
        const val CAM_PIN = 225
        const val TERMS_OF_SERVICE = 226
        const val FEEDBACK = 227
    }


    /**
     * Playback state
     */
    enum class PlaybackState {
        PLAYBACK_LIVE, TIME_SHIFT, RECORDING, VOD, PVR_PLAYBACK, VOD_TRAILER
    }

    var playbackState = PlaybackState.PLAYBACK_LIVE

    private lateinit var moduleProvider: ModuleProvider

    fun setModuleProvider(moduleProvider: ModuleProvider) {
        this.moduleProvider = moduleProvider
        BackFromPlayback.setRegionSupportedListener {
            moduleProvider.getFastUserSettingsModule().isRegionSupported()
        }
        moduleProvider.getParentalControlSettingsModule()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun setup() {
        super.setup()
    }

    fun initHelperClasses() {
        AudioSubtitlesHelper.setup(moduleProvider.getTvModule(), moduleProvider.getUtilsModule())
        IpAddressHelper.setNetworkModule(moduleProvider.getNetworkModule())
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun setPlaybackViews() {
        moduleProvider.getPlayerModule().setPlaybackView(context.liveTvView!!)
        moduleProvider.getTimeshiftModule().setLiveTvView(context.liveTvView!!)
        moduleProvider.getPvrModule().setPvrTvView(context.liveTvView!!)
        moduleProvider.getInputSourceMoudle().setTvView(context.liveTvView!!)
    }

    /**
     * Teletext State
     */
    enum class TTXState {
        TTX_AVAILABLE,
        TTX_UNAVAILABLE,
        TTX_INACTIVE,
        TTX_ACTIVE,
        NO_TTX,
        TTX_X_RATED,
        TTX_PAGE_SET,
        TTX_RECEIVE_DATA
    }

    var ttxState = TTXState.TTX_INACTIVE

    /**
     * Action tracker used to track World and World state changes
     */
    private val actionTracker = ActionTracker()

    companion object {

        fun isFastOnly() = moduleProvider!!.getPlatformOSModule().getPlatformName().contains("FAST")



        fun getSceneName(sceneId: Int): String {
            return when (sceneId) {
                SceneId.INTRO -> {
                    "Intro"
                }
                SceneId.LIVE -> {
                    "Live"
                }
                SceneId.ZAP_BANNER -> {
                    "ZapBanner"
                }
                SceneId.CHANNEL_SCENE -> {
                    "ChannelScene"
                }
                SceneId.FTI_SELECT_INPUT_SCAN -> {
                    "Fti scan"
                }
                SceneId.FTI_FINISH_SCAN -> {
                    "Fti finish scan"
                }
                SceneId.HOME_SCENE -> {
                    "Home Scene"
                }
                SceneId.INFO_BANNER -> {
                    "Info banner"
                }
                SceneId.DIALOG_SCENE -> {
                    "Dialog scene"
                }
                SceneId.SEARCH -> {
                    "Search"
                }
                SceneId.DIGIT_ZAP -> {
                    "Digit zap scene"
                }
                SceneId.DIGIT_ZAP_CONFLICT -> {
                    "Digit zap conflict scene"
                }
                SceneId.PLAYER_SCENE -> {
                    "Player scene"
                }
                SceneId.DETAILS_SCENE -> {
                    "Details scene"
                }
                SceneId.PVR_BANNER_SCENE -> {
                    "Pvr banner scene"
                }
                SceneId.PARENTAL_PIN -> {
                    "Parental pin"
                }
                SceneId.FAVOURITES_MODIFICATION_SCENE -> {
                    "Favourites modification"
                }
                SceneId.PREFERENCES_STATUS_SCENE -> {
                    "Preferences status"
                }
                SceneId.PREFERENCES_INFO_SCENE -> {
                    "Preferences info"
                }
                SceneId.DUMMY -> {
                    "Dummy"
                }
                SceneId.WALKTHROUGH -> {
                    "Walkthrough"
                }
                SceneId.CUSTOM_RECORDING -> {
                    "Custom Recording"
                }
                SceneId.RCU_SCENE -> {
                    "RCUScene"
                }
                SceneId.RENAME_RECORDING_SCENE -> {
                    "Rename Recording Scene"
                }
                SceneId.PIN_SCENE -> {
                    "PIN Scene"
                }
                SceneId.EVALUATION_SCENE -> {
                    "Evaluation Scene"
                }
                SceneId.EVAL_LICENSE_EXPIRY_SCENE -> {
                    "EvalLicenceExpiry Scene"
                }
                SceneId.OPEN_SOURCE_LICENSE_SCENE -> {
                    "OpenSourceLicense Scene"
                }
                SceneId.RECORDING_CONFLICT_SCENE -> {
                    "Recording conflict Scene"
                }
                SceneId.AUDIO_SUBTITLE_SCENE -> {
                    "Audio subtitle Scene"
                }
                SceneId.REMINDER_CONFLICT_SCENE -> {
                    "Reminder conflict Scene"
                }
                SceneId.RECORDING_WATCHLIST_CONFLICT_SCENE -> {
                    "Recording watchlist conflict Scene"
                }
                SceneId.DEVICE_OPTION_SCENE-> {
                    "Device Option"
                }
                SceneId.TIMESHIFT_SCENE -> {
                    "Timeshift Scene"
                }

                SceneId.INPUT_SELECTED_SCENE-> {
                    "Input Selected Source"
                }
                SceneId.INPUT_PREF_SCENE-> {
                    "Input Preference Source"
                }
                SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE -> {
                    "PIN Scene"
                }
                SceneId.CI_ENCRYPTED_SCENE -> {
                    return "Ci Encrypted Scene"
                }
                SceneId.MMI_MENU_SCENE-> {
                    return "Mmi Menu Scene"
                }

                SceneId.CI_POPUP -> {
                    return "Ci Popup"
                }
                SceneId.CAM_PIN_SCENE-> {
                    return "Cam Pin Scene"
                }
                SceneId.OAD_POPUP-> {
                    return "Over air update popup"
                }
                SceneId.FAVOURITE_SCENE -> {
                    "Favorite Scene"
                }
                SceneId.TIMESHIFT_BUFFER_LIMIT_SCENE -> {
                    "Timeshift buffer limit Scene"
                }

                SceneId.VOD_SERIES_DETAILS_SCENE -> {
                    return "Series details scene"
                }

                SceneId.VOD_SINGLE_WORK_DETAILS_SCENE -> {
                    return "Single work details scene"
                }

                SceneId.VOD_BANNER_SCENE -> {
                    "Vod Scene"
                }

                else -> "Scene name is not found!"
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun getNewSceneManager(sceneManagerId: Int): SceneManager {
        if (sceneManagerId == SceneId.EVALUATION_SCENE) {
            return EvaluationSceneManager(context, this, moduleProvider.getUtilsModule(), moduleProvider.getTvModule())
        } else if (sceneManagerId == SceneId.EVAL_LICENSE_EXPIRY_SCENE) {
            return EvalLicenceExpirySceneManager(context, this)
        } else if (sceneManagerId == SceneId.OPEN_SOURCE_LICENSE_SCENE) {
            return OpenSourceLicenseSceneManager(context, this)
        } else if (sceneManagerId == SceneId.INTRO) {
            return IntroSceneManager(
                context,
                this,
                moduleProvider.getTvModule(),
                moduleProvider.getUtilsModule(),
                moduleProvider.getInputSourceMoudle(),
                moduleProvider.getParentalControlSettingsModule(),
                moduleProvider.getNetworkModule(),
                moduleProvider.getFastUserSettingsModule(),
                moduleProvider.getCiPlusModule()
            )
        } else if (sceneManagerId == SceneId.LIVE) {
            return LiveManager(
                context,
                this,
                moduleProvider.getTvModule(),
                moduleProvider.getPlayerModule(),
                moduleProvider.getEpgModule(),
                moduleProvider.getPvrModule(),
                moduleProvider.getSchedulerModule(),
                moduleProvider.getWatchlistModule(),
                moduleProvider.getUtilsModule(),
                moduleProvider.getTimeshiftModule(),
                moduleProvider.getEasModule(),
                moduleProvider.getInputSourceMoudle(),
                moduleProvider.getTimeModule(),
                moduleProvider.getClosedCaptionModule(),
                moduleProvider.getParentalControlSettingsModule(),
                moduleProvider.getCategoryModule(),
                moduleProvider.getGeneralConfigModule(),
                moduleProvider.getTTXModule(),
                moduleProvider.getHbbTvModule(),
                moduleProvider.getTextToSpeechModule(),
                moduleProvider.getPlatformOSModule()
            )
        } else if (sceneManagerId == SceneId.ZAP_BANNER) {
            return ZapBannerManager(
                context,
                this,
                moduleProvider.getTvModule(),
                moduleProvider.getEpgModule(),
                moduleProvider.getPlayerModule(),
                moduleProvider.getUtilsModule(),
                moduleProvider.getParentalControlSettingsModule(),
                moduleProvider.getClosedCaptionModule(),
                moduleProvider.getTimeModule(),
                moduleProvider.getCategoryModule(),
                moduleProvider.getGeneralConfigModule(),
                moduleProvider.getTextToSpeechModule()
            )
        } else if (sceneManagerId == SceneId.CHANNEL_SCENE) {
            return ChannelListSceneManager(
                context,
                this,
                moduleProvider!!.getTvModule(),
                moduleProvider!!.getEpgModule(),
                moduleProvider!!.getPvrModule(),
                moduleProvider!!.getFavoriteModule(),
                moduleProvider!!.getPlayerModule(),
                moduleProvider!!.getTimeshiftModule(),
                moduleProvider!!.getUtilsModule(),
                moduleProvider.getParentalControlSettingsModule(),
                moduleProvider.getTvInputModule(),
                moduleProvider!!.getWatchlistModule(),
                moduleProvider.getClosedCaptionModule(),
                moduleProvider.getTimeModule(),
                moduleProvider.getCategoryModule(),
                moduleProvider.getGeneralConfigModule(),
                moduleProvider.getTextToSpeechModule()
            )
        }
        else if (sceneManagerId == SceneId.FTI_SELECT_INPUT_SCAN) {
            return FtiSelectInputScanSceneManager(context, this, moduleProvider.getTvInputModule(), moduleProvider.getPlayerModule())
        }
        else if (sceneManagerId == SceneId.FTI_FINISH_SCAN) {
            return FtiFinishScanSceneManager(context, this)
        } else if (sceneManagerId == SceneId.HOME_SCENE) {
            return HomeSceneManager(
                context,
                this,
                moduleProvider.getPvrModule(),
                moduleProvider.getHbbTvModule(),
                moduleProvider.getFavoriteModule(),
                moduleProvider.getForYouModule(),
                moduleProvider.getTvModule(),
                moduleProvider.getEpgModule(),
                moduleProvider.getWatchlistModule(),
                moduleProvider.getSchedulerModule(),
                moduleProvider.getPreferenceModule(),
                moduleProvider.getTimeshiftModule(),
                moduleProvider.getPlayerModule(),
                moduleProvider.getTvInputModule(),
                moduleProvider.getParentalControlSettingsModule(),
                moduleProvider.getUtilsModule(),
                moduleProvider.getPreferenceChannelsModule(),
                moduleProvider.getClosedCaptionModule(),
                moduleProvider.getCiPlusModule(),
                moduleProvider.getInputSourceMoudle(),
                moduleProvider.getTimeModule(),
                moduleProvider.getSubtitleModule(),
                moduleProvider.getPromotionModule(),
                moduleProvider.getCategoryModule(),
                moduleProvider.getFastUserSettingsModule(),
                moduleProvider.getNetworkModule(),
                moduleProvider.getTTXModule(),
                moduleProvider.getOadUpdateModule(),
                moduleProvider.getGeneralConfigModule(),
                moduleProvider.getTextToSpeechModule(),
                moduleProvider.getPlatformOSModule()
            )
        } else if (sceneManagerId == SceneId.INFO_BANNER) {
            return InfoBannerSceneManager(
                context,
                this,
                moduleProvider.getTvModule(),
                moduleProvider.getEpgModule(),
                moduleProvider.getSchedulerModule(),
                moduleProvider.getPvrModule(),
                moduleProvider.getPlayerModule(),
                moduleProvider.getUtilsModule(),
                moduleProvider.getWatchlistModule(),
                moduleProvider.getTimeshiftModule(),
                moduleProvider.getClosedCaptionModule(),
                moduleProvider.getParentalControlSettingsModule(),
                moduleProvider.getTimeModule(),
                moduleProvider.getGeneralConfigModule(),
                moduleProvider.getTextToSpeechModule()
            )
        } else if (sceneManagerId == SceneId.DIALOG_SCENE) {
            return DialogSceneManager(
                context,
                this,
                moduleProvider.getInputSourceMoudle(),
                moduleProvider.getTextToSpeechModule(),
                moduleProvider.getUtilsModule()
            )
        } else if (sceneManagerId == SceneId.NO_INTERNET_DIALOG_SCENE) {
            return NoInternetChannelDialogSceneManager(
                context,
                this,
                moduleProvider.getTextToSpeechModule(),
                moduleProvider.getUtilsModule()
            )
        } else if (sceneManagerId == SceneId.SEARCH) {
            return SearchSceneManager(
                context,
                this,
                moduleProvider.getSearchModule(),
                moduleProvider.getTvModule(),
                moduleProvider.getEpgModule(),
                moduleProvider.getPvrModule(),
                moduleProvider.getParentalControlSettingsModule(),
                moduleProvider.getTimeModule(),
                moduleProvider.getWatchlistModule(),
                moduleProvider.getUtilsModule(),
                moduleProvider.getNetworkModule(),
                moduleProvider.getSchedulerModule(),
                moduleProvider.getTextToSpeechModule()
            )
        } else if (sceneManagerId == SceneId.DIGIT_ZAP) {
            return ZapDigitManager(
                context,
                this,
                moduleProvider.getTvModule(),
                moduleProvider.getTimeshiftModule(),
                moduleProvider.getCategoryModule(),
                moduleProvider.getUtilsModule(),
                moduleProvider.getTextToSpeechModule()
            )
        } else if (sceneManagerId == SceneId.DIGIT_ZAP_CONFLICT) {
            return DigitZapConflictManager(context, this, moduleProvider.getTvModule())
        } else if (sceneManagerId == SceneId.PLAYER_SCENE) {
            return PlayerSceneManager(
                context,
                this,
                moduleProvider.getPvrModule(),
                moduleProvider.getPlayerModule(),
                moduleProvider.getTvModule(),
                moduleProvider.getUtilsModule(),
                moduleProvider.getGeneralConfigModule(),
                moduleProvider.getTextToSpeechModule()
            )
        } else if (sceneManagerId == SceneId.DETAILS_SCENE) {
            return DetailsSceneManager(
                context,
                this,
                moduleProvider.getTvModule(),
                moduleProvider.getPlayerModule(),
                moduleProvider.getPvrModule(),
                moduleProvider.getFavoriteModule(),
                moduleProvider.getWatchlistModule(),
                moduleProvider.getSchedulerModule(),
                moduleProvider.getUtilsModule(),
                moduleProvider.getTvInputModule(),
                moduleProvider.getClosedCaptionModule(),
                moduleProvider.getTimeshiftModule(),
                moduleProvider.getTimeModule(),
                moduleProvider.getCategoryModule(),
                moduleProvider.getInputSourceMoudle(),
                moduleProvider.getGeneralConfigModule(),
                moduleProvider.getTextToSpeechModule()
            )
        } else if (sceneManagerId == SceneId.PVR_BANNER_SCENE) {
            return PvrBannerSceneManager(
                context, this,
                moduleProvider.getTvModule(),
                moduleProvider.getUtilsModule(),
                moduleProvider.getPvrModule()
            )
        } else if (sceneManagerId == SceneId.PARENTAL_PIN) {
            return ParentalPinSceneManager(context, this,moduleProvider.getUtilsModule(), moduleProvider.getTextToSpeechModule())
        }else if (sceneManagerId == SceneId.POSTAL_SCENE) {
            return PostalCodeSceneManager(context, this,moduleProvider.getUtilsModule())
        } else if (sceneManagerId == SceneId.FAVOURITES_MODIFICATION_SCENE) {
            return FavouritesListModificationSceneManager(context, this)
        } else if (sceneManagerId == SceneId.PREFERENCES_STATUS_SCENE) {
            return PreferencesStatusSceneManager(context, this)
        } else if (sceneManagerId == SceneId.PREFERENCES_INFO_SCENE) {
            return PreferencesInfoSceneManager(context, this)
        } else if (sceneManagerId == SceneId.CHOOSE_PVR_TYPE) {
            return ChoosePvrTypeManager(context, this, moduleProvider.getTextToSpeechModule())
        }  else if (sceneManagerId == SceneId.CUSTOM_WEB_VIEW_SCENE) {
            return CustomWebViewManager(context, this)
        } else if (sceneManagerId == SceneId.CUSTOM_RECORDING) {
            return CustomRecordingSceneManager(context, this,
                moduleProvider.getEpgModule(),
                moduleProvider.getSchedulerModule(),
                moduleProvider.getTvModule(),
                moduleProvider.getTimeModule(),
                moduleProvider.getTextToSpeechModule(),
                moduleProvider.getUtilsModule())
        } else if (sceneManagerId == SceneId.RCU_SCENE) {
            return RCUManager(
                context,
                this,
                moduleProvider.getTimeModule(),
                moduleProvider.getTTXModule(),
                moduleProvider.getTextToSpeechModule()
            )
        } else if (sceneManagerId == SceneId.RENAME_RECORDING_SCENE) {
            return RenameRecordingSceneManager(context, this)
        } else if (sceneManagerId == SceneId.PIN_SCENE) {
            return PinSceneManager(
                context,
                this,
                moduleProvider.getUtilsModule(),
                moduleProvider.getTextToSpeechModule()
            )
        } else if (sceneManagerId == SceneId.RECORDING_CONFLICT_SCENE) {
            return RecordingConflictSceneManager(
                context,
                this,
                moduleProvider.getSchedulerModule(),
                moduleProvider.getEpgModule(),
                moduleProvider.getTvModule(),
                moduleProvider.getPvrModule(),
                moduleProvider.getUtilsModule(),
                moduleProvider.getTimeModule(),
                moduleProvider.getTextToSpeechModule()
            )
        } else if (sceneManagerId == SceneId.AUDIO_SUBTITLE_SCENE) {
            return AudioSubtitleSceneManager(
                context,
                this,
                moduleProvider.getTvModule(),
                moduleProvider.getPlayerModule(),
                moduleProvider.getUtilsModule(),
                moduleProvider.getTextToSpeechModule()
            )
        } else if (sceneManagerId == SceneId.WALKTHROUGH) {
            return WalkthroughSceneManager(context, this, moduleProvider.getUtilsModule(), moduleProvider.getGeneralConfigModule())
        } else if (sceneManagerId == SceneId.REMINDER_CONFLICT_SCENE) {
            return ReminderConflictSceneManager(context, this, moduleProvider.getTvModule(), moduleProvider.getUtilsModule(), moduleProvider.getTextToSpeechModule())
        } else if (sceneManagerId == SceneId.RECORDING_WATCHLIST_CONFLICT_SCENE) {
            return RecordingWatchlistConflictSceneManager(context, this,
                      moduleProvider.getTvModule(), moduleProvider.getPvrModule(), moduleProvider.getTimeshiftModule(), moduleProvider.getTimeModule(), moduleProvider.getUtilsModule(), moduleProvider.getTextToSpeechModule())
        } else if(sceneManagerId == SceneId.DEVICE_OPTION_SCENE){
            return DeviceOptionSceneManager(
                context,
                this,
                moduleProvider.getUtilsModule(),
                moduleProvider.getPvrModule(),
                moduleProvider.getTextToSpeechModule()
            )
        } else if (sceneManagerId == SceneId.TIMESHIFT_SCENE) {
            return TimeshiftSceneManager(
                context,
                this,
                moduleProvider.getPlayerModule(),
                moduleProvider.getTvModule(),
                moduleProvider.getTimeshiftModule(),
                moduleProvider.getUtilsModule(),
                moduleProvider.getGeneralConfigModule(),
                moduleProvider.getTextToSpeechModule()
            )
        } else if (sceneManagerId == SceneId.INPUT_SELECTED_SCENE) {
            return InputSelectedSceneManager(context, this, moduleProvider.getInputSourceMoudle())
        } else if (sceneManagerId == SceneId.INPUT_PREF_SCENE) {
            return InputPrefSceneManager(
                context, this,
                moduleProvider.getHbbTvModule(),
                moduleProvider.getTvModule(),
                moduleProvider.getPreferenceModule(),
                moduleProvider.getTvInputModule(),
                moduleProvider.getParentalControlSettingsModule(),
                moduleProvider.getUtilsModule(),
                moduleProvider.getCiPlusModule(),
                moduleProvider.getClosedCaptionModule(),
                moduleProvider.getPreferenceChannelsModule(),
                moduleProvider.getInputSourceMoudle(),
                moduleProvider.getTimeModule(),
                moduleProvider.getNetworkModule(),
                moduleProvider.getGeneralConfigModule(),
                moduleProvider.getTextToSpeechModule()
            )
        }else if (sceneManagerId == SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE){
            return ParentalLockedChannelManager(
                context,
                this,
                moduleProvider.getUtilsModule(),
                moduleProvider.getTextToSpeechModule()
            )
        }else if(sceneManagerId == SceneId.CI_ENCRYPTED_SCENE){
            return CiEncryptedManager(context,this, moduleProvider.getCiPlusModule(), moduleProvider.getTvModule())
        }else if(sceneManagerId == SceneId.MMI_MENU_SCENE){
            return MmiMenuSceneManager(context,this, moduleProvider.getCiPlusModule())
        }else if(sceneManagerId == SceneId.CI_POPUP){
            return CiPopUpManager(context,this, moduleProvider.getCiPlusModule(), moduleProvider.getUtilsModule())
        }else if(sceneManagerId == SceneId.CAM_PIN_SCENE){
            return CamPinManager(context,this, moduleProvider.getCiPlusModule(), moduleProvider.getTextToSpeechModule())
        } else if(sceneManagerId == SceneId.OAD_POPUP) {
            return OadPopUpManager(context,this)
        } else if (sceneManagerId == SceneId.FAVOURITE_SCENE) {
            return FavoriteSceneManager(
                context,
                this,
                moduleProvider.getTvModule(),
                moduleProvider.getCategoryModule(),
                moduleProvider.getFavoriteModule(),
                moduleProvider.getTextToSpeechModule()
            )
        } else if (sceneManagerId == SceneId.VOD_SERIES_DETAILS_SCENE) {
            return SeriesDetailsSceneManager(
                context,
                this,
                moduleProvider.getPlayerModule(),
                moduleProvider.getNetworkModule()
            )
        } else if (sceneManagerId == SceneId.VOD_SINGLE_WORK_DETAILS_SCENE) {
            return SingleWorkDetailsSceneManager(
                context,
                this,
                moduleProvider.getPlayerModule(),
                moduleProvider.getNetworkModule()
            )
        }else if(sceneManagerId == SceneId.VOD_BANNER_SCENE) {
            return VodBannerSceneManager(
                context,
                this,
                moduleProvider.getPlayerModule(),
                moduleProvider.getTvModule(),
                moduleProvider.getNetworkModule()
            )
        } else if (sceneManagerId == SceneId.TIMESHIFT_BUFFER_LIMIT_SCENE) {
            return TimeshiftBufferLimitSceneManager(
                context,
                this,
                moduleProvider.getTimeshiftModule()
            )
        }

        return DummyManager(context, this)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun scaleTvView(action: Int) {
        if (action == SceneManager.Action.SHOW || action == SceneManager.Action.SHOW_OVERLAY) {

            //resizing of playback in fast channel on refplus 5 boards causing black screen in homescene,
            //so disabling the resizing in that case
            if (BuildConfig.FLAVOR.contains("refplus5")
                && (ReferenceApplication.worldHandler as ReferenceWorldHandler).getApplicationMode()==ApplicationMode.FAST_ONLY.ordinal) {
                return
            }
            TvViewScaler.scaleDownTvView()
        } else if (action == SceneManager.Action.HIDE || action == SceneManager.Action.DESTROY) {
            TvViewScaler.scaleUpTvView()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun hbbtvFocusHandling(sceneManagerId: Int, action: Int) {
        thread {
            if (action == SceneManager.Action.SHOW || action == SceneManager.Action.SHOW_OVERLAY) {
                if (sceneManagerId == SceneId.LIVE) {
                    moduleProvider.getHbbTvModule().setHbbTVFocus(focused = true, fullScreen = true)
                } else {
                    if (sceneManagerId == SceneId.HOME_SCENE) {
                        moduleProvider.getHbbTvModule().setHbbTVFocus(focused = false, fullScreen = false)
                    } else {
                        moduleProvider.getHbbTvModule().setHbbTVFocus(focused = false, fullScreen = true)
                    }
                }
            } else if (action == SceneManager.Action.HIDE || action == SceneManager.Action.DESTROY) {
                moduleProvider.getHbbTvModule().setHbbTVFocus(focused = true, fullScreen = true)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun triggerAction(sceneManagerId: Int, action: Int) {
        try {
            if (sceneManagerId == SceneId.HOME_SCENE) {
                scaleTvView(action)
            }
            hbbtvFocusHandling(sceneManagerId, action)
            super.triggerAction(sceneManagerId, action)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    override fun triggerActionWithDataOverride(
        sceneManagerId: Int,
        sceneManagerInstanceId: Int,
        action: Int,
        data: SceneData?,
        isDataOverride: Boolean
    ) {
        if (sceneManagerId == SceneId.HOME_SCENE) {
            scaleTvView(action)
        }
        hbbtvFocusHandling(sceneManagerId, action)

        super.triggerActionWithDataOverride(
            sceneManagerId,
            sceneManagerInstanceId,
            action,
            data,
            isDataOverride
        )
        when (action) {
            SceneManager.Action.SHOW -> {
                actionTracker.logAction(
                    getSceneName(sceneManagerId),
                    SceneStateTrackerAdapter.SHOW
                )
            }

            SceneManager.Action.HIDE -> {
                actionTracker.logAction(
                    getSceneName(sceneManagerId),
                    SceneStateTrackerAdapter.HIDE
                )
            }

            SceneManager.Action.DESTROY -> {
                actionTracker.logAction(
                    getSceneName(sceneManagerId),
                    SceneStateTrackerAdapter.DESTROY
                )
            }

            SceneManager.Action.SHOW_OVERLAY -> {
                actionTracker.logAction(
                    getSceneName(sceneManagerId),
                    SceneStateTrackerAdapter.SHOW_OVERLAY
                )
            }

            SceneManager.Action.SHOW_GLOBAL -> {
                actionTracker.logAction(
                    getSceneName(sceneManagerId),
                    SceneStateTrackerAdapter.SHOW_GLOBAL
                )
            }
        }
    }

    override fun destroyExisting() {
        super.destroyExisting()
        actionTracker.logDestroyExisting()
    }

    override fun destroyOtherExisting(skipManagerId: Int) {
        try {
            super.destroyOtherExisting(skipManagerId)
            actionTracker.logDestroyOtherExisting(getSceneName(skipManagerId))
        } catch (ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "destroyOtherExisting: Destroy other raised exception, ignoring")
        }
    }

    override fun returnTo(id: Int, isShow: Boolean, returnToInstance: Boolean) {
        super.returnTo(id, isShow, returnToInstance)
        actionTracker.logReturnTo(getSceneName(id), isShow, returnToInstance)
    }

    override fun revertHistory(steps: Int, isShow: Boolean, returnToInstance: Boolean) {
        super.revertHistory(steps, isShow, returnToInstance)
        actionTracker.logRevertHistory(steps, isShow, returnToInstance)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getApplicationMode(): Int {
        //TODO DejanN remove this for the hybrid use case
        return if (isFastOnly()) ApplicationMode.FAST_ONLY.ordinal
        else moduleProvider.getUtilsModule().getApplicationMode()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun isFastOnly(): Boolean = BuildConfig.FLAVOR.contains("base") || moduleProvider.getPlatformOSModule().getPlatformName().contains("FAST")
    @RequiresApi(Build.VERSION_CODES.R)
    fun isT56() = BuildConfig.FLAVOR.contains("t56") || moduleProvider.getPlatformOSModule().getPlatformName().contains("t56")

    /**
     * Check is scene already shown
     *
     * @param sceneId
     * @return is scene shown
     */
    fun isAlreadyShown(sceneId: Int): Boolean {
        return stateTracker?.active?.scene?.id == SceneId.HOME_SCENE && sceneId == SceneId.HOME_SCENE
    }

    fun getPlatformName() = moduleProvider.getPlatformOSModule().getPlatformName()

    /**
     * A set of scene IDs representing Video on Demand (VOD) scenes.
     */
    private val vodScenesSet = setOf(
        SceneId.VOD_BANNER_SCENE,
        SceneId.VOD_SERIES_DETAILS_SCENE,
        SceneId.VOD_SINGLE_WORK_DETAILS_SCENE
    )

    /**
     * Checks if the current active scene is a Video on Demand (VOD) scene.
     *
     * @return `true` if the active scene is a VOD scene, `false` otherwise.
     */
    fun isVodScene() = stateTracker?.active?.scene?.id in vodScenesSet
}