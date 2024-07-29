package com.iwedia.cltv.scene.walkthrough_scene

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.lottie.LottieAnimationView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import kotlinx.coroutines.Dispatchers

/**
 * Walkthrough scene
 *
 * @author Veljko Ilkic
 */
class WalkthroughScene(context: Context, sceneListener: WalkthroughSceneListener) :

    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.WALKTHROUGH,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.WALKTHROUGH),
        sceneListener
    ) {


    /**
     *
     * Xml views
     */
    var messageTv: TextView? = null
    var tvAnimation: LottieAnimationView? = null
    var remoteAnimation: LottieAnimationView? = null
    var dots1: ImageView? = null
    var dots2: ImageView? = null
    var dots3: ImageView? = null
    var dots4: ImageView? = null
    var dotsContainer: View? = null

    private val STEPS_COUNT = 4
    private var currentStep = 0
    val TAG = javaClass.simpleName
    private var doubleBackToExitPressedOnce = false
    override fun createView() {
        super.createView()

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "createView: ")
        view = GAndroidSceneFragment(name, R.layout.layout_scene_walkthrough, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {
                val background_layout: ConstraintLayout = view!!.findViewById(R.id.relativeLayout)
                background_layout.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_background")))

                tvAnimation = view!!.findViewById(R.id.tvAnimation)
                remoteAnimation = view!!.findViewById(R.id.remoteAnimation)
                dotsContainer = view!!.findViewById(R.id.dots_container)
                dots1 = view!!.findViewById(R.id.dots1)
                dots2 = view!!.findViewById(R.id.dots2)
                dots3 = view!!.findViewById(R.id.dots3)
                dots4 = view!!.findViewById(R.id.dots4)
                messageTv = view!!.findViewById(R.id.message)
                messageTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                if((sceneListener as WalkthroughSceneListener).isAccessibilityEnabled()) {
                    background_layout.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    tvAnimation?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    dotsContainer?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    remoteAnimation?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    messageTv!!.isFocusable = true
                    messageTv!!.setFocusable(true)
                    messageTv!!.requestFocus()
                    messageTv!!.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    messageTv!!.setOnClickListener {
                        currentStep++
                        if (currentStep < STEPS_COUNT) {
                            setStep(currentStep)
                        } else {
                            (sceneListener as WalkthroughSceneListener).onEnd()
                        }
                    }
                }

                sceneListener.onSceneInitialized()

                setStep(0)
            }
        })
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {

    }

    private fun setStep(step: Int) {
        dotsContainer!!.visibility = View.VISIBLE

        if ((sceneListener as WalkthroughSceneListener).getConfigInfo("timeshift_enabled")) {
            dots4!!.visibility = View.VISIBLE
        }
        when (step) {
            0 -> {
                //Setup message
                messageTv!!.text = ConfigStringsManager.getStringById("open_channel_list")

                tvAnimation!!.visibility = View.VISIBLE
                tvAnimation!!.setAnimation("Left.json")
                tvAnimation!!.playAnimation()

                remoteAnimation!!.visibility = View.VISIBLE
                remoteAnimation!!.setAnimation("Channellist.json")
                remoteAnimation!!.playAnimation()

                dots1!!.alpha = 1.toFloat()
                dots2!!.alpha = 0.5.toFloat()
                dots3!!.alpha = 0.5.toFloat()
                dots4!!.alpha = 0.5.toFloat()

            }

            1 -> {
                //Setup message
                messageTv!!.text = ConfigStringsManager.getStringById("open_info_list")

                tvAnimation!!.visibility = View.VISIBLE
                tvAnimation!!.setAnimation("Right.json")
                tvAnimation!!.playAnimation()

                remoteAnimation!!.visibility = View.VISIBLE
                remoteAnimation!!.setAnimation("InfoBanner.json")
                remoteAnimation!!.playAnimation()

                dots1!!.alpha = 0.5.toFloat()
                dots2!!.alpha = 1.toFloat()
                dots3!!.alpha = 0.5.toFloat()
                dots4!!.alpha = 0.5.toFloat()
            }

            2 -> {
                //Setup message
                messageTv!!.text = ConfigStringsManager.getStringById("open_main_menu")

                tvAnimation!!.visibility = View.VISIBLE
                tvAnimation!!.setAnimation("Up.json")
                tvAnimation!!.playAnimation()

                remoteAnimation!!.visibility = View.VISIBLE
                remoteAnimation!!.setAnimation("Menu.json")
                remoteAnimation!!.playAnimation()

                dots1!!.alpha = 0.5.toFloat()
                dots2!!.alpha = 0.5.toFloat()
                dots3!!.alpha = 1.toFloat()
                dots4!!.alpha = 0.5.toFloat()
            }

            3 -> {
                //Setup message
                if ((sceneListener as WalkthroughSceneListener).getConfigInfo("timeshift_enabled")) {
                    messageTv!!.text = ConfigStringsManager.getStringById("open_timeshift")

                    tvAnimation!!.visibility = View.VISIBLE
                    tvAnimation!!.setAnimation("Down.json")
                    tvAnimation!!.playAnimation()

                    remoteAnimation!!.visibility = View.VISIBLE
                    remoteAnimation!!.setAnimation("Player.json")
                    remoteAnimation!!.playAnimation()

                    dots1!!.alpha = 0.5.toFloat()
                    dots2!!.alpha = 0.5.toFloat()
                    dots3!!.alpha = 0.5.toFloat()
                    dots4!!.alpha = 1.toFloat()
                } else (sceneListener as WalkthroughSceneListener).onEnd()
            }

            4 -> {
                (sceneListener as WalkthroughSceneListener).onEnd()
            }
        }
    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "dispatchKeyEvent: $currentStep")
        if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
            var validKey = false
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                currentStep++
                validKey = true
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (currentStep == 0) {
                    currentStep = 0
                    validKey = false
                } else {
                    currentStep--
                    validKey = true
                }

            } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (doubleBackToExitPressedOnce) {
                    (sceneListener as WalkthroughSceneListener).exitApplication()
                    return true
                }

                this.doubleBackToExitPressedOnce = true
                (sceneListener as WalkthroughSceneListener).showToast(ConfigStringsManager.getStringById("please_click_again"))

                CoroutineHelper.runCoroutineWithDelay({
                    doubleBackToExitPressedOnce = false
                }, 2000, Dispatchers.Main)
                return true
            }
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "dispatchKeyEvent: setStep $currentStep")
            if (currentStep < STEPS_COUNT) {
                if (validKey) setStep(currentStep)
            } else {
                (sceneListener as WalkthroughSceneListener).onEnd()
            }
        }
        return true
    }
}