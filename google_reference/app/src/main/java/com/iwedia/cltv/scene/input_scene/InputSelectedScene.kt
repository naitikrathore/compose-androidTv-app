package com.iwedia.cltv.scene.input_scene

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import world.SceneListener

/**
 * Input selected scene
 *
 * @author Vandana B
 */
class InputSelectedScene(context: Context, sceneListener: SceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.INPUT_SELECTED_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.INPUT_SELECTED_SCENE),
    sceneListener
) {

    private var tempList: List<String> = listOf()

    /**
     * Scene content
     */
    var inputSelectedText: TextView? = null
    var inputPixelText: TextView? = null
    var inputSourceText: TextView? = null
    var inputResHDIcon: ImageView? = null
    var inputResUHDIcon: ImageView? = null
    var inputResSDIcon: ImageView? = null
    var inputResFHDIcon: ImageView? = null
    var inputHdrValue: TextView? = null

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.input_selected_layout, object :
            GAndroidSceneFragmentListener {
            override fun onCreated() {

                inputSelectedText =
                    view?.findViewById(R.id.input_selected_text)

                inputPixelText =
                    view?.findViewById(R.id.input_pixel)

                inputResHDIcon = view?.findViewById(R.id.input_hd_icon)

                inputResUHDIcon = view?.findViewById(R.id.input_uhd_icon)

                inputResFHDIcon = view?.findViewById(R.id.input_fhd_icon)

                inputResSDIcon = view?.findViewById(R.id.input_sd_icon)


                inputHdrValue = view?.findViewById(R.id.input_hdr)

                inputSourceText =
                    view?.findViewById(R.id.input_source)

                sceneListener.onSceneInitialized()
            }
        })
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
        TODO("Not yet implemented")
    }

    override fun refresh(data: Any?) {
        if (data != null && data is InputSelectedSceneData) {
            if (data.getData() != null) {
                if (data.inputType.isNotEmpty()) {
                    tempList = data.inputType.split(":")
                    if (tempList.isNotEmpty()) {
                        inputSelectedText?.text = tempList[0]
                    }
                }
                if (data.inputPixelValue.isNotEmpty()) {
                    inputPixelText?.visibility = View.VISIBLE
                    inputPixelText?.text = data.inputPixelValue.toLowerCase()
                } else {
                    inputPixelText?.visibility = View.GONE
                }
                if (data.inputHdrValue.isNotEmpty()) {
                    inputHdrValue?.visibility = View.VISIBLE
                    inputHdrValue?.text = data.inputHdrValue
                } else {
                    inputHdrValue?.visibility = View.GONE
                }
                if (data.inputIcon.isNotEmpty()) {
                    when (data.inputIcon) {
                        "0" -> {
                            inputResHDIcon?.visibility = View.VISIBLE
                            inputResHDIcon?.visibility = View.GONE
                            inputResHDIcon?.visibility = View.GONE
                            inputResHDIcon?.visibility = View.GONE

                        }

                        "1" -> {
                            inputResHDIcon?.visibility = View.GONE
                            inputResUHDIcon?.visibility = View.VISIBLE
                            inputResFHDIcon?.visibility = View.GONE
                            inputResSDIcon?.visibility = View.GONE

                        }

                        "2" -> {
                            inputResHDIcon?.visibility = View.GONE
                            inputResUHDIcon?.visibility = View.GONE
                            inputResFHDIcon?.visibility = View.VISIBLE
                            inputResSDIcon?.visibility = View.GONE

                        }

                        "3", "4" -> {
                            inputResHDIcon?.visibility = View.GONE
                            inputResUHDIcon?.visibility = View.GONE
                            inputResFHDIcon?.visibility = View.GONE
                            inputResSDIcon?.visibility = View.VISIBLE

                        }
                    }
                } else {
                    inputResHDIcon?.visibility = View.GONE
                    inputResUHDIcon?.visibility = View.GONE
                    inputResFHDIcon?.visibility = View.GONE
                    inputResSDIcon?.visibility = View.GONE

                }
                if (tempList.size > 1 && tempList[1].isNotEmpty()) {
                    inputSourceText?.visibility = View.VISIBLE
                    inputSourceText?.text = tempList[1]
                } else {
                    inputSourceText?.visibility = View.GONE
                }
            }
        }
    }
}