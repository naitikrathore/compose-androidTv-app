package com.iwedia.cltv.scene.zap_digit

import android.content.Context
import android.view.KeyEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.components.ReferenceWidgetZapDigit
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import kotlin.Error

class ZapDigitScene(context: Context, sceneListener: ZapDigitSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.DIGIT_ZAP,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.DIGIT_ZAP),
    sceneListener
) {
    var sceneContainer: ConstraintLayout? = null
    var widget: ReferenceWidgetZapDigit? = null

    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(name, R.layout.layout_scene_zap_digit, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {

                if (view != null) {

                    sceneContainer = view!!.findViewById(R.id.scene_container)
                    widget = ReferenceWidgetZapDigit(
                        context,
                        object : ReferenceWidgetZapDigit.GZapDigitListener {
                            override fun timerEndZap(itemId: Int) {
                                (sceneListener as ZapDigitSceneListener).onTimerEndZap(itemId)
                            }

                            override fun timerEnd() {
                                (sceneListener as ZapDigitSceneListener).onTimerEnd()
                            }

                            override fun digitPressed(digit: Int) {
                                (sceneListener as ZapDigitSceneListener).onDigitPressed(digit)
                            }

                            override fun channelClicked(itemId: Int) {
                                (sceneListener as ZapDigitSceneListener).zapOnDigit(itemId)

                            }

                            override fun onPeriodPressed() {
                                (sceneListener as ZapDigitSceneListener).onPeriodPressed()
                            }

                            override fun getChannelSourceType(tvChannel: TvChannel): String {
                                return (sceneListener as ZapDigitSceneListener).getChannelSourceType(tvChannel)
                            }

                            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                                (sceneListener as ZapDigitSceneListener).setSpeechText(text = text, importance = importance)
                            }

                        })

                    //add the view with 0dp width and height
                    val layoutParams = ConstraintLayout.LayoutParams(0, 0)
                    val view = widget!!.view
                    view!!.layoutParams = layoutParams
                    view.id = View.generateViewId()
                    sceneContainer!!.addView(view)

                    val constraints = ConstraintSet()
                    constraints.connect(
                        view.id,
                        ConstraintSet.LEFT,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.LEFT
                    );
                    constraints.connect(
                        view.id,
                        ConstraintSet.RIGHT,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.RIGHT
                    );
                    constraints.connect(
                        view.id,
                        ConstraintSet.TOP,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.TOP
                    );
                    constraints.connect(
                        view.id,
                        ConstraintSet.BOTTOM,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.BOTTOM
                    );
                    constraints.applyTo(sceneContainer)

                    parseConfig(configParam)
                    sceneListener.onSceneInitialized()
                }
            }
        })
    }

    var isGotActionDownKey = false

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if (isGotActionDownKey && (keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
            isGotActionDownKey = false
            sceneListener.onBackPressed()
            return true
        }

        return super.dispatchKeyEvent(keyCode, keyEvent)
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
    }

    override fun refresh(data: Any?) {
        super.refresh(data)
        if (data is Error){
            widget?.channelsList?.clear()
            widget?.startTimer()
        }
        if (data is Long) {
            widget?.timerTimeout = data
        } else {
            if(data != null) {
                widget?.refresh(data)
            }
        }
    }

    fun restoreFocus() {
        widget!!.restoreFocus()
    }

    fun requestFocus() {
        widget!!.requestFocus()
    }

    fun restartTimer() {
        if(widget != null) widget!!.startTimer()
    }

    fun hasFocus(): Boolean {
        val hasFocus = widget!!.view!!.hasFocus()
        return  hasFocus
    }

    override fun onDestroy() {
        super.onDestroy()

        if (widget != null) {
            widget!!.dispose()
        }
    }
}