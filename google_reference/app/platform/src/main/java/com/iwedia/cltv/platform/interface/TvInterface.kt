/**
 * This interface defines the contract for interacting with the TV service.
 */
package com.iwedia.cltv.platform.`interface`

import android.content.Context
import android.media.tv.TvInputInfo
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.channel.FilterItemType
import com.iwedia.cltv.platform.model.player.PlayableItem

//import listeners.AsyncDataReceiver
//import listeners.AsyncReceiver
//import com.iwedia.cltv.sdk.entities.ReferenceTvChannel

// AsyncDataReceiver -> IAsyncDataCallback
// AsyncReceiver -> IAsyncCallback
// ReferenceTvChannel -> TvChannel

/**
 * Interface for TV-related operations.
 */

interface TvInterface {
    /**
     * The currently active category filter for TV channels.
     */
    var activeCategoryId: FilterItemType

    /**
     * Interface for receiving playback status updates.
     */
    var playbackStatusInterface: PlaybackStatusInterface

    /**
     * Initialize the TV interface.
     */
    fun setup()

    /**
     * Set up the default TV service with the provided list of channels.
     *
     * @param channels The list of TV channels to set up.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun setupDefaultService(channels : List<TvChannel>, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Dispose of resources and clean up the TV interface.
     */
    fun dispose()

    /**
     * Change the TV channel to the specified channel.
     *
     * @param channel The TV channel to change to.
     * @param callback Callback to receive asynchronous result notifications.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun changeChannel(channel: TvChannel, callback: IAsyncCallback , applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Get a list of selected TV channels.
     *
     * @param callback Callback to receive asynchronous data result notifications.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     *
     * in case we want to get channellist for some selected filter we can use this
     * we passed this params- [filter] and [filtermetadata] in case of epg zap only(advised)
     * @param filter denotes which is the active filter
     * @param filterMetadata denotes extra info like if it is favorite then category name can be fav1, fav2 etc
     */
    fun getSelectedChannelList(callback: IAsyncDataCallback<ArrayList<TvChannel>> , applicationMode: ApplicationMode = ApplicationMode.DEFAULT, filter : FilterItemType? =null,filterMetadata: String? = null)

    /**
     * Change to the next TV channel.
     *
     * @param callback Callback to receive asynchronous result notifications.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun nextChannel(callback: IAsyncCallback , applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Change to the previous TV channel.
     *
     * @param callback Callback to receive asynchronous result notifications.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun previousChannel(callback: IAsyncCallback, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Change to the last TV channel.
     *
     * @param callback Callback to receive asynchronous result notifications.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun getLastActiveChannel(callback: IAsyncCallback, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Retrieves a TV channel by its unique identifier.
     *
     * @param channelId The ID of the channel to retrieve.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return The TV channel with the specified ID, or null if not found.
     */
    fun getChannelById(channelId: Int, applicationMode: ApplicationMode = ApplicationMode.DEFAULT): TvChannel?

    /**
     * Finds the position of a TV channel in the selected channel list.
     *
     * @param tvChannel The TV channel to find the position of.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return The position of the TV channel in the selected channel list, or -1 if not found.
     */
    fun findChannelPosition(tvChannel: TvChannel, applicationMode: ApplicationMode = ApplicationMode.DEFAULT) : Int

    /**
     * Change the TV channel to the channel at the specified index.
     *
     * @param index The index of the channel to change to.
     * @param callback Callback to receive asynchronous result notifications.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun changeChannel(index: Int, callback: IAsyncCallback, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Play the next TV channel in the selected channel list.
     *
     * @param selectedChannelList The list of selected TV channels.
     * @param callback Callback to receive asynchronous result notifications.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun playNextIndex(selectedChannelList: ArrayList<TvChannel>, callback: IAsyncCallback, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Play the previous TV channel in the selected channel list.
     *
     * @param selectedChannelList The list of selected TV channels.
     * @param callback Callback to receive asynchronous result notifications.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun playPrevIndex(selectedChannelList: ArrayList<TvChannel>, callback: IAsyncCallback, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Retrieves a TV channel by its display number.
     *
     * @param displayNumber The display number of the channel to retrieve.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return The TV channel with the specified display number, or null if not found.
     */
    fun getChannelByDisplayNumber(displayNumber: String, applicationMode: ApplicationMode = ApplicationMode.DEFAULT) : TvChannel?

