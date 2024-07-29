package com.iwedia.cltv.scene.search

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.TextView.OnEditorActionListener
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.*
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.foryou.RailItem
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.scene.home_scene.rail.AdapterListener
import com.iwedia.cltv.scene.home_scene.rail.RailAdapter
import com.iwedia.cltv.utils.LoadingPlaceholder
import com.iwedia.cltv.utils.PlaceholderName
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import tv.anoki.ondemand.domain.model.VODItem
import tv.anoki.ondemand.domain.model.VODType


/**
 * Search scene
 *
 * @author Aleksandar Lazic
 */
class SearchScene(context: Context, sceneListener: SearchSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.SEARCH,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.SEARCH),
    sceneListener
) {
    val TAG = javaClass.simpleName
    var searchEditText: EditText? = null
    var verticalGridView: VerticalGridView? = null

    var railAdapter: RailAdapter? = null
    var railsForAdapter: MutableList<RailItem> = mutableListOf()
    var searchLayout: LinearLayout? = null
    var searchIcon: ImageView? = null
    var noResultsFound: TextView? = null
    var searchEditTextContainer: LinearLayout? = null

    var backgroundLayout: ConstraintLayout? = null

    private var composableContainerConstraintLayout: ComposeView? = null
    private var nonComposableContainerConstraintLayout: ConstraintLayout? = null

    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(
            "Search",
            R.layout.layout_scene_search,
            object : GAndroidSceneFragmentListener {
                override fun onCreated() {
                    setupRefs()
//                    initializeLoadingPlaceholders()
                    setupAdapter()
                    init()
                }
            })
    }

