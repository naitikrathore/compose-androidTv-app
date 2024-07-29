package com.iwedia.cltv.platform.base.content_provider

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.os.Build
import android.provider.BaseColumns
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.`interface`.ChannelDataProviderInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.channel.VideoResolution
import com.iwedia.cltv.platform.model.content_provider.Contract
import com.iwedia.cltv.platform.model.favorite.FavoriteItem
import com.iwedia.cltv.platform.model.favorite.FavoriteItemType
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.RepeatFlag
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import java.io.*

@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("Range")
fun createChannelFromCursor(context: Context? = null, cursor: Cursor, index: Int = 0): TvChannel {
    var id = -1
    var inputId = ""
    var displayNumber = "0"
    var displayName = ""
    var logoImagePath = ""
    var isRadioChannel = false
    var isLocked = false
    var tunerType = TunerType.DEFAULT
    var tsId = 0
    var onId = 0
    var serviceId = 0
    var isBrowsable = true
    var packageName = ""
    var typeFullName = ""
    var genres = arrayListOf<String>()
    var providerFlag1: Int? = null
    var providerFlag2: Int? = null
    var providerFlag3: Int? = null
    var providerFlag4: Int? = null
    var internalProviderId: String? = null

    var referenceVideoQuality = ArrayList<VideoResolution>()
    try {
        if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels._ID)) != null) {
            id = cursor.getInt(cursor.getColumnIndex(TvContract.Channels._ID))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INPUT_ID)) != null) {
            inputId =
                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INPUT_ID))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER)) != null) {
            displayNumber =
                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME)) != null) {
            displayName =
                (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME))).trim()
        }

        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_ID)) != null) {
            serviceId =
                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_ID))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID)) != null) {
            tsId =
                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID)) != null) {
            onId =
                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_ID)) != null) {
            internalProviderId =  (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_ID)))
        }
        //TODO uncomment in order to have channel logo in app
        /* if (context != null) {
             logoImagePath = loadLogoUrl(context, displayName, id.toLong())!!
         } else */if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_ICON_URI)) != null) {
            logoImagePath =
                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_ICON_URI))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_TYPE)) != null) {
            isRadioChannel =
                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_TYPE)) == TvContract.Channels.SERVICE_TYPE_AUDIO
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_VIDEO_FORMAT)) != null) {
            var videoFormat =
                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_VIDEO_FORMAT))
            var videoResolution = TvContract.Channels.getVideoResolution(videoFormat)
            when (videoResolution) {
                TvContract.Channels.VIDEO_RESOLUTION_ED -> {
                    referenceVideoQuality.add(VideoResolution.VIDEO_RESOLUTION_ED)
                }
                TvContract.Channels.VIDEO_RESOLUTION_FHD -> {
                    referenceVideoQuality.add(VideoResolution.VIDEO_RESOLUTION_FHD)
                }
                TvContract.Channels.VIDEO_RESOLUTION_HD -> {
                    referenceVideoQuality.add(VideoResolution.VIDEO_RESOLUTION_HD)
                }
                TvContract.Channels.VIDEO_RESOLUTION_SD -> {
                    referenceVideoQuality.add(VideoResolution.VIDEO_RESOLUTION_SD)
                }
                TvContract.Channels.VIDEO_RESOLUTION_UHD -> {
                    referenceVideoQuality.add(VideoResolution.VIDEO_RESOLUTION_UHD)
                }
            }
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_TYPE)) != null) {
            // TODO Add support for other standard not only for DVB
            val type =
                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_TYPE))
            if (type == TvContract.Channels.TYPE_DVB_T || type == TvContract.Channels.TYPE_DVB_T2 ||
                type == TvContract.Channels.TYPE_ATSC_T || type == TvContract.Channels.TYPE_ATSC3_T) {
                tunerType = TunerType.TERRESTRIAL_TUNER_TYPE
            }
            if (type == TvContract.Channels.TYPE_DVB_C || type == TvContract.Channels.TYPE_DVB_C2 ||
                type == TvContract.Channels.TYPE_ATSC_C) {
                tunerType = TunerType.CABLE_TUNER_TYPE
            }
            if (type == TvContract.Channels.TYPE_NTSC || type == TvContract.Channels.TYPE_PAL ||
                type == TvContract.Channels.TYPE_SECAM) {
                tunerType = TunerType.ANALOG_TUNER_TYPE
            }
            if (type == TvContract.Channels.TYPE_DVB_S || type == TvContract.Channels.TYPE_DVB_S2) {
                tunerType = TunerType.SATELLITE_TUNER_TYPE
            }
            typeFullName = type
        }

        if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_BROWSABLE)) != null) {
            isBrowsable =
                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_BROWSABLE)) == 1
        }
        if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_LOCKED)) != null) {
            isLocked =
                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_LOCKED)) == 1
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_PACKAGE_NAME)) != null) {
            packageName =
                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_PACKAGE_NAME))
        }

        if(typeFullName!!.contains("ATSC") || typeFullName!!.contains("NTSC")){
            genres = arrayListOf()
        } else if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DESCRIPTION)) != null) {
            var channelGenres =
                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DESCRIPTION))
            val items = channelGenres.split(",")
            items.forEach { genre->
                if (!genre.equals("description")) {
                    genres.add(genre)
                }
            }
        }

        if (cursor.getLong(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1)) != null) {
            providerFlag1 =
                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1))
        }

        if (cursor.getLong(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2)) != null) {
            providerFlag2 =
                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2))
        }

        if (cursor.getLong(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG3)) != null) {
            providerFlag3 =
                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG3))
        }

        if (cursor.getLong(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG4)) != null) {
            providerFlag4 =
                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG4))
        }
    } catch (e: Exception) {
         e.printStackTrace()
     }

    return TvChannel(
        id = id,
        index = index,
        name = displayName,
        logoImagePath = logoImagePath,
        videoQuality = referenceVideoQuality,
        channelId = id.toLong(),
        inputId = inputId,
        displayNumber = displayNumber,
        isRadioChannel = isRadioChannel,
        tunerType = tunerType,
        isLocked = isLocked,
        tsId = tsId,
        onId = onId,
        serviceId = serviceId,
        isBrowsable = isBrowsable,
        packageName = packageName,
        genres = genres,
        type = typeFullName,
        providerFlag1 = providerFlag1,
        providerFlag2 = providerFlag2,
        providerFlag3 = providerFlag3,
        providerFlag4 = providerFlag4,
        internalProviderId = internalProviderId
    )
}