    /**
     * Enable or disable the Logical Channel Number (LCN) feature.
     *
     * @param enableLcn True to enable LCN, false to disable it.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun enableLcn(enableLcn: Boolean, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Update the desired channel index.
     *
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun updateDesiredChannelIndex(applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Update the launch origin information.
     *
     * @param categoryId The category ID.
     * @param favGroupName The favorite group name (default is an empty string).
     * @param tifCategoryName The TIF category name.
     * @param genreCategoryName The genre category name.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun updateLaunchOrigin(categoryId: Int, favGroupName: String = "", tifCategoryName: String, genreCategoryName: String, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    // The following functions are found in TvHandler, but not in ReferenceTvHandler
    /**
     * Get the currently active TV channel.
     *
     * @param callback Callback to receive asynchronous data result notifications.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Store the currently active TV channel.
     *
     * @param tvChannel The TV channel to store as the active channel.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun storeActiveChannel(tvChannel: TvChannel, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Store the last active TV channel.
     *
     * @param tvChannel The TV channel to store as the last active channel.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun storeLastActiveChannel(channel: TvChannel, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)


    /**
     * Get a TV channel by its index.
     *
     * @param index The index of the channel to retrieve.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return The TV channel at the specified index.
     */
    fun getChannelByIndex(index: Int, applicationMode: ApplicationMode = ApplicationMode.DEFAULT): TvChannel

    /**
     * Get a list of TV channels.
     *
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return The list of TV channels.
     */
    fun getChannelList(applicationMode: ApplicationMode = ApplicationMode.DEFAULT): ArrayList<TvChannel>

    /**
     * Get a list of browsable TV channels.
     *
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return The list of browsable TV channels.
     */
    fun getBrowsableChannelList(applicationMode: ApplicationMode = ApplicationMode.DEFAULT): ArrayList<TvChannel>

    /**
     * Get a list of TV channels asynchronously.
     *
     * @param callback Callback to receive asynchronous data result notifications.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun getChannelListAsync(callback: IAsyncDataCallback<ArrayList<TvChannel>>, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Change to the next TV channel within the specified category.
     *
     * @param categoryId The category ID.
     * @param callback Callback to receive asynchronous result notifications.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun nextChannelByCategory(categoryId: Int, callback: IAsyncCallback, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Change to the previous TV channel within the specified category.
     *
     * @param categoryId The category ID.
     * @param callback Callback to receive asynchronous result notifications.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun previousChannelByCategory(categoryId: Int, callback: IAsyncCallback, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Set the recently watched TV channel by its index.
     *
     * @param channelIndex The index of the recently watched channel.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun setRecentChannel(channelIndex: Int, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Start the initial playback of a TV channel.
     *
     * @param callback Callback to receive asynchronous result notifications.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun startInitialPlayback(callback: IAsyncCallback, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Add a recently watched item to the list of recently watched content.
     *
     * @param playableItem The playable item to add to the list.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun addRecentlyWatched(playableItem: PlayableItem, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)
    /**
     * Get the list of recently watched items.
     *
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return The list of recently watched items, or null if not available.
     */
    fun getRecentlyWatched(applicationMode: ApplicationMode = ApplicationMode.DEFAULT): MutableList<PlayableItem>?

    /**
     * Delete a TV channel.
     *
     * @param tvChannel The TV channel to be deleted.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return `true` if the channel was successfully deleted, `false` otherwise.
     */
    fun deleteChannel(tvChannel: TvChannel, applicationMode: ApplicationMode = ApplicationMode.DEFAULT): Boolean

    /**
     * Lock or unlock a TV channel.
     *
     * @param tvChannel The TV channel to lock or unlock.
     * @param lockUnlock `true` to lock the channel, `false` to unlock it.
     * @param callback Callback to receive asynchronous result notifications.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun lockUnlockChannel(tvChannel: TvChannel, lockUnlock: Boolean,callback: IAsyncCallback, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Check if channel lock is available for a TV channel.
     *
     * @param tvChannel The TV channel to check for lock availability.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return `true` if channel lock is available for the channel, `false` otherwise.
     */
    fun isChannelLockAvailable(tvChannel: TvChannel, applicationMode: ApplicationMode = ApplicationMode.DEFAULT): Boolean

    /**
     * Skip or unskip a TV channel.
     *
     * @param tvChannel The TV channel to skip or unskip.
     * @param skipUnskip `true` to skip the channel, `false` to unskip it.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return `true` if the channel was successfully skipped or unskipped, `false` otherwise.
     */
    fun skipUnskipChannel(tvChannel: TvChannel, skipUnskip: Boolean, applicationMode: ApplicationMode = ApplicationMode.DEFAULT): Boolean

