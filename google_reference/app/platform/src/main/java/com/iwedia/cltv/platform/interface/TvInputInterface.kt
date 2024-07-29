package com.iwedia.cltv.platform.`interface`

import android.media.tv.ContentRatingSystem
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvEvent

/**
 * Tv Input interface definition
 *
 * @author Dejan Nadj
 */
interface TvInputInterface {

    /**
     * Get tv input manager instance
     */
    fun getTvInputManager(): TvInputManager

    /**
     * Get tv input list
     *
     * @param callback callback
     */
    fun getTvInputList(callback: IAsyncDataCallback<ArrayList<TvInputInfo>>)

    /**
     * Returns tv input list without input that contains filter text as id value
     *
     * @param filter input that should be filtered
     */
    open fun getTvInputFilteredList(filter: String, callback: IAsyncDataCallback<ArrayList<TvInputInfo>>)

    /**
     * Start setup activity for the given tv input
     *
     * @param input tv input for which to start setup activity
     * @param callback callback
     */
    open fun startSetupActivity(input: TvInputInfo, callback: IAsyncCallback)

    /**
     * Trigger scan callback
     *
     * @param isSuccessful scan finished successfully
     */
    fun triggerScanCallback(isSuccessful: Boolean)

    /**
     * Get channel count for the tv input
     *
     * @param input tv input
     * @param callback callback
     */
    fun getChannelCountForInput(input: TvInputInfo, callback: IAsyncDataCallback<Int>)

    /**
     * Function to check if parental is on or off
     */
    fun isParentalEnabled() : Boolean

    fun getContentRatingSystems(): List<ContentRatingSystem>

    fun getContentRatingSystemsList(): MutableList<ContentRatingSystem>

    fun getContentRatingSystemDisplayName(contentRatingSystem: ContentRatingSystem): String

    fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String
}