/**
 * Load channel logo path
 *
 * @param channelName
 * @param channelId
 * @return
 */
fun loadLogoUrl(context: Context, channelName: String, channelId: Long): String? {
    val channelLogoUri = TvContract.buildChannelLogoUri(channelId)
    val fileName = "/" + channelName.replace(' ', '_') + channelId.toString()
    try {
        val inputStream: InputStream =
            context.contentResolver.openInputStream(
                channelLogoUri
            )!!
        val outputStream: OutputStream =
            FileOutputStream(
                File(
                    context.filesDir,
                    fileName
                )
            )
        copy(inputStream, outputStream)
        inputStream.close()
        outputStream.close()
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return context.filesDir.toString() + fileName
}

/**
 * Copy input stream into the output stream
 *
 * @param is input stream
 * @param os output stream
 * @throws IOException
 */
@Throws(IOException::class)
private fun copy(`is`: InputStream, os: OutputStream) {
    val buffer = ByteArray(1024)
    var len: Int
    while (`is`.read(buffer).also { len = it } != -1) {
        os.write(buffer, 0, len)
    }
}

fun getInputIds(context: Context): ArrayList<String>? {
    val retList = ArrayList<String>()
    //Get all TV inputs
    for (input in (context.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager).tvInputList) {
        val inputId = input.id
        retList.add(inputId)
    }
    return retList
}

@SuppressLint("Range")
fun createTvEventFromCursor(
    tvChannel: TvChannel,
    cursor: Cursor
): TvEvent {
    var id = -1
    var name = ""
    var shortDescription = ""
    var longDescription = ""
    var imagePath = ""
    var eventStart = 0L
    var eventEnd = 0L
    var parentalRating: String? = null
    var providerFlag: Int? = null
    var genre = ""
    var eventId : Int? = -1

    try {
        if (cursor.getLong(cursor.getColumnIndex(TvContract.Programs._ID)) != null) {
            id = cursor.getLong(cursor.getColumnIndex(TvContract.Programs._ID)).toInt()
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_TITLE)) != null) {
            name = cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_TITLE))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_SHORT_DESCRIPTION)) != null) {
            shortDescription =
                cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_SHORT_DESCRIPTION))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_LONG_DESCRIPTION)) != null) {
            longDescription =
                cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_LONG_DESCRIPTION))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_THUMBNAIL_URI)) != null) {
            imagePath =
                cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_THUMBNAIL_URI))
        }
        if (cursor.getLong(cursor.getColumnIndex(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS)) != null) {
            eventStart =
                cursor.getLong(cursor.getColumnIndex(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS))
        }
        if (cursor.getLong(cursor.getColumnIndex(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS)) != null) {
            eventEnd =
                cursor.getLong(cursor.getColumnIndex(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_CONTENT_RATING)) != null) {

            parentalRating =
                cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_CONTENT_RATING))
        }
        if (parentalRating == null) parentalRating = ""
        if (cursor.getLong(cursor.getColumnIndex(TvContract.Programs.COLUMN_INTERNAL_PROVIDER_FLAG1)) != null) {
            providerFlag =
                cursor.getInt(cursor.getColumnIndex(TvContract.Programs.COLUMN_INTERNAL_PROVIDER_FLAG1))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_BROADCAST_GENRE)) != null) {
            genre = cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_BROADCAST_GENRE))
        }

        if (cursor.getInt(cursor.getColumnIndex(TvContract.Programs.COLUMN_EVENT_ID)) != null) {
            eventId = cursor.getInt(cursor.getColumnIndex(TvContract.Programs.COLUMN_EVENT_ID))
        }

        shortDescription = shortDescription.trim().replace("\\s+", " ")
        longDescription = longDescription.trim().replace("\\s+", " ")
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return TvEvent(
        id,
        tvChannel,
        name,
        shortDescription,
        longDescription,
        imagePath,
        startTime = eventStart,
        endTime = eventEnd,
        categories = arrayListOf<Int>() ,
        parentalRating = parentalRating,
        providerFlag = providerFlag,
        isInitialChannel = false,
        rating = 0,
        isProgramSame = false,
        parentalRate = 0,
        genre = genre,
        tvEventId = eventId
    )
}

