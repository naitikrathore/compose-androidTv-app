package com.iwedia.cltv.platform.model.content_provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.content.ContentUris
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.provider.BaseColumns;
import android.text.TextUtils
import android.util.Log
import com.iwedia.cltv.platform.model.Constants

import com.iwedia.cltv.platform.model.content_provider.Contract.Channels
import com.iwedia.cltv.platform.model.content_provider.Contract.Config
import com.iwedia.cltv.platform.model.content_provider.Contract.OemCustomization
import com.iwedia.cltv.platform.model.content_provider.Contract.Favorites
import java.lang.IllegalArgumentException

class ContentProvider : ContentProvider() {

    private var db: SQLiteDatabase? = null


    companion object {
        val AUTHORITY = "com.iwedia.cltv.sdk.content_provider.ReferenceContentProvider"
        private val CHANNELS_TABLE = "channels"
        private val CONFIG_TABLE = "config"
        private val OEM_CUSTOMIZATION_TABLE = "oem_customization"
        private val LANGUAGES_TABLE = "languages"
        private val SCHEDULED_REMINDERS_TABLE = "scheduled_reminders"
        private val SCHEDULED_RECORDINGS_TABLE = "scheduled_recordings"
        private val FAVORITES_TABLE = "favorites"
        private val CONFIGURABLE_KEYS_TABLE = "configurable_keys"
        private val TAG: String = "ReferContentProvider"
        val CHANNELS_URI: Uri = Uri.parse("content://${AUTHORITY}/${CHANNELS_TABLE}")
        val CONFIG_URI: Uri = Uri.parse("content://${AUTHORITY}/${CONFIG_TABLE}")
        val OEM_CUSTOMIZATION_URI: Uri =
            Uri.parse("content://${AUTHORITY}/${OEM_CUSTOMIZATION_TABLE}")
        val LANGUAGES_URI: Uri = Uri.parse("content://${AUTHORITY}/${LANGUAGES_TABLE}")
        val SCHEDULED_REMINDERS_URI: Uri =
            Uri.parse("content://${AUTHORITY}/${SCHEDULED_REMINDERS_TABLE}")
        val SCHEDULED_RECORDINGS_URI: Uri =
            Uri.parse("content://${AUTHORITY}/${SCHEDULED_RECORDINGS_TABLE}")
        val FAVORITES_URI: Uri = Uri.parse("content://${AUTHORITY}/${FAVORITES_TABLE}")
        val CONFIGURABLE_KEYS_URI: Uri =
            Uri.parse("content://${AUTHORITY}/${CONFIGURABLE_KEYS_TABLE}")
        const val SYSTEM_INFO_TABLE = "system_info"
        val SYSTEM_INFO_URI : Uri = Uri.parse("content://${AUTHORITY}/${SYSTEM_INFO_TABLE}")

        var sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        private val CHANNELS = 1
        private val CHANNELS_ID = 2
        private val CONFIG = 3
        private val CONFIG_ID = 4
        private val OEM_CUSTOMIZATION = 5
        private val OEM_CUSTOMIZATION_ID = 6
        private val LANGUAGES = 7
        private val LANGUAGES_ID = 8
        private val SCHEDULED_REMINDERS = 9
        private val SCHEDULED_REMINDERS_ID = 10
        private val SCHEDULED_RECORDINGS = 11
        private val SCHEDULED_RECORDINGS_ID = 12
        private val CONFIG_ID_SCAMBLED = 13
        private val FAVORITES = 14
        private val FAVORITES_ID = 15
        private val CONFIGURABLE_KEYS = 16
        private val CONFIGURABLE_KEYS_ID = 17
        private val SYSTEM_INFO = 18
        private val SYSTEM_INFO_ID = 19

        val DATABASE_NAME = "reference.db"
        val DATABASE_VERSION = 35

        var CHANNELS_PROJECTION_MAP: HashMap<String, String> = HashMap()
        var CONFIG_PROJECTION_MAP: HashMap<String, String> = HashMap()
        var OEM_CUSTOMIZATION_PROJECTION_MAP: HashMap<String, String> = HashMap()
        var LANGUAGES_PROJECTION_MAP: HashMap<String, String> = HashMap()
        var SCHEDULED_REMINDERS_PROJECTION_MAP: HashMap<String, String> = HashMap()
        var SCHEDULED_RECORDINGS_PROJECTION_MAP: HashMap<String, String> = HashMap()
        var FAVORITES_PROJECTION_MAP: HashMap<String, String> = HashMap()
        var CONFIGURABLE_KEYS_PROJECTION_MAP: HashMap<String, String> = HashMap()
        var SYSTEM_INFO_PROJECTION_MAP: HashMap<String, String> = HashMap()

        val CHANNELS_COLUMN_LOGO = "logo"

        private val createOemTable: String = "CREATE TABLE $OEM_CUSTOMIZATION_TABLE ( " +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                OemCustomization.BRANDING_COMPANY_NAME_COLUMN + " TEXT," +
                OemCustomization.BRANDING_CHANNEL_LOGO_COLUMN + " BLOB," +
                OemCustomization.BRANDING_WELCOME_MESSAGE_COLUMN + " TEXT," +
                OemCustomization.COLOR_BACKGROUND_COLUMN + " TEXT," +
                OemCustomization.COLOR_NOT_SELECTED_COLUMN + " TEXT," +
                OemCustomization.COLOR_TEXT_DESCRIPTION_COLUMN + " TEXT," +
                OemCustomization.COLOR_MAIN_TEXT_COLUMN + " TEXT," +
                OemCustomization.COLOR_SELECTOR_COLUMN + " TEXT," +
                OemCustomization.COLOR_PROGRESS_COLUMN + " TEXT," +
                OemCustomization.COLOR_PVR_AND_OTHER_COLUMN + " TEXT," +
                OemCustomization.FONT_REGULAR + " TEXT," +
                OemCustomization.FONT_MEDIUM + " TEXT," +
                OemCustomization.FONT_BOLD + " TEXT," +
                OemCustomization.FONT_LIGHT + " TEXT," +
                OemCustomization.PVR_ENABLED_COLUMN + " INTEGER DEFAULT 1," +
                OemCustomization.VIRTUAL_RCU_COLUMN + " INTEGER DEFAULT 0," +
                OemCustomization.VERTICAL_EPG_COLUMN + " INTEGER DEFAULT 0," +
                OemCustomization.EPG_CELL_MERGE_COLUMN + " INTEGER DEFAULT 0," +
                OemCustomization.THIRD_PARTY_INPUT_ENABLED_COLUMN + " INTEGER DEFAULT 1," +
                OemCustomization.CHANNEL_LOGO_METADATA_ENABLED_COLUMN + " INTEGER DEFAULT 0," +
                OemCustomization.SCAN_TYPE + "TEXT DEFAULT 'free'," +
                OemCustomization.SUBTITLE_COLUMN_ID + " INTEGER DEFAULT 1," +
                OemCustomization.SWAP_CHANNEL + "INTEGER DEFAULT 0," +
                OemCustomization.SCAN_TYPE_SWITCH + " INTEGER DEFAULT 0," +
                OemCustomization.COUNTRY_SELECT + " TEXT DEFAULT 'albania'," +
                OemCustomization.SIGNAL_STATUS_MONITORING_ENABLED_COLUMN + " INTEGER DEFAULT 0," +
                OemCustomization.LICENCE_KEY + " TEXT," +
                OemCustomization.CLEAR_CHANNELS_SCAN_OPTION_ENABLED_COLUMN + " INTEGER DEFAULT 1," +
                OemCustomization.ASPECT_RATIO_ENABLED_COLUMN + " INTEGER DEFAULT 0," +
                OemCustomization.TELETEXT_ENABLE_COLUMN + " INTEGER DEFAULT 1," +
                OemCustomization.DELETE_THIRD_PARTY_INPUT_COLUMN + " INTEGER DEFAULT 0," +
                OemCustomization.TIMESHIFT_ENABLED_COLUMN + " INTEGER DEFAULT 0," +
                "UNIQUE(${BaseColumns._ID})" +
                ");"
    }

