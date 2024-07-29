package com.iwedia.cltv.platform.refplus5.audio


import android.net.Uri
import android.provider.BaseColumns._ID

object SoundDBHelper {
    const val ITEM = "item"
    const val SOURCE_ID = "Source"

    val INPUT_SOURCE_CONTENT_URI =
        Uri.parse("content://" + Constants.AUTHORITY + "/" + Source.SCHEDULE_CONTACTS_TABLE)
    val SOUND_STYLE_CONTENT_URI =
        Uri.parse("content://" + Constants.AUTHORITY + "/" + SoundStyle.SCHEDULE_CONTACTS_TABLE)
    const val UPDATE_SOURCE = "update_source"
    const val UPDATE_SOUND_STYLE = "update_sound_style"
    const val RESET_DEFAULT = "reset_default"
    const val RESET_DIGITAL = "reset_digital"
    const val SOUND_STYLE_CONTENT_URI_DEFAULT_VALUE = "query_default_value"
    val SOUND_STYLE_CONTENT_DEFAULT_VALUE_URI = Uri.parse("content://" + Constants.AUTHORITY + "/" + SOUND_STYLE_CONTENT_URI_DEFAULT_VALUE)

    object Source {
        // Columns for InputSourceDB
        /** Type: TEXT */
        const val COMMON = "common"

        /** Type: TEXT */
        const val DTV = "dtv"

        /** Type: TEXT */
        const val ATV = "atv"

        /** Type: TEXT */
        const val COMPOSITE = "composite"

        /** Type: TEXT */
        const val COMPONENT = "component"

        /** Type: TEXT */
        const val VGA = "vga"

        /** Type: TEXT */
        const val HDMI1 = "hdmi1"

        /** Type: TEXT */
        const val HDMI2 = "hdmi2"

        /** Type: TEXT */
        const val HDMI3 = "hdmi3"

        /** Type: TEXT */
        const val HDMI4 = "hdmi4"

        /** Type: TEXT */
        const val MM = "mm"

        const val SCHEDULE_CONTACTS_TABLE = "input_source"

        const val INPUT_SOURCE_DATABASE_CREATE =
            ("CREATE TABLE " +
                    SCHEDULE_CONTACTS_TABLE +
                    " (" +
                    _ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    ITEM +
                    " TEXT," +
                    COMMON +
                    " TEXT," +
                    DTV +
                    " TEXT," +
                    ATV +
                    " TEXT," +
                    COMPOSITE +
                    " TEXT," +
                    COMPONENT +
                    " TEXT," +
                    VGA +
                    " TEXT," +
                    HDMI1 +
                    " TEXT," +
                    HDMI2 +
                    " TEXT," +
                    HDMI3 +
                    " TEXT," +
                    HDMI4 +
                    " TEXT," +
                    MM +
                    " TEXT);")
    }

    object SoundStyle {
        const val SCHEDULE_CONTACTS_TABLE = "sound_style"

        /** Type: TEXT */
        const val USER = "User"

        /** Type: TEXT */
        const val STANDARD = "Standard"

        /** Type: TEXT */
        const val VIVID = "Vivid"

        /** Type: TEXT */
        const val SPORTS = "Sports"

        /** Type: TEXT */
        const val MOVIE = "Movie"

        /** Type: TEXT */
        const val MUSIC = "Music"

        /** Type: TEXT */
        const val NEWS = "News"

        /** Type: TEXT */
        const val AUTO = "Auto"

        const val SOUND_STYLE_DATABASE_CREATE =
            ("CREATE TABLE " +
                    SCHEDULE_CONTACTS_TABLE +
                    " (" +
                    _ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    ITEM +
                    " TEXT," +
                    USER +
                    " TEXT," +
                    STANDARD +
                    " TEXT," +
                    VIVID +
                    " TEXT," +
                    SPORTS +
                    " TEXT," +
                    MOVIE +
                    " TEXT," +
                    MUSIC +
                    " TEXT," +
                    NEWS +
                    " TEXT," +
                    AUTO +
                    " TEXT);")
    }
    var mSource: String = Source.MM
}