fun createDummyServiceForRecording(inputId : String, name : String, displayNumber : String) : TvChannel {
    return TvChannel(
        id = 0,
        index = 0,
        name = name,
        logoImagePath = "",
        videoQuality = ArrayList(),
        channelId = 0,
        inputId = inputId,
        displayNumber = displayNumber,
        isRadioChannel = false,
        tunerType = TunerType.DEFAULT,
        isLocked = false,
        tsId = 0,
        onId = 0,
        serviceId = 0,
        isBrowsable = true,
        packageName = "",
        genres = ArrayList(),
        type = "",
        providerFlag1 = 0,
        providerFlag2 = 0,
        providerFlag3 = 0,
        providerFlag4 = 0
    )
}

@SuppressLint("Range")
fun createRecordingsFromCursor(cursor: Cursor, tvModule: TvInterface, timeInterface: TimeInterface, utilsInterface: UtilsInterface): Recording? {
    var recordingId = -1
    var recordingName = ""
    var recordingDuration = 0L
    var recordingStartTime = 0L
    var recordingEndTime = 0L
    var recordingThumbnail = ""
    var recordingUri = ""
    var recordingTvChannel: TvChannel? = null
    var recordedTvEvent: TvEvent? = null
    var recordingShortDescription = ""
    var recordingLongDescription = ""
    var contentRating = ""
    var inputId = ""

    try {
        if (cursor.getInt(cursor.getColumnIndex(TvContract.RecordedPrograms._ID)) != null) {
            recordingId = cursor.getInt(cursor.getColumnIndex(TvContract.RecordedPrograms._ID))
        }

        if (cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_CONTENT_RATING)) != null){
            contentRating = cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_CONTENT_RATING))
        }

        if (cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_TITLE)) != null) {
            recordingName =
                cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_TITLE))
        }

        if (cursor.getLong(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_RECORDING_DURATION_MILLIS)) != null) {
            recordingDuration =
                cursor.getLong(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_RECORDING_DURATION_MILLIS))
        }

        if (cursor.getLong(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_END_TIME_UTC_MILLIS)) != null) {
            recordingEndTime =
                cursor.getLong(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_END_TIME_UTC_MILLIS))
        }

        if (cursor.getLong(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_START_TIME_UTC_MILLIS)) != null) {
            recordingStartTime =
                cursor.getLong(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_START_TIME_UTC_MILLIS))
        }

        if (cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_THUMBNAIL_URI)) != null) {
            recordingThumbnail =
                cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_THUMBNAIL_URI))
        }

        if (cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_RECORDING_DATA_URI)) != null) {
            recordingUri =
                cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_RECORDING_DATA_URI))
        }

        if (cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_SHORT_DESCRIPTION)) != null) {
            recordingShortDescription =
                cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_SHORT_DESCRIPTION))
        }

        if (cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_LONG_DESCRIPTION)) != null) {
            recordingLongDescription =
                cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_LONG_DESCRIPTION))
        }

        recordingTvChannel = tvModule?.getChannelById(
            cursor.getLong(
                cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_CHANNEL_ID)
            ).toInt()
        )

        if (recordingTvChannel == null) {
            var displayNumber = ""
            var displayName = ""

            if (cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_INPUT_ID)) != null) {
                inputId =
                    cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_INPUT_ID))
            } else {
                return null
            }

            if(utilsInterface.getPlatformPreferences(UtilsInterface.PlatformPreference.USE_POSTER_THUMB_RECORDING_FALLBACK, false) == true) {
                displayNumber = recordingThumbnail
                if (cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_POSTER_ART_URI)) != null) {
                    displayName =
                        cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_POSTER_ART_URI))
                    Log.d("DataProviderUtil","Got fallback channel : $displayNumber $displayName for $recordingId")
                }
            }

            recordingTvChannel = createDummyServiceForRecording(inputId, displayName, displayNumber)
        }
        recordingEndTime = recordingStartTime + recordingDuration

        recordedTvEvent = TvEvent.createRecordingEvent(recordingTvChannel, timeInterface.getCurrentTime(recordingTvChannel),
            contentRating,recordingName,recordingShortDescription,recordingLongDescription,recordingStartTime,recordingEndTime)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return Recording(
        recordingId,
        recordingName,
        recordingDuration,
        0,
        recordingThumbnail,
        recordingUri,
        recordingTvChannel,
        recordedTvEvent,
        recordingStartTime,
        recordingEndTime,
        recordingShortDescription,
        recordingLongDescription,
        contentRating
    )
}

