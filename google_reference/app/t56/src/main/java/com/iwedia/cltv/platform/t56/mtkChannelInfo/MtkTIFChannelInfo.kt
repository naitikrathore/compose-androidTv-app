package com.iwedia.cltv.platform.t56.mtkChannelInfo

import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.media.tv.TvContract
import android.util.Log
import com.iwedia.cltv.platform.model.Constants
import com.mediatek.twoworlds.tv.common.MtkTvChCommonBase
import com.mediatek.twoworlds.tv.model.MtkTvChannelInfoBase

class MtkTIFChannelInfo {

    val TAG = "TIFChannelInfo"
    var mId: Long = 0
    var mPackageName = "invalid package name"
    var mInputServiceName: String? = null
    var mType: String? = null
    var mServiceType: String? = null
    var mOriginalNetworkId = 0
    var mTransportStreamId = 0
    var mServiceId = 0
    var mDisplayNumber: String? = null
    var mDisplayName: String? = null
    var mNetworkAffiliation: String? = null
    var mDescription: String? = null
    var mVideoFormat: String? = null
    var mIsBrowsable = false
    var mSearchable = false
    var mLocked = false
    var mVersionNumber = 0
    var mAppLinkIconUri: String? = null
    var mAppLinkPosterArtUri: String? = null
    var mAppLinkText: String? = null
    var mAppLinkColor = 0
    var mAppLinkIntentUri: String? = null
    var mData: String? = null
    var mInternalProviderFlag1 = 0
    var mInternalProviderFlag2 = 0
    var mInternalProviderFlag3 = 0
    var mInternalProviderFlag4 = 0
    private val mAppLinkIntent: Intent? = null
    private val mAppLinkType = 0

    /**
     * int mSvlId = Integer.parseInt(value[1]); int mSvlRecId = Integer.parseInt(value[2]); unsigned
     * int mChannelId = Integer.parseInt(value[3]); int mHashcode = Integer.parseInt(value[4]); int
     * mKey = (mSvlId<<16)+mSvlRecId;
     */
    var mDataValue: LongArray? = null
    var mMtkTvChannelInfo: MtkTvChannelInfoBase? = null

    companion object{
        var TYPE_ATV = 10000
        val ANALOG_CHANNEL_NUMBER_START = 1

    }

    /*
    * parser public property
    */
    fun parse(c: Cursor?): MtkTIFChannelInfo {
        val info = MtkTIFChannelInfo()
        parse(info, c)
        return info
    }

