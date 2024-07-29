package com.iwedia.cltv.scene.digit_zap_conflict

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceDrawableButton
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.scene.dialog.DialogSceneListener
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener

class DigitZapConflictScene(context: Context, sceneListener: DigitZapConflictSceneListener) :
    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.DIGIT_ZAP_CONFLICT,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.DIGIT_ZAP_CONFLICT),
        sceneListener
    ) {

    var buttonContainer: LinearLayout? = null

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_scene_digit_zap_conflict, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {

                buttonContainer = view!!.findViewById(R.id.button_container)


                var channelsInConflict =
                    (sceneListener as DigitZapConflictSceneListener).getChannelsInConflict()

                buttonContainer!!.removeAllViews()

                channelsInConflict.forEach { item ->

                    var button = ReferenceDrawableButton(context)
                    button.setText(item.name)
                    button.isFocusable = true
                    button.setTextColor(
                        Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                    )
                    button.setDrawable(null)
                    button.setOnClickListener { (sceneListener as DialogSceneListener).onNegativeButtonClicked() }
                    button.gravity = Gravity.CENTER
                    button.post {
                        var buttonParams = button!!.layoutParams as LinearLayout.LayoutParams
                        buttonParams.setMargins(0, 0, 0, Utils.getDimensInPixelSize(R.dimen.custom_dim_10))
                        button.layoutParams = buttonParams
                    }

                    button.onFocusChangeListener =
                        View.OnFocusChangeListener { view, hasFocus ->
                            if (hasFocus) {
                                button.setTextColor(
                                    Color.parseColor(ConfigColorManager.getColor("color_background"))
                                )
                            } else {
                                button.setTextColor(
                                    Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                                )
                            }
                        }

                    button.setOnClickListener {
                        (sceneListener as DigitZapConflictSceneListener).onChannelClicked(
                            item
                        )
                    }

                    buttonContainer!!.addView(button)
                }

                sceneListener.onSceneInitialized()
                buttonContainer!!.getChildAt(0).requestFocus()

            }
        })
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
    }

    override fun refresh(data: Any?) {
        super.refresh(data)
    }
}