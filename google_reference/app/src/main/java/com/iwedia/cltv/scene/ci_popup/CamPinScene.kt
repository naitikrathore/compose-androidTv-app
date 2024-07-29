package com.iwedia.cltv.scene.ci_popup

import android.content.Context
import android.view.KeyEvent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.components.CamPinWidget
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener

class CamPinScene (context: Context, sceneListener: CamPinSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.LIVE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.CAM_PIN_SCENE),
    sceneListener
)  {

    /**
     * Scene container
     */
    var sceneContainer: ConstraintLayout? = null
    /**
     * Widget
     */
    var widget: CamPinWidget? = null

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_cam_pin_scene, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {
                println("####### onCreated CiEncryptedScene")

                if (view != null) {
                    sceneContainer = view!!.findViewById(R.id.scene_container)
                    widget = CamPinWidget(context, object :
                        CamPinWidget.CamPinWidgetListener {
                        override fun setCamPin(pin: String) {
                            (sceneListener as CamPinSceneListener).setCamPin(pin)
                        }

                        override fun onBackPress() {
                            sceneListener.onBackPressed()
                        }

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            (sceneListener as CamPinSceneListener).setSpeechText(text = text, importance = importance)
                        }

                        override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                            return false
                        }
                    })

                    sceneContainer!!.addView(widget!!.view)

                    val view = widget!!.view
                    val constraints = ConstraintSet()
                    constraints.connect(
                        view!!.id,
                        ConstraintSet.LEFT,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.LEFT
                    );
                    constraints.connect(
                        view.id,
                        ConstraintSet.RIGHT,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.RIGHT
                    );
                    constraints.connect(
                        view.id,
                        ConstraintSet.TOP,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.TOP
                    );
                    constraints.connect(
                        view.id,
                        ConstraintSet.BOTTOM,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.BOTTOM
                    );
                    constraints.applyTo(sceneContainer)

                    sceneListener.onSceneInitialized()
                }
            }
        })
    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        return super.dispatchKeyEvent(keyCode, keyEvent)
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {

    }

}