    /*
     * parser public property
     */
    @SuppressLint("Range")
    fun parse(temTIFChannel: MtkTIFChannelInfo?, c: Cursor?) {
        if (temTIFChannel == null || c == null) {
            return
        }
        temTIFChannel.mId = c.getLong(
            c.getColumnIndex(TvContract.Channels._ID)
        )
        temTIFChannel.mPackageName = c.getString(
            c.getColumnIndex(TvContract.Channels.COLUMN_PACKAGE_NAME)
        )
        temTIFChannel.mInputServiceName = c.getString(
            c.getColumnIndex(TvContract.Channels.COLUMN_INPUT_ID)
        )
        temTIFChannel.mType = c.getString(
            c.getColumnIndex(TvContract.Channels.COLUMN_TYPE)
        )
        temTIFChannel.mServiceType = c.getString(
            c.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_TYPE)
        )
        temTIFChannel.mOriginalNetworkId = c.getInt(
            c.getColumnIndex(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID)
        )
        temTIFChannel.mTransportStreamId = c.getInt(
            c.getColumnIndex(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID)
        )
        temTIFChannel.mServiceId = c.getInt(
            c.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_ID)
        )
        //TODO check if current input type is TYPE_ATV
        if (isAtv()) {
            temTIFChannel.mDisplayNumber =
                "" + getAnalogChannelDisplayNumInt(c.getString(c.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER)))
        } else {
            temTIFChannel.mDisplayNumber = c.getString(
                c
                    .getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER)
            )
        }
        temTIFChannel.mDisplayName = c.getString(
            c.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME)
        )
        if (temTIFChannel.mDisplayName == null) {
            temTIFChannel.mDisplayName = ""
        } else {
            temTIFChannel.mDisplayName = getAvailableString(temTIFChannel.mDisplayName)
        }
        temTIFChannel.mNetworkAffiliation = c.getString(
            c.getColumnIndex(TvContract.Channels.COLUMN_NETWORK_AFFILIATION)
        )
        temTIFChannel.mDescription = c.getString(
            c.getColumnIndex(TvContract.Channels.COLUMN_DESCRIPTION)
        )
        temTIFChannel.mVideoFormat = c.getString(
            c.getColumnIndex(TvContract.Channels.COLUMN_VIDEO_FORMAT)
        )
        var isBrowsable = c.getInt(
            c.getColumnIndex(TvContract.Channels.COLUMN_BROWSABLE)
        )
        temTIFChannel.mIsBrowsable = isBrowsable == 1
        isBrowsable = c.getInt(c.getColumnIndex(TvContract.Channels.COLUMN_SEARCHABLE))
        temTIFChannel.mSearchable = isBrowsable == 1
        isBrowsable = c.getInt(c.getColumnIndex(TvContract.Channels.COLUMN_LOCKED))
        temTIFChannel.mLocked = isBrowsable == 1
        temTIFChannel.mVersionNumber = c.getInt(
            c.getColumnIndex(TvContract.Channels.COLUMN_VERSION_NUMBER)
        )
        if (c.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_ICON_URI) > -1) {
            temTIFChannel.mAppLinkIconUri = c.getString(
                c.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_ICON_URI)
            )
            temTIFChannel.mAppLinkPosterArtUri = c.getString(
                c.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_POSTER_ART_URI)
            )
            temTIFChannel.mAppLinkText = c.getString(
                c.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_TEXT)
            )
            temTIFChannel.mAppLinkColor = c.getInt(
                c.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_COLOR)
            )
            temTIFChannel.mAppLinkIntentUri = c.getString(
                c.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_INTENT_URI)
            )
            temTIFChannel.mInternalProviderFlag1 = c.getInt(
                c.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1)
            )
            temTIFChannel.mInternalProviderFlag2 = c.getInt(
                c.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2)
            )
            temTIFChannel.mInternalProviderFlag3 = c.getInt(
                c.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG3)
            )
            temTIFChannel.mInternalProviderFlag4 = c.getInt(
                c.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG4)
            )
        }
        try {
            val mData =
                c.getBlob(c.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA))
            if (mData != null && mData.size != 0) {
                temTIFChannel.mData = String(mData)
                parserTIFChannelData(temTIFChannel, temTIFChannel.mData)
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
        // printProviderInfo(temTIFChannel);
    }

    private fun isAtv(): Boolean {
        return false
    }

    private fun parserTIFChannelData(temTIFChannel: MtkTIFChannelInfo, data: String?) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "data:$data")
        if (data == null) {
            return
        }
        val value = data.split(",".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        if (!(value.size == MtkTvChCommonBase.TV_DB_BLOB_LEN || value.size == 6)) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "parserTIFChannelData data.length != 6 or 10")
            return
        }
        val length = value.size
        val v = LongArray(length)
        val mSvlId = value[1].toLong()
        val mSvlRecId = value[2].toLong()
        val channelId = value[3].toLong()
        v[0] = mSvlId
        v[1] = mSvlRecId
        v[2] = channelId
        // v[3] = mHashcode;
        v[4] = (mSvlId shl 16) + mSvlRecId
        for (i in 5 until value.size) {
            v[i] = value[i].toInt().toLong()
        }
        temTIFChannel.mDataValue = v
    }

    private fun getAvailableString(illegalString: String?): String {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "come in getAvailableString, start illegalString ==$illegalString")
        var resultString = ""
        if (null != illegalString && "" != illegalString) {
            val illegalByte = illegalString.toByteArray()
            var j = 0
            val availableByte = ByteArray(illegalByte.size)
            for (mByte in illegalByte) {
                if (mByte.toInt() and 0xff >= 32 && mByte.toInt() and 0xff != 127 || mByte.toInt() and 0xff == 10 || mByte.toInt() and 0xff == 13) {
                    availableByte[j] = mByte
                    j++
                }
            }
            if (availableByte[availableByte.size - 1].toInt() and 0xff == 10 || availableByte[availableByte.size - 1].toInt() and 0xff == 13) {
                j--
            }
            if (null != availableByte) {
                resultString = String(availableByte, 0, j)
            }
        }
        if (resultString.length > 61) {
            resultString = resultString.substring(0, 61) + "..."
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "come in getAvailableString,end resultString ==$resultString")
        return resultString
    }


    private fun getAnalogChannelDisplayNumInt(orignalNum: String): Int {
        var displayNum = -1
        displayNum = try {
            orignalNum.toInt()
        } catch (e: java.lang.Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAnalogChannelDisplayNumInt invalid channel number:$orignalNum")
            return displayNum
        }
        if (displayNum < ANALOG_CHANNEL_NUMBER_START) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAnalogChannelDisplayNumInt invalid original channel number:$displayNum")
            return displayNum
        }
        displayNum =
            displayNum - ANALOG_CHANNEL_NUMBER_START + 1
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAnalogChannelDisplayNumInt channel number:$displayNum")
        return displayNum
    }

    override fun toString(): String {
        return "MtkTIFChannelInfo(TAG='$TAG', mId=$mId, mPackageName='$mPackageName', mInputServiceName=$mInputServiceName, mType=$mType, mServiceType=$mServiceType, mOriginalNetworkId=$mOriginalNetworkId, mTransportStreamId=$mTransportStreamId, mServiceId=$mServiceId, mDisplayNumber=$mDisplayNumber, mDisplayName=$mDisplayName, mNetworkAffiliation=$mNetworkAffiliation, mDescription=$mDescription, mVideoFormat=$mVideoFormat, mIsBrowsable=$mIsBrowsable, mSearchable=$mSearchable, mLocked=$mLocked, mVersionNumber=$mVersionNumber, mAppLinkIconUri=$mAppLinkIconUri, mAppLinkPosterArtUri=$mAppLinkPosterArtUri, mAppLinkText=$mAppLinkText, mAppLinkColor=$mAppLinkColor, mAppLinkIntentUri=$mAppLinkIntentUri, mData=$mData, mInternalProviderFlag1=$mInternalProviderFlag1, mInternalProviderFlag2=$mInternalProviderFlag2, mInternalProviderFlag3=$mInternalProviderFlag3, mInternalProviderFlag4=$mInternalProviderFlag4, mAppLinkIntent=$mAppLinkIntent, mAppLinkType=$mAppLinkType, mDataValue=${mDataValue.contentToString()}, mMtkTvChannelInfo=$mMtkTvChannelInfo)"
    }


}