package com.iwedia.cltv.anoki_fast.vod.details.series

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.toArgb
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.anoki_fast.reference_scene.ComposableReferenceScene
import com.iwedia.cltv.anoki_fast.vod.details.DetailsSceneData
import com.iwedia.cltv.anoki_fast.vod.details.DetailsSceneListener
import tv.anoki.components.theme.BackgroundColor
import tv.anoki.ondemand.constants.StringConstants
import tv.anoki.ondemand.navigation.SeriesNavigation
import tv.anoki.ondemand.presentation.details.series.SeriesDetailsViewModel
import tv.anoki.ondemand.presentation.seasons_and_episode_selection.SeasonsAndEpisodeViewModel

private const val TAG = "SeriesDetailsScene"

class SeriesDetailsScene(context: Context, sceneListener: DetailsSceneListener) :
    ComposableReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.VOD_SERIES_DETAILS_SCENE,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.VOD_SERIES_DETAILS_SCENE),
        sceneListener
    ) {

    private lateinit var contentId: String
    private var seriesDetailsViewModel: SeriesDetailsViewModel? = null
    private var seasonsAndEpisodeViewModel: SeasonsAndEpisodeViewModel? = null
    private var navController: NavHostController? = null

    override fun refresh(data: Any?) {
        when (data) {
            is DetailsSceneData -> { // this is initially passed when this scene is created in onSceneInitialized inside corresponding manager.
                container?.let {
                    it.setContent {
                        seriesDetailsViewModel = hiltViewModel()
                        seasonsAndEpisodeViewModel = hiltViewModel()
                        navController = rememberNavController()
                        SeriesNavigation(
                            seriesDetailsViewModel = seriesDetailsViewModel!!,
                            seasonsAndEpisodeViewModel = seasonsAndEpisodeViewModel!!,
                            navController = navController!!,
                            contentId = data.contentId,
                            onBackPressed = sceneListener::onBackPressed,
                            onNavigateToPlayer = (sceneListener as DetailsSceneListener)::onVodItemClicked
                        )
                    }
                    contentId = data.contentId
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        navController?.let {
            val currentRoute = it.currentDestination?.route
            when (currentRoute) {
                StringConstants.ROUTE_VOD_SERIES_DETAILS -> {
                    seriesDetailsViewModel?.setResumeCalled(isResume = true)
                    Handler(Looper.getMainLooper()).postDelayed({
                        seriesDetailsViewModel?.setResumeCalled(isResume = false)
                    }, 200)
                }
                StringConstants.ROUTE_VOD_SEASONS_AND_EPISODES -> {
                    seasonsAndEpisodeViewModel?.setResumeCalled(isResume = true)
                    Handler(Looper.getMainLooper()).postDelayed({
                        seasonsAndEpisodeViewModel?.setResumeCalled(isResume = false)
                    }, 200)
                }
                else -> {

                }
            }
        }
    }

    override fun setRefs() {
        super.setRefs()
        container?.let {
            it.background = ColorDrawable(BackgroundColor.toArgb())
        }
    }

}