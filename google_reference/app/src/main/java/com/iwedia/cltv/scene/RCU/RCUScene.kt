package com.iwedia.cltv.scene.RCU

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.components.ReferenceWidgetRCU
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener


/**
 * RCU scene
 *
 * @author Nishant Bansal
 */
class RCUScene(context: Context, sceneListener: RCUSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.RCU_SCENE,
    "RCUScene",
    sceneListener
) {
    /** Scene container */
    var sceneContainer: ConstraintLayout? = null

    /**
     * Widget
     */
    var widget: ReferenceWidgetRCU? = null

    /**Is scene paused flag*/
    private var isPaused = false

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(
            name,
            R.layout.layout_min_rcu,
            object : GAndroidSceneFragmentListener{
                @RequiresApi(Build.VERSION_CODES.R)
                override fun onCreated() {
                    sceneContainer = view?.findViewById(R.id.scene_container)
                    widget = ReferenceWidgetRCU(context, object : ReferenceWidgetRCU.RCUWidgetListener{
                        override fun getCurrentTime(): Long {
                            return (sceneListener as RCUSceneListener).getCurrentTime()
                        }

                        override fun timerEnd() {
                            (sceneListener as RCUSceneListener).timerEnd()
                        }

                        override fun okPressed() {
                            (sceneListener as RCUSceneListener).okPressed()
                        }

                        override fun ttxPressed() {
                            (sceneListener as RCUSceneListener).ttxPressed()
                            if((sceneListener as RCUSceneListener).ttxState()) {
                                sceneContainer!!.setBackgroundColor(
                                    Color.parseColor(
                                        ConfigColorManager.getColor("color_background")
                                    )
                                )
                            }else{
                                Utils.makeGradient(
                                    view = view!!.findViewById(R.id.scene_container),
                                    type = GradientDrawable.LINEAR_GRADIENT,
                                    orientation = GradientDrawable.Orientation.RIGHT_LEFT,
                                    firstQuarter = Color.parseColor(ConfigColorManager.getColor("color_dark")),
                                    secondQuarter = Color.parseColor(ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_97)),
                                    thirdQuarter = Color.parseColor(ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_86)),
                                    fourthQuarter = Color.TRANSPARENT,
                                    centerX = 0f,
                                    centerY = 0f
                                )
                            }
                        }

                        override fun redPressed() {
                            return (sceneListener as RCUSceneListener).redPressed()
                        }

                        override fun bluePressed() {
                            return (sceneListener as RCUSceneListener).bluePressed()
                        }

                        override fun yellowPressed() {
                            return (sceneListener as RCUSceneListener).yellowPressed()
                        }

                        override fun backPressed() {
                            return (sceneListener as RCUSceneListener).backPressed()
                        }

                        override fun ttxOn(): Boolean {
                            return (sceneListener as RCUSceneListener).ttxState()
                        }

                        override fun greenPressed() {
                            return (sceneListener as RCUSceneListener).greenPressed()
                        }

                        override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                            return false
                        }

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            (sceneListener as RCUSceneListener).setSpeechText(text = text, importance = importance)
                        }

                        override fun digitPressed(digit: Int) {
                            (sceneListener as RCUSceneListener).digitPressed(digit)
                        }

                        override fun onKey() {
                            (sceneListener as RCUSceneListener).onKey()
                        }


                    })

                    //set scene background gradient
                    Utils.makeGradient(
                        view = view!!.findViewById(R.id.scene_container),
                        type = GradientDrawable.LINEAR_GRADIENT,
                        orientation = GradientDrawable.Orientation.RIGHT_LEFT,
                        firstQuarter = Color.parseColor(ConfigColorManager.getColor("color_dark")),
                        secondQuarter = Color.parseColor(ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_97)),
                        thirdQuarter = Color.parseColor(ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_86)),
                        fourthQuarter = Color.TRANSPARENT,
                        centerX = 0f,
                        centerY = 0f
                    )

                    sceneContainer!!.addView(widget!!.view)
                    parseConfig(configParam)
                    sceneListener.onSceneInitialized()
                }

            })
    }
    override fun onPause() {
        widget?.onPause()
        isPaused = true
    }
    override fun onResume() {
        widget?.onResume()
        if (isPaused) {
            isPaused = false
        }
    }

    override fun refresh(data: Any?) {
        super.refresh(data)
        ReferenceApplication.runOnUiThread{
            widget!!.refresh(data!!)
        }
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {

    }

    override fun onDestroy() {
        super.onDestroy()
        widget!!.dispose()
    }


}