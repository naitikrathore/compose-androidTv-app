package com.iwedia.cltv.scene.pvr_banner

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.components.RefereceWidgetPvrBanner
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener

/**
 * Pvr banner scene
 *
 * @author Dragan Krnjaic
 */
class PvrBannerScene(context: Context, sceneListener: PvrBannerSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.PVR_BANNER_SCENE,
    "Pvr banner",
    sceneListener
) {

    /**
     * Scene container
     */
    var sceneContainer: ConstraintLayout? = null

    /**
     * Widget
     */
    var widget: RefereceWidgetPvrBanner? = null

    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(
            name,
            R.layout.layout_scene_pvr_banner,
            object : GAndroidSceneFragmentListener {

                override fun onCreated() {
                    if (view != null) {
                        sceneContainer = view!!.findViewById(R.id.scene_container)
                        widget = RefereceWidgetPvrBanner(context,
                            object :
                            RefereceWidgetPvrBanner.PvrBannerWidgetListener {
                                override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                                    return false
                                }

                                override fun onTimerEnd() {
                                    sceneListener.onBackPressed()
                                }

                                override fun setRecIndication(boolean: Boolean) {
                                    (sceneListener as PvrBannerSceneListener).setRecIndication(boolean)
                                }

                                override fun getChannelById(id: Int): TvChannel {
                                    return (sceneListener as PvrBannerSceneListener).getChannelById(id)
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
                        )
                        constraints.connect(
                            view.id,
                            ConstraintSet.RIGHT,
                            ConstraintSet.PARENT_ID,
                            ConstraintSet.RIGHT
                        )
                        constraints.connect(
                            view.id,
                            ConstraintSet.TOP,
                            ConstraintSet.PARENT_ID,
                            ConstraintSet.TOP
                        )
                        constraints.connect(
                            view.id,
                            ConstraintSet.BOTTOM,
                            ConstraintSet.PARENT_ID,
                            ConstraintSet.BOTTOM
                        )
                        constraints.applyTo(sceneContainer)

                        parseConfig(configParam)
                        sceneListener.onSceneInitialized()
                        widget?.startTimer()
                    }
                }
            })
    }

    fun setVisible() {
        view?.let {
            (it as GAndroidSceneFragment).fragmentView?.visibility = View.VISIBLE
        }
    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        Log.i("BACKPRESS", "PVR keyevent $keyEvent keyCode $keyCode")
        if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                if (widget!!.isVisible()) {
                    (sceneListener as PvrBannerSceneListener).showStopRecordingDialog("NULL")
                } else {
                    (sceneListener as PvrBannerSceneListener).onBackPressed()
                }
                return true
            }

            if (keyCode != KeyEvent.KEYCODE_DPAD_DOWN &&
                keyCode != KeyEvent.KEYCODE_MEDIA_PLAY &&
                keyCode != KeyEvent.KEYCODE_MEDIA_PAUSE &&
                keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE &&
                keyCode != KeyEvent.KEYCODE_MEDIA_FAST_FORWARD &&
                keyCode != KeyEvent.KEYCODE_MEDIA_REWIND) {
                sceneListener.onBackPressed()
            }
        }
        if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN) {
            if ((keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE ||
                        keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
                        keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) && !widget!!.isVisible()
            ) {
                (sceneListener as PvrBannerSceneListener).showToast(ConfigStringsManager.getStringById("recording_progress_toast"), UtilsInterface.ToastDuration.LENGTH_LONG)
            }
            if (keyCode != KeyEvent.KEYCODE_MEDIA_RECORD &&
                keyCode != KeyEvent.KEYCODE_MEDIA_STOP &&
                keyCode != KeyEvent.KEYCODE_BACK &&
                keyCode != KeyEvent.KEYCODE_ESCAPE &&
                keyCode != KeyEvent.KEYCODE_DPAD_DOWN &&
                keyCode != KeyEvent.KEYCODE_PROG_RED &&
                keyCode != KeyEvent.KEYCODE_CHANNEL_UP &&
                keyCode != KeyEvent.KEYCODE_CHANNEL_DOWN) {
                sceneListener.onBackPressed()
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun refresh(data: Any?) {
        super.refresh(data)
        widget!!.refresh(data!!)
    }

    override fun onResume() {
        super.onResume()
        if(widget != null && !widget!!.isVisible()) {
            widget!!.startTimer()
        }
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
    }

    override fun onDestroy() {
        super.onDestroy()
        widget!!.dispose()
    }
}