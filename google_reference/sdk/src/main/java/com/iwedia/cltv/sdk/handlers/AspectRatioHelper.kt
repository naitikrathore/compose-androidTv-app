package com.iwedia.cltv.sdk.handlers

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.media.tv.TvView
import android.util.Log
import android.view.Display
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.iwedia.cltv.sdk.DisplayMode
import com.iwedia.cltv.sdk.ReferenceSdk
import kotlin.math.abs
import kotlin.math.roundToInt
/**
 *
 * Aspect ratio calculation mechanism
 * Here we are updating and calculating margin and layout params to change liveTVView's size.
 *
 * @author Aditya Pise
 *
 * **/
public class AspectRatioHelper {

    private val VIDEO_SD_WIDTH = 704
    private val VIDEO_SD_HEIGHT = 480
    private val VIDEO_HD_WIDTH = 1280
    private val VIDEO_HD_HEIGHT = 720
    private val VIDEO_FULL_HD_WIDTH = 1920
    private val VIDEO_FULL_HD_HEIGHT = 1080
    private val VIDEO_ULTRA_HD_WIDTH = 2048
    private val VIDEO_ULTRA_HD_HEIGHT = 1536
    var VIDEO_DEFINITION_LEVEL_UNKNOWN = 0
    var VIDEO_DEFINITION_LEVEL_SD = 1
    var VIDEO_DEFINITION_LEVEL_HD = 2
    var VIDEO_DEFINITION_LEVEL_FULL_HD = 3
    var VIDEO_DEFINITION_LEVEL_ULTRA_HD = 4
    private val DISPLAY_MODE_EPSILON = 0.001f
    var checkScaleType = 1
    val TAG = javaClass.simpleName

    /**
     * Calculates the width and height of device.
     **/
    public fun getDisplaySize(): Point {
        val displayManager: DisplayManager =
            ReferenceSdk.context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display: Display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val size = Point()
        display.getSize(size)
        return size
    }

    /**
     * Create RelativeLayout layout params for liveTVView parent view
     **/
    public fun createMarginLayoutParams(
        startMargin: Int, endMargin: Int, topMargin: Int, bottomMargin: Int
    , mWindowWidth: Int, mWindowHeight: Int): RelativeLayout.LayoutParams {
        val lp = RelativeLayout.LayoutParams(0, 0)
        lp.marginStart = startMargin
        lp.marginEnd = endMargin
        lp.topMargin = topMargin
        lp.bottomMargin = bottomMargin
        lp.width = mWindowWidth!! - startMargin - endMargin
        lp.height = mWindowHeight!! - topMargin - bottomMargin
        return lp
    }


    /**
     * returns definition for video (SD/HD) as per video height width
     * @param width video width
     * @param height video height
     *
     **/
    fun getVideoDefinitionLevelFromSize(width: Int, height: Int): Int {
        if (width >= VIDEO_ULTRA_HD_WIDTH && height >= VIDEO_ULTRA_HD_HEIGHT) {
            return VIDEO_DEFINITION_LEVEL_ULTRA_HD
        } else if (width >= VIDEO_FULL_HD_WIDTH && height >= VIDEO_FULL_HD_HEIGHT) {
            return VIDEO_DEFINITION_LEVEL_FULL_HD
        } else if (width >= VIDEO_HD_WIDTH && height >= VIDEO_HD_HEIGHT) {
            return VIDEO_DEFINITION_LEVEL_HD
        } else if (width >= VIDEO_SD_WIDTH && height >= VIDEO_SD_HEIGHT) {
            return VIDEO_DEFINITION_LEVEL_SD
        }
        return VIDEO_DEFINITION_LEVEL_UNKNOWN
    }

