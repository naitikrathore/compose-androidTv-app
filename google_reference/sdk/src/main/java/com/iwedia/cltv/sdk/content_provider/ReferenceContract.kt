package com.iwedia.cltv.sdk.content_provider

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

class ReferenceContract private constructor() {
    companion object {
        fun buildChannelsUri(id: Long) : Uri {
            return ContentUris.withAppendedId(ReferenceContentProvider.CHANNELS_URI, id)
        }

        fun buildConfigUri(id: Long) : Uri {
            return ContentUris.withAppendedId(ReferenceContentProvider.CONFIG_URI, id)
        }

        fun buildOemCustomizationUri(id: Long) : Uri {
            return ContentUris.withAppendedId(ReferenceContentProvider.OEM_CUSTOMIZATION_URI, id)
        }

        fun buildLanguagesUri(id: Long) : Uri {
            return ContentUris.withAppendedId(ReferenceContentProvider.LANGUAGES_URI, id)
        }

        fun buildScheduledRemindersUri(id: Long) : Uri {
            return ContentUris.withAppendedId(ReferenceContentProvider.SCHEDULED_REMINDERS_URI, id)
        }

        fun buildScheduledRecordingsUri(id: Long) : Uri {
            return ContentUris.withAppendedId(ReferenceContentProvider.SCHEDULED_RECORDINGS_URI, id)
        }

        fun buildFavoritesUri(id: Long) : Uri {
            return ContentUris.withAppendedId(ReferenceContentProvider.FAVORITES_URI, id)
        }

        fun buildConfigurableKeysUri(id: Long) : Uri {
            return ContentUris.withAppendedId(ReferenceContentProvider.CONFIGURABLE_KEYS_URI, id)
        }

        fun buildSystemInfoUri(id: Long) : Uri {
            return ContentUris.withAppendedId(ReferenceContentProvider.SYSTEM_INFO_URI, id)
        }
    }

    class Channels private constructor(): BaseColumns {
        companion object {
            const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.iwedia.cltv.platform.model.content_provider.channels"
            const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.iwedia.cltv.platform.model.content_provider.channels"

            /*Channels table columns*/
            const val PACKAGE_NAME_COLUMN = "package_name"
            const val INPUT_ID_COLUMN = "input_id"
            const val TYPE_COLUMN = "type"
            const val SERVICE_TYPE_COLUMN = "service_type"
            const val ORIGINAL_NETWORK_ID_COLUMN = "original_network_id"
            const val TRANSPORT_STREAM_ID_COLUMN = "transport_stream_id"
            const val SERVICE_ID_COLUMN = "service_id"
            const val DISPLAY_NUMBER_COLUMN = "display_number"
            const val NAME_COLUMN = "display_name"
            const val NETWORK_AFFILIATION_COLUMN = "network_affiliation"
            const val DESCRIPTION_COLUMN = "description"
            const val VIDEO_FORMAT_COLUMN = "video_format"
            const val BROWSABLE_COLUMN = "browsable"
            const val SEARCHABLE_COLUMN = "searchable"
            const val LOCKED_COLUMN = "locked"
            const val APP_LINK_ICON_URI_COLUMN = "app_link_icon_uri"
            const val APP_LINK_POSTER_ART_URI_COLUMN = "app_link_poster_art_uri"
            const val APP_LINK_TEXT_COLUMN = "app_link_text"
            const val APP_LINK_COLOR_COLUMN = "app_link_color"
            const val APP_LINK_INTENT_URI_COLUMN = "app_link_intent_uri"
            const val INTERNAL_PROVIDER_ID_COLUMN = "internal_provider_id"
            const val INTERNAL_PROVIDER_DATA_COLUMN = "internal_provider_data"
            const val INTERNAL_PROVIDER_FLAG1_COLUMN = "internal_provider_flag1"
            const val VERSION_NUMBER_COLUMN = "version_number"
            const val TRANSIENT_COLUMN = "transient"
            const val SKIP_COLUMN = "iw_skip"
            const val ORIG_ID_COLUMN = "iw_original_id"
            const val REFERENCE_NAME_COLUMN = "iw_display_name"
            const val DISPLAY_NUMBER_CHANGED_COLUMN = "iw_display_number_changed"
            const val ORDINAL_NUMBER_COLUMN = "ordinal_number"
            const val DELETED_COLUMN = "is_deleted"
        }
    }

