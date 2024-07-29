package com.iwedia.cltv.platform.base.provider

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.iwedia.cltv.platform.base.content_provider.ReferenceContract

const val AUTHORITY = "com.iwedia.cltv.content.provider.test"

const val CHANNELS_TABLE = "channels"
const val CONFIG_TABLE = "config"
const val OEM_CUSTOMIZATION_TABLE = "oem_customization"
const val LANGUAGES_TABLE = "languages"
const val SCHEDULED_REMINDERS_TABLE = "scheduled_reminders"
const val SCHEDULED_RECORDINGS_TABLE = "scheduled_recordings"
const val FAVORITES_TABLE = "favorites"
const val CONFIGURABLE_KEYS_TABLE = "configurable_keys"
const val SYSTEM_INFO_TABLE = "system_info"

val CHANNELS_URI: Uri = Uri.parse("content://$AUTHORITY/$CHANNELS_TABLE")
val CONFIG_URI: Uri = Uri.parse("content://$AUTHORITY/$CONFIG_TABLE")
val OEM_CUSTOMIZATION_URI: Uri = Uri.parse("content://$AUTHORITY/$OEM_CUSTOMIZATION_TABLE")
val LANGUAGES_URI: Uri = Uri.parse("content://$AUTHORITY/$LANGUAGES_TABLE")
val SCHEDULED_REMINDERS_URI: Uri = Uri.parse("content://$AUTHORITY/$SCHEDULED_REMINDERS_TABLE")
val SCHEDULED_RECORDINGS_URI: Uri = Uri.parse("content://$AUTHORITY/$SCHEDULED_RECORDINGS_TABLE")
val FAVORITES_URI: Uri = Uri.parse("content://$AUTHORITY/$FAVORITES_TABLE")
val CONFIGURABLE_KEYS_URI: Uri = Uri.parse("content://$AUTHORITY/$CONFIGURABLE_KEYS_TABLE")
val SYSTEM_INFO_URI: Uri = Uri.parse("content://$AUTHORITY/$SYSTEM_INFO_TABLE")

fun ContentResolver.insertChannel(
    package_name: String = "com.google.android.tv.dtvinput",
    input_id: String = "com.google.android.tv.dtvinput/.DtvInputService",
    type: String = "TYPE_DVB_T",
    service_type: String = "SERVICE_TYPE_AUDIO_VIDEO",
    original_network_id: Int = 8572,
    transport_stream_id: Int = 143,
    service_id: Int = 1264,
    display_number: String = "8",
    display_name: String = "Rai 3 TGR Lazio",
    network_affiliation: String? = null,
    description: String? = null,
    video_format: String? = null,
    browsable: Int = 1,
    searchable: Int = 1,
    locked: Int = 0,
    app_link_icon_uri: String? = null,
    app_link_poster_art_uri: String? = null,
    app_link_text: String? = null,
    app_link_color: Int? = null,
    app_link_intent_uri: String? = null,
    internal_provider_id: String? = null,
    internal_provider_data: ByteArray? = null,
    internal_provider_flag1: Int = 3,
    logo: ByteArray? = null,
    version_number: Int? = null,
    transient: Int = 0,
    iw_skip: Int = 0,
    iw_original_id: Int = 0,
    iw_display_name: String? = null,
    iw_display_number_changed: Int = 0,
    ordinal_number: Int = 0,
    is_deleted: Int = 0,
    canonical_genre: String = ""
): Uri? {
    val contentValues = ContentValues()
    contentValues.put(ReferenceContract.Channels.PACKAGE_NAME_COLUMN, package_name)
    contentValues.put(ReferenceContract.Channels.INPUT_ID_COLUMN, input_id)
    contentValues.put(ReferenceContract.Channels.TYPE_COLUMN, type)
    contentValues.put(ReferenceContract.Channels.SERVICE_TYPE_COLUMN, service_type)
    contentValues.put(ReferenceContract.Channels.ORIGINAL_NETWORK_ID_COLUMN, original_network_id)
    contentValues.put(ReferenceContract.Channels.TRANSPORT_STREAM_ID_COLUMN, transport_stream_id)
    contentValues.put(ReferenceContract.Channels.SERVICE_ID_COLUMN, service_id)
    contentValues.put(ReferenceContract.Channels.DISPLAY_NUMBER_COLUMN, display_number)
    contentValues.put(ReferenceContract.Channels.NAME_COLUMN, display_name)
    contentValues.put(ReferenceContract.Channels.NETWORK_AFFILIATION_COLUMN, network_affiliation)
    contentValues.put(ReferenceContract.Channels.DESCRIPTION_COLUMN, description)
    contentValues.put(ReferenceContract.Channels.VIDEO_FORMAT_COLUMN, video_format)
    contentValues.put(ReferenceContract.Channels.BROWSABLE_COLUMN, browsable)
    contentValues.put(ReferenceContract.Channels.SEARCHABLE_COLUMN, searchable)
    contentValues.put(ReferenceContract.Channels.LOCKED_COLUMN, locked)
    contentValues.put(ReferenceContract.Channels.APP_LINK_ICON_URI_COLUMN, app_link_icon_uri)
    contentValues.put(ReferenceContract.Channels.APP_LINK_POSTER_ART_URI_COLUMN, app_link_poster_art_uri)
    contentValues.put(ReferenceContract.Channels.APP_LINK_TEXT_COLUMN, app_link_text)
    contentValues.put(ReferenceContract.Channels.APP_LINK_COLOR_COLUMN, app_link_color)
    contentValues.put(ReferenceContract.Channels.APP_LINK_INTENT_URI_COLUMN, app_link_intent_uri)
    contentValues.put(ReferenceContract.Channels.INTERNAL_PROVIDER_ID_COLUMN, internal_provider_id)
    contentValues.put(ReferenceContract.Channels.INTERNAL_PROVIDER_DATA_COLUMN, internal_provider_data)
    contentValues.put(ReferenceContract.Channels.INTERNAL_PROVIDER_FLAG1_COLUMN, internal_provider_flag1)
    //contentValues.put(ReferenceContract.Channels.LOGO_COLUMN, logo)
    contentValues.put(ReferenceContract.Channels.VERSION_NUMBER_COLUMN, version_number)
    contentValues.put(ReferenceContract.Channels.TRANSIENT_COLUMN, transient)
    contentValues.put(ReferenceContract.Channels.SKIP_COLUMN, iw_skip)
    contentValues.put(ReferenceContract.Channels.ORIG_ID_COLUMN, iw_original_id)
    contentValues.put(ReferenceContract.Channels.REFERENCE_NAME_COLUMN, iw_display_name)
    contentValues.put(ReferenceContract.Channels.DISPLAY_NUMBER_CHANGED_COLUMN, iw_display_number_changed)
    contentValues.put(ReferenceContract.Channels.ORDINAL_NUMBER_COLUMN, ordinal_number)
    contentValues.put(ReferenceContract.Channels.DELETED_COLUMN, is_deleted)
    //contentValues.put(ReferenceContract.Channels.CANONICAL_GENRE_COLUMN, canonical_genre)

    return insert(CHANNELS_URI, contentValues)
}

