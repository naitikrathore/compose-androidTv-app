package com.iwedia.cltv.utils

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceWorldHandler
import kotlin.concurrent.thread

/**
 * @author Maksim
 *
 * This class is used to Scale TvView that is in LiveScene
 * This view will be live through the whole app lifecycle
 * and will not leak memory in that sense.
 *
 * DO NOT place any views here for now until generic class for View scaling and
 * animation is made.
 */
@SuppressLint("StaticFieldLeak")
object TvViewScaler {

    enum class ScaleState {
        NONE,
        SCALED_UP,
        SCALED_DOWN
    }

    interface TvViewRescaleListener {
        fun onResize(state: ScaleState)
    }

    var listeners = mutableListOf<TvViewRescaleListener>()


    private lateinit var tvView: ViewGroup
    private var state = ScaleState.NONE
    private var worldHandler: ReferenceWorldHandler?= null

    fun init(view: ViewGroup, worldHandler: ReferenceWorldHandler) {
        this.tvView = view
        this.worldHandler = worldHandler
    }

    fun registerResizeListener(listener : TvViewRescaleListener) {
        listeners.add(listener)
    }

    fun restore() {
        when(state) {
            ScaleState.SCALED_DOWN -> {
                state = ScaleState.NONE
                scaleDownTvView()
            }
            ScaleState.SCALED_UP -> {
                state = ScaleState.NONE
                scaleUpTvView()
            }
            ScaleState.NONE ->{
                scaleDownTvView()
            }
        }
    }

    fun reset() {
        tvView.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
    }


    fun scaleUpTvView() {
        var scaleExecuted = false
        if (state != ScaleState.SCALED_UP) {
            tvView.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            scaleExecuted = true
        }
        state = ScaleState.SCALED_UP
        if(scaleExecuted) {
            thread {
                for (listener in listeners) {
                    listener.onResize(ScaleState.SCALED_UP)
                }
            }
        }
    }

    fun scaleDownTvView() {
        //Initial scale 4sec delay is needed for mtk broadcast initial playback case
        //Scaling does not work properly if the view is scaled down before initial playback start
        var delay = if (state == ScaleState.NONE) 4000L else 100L
        tvView.postDelayed(Runnable {
            var scaleExecuted = false
            if (state == ScaleState.SCALED_DOWN) {
                if (worldHandler?.active?.id == ReferenceWorldHandler.SceneId.HOME_SCENE) {
                    tvView.layoutParams = RelativeLayout.LayoutParams(
                        Utils.getDimensInPixelSize(R.dimen.custom_dim_640),
                        Utils.getDimensInPixelSize(R.dimen.custom_dim_320)
                    ).apply {
                        addRule(RelativeLayout.ALIGN_PARENT_TOP)
                        addRule(RelativeLayout.ALIGN_PARENT_END)
                    }
                    scaleExecuted = true
                }
            }
            if(scaleExecuted) {
                thread {
                    for (listener in listeners) {
                        listener.onResize(ScaleState.SCALED_DOWN)
                    }
                }
            }
        },delay)
        state = ScaleState.SCALED_DOWN
    }
}