@SuppressLint("Range")
fun createScheduledRecordingsFromCursor(cursor: Cursor, tvModule: TvInterface, timeInterface: TimeInterface) : ScheduledRecording? {
    var id = -1
    var name = ""
    var tvEvent : TvEvent?= null
    var startTime : Long = 0
    var endTime : Long = 0
    var tvChannel : TvChannel?= null

    try {
        if (cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)) != null) {
            id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID))
        }

        if (cursor.getString(cursor.getColumnIndex(Contract.ScheduledRecordings.NAME_COLUMN)) != null){
            name = cursor.getString(cursor.getColumnIndex(Contract.ScheduledRecordings.NAME_COLUMN))
        }

        if (cursor.getLong(cursor.getColumnIndex(Contract.ScheduledRecordings.START_TIME_COLUMN)) != null){
            startTime = cursor.getLong(cursor.getColumnIndex(Contract.ScheduledRecordings.START_TIME_COLUMN))
        }

        if (cursor.getLong(cursor.getColumnIndex(Contract.ScheduledRecordings.END_TIME_COLUMN)) != null){
            endTime = cursor.getLong(cursor.getColumnIndex(Contract.ScheduledRecordings.END_TIME_COLUMN))
        }

        tvChannel = tvModule.getChannelById(
            cursor.getLong(
                cursor.getColumnIndex(Contract.ScheduledRecordings.CHANNEL_ID_COLUMN)
            ).toInt()
        )

        if (tvChannel == null)
            return null

        tvEvent = TvEvent.createNoInformationEvent(tvChannel, timeInterface.getCurrentTime(tvChannel))
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return ScheduledRecording(
        id,
        name,
        startTime,
        endTime,
        tvChannel!!.id ,
        tvEvent!!.id,
        RepeatFlag.NONE,
        tvChannel,
        tvEvent
    )
}

