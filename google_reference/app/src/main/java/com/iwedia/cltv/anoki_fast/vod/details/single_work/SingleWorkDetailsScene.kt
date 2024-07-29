package com.iwedia.cltv.anoki_fast.vod.details.single_work

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.toArgb
import androidx.hilt.navigation.compose.hiltViewModel
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.anoki_fast.vod.details.DetailsSceneData
import com.iwedia.cltv.anoki_fast.vod.details.DetailsSceneListener
import com.iwedia.cltv.anoki_fast.reference_scene.ComposableReferenceScene
import tv.anoki.components.theme.BackgroundColor
import tv.anoki.ondemand.navigation.SingleWorkDetailsNavigation
import tv.anoki.ondemand.presentation.details.single_work.SingleWorkDetailsViewModel

private const val TAG = "SingleWorkDetailsScene"

class SingleWorkDetailsScene(context: Context, sceneListener: DetailsSceneListener) :
    ComposableReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.VOD_SINGLE_WORK_DETAILS_SCENE,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.VOD_SINGLE_WORK_DETAILS_SCENE),
        sceneListener
    ) {

    private var viewModel: SingleWorkDetailsViewModel? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun refresh(data: Any?) {
        when (data) {
            is DetailsSceneData -> { // this is initially passed when this scene is created in onSceneInitialized inside corresponding manager.
                container?.let {
                    it.setContent {
                        viewModel = hiltViewModel()
                        SingleWorkDetailsNavigation(
                            viewModel = viewModel!!,
                            contentId = data.contentId,
                            onBackPressed = (sceneListener as DetailsSceneListener)::onBackPressed,
                            onNavigateToPlayer = (sceneListener as DetailsSceneListener)::onVodItemClicked
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel?.setResumeCalled(isResume = true)
        Handler(Looper.getMainLooper()).postDelayed({
            viewModel?.setResumeCalled(isResume = false)
        }, 200)
    }

    override fun setRefs() {
        super.setRefs()
        container?.let {
            it.background = ColorDrawable(BackgroundColor.toArgb())
        }
    }

}