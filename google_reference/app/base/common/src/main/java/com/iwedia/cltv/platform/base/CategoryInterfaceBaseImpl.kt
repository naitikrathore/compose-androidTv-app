package com.iwedia.cltv.platform.base

import android.content.Context
import android.util.Log
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.CategoryInterface
import com.iwedia.cltv.platform.`interface`.FastDataProviderInterface
import com.iwedia.cltv.platform.`interface`.FavoritesInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.category.Category
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections

open class CategoryInterfaceBaseImpl(
    protected var context: Context,
    protected var tvModule: TvInterface,
    protected var favoritesModule: FavoritesInterface,
    protected var utilsModule: UtilsInterface,
    var fastDataProvider: FastDataProviderInterface
) : CategoryInterface {
    private val TAG = "CategoryInterfaceBaseImpl"

    private val allChannels: ArrayList<TvChannel> = arrayListOf()
    open var activeCategoryName = ""

    // epg current remember filter position
    private var epgActiveFilter = 0
    private var checkCount = 0
    private val CHECK_TRESHOLD = 1
    private val enableAllCategory = false
    private val refreshChannelsMutex = Any()

    /**
     * Refresh channels
     *
     * @param callback callback
     */
    @Synchronized
    private fun refreshChannels(callback: IAsyncCallback) {
        synchronized(refreshChannelsMutex) {
            val tempChannel = arrayListOf<TvChannel>()
            allChannels.clear()
            tvModule?.getChannelList(ApplicationMode.FAST_ONLY)?.forEach { item ->
                tempChannel.add(item)
            }
            allChannels.addAll(tempChannel)
            tvModule?.initSkippedChannels()
            callback.onSuccess()
        }
    }

    override fun getActiveCategory(applicationMode: ApplicationMode): String = activeCategoryName

    override fun setActiveCategory(activeCategory: String,applicationMode: ApplicationMode) {
        this.activeCategoryName = activeCategory
    }

    override fun getActiveEpgFilter(applicationMode: ApplicationMode): Int = epgActiveFilter

    override fun setActiveEpgFilter(filterId: Int, applicationMode: ApplicationMode) {
        this.epgActiveFilter = filterId
    }

    override fun getActiveCategoryChannelList(
        activeCategoryId: Int,
        callback: IAsyncDataCallback<ArrayList<TvChannel>>,
        applicationMode: ApplicationMode
    ) {
        refreshChannels(object : IAsyncCallback {
            override fun onFailed(error: Error) {
            }

            override fun onSuccess() {
                if (enableAllCategory && activeCategoryId == Category.ALL_ID) {
                    callback.onReceive(allChannels)
                    return
                }
                getAvailableFilters(object : IAsyncDataCallback<ArrayList<Category>> {

                    override fun onFailed(error: Error) {
                        callback.onFailed(error)
                    }

                    override fun onReceive(data: ArrayList<Category>) {
                        data.forEach { filterItem ->
                            if (filterItem.id == activeCategoryId) {
                                filterChannels(filterItem, callback)
                                return
                            }
                        }
                    }
                })
            }
        })
    }

    override fun getAvailableFilters(callback: IAsyncDataCallback<ArrayList<Category>>, applicationMode: ApplicationMode) {
        checkCount = 0
        CoroutineHelper.runCoroutine({
            refreshChannels(object : IAsyncCallback {
                override fun onFailed(error: Error) {
                    callback.onFailed(error)
                }

                override fun onSuccess() {
                    var availableFilters = ArrayList<Category>()
                    checkForChannelCategories(object : IAsyncDataCallback<ArrayList<Category>> {
                        override fun onFailed(error: Error) {
                            checkCount++

                            if (checkCount == CHECK_TRESHOLD) {
                                handleFilterResult(availableFilters, callback)
                            }
                        }

                        override fun onReceive(data: ArrayList<Category>) {
                            availableFilters.addAll(data)

                            checkCount++
                            if (checkCount >= CHECK_TRESHOLD) {
                                handleFilterResult(availableFilters, callback)
                            }
                        }
                    })
                }
            })
        })
    }

    private fun handleFilterResult(
        existingItems: ArrayList<Category>,
        callback: IAsyncDataCallback<ArrayList<Category>>
    ) {
        if (enableAllCategory) {
            var allFilter = Category(
                Category.ALL_ID,
                utilsModule.getStringValue("all")
            )
            allFilter.priority = 0

            existingItems.add(0, allFilter)
        }

        //Sort filter list based on priority value
        var sortList = mutableListOf<Category>()
        sortList.addAll(existingItems)

        Collections.sort(sortList, object : Comparator<Category> {
            override fun compare(f1: Category, f2: Category): Int {
                val category1: Category = f1 as Category
                val category2: Category = f2 as Category
                if (category1.priority == category2.priority) {
                    return 0
                } else {
                    return category1.priority - category2.priority
                }
            }
        })

        existingItems.clear()
        sortList.forEach { filterItem ->
            existingItems.add(filterItem)
        }

        callback.onReceive(existingItems)
        checkCount = 0
    }

    /**
     * Check is there available channels in the channel list
     * for the particular genre
     */
    private fun checkGenre(genre: String): Boolean {
        tvModule!!.getChannelList(ApplicationMode.FAST_ONLY).forEach { tvChannel ->
            if (tvChannel.genres.isNotEmpty() && tvChannel.isFastChannel()) {
                if (tvChannel.genres.contains(genre)) {
                    return true
                }
            }
        }
        return false
    }

    @Synchronized
    private fun checkForChannelCategories(callback: IAsyncDataCallback<ArrayList<Category>>) {
        var filters = ArrayList<Category>()
        var genres = ArrayList<String>()
        val genreList = arrayListOf<String>()
        genreList.addAll(fastDataProvider.getGenreList())
        if (genreList.isNotEmpty()) {
            genreList.forEach { genre->
                if (!genres.contains(genre) && checkGenre(genre)) {
                    genres.add(genre)
                }
            }
        } else {
            tvModule!!.getChannelList(ApplicationMode.FAST_ONLY).forEach { tvChannel ->
                if (tvChannel.genres.isNotEmpty() && tvChannel.isFastChannel()) {
                    var genre = tvChannel.genres[0]
                    if (!genres.contains(genre)) {
                        genres.add(genre)
                    }
                }
            }
        }

        var id = 100
        genres.forEach { genre ->
            if (genre.isNotEmpty()) {
                var category = Category(id, genre)
                category.priority = 5
                filters.add(category)
            }
        }

        callback.onReceive(filters)
    }

    override fun filterChannels(
        category: Category,
        callback: IAsyncDataCallback<ArrayList<TvChannel>>,
        applicationMode: ApplicationMode
    ) {
        refreshChannels(object : IAsyncCallback {
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, " onSuccess: filterItem.id ${category.id} name ${category.name}")
                if (enableAllCategory && category.id == Category.ALL_ID) {
                    callback.onReceive(allChannels)
                    return
                } else if (category.id == Category.GENRE_CATEGORY) {
                    var list = ArrayList<TvChannel>()
                    tvModule!!.getChannelList(ApplicationMode.FAST_ONLY).forEach { channel ->
                        if (channel.genres.contains(category.name) && channel.isFastChannel()) {
                            list.add(channel)
                        }
                    }
                    callback.onReceive(list)
                    return
                }
            }

            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "refreshChannels filterItem.id ${category.id} name ${category.name}")
                callback.onFailed(error)
            }
        })
    }
}