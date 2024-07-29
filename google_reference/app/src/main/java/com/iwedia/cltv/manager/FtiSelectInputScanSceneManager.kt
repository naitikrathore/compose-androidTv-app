package com.iwedia.cltv.manager

import android.content.Intent
import android.media.tv.TvInputInfo
import android.os.Handler
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.TvInputInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.scan_activity.IwediaSetupActivity
import com.iwedia.cltv.scene.fti.selectInput.FtiSelectInputScanScene
import com.iwedia.cltv.scene.fti.selectInput.FtiSelectInputScanSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager
import world.SceneData
import world.SceneManager

/**
 * Fti selected input scan scene maanager
 *
 * @author Aleksandar Lazic
 */
class FtiSelectInputScanSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    val tvInputModule: TvInputInterface,
    val playerModule: PlayerInterface
) : GAndroidSceneManager(
    context,
    worldHandler, ReferenceWorldHandler.SceneId.FTI_SELECT_INPUT_SCAN
), FtiSelectInputScanSceneListener {

    init {
        isScreenFlowSecured = false
    }

    override fun createScene() {
        scene = FtiSelectInputScanScene(context!!, this)
    }

    override fun onSceneInitialized() {}

    override fun onInputSelected(input: TvInputInfo) {

        val setupIntent = input.createSetupIntent()
        val component = setupIntent!!.component

        if (component!!.packageName.contains("com.google.android.tv.dtvinput") || component!!.packageName.contains(
                "com.mediatek.tvinput"
            )
        ) {

            val intent = Intent(
                ReferenceApplication.get().applicationContext,
                com.iwedia.cltv.scan_activity.IwediaSetupActivity::class.java
            )
            ReferenceApplication.get().activity!!.startActivityForResult(
                intent,
                1000
            )
            ReferenceApplication.worldHandler!!.triggerAction(
                ReferenceWorldHandler.SceneId.FTI_SELECT_INPUT_SCAN,
                Action.DESTROY
            )

        } else {
            tvInputModule!!.startSetupActivity(input, object : IAsyncCallback {
                override fun onFailed(error: Error) {
                }


                override fun onSuccess() {
                    Handler().postDelayed(Runnable {
                        tvInputModule!!.getChannelCountForInput(
                            input,
                            object : IAsyncDataCallback<Int> {
                                override fun onFailed(error: Error) {

                                }

                                override fun onReceive(data: Int) {
                                    ReferenceApplication.worldHandler!!.triggerAction(
                                        id,
                                        Action.HIDE
                                    )
                                    val sceneData = SceneData(id, instanceId, input, data)

                                    ReferenceApplication.runOnUiThread(Runnable {
                                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                                            ReferenceWorldHandler.SceneId.FTI_FINISH_SCAN,
                                            Action.SHOW, sceneData
                                        )
                                    })
                                }
                            })
                    }, 100)

                }
            })
        }
    }

    override fun requestInputs() {
        tvInputModule!!.getTvInputFilteredList("com.google.android.tv.dtvinput", object :
            IAsyncDataCallback<ArrayList<TvInputInfo>> {
            override fun onFailed(error: Error) {

            }

            override fun onReceive(data: ArrayList<TvInputInfo>) {
                scene!!.refresh(data)
            }
        })
    }

    override fun onBackPressed(): Boolean {
        if (data != null) {
            worldHandler!!.triggerAction(id, Action.DESTROY)
            worldHandler!!.triggerAction(
                data!!.previousSceneId,
                SceneManager.Action.SHOW
            )
            playerModule.resume()
            return true
        } else {
            return super.onBackPressed()
        }
    }
}
