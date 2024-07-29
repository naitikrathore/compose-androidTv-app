package com.iwedia.cltv.fti.data

import android.annotation.SuppressLint
import android.database.Cursor
import android.util.Log
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting
import com.iwedia.cltv.platform.model.Constants
import java.io.UnsupportedEncodingException
import java.util.*

class Channel {
    private val TAG = "Channel"

    val INVALID_ID: Long = -1
    val channelsContract: ChannelsContract = ChannelsContract()

    val PROJECTION = arrayOf<String>( // Columns must match what is read in Channel.fromCursor()
        channelsContract.ID,
        channelsContract.PACKAGE_NAME_COLUMN,
        channelsContract.INPUT_ID_COLUMN,
        channelsContract.TYPE_COLUMN,
        channelsContract.DISPLAY_NUMBER_COLUMN,
        channelsContract.NAME_COLUMN,
        channelsContract.SERVICE_TYPE_COLUMN,
        channelsContract.DESCRIPTION_COLUMN,
        channelsContract.BROWSABLE_COLUMN,
        channelsContract.LOCKED_COLUMN,
        channelsContract.SERVICE_ID_COLUMN,
        channelsContract.TRANSPORT_STREAM_ID_COLUMN,
        channelsContract.ORIGINAL_NETWORK_ID_COLUMN,
        channelsContract.SKIP_COLUMN,
        channelsContract.REFERENCE_NAME_COLUMN,
        channelsContract.DISPLAY_NUMBER_CHANGED_COLUMN,
        channelsContract.INTERNAL_PROVIDER_DATA_COLUMN
    )

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val c = o as Channel
        return if (mDisplayNumber != null && c.getDisplayNumber() != null) {
            mDisplayNumber == c.getDisplayNumber()
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(0, mDisplayNumber)
    }

    /**
     * Creates `Channel` object from cursor.
     *
     *
     * The query that created the cursor MUST use [.PROJECTION]
     *
     */
    @SuppressLint("Range")
    fun fromCursor(cursor: Cursor): Channel? {
        // Columns read must match the order of {@link #PROJECTION}
        val channel: Channel = Channel()
        val index = 0
        channel.mId = cursor.getLong(cursor.getColumnIndex("_id"))
        channel.mPackageName =
            cursor.getString(cursor.getColumnIndex(channelsContract.PACKAGE_NAME_COLUMN))
        channel.mInputId = cursor.getString(cursor.getColumnIndex(channelsContract.INPUT_ID_COLUMN))
        channel.mType = cursor.getString(cursor.getColumnIndex(channelsContract.TYPE_COLUMN))
        channel.mDisplayNumber =
            cursor.getString(cursor.getColumnIndex(channelsContract.DISPLAY_NUMBER_COLUMN))
        if (cursor.getInt(cursor.getColumnIndex(channelsContract.DISPLAY_NUMBER_CHANGED_COLUMN)) === 1) {
            channel.mDisplayName = cursor.getString(cursor.getColumnIndex(channelsContract.REFERENCE_NAME_COLUMN))
        } else {
            channel.mDisplayName = cursor.getString(cursor.getColumnIndex(channelsContract.NAME_COLUMN))
        }
        channel.mServiceType = cursor.getString(cursor.getColumnIndex(channelsContract.SERVICE_TYPE_COLUMN))
        channel.mDescription = cursor.getString(cursor.getColumnIndex(channelsContract.DESCRIPTION_COLUMN))
        channel.mBrowsable = cursor.getInt(cursor.getColumnIndex(channelsContract.BROWSABLE_COLUMN)) === 1
        channel.mLocked = cursor.getInt(cursor.getColumnIndex(channelsContract.LOCKED_COLUMN)) === 1

        channel.mServiceId = cursor.getLong(cursor.getColumnIndex(channelsContract.SERVICE_ID_COLUMN))
        channel.transportId = cursor.getLong(cursor.getColumnIndex(channelsContract.TRANSPORT_STREAM_ID_COLUMN))
        channel.networkId = cursor.getLong(cursor.getColumnIndex(channelsContract.ORIGINAL_NETWORK_ID_COLUMN))

        channel.mSkipChannel = cursor.getInt(cursor.getColumnIndex(channelsContract.SKIP_COLUMN)) === 1
        val blob: ByteArray = cursor.getBlob(cursor.getColumnIndex(channelsContract.INTERNAL_PROVIDER_DATA_COLUMN))
        try {
            channel.mInternalProviderData = String(blob, Charsets.UTF_8)
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return channel
    }

    /** ID of this channel. Matches to BaseColumns._ID.  */
    var mId: Long = 0
    var mPackageName: String? = null
    var mInputId: String? = null
    var mType: String? = null
    var mDisplayNumber: String? = null
    var mDisplayName: String? = null
    var mServiceType: String? = null
    var mDescription: String? = null
    var mBrowsable = false
    var mLocked = false
    var mChannelIndex = 0
    val mSiBrowsable = 0

    var transportId: Long = 0
    var networkId: Long = 0
    var mServiceId: Long = 0

    var mSkipChannel = false
    var mInternalProviderData: String? = null

    fun getId(): Long {
        return mId
    }

    fun getPackageName(): String? {
        return mPackageName
    }

    fun getInputId(): String? {
        return mInputId
    }

    fun getType(): String? {
        return mType
    }

    fun getDisplayNumber(): String? {
        return mDisplayNumber
    }

    fun setDisplayNumber(displayNumber: String?) {
        mDisplayNumber = displayNumber
    }

    @Nullable
    fun getDisplayName(): String? {
        return mDisplayName
    }

    fun setDisplayName(name: String?) {
        mDisplayName = name
    }

    @Nullable
    fun getServiceType(): String? {
        return mServiceType
    }

    @VisibleForTesting
    fun getDescription(): String? {
        return mDescription
    }

    fun getmServiceId(): Long {
        return mServiceId
    }


    fun isBrowsable(): Boolean {
        return mBrowsable
    }

    fun isLocked(): Boolean {
        return mLocked
    }

    fun getChannelIndex(): Int {
        return mChannelIndex
    }

    fun getInternalProviderData(): String? {
        return mInternalProviderData
    }

    fun setBrowsable(browsable: Boolean) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Set browsable $browsable")
        mBrowsable = browsable
    }

    fun setLocked(locked: Boolean) {
        mLocked = locked
    }

    fun setChannelIndex(index: Int) {
        mChannelIndex = index
    }

    fun setSkip(skip: Boolean) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Set skip $skip")
        mSkipChannel = skip
    }

    fun isSkipped(): Boolean {
        return mSkipChannel
    }

    override fun toString(): String {
        return ("Channel{"
                + "id=" + mId
                + ", packageName=" + mPackageName
                + ", inputId=" + mInputId
                + ", type=" + mType
                + ", displayNumber=" + mDisplayNumber
                + ", displayName=" + mDisplayName
                + ", mServiceType=" + mServiceType
                + ", description=" + mDescription
                + ", browsable=" + mBrowsable
                + ", si_browsable=" + mSiBrowsable
                + ", locked=" + mLocked
                + ", channelIndex=" + mChannelIndex
                + ", skip=" + mSkipChannel
                + ", mServiceId=" + mServiceId
                + ", transportId=" + transportId
                + ", networkId=" + networkId
                + ", mInternalProviderData=" + mInternalProviderData
                + "}")
    }
}