package com.iwedia.cltv.platform.t56

import android.content.Context
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.util.Log
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.base.CategoryInterfaceBaseImpl
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
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.favorite.FavoriteItem
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

internal class CategoryInterfaceImpl(
    context: Context,
    tvModule: TvInterface,
    favoritesModule: FavoritesInterface,
    utilsModule: UtilsInterface,
    fastDataProvider: FastDataProviderInterface
) : CategoryInterfaceBaseImpl(context, tvModule, favoritesModule, utilsModule, fastDataProvider) {
    private val TAG = "CategoryInterfaceBaseImpl"

    private val allChannels: ArrayList<TvChannel> = arrayListOf()
    override var activeCategoryName = ""

    // epg current remember filter position
    private var epgActiveFilter = 0
    private var checkCount = 0
    private val CHECK_TRESHOLD = 6

    /**
     * Refresh channels
     *
     * @param callback callback
     */
    private fun refreshChannels(callback: IAsyncCallback) {
        allChannels.clear()

        tvModule?.getChannelList()?.forEach { item ->
            if (!item.isFastChannel())
                allChannels.add(item)
        }
        tvModule?.initSkippedChannels()
        callback.onSuccess()
    }

    override fun getActiveCategory(applicationMode: ApplicationMode): String {
        return if (applicationMode == ApplicationMode.FAST_ONLY) super.activeCategoryName
        else activeCategoryName
    }

    override fun setActiveCategory(activeCategory: String, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) super.setActiveCategory(activeCategory, applicationMode)
        else this.activeCategoryName = activeCategory
    }

    override fun getActiveEpgFilter(applicationMode: ApplicationMode): Int {
        return if (applicationMode == ApplicationMode.FAST_ONLY) super.getActiveEpgFilter(applicationMode)
        else epgActiveFilter
    }

    override fun setActiveEpgFilter(filterId: Int, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.setActiveEpgFilter(filterId, applicationMode)
        } else {
            this.epgActiveFilter = filterId
        }
    }

    override fun getActiveCategoryChannelList(
        activeCategoryId: Int,
        callback: IAsyncDataCallback<ArrayList<TvChannel>>,
        applicationMode: ApplicationMode
    ) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.getActiveCategoryChannelList(activeCategoryId, callback, applicationMode)
        } else {
            refreshChannels(object : IAsyncCallback {
                override fun onFailed(error: Error) {
                }

                override fun onSuccess() {
                    if (activeCategoryId == Category.ALL_ID) {
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

    }

    override fun getAvailableFilters(callback: IAsyncDataCallback<ArrayList<Category>>, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.getAvailableFilters(callback, applicationMode)
        } else {
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

                        checkForTvInputs(object : IAsyncDataCallback<ArrayList<Category>> {
                            override fun onFailed(error: Error) {
                                checkCount++
                                if (checkCount >= CHECK_TRESHOLD) {
                                    handleFilterResult(availableFilters, callback)
                                }
                            }

                            override fun onReceive(data: ArrayList<Category>) {
                                if (data.size != 0) {
                                    availableFilters.addAll(data)
                                }

                                checkCount++
                                if (checkCount >= CHECK_TRESHOLD) {
                                    handleFilterResult(availableFilters, callback)
                                }
                            }
                        })

                        checkForRecentChannels(object : IAsyncDataCallback<ArrayList<Category>> {
                            override fun onFailed(error: Error) {
                                checkCount++
                                if (checkCount >= CHECK_TRESHOLD) {
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

                        checkForFavorites(object : IAsyncDataCallback<ArrayList<Category>> {
                            override fun onFailed(error: Error) {
                                checkCount++
                                if (checkCount >= CHECK_TRESHOLD) {
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

                        checkForRadioChannels(object : IAsyncDataCallback<ArrayList<Category>> {
                            override fun onFailed(error: Error) {
                                checkCount++
                                if (checkCount >= CHECK_TRESHOLD) {
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

                        checkForTunerTypeCategories(object :
                            IAsyncDataCallback<ArrayList<Category>> {
                            override fun onFailed(error: Error) {
                                checkCount++
                                if (checkCount >= CHECK_TRESHOLD) {
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

    }

    private fun handleFilterResult(
        existingItems: ArrayList<Category>,
        callback: IAsyncDataCallback<ArrayList<Category>>
    ) {
        var allFilter = Category(
            Category.ALL_ID,
            utilsModule.getStringValue("all")
        )
        allFilter.priority = 0

        existingItems.add(0, allFilter)

        //Sort filter list based on priority value
        var sortList = mutableListOf<Category>()
        sortList.addAll(existingItems)

        Collections.sort(sortList, object : Comparator<Category> {
            override fun compare(f1: Category, f2: Category): Int {
                val category1: Category = f1 as Category
                val category2: Category = f2 as Category
                if (category1.priority == category2.priority) {
                    return category1.name!!.compareTo(category2.name!!)
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

    private fun checkForTvInputs(callback: IAsyncDataCallback<ArrayList<Category>>) {

        val filterListItems: ArrayList<Category> = arrayListOf()
        val tvInputInfos = ArrayList<TvInputInfo>()
        //Get all TV inputs
        for (input in (context
            .getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager).tvInputList) {
            tvInputInfos.add(input)
        }

        val tvInputIndexOffset = intArrayOf(0)
        for (tvInputInfo in tvInputInfos) {

            var isInputScanned = false
            allChannels.forEach { channel ->

                try {
                    if (!(channel.packageName.lowercase().contains("com.mediatek.tvinput"))
                        && !(channel.packageName.lowercase()
                            .contains("com.mediatek.dtv.tvinput.atsctuner"))
                        && !(channel.packageName.lowercase().contains("com.mediatek.tis"))
                        && !(channel.packageName.lowercase().contains("com.realtek.dtv"))
                        && !(channel.packageName.lowercase().contains("com.iwedia.tvinput"))
                        && !(channel.packageName.lowercase().contains("com.mediatek.tis"))
                        && tvInputInfo.id.equals(channel.inputId, ignoreCase = true)
                    ) {
                        isInputScanned = true
                    }
                } catch (E: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: ${E.message}")
                }
            }

            if (isInputScanned) {
                if (!tvInputInfo.id.contains("Anoki")) {
                    var filterName =
                        tvInputInfo.loadLabel(context) as String
                    val filterListInputItem = Category(
                        Category.TIF_INPUT_CATEGORY + tvInputIndexOffset[0],
                        filterName
                    )
                    filterListInputItem.priority = 4
                    filterListItems.add(filterListInputItem)
                    tvInputIndexOffset[0]++
                }
            }
        }
        callback.onReceive(filterListItems)
    }

    private fun checkForRecentChannels(callback: IAsyncDataCallback<ArrayList<Category>>) {
        val recentlyCount: Int = tvModule!!.getRecentlyWatched()!!.size
        if (recentlyCount > 0) {
            val filterListItems: ArrayList<Category> = ArrayList()
            val filterListItem = Category(
                Category.RECENTLY_WATCHED_ID,
                utilsModule.getStringValue("recent")
            )
            filterListItem.priority = 1
            filterListItems.add(filterListItem)
            callback.onReceive(filterListItems)
            return
        }
        callback.onFailed(Error("No recents"))
    }

    private fun checkForFavorites(callback: IAsyncDataCallback<ArrayList<Category>>) {
        favoritesModule?.getAvailableCategories(object : IAsyncDataCallback<ArrayList<String>> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(categories: ArrayList<String>) {
                val index = AtomicInteger(0)
                val filterListItems: ArrayList<Category> = arrayListOf()
                for (favCategory in categories) {
                    favoritesModule!!.getFavoritesForCategory(favCategory,
                        object : IAsyncDataCallback<ArrayList<FavoriteItem>> {
                            override fun onReceive(data: ArrayList<FavoriteItem>) {
                                if (data.size > 0) {
                                    val filterListItem = Category(
                                        Category.FAVORITE_ID,
                                        favCategory
                                    )
                                    filterListItem.priority = 2
                                    filterListItems.add(filterListItem)
                                    if (index.incrementAndGet() == categories.size) {
                                        callback.onReceive(filterListItems)
                                    }
                                } else {
                                    if (index.incrementAndGet() == categories.size) {
                                        callback.onReceive(filterListItems)
                                    }
                                }
                            }

                            override fun onFailed(error: Error) {
                                if (index.incrementAndGet() == categories.size) {
                                    callback.onFailed(error)
                                }
                            }
                        })
                }
            }
        })
    }

    private fun checkForChannelCategories(callback: IAsyncDataCallback<ArrayList<Category>>) {
        var filters = ArrayList<Category>()
        var genres = ArrayList<String>()
        tvModule!!.getChannelList().forEach { tvChannel ->
            if (tvChannel.genres.isNotEmpty() && !tvChannel.isFastChannel()) {
                var genre = tvChannel.genres[0]
                if (!genres.contains(genre)) {
                    genres.add(genre)
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

    private fun checkForRadioChannels(callback: IAsyncDataCallback<ArrayList<Category>>) {
        var radioChannelsNumber = 0
        tvModule!!.getChannelList().forEach { tvChannel ->
            if (tvChannel.isRadioChannel) {
                radioChannelsNumber++
            }
        }
        if (radioChannelsNumber > 0) {
            val filterListItems: ArrayList<Category> = arrayListOf()
            val filterListItem = Category(
                Category.RADIO_CHANNELS_ID,
                utilsModule.getStringValue("radio")
            )
            filterListItem.priority = 3
            filterListItems.add(filterListItem)
            callback.onReceive(filterListItems)
        } else {
            callback.onFailed(Error())
        }
    }

    private fun checkForTunerTypeCategories(callback: IAsyncDataCallback<ArrayList<Category>>) {
        // Add tuner type filters
        var terrestrialFilterAvailable = false
        var cableFilterAvailable = false
        var satelliteFilterAvailable = false
        var analogAntennaFilterAvailable = false
        var analogCableFilterAvailable = false

        tvModule!!.getChannelList().forEach { channel ->
            if (channel.isBrowsable) {
                if (channel.tunerType == TunerType.TERRESTRIAL_TUNER_TYPE) {
                    terrestrialFilterAvailable = true
                }
                if (channel.tunerType == TunerType.CABLE_TUNER_TYPE) {
                    cableFilterAvailable = true
                }
                if (channel.tunerType == TunerType.SATELLITE_TUNER_TYPE) {
                    satelliteFilterAvailable = true
                }

                if (channel.tunerType == TunerType.ANALOG_TUNER_TYPE) {
                    when (tvModule?.getAnalogServiceListID(channel)) {
                        TunerType.TYPE_ANALOG_ANTENNA -> analogAntennaFilterAvailable = true
                        TunerType.TYPE_ANALOG_CABLE -> analogCableFilterAvailable = true
                    }
                }
            }
        }
        val filterListItems: ArrayList<Category> = arrayListOf()
        if (terrestrialFilterAvailable) {
            var terrestrialTunerFilter = Category(
                Category.TERRESTRIAL_TUNER_TYPE_ID,
                utilsModule.getStringValue("antenna_type")
            )
            terrestrialTunerFilter.priority = 3
            filterListItems.add(terrestrialTunerFilter)
        }
        if (cableFilterAvailable) {
            var cableTunerFilter = Category(
                Category.CABLE_TUNER_TYPE_ID,
                utilsModule.getStringValue("cable")
            )
            cableTunerFilter.priority = 3
            filterListItems.add(cableTunerFilter)
        }
        if (satelliteFilterAvailable) {
            var satelliteTunerFilter = Category(
                Category.SATELLITE_TUNER_TYPE_ID, utilsModule.getStringValue("satellite")
            )
            satelliteTunerFilter.priority = 3
            filterListItems.add(satelliteTunerFilter)
        }

        if (analogAntennaFilterAvailable) {
            var analogAntennaTunerFilter = Category(
                Category.ANALOG_ANTENNA_TUNER_TYPE_ID,
                utilsModule.getStringValue("analog_antenna")
            )
            analogAntennaTunerFilter.priority = 3
            filterListItems.add(analogAntennaTunerFilter)
        }

        if (analogCableFilterAvailable) {
            var analogCableTunerFilter = Category(
                Category.ANALOG_CABLE_TUNER_TYPE_ID,
                utilsModule.getStringValue("analog_cable")
            )
            analogCableTunerFilter.priority = 3
            filterListItems.add(analogCableTunerFilter)
        }

        callback.onReceive(filterListItems)
    }

    override fun filterChannels(
        category: Category,
        callback: IAsyncDataCallback<ArrayList<TvChannel>>,
        applicationMode: ApplicationMode
    ) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.filterChannels(category, callback, applicationMode)
        } else {
            refreshChannels(object : IAsyncCallback {
                override fun onSuccess() {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, " onSuccess: filterItem.id ${category.id} name ${category.name}")
                    if (category.id == Category.ALL_ID) {
                        callback.onReceive(allChannels)
                        return
                    } else if (category.id == Category.RECENTLY_WATCHED_ID) {
                        val recentlyWatchedList: ArrayList<TvChannel> = arrayListOf()
                        val channelIdList = mutableListOf<Long>()
                        val channelList = tvModule?.getChannelList()

                        channelList!!.forEach {
                            channelIdList.add(it.channelId)
                        }

                        for (playableItem in tvModule!!.getRecentlyWatched()!!) {
                            if (playableItem is TvChannel) {
                                if ((playableItem as TvChannel).channelId in channelIdList) {
                                    channelList.forEach {
                                        if (it.channelId == (playableItem as TvChannel).channelId) {
                                            recentlyWatchedList.add(it)
                                        }
                                    }
                                }
                            }
                        }
                        callback.onReceive(recentlyWatchedList)
                        return
                    } else if (category.id == Category.FAVORITE_ID) {

                        val favoriteChannelItems: ArrayList<TvChannel> = arrayListOf()
                        val favCategory = category.name
                        favoritesModule!!.getFavoritesForCategory(
                            favCategory!!,
                            object : IAsyncDataCallback<ArrayList<FavoriteItem>> {
                                override fun onReceive(data: ArrayList<FavoriteItem>) {
                                    data.forEach { favItem ->
                                        favoriteChannelItems.add(favItem.tvChannel)
                                    }
                                    callback.onReceive(favoriteChannelItems)
                                }

                                override fun onFailed(error: Error) {
                                    callback.onReceive(favoriteChannelItems)
                                }
                            })
                        return
                    } else if (category.id == Category.RADIO_CHANNELS_ID) {
                        val radioChannelItems: ArrayList<TvChannel> = arrayListOf()
                        tvModule!!.getChannelList().forEach { tvChannel ->
                            if (tvChannel.isRadioChannel) {
                                radioChannelItems.add(tvChannel)
                            }
                        }
                        callback.onReceive(radioChannelItems)
                        return
                    } else if (category.id == Category.TERRESTRIAL_TUNER_TYPE_ID) {
                        var list = ArrayList<TvChannel>()
                        tvModule!!.getChannelList().forEach { channel ->
                            if (channel?.tunerType == TunerType.TERRESTRIAL_TUNER_TYPE) {
                                list.add(channel)
                            }

                        }
                        callback.onReceive(list)
                        return
                    } else if (category.id == Category.CABLE_TUNER_TYPE_ID) {
                        var list = ArrayList<TvChannel>()
                        tvModule!!.getChannelList().forEach { channel ->
                            if (channel?.tunerType == TunerType.CABLE_TUNER_TYPE) {
                                list.add(channel)
                            }
                        }
                        callback.onReceive(list)
                        return
                    } else if (category.id == Category.SATELLITE_TUNER_TYPE_ID) {
                        var list = ArrayList<TvChannel>()
                        tvModule!!.getChannelList().forEach { channel ->
                            if (channel?.tunerType == TunerType.SATELLITE_TUNER_TYPE) {
                                list.add(channel)
                            }
                        }
                        callback.onReceive(list)
                        return
                    } else if (category.id == Category.ANALOG_ANTENNA_TUNER_TYPE_ID) {
                        var list = ArrayList<TvChannel>()

                        tvModule!!.getChannelList().forEach { channel ->
                            if (channel.tunerType == TunerType.ANALOG_TUNER_TYPE
                                && tvModule!!.getAnalogServiceListID(channel) == TunerType.TYPE_ANALOG_ANTENNA
                            ) {
                                list.add(channel)
                            }
                        }
                        callback.onReceive(list)
                        return
                    } else if (category.id == Category.ANALOG_CABLE_TUNER_TYPE_ID) {
                        var list = ArrayList<TvChannel>()
                        tvModule!!.getChannelList().forEach { channel ->
                            if (channel.tunerType == TunerType.ANALOG_TUNER_TYPE
                                && tvModule!!.getAnalogServiceListID(channel) == TunerType.TYPE_ANALOG_CABLE
                            ) {
                                list.add(channel)
                            }
                        }
                        callback.onReceive(list)
                        return
                    } else if (category.id == Category.GENRE_CATEGORY) {
                        var list = ArrayList<TvChannel>()
                        tvModule!!.getChannelList().forEach { channel ->
                            if (channel.genres.contains(category.name)) {
                                list.add(channel)
                            }
                        }
                        callback.onReceive(list)
                        return
                    }
                    tvModule!!.getTvInputList(object : IAsyncDataCallback<MutableList<TvInputInfo>> {
                        override fun onReceive(tvInputInfos: MutableList<TvInputInfo>) {
                            Log.d(Constants.LogTag.CLTV_TAG +
                                TAG,
                                " onReceive: getTvInputList filterItem.id ${category.id} tvInputInfos ${tvInputInfos.size} name ${category.name}"
                            )
                            val scannedInputs: MutableList<TvInputInfo> = mutableListOf()
                            try {
                                for (tvInputInfo in tvInputInfos) {
                                    var isInputScanned = false

                                    run exitForEach@{
                                        allChannels.forEach { channel ->
                                            if (!(channel.packageName.lowercase()
                                                    .contains("com.mediatek.tvinput"))
                                                && !(channel.packageName.lowercase()
                                                    .contains("com.mediatek.dtv.tvinput.atsctuner"))
                                                && !(channel.packageName.lowercase()
                                                    .contains("com.realtek.dtv"))
                                                && !(channel.packageName.lowercase()
                                                    .contains("com.iwedia.tvinput"))
                                                && !(channel.packageName.lowercase()
                                                    .contains("com.mediatek.tis"))
                                                && tvInputInfo.id.equals(
                                                    channel.inputId,
                                                    ignoreCase = true
                                                )
                                            ) {
                                                isInputScanned = true
                                                return@exitForEach
                                            }
                                        }
                                    }
                                    if (isInputScanned) {
                                        scannedInputs.add(tvInputInfo)
                                    }
                                }
                                val inputIndex: Int = category.id - Category.TIF_INPUT_CATEGORY
                                if ((inputIndex < tvInputInfos.size) && (inputIndex >= 0)) {
                                    if (inputIndex >= 0 && (scannedInputs.size > 0)) {
                                        try {
                                            val tvInputInfo = scannedInputs[inputIndex]
                                            val inputId = tvInputInfo.id
                                            val filteredChannels: MutableList<TvChannel> =
                                                mutableListOf()
                                            allChannels.forEach { channelListItem ->
                                                if (inputId.equals(channelListItem!!.inputId)) {
                                                    filteredChannels.add(channelListItem)
                                                }
                                            }
                                            if (filteredChannels.size > 0) {
                                                callback.onReceive(filteredChannels as ArrayList<TvChannel>)
                                                return
                                            } else {
                                                val filteredChannels: MutableList<TvChannel> =
                                                    mutableListOf()
                                                callback.onReceive(filteredChannels as ArrayList<TvChannel>)
                                                return
                                            }
                                        } catch (E: Exception) {
                                            val filteredChannels: MutableList<TvChannel> =
                                                mutableListOf()
                                            callback.onReceive(filteredChannels as ArrayList<TvChannel>)
                                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: ${E.printStackTrace()}")
                                            return
                                        }
                                    } else {
                                        val filteredChannels: MutableList<TvChannel> = mutableListOf()
                                        callback.onReceive(filteredChannels as ArrayList<TvChannel>)
                                        return
                                    }
                                }
                                filterRegularCategories(category, callback)
                            } catch (E: Exception) {
                                val filteredChannels: MutableList<TvChannel> = mutableListOf()
                                callback.onReceive(filteredChannels as ArrayList<TvChannel>)
                                return
                            }
                        }

                        override fun onFailed(error: kotlin.Error) {
                            Log.d(Constants.LogTag.CLTV_TAG +
                                TAG,
                                " onFailed: getTvInputList filterItem.id ${category.id} name ${category.name}"
                            )
                            callback.onFailed(error)
                        }
                    })
                }

                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG +
                        TAG,
                        "refreshChannels filterItem.id ${category.id} name ${category.name}"
                    )
                    callback.onFailed(error)
                }
            })
        }
    }

    /**
     * Filter regular categories embedded inside channels (genres, types, metadata info)
     *
     * @param category filter item
     * @param callback   callback
     */
    private fun filterRegularCategories(
        category: Category,
        callback: IAsyncDataCallback<ArrayList<TvChannel>>
    ) {
        Log.d(
            Constants.LogTag.CLTV_TAG +
            TAG,
            " filterRegularCategories: filterItem.id ${category.id} name ${category.name}"
        )
        tvModule!!.getChannelListByCategories(object : IAsyncDataCallback<ArrayList<TvChannel>> {
            override fun onFailed(error: kotlin.Error) {
                Log.d(Constants.LogTag.CLTV_TAG +
                    TAG,
                    " onFailed: filterRegularCategories getChannelListByCategories name ${category.name} "
                )
                callback.onFailed(error)
            }

            override fun onReceive(data: ArrayList<TvChannel>) {
                Log.d(Constants.LogTag.CLTV_TAG +
                    TAG,
                    " onReceive: filterRegularCategories getChannelListByCategories name ${category.name}"
                )
                callback.onReceive(data)
            }
        }, null)
    }
}