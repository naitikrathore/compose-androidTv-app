package com.iwedia.cltv.scene.intro_scene

import android.animation.Animator
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import kotlinx.coroutines.Dispatchers

/**
 * Intro scene
 *
 * @author Aleksandar Lazic
 */
class IntroScene(context: Context, sceneListener: IntroSceneListener) :

    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.INTRO,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.INTRO),
        sceneListener
    ) {

    var isAppInitialized: Boolean = false
    var isAppInitializedDone : Boolean = false

    /**
     * Xml views
     */
    var logoIv: ImageView? = null
    var messageTv: TextView? = null
    var titleTv: TextView? = null
    var animationView: LottieAnimationView? = null
    var networkInfo: TextView? = null
    private var doubleBackToExitPressedOnce = false
    private var animationFinished = false
    private var animationCounter = 0


    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(name, R.layout.layout_scene_intro, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {
                findRefs()
                setupScene()
                startAnimation()
                sceneListener.onSceneInitialized()
            }
        })
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
    }

    override fun refresh(data: Any?) {
        super.refresh(data)

        if (data is Boolean) {
            isAppInitialized = data
            if (isAppInitialized && animationFinished) {
                (sceneListener as IntroSceneListener).onAppInitialized()
            }
        } else if (data is String) {
            networkInfo!!.text = data
        }
    }

    private fun findRefs() {
        val background_layout: ConstraintLayout = view!!.findViewById(R.id.relativeLayout)
        background_layout.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_background")))
        logoIv = view!!.findViewById(R.id.iv_loader)
        logoIv?.visibility = View.GONE
        messageTv = view!!.findViewById(R.id.message)
        messageTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        titleTv = view!!.findViewById(R.id.title)
        titleTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_selector")))
        animationView = view!!.findViewById(R.id.animationView)
        networkInfo = view!!.findViewById(R.id.network_info)
        networkInfo!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
    }

    private fun startAnimation() {
        if (++animationCounter > 3) {
            return
        }
        animationView!!.visibility = View.VISIBLE

        //Wait for is region supported to be detected
        animationView?.postDelayed({
            if ((sceneListener as IntroSceneListener).isRegionSupported()) {
                animationView!!.setAnimation("intro_anim.json")
            } else {
                animationView!!.setAnimation("intro_anim_region_not_supported.json")
            }
            animationView!!.repeatCount = 0
            animationView!!.playAnimation()
            animationView?.removeAllAnimatorListeners()
            animationView!!.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {}

                override fun onAnimationEnd(p0: Animator) {
                    animationFinished = true
                    if ((isAppInitialized) && (!isAppInitializedDone)) {
                        (sceneListener as IntroSceneListener).onAppInitialized()
                        isAppInitializedDone = true
                    } else {
                        if(!isAppInitialized) (sceneListener as IntroSceneListener).onSceneInitialized()
                        startAnimation()
                    }
                }

                override fun onAnimationCancel(p0: Animator) {

                }

                override fun onAnimationRepeat(p0: Animator) {

                }
            })
        }, 2000)
    }

    private fun setupScene() {
        messageTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        messageTv!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        var event = keyEvent as KeyEvent
        if (event.action == KeyEvent.ACTION_UP) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (doubleBackToExitPressedOnce) {
                        (sceneListener as IntroSceneListener).exitApplication()
                        return true
                    }
                    this.doubleBackToExitPressedOnce = true
                    (sceneListener as IntroSceneListener).showToast(ConfigStringsManager.getStringById("please_click_again"))

                    CoroutineHelper.runCoroutineWithDelay({
                        doubleBackToExitPressedOnce = false
                    }, 2000, Dispatchers.Main)
                }
            }
        }
        return true
    }
}