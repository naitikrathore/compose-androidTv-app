package com.iwedia.cltv.scene.ci_popup

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.components.MmiMenuWidget
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener

class MmiMenuScene (context: Context, var mmiSceneListener: MmiMenuSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.MMI_MENU_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.MMI_MENU_SCENE),
    mmiSceneListener
)  {

    /**
     * Scene container
     */
    var sceneContainer: ConstraintLayout? = null
    /**
     * Widget
     */
    var widget: MmiMenuWidget? = null

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_mmi_main_menu, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {
                println("####### onCreated CiEncryptedScene")

                if (view != null) {
                    sceneContainer = view!!.findViewById(R.id.scene_container)
                    widget = MmiMenuWidget(context, object :
                        MmiMenuWidget.MmiMenuWidgetListener {
                        override fun onSelectMenuItem(position: Int) {
                            mmiSceneListener.onSelectMenuItem(position)
                        }

                        override fun onCancelCurrentMenu() {
                            mmiSceneListener.onCancelCurrentMenu()
                        }

                        override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                            return true
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

                    // camInfoWidget!!.refresh(list)
                    widget!!.requestFocus()
                    //(sceneListener as CiPopupSceneListener).getCamInfoModuleInfoData()
                    widget!!.setFocusToGrid()

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
        println("MMiM MmiMenuScene refresh")
        super.refresh(data)
        if (data != null) {
            widget?.refresh(data)
        }
    }
}