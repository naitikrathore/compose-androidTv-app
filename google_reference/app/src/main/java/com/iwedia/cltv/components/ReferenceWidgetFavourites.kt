package com.iwedia.cltv.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.CountDownTimer
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.*
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.scene.favourite.FavouriteCategoryExpandedAdapter
import com.iwedia.cltv.scene.favourite.FavouriteCategoryExpandedViewHolder
import com.iwedia.cltv.scene.favourite.FavouriteChannelsAdapter
import com.iwedia.cltv.utils.AnimationListener
import com.iwedia.cltv.utils.Utils
import world.widget.GWidget
import world.widget.GWidgetListener

/**
 * Reference widget favourites
 *
 * @author Aleksandar Lazic
 */
class ReferenceWidgetFavourites :
    GWidget<ConstraintLayout, ReferenceWidgetFavourites.FavouritesWidgetListener> {

    var context: Context? = null

    //Scene layout views references
    private var favouritesFilterGridView: HorizontalGridView? = null
    private var favouritesChannelsGridView: VerticalGridView? = null

    //List adapters
    private val favouritesFilterCategoryAdapter = FavouriteCategoryExpandedAdapter()
    private val favouritesChannelsAdapter = FavouriteChannelsAdapter()
    private var selectedItemTimer: CountDownTimer?= null

    //add button
    var addButton: ImageView? = null

    //categories
    val categories = mutableListOf<String>()

    //active favourite category
    private var activeCategory = 0

    var channels = mutableListOf<TvChannel>()

    //views
    var noEventsMsg: TextView? = null

    var dpadDownFromAddButton = false

    var hintMessageText: TextView? = null

    var categoryFocus = false
    val TAG = javaClass.simpleName
    constructor(
        context: Context,
        listener: FavouritesWidgetListener
    ) : super(
        ReferenceWorldHandler.WidgetId.FAVOURITES,
        ReferenceWorldHandler.WidgetId.FAVOURITES,
        listener
    ) {
        this.context = context

        view = LayoutInflater.from(context)
            .inflate(R.layout.layout_widget_favourites, null) as ConstraintLayout

        //find refs
        favouritesFilterGridView = view!!.findViewById(R.id.favourites_filter_list_view)
        favouritesChannelsGridView = view!!.findViewById(R.id.fav_channels_grid_view)
        noEventsMsg = view!!.findViewById(R.id.no_events_msg)
        noEventsMsg!!.setText(ConfigStringsManager.getStringById("no_categories_msg"))
        noEventsMsg!!.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")))
        noEventsMsg!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )
        val topFade: View = view!!.findViewById(R.id.bg_top_fade)
        view!!.findViewById<View>(R.id.bg_top_fade_channel_scene_solid_part).background = ColorDrawable(Color.parseColor(ConfigColorManager.getColor("color_gradient")))

        Utils.makeGradient(
            view!!.findViewById(R.id.bg_top_fade_channel_scene),
            GradientDrawable.LINEAR_GRADIENT,
            GradientDrawable.Orientation.TOP_BOTTOM,
            Color.parseColor(ConfigColorManager.getColor("color_gradient")),
            Color.TRANSPARENT,
            0.0F,
            0.35F
        )

        hintMessageText = view!!.findViewById<TextView?>(R.id.hint_overlay_text).apply {
            text = ConfigStringsManager.getStringById("long_press_ok_edit_favorite_list")
            setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
            typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))
        }

        //add button
        addButton = view!!.findViewById(R.id.addButton)
        addButton!!.setBackgroundResource(R.drawable.add)

        addButton!!.focusable = View.FOCUSABLE
        addButton!!.elevation = 10f
        val selectorDrawable =
            ContextCompat.getDrawable(context, R.drawable.reference_button_focus_shape)
        DrawableCompat.setTint(
            selectorDrawable!!,
            Color.parseColor(ConfigColorManager.getColor("color_selector"))
        )

        addButton!!.imageTintList =
            ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        addButton!!.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        addButton!!.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                Utils.unFocusAnimation(addButton!!)
                addButton!!.setBackgroundResource(R.drawable.reference_button_focus_shape)
                addButton!!.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.add_focused
                    )
                )
                try {
                    val color_context =
                        Color.parseColor(ConfigColorManager.getColor("color_background"))
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Constructor: Exception color_context $color_context")
                    addButton!!.imageTintList = ColorStateList.valueOf(color_context)

                } catch (ex: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Constructor: Exception color_context $ex")
                }
                try {
                    val color_context =
                        Color.parseColor(ConfigColorManager.getColor("color_selector"))
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Constructor: Exception color_context $color_context")
                    addButton!!.backgroundTintList = ColorStateList.valueOf(color_context)

                } catch (ex: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Constructor: Exception color_context $ex")
                }
            } else {
                addButton!!.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.add))
                try {
                    val color_context =
                        Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Constructor: Exception color_context $color_context")
                    addButton!!.imageTintList = ColorStateList.valueOf(color_context)

                } catch (ex: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Constructor: Exception color rdb $ex")
                }
                addButton!!.setBackgroundResource(0)
