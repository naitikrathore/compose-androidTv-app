package com.iwedia.cltv.platform.gretzky.provider

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.media.tv.TvContract
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.text.isDigitsOnly
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.channel.VideoResolution
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.model.content_provider.Contract

val DUMMY_CHANNEL_ID = -555
val TERRESTRIAL_TUNER_TYPE = 100
val CABLE_TUNER_TYPE = 200
val SATELLITE_TUNER_TYPE = 300
val VIDEO_RESOLUTION_ED = 400
val VIDEO_RESOLUTION_FHD = 500
val VIDEO_RESOLUTION_SD = 600
val VIDEO_RESOLUTION_HD = 700
val VIDEO_RESOLUTION_UHD = 800

fun ContentResolver.insertChannel(tvChannel: TvChannel): Uri? {
    val contentValues = ContentValues()
    contentValues.put(Contract.Channels.PACKAGE_NAME_COLUMN, tvChannel.packageName)
    contentValues.put(Contract.Channels.INPUT_ID_COLUMN, tvChannel.inputId)
    contentValues.put(Contract.Channels.TYPE_COLUMN, tvChannel.tunerType.toString())
    contentValues.put(Contract.Channels.SERVICE_TYPE_COLUMN, tvChannel.serviceType)
    contentValues.put(Contract.Channels.ORIGINAL_NETWORK_ID_COLUMN, "")
    contentValues.put(Contract.Channels.TRANSPORT_STREAM_ID_COLUMN, tvChannel.tsId)
    contentValues.put(Contract.Channels.SERVICE_ID_COLUMN, tvChannel.serviceId)
    contentValues.put(Contract.Channels.DISPLAY_NUMBER_COLUMN, tvChannel.displayNumber)
    contentValues.put(Contract.Channels.NAME_COLUMN, tvChannel.name)
    contentValues.put(Contract.Channels.NETWORK_AFFILIATION_COLUMN, "")
    contentValues.put(Contract.Channels.DESCRIPTION_COLUMN, "")
    contentValues.put(Contract.Channels.VIDEO_FORMAT_COLUMN, "")
    contentValues.put(Contract.Channels.BROWSABLE_COLUMN, tvChannel.isBrowsable)
    contentValues.put(Contract.Channels.SEARCHABLE_COLUMN, 1)
    contentValues.put(Contract.Channels.LOCKED_COLUMN, tvChannel.isLocked)
    contentValues.put(Contract.Channels.APP_LINK_ICON_URI_COLUMN, tvChannel.appLinkIconUri)
    contentValues.put(Contract.Channels.APP_LINK_POSTER_ART_URI_COLUMN, tvChannel.appLinkPosterUri)
    contentValues.put(Contract.Channels.APP_LINK_TEXT_COLUMN, tvChannel.appLinkText)
    contentValues.put(Contract.Channels.APP_LINK_COLOR_COLUMN, "")
    contentValues.put(Contract.Channels.APP_LINK_INTENT_URI_COLUMN, tvChannel.appLinkIntentUri)
    contentValues.put(Contract.Channels.INTERNAL_PROVIDER_ID_COLUMN, "")
    contentValues.put(Contract.Channels.INTERNAL_PROVIDER_DATA_COLUMN, "")
    contentValues.put(Contract.Channels.INTERNAL_PROVIDER_FLAG1_COLUMN, "")
    contentValues.put(Contract.Channels.VERSION_NUMBER_COLUMN, "")
    contentValues.put(Contract.Channels.TRANSIENT_COLUMN, "")
    contentValues.put(Contract.Channels.SKIP_COLUMN, tvChannel.isSkipped)
    contentValues.put(Contract.Channels.ORIG_ID_COLUMN, tvChannel.onId)
    contentValues.put(Contract.Channels.REFERENCE_NAME_COLUMN, "")
    contentValues.put(Contract.Channels.DISPLAY_NUMBER_COLUMN, 0)
    contentValues.put(Contract.Channels.ORDINAL_NUMBER_COLUMN, tvChannel.ordinalNumber)
    contentValues.put(Contract.Channels.DELETED_COLUMN, 0)

    return insert(ContentProvider.CHANNELS_URI, contentValues)
}