//    private fun initializeLoadingPlaceholders() {
//        LoadingPlaceholder.hideAllRegisteredLoadingPlaceholders()
//        LoadingPlaceholder(
//            context = context,
//            placeholderViewId = R.layout.loading_layout_rail_main,
//            parentConstraintLayout = backgroundLayout!!,
//            name = PlaceholderName.SEARCH_SCENE
//        )
//    }

    override fun refresh(data: Any?) {

        if (Utils.isDataType(data, MutableList::class.java)) {
            railsForAdapter.clear()
            railsForAdapter.addAll(data as Collection<RailItem>)

            //sort rails
            railsForAdapter.sortedWith(compareBy { it.id })

            ReferenceApplication.runOnUiThread {
                railAdapter!!.refresh(railsForAdapter as ArrayList<RailItem>)
            }

            if (railsForAdapter.isEmpty()) {
                searchEditText!!.requestFocus()
                noResultsFound!!.visibility = View.VISIBLE
            } else {
                noResultsFound!!.visibility = View.GONE
            }

//            LoadingPlaceholder.hideLoadingPlaceholder(PlaceholderName.SEARCH_SCENE, onHidden = {
            verticalGridView!!.visibility = View.VISIBLE
//            })
        }

        super.refresh(data)
    }

    private fun setupAdapter() {
        railAdapter = RailAdapter((sceneListener as SearchSceneListener).getDateTimeFormat())

        railAdapter!!.setListener(object : AdapterListener {
            override fun onBroadcastChannelSelected(tvChannel: TvChannel?, railItem: RailItem) {

            }
            override fun onEventSelected(
                tvEvent: TvEvent?,
                vodItem: VODItem?,
                parentalRatingDisplayName: String,
                railItem: RailItem,
                currentTime: Long,
                isStillFocused: () -> Boolean
            ) {
            }

            override fun getCurrentTime(tvChannel: TvChannel): Long {
                return (sceneListener as SearchSceneListener).getCurrent(tvChannel)
            }

            override fun onScrollDown(position: Int) {
                verticalGridView?.smoothScrollToPosition(position)
                verticalGridView?.layoutManager?.findViewByPosition(position)?.requestFocus()
            }

            override fun onScrollUp(position: Int) {
                verticalGridView?.smoothScrollToPosition(position)
                verticalGridView?.layoutManager?.findViewByPosition(position)?.requestFocus()
            }

            override fun onKeyUp(onKeyUpFinished: () -> Unit) {
                searchEditText!!.requestFocus()
                onKeyUpFinished.invoke() // crucial for collapsing the card
            }

            override fun onItemClicked(item: Any) {
                if (item is VODItem) { // display Composable container with VodDetails
                    (sceneListener as SearchSceneListener).onVodItemClicked(item.type, item.contentId)
                } else {
                    (sceneListener as SearchSceneListener).onTvEventClicked(item)
                }
            }

            override fun isChannelLocked(channelId: Int): Boolean {
                return (sceneListener as SearchSceneListener).isChannelLocked(channelId)
            }

            override fun isParentalControlsEnabled(): Boolean {
                return (sceneListener as SearchSceneListener).isParentalControlsEnabled()
            }

            override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
                return (sceneListener as SearchSceneListener).getParentalRatingDisplayName(parentalRating, tvEvent)
            }

            override fun isInWatchlist(tvEvent: TvEvent): Boolean {
                return (sceneListener as SearchSceneListener).isInWatchlist(tvEvent)
            }

            override fun isEventLocked(tvEvent: TvEvent?) = (sceneListener as SearchSceneListener).isEventLocked(tvEvent)

            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                (sceneListener as SearchSceneListener).setSpeechText(text = text, importance = importance)
            }

            override fun isInRecList(tvEvent: TvEvent): Boolean {
                return (sceneListener as SearchSceneListener).isInRecList(tvEvent)
            }

            override fun isRegionSupported(): Boolean {
                return true
            }
        })

        verticalGridView!!.setNumColumns(1)
        verticalGridView!!.adapter = railAdapter

        //make focus fixed on th top side of the screen
        verticalGridView!!.itemAlignmentOffset =
            context.resources.getDimensionPixelSize(R.dimen.custom_dim_0)
        verticalGridView!!.itemAlignmentOffsetPercent =
            VerticalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED
        verticalGridView!!.windowAlignmentOffset = 0
        verticalGridView!!.clipToOutline = true // used to disable item inside recyclerview to go out of recyclerview's bounds.
        verticalGridView!!.windowAlignmentOffsetPercent =
            VerticalGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
        verticalGridView!!.windowAlignment = VerticalGridView.WINDOW_ALIGN_NO_EDGE
    }

    private fun setupRefs() {
        composableContainerConstraintLayout = view!!.findViewById(R.id.composable_container_constraint_layout)
        backgroundLayout = view!!.findViewById(R.id.background_layout)
        nonComposableContainerConstraintLayout = view!!.findViewById(R.id.non_composable_container_constraint_layout)
        backgroundLayout!!.setBackgroundColor(
            Color.parseColor(
                ConfigColorManager.getColor("color_background")
                    .replace("#", ConfigColorManager.alfa_light_bg)
            )
        )
        verticalGridView = view!!.findViewById(R.id.search_recycler)
        searchEditText = view!!.findViewById(R.id.search_edit_text)
        searchEditText!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_background")))
        searchEditText!!.hint = ConfigStringsManager.getStringById("search_hint")
        searchEditText!!.inputType = InputType.TYPE_CLASS_TEXT
        searchEditText!!.imeOptions = EditorInfo.IME_ACTION_DONE
        searchLayout = view!!.findViewById(R.id.search_layout)
        searchLayout!!.background = ConfigColorManager.generateButtonBackground()
        searchIcon = view!!.findViewById(R.id.search_icon)
        searchIcon!!.setImageDrawable(ContextCompat.getDrawable(
            ReferenceApplication.applicationContext(),
            R.drawable.search_icon
        ))

        noResultsFound = view!!.findViewById(R.id.no_results_msg)
        noResultsFound!!.setText(ConfigStringsManager.getStringById("no_results_found"))
        noResultsFound!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )
        noResultsFound!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        searchEditTextContainer = view!!.findViewById(R.id.edittext_container)

        //font
        searchEditText!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
    }

    //Show keyboard input
    private fun showKeyboard() {
        val imm = ReferenceApplication.get()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun init() {
        searchEditText!!.requestFocus()

        showKeyboard()
        searchEditText!!.setOnClickListener {
            //this method is called to restart inactivity timer for no signal power off
            (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

            showKeyboard()
        }

        // Search done button clicked
        searchEditText!!.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                //check duration
                if (searchEditText!!.text.toString().isBlank()) {
                    refresh(-1)
                    return@OnEditorActionListener true
                }
//                LoadingPlaceholder.showLoadingPlaceholder(
//                    PlaceholderName.SEARCH_SCENE,
//                    onShown = {
                //Hide keyboard
                val imm = ReferenceApplication.get()
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchEditText!!.windowToken, 0)
                return@OnEditorActionListener true
            }
            false
        })

        searchEditText!!.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                (sceneListener as SearchSceneListener).stopSpeech()
                searchLayout!!.background = ConfigColorManager.generateButtonBackground()
                try {
                    val color_context =
                        Color.parseColor(ConfigColorManager.getColor("color_background"))
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "init: Exception color_context $color_context")
                    searchEditText!!.setTextColor(color_context)
                } catch (ex: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "init: Exception color rdb $ex")
                }

                searchIcon!!.setColorFilter(
                    Color.parseColor(
                        ConfigColorManager.getColor(
                            "color_background"
                        )
                    )
                )
            } else {
                searchLayout!!.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_search_bar_rounded)
                try {
                    val color_context =
                        Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "init: Exception color_context $color_context")
                    searchEditText!!.setTextColor(color_context)
                } catch (ex: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "init: Exception color rdb $ex")
                }

                searchIcon!!.setColorFilter(
                    Color.parseColor(
                        ConfigColorManager.getColor(
                            "color_main_text"
                        )
                    )
                )
            }
        }

        searchEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //this method is called to restart inactivity timer for no signal power off
                (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                if (s.isNullOrBlank()) {
                    // If the search query is empty, clear the results
                    refresh(-1)
                    verticalGridView!!.visibility = View.INVISIBLE
                    noResultsFound!!.visibility = View.GONE
                } else {
                    // Perform search without hiding the keyboard
                    (sceneListener as SearchSceneListener).onSearchQuery(s.toString())
                }
            }

            override fun afterTextChanged(p0: Editable?) {}
        })


    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (railsForAdapter.isEmpty() || noResultsFound!!.visibility == View.VISIBLE) {
                    return true
                }
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (searchEditText!!.hasFocus()) {
                    return true
                }
            }
        } else if ((keyEvent).action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                if (!searchEditText!!.hasFocus()) {
                    searchEditText!!.requestFocus()
                    return true
                }
            }
        }

        return super.dispatchKeyEvent(keyCode, keyEvent)
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {

    }

    override fun onDestroy() {
        val imm = ReferenceApplication.get()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText!!.windowToken, 0)
        super.onDestroy()
    }
}