@SuppressLint("Range")
fun createScheduledRemindersFromCursor(cursor: Cursor, tvModule: TvInterface, timeInterface: TimeInterface) : ScheduledReminder? {
    var id = -1
    var name = ""
    var tvEvent : TvEvent?= null
    var tvChannel : TvChannel?= null

    try {
        if (cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)) != null) {
            id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID))
        }

        if (cursor.getString(cursor.getColumnIndex(Contract.ScheduledRecordings.NAME_COLUMN)) != null){
            name = cursor.getString(cursor.getColumnIndex(Contract.ScheduledRecordings.NAME_COLUMN))
        }
        tvChannel  = tvModule.getChannelById(
            cursor.getLong(
                cursor.getColumnIndex(Contract.ScheduledReminders.CHANNEL_ID_COLUMN)
            ).toInt()
        )

        if (tvChannel == null)
            return null

        tvEvent = TvEvent.createNoInformationEvent(tvChannel, timeInterface.getCurrentTime(tvChannel))
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return ScheduledReminder(
        id,
        name,
        tvChannel,
        tvEvent
    )
}

@SuppressLint("Range")
fun fromFavoriteCursor(cursor: Cursor, channelList: ArrayList<TvChannel>): FavoriteItem? {
    try {
        val onid =
            cursor.getInt(cursor.getColumnIndex(Contract.Favorites.ORIGINAL_NETWORK_ID_COLUMN))
        val tsid =
            cursor.getInt(cursor.getColumnIndex(Contract.Favorites.TRANSPORT_STREAM_ID_COLUMN))
        val sid =
            cursor.getInt(cursor.getColumnIndex(Contract.Favorites.SERVICE_ID_COLUMN))
        val type = cursor.getInt(cursor.getColumnIndex(Contract.Favorites.COLUMN_TYPE))
        val listIds =
            cursor.getString(cursor.getColumnIndex(Contract.Favorites.COLUMN_LIST_IDS))
        var favListIds = ArrayList<String>()
        val tempList = listIds.split(",")
        favListIds.addAll(tempList)
        channelList.forEach { tvChannel ->
            if (tvChannel.onId == onid && tvChannel.tsId == tsid && tvChannel.serviceId == sid) {
                tvChannel.favListIds.clear()
                tvChannel.favListIds.addAll(favListIds)
                return FavoriteItem(
                    tvChannel.id,
                    FavoriteItemType.TV_CHANNEL,
                    tvChannel.favListIds,
                    tvChannel,
                    tvChannel.favListIds
                )
            }
        }
        return null
    } catch (e: Exception) {
        e.printStackTrace()
    }
   return null
}

 fun toFavContentValues(referenceFavoriteItem: FavoriteItem): ContentValues {
    val contentValues = ContentValues()
    contentValues.put(
        Contract.Favorites.ORIGINAL_NETWORK_ID_COLUMN,
        referenceFavoriteItem.tvChannel.onId
    )
    contentValues.put(
        Contract.Favorites.TRANSPORT_STREAM_ID_COLUMN,
        referenceFavoriteItem.tvChannel.tsId
    )
    contentValues.put(
        Contract.Favorites.SERVICE_ID_COLUMN,
        referenceFavoriteItem.tvChannel.serviceId
    )
        contentValues.put(Contract.Favorites.COLUMN_TYPE, FavoriteItemType.TV_CHANNEL.getFilterId())
    var listIds = ""
    for (index in 0 until referenceFavoriteItem.favListIds.size) {
        val listId = referenceFavoriteItem.favListIds[index]
        listIds += listId
        if (index != (referenceFavoriteItem.favListIds.size - 1)) {
            listIds += ","
        }
    }
    contentValues.put(Contract.Favorites.COLUMN_LIST_IDS, listIds)
    return contentValues
}

 fun isNumeric(channelList: ArrayList<TvChannel>): Boolean {
    val regex = "-?[0-9]+(\\.[0-9]+)?".toRegex()
    channelList.toList().forEach { it ->
        var isNum = it.displayNumber.matches(regex)
        if(!isNum){
            return false
        }
    }
    return true
}