    init {
        sUriMatcher.addURI(AUTHORITY, CHANNELS_TABLE, CHANNELS)
        sUriMatcher.addURI(AUTHORITY, CHANNELS_TABLE + "/#", CHANNELS_ID)
        sUriMatcher.addURI(AUTHORITY, CONFIG_TABLE, CONFIG)
        sUriMatcher.addURI(AUTHORITY, CONFIG_TABLE + "/#", CONFIG_ID)
        sUriMatcher.addURI(AUTHORITY, OEM_CUSTOMIZATION_TABLE, OEM_CUSTOMIZATION)
        sUriMatcher.addURI(AUTHORITY, OEM_CUSTOMIZATION_TABLE + "/#", OEM_CUSTOMIZATION_ID)
        sUriMatcher.addURI(AUTHORITY, LANGUAGES_TABLE, LANGUAGES)
        sUriMatcher.addURI(AUTHORITY, LANGUAGES_TABLE + "/#", LANGUAGES_ID)
        sUriMatcher.addURI(AUTHORITY, SCHEDULED_REMINDERS_TABLE, SCHEDULED_REMINDERS)
        sUriMatcher.addURI(AUTHORITY, SCHEDULED_REMINDERS_TABLE + "/#", SCHEDULED_REMINDERS_ID)
        sUriMatcher.addURI(AUTHORITY, SCHEDULED_RECORDINGS_TABLE, SCHEDULED_RECORDINGS)
        sUriMatcher.addURI(AUTHORITY, SCHEDULED_RECORDINGS_TABLE + "/#", SCHEDULED_RECORDINGS_ID)
        sUriMatcher.addURI(AUTHORITY, CONFIG_TABLE + "/#/*", CONFIG_ID_SCAMBLED)
        sUriMatcher.addURI(AUTHORITY, FAVORITES_TABLE, FAVORITES)
        sUriMatcher.addURI(AUTHORITY, FAVORITES_TABLE + "/#", FAVORITES_ID)
        sUriMatcher.addURI(AUTHORITY, CONFIGURABLE_KEYS_TABLE, CONFIGURABLE_KEYS)
        sUriMatcher.addURI(AUTHORITY, CONFIGURABLE_KEYS_TABLE + "/#", CONFIGURABLE_KEYS_ID)
        sUriMatcher.addURI(AUTHORITY, SYSTEM_INFO_TABLE, SYSTEM_INFO)
        sUriMatcher.addURI(AUTHORITY, SYSTEM_INFO_TABLE + "/#", SYSTEM_INFO_ID)


        CHANNELS_PROJECTION_MAP.put(BaseColumns._ID, CHANNELS_TABLE + "." + BaseColumns._ID)
        CHANNELS_PROJECTION_MAP.put(
            Channels.PACKAGE_NAME_COLUMN,
            CHANNELS_TABLE + "." + Channels.PACKAGE_NAME_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.INPUT_ID_COLUMN,
            CHANNELS_TABLE + "." + Channels.INPUT_ID_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.TYPE_COLUMN,
            CHANNELS_TABLE + "." + Channels.TYPE_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.SERVICE_TYPE_COLUMN,
            CHANNELS_TABLE + "." + Channels.SERVICE_TYPE_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.ORIGINAL_NETWORK_ID_COLUMN,
            CHANNELS_TABLE + "." + Channels.ORIGINAL_NETWORK_ID_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.TRANSPORT_STREAM_ID_COLUMN,
            CHANNELS_TABLE + "." + Channels.TRANSPORT_STREAM_ID_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.SERVICE_ID_COLUMN,
            CHANNELS_TABLE + "." + Channels.SERVICE_ID_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.DISPLAY_NUMBER_COLUMN,
            CHANNELS_TABLE + "." + Channels.DISPLAY_NUMBER_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.NAME_COLUMN,
            CHANNELS_TABLE + "." + Channels.NAME_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.NETWORK_AFFILIATION_COLUMN,
            CHANNELS_TABLE + "." + Channels.NETWORK_AFFILIATION_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.DESCRIPTION_COLUMN,
            CHANNELS_TABLE + "." + Channels.DESCRIPTION_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.VIDEO_FORMAT_COLUMN,
            CHANNELS_TABLE + "." + Channels.VIDEO_FORMAT_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.BROWSABLE_COLUMN,
            CHANNELS_TABLE + "." + Channels.BROWSABLE_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.SEARCHABLE_COLUMN,
            CHANNELS_TABLE + "." + Channels.SEARCHABLE_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.LOCKED_COLUMN,
            CHANNELS_TABLE + "." + Channels.LOCKED_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.APP_LINK_ICON_URI_COLUMN,
            CHANNELS_TABLE + "." + Channels.APP_LINK_ICON_URI_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.APP_LINK_POSTER_ART_URI_COLUMN,
            CHANNELS_TABLE + "." + Channels.APP_LINK_POSTER_ART_URI_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.APP_LINK_TEXT_COLUMN,
            CHANNELS_TABLE + "." + Channels.APP_LINK_TEXT_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.APP_LINK_COLOR_COLUMN,
            CHANNELS_TABLE + "." + Channels.APP_LINK_COLOR_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.APP_LINK_INTENT_URI_COLUMN,
            CHANNELS_TABLE + "." + Channels.APP_LINK_INTENT_URI_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.INTERNAL_PROVIDER_DATA_COLUMN,
            CHANNELS_TABLE + "." + Channels.INTERNAL_PROVIDER_DATA_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.INTERNAL_PROVIDER_FLAG1_COLUMN,
            CHANNELS_TABLE + "." + Channels.INTERNAL_PROVIDER_FLAG1_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.VERSION_NUMBER_COLUMN,
            CHANNELS_TABLE + "." + Channels.VERSION_NUMBER_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.TRANSIENT_COLUMN,
            CHANNELS_TABLE + "." + Channels.TRANSIENT_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.INTERNAL_PROVIDER_ID_COLUMN,
            CHANNELS_TABLE + "." + Channels.INTERNAL_PROVIDER_ID_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.SKIP_COLUMN,
            CHANNELS_TABLE + "." + Channels.SKIP_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.ORIG_ID_COLUMN,
            CHANNELS_TABLE + "." + Channels.ORIG_ID_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.REFERENCE_NAME_COLUMN,
            CHANNELS_TABLE + "." + Channels.REFERENCE_NAME_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.DISPLAY_NUMBER_CHANGED_COLUMN,
            CHANNELS_TABLE + "." + Channels.DISPLAY_NUMBER_CHANGED_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.ORDINAL_NUMBER_COLUMN,
            CHANNELS_TABLE + "." + Channels.ORDINAL_NUMBER_COLUMN
        )
        CHANNELS_PROJECTION_MAP.put(
            Channels.DELETED_COLUMN,
            CHANNELS_TABLE + "." + Channels.DELETED_COLUMN
        )

        CONFIG_PROJECTION_MAP.put(BaseColumns._ID, CONFIG_TABLE + "." + BaseColumns._ID)
        CONFIG_PROJECTION_MAP.put(Config.PIN_COLUMN, CONFIG_TABLE + "." + Config.PIN_COLUMN)
        CONFIG_PROJECTION_MAP.put(Config.LCN_COLUMN, CONFIG_TABLE + "." + Config.LCN_COLUMN)
        CONFIG_PROJECTION_MAP.put(Config.SCAN_TYPE, CONFIG_TABLE + "." + Config.SCAN_TYPE)
        CONFIG_PROJECTION_MAP.put(Config.IS_RECORDING_STARTED, CONFIG_TABLE + "." + Config.IS_RECORDING_STARTED)
        CONFIG_PROJECTION_MAP.put(Config.CURRENT_COUNTRY_COLUMN, CONFIG_TABLE + "." + Config.CURRENT_COUNTRY_COLUMN)
        CONFIG_PROJECTION_MAP.put(
            Config.CURRENT_COUNTRY_COLUMN,
            CONFIG_TABLE + "." + Config.CURRENT_COUNTRY_COLUMN
        )

        OEM_CUSTOMIZATION_PROJECTION_MAP.put(BaseColumns._ID, OEM_CUSTOMIZATION_TABLE + "." + BaseColumns._ID)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.PVR_ENABLED_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.PVR_ENABLED_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.VIRTUAL_RCU_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.VIRTUAL_RCU_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.VERTICAL_EPG_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.VERTICAL_EPG_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.EPG_CELL_MERGE_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.EPG_CELL_MERGE_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.COLOR_BACKGROUND_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.COLOR_BACKGROUND_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.COLOR_NOT_SELECTED_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.COLOR_NOT_SELECTED_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.COLOR_TEXT_DESCRIPTION_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.COLOR_TEXT_DESCRIPTION_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.COLOR_MAIN_TEXT_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.COLOR_MAIN_TEXT_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.COLOR_SELECTOR_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.COLOR_SELECTOR_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.COLOR_PROGRESS_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.COLOR_PROGRESS_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.COLOR_PVR_AND_OTHER_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.COLOR_PVR_AND_OTHER_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.BRANDING_CHANNEL_LOGO_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.BRANDING_CHANNEL_LOGO_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.BRANDING_COMPANY_NAME_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.BRANDING_COMPANY_NAME_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.BRANDING_WELCOME_MESSAGE_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.BRANDING_WELCOME_MESSAGE_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.FONT_REGULAR, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.FONT_REGULAR)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.FONT_MEDIUM, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.FONT_MEDIUM)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.FONT_BOLD, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.FONT_BOLD)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.FONT_LIGHT, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.FONT_LIGHT)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.THIRD_PARTY_INPUT_ENABLED_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.THIRD_PARTY_INPUT_ENABLED_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.TERRESTRIAL_SCAN_ENABLED_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.TERRESTRIAL_SCAN_ENABLED_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.CABLE_SCAN_ENABLED_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.CABLE_SCAN_ENABLED_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.SATELLITE_SCAN_ENABLED_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.SATELLITE_SCAN_ENABLED_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.CHANNEL_LOGO_METADATA_ENABLED_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.CHANNEL_LOGO_METADATA_ENABLED_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.SCAN_TYPE, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.SCAN_TYPE)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.SWAP_CHANNEL, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.SWAP_CHANNEL)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.SUBTITLE_COLUMN_ID, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.SUBTITLE_COLUMN_ID)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.SCAN_TYPE_SWITCH, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.SCAN_TYPE_SWITCH)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.COUNTRY_SELECT, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.COUNTRY_SELECT)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.CLEAR_CHANNELS_SCAN_OPTION_ENABLED_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.CLEAR_CHANNELS_SCAN_OPTION_ENABLED_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.ASPECT_RATIO_ENABLED_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.ASPECT_RATIO_ENABLED_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.TELETEXT_ENABLE_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.TELETEXT_ENABLE_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.DELETE_THIRD_PARTY_INPUT_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.DELETE_THIRD_PARTY_INPUT_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            BaseColumns._ID,
            OEM_CUSTOMIZATION_TABLE + "." + BaseColumns._ID
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.PVR_ENABLED_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.PVR_ENABLED_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.VIRTUAL_RCU_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.VIRTUAL_RCU_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.VERTICAL_EPG_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.VERTICAL_EPG_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.EPG_CELL_MERGE_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.EPG_CELL_MERGE_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.COLOR_BACKGROUND_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.COLOR_BACKGROUND_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.COLOR_NOT_SELECTED_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.COLOR_NOT_SELECTED_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.COLOR_TEXT_DESCRIPTION_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.COLOR_TEXT_DESCRIPTION_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.COLOR_MAIN_TEXT_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.COLOR_MAIN_TEXT_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.COLOR_SELECTOR_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.COLOR_SELECTOR_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.COLOR_PROGRESS_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.COLOR_PROGRESS_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.COLOR_PVR_AND_OTHER_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.COLOR_PVR_AND_OTHER_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.BRANDING_CHANNEL_LOGO_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.BRANDING_CHANNEL_LOGO_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.BRANDING_COMPANY_NAME_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.BRANDING_COMPANY_NAME_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.BRANDING_WELCOME_MESSAGE_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.BRANDING_WELCOME_MESSAGE_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.FONT_REGULAR,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.FONT_REGULAR
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.FONT_MEDIUM,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.FONT_MEDIUM
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.FONT_BOLD,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.FONT_BOLD
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.FONT_LIGHT,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.FONT_LIGHT
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.THIRD_PARTY_INPUT_ENABLED_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.THIRD_PARTY_INPUT_ENABLED_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.TERRESTRIAL_SCAN_ENABLED_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.TERRESTRIAL_SCAN_ENABLED_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.CABLE_SCAN_ENABLED_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.CABLE_SCAN_ENABLED_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.SATELLITE_SCAN_ENABLED_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.SATELLITE_SCAN_ENABLED_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.CHANNEL_LOGO_METADATA_ENABLED_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.CHANNEL_LOGO_METADATA_ENABLED_COLUMN
        )
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.SCAN_TYPE_SWITCH, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.SCAN_TYPE_SWITCH)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.SIGNAL_STATUS_MONITORING_ENABLED_COLUMN,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.SIGNAL_STATUS_MONITORING_ENABLED_COLUMN)
        OEM_CUSTOMIZATION_PROJECTION_MAP.put(
            OemCustomization.LICENCE_KEY,
            OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.LICENCE_KEY)

        OEM_CUSTOMIZATION_PROJECTION_MAP.put(OemCustomization.TIMESHIFT_ENABLED_COLUMN, OEM_CUSTOMIZATION_TABLE + "." + OemCustomization.TIMESHIFT_ENABLED_COLUMN)


        LANGUAGES_PROJECTION_MAP.put(BaseColumns._ID, LANGUAGES_TABLE + "." + BaseColumns._ID)
        LANGUAGES_PROJECTION_MAP.put(
            Contract.Languages.LANGUAGE_CODE_COLUMN,
            LANGUAGES_TABLE + "." + Contract.Languages.LANGUAGE_CODE_COLUMN
        )
        LANGUAGES_PROJECTION_MAP.put(
            Contract.Languages.LANGUAGE_CONTENT_COLUMN,
            LANGUAGES_TABLE + "." + Contract.Languages.LANGUAGE_CONTENT_COLUMN
        )

        SCHEDULED_REMINDERS_PROJECTION_MAP.put(
            BaseColumns._ID,
            SCHEDULED_REMINDERS_TABLE + "." + BaseColumns._ID
        )
        SCHEDULED_REMINDERS_PROJECTION_MAP.put(
            Contract.ScheduledReminders.NAME_COLUMN,
            SCHEDULED_REMINDERS_TABLE + "." + Contract.ScheduledReminders.NAME_COLUMN
        )
        SCHEDULED_REMINDERS_PROJECTION_MAP.put(
            Contract.ScheduledReminders.CHANNEL_ID_COLUMN,
            SCHEDULED_REMINDERS_TABLE + "." + Contract.ScheduledReminders.CHANNEL_ID_COLUMN
        )
        SCHEDULED_REMINDERS_PROJECTION_MAP.put(
            Contract.ScheduledReminders.EVENT_ID_COLUMN,
            SCHEDULED_REMINDERS_TABLE + "." + Contract.ScheduledReminders.EVENT_ID_COLUMN
        )
        SCHEDULED_REMINDERS_PROJECTION_MAP.put(
            Contract.ScheduledReminders.START_TIME_COLUMN,
            SCHEDULED_REMINDERS_TABLE + "." + Contract.ScheduledReminders.START_TIME_COLUMN
        )

        SCHEDULED_RECORDINGS_PROJECTION_MAP.put(
            BaseColumns._ID,
            SCHEDULED_RECORDINGS_TABLE + "." + BaseColumns._ID
        )
        SCHEDULED_RECORDINGS_PROJECTION_MAP.put(
            Contract.ScheduledRecordings.NAME_COLUMN,
            SCHEDULED_RECORDINGS_TABLE + "." + Contract.ScheduledRecordings.NAME_COLUMN
        )
        SCHEDULED_RECORDINGS_PROJECTION_MAP.put(
            Contract.ScheduledRecordings.CHANNEL_ID_COLUMN,
            SCHEDULED_RECORDINGS_TABLE + "." + Contract.ScheduledRecordings.CHANNEL_ID_COLUMN
        )
        SCHEDULED_RECORDINGS_PROJECTION_MAP.put(
            Contract.ScheduledRecordings.TV_EVENT_ID_COLUMN,
            SCHEDULED_RECORDINGS_TABLE + "." + Contract.ScheduledRecordings.TV_EVENT_ID_COLUMN
        )
        SCHEDULED_RECORDINGS_PROJECTION_MAP.put(
            Contract.ScheduledRecordings.START_TIME_COLUMN,
            SCHEDULED_RECORDINGS_TABLE + "." + Contract.ScheduledRecordings.START_TIME_COLUMN
        )
        SCHEDULED_RECORDINGS_PROJECTION_MAP.put(
            Contract.ScheduledRecordings.END_TIME_COLUMN,
            SCHEDULED_RECORDINGS_TABLE + "." + Contract.ScheduledRecordings.END_TIME_COLUMN
        )
        SCHEDULED_RECORDINGS_PROJECTION_MAP.put(
            Contract.ScheduledRecordings.DATA_COLUMN,
            SCHEDULED_RECORDINGS_TABLE + "." + Contract.ScheduledRecordings.DATA_COLUMN
        )

        FAVORITES_PROJECTION_MAP.put(BaseColumns._ID, FAVORITES_TABLE + "." + BaseColumns._ID)
        FAVORITES_PROJECTION_MAP.put(Favorites.ORIGINAL_NETWORK_ID_COLUMN, FAVORITES_TABLE + "." + Favorites.ORIGINAL_NETWORK_ID_COLUMN)
        FAVORITES_PROJECTION_MAP.put(Favorites.TRANSPORT_STREAM_ID_COLUMN, FAVORITES_TABLE + "." + Favorites.TRANSPORT_STREAM_ID_COLUMN)
        FAVORITES_PROJECTION_MAP.put(Favorites.SERVICE_ID_COLUMN, FAVORITES_TABLE + "." + Favorites.SERVICE_ID_COLUMN)
        FAVORITES_PROJECTION_MAP.put(
            Favorites.COLUMN_TYPE,
            FAVORITES_TABLE + "." + Favorites.COLUMN_TYPE
        )
        FAVORITES_PROJECTION_MAP.put(
            Favorites.COLUMN_LIST_IDS,
            FAVORITES_TABLE + "." + Favorites.COLUMN_LIST_IDS
        )

        CONFIGURABLE_KEYS_PROJECTION_MAP.put(
            BaseColumns._ID,
            CONFIGURABLE_KEYS_TABLE + "." + BaseColumns._ID
        )
        CONFIGURABLE_KEYS_PROJECTION_MAP.put(
            Contract.ConfigurableKeys.KEY_NAME_COLUMN,
            CONFIGURABLE_KEYS_TABLE + "." + Contract.ConfigurableKeys.KEY_NAME_COLUMN
        )
        CONFIGURABLE_KEYS_PROJECTION_MAP.put(
            Contract.ConfigurableKeys.KEY_ACTION_TYPE_COLUMN,
            CONFIGURABLE_KEYS_TABLE + "." + Contract.ConfigurableKeys.KEY_ACTION_TYPE_COLUMN
        )
        CONFIGURABLE_KEYS_PROJECTION_MAP.put(
            Contract.ConfigurableKeys.KEY_DESCRIPTION_COLUMN,
            CONFIGURABLE_KEYS_TABLE + "." + Contract.ConfigurableKeys.KEY_DESCRIPTION_COLUMN
        )

        SYSTEM_INFO_PROJECTION_MAP.put(Contract.SystemInfo.RF_CHANNEL_NUMBER_ENABLED_COLUMN, SYSTEM_INFO_TABLE + "." + Contract.SystemInfo.RF_CHANNEL_NUMBER_ENABLED_COLUMN)
        SYSTEM_INFO_PROJECTION_MAP.put(Contract.SystemInfo.BER_ENABLED_COLUMN, SYSTEM_INFO_TABLE + "." + Contract.SystemInfo.BER_ENABLED_COLUMN)
        SYSTEM_INFO_PROJECTION_MAP.put(Contract.SystemInfo.FREQUENCY_ENABLED_COLUMN, SYSTEM_INFO_TABLE + "." + Contract.SystemInfo.FREQUENCY_ENABLED_COLUMN)
        SYSTEM_INFO_PROJECTION_MAP.put(Contract.SystemInfo.PROG_ENABLED_COLUMN, SYSTEM_INFO_TABLE + "." + Contract.SystemInfo.PROG_ENABLED_COLUMN)
        SYSTEM_INFO_PROJECTION_MAP.put(Contract.SystemInfo.UEC_ENABLED_COLUMN, SYSTEM_INFO_TABLE + "." + Contract.SystemInfo.UEC_ENABLED_COLUMN)
        SYSTEM_INFO_PROJECTION_MAP.put(Contract.SystemInfo.SERVICEID_ENABLED_COLUMN, SYSTEM_INFO_TABLE + "." + Contract.SystemInfo.SERVICEID_ENABLED_COLUMN)
        SYSTEM_INFO_PROJECTION_MAP.put(Contract.SystemInfo.POSTVITERBI_ENABLED_COLUMN, SYSTEM_INFO_TABLE + "." + Contract.SystemInfo.POSTVITERBI_ENABLED_COLUMN)
        SYSTEM_INFO_PROJECTION_MAP.put(Contract.SystemInfo.TSID_ENABLED_COLUMN, SYSTEM_INFO_TABLE + "." + Contract.SystemInfo.TSID_ENABLED_COLUMN)
        SYSTEM_INFO_PROJECTION_MAP.put(Contract.SystemInfo.FIVES_ENABLED_COLUMN, SYSTEM_INFO_TABLE + "." + Contract.SystemInfo.FIVES_ENABLED_COLUMN)
        SYSTEM_INFO_PROJECTION_MAP.put(Contract.SystemInfo.ONID_ENABLED_COLUMN, SYSTEM_INFO_TABLE + "." + Contract.SystemInfo.ONID_ENABLED_COLUMN)
        SYSTEM_INFO_PROJECTION_MAP.put(Contract.SystemInfo.AGC_ENABLED_COLUMN, SYSTEM_INFO_TABLE + "." + Contract.SystemInfo.AGC_ENABLED_COLUMN)
        SYSTEM_INFO_PROJECTION_MAP.put(Contract.SystemInfo.NETWORKID_ENABLED_COLUMN, SYSTEM_INFO_TABLE + "." + Contract.SystemInfo.NETWORKID_ENABLED_COLUMN)
        SYSTEM_INFO_PROJECTION_MAP.put(Contract.SystemInfo.NETWORKNAME_ENABLED_COLUMN, SYSTEM_INFO_TABLE + "." + Contract.SystemInfo.NETWORKNAME_ENABLED_COLUMN)
        SYSTEM_INFO_PROJECTION_MAP.put(Contract.SystemInfo.BANDWIDTH_ENABLED_COLUMN, SYSTEM_INFO_TABLE + "." + Contract.SystemInfo.BANDWIDTH_ENABLED_COLUMN)
    }

    private class DBHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(p0: SQLiteDatabase?) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Path of DB = ${p0!!.path}")
            p0.execSQL(
                "CREATE TABLE $CHANNELS_TABLE ("
                        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + Channels.PACKAGE_NAME_COLUMN + " TEXT NOT NULL,"
                        + Channels.INPUT_ID_COLUMN + " TEXT NOT NULL,"
                        + Channels.TYPE_COLUMN + " TEXT NOT NULL DEFAULT TYPE_OTHER,"
                        + Channels.SERVICE_TYPE_COLUMN + " TEXT NOT NULL DEFAULT SERVICE_TYPE_AUDIO_VIDEO,"
                        + Channels.ORIGINAL_NETWORK_ID_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                        + Channels.TRANSPORT_STREAM_ID_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                        + Channels.SERVICE_ID_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                        + Channels.DISPLAY_NUMBER_COLUMN + " TEXT,"
                        + Channels.NAME_COLUMN + " TEXT,"
                        + Channels.NETWORK_AFFILIATION_COLUMN + " TEXT,"
                        + Channels.DESCRIPTION_COLUMN + " TEXT,"
                        + Channels.VIDEO_FORMAT_COLUMN + " TEXT,"
                        + Channels.BROWSABLE_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                        + Channels.SEARCHABLE_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                        + Channels.LOCKED_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                        + Channels.APP_LINK_ICON_URI_COLUMN + " TEXT,"
                        + Channels.APP_LINK_POSTER_ART_URI_COLUMN + " TEXT,"
                        + Channels.APP_LINK_TEXT_COLUMN + " TEXT,"
                        + Channels.APP_LINK_COLOR_COLUMN + " INTEGER,"
                        + Channels.APP_LINK_INTENT_URI_COLUMN + " TEXT,"
                        + Channels.INTERNAL_PROVIDER_DATA_COLUMN + " BLOB,"
                        + Channels.INTERNAL_PROVIDER_FLAG1_COLUMN + " INTEGER,"
                        + CHANNELS_COLUMN_LOGO + " BLOB,"
                        + Channels.VERSION_NUMBER_COLUMN + " INTEGER,"
                        + Channels.TRANSIENT_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                        + Channels.INTERNAL_PROVIDER_ID_COLUMN + " TEXT,"
                        + Channels.SKIP_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                        + Channels.ORIG_ID_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                        + Channels.REFERENCE_NAME_COLUMN + " TEXT,"
                        + Channels.DISPLAY_NUMBER_CHANGED_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                        + Channels.ORDINAL_NUMBER_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                        + Channels.DELETED_COLUMN + " INTEGER DEFAULT 0,"
                        + "UNIQUE(${BaseColumns._ID})"
                        + ");"
            )
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Created Channels table. Now create Config")
            p0.execSQL("CREATE TABLE $CONFIG_TABLE ("
                    + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Config.PIN_COLUMN + " TEXT,"
                    + Config.LCN_COLUMN + " TEXT,"
                    + Config.SCAN_TYPE + " TEXT,"
                    + Config.IS_RECORDING_STARTED + " TEXT,"
                    + Config.CURRENT_COUNTRY_COLUMN + " TEXT,"
                    + "UNIQUE(${BaseColumns._ID})"
                    +");")

            p0.execSQL("CREATE TABLE $OEM_CUSTOMIZATION_TABLE ("
                    + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + OemCustomization.BRANDING_COMPANY_NAME_COLUMN + " TEXT,"
                    + OemCustomization.BRANDING_CHANNEL_LOGO_COLUMN + " BLOB,"
                    + OemCustomization.BRANDING_WELCOME_MESSAGE_COLUMN + " TEXT,"
                    + OemCustomization.COLOR_BACKGROUND_COLUMN + " TEXT,"
                    + OemCustomization.COLOR_NOT_SELECTED_COLUMN + " TEXT,"
                    + OemCustomization.COLOR_TEXT_DESCRIPTION_COLUMN + " TEXT,"
                    + OemCustomization.COLOR_MAIN_TEXT_COLUMN + " TEXT,"
                    + OemCustomization.COLOR_SELECTOR_COLUMN + " TEXT,"
                    + OemCustomization.COLOR_PROGRESS_COLUMN + " TEXT,"
                    + OemCustomization.COLOR_PVR_AND_OTHER_COLUMN + " TEXT,"
                    + OemCustomization.FONT_REGULAR + " TEXT,"
                    + OemCustomization.FONT_MEDIUM + " TEXT,"
                    + OemCustomization.FONT_BOLD + " TEXT,"
                    + OemCustomization.FONT_LIGHT + " TEXT,"
                    + OemCustomization.PVR_ENABLED_COLUMN + " INTEGER DEFAULT 1,"
                    + OemCustomization.VIRTUAL_RCU_COLUMN + " INTEGER DEFAULT 0,"
                    + OemCustomization.VERTICAL_EPG_COLUMN + " INTEGER DEFAULT 0,"
                    + OemCustomization.EPG_CELL_MERGE_COLUMN + " INTEGER DEFAULT 0,"
                    + OemCustomization.THIRD_PARTY_INPUT_ENABLED_COLUMN + " INTEGER DEFAULT 1,"
                    + OemCustomization.TERRESTRIAL_SCAN_ENABLED_COLUMN + " INTEGER DEFAULT 1,"
                    + OemCustomization.CABLE_SCAN_ENABLED_COLUMN + " INTEGER DEFAULT 0,"
                    + OemCustomization.SATELLITE_SCAN_ENABLED_COLUMN + " INTEGER DEFAULT 0,"
                    + OemCustomization.CHANNEL_LOGO_METADATA_ENABLED_COLUMN + " INTEGER DEFAULT 0,"
                    + OemCustomization.SCAN_TYPE + " TEXT DEFAULT 'free',"
                    + OemCustomization.SUBTITLE_COLUMN_ID + " INTEGER DEFAULT 1,"
                    + OemCustomization.SWAP_CHANNEL + " INTEGER DEFAULT 0,"
                    + OemCustomization.SCAN_TYPE_SWITCH + " INTEGER DEFAULT 0,"
                    + OemCustomization.COUNTRY_SELECT + " TEXT DEFAULT 'albania',"
                    + OemCustomization.SIGNAL_STATUS_MONITORING_ENABLED_COLUMN + " INTEGER DEFAULT 0,"
                    + OemCustomization.CLEAR_CHANNELS_SCAN_OPTION_ENABLED_COLUMN + " INTEGER DEFAULT 1,"
                    + OemCustomization.ASPECT_RATIO_ENABLED_COLUMN + " INTEGER DEFAULT 0,"
                    + OemCustomization.TELETEXT_ENABLE_COLUMN + " INTEGER DEFAULT 1,"
                    + OemCustomization.DELETE_THIRD_PARTY_INPUT_COLUMN + " INTEGER DEFAULT 0,"
                    + OemCustomization.LICENCE_KEY + " TEXT,"
                    + OemCustomization.TIMESHIFT_ENABLED_COLUMN + " INTEGER DEFAULT 0,"
                    + "UNIQUE(${BaseColumns._ID})"
                    +");")
            // Create languages table
            p0.execSQL("CREATE TABLE $LANGUAGES_TABLE ("
                    + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Contract.Languages.LANGUAGE_CODE_COLUMN + " TEXT,"
                    + Contract.Languages.LANGUAGE_CONTENT_COLUMN + " TEXT,"
                    + "UNIQUE(${BaseColumns._ID})"
                    +");")
            // Create scheduled reminders table
            p0.execSQL("CREATE TABLE $SCHEDULED_REMINDERS_TABLE ("
                    + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Contract.ScheduledReminders.NAME_COLUMN + " TEXT,"
                    + Contract.ScheduledReminders.CHANNEL_ID_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                    + Contract.ScheduledReminders.EVENT_ID_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                    + Contract.ScheduledReminders.START_TIME_COLUMN + " LONG NOT NULL DEFAULT 0,"
                    + "UNIQUE(${BaseColumns._ID})"
                    +");")
            // Create scheduled recordings table
            p0.execSQL("CREATE TABLE $SCHEDULED_RECORDINGS_TABLE ("
                    + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Contract.ScheduledRecordings.NAME_COLUMN + " TEXT,"
                    + Contract.ScheduledRecordings.CHANNEL_ID_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                    + Contract.ScheduledRecordings.TV_EVENT_ID_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                    + Contract.ScheduledRecordings.START_TIME_COLUMN + " LONG NOT NULL DEFAULT 0,"
                    + Contract.ScheduledRecordings.END_TIME_COLUMN + " LONG NOT NULL DEFAULT 0,"
                    + Contract.ScheduledRecordings.DATA_COLUMN + " TEXT,"
                    + "UNIQUE(${BaseColumns._ID})"
                    +");")
            // Create favorite table
            p0.execSQL("CREATE TABLE $FAVORITES_TABLE ("
                    + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Favorites.ORIGINAL_NETWORK_ID_COLUMN + " INTEGER NOT NULL,"
                    + Favorites.TRANSPORT_STREAM_ID_COLUMN + " INTEGER NOT NULL,"
                    + Favorites.SERVICE_ID_COLUMN + " INTEGER NOT NULL,"
                    + Favorites.COLUMN_TYPE + " INTEGER NOT NULL,"
                    + Favorites.COLUMN_LIST_IDS + " TEXT NOT NULL,"
                    + "UNIQUE(${BaseColumns._ID})"
                    +");")
            // Create configurable keys table
            p0.execSQL(
                "CREATE TABLE $CONFIGURABLE_KEYS_TABLE ("
                        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + Contract.ConfigurableKeys.KEY_NAME_COLUMN + " TEXT,"
                        + Contract.ConfigurableKeys.KEY_ACTION_TYPE_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                        + Contract.ConfigurableKeys.KEY_DESCRIPTION_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                        + "UNIQUE(${BaseColumns._ID})"
                        + ");"
            )
            // Create System information keys table
            p0.execSQL(
                "CREATE TABLE $SYSTEM_INFO_TABLE ("
                        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + Contract.SystemInfo.RF_CHANNEL_NUMBER_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                        + Contract.SystemInfo.BER_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                        + Contract.SystemInfo.FREQUENCY_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                        + Contract.SystemInfo.PROG_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                        + Contract.SystemInfo.UEC_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                        + Contract.SystemInfo.SERVICEID_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                        + Contract.SystemInfo.POSTVITERBI_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                        + Contract.SystemInfo.TSID_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                        + Contract.SystemInfo.FIVES_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                        + Contract.SystemInfo.ONID_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                        + Contract.SystemInfo.AGC_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                        + Contract.SystemInfo.NETWORKID_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                        + Contract.SystemInfo.NETWORKNAME_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                        + Contract.SystemInfo.BANDWIDTH_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                        + "UNIQUE(${BaseColumns._ID})"
                        + ");"
            )
        }

        override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onUpgrade from $p0 to $p1")
            if (p1 < 11) {
                Log.i(TAG, "Upgrading from version $p1 to $p2, data will be lost")
                p0!!.execSQL("DROP TABLE IF EXISTS $CHANNELS_TABLE")
                p0!!.execSQL("DROP TABLE IF EXISTS $CONFIG_TABLE")
                p0!!.execSQL("DROP TABLE IF EXISTS $OEM_CUSTOMIZATION_TABLE")
                onCreate(p0)
                return
            }
            Log.i(TAG, "Upgrading from version $p1 to $p2")
            if (p1 <= 11) {
                Log.i(TAG, "Add internal provider column flag 1 into Channels table")
                p0!!.execSQL("ALTER TABLE $CHANNELS_TABLE ADD ${Contract.Channels.INTERNAL_PROVIDER_FLAG1_COLUMN} INTEGER;")
            }
            if (p1 <= 12) {
                p0!!.execSQL(
                    "CREATE TABLE $SCHEDULED_REMINDERS_TABLE ("
                            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + Contract.ScheduledReminders.NAME_COLUMN + " TEXT,"
                            + Contract.ScheduledReminders.CHANNEL_ID_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                            + Contract.ScheduledReminders.EVENT_ID_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                            + Contract.ScheduledReminders.START_TIME_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                            + "UNIQUE(${BaseColumns._ID})"
                            + ");"
                )
            }
            if (p1 <= 13) {
                // Create scheduled recordings table
                p0!!.execSQL(
                    "CREATE TABLE $SCHEDULED_RECORDINGS_TABLE ("
                            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + Contract.ScheduledRecordings.NAME_COLUMN + " TEXT,"
                            + Contract.ScheduledRecordings.CHANNEL_ID_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                            + Contract.ScheduledRecordings.TV_EVENT_ID_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                            + Contract.ScheduledRecordings.START_TIME_COLUMN + " LONG NOT NULL DEFAULT 0,"
                            + Contract.ScheduledRecordings.END_TIME_COLUMN + " LONG NOT NULL DEFAULT 0,"
                            + Contract.ScheduledRecordings.DATA_COLUMN + " TEXT,"
                            + "UNIQUE(${BaseColumns._ID})"
                            + ");"
                )

            }
            if (p1 <= 14) {
                //Color column names are changed
                //Recreate oem customization table
                p0!!.execSQL("DROP TABLE IF EXISTS $OEM_CUSTOMIZATION_TABLE")
                p0.execSQL(createOemTable)
            }
            if (p1 <= 15) {
                p0!!.execSQL("ALTER TABLE $CHANNELS_TABLE RENAME COLUMN iw_renamed TO ${Channels.DISPLAY_NUMBER_CHANGED_COLUMN};")
                p0!!.execSQL("DROP TABLE IF EXISTS $OEM_CUSTOMIZATION_TABLE")
                p0!!.execSQL(createOemTable)
            }
            if (p1 <= 16) {
                Log.i(TAG, "Add is_deleted column into Channels table")
                p0!!.execSQL("ALTER TABLE $CHANNELS_TABLE ADD ${Contract.Channels.DELETED_COLUMN} INTEGER DEFAULT 0;")
            }
            if (p1 <= 17) {
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.TERRESTRIAL_SCAN_ENABLED_COLUMN} INTEGER DEFAULT 1;")
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.CABLE_SCAN_ENABLED_COLUMN} INTEGER DEFAULT 0;")
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.SATELLITE_SCAN_ENABLED_COLUMN} INTEGER DEFAULT 0;")
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.SCAN_TYPE_SWITCH} INTEGER DEFAULT 0;")
            }
            if (p1 <= 18) {
                //Clear reminders table and create new one because of missing start_time column in previous release
                //Create again   SCHEDULED_REMINDERS_TABLE table in order to fix customer update issue
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Delete and recreate scheduled_recordings_table")
                p0!!.execSQL("DROP TABLE IF EXISTS $SCHEDULED_REMINDERS_TABLE")
                p0!!.execSQL(
                    "CREATE TABLE $SCHEDULED_REMINDERS_TABLE ("
                            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + Contract.ScheduledReminders.NAME_COLUMN + " TEXT,"
                            + Contract.ScheduledReminders.CHANNEL_ID_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                            + Contract.ScheduledReminders.EVENT_ID_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                            + Contract.ScheduledReminders.START_TIME_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                            + "UNIQUE(${BaseColumns._ID})"
                            + ");"
                )
            }
            if (p1 <= 18) {
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.SCAN_TYPE} TEXT DEFAULT 'free';")
            }
            if (p1 <= 19) {
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.CHANNEL_LOGO_METADATA_ENABLED_COLUMN} INTEGER DEFAULT 0;")
            }
            if (p1 <= 20) {
                // Create scheduled recordings table
                p0!!.execSQL("DROP TABLE IF EXISTS $SCHEDULED_RECORDINGS_TABLE")
                p0!!.execSQL("CREATE TABLE $SCHEDULED_RECORDINGS_TABLE ("
                        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + Contract.ScheduledRecordings.NAME_COLUMN + " TEXT,"
                        + Contract.ScheduledRecordings.CHANNEL_ID_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                        + Contract.ScheduledRecordings.TV_EVENT_ID_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                        + Contract.ScheduledRecordings.START_TIME_COLUMN + " LONG NOT NULL DEFAULT 0,"
                        + Contract.ScheduledRecordings.END_TIME_COLUMN + " LONG NOT NULL DEFAULT 0,"
                        + Contract.ScheduledRecordings.DATA_COLUMN + " TEXT,"
                        + "UNIQUE(${BaseColumns._ID})"
                        +");")
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.SCAN_TYPE} TEXT DEFAULT 'free';")
            }
	     if(p1 <= 21) {
             p0!!.execSQL("DROP TABLE IF EXISTS $FAVORITES_TABLE")
             // Create favorite table
             p0.execSQL("CREATE TABLE $FAVORITES_TABLE ("
                     + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                     + Favorites.ORIGINAL_NETWORK_ID_COLUMN + " INTEGER NOT NULL,"
                     + Favorites.TRANSPORT_STREAM_ID_COLUMN + " INTEGER NOT NULL,"
                     + Favorites.SERVICE_ID_COLUMN + " INTEGER NOT NULL,"
                     + Favorites.COLUMN_TYPE + " INTEGER NOT NULL,"
                     + Favorites.COLUMN_LIST_IDS + " TEXT NOT NULL,"
                     + "UNIQUE(${BaseColumns._ID})"
                     +");")
            }
            if(p1 <= 22) {
                // Create favorite table
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.SUBTITLE_COLUMN_ID} INTEGER DEFAULT 1;")
            }

            if(p1 <= 23){
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.SWAP_CHANNEL} INTEGER DEFAULT 0;")
            }

            if (p1 <= 24) {
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.TERRESTRIAL_SCAN_ENABLED_COLUMN} INTEGER DEFAULT 1;")
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.CABLE_SCAN_ENABLED_COLUMN} INTEGER DEFAULT 0;")
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.SATELLITE_SCAN_ENABLED_COLUMN} INTEGER DEFAULT 0;")
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.SCAN_TYPE_SWITCH} INTEGER DEFAULT 0;")

                // Create configurable keys table
                p0!!.execSQL("DROP TABLE IF EXISTS $CONFIGURABLE_KEYS_TABLE")
                p0!!.execSQL(
                    "CREATE TABLE $CONFIGURABLE_KEYS_TABLE ("
                            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + Contract.ConfigurableKeys.KEY_NAME_COLUMN + " TEXT,"
                            + Contract.ConfigurableKeys.KEY_ACTION_TYPE_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                            + Contract.ConfigurableKeys.KEY_DESCRIPTION_COLUMN + " INTEGER NOT NULL DEFAULT 0,"
                            + "UNIQUE(${BaseColumns._ID})"
                            + ");"
                )
            }
            if (p1 <= 25) {
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.COUNTRY_SELECT} TEXT DEFAULT 'albania';")
            }

            if (p1 <= 26) {
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.SIGNAL_STATUS_MONITORING_ENABLED_COLUMN} INTEGER DEFAULT 0;")
            }

            if (p1 <= 27) {
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.LICENCE_KEY} TEXT DEFAULT '';")
            }

            if (p1 <= 28) {
                p0!!.execSQL("DROP TABLE IF EXISTS $FAVORITES_TABLE")
                // Create favorite table
                p0.execSQL("CREATE TABLE $FAVORITES_TABLE ("
                        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + Favorites.ORIGINAL_NETWORK_ID_COLUMN + " INTEGER NOT NULL,"
                        + Favorites.TRANSPORT_STREAM_ID_COLUMN + " INTEGER NOT NULL,"
                        + Favorites.SERVICE_ID_COLUMN + " INTEGER NOT NULL,"
                        + Favorites.COLUMN_TYPE + " INTEGER NOT NULL,"
                        + Favorites.COLUMN_LIST_IDS + " TEXT NOT NULL,"
                        + "UNIQUE(${BaseColumns._ID})"
                        +");")
            }

            if (p1 <= 29) {
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.CLEAR_CHANNELS_SCAN_OPTION_ENABLED_COLUMN} INTEGER DEFAULT 1;")
            }
            if (p1 <= 30) {
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.ASPECT_RATIO_ENABLED_COLUMN} INTEGER DEFAULT 0;")
            }
            if(p1 <= 31){
                // Create System information keys table
                p0!!.execSQL(
                    "CREATE TABLE $SYSTEM_INFO_TABLE ("
                            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + Contract.SystemInfo.RF_CHANNEL_NUMBER_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                            + Contract.SystemInfo.BER_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                            + Contract.SystemInfo.FREQUENCY_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                            + Contract.SystemInfo.PROG_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                            + Contract.SystemInfo.UEC_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                            + Contract.SystemInfo.SERVICEID_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                            + Contract.SystemInfo.POSTVITERBI_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                            + Contract.SystemInfo.TSID_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                            + Contract.SystemInfo.FIVES_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                            + Contract.SystemInfo.ONID_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                            + Contract.SystemInfo.AGC_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                            + Contract.SystemInfo.NETWORKID_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                            + Contract.SystemInfo.NETWORKNAME_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                            + Contract.SystemInfo.BANDWIDTH_ENABLED_COLUMN + " INTEGER NOT NULL DEFAULT 1,"
                            + "UNIQUE(${BaseColumns._ID})"
                            + ");"
                )
            }
            if (p1 <= 32) {
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.TELETEXT_ENABLE_COLUMN} INTEGER DEFAULT 1;")
            }
            if (p1 <= 33) {
                p0!!.execSQL("ALTER TABLE $OEM_CUSTOMIZATION_TABLE ADD ${OemCustomization.DELETE_THIRD_PARTY_INPUT_COLUMN} INTEGER DEFAULT 0;")
            }
        }
    }

    private lateinit var mOpenHelper: DBHelper

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        var count = 0
        db = mOpenHelper.writableDatabase
        when (sUriMatcher.match(uri)) {
            CHANNELS -> count = db!!.delete(CHANNELS_TABLE, selection, selectionArgs)
            CHANNELS_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.delete(
                    CHANNELS_TABLE, "${BaseColumns._ID} = $id" + tempSelection, selectionArgs
                )
            }
            CONFIG -> {
                count = db!!.delete(CONFIG_TABLE, selection, selectionArgs)
            }
            CONFIG_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.delete(
                    CONFIG_TABLE, "${BaseColumns._ID} = $id" + tempSelection, selectionArgs
                )
            }
            OEM_CUSTOMIZATION -> {
                count = db!!.delete(OEM_CUSTOMIZATION_TABLE, selection, selectionArgs)
            }
            OEM_CUSTOMIZATION_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.delete(
                    OEM_CUSTOMIZATION_TABLE,
                    "${BaseColumns._ID} = $id" + tempSelection,
                    selectionArgs
                )
            }
            LANGUAGES -> {
                count = db!!.delete(LANGUAGES_TABLE, selection, selectionArgs)
            }
            LANGUAGES_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.delete(
                    LANGUAGES_TABLE, "${BaseColumns._ID} = $id" + tempSelection, selectionArgs
                )
            }
            SCHEDULED_REMINDERS -> {
                count = db!!.delete(SCHEDULED_REMINDERS_TABLE, selection, selectionArgs)
            }
            SCHEDULED_REMINDERS_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.delete(
                    SCHEDULED_REMINDERS_TABLE,
                    "${BaseColumns._ID} = $id" + tempSelection,
                    selectionArgs
                )
            }
            SCHEDULED_RECORDINGS -> {
                count = db!!.delete(SCHEDULED_RECORDINGS_TABLE, selection, selectionArgs)
            }
            SCHEDULED_RECORDINGS_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.delete(
                    SCHEDULED_RECORDINGS_TABLE,
                    "${BaseColumns._ID} = $id" + tempSelection,
                    selectionArgs
                )
            }
            FAVORITES -> {
                count = db!!.delete(FAVORITES_TABLE, selection, selectionArgs)
            }
            FAVORITES_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.delete(
                    FAVORITES_TABLE, "${BaseColumns._ID} = $id" + tempSelection, selectionArgs
                )
            }
            CONFIGURABLE_KEYS -> {
                count = db!!.delete(CONFIGURABLE_KEYS_TABLE, selection, selectionArgs)
            }
            CONFIGURABLE_KEYS_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.delete(
                    CONFIGURABLE_KEYS_TABLE,
                    "${BaseColumns._ID} = $id" + tempSelection,
                    selectionArgs
                )
            }
            SYSTEM_INFO -> {
                count = db!!.delete(SYSTEM_INFO_TABLE, selection, selectionArgs)
            }
            SYSTEM_INFO_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.delete(
                    SYSTEM_INFO_TABLE,
                    "${BaseColumns._ID} = $id" + tempSelection,
                    selectionArgs
                )
            }
            else -> throw IllegalArgumentException("Unknown URI + $uri")
        }
        context!!.contentResolver.notifyChange(uri, null)
        return count;
    }

    override fun getType(uri: Uri): String? {
        when (sUriMatcher.match(uri)) {
            CHANNELS -> return Channels.CONTENT_TYPE
            CHANNELS_ID -> return Channels.CONTENT_ITEM_TYPE
            CONFIG -> return Config.CONTENT_TYPE
            CONFIG_ID -> return Config.CONTENT_ITEM_TYPE
            OEM_CUSTOMIZATION -> return OemCustomization.CONTENT_TYPE
            OEM_CUSTOMIZATION_ID -> return OemCustomization.CONTENT_ITEM_TYPE
            LANGUAGES -> return Contract.Languages.CONTENT_TYPE
            LANGUAGES_ID -> return Contract.Languages.CONTENT_ITEM_TYPE
            SCHEDULED_REMINDERS -> return Contract.ScheduledReminders.CONTENT_TYPE
            SCHEDULED_REMINDERS_ID -> return Contract.ScheduledReminders.CONTENT_ITEM_TYPE
            SCHEDULED_RECORDINGS -> return Contract.ScheduledRecordings.CONTENT_TYPE
            SCHEDULED_RECORDINGS_ID -> return Contract.ScheduledRecordings.CONTENT_ITEM_TYPE
            FAVORITES -> return Favorites.CONTENT_TYPE
            FAVORITES_ID -> return Favorites.CONTENT_ITEM_TYPE
            SYSTEM_INFO -> return Contract.SystemInfo.CONTENT_ITEM_TYPE
            SYSTEM_INFO_ID -> return Contract.SystemInfo.CONTENT_ITEM_TYPE
            else -> throw IllegalArgumentException("Unknown URI + $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        when (sUriMatcher.match(uri)) {
            CHANNELS -> {
                var retUri = insertChannel(uri, values!!)
                context!!.contentResolver.notifyChange(retUri, null)
                return retUri
            }
            CONFIG -> {
                var retUri = insertConfig(uri, values!!)
                context!!.contentResolver.notifyChange(retUri, null)
                return retUri
            }
            OEM_CUSTOMIZATION -> {
                var retUri = insertOemCustomization(uri, values!!)
                context!!.contentResolver.notifyChange(retUri, null)
                return retUri
            }
            LANGUAGES -> {
                var retUri = insertLanguages(uri, values!!)
                context!!.contentResolver.notifyChange(retUri, null)
                return retUri
            }
            SCHEDULED_REMINDERS -> {
                var retUri = insertScheduledReminder(uri, values!!)
                context!!.contentResolver.notifyChange(retUri, null)
                return retUri
            }
            SCHEDULED_RECORDINGS -> {
                var retUri = insertScheduledRecording(uri, values!!)
                context!!.contentResolver.notifyChange(retUri, null)
                return retUri
            }
            FAVORITES -> {
                var retUri = insertFavorite(uri, values!!)
                context!!.contentResolver.notifyChange(retUri, null)
                return retUri
            }
            CONFIGURABLE_KEYS -> {
                var retUri = insertConfigurableKey(uri, values!!)
                context!!.contentResolver.notifyChange(retUri, null)
                return retUri
            }
            SYSTEM_INFO -> {
                var retUri = insertSystemInformation(uri, values!!)
                context!!.contentResolver.notifyChange(retUri, null)
                return retUri
            }
            else -> throw IllegalArgumentException("Unknown uri $uri")
        }
    }

    override fun onCreate(): Boolean {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreate - create new DB")
        mOpenHelper = DBHelper(context!!)
        db = mOpenHelper.writableDatabase
        initOemCustomizationTable()
        initialConfigInsert()
        return true
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        val queryBuilder: SQLiteQueryBuilder = SQLiteQueryBuilder()
        var sortOrderTemp: String? = null
        var isQueryForChannel: Boolean = true
        when (sUriMatcher.match(uri)) {
            CHANNELS -> {
                queryBuilder.tables = CHANNELS_TABLE
                queryBuilder.projectionMap = CHANNELS_PROJECTION_MAP
                isQueryForChannel = true
            }
            CHANNELS_ID -> {
                queryBuilder.tables = CHANNELS_TABLE
                queryBuilder.appendWhere("${BaseColumns._ID}=${uri.pathSegments.get(1)}")
                isQueryForChannel = true
            }
            CONFIG -> {
                queryBuilder.tables = CONFIG_TABLE
                queryBuilder.projectionMap = CONFIG_PROJECTION_MAP
                isQueryForChannel = false
            }
            CONFIG_ID -> {
                queryBuilder.tables = CONFIG_TABLE
                queryBuilder.appendWhere("${BaseColumns._ID}=${uri.pathSegments.get(1)}")
                isQueryForChannel = false
            }
            OEM_CUSTOMIZATION -> {
                queryBuilder.tables = OEM_CUSTOMIZATION_TABLE
                queryBuilder.projectionMap = OEM_CUSTOMIZATION_PROJECTION_MAP
                isQueryForChannel = false
            }
            OEM_CUSTOMIZATION_ID -> {
                queryBuilder.tables = OEM_CUSTOMIZATION_TABLE
                queryBuilder.appendWhere("${BaseColumns._ID}=${uri.pathSegments.get(1)}")
                isQueryForChannel = false
            }
            LANGUAGES -> {
                queryBuilder.tables = LANGUAGES_TABLE
                queryBuilder.projectionMap = LANGUAGES_PROJECTION_MAP
                isQueryForChannel = false
            }
            LANGUAGES_ID -> {
                queryBuilder.tables = LANGUAGES_TABLE
                queryBuilder.appendWhere("${BaseColumns._ID}=${uri.pathSegments.get(1)}")
                isQueryForChannel = false
            }
            SCHEDULED_REMINDERS -> {
                queryBuilder.tables = SCHEDULED_REMINDERS_TABLE
                queryBuilder.projectionMap = SCHEDULED_REMINDERS_PROJECTION_MAP
                isQueryForChannel = false
            }
            SCHEDULED_REMINDERS_ID -> {
                queryBuilder.tables = SCHEDULED_REMINDERS_TABLE
                queryBuilder.appendWhere("${BaseColumns._ID}=${uri.pathSegments.get(1)}")
                isQueryForChannel = false
            }
            SCHEDULED_RECORDINGS -> {
                queryBuilder.tables = SCHEDULED_RECORDINGS_TABLE
                queryBuilder.projectionMap = SCHEDULED_RECORDINGS_PROJECTION_MAP
                isQueryForChannel = false
            }
            SCHEDULED_RECORDINGS_ID -> {
                queryBuilder.tables = SCHEDULED_RECORDINGS_TABLE
                queryBuilder.appendWhere("${BaseColumns._ID}=${uri.pathSegments.get(1)}")
                isQueryForChannel = false
            }
            CONFIG_ID_SCAMBLED -> {
                queryBuilder.tables = CONFIG_TABLE
                queryBuilder.appendWhere("${BaseColumns._ID}=${uri.pathSegments.get(1)}")
                isQueryForChannel = false
            }
            FAVORITES -> {
                queryBuilder.tables = FAVORITES_TABLE
                queryBuilder.projectionMap = FAVORITES_PROJECTION_MAP
                isQueryForChannel = false
            }
            FAVORITES_ID -> {
                queryBuilder.tables = FAVORITES_TABLE
                queryBuilder.appendWhere("${BaseColumns._ID}=${uri.pathSegments.get(1)}")
                isQueryForChannel = false
            }
            CONFIGURABLE_KEYS -> {
                queryBuilder.tables = CONFIGURABLE_KEYS_TABLE
                queryBuilder.projectionMap = CONFIGURABLE_KEYS_PROJECTION_MAP
                isQueryForChannel = false
            }
            CONFIGURABLE_KEYS_ID -> {
                queryBuilder.tables = CONFIGURABLE_KEYS_TABLE
                queryBuilder.appendWhere("${BaseColumns._ID}=${uri.pathSegments.get(1)}")
                isQueryForChannel = false
            }
            SYSTEM_INFO -> {
                queryBuilder.tables = SYSTEM_INFO_TABLE
                queryBuilder.projectionMap = SYSTEM_INFO_PROJECTION_MAP
                isQueryForChannel = false
            }
            SYSTEM_INFO_ID -> {
                queryBuilder.tables = SYSTEM_INFO_TABLE
                queryBuilder.appendWhere("${BaseColumns._ID}=${uri.pathSegments.get(1)}")
                isQueryForChannel = false
            }
        }

        if (sortOrder == null || sortOrder == "") {
            if (isQueryForChannel)
                sortOrderTemp = Channels.NAME_COLUMN
            else
                sortOrderTemp = BaseColumns._ID
        } else {
            sortOrderTemp = sortOrder
        }
        db = mOpenHelper.readableDatabase
        val cursor =
            queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrderTemp)
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        var count = 0
        db = mOpenHelper.writableDatabase
        when (sUriMatcher.match(uri)) {
            CHANNELS -> {
                count = db!!.update(CHANNELS_TABLE, values, selection, selectionArgs)
            }
            CHANNELS_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.update(
                    CHANNELS_TABLE,
                    values,
                    "${BaseColumns._ID} = $id" + tempSelection,
                    selectionArgs
                )
            }
            CONFIG -> {
                count = db!!.update(CONFIG_TABLE, values, selection, selectionArgs)
            }
            CONFIG_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.update(
                    CONFIG_TABLE, values, "${BaseColumns._ID} = $id" + tempSelection, selectionArgs
                )
            }
            OEM_CUSTOMIZATION -> {
                count = db!!.update(OEM_CUSTOMIZATION_TABLE, values, selection, selectionArgs)
            }
            OEM_CUSTOMIZATION_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.update(
                    OEM_CUSTOMIZATION_TABLE,
                    values,
                    "${BaseColumns._ID} = $id" + tempSelection,
                    selectionArgs
                )
            }
            LANGUAGES -> {
                count = db!!.update(LANGUAGES_TABLE, values, selection, selectionArgs)
            }
            LANGUAGES_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.update(
                    LANGUAGES_TABLE,
                    values,
                    "${BaseColumns._ID} = $id" + tempSelection,
                    selectionArgs
                )
            }
            SCHEDULED_REMINDERS -> {
                count = db!!.update(SCHEDULED_REMINDERS_TABLE, values, selection, selectionArgs)
            }
            SCHEDULED_REMINDERS_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.update(
                    SCHEDULED_REMINDERS_TABLE,
                    values,
                    "${BaseColumns._ID} = $id" + tempSelection,
                    selectionArgs
                )
            }
            SCHEDULED_RECORDINGS -> {
                count = db!!.update(SCHEDULED_RECORDINGS_TABLE, values, selection, selectionArgs)
            }
            SCHEDULED_RECORDINGS_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.update(
                    SCHEDULED_RECORDINGS_TABLE,
                    values,
                    "${BaseColumns._ID} = $id" + tempSelection,
                    selectionArgs
                )
            }
            CONFIG_ID_SCAMBLED -> {
                val id = uri.pathSegments.get(1)
                val column_name = uri.pathSegments.get(2)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Column_name from uri + $column_name")
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.update(
                    CONFIG_TABLE, values, "${BaseColumns._ID} = $id" + tempSelection, selectionArgs
                )
            }
            FAVORITES -> {
                count = db!!.update(FAVORITES_TABLE, values, selection, selectionArgs)
            }
            FAVORITES_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.update(
                    FAVORITES_TABLE,
                    values,
                    "${BaseColumns._ID} = $id" + tempSelection,
                    selectionArgs
                )
            }
            CONFIGURABLE_KEYS -> {
                count = db!!.update(CONFIGURABLE_KEYS_TABLE, values, selection, selectionArgs)
            }
            CONFIGURABLE_KEYS_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.update(
                    CONFIGURABLE_KEYS_TABLE,
                    values,
                    "${BaseColumns._ID} = $id" + tempSelection,
                    selectionArgs
                )
            }
            SYSTEM_INFO -> {
                count = db!!.update(SYSTEM_INFO_TABLE, values, selection, selectionArgs)
            }
            SYSTEM_INFO_ID -> {
                val id = uri.pathSegments.get(1)
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                count = db!!.update(
                    SYSTEM_INFO_TABLE,
                    values,
                    "${BaseColumns._ID} = $id" + tempSelection,
                    selectionArgs
                )
            }
        }
        context!!.contentResolver.notifyChange(uri, null)
        return count
    }

    fun insertChannel(uri: Uri, values: ContentValues): Uri {
        db = mOpenHelper.writableDatabase
        val rowId: Long = db!!.insert(CHANNELS_TABLE, null, values)
        if (rowId > 0) {
            return ContentUris.withAppendedId(uri, rowId)
        }
        throw  SQLException("Failed to insert row into $uri")
    }

    fun insertConfig(uri: Uri, values: ContentValues): Uri {
        db = mOpenHelper.writableDatabase
        val rowId: Long = db!!.insert(CONFIG_TABLE, null, values)
        if (rowId > 0) {
            val configUri: Uri = Contract.buildConfigUri(rowId)
            return configUri
        } else {
            throw  SQLException("Failed to insert row into $uri")
        }
    }

    fun insertOemCustomization(uri: Uri, values: ContentValues): Uri {
        db = mOpenHelper.writableDatabase
        val rowId: Long = db!!.insert(OEM_CUSTOMIZATION_TABLE, null, values)
        if (rowId > 0) {
            val configUri: Uri = Contract.buildOemCustomizationUri(rowId)
            return configUri
        } else {
            throw  SQLException("Failed to insert row into $uri")
        }
    }

    fun insertLanguages(uri: Uri, values: ContentValues): Uri {
        db = mOpenHelper.writableDatabase
        val rowId: Long = db!!.insert(LANGUAGES_TABLE, null, values)
        if (rowId > 0) {
            val configUri: Uri = Contract.buildLanguagesUri(rowId)
            return configUri
        } else {
            throw  SQLException("Failed to insert row into $uri")
        }
    }

    fun insertScheduledReminder(uri: Uri, values: ContentValues): Uri {
        db = mOpenHelper.writableDatabase
        val rowId: Long = db!!.insert(SCHEDULED_REMINDERS_TABLE, null, values)
        if (rowId > 0) {
            val configUri: Uri = Contract.buildScheduledRemindersUri(rowId)
            return configUri
        } else {
            throw  SQLException("Failed to insert row into $uri")
        }
    }

    fun insertScheduledRecording(uri: Uri, values: ContentValues): Uri {
        db = mOpenHelper.writableDatabase
        val rowId: Long = db!!.insert(SCHEDULED_RECORDINGS_TABLE, null, values)
        if (rowId > 0) {
            val configUri: Uri = Contract.buildScheduledRecordingsUri(rowId)
            return configUri
        } else {
            throw  SQLException("Failed to insert row into $uri")
        }
    }

    fun insertConfigurableKey(uri: Uri, values: ContentValues): Uri {
        db = mOpenHelper.writableDatabase
        val rowId: Long = db!!.insert(CONFIGURABLE_KEYS_TABLE, null, values)
        if (rowId > 0) {
            val configUri: Uri = Contract.buildConfigurableKeysUri(rowId)
            return configUri
        } else {
            throw  SQLException("Failed to insert row into $uri")
        }
    }

    fun insertFavorite(uri: Uri, values: ContentValues): Uri {
        db = mOpenHelper.writableDatabase
        val rowId: Long = db!!.insert(FAVORITES_TABLE, null, values)
        if (rowId > 0) {
            val favoriteUri: Uri = Contract.buildFavoritesUri(rowId)
            return favoriteUri
        } else {
            throw SQLException("Failied to insert row into $uri")
        }
    }

    fun insertSystemInformation(uri: Uri, values: ContentValues): Uri {
        db = mOpenHelper.writableDatabase
        val rowId: Long = db!!.insert(SYSTEM_INFO_TABLE, null, values)
        if (rowId > 0) {
            val systemInfroUri: Uri = Contract.buildSystemInfoUri(rowId)
            return systemInfroUri
        } else {
            throw SQLException("Failied to insert row into $uri")
        }
    }

    override fun bulkInsert(uri: Uri, values: Array<out ContentValues>): Int {
        //var ret = super.bulkInsert(uri, values)
        //context!!.contentResolver.notifyChange(uri,null)
        //return ret
        when (sUriMatcher.match(uri)) {
            CHANNELS -> {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Enter custom bulk for channels")
                var counter = 0
                db = mOpenHelper.writableDatabase
                db!!.beginTransaction()
                try {
                    for (cv in values) {
                        val rowId : Long = db!!.insert(CHANNELS_TABLE, null, cv)
                        if (rowId<=0) {
                            throw SQLException("Failed to insert row into $uri")
                        } else {
                            counter++
                        }
                    }
                    db!!.setTransactionSuccessful()
                } catch (e: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "$e")
                } finally {
                    db!!.endTransaction()
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Notify observer that bulk is finished")
                    context!!.contentResolver.notifyChange(uri,null)
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Channels inserted $counter")
                    return counter
                }
            }
            else -> {
                return super.bulkInsert(uri, values)
            }
        }
    }

    private fun initialConfigInsert() {
        val cursor = query(CONFIG_URI, null, null, null, null)
        if (!cursor!!.moveToFirst()) {
            var cv = ContentValues()
            cv.put(Config.LCN_COLUMN, "true")
            cv.put(Config.PIN_COLUMN,"0000")
            cv.put(Config.SCAN_TYPE,"all")
            cv.put(Config.IS_RECORDING_STARTED,"false")
            insert(CONFIG_URI, cv)
        }
    }

    private fun initOemCustomizationTable() {
        val cursor = query(OEM_CUSTOMIZATION_URI, null, null, null, null)
        if (!cursor!!.moveToFirst()) {
            var cv = ContentValues()
            cv.put(OemCustomization.PVR_ENABLED_COLUMN, 1)
            cv.put(OemCustomization.VIRTUAL_RCU_COLUMN, 0)
            cv.put(OemCustomization.THIRD_PARTY_INPUT_ENABLED_COLUMN, 0)
            cv.put(OemCustomization.VERTICAL_EPG_COLUMN, 0)
            cv.put(OemCustomization.CHANNEL_LOGO_METADATA_ENABLED_COLUMN, 0)
            cv.put(OemCustomization.SCAN_TYPE, "free")
            cv.put(OemCustomization.SUBTITLE_COLUMN_ID, 1)
            cv.put(OemCustomization.SWAP_CHANNEL, 0)
            cv.put(OemCustomization.SCAN_TYPE_SWITCH, 0)
            cv.put(OemCustomization.COUNTRY_SELECT, "albania")
            cv.put(OemCustomization.CLEAR_CHANNELS_SCAN_OPTION_ENABLED_COLUMN, 1)
            cv.put(OemCustomization.ASPECT_RATIO_ENABLED_COLUMN, 0)
            cv.put(OemCustomization.TELETEXT_ENABLE_COLUMN, 1)
            cv.put(OemCustomization.DELETE_THIRD_PARTY_INPUT_COLUMN, 0)
            cv.put(OemCustomization.TIMESHIFT_ENABLED_COLUMN, 0)
            insert(OEM_CUSTOMIZATION_URI, cv)
        }
    }
}