fun ContentResolver.insertFavorite(
    /*content_type: String = "com.google.android.tv.dtvinput",
    content_item_type: String = "com.google.android.tv.dtvinput/.DtvInputService",*/
    original_network_id_column: Int = 8572,
    transport_stream_id_column: Int = 143,
    service_id_column: Int = 1264,
    column_type: String = "TYPE_DVB_T",
    column_list_Ids: String = "Favorite 1, Favorite 2, Favorite 3, Favorite 4, Favorite 5"
): Uri? {
    val contentValues = ContentValues()
    /*contentValues.put(ReferenceContract.Favorites.CONTENT_TYPE, content_type)
    contentValues.put(ReferenceContract.Favorites.CONTENT_ITEM_TYPE, content_item_type)*/
    contentValues.put(ReferenceContract.Favorites.ORIGINAL_NETWORK_ID_COLUMN, original_network_id_column)
    contentValues.put(ReferenceContract.Favorites.TRANSPORT_STREAM_ID_COLUMN, transport_stream_id_column)
    contentValues.put(ReferenceContract.Favorites.SERVICE_ID_COLUMN, service_id_column)
    contentValues.put(ReferenceContract.Favorites.COLUMN_TYPE, column_type)
    contentValues.put(ReferenceContract.Favorites.COLUMN_LIST_IDS, column_list_Ids)

    return insert(FAVORITES_URI, contentValues)
}

fun ContentResolver.insertReminder(
    name_column: String = "scheduledReminder",
    channel_id_column: Int = 1,
    event_id_column: Int = 2,
    start_id_column: Long = System.currentTimeMillis()
): Uri? {
    val contentValues = ContentValues()
    contentValues.put(ReferenceContract.ScheduledReminders.NAME_COLUMN, name_column)
    contentValues.put(ReferenceContract.ScheduledReminders.CHANNEL_ID_COLUMN, channel_id_column)
    contentValues.put(ReferenceContract.ScheduledReminders.EVENT_ID_COLUMN, event_id_column)
    contentValues.put(ReferenceContract.ScheduledReminders.START_TIME_COLUMN, start_id_column)

    return insert(SCHEDULED_REMINDERS_URI, contentValues)
}

fun ContentResolver.insertRecordings(
    name_column: String = "scheduledReminder",
    channel_id_column: Int = 1,
    event_id_column: Int = 2,
    start_id_column: Long = System.currentTimeMillis(),
    end_id_column: Long = System.currentTimeMillis() + 1000000,
    data:Int =  0
): Uri? {
    val contentValues = ContentValues()
    contentValues.put(ReferenceContract.ScheduledRecordings.NAME_COLUMN, name_column)
    contentValues.put(ReferenceContract.ScheduledRecordings.CHANNEL_ID_COLUMN, channel_id_column)
    contentValues.put(ReferenceContract.ScheduledRecordings.TV_EVENT_ID_COLUMN, event_id_column)
    contentValues.put(ReferenceContract.ScheduledRecordings.START_TIME_COLUMN, start_id_column)
    contentValues.put(ReferenceContract.ScheduledRecordings.END_TIME_COLUMN, end_id_column)
    contentValues.put(ReferenceContract.ScheduledRecordings.DATA_COLUMN, data)

    return insert(SCHEDULED_RECORDINGS_URI, contentValues)
}

fun Cursor.ci(columnName: String) = getColumnIndex(columnName)