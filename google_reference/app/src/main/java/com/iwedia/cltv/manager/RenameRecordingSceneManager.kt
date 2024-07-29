package com.iwedia.cltv.manager

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.scene.home_scene.recordings.rename_recording.RenameRecordingScene
import com.iwedia.cltv.scene.home_scene.recordings.rename_recording.RenameRecordingSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager
import listeners.AsyncDataReceiver

/**
 * Rename recording scene manager
 *
 * @author Dejan Nadj
 */
class RenameRecordingSceneManager: GAndroidSceneManager, RenameRecordingSceneListener {

    constructor(context: MainActivity, worldHandler: ReferenceWorldHandler) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.RENAME_RECORDING_SCENE
    )

    override fun createScene() {
        scene = RenameRecordingScene(context!!, this)
    }

    override fun onEnterPressed(text: String) {
        (data!!.getDataByIndex(0) as AsyncDataReceiver<String>).onReceive(text)

        //destroy scene
        worldHandler!!.triggerAction(
            id,
            Action.DESTROY
        )
    }

    override fun onSceneInitialized() {
        //send recording name
        scene!!.refresh(data!!.getDataByIndex(1))
    }

    override fun onBackPressed(): Boolean {
        if(data != null && (data!!.getDataByIndex(0) != null)) {
            (data!!.getDataByIndex(0) as AsyncDataReceiver<String>).onReceive("")
        }
        worldHandler!!.triggerAction(
            id,
            Action.DESTROY
        )
        return true
    }
}