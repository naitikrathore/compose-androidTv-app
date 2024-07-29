package com.iwedia.cltv.anoki_fast.vod.player

import android.content.Context
import androidx.hilt.navigation.compose.hiltViewModel
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.anoki_fast.reference_scene.ComposableReferenceScene
import tv.anoki.ondemand.domain.model.VODItem
import tv.anoki.ondemand.presentation.player.VideoPlayerScreen
import tv.anoki.ondemand.presentation.player.VideoPlayerScreenViewModel

/**
 * Scene class for displaying a VOD (Video On Demand) banner.
 *
 * @param context The context in which the scene is created.
 * @param sceneListener The listener for scene events.
 */
class VodBannerScene(context: Context, sceneListener: VodBannerSceneListener) :
    ComposableReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.VOD_BANNER_SCENE,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.VOD_BANNER_SCENE),
        sceneListener
    ) {

    private var videoViewModel: VideoPlayerScreenViewModel? = null

    /**
     * Refreshes the scene with new data.
     *
     * @param data The new data to refresh the scene with. Expected to be a VODItem.
     */
    override fun refresh(data: Any?) {
        when (data) {
            is VODItem -> {
                container?.let {
                    it.setContent {

                        videoViewModel = hiltViewModel()

                        videoViewModel!!.setData(
                            videoTitle = data.title,
                            videoDescription = data.description,
                            origRating = data.origRating
                        )

                        VideoPlayerScreen(
                            videoPlayerScreenViewModel = videoViewModel!!,
                            onBackPressed = (sceneListener as VodBannerSceneListener)::onBackClicked,
                            onClickPlay = (sceneListener as VodBannerSceneListener)::onPlayClicked,
                            onClickPause = (sceneListener as VodBannerSceneListener)::onPauseClicked,
                            onSeek = (sceneListener as VodBannerSceneListener)::onSeek,
                        )
                    }
                }
            }
        }
    }

    /**
     * Sets the state of the video player.
     *
     * @param isPlaying Whether the video is currently playing.
     * @param isError Whether there is an error in the video player.
     */
    fun setState(isPlaying: Boolean, isError: Boolean) {
        videoViewModel?.setState(isPlaying = isPlaying, isError = isError)
    }

    /**
     * Sets whether the video is playing.
     *
     * @param isPlaying Whether the video is currently playing.
     */
    fun setPlaying(isPlaying: Boolean) {
        videoViewModel?.setPlaying(isPlaying = isPlaying)
    }

    /**
     * Sets the total duration of the content.
     *
     * @param duration The total duration of the content in milliseconds.
     */
    fun setContentDuration(duration: Long) {
        videoViewModel?.setContentDuration(contentDuration = duration)
    }

    /**
     * Sets the current position of the content.
     *
     * @param contentCurrentPosition The current position of the content in milliseconds.
     */
    fun setContentCurrentPosition(contentCurrentPosition: Long) {
        videoViewModel?.setContentCurrentPosition(contentCurrentPosition = contentCurrentPosition)
    }

}