package com.iwedia.cltv.scene.oad

import android.content.Context
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.TextView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener

class OadScene(context: Context, sceneListener: OadSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.OAD_POPUP,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.OAD_POPUP),
    sceneListener
) {
    var titleTv: TextView? = null
    var versionTv: TextView? = null
    var descriptionTv: TextView? = null
    var progressTv: TextView? = null
    var state = OadSceneData.eSceneState.OAD_SCENE_SCAN
    var topButton : Button? = null
    var bottomButton : Button? = null



    override fun parseConfig(sceneConfig: SceneConfig?) {

    }

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.oad_update_layour, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {
                view?.let {
                    findRefs()
                    sceneListener.onSceneInitialized()
                }

            }
        })
    }

    fun findRefs() {
        titleTv = view!!.findViewById(R.id.oad_title)
        versionTv = view!!.findViewById(R.id.oad_version)
        descriptionTv = view!!.findViewById(R.id.oad_description)
        progressTv = view!!.findViewById(R.id.oad_download_progress)
        topButton = view!!.findViewById(R.id.btnTop)
        bottomButton = view!!.findViewById(R.id.btnBottom)
    }

    override fun refresh(data: Any?) {
        if(data is OadSceneData) {
            titleTv?.text = data.title
            versionTv?.text = data.version
            descriptionTv?.text = data.description
            state = data.sceneState

            if(state == OadSceneData.eSceneState.OAD_SCENE_SCAN)
            {
                progressTv?.visibility = View.VISIBLE
                topButton?.setOnClickListener {
                    (sceneListener as OadSceneListener).cancelOadScan()
                }
            }

            if(state == OadSceneData.eSceneState.OAD_SCENE_SCAN_FAIL) {
                progressTv?.visibility = View.INVISIBLE
                topButton?.setOnClickListener {
                    (sceneListener as OadSceneListener).restartOadScan()
                }
                bottomButton?.setOnClickListener {
                    (sceneListener as OadSceneListener).cancelOadScan()
                }
            }

            if(state == OadSceneData.eSceneState.OAD_UP_TO_DATE) {
                progressTv?.visibility = View.INVISIBLE
                topButton?.setOnClickListener {
                    (sceneListener as OadSceneListener).cancelOadScan()
                }
            }

            if(state == OadSceneData.eSceneState.OAD_SCENE_SCAN_SUCCESS) {
                progressTv?.visibility = View.INVISIBLE
                topButton?.setOnClickListener {
                    (sceneListener as OadSceneListener).acceptOadDownload()
                }
                bottomButton?.setOnClickListener {
                    (sceneListener as OadSceneListener).cancelOadScan()
                }
            }

            if(state == OadSceneData.eSceneState.OAD_SCENE_RESTART_CONFIRM) {
                progressTv?.visibility = View.INVISIBLE
                topButton?.setOnClickListener {
                    (sceneListener as OadSceneListener).installOadUpdate()
                }
                bottomButton?.setOnClickListener {
                    (sceneListener as OadSceneListener).cancelOadDownload()
                }
            }

            if(state == OadSceneData.eSceneState.OAD_SCENE_DOWNLOAD_FAIL) {
                progressTv?.visibility = View.INVISIBLE
                topButton?.setOnClickListener {
                    (sceneListener as OadSceneListener).restartOadScan()
                }
                bottomButton?.setOnClickListener {
                    (sceneListener as OadSceneListener).cancelOadScan()
                }
            }

            if(state == OadSceneData.eSceneState.OAD_SCENE_DOWNLOAD) {
                progressTv?.visibility = View.VISIBLE
                topButton?.setOnClickListener {
                    (sceneListener as OadSceneListener).cancelOadDownload()
                }
            }

            progressTv?.text = data.progress

            if(data.topButtonEnabled) {
                topButton?.visibility = View.VISIBLE
                topButton?.text = data.topButtonText
                topButton?.requestFocus()
            } else {
                topButton?.visibility = View.INVISIBLE
            }

            if(data.bottomButtonEnabled) {
                bottomButton?.visibility = View.VISIBLE
                bottomButton?.text = data.bottomButtonText
            } else {
                bottomButton?.visibility = View.INVISIBLE
            }
        }
        if(data is String) {
            ReferenceApplication.runOnUiThread(kotlinx.coroutines.Runnable {
                progressTv?.text = data
            })
        }
    }
}