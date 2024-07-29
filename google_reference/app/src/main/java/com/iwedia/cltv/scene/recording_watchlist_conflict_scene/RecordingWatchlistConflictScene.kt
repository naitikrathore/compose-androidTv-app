package com.iwedia.cltv.scene.recording_watchlist_conflict_scene

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.CountDownTimer
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.worldHandler
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.components.CategoryAdapter
import com.iwedia.cltv.components.CategoryItem
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import utils.information_bus.Event
import utils.information_bus.InformationBus
import world.SceneManager

/**
 *  Recording Watchlist Conflict Scene
 *
 *  @author Shubham Kumar
 */

class RecordingWatchlistConflictScene (context: Context, sceneListener: RecordingWatchlistConflictSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.RECORDING_WATCHLIST_CONFLICT_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.RECORDING_WATCHLIST_CONFLICT_SCENE),
    sceneListener
) {

    private var eventsInConflictList: VerticalGridView? = null
    private var backgroundLayout: ConstraintLayout? = null
    var listOfConflictedEvents = mutableListOf<TvEvent>()
    var adapterList = mutableListOf<CategoryItem>()
    var autoStartRecording: CountDownTimer? = null

    @SuppressLint("SetTextI18n")
    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(
            name,
            R.layout.layout_scene_recording_watchlist_conflict,
            object : GAndroidSceneFragmentListener {
                override fun onCreated() {
                    if(view==null){
                        return
                    }
                    eventsInConflictList = view!!.findViewById(
                        R.id.channels_in_conflict_grid
                    )
                    backgroundLayout = view!!.findViewById(
                        R.id.background_layout
                    )
                    backgroundLayout!!.setBackgroundColor(
                        Color.parseColor(
                            ConfigColorManager.getColor("color_background").replace("#",
                                ConfigColorManager.alfa_light_bg)))

                    val descriptionText : TextView = view!!.findViewById(R.id.dialog_title)
                    descriptionText.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    descriptionText.text = ConfigStringsManager.getStringById("recording_watchlist_conflict_title")

                    val descriptionText2 : TextView = view!!.findViewById(R.id.dialog_message)
                    descriptionText2.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    descriptionText2.typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))

                    val recordingWatchlistConflictAdapter = CategoryAdapter()
                    recordingWatchlistConflictAdapter.isCenterAligned = true

                    for(i in 0 until listOfConflictedEvents.size){
                        if(i==0){
                            adapterList.add(CategoryItem(i,"   "+ ConfigStringsManager.getStringById("record") + " " + listOfConflictedEvents[i].tvChannel.name + " - " + listOfConflictedEvents[i].name + "   "))
                        }else{
                            adapterList.add(CategoryItem(i,"   "+ ConfigStringsManager.getStringById("watch") + " " + listOfConflictedEvents[i].tvChannel.name + " - " + listOfConflictedEvents[i].name + "   "))
                        }
                    }
                    recordingWatchlistConflictAdapter.refresh(adapterList)
                    eventsInConflictList!!.setNumColumns(1)
                    eventsInConflictList!!.setItemSpacing(context.resources.getDimension(R.dimen.custom_dim_7).toInt())
                    eventsInConflictList!!.adapter = recordingWatchlistConflictAdapter

                    autoStartRecording = object :
                        CountDownTimer(
                            60000,
                            1000
                        ) {
                        override fun onTick(millisUntilFinished: Long) {
                            descriptionText2.text = ConfigStringsManager.getStringById("recording_watchlist_conflict_description") + (millisUntilFinished/1000).toString()
                        }
                        @SuppressLint("NewApi")
                        override fun onFinish() {
                            descriptionText2.text = ConfigStringsManager.getStringById("recording_watchlist_conflict_description") + "0"
                            val event = listOfConflictedEvents[0]
                            worldHandler!!.triggerAction(
                                ReferenceWorldHandler.SceneId.RECORDING_WATCHLIST_CONFLICT_SCENE,
                                SceneManager.Action.DESTROY
                            )
                            startRecording(event)
                        }
                    }
                    autoStartRecording!!.start()

                    recordingWatchlistConflictAdapter.adapterListener =
                        object : CategoryAdapter.ChannelListCategoryAdapterListener {
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
                                val channel = listOfConflictedEvents[position].tvChannel
                                val event = listOfConflictedEvents[position]
                                autoStartRecording!!.cancel()
                                worldHandler!!.triggerAction(
                                    ReferenceWorldHandler.SceneId.RECORDING_WATCHLIST_CONFLICT_SCENE,
                                    SceneManager.Action.DESTROY
                                )
                                if (position == 0) {
                                    startRecording(event)
                                } else {
                                    (sceneListener as RecordingWatchlistConflictSceneListener).playChannel(
                                        channel
                                    )
                                }
                                (sceneListener as RecordingWatchlistConflictSceneListener).onEventSelected()
                            }
                            @SuppressLint("NewApi")
                            override fun onBackPressed(position: Int): Boolean {
                                worldHandler!!.triggerAction(
                                    ReferenceWorldHandler.SceneId.RECORDING_WATCHLIST_CONFLICT_SCENE,
                                    SceneManager.Action.DESTROY
                                )
                                (sceneListener as RecordingWatchlistConflictSceneListener).onEventSelected()
                                return true
                            }

                            override fun onItemSelected(position: Int) {}
                            override fun digitPressed(digit: Int) {}

                            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                                (sceneListener as RecordingWatchlistConflictSceneListener).setSpeechText(text = text, importance = importance)
                            }
                        }
                    eventsInConflictList?.requestFocus()
                }
            }
        )
        sceneListener.onSceneInitialized()
    }
    override fun parseConfig(sceneConfig: SceneConfig?) {}

    override fun onDestroy() {
        autoStartRecording!!.cancel()
        listOfConflictedEvents.clear()
        (sceneListener as RecordingWatchlistConflictSceneListener).onEventSelected()
        super.onDestroy()
    }

    override fun refresh(data: Any?) {
        if (Utils.isDataType(data, RecordingWatchlistConflictSceneData::class.java)) {
            listOfConflictedEvents = (data as RecordingWatchlistConflictSceneData).listOfConflictedTvEvents!!
        }
        super.refresh(data)
    }

    private fun startRecording(event: TvEvent) {
        if ((sceneListener as RecordingWatchlistConflictSceneListener).isTimeShiftActive()) {
            (sceneListener as RecordingWatchlistConflictSceneListener).timeShiftStop(object :
                IAsyncCallback {
                override fun onFailed(error: Error) {}

                override fun onSuccess() {
                    worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                    ReferenceApplication.runOnUiThread {
                        (sceneListener as RecordingWatchlistConflictSceneListener).showToast(ConfigStringsManager.getStringById("time_shift_stop_toast"))
                    }
                }
            })
        }

        (sceneListener as RecordingWatchlistConflictSceneListener).changeChannel(
            event.tvChannel,
            object : IAsyncCallback {
                override fun onFailed(error: Error) {}
                override fun onSuccess() {
                    (sceneListener as RecordingWatchlistConflictSceneListener).startRecordingByChannel(
                        event.tvChannel,
                        object : IAsyncCallback {
                            override fun onFailed(error: Error) {}

                            override fun onSuccess() {
                                worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                                InformationBus.submitEvent(Event(Events.UPDATE_REC_LIST_INDICATOR))
                            }
                        })
                }
            }
        )
    }

}