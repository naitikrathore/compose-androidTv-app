
package com.iwedia.cltv.components
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.applicationContext
import com.iwedia.cltv.ReferenceDrawableButton
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider.Companion.getTypeFace
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigColorManager.Companion.getColor
import com.iwedia.cltv.config.ConfigFontManager.Companion.getFont
import com.iwedia.cltv.config.ConfigStringsManager.Companion.getStringById
import com.iwedia.cltv.platform.model.Constants
import world.widget.GWidget
import world.widget.GWidgetListener
/**
 * Preferences Speed Test Widget
 *
 * @author Dejan Nadj
 */
class SpeedTestWidget : GWidget<ViewGroup, SpeedTestWidget.SpeedTestWidgetListener> {
    /**
     * Widget background
     */
    private var backgroundLayout: RelativeLayout? = null

    /**
     * Progress bar
     */
    private var progressBar: ProgressBar? = null
    /**
     * Progress bar percentage indicator
     */
    private var progressValue: TextView? = null
    /**
     * Title
     */
    private var title: TextView? = null
    /**
     * Progress description text view
     */
    private var progressDescriptionTv: TextView? = null

    /**
     * Done button
     */
    private var doneButton: ReferenceDrawableButton? = null
    val TAG = javaClass.simpleName
    constructor(
        context: Context,
        listener: SpeedTestWidgetListener
    ) : super(
        ReferenceWorldHandler.WidgetId.SPEED_TEST,
        ReferenceWorldHandler.WidgetId.SPEED_TEST,
        listener
    ) {
        view = LayoutInflater.from(context).inflate(R.layout.layout_widget_speed_test, null) as RelativeLayout
        backgroundLayout = view!!.findViewById(R.id.background_layout)
        backgroundLayout!!.setBackgroundColor(
            Color.parseColor(
                getColor("color_background").replace("#", ConfigColorManager.alfa_full)
            )
        )
        title = view!!.findViewById(R.id.title)
        title!!.setTextColor(Color.parseColor(getColor("color_main_text")))
        title!!.setTypeface(getTypeFace(applicationContext(), getFont("font_medium")))
        title!!.text = getStringById("speed_test_title")
        //Text bellow progress bar
        progressDescriptionTv = view!!.findViewById(R.id.scanning_progress_description)
        progressDescriptionTv!!.text = getStringById("dont_unplug_device")
        progressDescriptionTv!!.setTextColor(Color.parseColor(getColor("color_main_text")))
        progressDescriptionTv!!.setTypeface(
            getTypeFace(
                applicationContext(),
                getFont("font_regular")
            )
        )
        //Percentage text indicator
        progressValue = view!!.findViewById(R.id.progress_value)
        progressValue!!.setTextColor(Color.parseColor(getColor("color_main_text")))
        progressValue!!.setTypeface(
            getTypeFace(
                applicationContext(),
                getFont("font_lignt")
            )
        )
        //Scanning progress bar
        progressBar = view!!.findViewById(R.id.progress_bar)
        progressBar!!.progressTintList = ColorStateList.valueOf(
            Color.parseColor(
                getColor("color_progress")
            )
        )
        try {
            progressBar!!.progressBackgroundTintList =
                ColorStateList.valueOf(Color.parseColor(getColor("color_text_description")))
        } catch (ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Constructor: Exception color rdb $ex")
        }
        doneButton = view!!.findViewById(R.id.skip_button)
        doneButton!!.setText(getStringById("done"))
        doneButton!!.getTextView().textSize = 15f
        doneButton!!.getTextView().setTypeface(getTypeFace(applicationContext(), getFont("font_medium")))
        doneButton!!.setOnClickListener {
            listener.onBackPress()
        }
        doneButton!!.background = ContextCompat.getDrawable(
            context,
            R.drawable.focus_shape
        )
        doneButton!!.backgroundTintList  = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_selector")))
        try {
            val color_context = Color.parseColor(getColor("color_background"))
            doneButton!!.getTextView().setTextColor(color_context)
        } catch (ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex)
        }
    }

    override fun refresh(data: Any) {
        super.refresh(data)
        if (data is Int) {
            progressBar!!.progress = data
            progressValue!!.setText("$data%")
        }
    }
    fun onFinish(speed : Float?) {
        ReferenceApplication.runOnUiThread {
            doneButton!!.visibility = View.VISIBLE
            doneButton!!.requestFocus()
            progressValue!!.visibility = View.GONE
            progressBar!!.visibility = View.GONE
            title!!.text = getStringById("speed_test_complete_title")
            if (speed == null) {
                progressDescriptionTv!!.text = getStringById("speed_test_failed")
            } else {
                var message = getStringById("max_speed_display")
                message = message.replace("%s", String.format("%3.1f MB/S", speed))
                progressDescriptionTv!!.text = message
                progressBar!!.progress = 100
                progressValue!!.text = "100%"
            }
        }
    }
    interface SpeedTestWidgetListener : GWidgetListener {
        fun onBackPress()
    }
}