//                addButton!!.backgroundTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text").replace("#",ConfigColorManager.alfa_light)))

            }
        }

        addButton!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(view: View?, keycode: Int, keyevent: KeyEvent?): Boolean {

                if (keyevent!!.action == KeyEvent.ACTION_DOWN) {
                    if (keycode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        return true
                    }

                    if (keycode == KeyEvent.KEYCODE_DPAD_UP) {
                        listener.requestFocusOnTopMenu()
                        return true
                    }

                    if (keycode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        dpadDownFromAddButton = true
                        favouritesChannelsGridView!!.requestFocus()
                        return true
                    }
                }

                return false
            }
        })

        addButton!!.setOnClickListener {
            //this method is called to restart inactivity timer for no signal power off
            (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

            Utils.viewClickAnimation(addButton!!, object : AnimationListener {
                override fun onAnimationEnd() {
                    listener.onAddButtonClicked()
                }

            })
        }

        //setup adapters
        favouritesFilterGridView!!.setNumRows(1)
        favouritesFilterGridView!!.adapter = favouritesFilterCategoryAdapter
        favouritesFilterGridView!!.preserveFocusAfterLayout = true
        favouritesChannelsGridView!!.setNumColumns(5)
        favouritesChannelsGridView!!.setItemSpacing(Utils.getDimensInPixelSize(R.dimen.custom_dim_12_5))
        favouritesChannelsGridView!!.adapter = favouritesChannelsAdapter

        //setup adapter listeners
        favouritesChannelsAdapter.setListener(object : FavouriteChannelsAdapter.AdapterListener {
            override fun onItemClicked(tvChannel: TvChannel) {
                //this method is called to restart inactivity timer for no signal power off
                (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                val favListIds = ArrayList<String>()
                val selectedCategory = categories[favouritesFilterCategoryAdapter.selectedItem]
                val isAdded = tvChannel.favListIds.contains(selectedCategory)

                favListIds.addAll(tvChannel.favListIds)

                if (isAdded) {
                    favListIds.remove(selectedCategory)
                    listener.changeFilterToAll(categories[activeCategory])
                } else {
                    favListIds.add(selectedCategory)
                }

                listener.onChannelClicked(tvChannel, favListIds)
            }

            override fun getAdapterPosition(position: Int) {
                if (position > 4 && channels.size > 15) {
                    topFade.translationZ = 2f
                    topFade.visibility = View.VISIBLE
                } else {
                    topFade.translationZ = 0f
                    topFade.visibility = View.INVISIBLE
                }
            }

            override fun requestFocusOnFilters() {
                if (!dpadDownFromAddButton) {
                    favouritesFilterGridView!!.requestFocus()
                } else {
                    addButton!!.requestFocus()
                }
            }

        })
        favouritesFilterCategoryAdapter.selectedItem = 0
        favouritesFilterCategoryAdapter.adapterListener =
            object : FavouriteCategoryExpandedAdapter.AdapterListener {
                override fun getAdapterPosition(position: Int) {
                    setHintVisibility(true)
                    selectedItemTimer?.cancel()
                    selectedItemTimer = null
                    selectedItemTimer = object :
                        CountDownTimer(
                            500,
                            500
                        ) {
                        override fun onTick(millisUntilFinished: Long) {}
                        override fun onFinish() {
                            // To start marquee effect on hint message
                            hintMessageText!!.isSelected = true

                            if (position == activeCategory) return
                            if (position < categories.size) {
                                favouritesChannelsGridView!!.visibility = View.VISIBLE
                                favouritesChannelsAdapter.focusedItemPosition = -1
                                favouritesChannelsGridView!!.visibility = View.INVISIBLE
                                favouritesChannelsAdapter.refreshSelectedCategory(categories[position])
                                favouritesChannelsGridView!!.visibility = View.VISIBLE
                                activeCategory = position
                            }
                            favouritesFilterCategoryAdapter.clearPreviousFocus()
                            activeCategory = position
                            favouritesFilterCategoryAdapter.requestFocus(activeCategory)
                        }
                    }
                    selectedItemTimer!!.start()
                }


                override fun onKeyLeft(currentPosition: Int): Boolean {
                    favouritesFilterCategoryAdapter.clearPreviousFocus()
                    if (currentPosition == 0) {
                        selectedItemTimer?.cancel()
                        selectedItemTimer = null
                        addButton!!.requestFocus()
                        setHintVisibility(false)
                        return true
                    }
                    return false
                }

                override fun onKeyRight(currentPosition: Int): Boolean {
                    if (currentPosition == favouritesFilterCategoryAdapter.itemCount - 1) return true
                    favouritesFilterCategoryAdapter.clearPreviousFocus()
                    return false
                }

                override fun onKeyUp(currentPosition: Int): Boolean {
                    listener.requestFocusOnTopMenu()
                    val holder =
                        favouritesFilterGridView!!.findViewHolderForAdapterPosition(
                            currentPosition
                        ) as FavouriteCategoryExpandedViewHolder
                    favouritesFilterCategoryAdapter.setActiveFilter(holder)
                    setHintVisibility(false)
                    return true
                }

                override fun onKeyDown(currentPosition: Int): Boolean {
                    val holder =
                        favouritesFilterGridView!!.findViewHolderForAdapterPosition(
                            currentPosition
                        ) as FavouriteCategoryExpandedViewHolder
                    favouritesFilterCategoryAdapter.setActiveFilter(holder)
                    dpadDownFromAddButton = false
                    setHintVisibility(false)
                    if (noEventsMsg!!.isVisible) {
                        return true
                    }
                    return false
                }

                override fun onItemClicked(position: Int) {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                    val holder =
                        favouritesFilterGridView!!.findViewHolderForAdapterPosition(
                            position
                        ) as FavouriteCategoryExpandedViewHolder
                    favouritesFilterCategoryAdapter.setActiveFilter(holder)
                    setHintVisibility(false)
                    if (noEventsMsg!!.isVisible) {
                        return
                    }
                    requestFocusOnGrid(0)

                }

                override fun onItemLongClicked(position: Int) {
                    if (noEventsMsg!!.isVisible) {
                        return
                    }

                    if (activeCategory == position) {
                        val holder =
                            favouritesFilterGridView!!.findViewHolderForAdapterPosition(
                                position
                            ) as FavouriteCategoryExpandedViewHolder
                        favouritesFilterCategoryAdapter.setActiveFilter(holder)
                        setHintVisibility(false)
                        return
                    }
                    activeCategory = position
                    favouritesChannelsAdapter.focusedItemPosition = -1
                    favouritesChannelsGridView!!.visibility = View.INVISIBLE
                    favouritesChannelsAdapter.refreshSelectedCategory(categories[position])

                    favouritesChannelsGridView!!.postDelayed({
                        favouritesChannelsGridView!!.visibility = View.VISIBLE
                    }, 1000)
                }

                override fun onDeleteButtonClicked(listName: String) {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                    listener.onDeleteButtonClicked(listName)
                }

                override fun onBackPressed(position: Int): Boolean {
                    listener.requestFocusOnTopMenu()
                    val holder =
                        favouritesFilterGridView!!.findViewHolderForAdapterPosition(
                            position
                        ) as FavouriteCategoryExpandedViewHolder
                    favouritesFilterCategoryAdapter.setActiveFilter(holder)
                    setHintVisibility(false)
                    return true
                }

                override fun onRenameButtonClicked(listName: String) {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                    listener.onRenameButtonClicked(listName)
                }

            }
    }

    override fun dispose() {
        super.dispose()
        selectedItemTimer?.cancel()
        selectedItemTimer = null
    }

    override fun refresh(data: Any) {
        super.refresh(data)
        addButton!!.imageTintList =
            ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        if (data is ArrayList<*>) {
            if (data.isNotEmpty()) {
                if (data[0] is TvChannel) {
                    channels.clear()
                    data.forEach { item ->
                        channels.add(item as TvChannel)
                    }

                    favouritesChannelsAdapter.refresh(channels)
                } else if (data[0] is String) {
                    categories.clear()
                    data.forEach { item ->
                        if (!categories.contains(item as String)) {
                            categories.add(item)
                        }
                    }
                    favouritesChannelsAdapter.refreshSelectedCategory(categories[activeCategory])
                    favouritesFilterCategoryAdapter.refresh(categories)
                }
            }
        } else {
            noEventsMsg!!.visibility = View.VISIBLE
            favouritesFilterCategoryAdapter.shoudKeepFocusOnClick = true
        }
    }

    //handle if channels is added/removed from favourites
    fun refreshHearts() {
        favouritesChannelsAdapter.refreshFocusedItem()
    }

    // Refresh favorites categories on add/remove/rename
    fun refreshCategories(list: ArrayList<String>) {
        categories.clear()
        list.forEach { item ->
            categories.add(item as String)
        }

        favouritesFilterCategoryAdapter.refresh(categories)
        if (activeCategory >= categories.size) {
            activeCategory = categories.size - 1
        }

        if (activeCategory >= 0 && activeCategory < categories.size) {
            favouritesChannelsAdapter.refreshSelectedCategory(categories[activeCategory])
        }
        favouritesChannelsGridView!!.postDelayed(
            Runnable { favouritesChannelsGridView!!.clearFocus() },
            100
        )
        refreshFocusOnCategories()
    }

    fun refreshFocusOnCategories() {
        if (!addButton!!.hasFocus()) {
            favouritesFilterGridView!!.post {
                favouritesFilterCategoryAdapter.selectedItem = activeCategory
                favouritesFilterGridView!!.findViewHolderForAdapterPosition(activeCategory)!!.itemView.requestFocus()
            }
        } else {
            favouritesFilterGridView!!.layoutManager!!.findViewByPosition(0)!!.let {
                it.clearFocus()
            }
            favouritesFilterCategoryAdapter.clearPreviousFocus()
        }
    }

    fun requestFocusOnGrid(position: Int) {
        //request focus on grid view
        if (favouritesChannelsGridView != null && favouritesChannelsGridView!!.getChildAt(position) != null) {
            favouritesChannelsGridView!!.post {
                favouritesChannelsGridView!!.smoothScrollToPosition(position)
                favouritesChannelsGridView!!.getChildAt(position).requestFocus()
            }
        }
    }

    fun selectedFilterList() {
        if (favouritesFilterCategoryAdapter.selectedItem != -1) {
            favouritesFilterCategoryAdapter.clearPreviousFocus()
        }
        if (favouritesFilterGridView?.layoutManager!!.findViewByPosition(
                favouritesFilterCategoryAdapter.selectedItem
            ) != null
        ) {
            favouritesFilterGridView?.layoutManager!!.findViewByPosition(
                favouritesFilterCategoryAdapter.selectedItem
            )!!.requestFocus()
        } else if (favouritesFilterGridView?.layoutManager!!.findViewByPosition(0) != null) {
            favouritesFilterGridView?.layoutManager!!.findViewByPosition(0)!!.requestFocus()

        }

    }


    fun saveFocus(saveFocus: Boolean) {
        favouritesChannelsAdapter.shouldKeepFocus = saveFocus
        favouritesFilterCategoryAdapter.keepFocus = saveFocus
    }

    fun setFocusForCategory() {
        if (favouritesFilterCategoryAdapter.selectedItem == -1) favouritesFilterCategoryAdapter.selectedItem =
            0
        ReferenceApplication.runOnUiThread(Runnable {
            if (favouritesFilterGridView!!.getChildAt(0) != null) {
                favouritesFilterGridView!!.post {
                    favouritesFilterGridView!!.layoutManager!!.findViewByPosition(
                        favouritesFilterCategoryAdapter.selectedItem
                    )?.requestFocus()
                }
            }
        })
    }

    /*
    * To set visibility of long press hint message
    * */
    private fun setHintVisibility(isVisible: Boolean){
        if(isVisible){
            hintMessageText!!.visibility = View.VISIBLE
        }else{
            hintMessageText!!.visibility = View.GONE
            hintMessageText!!.isSelected = false
        }
    }

    interface FavouritesWidgetListener : GWidgetListener {
        fun requestFocusOnTopMenu()
        fun onChannelClicked(tvChannel: TvChannel, favListIds: ArrayList<String>)
        fun onAddButtonClicked()
        fun onRenameButtonClicked(listName: String)
        fun onDeleteButtonClicked(listName: String)
        fun changeFilterToAll(category: String)
    }
}