    /**
     * Get the index of the desired TV channel.
     *
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return The index of the desired TV channel.
     */
    fun getDesiredChannelIndex(applicationMode: ApplicationMode = ApplicationMode.DEFAULT): Int

    /**
     * Retrieve a list of TV input information.
     *
     * @param receiver Callback to receive asynchronous data result notifications.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun getTvInputList(receiver: IAsyncDataCallback<MutableList<TvInputInfo>>, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Get a list of TV channels filtered by categories.
     *
     * @param callback Callback to receive asynchronous data result notifications.
     * @param entityCategory The filter item type for entity category (e.g., genre, favorite, etc.).
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun getChannelListByCategories(callback: IAsyncDataCallback<ArrayList<TvChannel>>, entityCategory: FilterItemType?, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Check if parental controls are enabled.
     *
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return `true` if parental controls are enabled, `false` otherwise.
     */
    fun isParentalEnabled(applicationMode: ApplicationMode = ApplicationMode.DEFAULT) : Boolean
    /**
     * Check if TV navigation is blocked.
     *
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return `true` if TV navigation is blocked, `false` otherwise.
     */
    fun isTvNavigationBlocked(applicationMode: ApplicationMode = ApplicationMode.DEFAULT): Boolean

    /**
     * Initialize skipped channels.
     *
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun initSkippedChannels(applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Check if a TV channel is locked.
     *
     * @param channelId The ID of the channel to check.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return `true` if the channel is locked, `false` otherwise.
     */
    fun isChannelLocked(channelId: Int, applicationMode: ApplicationMode = ApplicationMode.DEFAULT): Boolean

    /**
     * Get the source label for a TV channel.
     *
     * @param tvChannel The TV channel to retrieve the source label for.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return The source label of the TV channel.
     */
    fun getTifChannelSourceLabel(tvChannel: TvChannel, applicationMode: ApplicationMode = ApplicationMode.DEFAULT): String

    /**
     * Get the source type of a TV channel.
     *
     * @param tvChannel The TV channel to retrieve the source type for.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return The source type of the TV channel.
     */
    fun getChannelSourceType(tvChannel: TvChannel, applicationMode: ApplicationMode = ApplicationMode.DEFAULT): String

    /**
     * Get the analog tuner type name for a TV channel.
     *
     * @param tvChannel The TV channel to retrieve the analog tuner type name for.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return The analog tuner type name of the TV channel.
     */
    fun getAnalogTunerTypeName(tvChannel: TvChannel, applicationMode: ApplicationMode = ApplicationMode.DEFAULT): String

    /**
     * Get the analog service list ID for a TV channel.
     *
     * @param tvChannel The TV channel to retrieve the analog service list ID for.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return The analog service list ID of the TV channel.
     */
    fun getAnalogServiceListID(tvChannel: TvChannel, applicationMode: ApplicationMode = ApplicationMode.DEFAULT): Int

    /**
     * Get the display name of a parental rating.
     *
     * @param parentalRating The parental rating to retrieve the display name for.
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return The display name of the parental rating.
     */
    fun getParentalRatingDisplayName(parentalRating: String?, applicationMode: ApplicationMode = ApplicationMode.DEFAULT, tvEvent: TvEvent): String

    /**
     * Check if the Logical Channel Number (LCN) feature is enabled.
     *
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return `true` if LCN is enabled, `false` otherwise.
     */
    fun isLcnEnabled(applicationMode: ApplicationMode = ApplicationMode.DEFAULT):Boolean

    /**
     * Get the locked channel list after over-the-air (OTA) update.
     *
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     */
    fun getLockedChannelListAfterOta(applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    /**
     * Get the list of visually impaired audio tracks.
     *
     * @param applicationMode The application mode to use (default is [ApplicationMode.DEFAULT]).
     * @return The list of visually impaired audio tracks.
     */
    fun getVisuallyImpairedAudioTracks(applicationMode: ApplicationMode = ApplicationMode.DEFAULT): List<String>

    fun isChannelSelectable(channel : TvChannel) : Boolean

    fun isSignalAvailable(): Boolean

    fun isChannelsAvailable(): Boolean

    fun isWaitingChannel(): Boolean

    fun isPlayerTimeout(): Boolean

    fun isNetworkAvailable(): Boolean

    fun appJustStarted(): Boolean

    fun forceChannelsRefresh(applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    fun addDirectTuneChannel(index: String, context: Context):TvChannel?
    
    fun checkAndRunBarkerChannel(run : Boolean)

}