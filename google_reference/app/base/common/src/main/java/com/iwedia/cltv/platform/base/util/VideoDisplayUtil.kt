package com.iwedia.cltv.platform.base.util

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.media.tv.TvTrackInfo
import android.os.Build
import android.view.Display
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.player.VideoInfo
import kotlin.math.abs
import kotlin.math.roundToInt

enum class VideoDimensions(val width: Int, val height: Int) {
    VIDEO_SD(704, 480),
    VIDEO_HD(1280, 720),
    VIDEO_FULL_HD(1920, 1080),
    VIDEO_ULTRA_HD(2048, 1536);

    fun isDimensionSatisfying(w: Int, h: Int) = w >= width && h >= height
}

enum class VideoDefinition {
    UNKNOWN_LEVEL, SD_LEVEL, HD_LEVEL, FULL_HD_LEVEL, ULTRA_HD_LEVEL
}

enum class DisplayMode(val value: Int) {
    /* Smaller playback screen */
    NORMAL(0),
    /* Full playback screen */
    FULL(1),
    /** Zoomed playback screen */
    ZOOM(2);
}

object VideoDisplayUtil {
    private const val DISPLAY_MODE_EPSILON = 0.001f

    fun resolveVideoDefinition(width: Int, height: Int): VideoDefinition {
        return if (VideoDimensions.VIDEO_ULTRA_HD.isDimensionSatisfying(width, height)) {
                    VideoDefinition.ULTRA_HD_LEVEL
                }
                else if (VideoDimensions.VIDEO_FULL_HD.isDimensionSatisfying(width, height)) {
                    VideoDefinition.FULL_HD_LEVEL
                }
                else if (VideoDimensions.VIDEO_HD.isDimensionSatisfying(width, height)) {
                    VideoDefinition.HD_LEVEL
                }
                else if (VideoDimensions.VIDEO_SD.isDimensionSatisfying(width, height)) {
                    VideoDefinition.SD_LEVEL
                }
                else {
                    VideoDefinition.UNKNOWN_LEVEL
                }
    }

    fun calculateAspectRatio(width: Int, height: Int, track: TvTrackInfo): Float {
        if(width <= 0 || height <= 0) return 0f
        val pixelAspectRation =
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) track.videoPixelAspectRatio
            else 1f
        return (width / height) * pixelAspectRation
    }

    fun isDisplayModeAvailable( displayMode: Int, videoInfo: VideoInfo): Boolean {
        if (displayMode == DisplayMode.FULL.value) {
            return true
        }
        if (videoInfo.width <= 0 || videoInfo.height <= 0 || videoInfo.aspectRation <= 0f) {
            return false
        }
        val viewRatio = videoInfo.width.toFloat() / videoInfo.height
        return abs(viewRatio - videoInfo.aspectRation) >= DISPLAY_MODE_EPSILON
    }

    /** Returns display's width and height through Pair object */
    fun getDisplaySize(context: Context): Pair<Int, Int> {
        return if(Build.VERSION_CODES.R >= Build.VERSION.SDK_INT) {
            getDisplaySize(
                context.getSystemService(DisplayManager::class.java))
        }
        else {
            getDisplaySize(
                context.getSystemService(WindowManager::class.java))
        }
    }

    private fun getDisplaySize(displayManager: DisplayManager): Pair<Int, Int> {
        val display: Display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val point = Point()
        display.getSize(point)
        return Pair(point.x, point.y)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getDisplaySize(windowManager: WindowManager): Pair<Int, Int> {
        val bounds = windowManager.currentWindowMetrics.bounds
        return Pair(bounds.width(), bounds.height())
    }

    fun createLayoutParams(
        windowBounds: Pair<Int, Int>,
        start: Int = 0,
        end: Int = 0,
        top: Int = 0,
        bottom: Int = 0
    ) = MarginLayoutParams(
        windowBounds.first - start - end,
        windowBounds.second - top - bottom
    ).also {
        it.marginStart = start
        it.marginEnd = end
        it.topMargin = top
        it.bottomMargin = bottom
    }

//    fun applyDisplayModeToLayoutParams(
//        mode: Int, videoAspectRatio: Float, displayParams: MarginLayoutParams
//    ): ConstraintLayout.LayoutParams {
//
//        return ConstraintLayout.LayoutParams(displayParams).also { params ->
//            val windowAspectRatio = displayParams.width.toFloat() / displayParams.height
//
//            when (mode) {
//                DisplayMode.ZOOM.value -> {
//                    if (videoAspectRatio < windowAspectRatio) {   // Y axis will be clipped.
//                        params.height = (params.width / videoAspectRatio).roundToInt()
//                    }
//                    else {  // X axis will be clipped.
//                        params.width = (params.height * videoAspectRatio).roundToInt()
//                    }
//                }
//                DisplayMode.NORMAL.value -> {
//                    if (videoAspectRatio < windowAspectRatio) { // X axis has black area.
//                        params.width = (params.height * videoAspectRatio).roundToInt()
//                    }
//                    else { // Y axis has black area.
//                        params.height = (params.width / videoAspectRatio).roundToInt()
//                    }
//                }
//            }
//        }
//    }

}