    class Config private constructor(): BaseColumns {
        companion object {
            const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.iwedia.cltv.platform.model.content_provider.config"
            const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.iwedia.cltv.platform.model.content_provider.config"
            const val PIN_COLUMN = "pin"
            const val LCN_COLUMN = "lcn"
            const val SCAN_TYPE = "scan_type"
            const val CURRENT_COUNTRY_COLUMN = "current_country"
            const val IS_RECORDING_STARTED = "is_recording_started"
        }
    }

    class SystemInfo private constructor(): BaseColumns {
        companion object {
            const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.iwedia.cltv.platform.model.content_provider.system_info"
            const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.iwedia.cltv.platform.model.content_provider.system_info"
            const  val RF_CHANNEL_NUMBER_ENABLED_COLUMN = "rf_channel_number_enabled"
            const  val BER_ENABLED_COLUMN = "ber_enabled"
            const  val FREQUENCY_ENABLED_COLUMN = "frequency_enabled"
            const  val PROG_ENABLED_COLUMN = "prog_enabled"
            const  val UEC_ENABLED_COLUMN = "uec_enabled"
            const  val SERVICEID_ENABLED_COLUMN = "serviceId_enabled"
            const  val POSTVITERBI_ENABLED_COLUMN = "postViterbi_enabled"
            const  val TSID_ENABLED_COLUMN = "tsId_enabled"
            const  val FIVES_ENABLED_COLUMN = "fiveS_enabled"
            const  val ONID_ENABLED_COLUMN = "onId_enabled"
            const  val AGC_ENABLED_COLUMN = "agc_enabled"
            const  val NETWORKID_ENABLED_COLUMN = "networkId_enabled"
            const  val NETWORKNAME_ENABLED_COLUMN = "networkName_enabled"
            const  val BANDWIDTH_ENABLED_COLUMN = "bandwidth_enabled"
        }
    }

