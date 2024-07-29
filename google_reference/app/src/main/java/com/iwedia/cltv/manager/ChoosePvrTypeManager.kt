package com.iwedia.cltv.manager

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.scene.choose_pvr_type.ChoosePvrTypeScene
import com.iwedia.cltv.scene.choose_pvr_type.ChoosePvrTypeSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager

//TODO Check is this class used
class ChoosePvrTypeManager:  GAndroidSceneManager, ChoosePvrTypeSceneListener {

    private val textToSpeechModule: TTSInterface

    constructor(context: MainActivity, worldHandler: ReferenceWorldHandler, textToSpeechModule: TTSInterface) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.CHOOSE_PVR_TYPE
    ){
        this.textToSpeechModule = textToSpeechModule
    }

    /**
     * Log TAG
     */
    private val TAG = "ChoosePvrTypeManager"

    override fun createScene() {
        scene = ChoosePvrTypeScene(context!!, this)

    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun startRecording() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "start recording")
        ReferenceApplication.worldHandler!!.triggerAction(id,
            Action.DESTROY)
        /*var activeTvChannel =
            (ReferenceSdk.tvHandler as ReferenceTvHandler).activeChannel

        RecordingHelper.startRecording(activeTvChannel!!)*/
    }

    override fun scheduleRecordingForNextEvent() {
/*        var activeTvChannel =
            (ReferenceSdk.tvHandler as ReferenceTvHandler).activeChannel
        var list = ( ReferenceSdk.dataProvider as TifDataProvider).events[activeTvChannel!!.id]
        if (list!!.size==1 ||list!!.size==0)
        {
            showToast("No information about next event")
            worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            return
        }
        var nextEvent: ReferenceTvEvent =list!!.get(1)
        if (nextEvent==null)
        {
            showToast("No information about next event")
            worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            return
        }


        val obj = ReferenceScheduledRecording(id, activeTvChannel!!.name, nextEvent.startDate, nextEvent.endDate, activeTvChannel!!, nextEvent!!, "0")*/
        //RecordingHelper.startRecordingNextEvent(this)
/*        ReferenceSdk.pvrSchedulerHandler?.scheduleRecording(obj, object :
            AsyncReceiver {
            override fun onFailed(error: Error?) {
                Log.i("Kaustubh", "onFailed: KK1")
                showToast(ConfigStringsManager.getStringById("recording_reminder_conflict_toast"))
            }

            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + "SCHEDULED RECORDING", "onSuccess: $nextEvent")
            }
        })*/
//        worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
        



    }

    override fun onBackPressed(): Boolean {
        worldHandler!!.triggerAction(id, Action.DESTROY)
        return true
    }

    override fun onSceneInitialized() {
        TODO("Not yet implemented")
    }
}