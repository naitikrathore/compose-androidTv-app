package com.iwedia.cltv.manager

import android.content.Intent
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.scene.evaluation_scene.EvaluationScene
import com.iwedia.cltv.scene.evaluation_scene.EvaluationSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager
import utils.information_bus.Event
import com.iwedia.cltv.BuildConfig
import utils.information_bus.events.Events
import com.iwedia.cltv.ReferenceApplication.Companion.runOnUiThread
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import world.SceneData

/**
 * EvaluationScene manager
 */
class EvaluationSceneManager(
    context: MainActivity, worldHandler: ReferenceWorldHandler,
    var utilsModule: UtilsInterface, var tvModule: TvInterface
) :
    GAndroidSceneManager(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.EVALUATION_SCENE
    ), EvaluationSceneListener {

    private val SCAN_ACTION = "com.iwedia.cltv.SCAN"

    init {
        isScreenFlowSecured = false

        registerGenericEventListener(Events.APP_INITIALIZED)
    }

    override fun createScene() {
        scene = EvaluationScene(context!!, this)
    }

    override fun onAccepted() {
        //persist the first run
       utilsModule.setPrefsValue("isFirstRun", true) as Boolean
        runOnUiThread {
            worldHandler!!.triggerAction(id, Action.DESTROY)

            if (tvModule.getChannelList().size == 0) {

                var sceneData = DialogSceneData(-1, -1)
                sceneData.type = DialogSceneData.DialogType.TEXT
                sceneData.title = ConfigStringsManager.getStringById("no_channels_found")
                sceneData.message = ConfigStringsManager.getStringById("connect_scan_msg")
                sceneData.positiveButtonText =
                    ConfigStringsManager.getStringById("scan_channels_text")
                sceneData.isBackEnabled = false
                sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                    override fun onNegativeButtonClicked() {
                    }

                    override fun onPositiveButtonClicked() {
                        if(!utilsModule.kidsModeEnabled()) {
                            worldHandler!!.triggerAction(
                                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                Action.DESTROY
                            )
                            try {
                                var intent = Intent("android.settings.CHANNELS_SETTINGS")
                                if (BuildConfig.FLAVOR == "rtk")
                                    intent = Intent("android.settings.SETTINGS")
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                ReferenceApplication.applicationContext()
                                    .startActivity(intent)
                            } catch (e: Exception) {
                                worldHandler!!.triggerAction(
                                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                    Action.DESTROY
                                )
                                context!!.runOnUiThread {
                                    worldHandler!!.triggerAction(
                                        ReferenceWorldHandler.SceneId.FTI_SELECT_INPUT_SCAN,
                                        Action.SHOW
                                    )
                                }

                            }
                        }
                    }
                }

                worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.LIVE,
                    Action.SHOW
                )
                if (!ReferenceApplication.isDialogueCreated) {
                    worldHandler!!.triggerActionWithData(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        Action.SHOW_OVERLAY, sceneData
                    )
                }
            } else {
                worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.LIVE,
                    Action.SHOW
                )
                if (ReferenceApplication.guideKeyPressed) {

                    utilsModule.runCoroutineWithDelay({


                        // Show guide scene if the guide global key is pressed
                        ReferenceApplication.worldHandler?.destroyOtherExisting(
                            ReferenceWorldHandler.SceneId.LIVE
                        )
                        var sceneId = ReferenceApplication.worldHandler?.active?.id
                        var sceneInstanceId = ReferenceApplication.worldHandler?.active?.instanceId
                        var position: Int = 1

                        var sceneData = SceneData(sceneId!!, sceneInstanceId!!, position)
                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.HOME_SCENE,
                            Action.SHOW_OVERLAY, sceneData
                        )
                        ReferenceApplication.guideKeyPressed = false

                    }, 100)
                }
                var status = utilsModule.getPrefsValue("walk", false) as Boolean
                //TODO Temporary disable walktrough
                status = true

//                if (!status) {
//                    runOnUiThread {
//                        Handler().postDelayed({
//                            WalkthroughtHelper.start()
//                        }, 1500)
//                    }
//                }

            }
        }
    }

    override fun onSceneInitialized() {
        // Check if already initialized
        if (ReferenceApplication.isInitalized) {
            scene!!.refresh(true)
        }
    }

    override fun onEventReceived(event: Event?) {
        super.onEventReceived(event)
    }

    override fun isFirstRun(): Boolean {
        return utilsModule.getPrefsValue("isFirstRun", false) as Boolean
    }
}