package com.iwedia.cltv.components

import com.iwedia.cltv.platform.model.TvChannel

/**
 * @author Gaurav Jain
 * classes in this file is used for preference ui
 */

class PrefItem<T>(
    val viewHolderType: ViewHolderType,
    var id: Pref,
    val data: T,
    val listener: PrefItemListener
)

/**
 * this enum tells us type of our viewholder as we are using single adapter for each type of views in prefrence
 */
enum class ViewHolderType{
    VT_MENU,
    VT_SWITCH,
    VT_RADIO,
    VT_CHECKBOX,
    VT_SEEKBAR,
    VT_EDIT_CHANNEL //added for channel- move,swap,skip etc.
}


//used for switch,radio,checkbox
open class CompoundItem(val title:String, var isChecked:Boolean,val id:Int, val compoundListener: PrefCompoundListener, val channelType: String? = null){
    val hiddenOptions: MutableList<PrefItem<Any>> = mutableListOf()
}


//used for switch,radio,checkbox
class ChannelItem(title:String, isChecked:Boolean,id:Int, compoundListener: PrefCompoundListener,val tvChannel:TvChannel,
                  val editChannel: EditChannel, val editChannelListener:EditChannelListener?=null
) :CompoundItem(title, isChecked, id, compoundListener
) {
    var isEnabled = true
    var isLongPressed = false
}

/**
 * used mainly for edit channel ui handling
 */

enum class EditChannel{
    CHANNEL_SWAP,
    CHANNEL_MOVE,
    CHANNEL_SKIP,
    CHANNEL_DELETE
}

//used for seekbar
class SeekBarItem(val title:String, var progress:Int, val seekBarListener: PrefSeekBarListener){}

/**
 * data class for root item
 */
class RootItem(
    val name:String,
    var info:String?,
    val showArrow:Boolean,
    val itemList:MutableList<PrefItem<Any>>? = null,
    val infoListener: InfoListener? = null,
){
    var isEnabled = true
}

/**
 * used to update information for respective viewholder
 * like when some radio item is selected we need to update that information in view
 */
interface InfoListener{
    fun getInfo() : String?
}

/**
 * this contains the ids for the each switch, radio groups, items, to uniquely define them
 */
enum class Pref{
    CHANNEL_SCAN,
    CHANNEL_EDIT,
    DEFAULT_CHANNEL,
    LCN,
    ANTENNA,
    PARENTAL_CONTROL,
    INTERACTION_CHANNEL,
    MHEG_PIN,
    EVALUATION_LICENCE,
    OPEN_SOURCE,
    DISPLAY_MODE,
    ASPECT_RATIO,
    AUDIO_DESCRIPTION_KONKA,
    FIRST_LANGUAGE,
    SECOND_LANGUAGE,
    AUDIO_DESCRIPTION,
    GENERAL,
    CLOSED_CAPTIONS,
    DIGITAL_LANGUAGE,
    DECODING_PAGE_LANGUAGE,
    HBBTV_SUPPORT,
    TRACK,
    COOKIE_SETTING,
    COOKIE_SETTING_RADIO,
    PERSISTENT_STORAGE,
    BLOCK_TRACKING_SITES,
    DEVICE_ID,
    RESET_DEVICE_ID,
    CAM_MENU,
    CAM_SUB_MENU,
    CAM_PIN,
    CAM_SCAN,
    USER_PREFERENCE,
    USER_PREFERENCE_RADIO,
    CAM_TYPE_PREFERENCE,
    CAM_TYPE_PREFERENCE_RADIO,
    CAM_OPERATOR,
    CAM_OPERATOR_MENU,
    PREFERRED_EPG_LANGUAGE,
    BLUE_MUTE,
    OAD_UPDATE,
    NO_SIGNAL_AUTO_POWER_OFF,
    CHANNEL_BLOCK,
    INPUT_BLOCK,
    RATING_SYSTEMS,
    RATING_LOCK,
    RRT5_LOCK,
    CHANGE_PIN,
    HEARING_IMPAIRED,
    VISUALLY_IMPAIRED,
    DISPLAY_CC,
    CAPTION_SERVICES,
    ADVANCED_SELECTION,
    TEXT_SIZE,
    FONT_FAMILY,
    TEXT_COLOR,
    TEXT_OPACITY,
    EDGE_TYPE,
    EDGE_COLOR,
    BACKGROUND_COLOR,
    BACKGROUND_OPACITY,
    DEVICE_INFO,
    DEVICE_INFO_ITEM,
    TIMESHIFT_MODE,

