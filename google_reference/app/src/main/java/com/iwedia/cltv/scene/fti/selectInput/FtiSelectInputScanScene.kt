package com.iwedia.cltv.scene.fti.selectInput

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.media.tv.TvInputInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener

/**
 * Fti select input scene
 *
 * @author Aleksandar Lazic
 */
class FtiSelectInputScanScene(context: Context, sceneListener: FtiSelectInputScanSceneListener) :
    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.FTI_SELECT_INPUT_SCAN,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.FTI_SELECT_INPUT_SCAN),
        sceneListener
    ) {

    var companyLogoIv: ImageView? = null
    var titleTv: TextView? = null
    var messageTv: TextView? = null
    var scanTv: TextView? = null
    var verticalGridView: VerticalGridView? = null
    var adapter: SelectInputListAdapter? = null
    val inputList = mutableListOf<TvInputInfo>()

    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(name, R.layout.layout_scene_fti_select_input_scan, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {
                (sceneListener as FtiSelectInputScanSceneListener).requestInputs()
                findRefs()
                setupAdapter()
                parseConfig(configParam!!)
                sceneListener.onSceneInitialized()
            }
        })
    }

    override fun refresh(data: Any?) {
        if (Utils.isDataType(data, MutableList::class.java)) {
            if (Utils.isListDataType(data, TvInputInfo::class.java)) {
                inputList.addAll(data as Collection<TvInputInfo>)
            }
        }


        super.refresh(data)
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
        for (i in 0 until sceneConfig!!.value.size) {
            when (sceneConfig.value[i].id) {
                0 -> {
                    ConfigHandler.applyConfig(companyLogoIv!!, sceneConfig.value[i])
                }
                1 -> {
                    ConfigHandler.applyConfig(titleTv!!, sceneConfig.value[i])
                }
                2 -> {
                    ConfigHandler.applyConfig(messageTv!!, sceneConfig.value[i])
                }
                3 -> {
                    ConfigHandler.applyConfig(scanTv!!, sceneConfig.value[i])
                }
            }
        }
    }

    private fun setupAdapter() {
        adapter = SelectInputListAdapter()
        adapter!!.setListener(object : SelectInputListAdapter.AdapterClickListener {
            override fun onItemClicked(position: Int, inputInfo: TvInputInfo) {
                (sceneListener as FtiSelectInputScanSceneListener).onInputSelected(
                        inputInfo)
            }

        })

        val listForAdapter = inputList.distinctBy { Utils.getTvInputName(it) }
        verticalGridView!!.setNumColumns(1)
        verticalGridView!!.adapter = adapter
        adapter!!.refresh(listForAdapter as MutableList<TvInputInfo>)
        verticalGridView!!.post {
            verticalGridView!!.layoutManager!!.findViewByPosition(0)!!.requestFocus()
        }
    }

    private fun findRefs() {

        companyLogoIv = view!!.findViewById(R.id.company_logo_iv)
        titleTv = view!!.findViewById(R.id.fti_scan_company_title_tv)
        titleTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        titleTv!!.setText(ConfigStringsManager.getStringById("tv_select_input_scan_title"))
        messageTv = view!!.findViewById(R.id.fti_select_input_message_tv)
        messageTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        messageTv!!.setText(ConfigStringsManager.getStringById("tv_select_input_scan_message"))
        scanTv = view!!.findViewById(R.id.scan_tv)
        scanTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        scanTv!!.setText(ConfigStringsManager.getStringById("scan"))
        verticalGridView = view!!.findViewById(R.id.tvInputsRecyclerView)

        companyLogoIv!!.setImageDrawable(ReferenceApplication.applicationContext().getDrawable(R.drawable.ic_iwedia))
    }
}