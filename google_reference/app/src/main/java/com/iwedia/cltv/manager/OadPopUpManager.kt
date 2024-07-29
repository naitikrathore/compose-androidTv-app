package com.iwedia.cltv.manager

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.`interface`.OadUpdateInterface
import com.iwedia.cltv.scene.PIN.PinSceneData
import com.iwedia.cltv.scene.oad.OadScene
import com.iwedia.cltv.scene.oad.OadSceneData
import com.iwedia.cltv.scene.oad.OadSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager

class OadPopUpManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
) : GAndroidSceneManager(context, worldHandler, ReferenceWorldHandler.SceneId.OAD_POPUP) {

    companion object {
        lateinit var mOadUpdateModule : OadUpdateInterface
        lateinit var mActiveScene: world.Scene<*, *>
        fun setOatUpdateModule(oadUpdateModule: OadUpdateInterface) {
            mOadUpdateModule = oadUpdateModule
            oadUpdateModule.registerListener(object : OadUpdateInterface.OadEventListener {
                override fun onFileFound(version: Int) {
                    var oadDownloadString =  ConfigStringsManager.getStringById("oad_download")
                    var oadDownloadDescString =  ConfigStringsManager.getStringById("oad_download_description")
                    var versionString = ConfigStringsManager.getStringById("oad_version")+String.format("0x%02x",mOadUpdateModule.getSoftwareVersion())
                    var acceptString = ConfigStringsManager.getStringById("oad_accept")
                    var remindString = ConfigStringsManager.getStringById("oad_remind")

                    ReferenceApplication.runOnUiThread(kotlinx.coroutines.Runnable {
                        ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.OAD_POPUP,
                            Action.SHOW, OadSceneData(
                                ReferenceApplication.worldHandler!!.active!!.id,
                                ReferenceApplication.worldHandler!!.active!!.instanceId,
                                oadDownloadString,versionString,oadDownloadDescString,"",OadSceneData.eSceneState.OAD_SCENE_SCAN_SUCCESS,
                                true,true,acceptString,remindString)
                        )
                    })
                }

                override fun onFileNotFound() {
                    var failedString =  ConfigStringsManager.getStringById("oad_failed")
                    var failedDescriptionString =  ConfigStringsManager.getStringById("oad_failed_desc")
                    var versionString = ConfigStringsManager.getStringById("oad_version")+String.format("0x%02x",mOadUpdateModule.getSoftwareVersion())
                    var okString = ConfigStringsManager.getStringById("oad_ok")
                    var CancelString = ConfigStringsManager.getStringById("oad_cancel")

                    ReferenceApplication.runOnUiThread(kotlinx.coroutines.Runnable {
                        ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.OAD_POPUP,
                            Action.SHOW, OadSceneData(
                                ReferenceApplication.worldHandler!!.active!!.id,
                                ReferenceApplication.worldHandler!!.active!!.instanceId,
                                failedString,versionString,failedDescriptionString,"",OadSceneData.eSceneState.OAD_SCENE_SCAN_FAIL,
                                topButtonEnabled = true,
                                bottomButtonEnabled = true,
                                topButtonText = okString,
                                bottomButtonText = CancelString)
                        )
                    })
                }

                override fun onScanStart() {
                    var scanningString =  ConfigStringsManager.getStringById("oad_scanning")
                    var versionString = ConfigStringsManager.getStringById("oad_version")+String.format("0x%02x",mOadUpdateModule.getSoftwareVersion())
                    var descriptionString = ConfigStringsManager.getStringById("oad_scanning_description")
                    var progressString  = ConfigStringsManager.getStringById("oad_scan_progress")+"0%"
                    var cancelString = ConfigStringsManager.getStringById("oad_cancel")

                    ReferenceApplication.runOnUiThread(kotlinx.coroutines.Runnable {
                        ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.OAD_POPUP,
                            Action.SHOW, OadSceneData(
                                ReferenceApplication.worldHandler!!.active!!.id,
                                ReferenceApplication.worldHandler!!.active!!.instanceId,
                                scanningString,versionString,descriptionString,progressString,OadSceneData.eSceneState.OAD_SCENE_SCAN,
                                true,false,cancelString,"")
                        )
                    })
                }

                override fun onScanProgress(progress: Int) {
                    mActiveScene!!.refresh(ConfigStringsManager.getStringById("oad_scan_progress")+progress+"%")
                  }

                override fun onDownloadStarted() {
                    var oadDownloadString =  ConfigStringsManager.getStringById("oad_downloading")
                    var oadDownloadDescString =  ConfigStringsManager.getStringById("oad_downloading_description")
                    var versionString = ConfigStringsManager.getStringById("oad_version")+String.format("0x%02x",mOadUpdateModule.getSoftwareVersion())
                    var cancelString = ConfigStringsManager.getStringById("oad_cancel")
                    var progressString = ConfigStringsManager.getStringById("oad_download_progress")+"0%"

                    ReferenceApplication.runOnUiThread(kotlinx.coroutines.Runnable {
                        ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.OAD_POPUP,
                            Action.SHOW, OadSceneData(
                                ReferenceApplication.worldHandler!!.active!!.id,
                                ReferenceApplication.worldHandler!!.active!!.instanceId,
                                oadDownloadString,versionString,oadDownloadDescString,progressString,OadSceneData.eSceneState.OAD_SCENE_DOWNLOAD,
                                true,false,cancelString,"")
                        )
                    })
                }

                override fun onDownloadProgress(progress: Int) {
                    mActiveScene!!.refresh(ConfigStringsManager.getStringById("oad_download_progress")+progress+"%")
                }

                override fun onDownloadFail() {
                    var oadDownloadFailString =  ConfigStringsManager.getStringById("oad_download_fail")
                    var oadDownloadFailDescString =  ConfigStringsManager.getStringById("oad_download_fail_desc")
                    var versionString = ConfigStringsManager.getStringById("oad_version")+String.format("0x%02x",mOadUpdateModule.getSoftwareVersion())
                    var okString = ConfigStringsManager.getStringById("oad_ok")
                    var cancelString = ConfigStringsManager.getStringById("oad_cancel")

                    ReferenceApplication.runOnUiThread(kotlinx.coroutines.Runnable {
                        ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.OAD_POPUP,
                            Action.SHOW, OadSceneData(
                                ReferenceApplication.worldHandler!!.active!!.id,
                                ReferenceApplication.worldHandler!!.active!!.instanceId,
                                oadDownloadFailString,versionString,oadDownloadFailDescString,"",OadSceneData.eSceneState.OAD_SCENE_DOWNLOAD_FAIL,
                                true,true,okString,cancelString)
                        )
                    })
                }

                override fun onDownloadSucess() {
                    var restartString =  ConfigStringsManager.getStringById("oad_restart_to_install")
                    var restartDescriptionString =  ConfigStringsManager.getStringById("oad_restart_description")
                    var versionString = ConfigStringsManager.getStringById("oad_version")+String.format("0x%02x",mOadUpdateModule.getSoftwareVersion())
                    var acceptString = ConfigStringsManager.getStringById("oad_accept")
                    var rejectString = ConfigStringsManager.getStringById("oad_reject")

                    ReferenceApplication.runOnUiThread(kotlinx.coroutines.Runnable {
                        ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.OAD_POPUP,
                            Action.SHOW, OadSceneData(
                                ReferenceApplication.worldHandler!!.active!!.id,
                                ReferenceApplication.worldHandler!!.active!!.instanceId,
                                restartString,versionString,restartDescriptionString,"",OadSceneData.eSceneState.OAD_SCENE_RESTART_CONFIRM,
                                true,true,acceptString,rejectString
                        ))
                    })
                }

                override fun onUpgradeSuccess(version: Int) {

                }

                override fun onNewestVersion() {
                    var upToDateString =  ConfigStringsManager.getStringById("oad_up_to_date")
                    var versionString = ConfigStringsManager.getStringById("oad_version")+String.format("0x%02x",mOadUpdateModule.getSoftwareVersion())
                    var okString = ConfigStringsManager.getStringById("oad_ok")

                    ReferenceApplication.runOnUiThread(kotlinx.coroutines.Runnable {
                        ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.OAD_POPUP,
                            Action.SHOW, OadSceneData(
                                ReferenceApplication.worldHandler!!.active!!.id,
                                ReferenceApplication.worldHandler!!.active!!.instanceId,
                                upToDateString,versionString,"","",OadSceneData.eSceneState.OAD_UP_TO_DATE,
                                true,false,okString,"")
                        )
                    })
                }
            })
        }
    }

    override fun createScene() {
        scene = OadScene(context!!, object : OadSceneListener {
            override fun cancelOadScan() {
                ReferenceApplication.runOnUiThread(kotlinx.coroutines.Runnable {
                    ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                })
                mOadUpdateModule.stopScan()
            }

            override fun acceptOadDownload() {
                mOadUpdateModule.startDownload()
            }

            override fun cancelOadDownload() {
                ReferenceApplication.runOnUiThread(kotlinx.coroutines.Runnable {
                    ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                })
                mOadUpdateModule.stopDownload()
            }

            override fun installOadUpdate() {
                mOadUpdateModule.applyOad()
            }

            override fun restartOadScan() {
                mOadUpdateModule.startScan()
            }

            override fun onBackPressed(): Boolean {
                ReferenceApplication.runOnUiThread(kotlinx.coroutines.Runnable {
                    ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                })
                return true
            }

            override fun onSceneInitialized() {
                val sceneData = data as OadSceneData
                scene!!.refresh(sceneData)
            }

        })
        mActiveScene = scene!!
    }

    override fun onSceneInitialized() {
        val sceneData = data as OadSceneData
        scene!!.refresh(sceneData)
    }
}