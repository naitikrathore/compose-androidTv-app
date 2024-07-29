package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.category.Category
import java.util.ArrayList

interface CategoryInterface {

    /**
     * Get active channels category name
     *
     * @return active category name
     */
    fun getActiveCategory(applicationMode: ApplicationMode = ApplicationMode.DEFAULT): String

    /**
     * Set channels active category name
     *
     * @param activeCategory active category name
     */
    fun setActiveCategory(activeCategory: String, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)
    /**
     * Get active epg filter id
     *
     * @return active epg filter id
     */
    fun getActiveEpgFilter(applicationMode: ApplicationMode = ApplicationMode.DEFAULT): Int
    /**
     * Set active epg filter
     *
     * @param filterId filter id
     */
    fun setActiveEpgFilter(filterId: Int, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)
    /**
     * Get active channel list for the active category
     *
     * @param activeCategoryId category id
     * @param callback
     */
    fun getActiveCategoryChannelList(activeCategoryId: Int, callback: IAsyncDataCallback<ArrayList<TvChannel>>, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)
    /**
     * Get available channels category filters
     * @param callback  callback
     */
    fun getAvailableFilters(callback: IAsyncDataCallback<ArrayList<Category>>, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)
    /**
     * Filter channels
     *
     * @param category filter item
     * @param callback   callback
     */
    fun filterChannels(category: Category, callback: IAsyncDataCallback<ArrayList<TvChannel>>, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)
}