@SuppressLint("Range")
fun createChannelFromCursor(cursor: Cursor): TvChannel {
    var id = -1
    var inputId = ""
    var displayNumber = ""
    var displayName = ""
    var logoImagePath = ""
    var isRadioChannel = false
    var isSkipped = false
    var isLocked = false
    var tunerType = -1
    var ordinalNumber = 0
    var tsId = 0
    var onId = 0
    var serviceId = 0
    var internalId = -1L
    var isBrowsable = true
    var appLinkText = ""
    var appLinkIntentUri = ""
    var appLinkIconUri = ""
    var appLinkPosterUri = ""
    var packageName = ""

    var referenceVideoQuality = ArrayList<VideoResolution>()
    if (cursor.getInt(cursor.getColumnIndex(Contract.Channels.ORIG_ID_COLUMN)) != null) {
        id = cursor.getInt(cursor.getColumnIndex(Contract.Channels.ORIG_ID_COLUMN))
    }
    if (cursor.getString(cursor.getColumnIndex(Contract.Channels.INPUT_ID_COLUMN)) != null) {
        inputId =
            cursor.getString(cursor.getColumnIndex(Contract.Channels.INPUT_ID_COLUMN))
    }
    if (cursor.getString(cursor.getColumnIndex(Contract.Channels.DISPLAY_NUMBER_COLUMN)) != null) {
        displayNumber =
            cursor.getString(cursor.getColumnIndex(Contract.Channels.DISPLAY_NUMBER_COLUMN))
    }
    if (cursor.getString(cursor.getColumnIndex(Contract.Channels.NAME_COLUMN)) != null) {
        displayName =
            (cursor.getString(cursor.getColumnIndex(Contract.Channels.NAME_COLUMN))).trim()
    }
    //Check is channel renamed
    if (cursor.getString(cursor.getColumnIndex(Contract.Channels.REFERENCE_NAME_COLUMN)) != null) {
        var referenceName =
            (cursor.getString(cursor.getColumnIndex(Contract.Channels.REFERENCE_NAME_COLUMN))).trim()
        if (referenceName != null && referenceName.isNotEmpty()) {
            displayName = referenceName
        }
    }
    if (cursor.getString(cursor.getColumnIndex(Contract.Channels.SERVICE_ID_COLUMN)) != null) {
        serviceId =
            cursor.getInt(cursor.getColumnIndex(Contract.Channels.SERVICE_ID_COLUMN))
    }
    if (cursor.getString(cursor.getColumnIndex(Contract.Channels.TRANSPORT_STREAM_ID_COLUMN)) != null) {
        tsId =
            cursor.getInt(cursor.getColumnIndex(Contract.Channels.TRANSPORT_STREAM_ID_COLUMN))
    }
    if (cursor.getString(cursor.getColumnIndex(Contract.Channels.ORIGINAL_NETWORK_ID_COLUMN)) != null) {
        onId =
            cursor.getInt(cursor.getColumnIndex(Contract.Channels.ORIGINAL_NETWORK_ID_COLUMN))
    }
    val channelLogoUri: Uri = TvContract.buildChannelLogoUri(id.toLong())
    if (channelLogoUri != null) {
        logoImagePath = channelLogoUri.toString()
    }
    if (cursor.getString(cursor.getColumnIndex(Contract.Channels.SERVICE_TYPE_COLUMN)) != null) {
        isRadioChannel =
            cursor.getString(cursor.getColumnIndex(Contract.Channels.SERVICE_TYPE_COLUMN)) == TvContract.Channels.SERVICE_TYPE_AUDIO
    }
    if (cursor.getString(cursor.getColumnIndex(Contract.Channels.VIDEO_FORMAT_COLUMN)) != null) {
        var videoFormat =
            cursor.getString(cursor.getColumnIndex(Contract.Channels.VIDEO_FORMAT_COLUMN))
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
    if (cursor.getString(cursor.getColumnIndex(Contract.Channels.TYPE_COLUMN)) != null) {
        // TODO Add support for other standard not only for DVB
        val type =
            cursor.getString(cursor.getColumnIndex(Contract.Channels.TYPE_COLUMN))
        if (type == TvContract.Channels.TYPE_DVB_T || type == TvContract.Channels.TYPE_DVB_T ||
            type == TvContract.Channels.TYPE_ATSC_T) {
            tunerType = TERRESTRIAL_TUNER_TYPE
        }
        if (type == TvContract.Channels.TYPE_DVB_C || type == TvContract.Channels.TYPE_DVB_C2 ||
            type == TvContract.Channels.TYPE_ATSC_C) {
            tunerType = CABLE_TUNER_TYPE
        }
        if (type == TvContract.Channels.TYPE_DVB_S || type == TvContract.Channels.TYPE_DVB_S2) {
            tunerType = SATELLITE_TUNER_TYPE
        }
    }
    if (cursor.getInt(cursor.getColumnIndex(Contract.Channels.SKIP_COLUMN)) != null) {
        isSkipped =
            cursor.getInt(cursor.getColumnIndex(Contract.Channels.SKIP_COLUMN)) == 1
    }
    if (cursor.getInt(cursor.getColumnIndex(Contract.Channels.BROWSABLE_COLUMN)) != null) {
        isBrowsable =
            cursor.getInt(cursor.getColumnIndex(Contract.Channels.BROWSABLE_COLUMN)) == 1
    }
    if (cursor.getInt(cursor.getColumnIndex(Contract.Channels.LOCKED_COLUMN)) != null) {
        isLocked =
            cursor.getInt(cursor.getColumnIndex(Contract.Channels.LOCKED_COLUMN)) == 1
    }
    if (cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)) != null) {
        internalId =
            cursor.getLong(cursor.getColumnIndex(BaseColumns._ID))
    }
    if (cursor.getInt(cursor.getColumnIndex(Contract.Channels.ORDINAL_NUMBER_COLUMN)) != null) {
        ordinalNumber =
            cursor.getInt(cursor.getColumnIndex(Contract.Channels.ORDINAL_NUMBER_COLUMN))
    }
    if (cursor.getString(cursor.getColumnIndex(Contract.Channels.APP_LINK_ICON_URI_COLUMN)) != null) {
        appLinkIconUri =
            cursor.getString(cursor.getColumnIndex(Contract.Channels.APP_LINK_ICON_URI_COLUMN))
    }
    if (cursor.getString(cursor.getColumnIndex(Contract.Channels.APP_LINK_POSTER_ART_URI_COLUMN)) != null) {
        appLinkPosterUri =
            cursor.getString(cursor.getColumnIndex(Contract.Channels.APP_LINK_POSTER_ART_URI_COLUMN))
    }
    if (cursor.getString(cursor.getColumnIndex(Contract.Channels.APP_LINK_TEXT_COLUMN)) != null) {
        appLinkText =
            cursor.getString(cursor.getColumnIndex(Contract.Channels.APP_LINK_TEXT_COLUMN))
    }
    if (cursor.getString(cursor.getColumnIndex(Contract.Channels.APP_LINK_INTENT_URI_COLUMN)) != null) {
        appLinkIntentUri =
            cursor.getString(cursor.getColumnIndex(Contract.Channels.APP_LINK_INTENT_URI_COLUMN))
    }
    if (cursor.getString(cursor.getColumnIndex(Contract.Channels.PACKAGE_NAME_COLUMN)) != null) {
        packageName =
            cursor.getString(cursor.getColumnIndex(Contract.Channels.PACKAGE_NAME_COLUMN))
    }
    var index = if (displayNumber.isDigitsOnly()) displayNumber.toInt() else ordinalNumber
    var tvChannel =
        TvChannel(
            id,
            index,
            displayName,
            logoImagePath,
            videoQuality = referenceVideoQuality
        )
    tvChannel.internalId = internalId
    tvChannel.inputId = inputId;
    tvChannel.displayNumber = displayNumber
    tvChannel.lcn = if (displayNumber.isDigitsOnly()) displayNumber.toInt() else ordinalNumber
    tvChannel.isRadioChannel = isRadioChannel
    tvChannel.channelId = id.toLong()
    tvChannel.tunerType = TunerType.getTunerType(tunerType.toString())
    tvChannel.isSkipped = isSkipped
    tvChannel.isLocked = isLocked
    tvChannel.ordinalNumber = ordinalNumber
    tvChannel.serviceId = serviceId
    tvChannel.tsId = tsId
    tvChannel.onId = onId
    tvChannel.isBrowsable = isBrowsable
    tvChannel.appLinkIconUri = appLinkIconUri
    tvChannel.appLinkPosterUri = appLinkPosterUri
    tvChannel.appLinkText = appLinkText
    tvChannel.appLinkIntentUri = appLinkIntentUri
    tvChannel.packageName = packageName
    return tvChannel
}

fun compare(channel1: TvChannel, channel2: TvChannel): Boolean {
    return channel1.onId == channel2.onId &&
            channel1.tsId == channel2.tsId &&
            channel1.serviceId == channel2.serviceId
}