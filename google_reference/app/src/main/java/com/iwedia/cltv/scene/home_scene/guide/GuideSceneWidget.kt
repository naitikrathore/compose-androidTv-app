package com.iwedia.cltv.scene.home_scene.guide

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.components.CategoryItem
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import world.widget.GWidget
import world.widget.GWidgetListener

/**
 * Guide scene widget abstract class
 *
 * @author Dejan Nadj
 */
abstract class GuideSceneWidget (var context: Context, listener: GuideSceneWidgetListener) :
    GWidget<ConstraintLayout, GWidgetListener>(0, 0, listener) {
    //Selected even list view
    var selectedEventListView = -1
    //Loading flag
    var isLoading = false
    //Day offset previous day, today or next day
    var dayOffset : Int = 0

    abstract  fun selectedFilterList(selectFirst: Boolean, selectLast: Boolean)
    abstract fun resume()
    abstract fun pause()
    abstract fun channelChanged()
    //move the focus on guide only should not do the actual zap
    abstract fun zapOnGuideOnly(channel: TvChannel)
    abstract fun update(map: LinkedHashMap<Int, MutableList<TvEvent>>)
    abstract fun refreshFavoriteButton()
    abstract fun refreshFilterList(filterList: MutableList<CategoryItem>)
    abstract fun extendTimeline(map: LinkedHashMap<Int, MutableList<TvEvent>>, additionalDayCount: Int)
    abstract fun refreshRecordButton()
    abstract fun refreshWatchlistButton()
    abstract fun refreshIndicators()
    abstract fun setFocusToCategory()
    abstract fun setInitialData()
    abstract fun refreshGuideOnUpdateFavorite()
    abstract fun getActiveCategory(): CategoryItem
    abstract fun refreshClosedCaption()

    /**
     * refreshEpg is used to refresh epg whenever we have new data into or db
     */
    abstract fun refreshEpg()

    /**
     * this method is used to automatic scroll after certain time i.e, 30 mins
     */
    abstract fun scrollRight()

    /**
     *this method will be called when fails to fetch events or channels.
     */
    abstract fun onFetchFailed()

    /**
     * this method is useful when we update fav from more info and to refresh on back press
     */
    abstract fun updateFavSelectedItem(favorites: ArrayList<String>, filterList: ArrayList<CategoryItem>)
    abstract fun onDigitPress(digit : Int)
}