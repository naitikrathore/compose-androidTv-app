package com.iwedia.cltv.fti

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.iwedia.cltv.R
import com.iwedia.cltv.SettingsActivity
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.fti.handlers.ConfigHandler
import com.iwedia.cltv.fti.scan_models.ConfigParser
import com.iwedia.cltv.fti.scan_models.DataParams
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import java.io.InputStream


class TestFragment : Fragment()  {
    companion object {
        fun newInstance() = TestFragment()
    }

    var viewModel: View? = null
    private var signalStrengthBar: ProgressBar? = null
    private var signalQualityBar: ProgressBar? = null
    private var signalQualityBarText: TextView? = null
    private var signalStrengthBarText: TextView? = null
    var startButton: Button? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("ResourceType", "SoonBlockedPrivateApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = inflater.inflate(R.layout.test_layout, container, false)
        viewModel!!.background = ContextCompat.getDrawable(context!!, R.drawable.fti_bg_gtv)

        val textViewDescription: TextView = viewModel!!.findViewById(R.id.textViewDescription)
        textViewDescription.text = ConfigStringsManager.getStringById("scan_terrestrial")
        textViewDescription!!.setTextAppearance(R.style.set_up_with)

        val textViewDescriptionText: TextView = viewModel!!.findViewById(R.id.textViewDescriptionText)
        textViewDescriptionText.text = ConfigStringsManager.getStringById("scan_terrestrial_description")
        textViewDescriptionText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)

        return viewModel!!
    }

    fun tintIndeterminateProgress(progress: ProgressBar
                                  , @ColorInt color: Int = ContextCompat.getColor(progress.context, R.color.fti_progress_front_gtv)
                                  , @ColorInt colorBackground: Int = ContextCompat.getColor(progress.context, R.color.fti_progress_bg_gtv)){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            progress.indeterminateTintList = ColorStateList.valueOf(color)

            progress.setProgressTintList(ColorStateList.valueOf(color))
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(colorBackground))
        }
    }

    fun tintHorizontalProgress(progress: ProgressBar
                               , @ColorInt color: Int = ContextCompat.getColor(progress.context, R.color.fti_progress_front_gtv)
                               , @ColorInt colorBackground: Int = ContextCompat.getColor(progress.context, R.color.fti_progress_bg_gtv)){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            progress.progressTintList = ColorStateList.valueOf(color)

            progress.setProgressTintList(ColorStateList.valueOf(color))
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(colorBackground))
        }
    }
}