package com.iwedia.cltv.tis.model

import android.content.ContentValues
import android.media.tv.TvContract
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Data class to handle Channels data.
 *
 * @author Abhilash M R
 */
class ChannelDescriptor (
    var mInput: String = "",
    var mChName: String = "",
    var mChPlaybackUrl: String = "",
    var mChLogo: String = "",
    var mChRating: String = "",
    var mOrdinalNumber: Int = -1,
    var mLicenseServerUrl: String = ""
) {
    var mChId: Long = 0
    var mChannelUri: Uri = Uri.EMPTY
    var mExternalId = ""
    @RequiresApi(Build.VERSION_CODES.S)
    fun getContentValues(inputId: String?): ContentValues? {
        val ret = ContentValues()
        ret.put(TvContract.Channels.COLUMN_DESCRIPTION, mChRating)
        ret.put(TvContract.Channels.COLUMN_APP_LINK_ICON_URI, mChLogo)
        ret.put(TvContract.Channels.COLUMN_GLOBAL_CONTENT_ID, mChPlaybackUrl)
        ret.put(TvContract.Channels.COLUMN_DISPLAY_NAME, mChName)
        ret.put(TvContract.Channels.COLUMN_INPUT_ID, mInput)
        ret.put(TvContract.Channels._ID, mChId)
        return ret
    }

    companion object {
        @RequiresApi(Build.VERSION_CODES.S)
        fun getProjection(): Array<String?>? {
            return arrayOf(
                TvContract.Channels.COLUMN_INPUT_ID,
                TvContract.Channels._ID,
                TvContract.Channels.COLUMN_DISPLAY_NAME,
                TvContract.Channels.COLUMN_GLOBAL_CONTENT_ID,
                TvContract.Channels.COLUMN_APP_LINK_ICON_URI,
                TvContract.Channels.COLUMN_DESCRIPTION,
            )
        }
    }
}