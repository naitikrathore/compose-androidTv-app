package com.iwedia.cltv.scene.reminder_conflict_scene

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.RecyclerView
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
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import world.SceneManager

/**
 *  Reminder conflict scene
 *
 *  @author Shubham Kumar
 */
class ReminderConflictScene(context: Context, sceneListener: ReminderConflictSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.REMINDER_CONFLICT_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.REMINDER_CONFLICT_SCENE),
    sceneListener
) {

    private var channelsInConflictList: VerticalGridView? = null

    /**
     * scene background
     */
    private var backgroundLayout: ConstraintLayout? = null

    //list for conflicted watch list events
    private var listOfConflictedEvents = mutableListOf<TvEvent>()
    private var adapterList = mutableListOf<CategoryItem>()

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(
            name,
            R.layout.layout_scene_reminder_conflict,
            object : GAndroidSceneFragmentListener {
                override fun onCreated() {
                    //because scene was being called multiple times and view was null (causing crash)
                    Log.d(Constants.LogTag.CLTV_TAG + "VIEW","$view")
                    if(view==null){
                        return
                    }
                    channelsInConflictList = view!!.findViewById(
                        R.id.channels_in_conflict_grid
                    )
                    // Adding space for cancel item
                    channelsInConflictList!!.addItemDecoration(object :RecyclerView.ItemDecoration() {
                        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                            if (parent.getChildAdapterPosition(view) == parent.getAdapter()!!.getItemCount() - 1)
                                outRect.top = Utils.getDimensInPixelSize(R.dimen.custom_dim_20)
                            else super.getItemOffsets(outRect, view, parent, state)
                        }
                    })
                    backgroundLayout = view!!.findViewById(
                        R.id.background_layout
                    )
                    backgroundLayout!!.setBackgroundColor(
                        Color.parseColor(
                            ConfigColorManager.getColor("color_background").replace("#",
                                ConfigColorManager.alfa_light_bg)))

                    val descriptionText : TextView = view!!.findViewById(R.id.dialog_title)
                    descriptionText.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    descriptionText.text = ConfigStringsManager.getStringById("reminder_conflict_title")

                    val descriptionText2 : TextView = view!!.findViewById(R.id.dialog_message)
                    descriptionText2.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    descriptionText2.text = ConfigStringsManager.getStringById("reminder_conflict_description")
                    descriptionText2.typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))

                    val reminderConflictAdapter = CategoryAdapter()
                    reminderConflictAdapter.isCenterAligned = true

                    for(i in 0 until listOfConflictedEvents.size){
                        adapterList.add(CategoryItem(i,"   "+ listOfConflictedEvents[i].tvChannel.name + " - " + listOfConflictedEvents[i].name + "   "))
                    }
                    // Adding cancel item
                    adapterList.add(CategoryItem(-1,ConfigStringsManager.getStringById("cancel")))
                    reminderConflictAdapter.refresh(adapterList)
                    channelsInConflictList!!.setNumColumns(1)
                    channelsInConflictList!!.setItemSpacing(context!!.resources.getDimension(R.dimen.custom_dim_7).toInt())
                    channelsInConflictList!!.adapter = reminderConflictAdapter

                    reminderConflictAdapter.adapterListener =
                        object : CategoryAdapter.ChannelListCategoryAdapterListener {
                            override fun getAdapterPosition(position: Int) {
                            }

                            override fun onItemSelected(position: Int) {
                            }

                            override fun digitPressed(digit: Int) {}

                            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                                (sceneListener as ReminderConflictSceneListener).setSpeechText(text = text, importance = importance)
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

                            override fun onItemClicked(position: Int) {
                                if(position==listOfConflictedEvents.size){ // If cancel is pressed
                                    onBackPressed(position)
                                    return
                                }
                                val channel = listOfConflictedEvents[position].tvChannel
                                worldHandler!!.triggerAction(
                                    ReferenceWorldHandler.SceneId.REMINDER_CONFLICT_SCENE,
                                    SceneManager.Action.DESTROY
                                )
                                startWatchlistProgram(channel)
                                (sceneListener as ReminderConflictSceneListener).onEventSelected()
                            }
                            override fun onBackPressed(position: Int): Boolean {
                                worldHandler!!.triggerAction(
                                    ReferenceWorldHandler.SceneId.REMINDER_CONFLICT_SCENE,
                                    SceneManager.Action.DESTROY
                                )
                                (sceneListener as ReminderConflictSceneListener).onEventSelected()
                                return true
                            }
                        }
                    channelsInConflictList?.requestFocus()
                }
            }
        )
        sceneListener.onSceneInitialized()
    }
    override fun parseConfig(sceneConfig: SceneConfig?) {}

    override fun onDestroy() {
        //callback when overlay is destroyed, just to clear list of conflicted watchlist events
        listOfConflictedEvents.clear()
        (sceneListener as ReminderConflictSceneListener).onEventSelected()
        super.onDestroy()
    }

    override fun refresh(data: Any?) {
        if (Utils.isDataType(data, ReminderSceneData::class.java)) {
            listOfConflictedEvents = (data as ReminderSceneData).listOfConflictedTvEvents!!
        }
        super.refresh(data)
    }

    private fun startWatchlistProgram(scheduledChannel: TvChannel){
        (sceneListener as ReminderConflictSceneListener).playChannel( scheduledChannel )
    }

}