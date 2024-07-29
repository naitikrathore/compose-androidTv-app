package com.iwedia.cltv.scene.timeshift.timeshiftBuffer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceDrawableButton
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener

/**
 * This scene is created to show over TimeShift Scene when buffer limit reached.
 * */
class TimeshiftBufferLimitScene(
    context: Context,
    sceneListener: TimeshiftBufferLimitSceneListener
) :
    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.TIMESHIFT_BUFFER_LIMIT_SCENE,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.TIMESHIFT_BUFFER_LIMIT_SCENE),
        sceneListener
    ), View.OnFocusChangeListener, View.OnClickListener {


    private var timeShiftBufferContainer: ConstraintLayout? = null
    private var tsBufferReachedText: TextView? = null
    private var tsWatchButton: ReferenceDrawableButton? = null
    private var tsReturnButton: ReferenceDrawableButton? = null

    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(
            name,
            R.layout.layout_ts_buffer_limit_scene,
            object : GAndroidSceneFragmentListener {
                @SuppressLint("ResourceType")
                override fun onCreated() {

                    // UI to show timeShift buffer limit reached warning.
                    timeShiftBufferContainer =
                        view!!.findViewById(R.id.ts_buffer_reached_warning_container)
                    tsBufferReachedText = view!!.findViewById(R.id.ts_buffer_reached_text)
                    tsBufferReachedText!!.setTextColor(
                        Color.parseColor(
                            ConfigColorManager.getColor(
                                "color_main_text"
                            )
                        )
                    )
                    tsBufferReachedText!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_medium")
                    )
                    tsBufferReachedText!!.text =
                        ConfigStringsManager.getStringById("timeshift_buffer_reached_warning")
                    tsReturnButton = view!!.findViewById(R.id.returnButton)
                    tsReturnButton!!.setText(ConfigStringsManager.getStringById("return_to_live"))
                    tsReturnButton!!.getTextView().typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_medium")
                    )
                    tsWatchButton = view!!.findViewById(R.id.watchButton)
                    tsWatchButton!!.setText(ConfigStringsManager.getStringById("watch"))
                    tsWatchButton!!.getTextView().typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_medium")
                    )
                    tsReturnButton!!.setOnClickListener(this@TimeshiftBufferLimitScene)
                    tsWatchButton!!.setOnClickListener(this@TimeshiftBufferLimitScene)
                    timeShiftBufferContainer?.visibility = View.VISIBLE
                    timeShiftBufferContainer!!.requestFocus()
                    sceneListener.onSceneInitialized()

                }
            }
        )
    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_UP -> return true
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (tsReturnButton!!.hasFocus()) {
                    return true
                } else {
                    tsReturnButton!!.requestFocus()
                    return true
                }
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (tsWatchButton!!.hasFocus()) {
                    return true
                } else {
                    tsWatchButton!!.requestFocus()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent)
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {}
    override fun onFocusChange(view: View?, hasFocus: Boolean) {
        if (view is ReferenceDrawableButton) {
            when (hasFocus) {
                true -> {
                    view.setBackgroundColor(
                        Color.parseColor(ConfigColorManager.getColor("color_selector"))
                    )
                    Utils.focusAnimation(view)
                }

                false -> {
                    view.setBackgroundColor(
                        Color.parseColor(ConfigColorManager.getColor("color_background"))
                    )
                    Utils.unFocusAnimation(view)
                }
            }
        }
    }

    override fun onClick(view: View?) {
        if (view is ReferenceDrawableButton) {
            when (view) {
                tsReturnButton -> {
                    timeShiftBufferContainer?.visibility = View.GONE
                    (sceneListener as TimeshiftBufferLimitSceneListener).onReturnToLiveClicked()
                    return
                }

                tsWatchButton -> {
                    timeShiftBufferContainer?.visibility = View.GONE
                    (sceneListener as TimeshiftBufferLimitSceneListener).onTimeShiftWatchClicked()
                    return
                }
            }
        }
    }
}