    /**
     * Set appropriate width and height for final display as per display mode and aspect ratio
     *
     * @param displayMode               display mode (NORMAL, FULL, ZOOM)
     * @param videoDisplayAspectRatio   video aspect ratio
     * @param availableAreaRatio        available aspect ration of display
     * @param availableAreaWidth        available width after margin
     * @param availableAreaHeight       available height after margin calculation
     * */
    fun getLayoutParamsForDisplayMode(displayMode: Int, videoDisplayAspectRatio: Float, availableAreaRatio: Float,
                                      availableAreaWidth: Int, availableAreaHeight: Int): RelativeLayout.LayoutParams{

        val layoutParams: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(0, 0)
        when (displayMode) {
            DisplayMode.MODE_ZOOM -> if (videoDisplayAspectRatio < availableAreaRatio) {
                // Y axis will be clipped.
                layoutParams.width = availableAreaWidth
                layoutParams.height = (availableAreaWidth / videoDisplayAspectRatio).roundToInt()
                checkScaleType = 1;

            } else {
                // X axis will be clipped.
                layoutParams.width = (availableAreaHeight * videoDisplayAspectRatio).roundToInt()
                layoutParams.height = availableAreaHeight
                checkScaleType = 0
            }
            DisplayMode.MODE_NORMAL -> if (videoDisplayAspectRatio < availableAreaRatio) {
                // X axis has black area.
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getLayoutParamsForDisplayMode:  DISPLAMODE if - $availableAreaWidth -- ${layoutParams.height}")
                layoutParams.width = (availableAreaHeight * videoDisplayAspectRatio).roundToInt()
                layoutParams.height = availableAreaHeight
            } else {
                // Y axis has black area.
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getLayoutParamsForDisplayMode: DISPLAMODE else - $availableAreaWidth -- ${layoutParams.height}")
                layoutParams.width = availableAreaWidth
                layoutParams.height = (availableAreaWidth / videoDisplayAspectRatio).roundToInt()
            }
            DisplayMode.MODE_FULL -> {
                layoutParams.width = availableAreaWidth
                layoutParams.height = availableAreaHeight
            }
            else -> {
                layoutParams.width = availableAreaWidth
                layoutParams.height = availableAreaHeight
            }
        }

        val marginStart = (availableAreaWidth - layoutParams.width) / 2
        layoutParams.marginStart = marginStart
        return layoutParams
    }


    fun interpolateMargins(
        out: ViewGroup.MarginLayoutParams,
        startValue: ViewGroup.MarginLayoutParams,
        endValue: ViewGroup.MarginLayoutParams,
        fraction: Float
    ) {
        out.topMargin = interpolate(startValue.topMargin, endValue.topMargin, fraction)
        out.bottomMargin = interpolate(startValue.bottomMargin, endValue.bottomMargin, fraction)
        out.setMarginStart(
            interpolate(startValue.getMarginStart(), endValue.getMarginStart(), fraction)
        )
        out.setMarginEnd(interpolate(startValue.getMarginEnd(), endValue.getMarginEnd(), fraction))
        out.width = interpolate(startValue.width, endValue.width, fraction)
        out.height = interpolate(startValue.height, endValue.height, fraction)
    }

    private fun interpolate(start: Int, end: Int, fraction: Float): Int {
        return (start + (end - start) * fraction).toInt()
    }

    /**
     * Check if video support display mode as per video height and width
     * difference between view aspect ratio and video aspect ratio should be (abs) >= 0.001
     *
     * @param displayMode               display mode selected (FULL, NORMAL, ZOOM)
     * @param videoDisplayAspectRatio   video aspect ratio
     * @param liveTvView                TV UI
     * */
    fun isDisplayModeAvailable(displayMode: Int, videoDisplayAspectRatio: Float,
                               liveTvView: TvView): Boolean {
        val viewWidth: Int = liveTvView!!.getWidth()
        val viewHeight: Int = liveTvView!!.getHeight()
        if (displayMode == DisplayMode.MODE_FULL) {
            return true
        }
        if (viewWidth <= 0 || viewHeight <= 0 || videoDisplayAspectRatio <= 0f) {
            return false
        }
        val viewRatio = viewWidth / viewHeight.toFloat()
        return abs(viewRatio - videoDisplayAspectRatio) >= DISPLAY_MODE_EPSILON
    }

}