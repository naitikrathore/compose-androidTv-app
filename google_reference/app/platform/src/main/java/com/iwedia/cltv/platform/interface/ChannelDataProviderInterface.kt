package com.iwedia.cltv.platform.`interface`

import android.database.Cursor
import com.iwedia.cltv.platform.model.TvChannel

/**
 * Channel data provider interface
 *
 * @author Dejan Nadj
 */
interface ChannelDataProviderInterface {

    /**
     * @return channel list
     */
    fun getChannelList(): ArrayList<TvChannel>

    fun getSortedChannelList(oldChannelList: MutableList<TvChannel>): ArrayList<TvChannel>

    /**
     * Delete tv channel
     *
     * @return true if channel is deleted
     */
    fun deleteChannel(tvChannel: TvChannel): Boolean

    /**
     * Lock or unlock channel
     * @param lock true to lock channel false to unlock it
     *
     * @return true if the channel is locked/unlocked
     */
    fun lockUnlockChannel(tvChannel: TvChannel, lock: Boolean): Boolean

    /**
     * @return true if channel is available for channel block menu
     */
    fun isChannelLockAvailable(tvChannel: TvChannel): Boolean

    /**
     * Skip or unskip channel
     * @param skip true to skip channel false to unskip it
     *
     * @return true if the channel is skipped/unskipped
     */
    fun skipUnskipChannel(tvChannel: TvChannel, skip: Boolean): Boolean

    /**
     * dispose provider
     */
    fun dispose()

    /**
     * Enable or disable lcn
     * @param enableLcn true to enable lcn false to disable it
     */
    fun enableLcn(enableLcn: Boolean)

    /**
     * @return true if the lcn is enabled false if it is not
     */
    fun isLcnEnabled(): Boolean

    fun getPlatformData(cursor : Cursor) : Any?
}