    class OemCustomization private constructor(): BaseColumns {
        companion object {
            const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.iwedia.cltv.platform.model.content_provider.oem_customization"
            const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.iwedia.cltv.platform.model.content_provider.oem_customization"
            const val PVR_ENABLED_COLUMN = "pvr_enabled"
            const val VIRTUAL_RCU_COLUMN = "virtual_rcu"
            const val VERTICAL_EPG_COLUMN = "vertical_epg"
            const val EPG_CELL_MERGE_COLUMN = "epg_cell_merge"
            const val CHANNEL_LOGO_METADATA_ENABLED_COLUMN = "channel_logo_metadata_enabled"
            const val SCAN_TYPE = "scan_type"
            const val SUBTITLE_COLUMN_ID = "subtitle"
            const val SWAP_CHANNEL = "swap_channel"
            const val COUNTRY_SELECT = "country_select"
            const val CLEAR_CHANNELS_SCAN_OPTION_ENABLED_COLUMN = "clear_channels_scan_option_enabled"
            const val ASPECT_RATIO_ENABLED_COLUMN = "aspect_ratio_enabled"
            const val TELETEXT_ENABLE_COLUMN = "teletext_enable_column"
            const val DELETE_THIRD_PARTY_INPUT_COLUMN = "delete_third_party_input_enabled"
            const val TIMESHIFT_ENABLED_COLUMN = "timeshift_enabled"

            /**
             * Colors
             */
            const val COLOR_BACKGROUND_COLUMN = "color_background"
            const val COLOR_NOT_SELECTED_COLUMN = "color_not_selected"
            const val COLOR_TEXT_DESCRIPTION_COLUMN = "color_text_description"
            const val COLOR_MAIN_TEXT_COLUMN = "color_main_text"
            const val COLOR_SELECTOR_COLUMN = "color_selector"
            const val COLOR_PROGRESS_COLUMN = "color_progress"
            const val COLOR_PVR_AND_OTHER_COLUMN = "color_pvr_and_other"
            const val COLOR_GRADIENT_COLUMN = "color_gradient"
            const val COLOR_BUTTON_COLUMN = "color_button"
            /**
             * Branding columns
             */
            const val BRANDING_CHANNEL_LOGO_COLUMN = "branding_channel_logo"
            const val BRANDING_COMPANY_NAME_COLUMN = "branding_company_name"
            const val BRANDING_WELCOME_MESSAGE_COLUMN = "branding_welcome_message"
            /**
             * Fonts
             */
            const val FONT_REGULAR = "font_regular"
            const val FONT_MEDIUM = "font_medium"
            const val FONT_BOLD = "font_bold"
            const val FONT_LIGHT = "font_light"

            const val THIRD_PARTY_INPUT_ENABLED_COLUMN = "third_party_input_enabled"
            const val TERRESTRIAL_SCAN_ENABLED_COLUMN = "terrestrial_scan_enabled"
            const val CABLE_SCAN_ENABLED_COLUMN = "cable_scan_enabled"
            const val SATELLITE_SCAN_ENABLED_COLUMN = "satellite_scan_enabled"
            const val SCAN_TYPE_SWITCH = "scan_type_switch"
            const val SIGNAL_STATUS_MONITORING_ENABLED_COLUMN = "signal_status_monitoring_enabled"
            const val LICENCE_KEY = "licence_key"
        }
    }
    class Languages private constructor(): BaseColumns {
        companion object {
            const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.iwedia.cltv.platform.model.content_provider.languages"
            const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.iwedia.cltv.platform.model.content_provider.languages"
            const val LANGUAGE_CODE_COLUMN = "language_code"
            const val LANGUAGE_CONTENT_COLUMN = "language_content"
        }
    }

    class ScheduledReminders private constructor(): BaseColumns {
        companion object {
            const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.iwedia.cltv.platform.model.content_provider.scheduled_reminders"
            const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.iwedia.cltv.platform.model.content_provider.scheduled_reminders"
            const val NAME_COLUMN = "name"
            const val CHANNEL_ID_COLUMN = "channel_id"
            const val EVENT_ID_COLUMN = "event_id"
            const val START_TIME_COLUMN = "start_time"
        }
    }

    class ScheduledRecordings private constructor(): BaseColumns {
        companion object {
            const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.iwedia.cltv.platform.model.content_provider.scheduled_recordings"
            const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.iwedia.cltv.platform.model.content_provider.scheduled_recordings"
            const val NAME_COLUMN = "name"
            const val CHANNEL_ID_COLUMN = "channel_id"
            const val TV_EVENT_ID_COLUMN = "tvevent_id"
            const val START_TIME_COLUMN = "start_time"
            const val END_TIME_COLUMN = "end_time"
            const val DATA_COLUMN = "data"
        }
    }

    class Favorites private constructor(): BaseColumns {
        companion object {
            const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.iwedia.cltv.platform.model.content_provider.favorites"
            const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.iwedia.cltv.platform.model.content_provider.favorites"
            const val ORIGINAL_NETWORK_ID_COLUMN = "original_network_id"
            const val TRANSPORT_STREAM_ID_COLUMN = "transport_stream_id"
            const val SERVICE_ID_COLUMN = "service_id"
            const val COLUMN_TYPE = "type"
            const val COLUMN_LIST_IDS = "listIds"
        }
    }

    class ConfigurableKeys private constructor(): BaseColumns {
        companion object {
            const  val KEY_NAME_COLUMN = "key_name"
            const  val KEY_ACTION_TYPE_COLUMN = "action_type"
            const  val KEY_DESCRIPTION_COLUMN = "description"
        }
    }
}