    /*additional menus*/
    DEFAULT_CHANNEL_SWITCH,
    DEFAULT_CHANNEL_RADIO,
    LCN_SWITCH,
    BLUE_MUTE_SWITCH,
    ANTENNA_SWITCH,
    DISPLAY_MODE_RADIO,
    INTERACTION_CHANNEL_SWITCH,
    NO_SIGNAL_AUTO_POWER_OFF_SWITCH,
    NO_SIGNAL_AUTO_POWER_OFF_RADIO,
    FIRST_LANGUAGE_RADIO,
    SECOND_LANGUAGE_RADIO,
    PREFERRED_EPG_LANGUAGE_RADIO,
    AUDIO_DESCRIPTION_SWITCH,
    AUDIO_DESCRIPTION_RADIO,
    GENERAL_RADIO,
    GENERAL_SWITCH,
    PARENTAL_CONTROL_SWITCH,
    CHANNEL_BLOCK_CHECKBOX,
    INPUT_BLOCK_CHECKBOX,
    RATING_SYSTEMS_CHECKBOX,
    ANOKI_RATING_SYSTEM_RADIO,
    AUDIO_FORMAT_RADIO,
    AUDIO_FORMAT,
    ENABLE_SUBTITLES,
    ENABLE_SUBTITLES_SWITCH,
    SUBTITLE_TYPE,
    SUBTITLE_TYPE_RADIO,
    AUDIO_DESCRIPTION_KONKA_SWITCH,
    VISUALLY_IMPAIRED_SWITCH,
    HEARING_IMPAIRED_SWITCH,
    SPEAKER_SWITCH,
    HEADPHONE_SWITCH,
    PANE_FADE_SWITCH,
    FADER_CONTROL,
    AUDIO_FOR_VISUALLY_IMPAIRED,
    VOLUME_SEEKBAR,
    FADER_CONTROL_RADIO,
    DIGITAL_LANGUAGE_RADIO,
    DECODING_PAGE_LANGUAGE_RADIO,
    PREFERED_TELETEXT_LANGUAGE,
    PREFERED_TELETEXT_LANGUAGE_RADIO,
    TIMESHIFT_MODE_SWITCH,
    HBBTV_SUPPORT_SWITCH,
    TRACK_SWITCH,
    PERSISTENT_STORAGE_SWITCH,
    BLOCK_TRACKING_SITES_SWITCH,
    DEVICE_ID_SWITCH,
    DISPLAY_CC_SWITCH,
    WITH_MUTE_CHECKBOX,
    CLOSED_CAPTIONS_RADIO,
    CAPTION_SERVICES_RADIO,
    ADVANCED_SELECTION_RADIO,
    TEXT_SIZE_RADIO,
    FONT_FAMILY_RADIO,
    TEXT_COLOR_RADIO,
    TEXT_OPACITY_RADIO,
    EDGE_TYPE_RADIO,
    EDGE_COLOR_RADIO,
    BACKGROUND_COLOR_RADIO,
    BACKGROUND_OPACITY_RADIO,
    SKIP_CHANNEL_CHECKBOX,
    CHANNEL_EDIT_KONKA,
    SKIP_CHANNEL,
    CLEAR_CHANNELS,
    DELETE_CHANNEL_CHECKBOX,
    DELETE_CHANNEL,
    SWAP_CHANNEL,
    SWAP_CHANNELS_ITEM,
    MOVE_CHANNEL_ITEM,
    MOVE_CHANNEL,
    AUTO_SERVICE_UPDATE_SWITCH,
    BLOCK_UNRATED_PROGRAMS_CHECKBOX,
    RATING_LOCK_GLOBAL_RESTRICTIONS,
    RATING_LOCK_COUNTRY,
    GLOBAL_RESTRICTIONS_RADIO,
    RATING_LOCK_CHECKBOX,
    RATING_LOCK_GROUP,
    RATING_LOCK_GROUP_CHECKBOX,
    RATING_LOCK_GROUP_CHECKBOX_ALL,
    RRT5_RESET,
    RRT5_REGION,
    RRT5_DIM,
    RRT5_LEVEL_CHECKBOX,
    CHANNELS,
    AUDIO_FOR_VISUALLY_IMPAIRED_RADIO,
    CHANNELS_SETTING,
    PICTURE,
    SOUND,
    SCREEN,
    POWER,
    SOUND_TRACKS,
    SOUND_TRACKS_RADIO,
    AUDIO_TYPE,
    AUDIO_TYPE_RADIO,
    ANALOG_SUBTITLE,
    ANALOG_SUBTITLE_SWITCH,
    DIGITAL_SUBTITLE,
    DIGITAL_SUBTITLE_RADIO,
    SUBTITLE_TRACKS,
    SUBTITLE_TRACKS_RADIO,
    DIGITAL_SUBTITLE_LANGUAGE,
    DIGITAL_SUBTITLE_LANGUAGE_RADIO,
    DIGITAL_SUBTITLE_LANGUAGE_2,
    DIGITAL_SUBTITLE_LANGUAGE_2_RADIO,
    ADS_TARGETING,
    ADS_TARGETING_SWITCH,
    ASPECT_RATIO_RADIO,
    POSTAL_CODE,
    PRIVACY_POLICY,
    TERMS_OF_SERVICE,
    FEEDBACK
}