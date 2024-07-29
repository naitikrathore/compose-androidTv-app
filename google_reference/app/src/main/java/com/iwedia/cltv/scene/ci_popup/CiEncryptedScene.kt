package com.iwedia.cltv.scene.ci_popup

import android.content.Context
import android.os.Build
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.components.CiEncryptedWidget
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener

class CiEncryptedScene(context: Context, sceneListener: CiEncryptedSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.CI_ENCRYPTED_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.CI_ENCRYPTED_SCENE),
    sceneListener
)  {

    /**
     * Scene container
     */
    var sceneContainer: ConstraintLayout? = null
    /**
     * Widget
     */
    var widget: CiEncryptedWidget? = null

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_scene_ci_encrypted, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {
                println("####### onCreated CiEncryptedScene")

                if (view != null) {
                    sceneContainer = view!!.findViewById(R.id.scene_container)
                    widget = CiEncryptedWidget(context, object :
                        CiEncryptedWidget.CiEncryptedWidgetListener {
                        override fun enquiryAnswer(abort: Boolean, answer: String) {
                            (sceneListener as CiEncryptedSceneListener).enquiryAnswer(abort, answer)
                        }

                        override fun onNextChannel() {
                            (sceneListener as CiEncryptedSceneListener).onNextChannel()
                        }

                        override fun onPreviousChannel() {
                            (sceneListener as CiEncryptedSceneListener).onPreviousChannel()
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

    @RequiresApi(Build.VERSION_CODES.R)
    override fun refresh(data: Any?) {
        println("CiEncryptedScene refresh")
        super.refresh(data)
        if (data != null) {
            widget?.refresh(data)
        }
    }

}