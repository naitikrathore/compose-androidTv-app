package com.iwedia.cltv.scene.favourite

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.components.ButtonType
import com.iwedia.cltv.components.HorizontalButtonsAdapter
import com.iwedia.cltv.components.MultiCheckListAdapter
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener

/**
 * A reference scene class to display Favorite list
 * Launched on Press of Favorite/PAGE_DOWN button from RC
 * Used to add channel in favorite list.
 */
class FavoriteScene(context: Context, sceneListener: FavoriteSceneListener) : ReferenceScene(
    context, ReferenceWorldHandler.SceneId.FAVOURITE_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.FAVOURITE_SCENE),
    sceneListener
) {

    private var favoritesOverlay: ConstraintLayout? = null
    private var favoritesGridView: VerticalGridView? = null
    private lateinit var favoritesListAdapter: MultiCheckListAdapter
    private var buttonsList = mutableListOf<ButtonType>()
    private var mData: Any? = null
    private var favoriteButtonPosition: Int? = null
    private var selectedFavListItems = ArrayList<String>()
    private var detailsButtonAdapter: HorizontalButtonsAdapter? = null


    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_scene_favorite, object :
            GAndroidSceneFragmentListener {
            override fun onCreated() {
                val colorStart = Color.parseColor(
                    ConfigColorManager.getColor("color_background")
                        .replace("#", ConfigColorManager.alfa_zero_per)
                )
                val colorMid = Color.parseColor(
                    ConfigColorManager.getColor("color_background")
                        .replace("#", ConfigColorManager.alfa_fifty_per)
                )
                val colorEnd = Color.parseColor(
                    ConfigColorManager.getColor("color_background")
                        .replace("#", ConfigColorManager.alfa_hundred_per)
                )
                favoritesOverlay = view!!.findViewById(R.id.favorites_overlay)
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.RECTANGLE
                drawable.orientation = GradientDrawable.Orientation.LEFT_RIGHT
                drawable.colors = intArrayOf(colorStart, colorMid, colorEnd)
                favoritesOverlay!!.background = drawable
                favoritesGridView = view!!.findViewById(R.id.favorites_overlay_grid_view)
                initFavoritesOverlay()
                sceneListener.onSceneInitialized()
            }

        })
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {}

    override fun refresh(data: Any?) {
        if (data is TvChannel) {
            mData = data
            showFavoritesOverlay()
        }
        super.refresh(data)
    }

    /**
     * Favorites overlay initialization
     */
    private fun initFavoritesOverlay() {
        val favoriteContainerTitle = view!!.findViewById<TextView>(R.id.favorites_overlay_title)
        favoriteContainerTitle.text = ConfigStringsManager.getStringById("add_to")
        favoriteContainerTitle.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        favoriteContainerTitle.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )

        Utils.makeGradient(
            view = view!!.findViewById(R.id.favorites_gradient_view),
            type = GradientDrawable.LINEAR_GRADIENT,
            orientation = GradientDrawable.Orientation.RIGHT_LEFT,
            startColor = Color.parseColor(
                ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_97)
            ),
            endColor = Color.TRANSPARENT,
            centerX = 0.8f,
            centerY = 0f
        )
        Utils.makeGradient(
            view = view!!.findViewById(R.id.favorites_linear_layout),
            type = GradientDrawable.LINEAR_GRADIENT,
            orientation = GradientDrawable.Orientation.RIGHT_LEFT,
            startColor = Color.parseColor(ConfigColorManager.getColor("color_dark")),
            endColor = Color.parseColor(
                ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_97)
            ),
            centerX = 0.8f,
            centerY = 0f
        )

        favoritesListAdapter = MultiCheckListAdapter()
        (sceneListener as FavoriteSceneListener).getFavoritesCategories(object :
            IAsyncDataCallback<ArrayList<String>> {
            override fun onFailed(error: Error) {}

            override fun onReceive(data: ArrayList<String>) {
                favoritesListAdapter.refresh(data)
            }
        })

        favoritesGridView!!.selectedPosition = 0
        favoritesGridView!!.preserveFocusAfterLayout = true
        favoritesGridView!!.setNumColumns(1)
        favoritesGridView!!.adapter = favoritesListAdapter
        favoritesGridView!!.setItemSpacing(Utils.getDimensInPixelSize(R.dimen.custom_dim_5))

        favoritesListAdapter.adapterListener =
            object : MultiCheckListAdapter.MultiCheckListAdapterListener {
                @RequiresApi(Build.VERSION_CODES.R)
                override fun onItemClicked(button: String, callback: IAsyncCallback) {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()
                    callback.onSuccess()
                }

                override fun onKeyUp(position: Int): Boolean {
                    return false
                }

                override fun onKeyDown(position: Int): Boolean {
                    return false
                }

                override fun onKeyRight(position: Int): Boolean {
                    return true
                }

                override fun onKeyLeft(position: Int): Boolean {
                    return true
                }

                override fun onBackPressed(position: Int): Boolean {
                    selectedFavListItems.clear()
                    selectedFavListItems.addAll(favoritesListAdapter.getSelectedItems())
                    if (mData != null && mData is TvChannel) {
                        val channel = mData as TvChannel
                        (sceneListener as FavoriteSceneListener).onFavoriteButtonPressed(
                            channel,
                            selectedFavListItems
                        )
                    }
                    (sceneListener as FavoriteSceneListener).onBackPressed()
                    return true
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    (sceneListener as FavoriteSceneListener).setSpeechText(text = text, importance = importance)
                }

            }
    }

    /**
     * Shows favorites overlay
     */
    private fun showFavoritesOverlay() {
        favoritesOverlay?.bringToFront()
        favoritesOverlay?.elevation = 10f
        favoritesOverlay?.visibility = View.VISIBLE
        if (mData != null && mData is TvChannel) {
            val channel = mData as TvChannel
            val selectedItemsList =
                (sceneListener as FavoriteSceneListener).getFavoriteItemList(channel)
            (favoritesGridView?.adapter as MultiCheckListAdapter).setSelectedItems(selectedItemsList)
        }
        favoritesGridView?.postDelayed({
            val view = favoritesGridView?.layoutManager?.findViewByPosition(0)
            if (view != null) {
                view.clearFocus()
                view.requestFocus()
            }
        }, 200)
        buttonsList.forEachIndexed { index, type ->
            if (type == ButtonType.ADD_TO_FAVORITES || type == ButtonType.EDIT_FAVORITES) {
                favoriteButtonPosition = index
            }
        }
    }

    fun refreshFavoriteButton() {
        if (mData != null && (mData is TvChannel || mData is Recording)) {
            val tvChannel =
                if (mData is TvChannel) mData as TvChannel else (mData as Recording).tvChannel
            val favListItems = tvChannel?.let {
                (sceneListener as FavoriteSceneListener).getFavoriteItemList(
                    it
                )
            }

            var position = -1
            for (index in 0 until buttonsList.size) {
                val button = buttonsList[index]
                if (button == ButtonType.ADD_TO_FAVORITES || button == ButtonType.EDIT_FAVORITES) {
                    if (favListItems != null) {
                        if (favListItems.isEmpty()) {
                            buttonsList[index] = ButtonType.ADD_TO_FAVORITES
                        } else {
                            buttonsList[index] = ButtonType.EDIT_FAVORITES
                        }
                    }
                    position = index
                    break
                }
            }
            if (position != -1) {
                detailsButtonAdapter!!.updateButton(position, buttonsList[position])
            }
        }
    }

}