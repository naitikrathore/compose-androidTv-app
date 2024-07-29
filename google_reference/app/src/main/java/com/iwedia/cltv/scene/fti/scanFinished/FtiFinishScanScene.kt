package com.iwedia.cltv.scene.fti.scanFinished

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.media.tv.TvInputInfo
import android.view.KeyEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.*
import com.iwedia.cltv.config.*
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener

/**
 * Fti finish scan scene
 *
 * @author Aleksandar Lazic
 */
class FtiFinishScanScene(context: Context, sceneListener: FtiFinishScanSceneListener) :
    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.FTI_FINISH_SCAN,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.FTI_FINISH_SCAN),
        sceneListener
    ) {

    var companyLogoIv: ImageView? = null
    var finishScanTv: TextView? = null
    var messageTv: TextView? = null
    var message2Tv: TextView? = null
    var proceedButton: ReferenceDrawableButton? = null

    var foundChannelsNumber = 8
    var inputName = ""

    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(name, R.layout.layout_scene_fti_scan_finished, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {
                findRefs()
                parseConfig(configParam!!)
                sceneListener.onSceneInitialized()
            }
        })
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
        for (i in 0 until sceneConfig!!.value.size) {
            when (sceneConfig.value[i].id) {
                0 -> {
                    ConfigHandler.applyConfig(companyLogoIv!!, sceneConfig.value[i])
                }
                1 -> {
                    ConfigHandler.applyConfig(finishScanTv!!, sceneConfig.value[i])
                }
                2 -> {
                    ConfigHandler.applyConfig(messageTv!!, sceneConfig.value[i])
                }
                3 -> {
                    ConfigHandler.applyConfig(message2Tv!!, sceneConfig.value[i])
                }
                4 -> {
                    ConfigHandler.applyConfig(proceedButton!!, sceneConfig.value[i])
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun refresh(data: Any?) {
        if (Utils.isDataType(data, TvInputInfo::class.java)) {
            inputName = Utils.getTvInputName(data as TvInputInfo)
        } else if (Utils.isDataType(data, Integer::class.java)) {
            foundChannelsNumber = data as Int

            messageTv!!.text =
                "$foundChannelsNumber " + ConfigStringsManager.getStringById("channels_found_on") +
                        " $inputName " + ConfigStringsManager.getStringById("input")

            if (foundChannelsNumber > 0) {
                proceedButton!!.requestFocus()
            }
        }
        super.refresh(data)
    }

    private fun findRefs() {
        val ftiFinishedScanLayout: ConstraintLayout = view!!.findViewById(R.id.ftiFinishedScanLayout)
        ftiFinishedScanLayout.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_background")))



        companyLogoIv = view!!.findViewById(R.id.iv_company_logo_id)
        finishScanTv = view!!.findViewById(R.id.finishScanMessage)
        finishScanTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        finishScanTv!!.setText(ConfigStringsManager.getStringById("finishScan"))
        messageTv = view!!.findViewById(R.id.infoMessage)
        messageTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        messageTv!!.setText(ConfigStringsManager.getStringById("channels_found_on"))
        message2Tv = view!!.findViewById(R.id.indo_message_2)
        message2Tv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        message2Tv!!.setText(ConfigStringsManager.getStringById("go_back_to_scan"))
        proceedButton = view!!.findViewById(R.id.proceed_btn)

        companyLogoIv!!.setImageDrawable(
            ReferenceApplication.applicationContext().getDrawable(R.drawable.ic_iwedia)
        )

        proceedButton!!.setText(ConfigStringsManager.getStringById("proceed"))
        proceedButton!!.getTextView().typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
        )
        proceedButton!!.setOnClickListener {
            (sceneListener as FtiFinishScanSceneListener).onProceedClicked()
        }
    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                sceneListener.onBackPressed()
                return true
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent)
    }
}