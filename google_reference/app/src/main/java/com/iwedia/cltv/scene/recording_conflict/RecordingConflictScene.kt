package com.iwedia.cltv.scene.recording_conflict

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.runOnUiThread
import com.iwedia.cltv.ReferenceApplication.Companion.worldHandler
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.components.CategoryAdapter
import com.iwedia.cltv.components.CategoryItem
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import world.SceneManager

/**
 * Class RecordingConflictScene
 *
 * @author Shreya Srivastava
 */
class RecordingConflictScene(context: Context, sceneListener: RecordingConflictListener) :
    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.RECORDING_CONFLICT_SCENE,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.RECORDING_CONFLICT_SCENE),
        sceneListener
    ){

    /**
     * list of conflicted channels
     */
    private var channelsInConflictList: VerticalGridView? = null

    /**
     * scene background
     */
    private var backgroundLayout: ConstraintLayout? = null

    //conflicted recordings and list
    var newScheduledRecording : ScheduledRecording? = null
    var newRecording: TvEvent? = null
    var oldScheduledRecordings : MutableList<ScheduledRecording>? = null

    var listOfConflictedRec = mutableListOf<CategoryItem>()
    val recConflictAdapter = CategoryAdapter()

    var sceneTitle: TextView? = null
    var conflictMessage: TextView? = null
    var instructionMessage: TextView? = null

    var itemTextFirst : String = ""
    var itemTextSecond : String = ""

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(
            name,
            R.layout.layout_scene_recording_conflict,
            object : GAndroidSceneFragmentListener {
                override fun onCreated() {
                    //because scene was being called multiple times and view was null (causing crash)
                    Log.d(Constants.LogTag.CLTV_TAG + "VIEW","$view")
                    if(view==null){
                        return
                    }

                    channelsInConflictList = view!!.findViewById(R.id.channels_in_conflict_grid)

                    backgroundLayout = view!!.findViewById(R.id.background_layout)

                    backgroundLayout!!.setBackgroundColor(Color.parseColor(
                                            ConfigColorManager.getColor("color_background").replace("#",
                                            ConfigColorManager.alfa_light_bg)))

                    recConflictAdapter.isCenterAligned = true

                    sceneTitle = view!!.findViewById(R.id.description_text)
                    sceneTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    sceneTitle!!.text = ConfigStringsManager.getStringById("description_text")

                    conflictMessage = view!!.findViewById(R.id.description_text2)
                    conflictMessage !!.text = ConfigStringsManager.getStringById("description_text2")
                    conflictMessage!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    conflictMessage!!.typeface = TypeFaceProvider.getTypeFace(
                                                ReferenceApplication.applicationContext(),
                                                ConfigFontManager.getFont("font_3"))

                    instructionMessage = view!!.findViewById(R.id.description_text3)
                    instructionMessage!!.text = ConfigStringsManager.getStringById("description_text3")
                    instructionMessage!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))

                    sceneListener.onSceneInitialized()

                }
            }
        )
    }


    override fun parseConfig(sceneConfig: SceneConfig?) {}

    override fun refresh(data: Any?) {
        if (Utils.isDataType(data, RecordingConflictSceneData::class.java)){

            sceneTitle!!.text = (data as RecordingConflictSceneData).title
            if (data.conflictMessage != null){
                conflictMessage!!.text = data.conflictMessage
            }

            if(data.type != null){
                when (data.type){
                    RecordingConflictSceneData.RecordingConflictType.RECORDING_RECORDING -> {
                        var message = ConfigStringsManager.getStringById("conflict_recording_recording_message")
                        message = message.replace("%s", data.firstRecording?.tvChannel?.name!!)
                        conflictMessage!!.text = message

                        var text1 = ConfigStringsManager.getStringById("conflict_recording_recording_text1")
                        text1 = text1.replace("%s1", data.firstRecording?.tvChannel?.name!!)

                        itemTextFirst = text1

                        var text2 = ConfigStringsManager.getStringById("conflict_recording_recording_text2")
                        text2 = text2.replace("%s2", data.secondRecording?.tvChannel?.name!!)

                        itemTextSecond = text2

                    }

                    RecordingConflictSceneData.RecordingConflictType.RECORDING_SCHEDULE -> {
                        var message = ConfigStringsManager.getStringById("conflict_recording_schedule_message")
                        message = message.replace("%s", data.firstRecording?.tvChannel?.name!!)
                        conflictMessage!!.text = message

                        var text1 = ConfigStringsManager.getStringById("conflict_recording_schedule_text1")
                        text1 = text1.replace("%s2", data.secondRecording?.tvChannel?.name!!)

                        itemTextFirst = text1

                        var text2 = ConfigStringsManager.getStringById("conflict_recording_schedule_text2")
                        text2 = text2.replace("%s2", data.secondSchedule?.tvChannel?.name!!)

                        itemTextSecond = text2

                    }

                    RecordingConflictSceneData.RecordingConflictType.SCHEDULE_SCHEDULE -> {
                    }

                    else -> {

                    }

                }
            }

            if (data.firstItemText != null && data.secondItemText != null){
                when (data.type) {
                    RecordingConflictSceneData.RecordingConflictType.SCHEDULE_RECORDING -> {
                        newRecording = data.secondRecording
                        oldScheduledRecordings = (sceneListener as RecordingConflictListener).getOldRecByTvEvent(newRecording!!)


                        listOfConflictedRec.add(CategoryItem(0, data.secondRecording!!.tvChannel.name+ " - "+data.secondRecording!!.name))

                        oldScheduledRecordings!!.forEach {
                            listOfConflictedRec.add(CategoryItem(listOfConflictedRec.size, it.tvChannel?.name+ " - "+it.name))
                        }
                    }
                    RecordingConflictSceneData.RecordingConflictType.SCHEDULE_SCHEDULE -> {
                        newScheduledRecording = (sceneListener as RecordingConflictListener).getNewRec()

                        listOfConflictedRec.add(CategoryItem(0, newScheduledRecording!!.tvChannel?.name+ " - "+newScheduledRecording!!.name))

                        oldScheduledRecordings = (sceneListener as RecordingConflictListener).getOldRec(newScheduledRecording!!)

                        oldScheduledRecordings!!.forEach {
                            listOfConflictedRec.add(CategoryItem(listOfConflictedRec.size, it.tvChannel?.name+ " - "+it.name))
                        }
                    }
                    else -> {
                        listOfConflictedRec.add(CategoryItem(0, itemTextFirst))
                        listOfConflictedRec.add(CategoryItem(1, itemTextSecond))
                    }
                }

                recConflictAdapter.refresh(listOfConflictedRec)
                channelsInConflictList!!.setNumColumns(1)
                channelsInConflictList!!.setItemSpacing(context!!.resources.getDimension(R.dimen.custom_dim_7).toInt())
                channelsInConflictList!!.adapter = recConflictAdapter

                recConflictAdapter.adapterListener = object : CategoryAdapter.ChannelListCategoryAdapterListener {
                    override fun getAdapterPosition(position: Int) {
                    }

                    override fun onKeyLeft(currentPosition: Int): Boolean {
                        return false
                    }

                    override fun onKeyRight(currentPosition: Int): Boolean {
                        return false
                    }

                    override fun onKeyUp(currentPosition: Int): Boolean {
                        if(currentPosition==0){
                            return true
                        }
                        return false
                    }

                    override fun onKeyDown(currentPosition: Int): Boolean {
                        return false
                    }

                    @SuppressLint("NewApi")
                    override fun onItemClicked(position: Int) {
                        when(position){
                            0 -> {
                                when (data.type) {
                                    RecordingConflictSceneData.RecordingConflictType.SCHEDULE_SCHEDULE -> {
                                        (sceneListener as RecordingConflictListener).scheduleNew(
                                            newScheduledRecording!!,
                                            oldScheduledRecordings!!
                                        )

                                        (sceneListener as RecordingConflictListener).showToast(ConfigStringsManager.getStringById("toast_for_updated_choice"))

                                        worldHandler!!.triggerAction(
                                            id,
                                            SceneManager.Action.DESTROY
                                        )

                                    }

                                    RecordingConflictSceneData.RecordingConflictType.SCHEDULE_RECORDING -> {
                                        (sceneListener as RecordingConflictListener).cancelScheduledStartActive(
                                            newRecording!!.tvChannel,
                                            oldScheduledRecordings!!
                                        )
                                    }

                                    else -> {
                                        (sceneListener as RecordingConflictListener).onFirstItemClicked()
                                    }
                                }
                            }

                            1 -> {
                                if (data.type != RecordingConflictSceneData.RecordingConflictType.SCHEDULE_SCHEDULE) {
                                    (sceneListener as RecordingConflictListener).onSecondItemClicked()
                                } else {
                                    (sceneListener as RecordingConflictListener).showToast(ConfigStringsManager.getStringById("toast_for_updated_choice"))

                                    worldHandler!!.triggerAction(id, SceneManager.Action.DESTROY)
                                }
                            }
                        }
                        (sceneListener as RecordingConflictListener).showToast(ConfigStringsManager.getStringById("toast_for_updated_choice"))

                        worldHandler!!.triggerAction(id, SceneManager.Action.DESTROY)
                    }

                    @SuppressLint("NewApi")
                    override fun onBackPressed(position: Int): Boolean {
                        worldHandler!!.triggerAction(id, SceneManager.Action.DESTROY)

                        runOnUiThread {
                            worldHandler!!.triggerAction(
                                ReferenceWorldHandler.SceneId.PVR_BANNER_SCENE,
                                SceneManager.Action.SHOW_OVERLAY
                            )
                        }
                        return true
                    }

                    override fun onItemSelected(position: Int) {
                    }

                    override fun digitPressed(digit: Int) {}

                    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                        (sceneListener as RecordingConflictListener).setSpeechText(text = text, importance = importance)
                    }
                }
                channelsInConflictList?.requestFocus()
            }
        }
        super.refresh(data)
    }

}