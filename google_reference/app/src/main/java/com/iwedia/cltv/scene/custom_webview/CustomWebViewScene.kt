package com.iwedia.cltv.scene.custom_webview

import android.content.Context
import android.view.ViewGroup
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.components.welcome_screen.CustomWelcomeScreenWebView
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import world.SceneListener

/**
 * CustomWebView Scene
 *
 * @author Thanvandh N
 */
class CustomWebViewScene(context: Context, sceneListener: SceneListener) :
    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.CUSTOM_WEB_VIEW_SCENE,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.CUSTOM_WEB_VIEW_SCENE),
        sceneListener
    ) {

    private var customWelcomeScreenWebView: CustomWelcomeScreenWebView? = null

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_custom_webview_scene, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {
                sceneListener.onSceneInitialized()
            }
        })
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
    }

    override fun refresh(data: Any?) {
        if (data is String) {
            openWebView(data)
        } else {
            super.refresh(data)
        }
    }

    private fun openWebView(url: String?){
        customWelcomeScreenWebView = CustomWelcomeScreenWebView(context, url!!, object : CustomWelcomeScreenWebView.Listener{
            override fun onBackClicked() {
                sceneListener.onBackPressed()
            }
        })
        (view!!.view as ViewGroup).addView(customWelcomeScreenWebView)
        customWelcomeScreenWebView!!.requestFocus()
    }

    override fun onDestroy() {
        customWelcomeScreenWebView?.destroy()
        super.onDestroy()
    }
}