package com.iwedia.cltv.tis.helper

import android.annotation.SuppressLint
import android.content.Context
import android.media.tv.TvContract
import com.iwedia.cltv.tis.model.ChannelDescriptor
import com.iwedia.cltv.tis.ui.SetupActivity
import java.util.*
import kotlin.collections.HashMap

/**
 * Helper class to add data into Channels Descriptor and expose channels.
 *
 * @author Abhilash M R
 */
object ChannelListHelper {

    var channels = Collections.synchronizedList(arrayListOf<ChannelDescriptor>())
    var idMap = Collections.synchronizedMap(HashMap<Int, Int>())
    var scanningInProgress = false

    fun findChannelById(id: Long): ChannelDescriptor? {
        var channelDescriptor: ChannelDescriptor? = null
        run exitForEach@{
            channels.toList().forEach {
                if (it.mChId == id) {
                    channelDescriptor = it
                    return@exitForEach
                }
            }
        }
        return channelDescriptor
    }

    @SuppressLint("Range")
    fun initData(context: Context) {
        if (scanningInProgress) {
            return
        }
        //Init ChannelListHelper list after reboot
        var cursor = context!!.contentResolver.query(
            TvContract.buildChannelsUriForInput(SetupActivity.INPUT_ID),
            null,
            null,
            null,
            null
        )
        if (cursor!!.count > 0) {
            cursor.moveToFirst()
            do {
                var channelId = cursor.getInt(cursor.getColumnIndex(TvContract.Channels._ID))
                var displayName =
                    (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME))).trim()
                val ordinalNumber = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG4))
                var logo =
                    cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_ICON_URI))
                var displayNumber = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER))
                var playbackUrl = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA))
                val licenseServerUrl = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_ID))
                // Necessary to maintain channel details inside app.
                var chDesc = ChannelDescriptor(
                    mInput = SetupActivity.INPUT_ID,
                    mChLogo = logo,
                    mChName = displayName,
                    mChPlaybackUrl = playbackUrl,
                    mOrdinalNumber = ordinalNumber,
                    mLicenseServerUrl = if(!licenseServerUrl.isNullOrEmpty()) licenseServerUrl else ""
                )
                //this is needed for referencing from TIS, check onTune in TvInputService to understand.
                chDesc.mChId = channelId.toLong()
                chDesc.mChannelUri = TvContract.buildChannelUri(channelId.toLong())
                chDesc.mExternalId = displayNumber
                idMap[displayNumber.toInt()] = channelId
                channels.add(chDesc)
            } while (cursor.moveToNext())
        }
        cursor!!.close()
    }
    fun getChannelList(): List<ChannelDescriptor> {
        return channels.toList()
    }
}