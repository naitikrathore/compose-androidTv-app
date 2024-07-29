package com.iwedia.cltv.scene.choose_pvr_type

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.components.CategoryAdapter
import com.iwedia.cltv.components.CategoryItem
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import world.SceneManager

class ChoosePvrTypeScene(context: Context, sceneListener: ChoosePvrTypeSceneListener) :
ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.CHOOSE_PVR_TYPE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.CHOOSE_PVR_TYPE),
    sceneListener
)  {

    private var pvrTypeGridView: VerticalGridView? = null

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_scene_choose_pvr_type, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {

                pvrTypeGridView = view!!.findViewById(R.id.pvr_type_grid_view)
                pvrTypeGridView!!.verticalSpacing = 10
                val background_layout: ConstraintLayout = view!!.findViewById(R.id.background_layout)
                background_layout.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_background").replace("#",ConfigColorManager.alfa_light_bg)))


                val recTypeTitle: TextView = view!!.findViewById(R.id.rec_type_title)
                recTypeTitle.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                recTypeTitle.setText(ConfigStringsManager.getStringById("rec_type_title"))
                val pvrTypeAdapter = CategoryAdapter()
                pvrTypeAdapter.isCenterAligned = true
                var list = mutableListOf<CategoryItem>()
                list.add(CategoryItem(0, ConfigStringsManager.getStringById("rec_now")))
                list.add(CategoryItem(1, ConfigStringsManager.getStringById("rec_after_curr_event")))
                list.add(CategoryItem(2, ConfigStringsManager.getStringById("rec_set_date_time")))

                pvrTypeAdapter.refresh(list)
                pvrTypeGridView!!.setNumColumns(1)
                pvrTypeGridView!!.adapter = pvrTypeAdapter

                pvrTypeAdapter.adapterListener =
                    object : CategoryAdapter.ChannelListCategoryAdapterListener {
                        override fun getAdapterPosition(position: Int) {

                        }

                        override fun onItemSelected(position: Int) {

                        }

                        override fun digitPressed(digit: Int) {}

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            (sceneListener as ChoosePvrTypeSceneListener).setSpeechText(text = text, importance = importance)
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
                            when(position){
                                0->{
                                    (sceneListener as ChoosePvrTypeSceneListener).startRecording()
                                }
                                1->{
                                    (sceneListener as ChoosePvrTypeSceneListener).scheduleRecordingForNextEvent()
                                }
                                2->{
                                    ReferenceApplication.worldHandler!!.triggerAction(id, SceneManager.Action.HIDE)

                                    ReferenceApplication.worldHandler!!.triggerAction(
                                            ReferenceWorldHandler.SceneId.CUSTOM_RECORDING,
                                            SceneManager.Action.SHOW_OVERLAY
                                        )


                                }
                            }
                        }

                        override fun onBackPressed(position: Int): Boolean {
                            return false
                        }

                    }
                    pvrTypeGridView?.requestFocus()
            }
        })
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
        TODO("Not yet implemented")
    }

    override fun refresh(data: Any?) {
        super.refresh(data)
    }
}