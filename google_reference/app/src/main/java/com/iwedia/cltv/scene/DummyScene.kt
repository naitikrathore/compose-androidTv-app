package com.iwedia.cltv.scene

import android.content.Context
import android.graphics.Color
import android.widget.RelativeLayout
import android.widget.TextView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigHandler
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import world.SceneListener

class DummyScene(context: Context, sceneListener: SceneListener) :
    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.DUMMY,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.DUMMY),
        sceneListener
    ) {

    var testTextView: TextView? = null
    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_dummy_scene, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {

                testTextView = view!!.findViewById(R.id.text_test_1)
                testTextView!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                parseConfig(configParam!!)
            }
        })
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
    }

}