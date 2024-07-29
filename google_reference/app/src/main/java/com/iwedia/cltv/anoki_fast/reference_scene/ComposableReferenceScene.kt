package com.iwedia.cltv.anoki_fast.reference_scene

import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import com.iwedia.cltv.R
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import tv.anoki.components.theme.BackgroundColor
import world.SceneListener


/**
 * [ComposableReferenceScene] is an abstract class that extends ReferenceScene and serves as a wrapper for
 * integrating composable views within a scene.
 *
 * @param context the context in which the scene operates
 * @param sceneId the unique identifier for the scene
 * @param name the name of the scene
 * @param sceneListener the listener for scene events
 */
abstract class ComposableReferenceScene(
    context: Context,
    sceneId: Int,
    name: String,
    sceneListener: SceneListener
): ReferenceScene(
    context, sceneId, name, sceneListener
) {


    /**
     * A ComposeView container used to hold the composable content.
     */
    protected lateinit var container: ComposeView


    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name,
            R.layout.composable_container,
            object : GAndroidSceneFragmentListener {
                override fun onCreated() {
                    setRefs()
                    sceneListener.onSceneInitialized()
                }
            }
        )
    }

    /**
     * Sets up references for the ComposeView container and applies the background color.
     */
    open fun setRefs() {
        container = view!!.findViewById(R.id.compose_view)
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
        TODO("Not yet implemented")
    }
}