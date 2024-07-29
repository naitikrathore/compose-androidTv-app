package com.iwedia.cltv.scene.device_option

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.worldHandler
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.components.CategoryAdapter
import com.iwedia.cltv.components.CategoryItem
import com.iwedia.cltv.components.SpeedTestWidget
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.entities.ReferenceDeviceItem
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import world.SceneData
import world.SceneManager
import java.io.File
import java.util.Timer
import java.util.TimerTask


class DeviceOptionScene(context: Context, sceneListener: DeviceOptionListener) :
    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.DEVICE_OPTION_SCENE,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.DEVICE_OPTION_SCENE),
        sceneListener
    ){

    interface SpeedTestProgressListener {
        fun reportProgress(progress: Int)
        fun onFinished(speed: Float?)
    }

    /**
     * list of options
     */
    private var optionGridView: VerticalGridView? = null

    /**
     * scene background
     */
    private var backgroundLayout: ConstraintLayout? = null

    /**
     * Title
     */
    private var title: TextView? = null

    /**
     * Device name
     */
    private var deviceName: TextView? = null

    /**
     * Progress bar
     */
    private var progressBar: ProgressBar? = null

    /**
     * Format message
     */
    private var formatMessage: TextView? = null

    /**
     * Widget Container
     */
    private var widgetContainer: RelativeLayout? = null

    /**
     * Speed Test Widget
     */
    private var speedTestWidget: SpeedTestWidget? = null

    /**
     * Options List
     */
    var optionList = mutableListOf<CategoryItem>()

    /**
     * Options Adapter
     */
    val optionAdapter = CategoryAdapter()

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(
            name,
            R.layout.layout_scene_device_option,
            object : GAndroidSceneFragmentListener {
                override fun onCreated() {
                    //todo musika

                    optionGridView = view!!.findViewById(R.id.options_grid)

                    backgroundLayout = view!!.findViewById(R.id.background_layout)

                    backgroundLayout!!.setBackgroundColor(Color.parseColor(
                        ConfigColorManager.getColor("color_background").replace("#",
                            ConfigColorManager.alfa_full)))

                    title = view!!.findViewById(R.id.title)
                    title!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    title!!.text = ConfigStringsManager.getStringById("device_option")
                    title!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_3"))

                    deviceName = view!!.findViewById(R.id.device_name)
                    deviceName!!.text = ConfigStringsManager.getStringById("conflicts_message")
                    deviceName!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))


                    progressBar = view!!.findViewById(R.id.loadingProgressBar)
                    progressBar!!.getIndeterminateDrawable().setColorFilter(
                        Color.parseColor(ConfigColorManager.getColor("color_selector")),
                        PorterDuff.Mode.MULTIPLY
                    )

                    formatMessage = view!!.findViewById(R.id.fromat_meaasge)
                    formatMessage!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    formatMessage!!.typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))

                    widgetContainer = view!!.findViewById(R.id.widget_container)

                    setUpAdapter()

                    sceneListener.onSceneInitialized()
                }
            }
        )
    }

    /**
     * Setup options adapter
     */
    fun setUpAdapter(){
        optionList.clear()
        optionList.add(CategoryItem(0,ConfigStringsManager.getStringById("set_timeshift")))
        optionList.add(CategoryItem(1,ConfigStringsManager.getStringById("set_pvr")))
        optionList.add(CategoryItem(2,ConfigStringsManager.getStringById("format")))
        optionList.add(CategoryItem(3,ConfigStringsManager.getStringById("speed_test")))

        optionAdapter.isCenterAligned = true
        optionAdapter.selectedItemEnabled = false
        optionAdapter.refresh(optionList)
        optionGridView!!.setNumColumns(1)
        optionGridView!!.setItemSpacing(context.resources.getDimension(R.dimen.custom_dim_7).toInt())
        optionGridView!!.adapter = optionAdapter
        optionAdapter.adapterListener =
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
                    if (currentPosition == 0) {
                        return true
                    }
                    return false
                }

                override fun onKeyDown(currentPosition: Int): Boolean {
                    return false
                }

                override fun onItemClicked(position: Int) {
                    when(position){
                        0->(sceneListener as DeviceOptionListener).onSelctTimeshift()
                        1->(sceneListener as DeviceOptionListener).onSelectPvr()
                        2->(sceneListener as DeviceOptionListener).onSelectFormat()
                        3->(sceneListener as DeviceOptionListener).onSelectSpeedTest()
                    }
                }

                override fun onBackPressed(position: Int): Boolean {
                    worldHandler!!.triggerAction(id, SceneManager.Action.DESTROY)
                    return true
                }

                override fun onItemSelected(position: Int) {
                }

                override fun digitPressed(digit: Int) {}

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    (sceneListener as DeviceOptionListener).setSpeechText(text = text, importance = importance)
                }
            }
        optionGridView?.requestFocus()
    }


    override fun parseConfig(sceneConfig: SceneConfig?) {
        TODO("Not yet implemented")
    }

    /**
     * Refresh
     */
    override fun refresh(data: Any?) {
        if (data == null) {
            speedTestWidget!!.onFinish(null)
        } else if (data is Float) {
            speedTestWidget!!.onFinish(data)
        } else if (data is Int) {
            speedTestWidget!!.refresh(data)
        } else if (data is SceneData) {
            val deviceItem = data.getData() as ReferenceDeviceItem
            deviceName!!.text = if (deviceItem.label == null) ConfigStringsManager.getStringById("no_name") else deviceItem.label + " "
        }else if (data is String) {
            formatMessage!!.text =  data
        }
        super.refresh(data)
    }

    override fun onResume() {
        worldHandler!!.destroySpecific(id, instanceId)
        super.onResume()
    }


    /**
     * Show speed test
     */
    fun showSpeedTest() {
        optionGridView!!.visibility=View.GONE
        speedTestWidget = SpeedTestWidget(context,object :SpeedTestWidget.SpeedTestWidgetListener{

            override fun onBackPress() {
                worldHandler!!.destroySpecific(id,instanceId)
            }

            override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                return false
            }
        })
        widgetContainer!!.addView(speedTestWidget!!.view)
    }

    fun onFinishSpeedWidget(speedRate: Float) {
        speedTestWidget?.onFinish(speedRate)
    }

    /**
     * Show format
     */
    fun showFormat() {
        title!!.text = ConfigStringsManager.getStringById("device_formatting")
        progressBar!!.visibility = View.VISIBLE
        optionGridView!!.visibility = View.GONE
    }
    fun hideFormat() {
        progressBar!!.visibility = View.GONE
        optionGridView!!.visibility = View.VISIBLE
    }

}