package com.iwedia.cltv.components

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.BuildConfig
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigColorManager.Companion.getColor
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.model.ReferenceSystemInformation
import com.iwedia.cltv.utils.Utils
import world.widget.GWidget
import world.widget.GWidgetListener

class PreferencesSystemInformationWidget :
    GWidget<ConstraintLayout,GWidgetListener> {

    private var signalStrengthProgressView: com.iwedia.cltv.scan_activity.core.ProgressBarView? = null
    private var signalQualityProgressView: com.iwedia.cltv.scan_activity.core.ProgressBarView? = null
    private var preferencesSystemInformationAdapter: PreferencesSystemInformationAdapter? = null
    private var systemInformationListener: PreferencesSystemInformationListener? = null

    @SuppressLint("SuspiciousIndentation")
    constructor(
        listener: PreferencesSystemInformationListener
    ) : super(
        ReferenceWorldHandler.WidgetId.PREFERENCES_SYSTEM_INFORMATION,
        ReferenceWorldHandler.WidgetId.PREFERENCES_SYSTEM_INFORMATION,
        listener
    ) {
        view = LayoutInflater.from(ReferenceApplication.applicationContext())
           .inflate(R.layout.preferences_system_information_layout, null) as ConstraintLayout

        preferencesSystemInformationAdapter = PreferencesSystemInformationAdapter()
        var verticalGridView: VerticalGridView? = view?.findViewById(R.id.si_grid_view)
        verticalGridView!!.setNumColumns(2)
        verticalGridView!!.setItemSpacing(30)
        verticalGridView!!.adapter = preferencesSystemInformationAdapter


        //Init signal strength/quality progress bar views
        signalStrengthProgressView =
            view!!.findViewById<View>(R.id.signal_strength_progress_view) as com.iwedia.cltv.scan_activity.core.ProgressBarView
        signalStrengthProgressView?.setTitle(ConfigStringsManager.getStringById("signal_strength"))

        var layoutParamsStrength = signalStrengthProgressView?.findViewById<ProgressBar>(R.id.progress_bar)?.layoutParams as RelativeLayout.LayoutParams
        layoutParamsStrength.setMargins(0,Utils.getDimensInPixelSize(R.dimen.custom_dim_5), 0, 0)
        var layoutParamsStrengthText = signalStrengthProgressView?.findViewById<TextView>(R.id.progress_text)?.layoutParams as RelativeLayout.LayoutParams
        layoutParamsStrengthText.setMargins(0,Utils.getDimensInPixelSize(R.dimen.custom_dim_17), 0, 0)
        signalStrengthProgressView?.findViewById<TextView>(R.id.title_tv)!!.typeface=
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_light")
            )
        signalStrengthProgressView?.findViewById<TextView>(R.id.progress_text)!!.typeface=
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )

        signalStrengthProgressView?.requestLayout()
        signalStrengthProgressView?.setProgress(80)
        signalStrengthProgressView?.progressBar?.progressTintList = ColorStateList.valueOf(Color.parseColor(getColor("color_progress")))

        signalQualityProgressView =
            view!!.findViewById<View>(R.id.signal_quality_progress_view) as com.iwedia.cltv.scan_activity.core.ProgressBarView
        signalQualityProgressView?.setTitle(ConfigStringsManager.getStringById("signal_quality"))
        var layoutParamsQuality = signalQualityProgressView?.findViewById<ProgressBar>(R.id.progress_bar)?.layoutParams as RelativeLayout.LayoutParams
        layoutParamsQuality.setMargins(0,Utils.getDimensInPixelSize(R.dimen.custom_dim_5), 0, 0)
        var layoutParamsQualityText = signalQualityProgressView?.findViewById<TextView>(R.id.progress_text)?.layoutParams as RelativeLayout.LayoutParams
        layoutParamsQualityText.setMargins(0,Utils.getDimensInPixelSize(R.dimen.custom_dim_17), 0, 0)
        signalQualityProgressView?.requestLayout()
        signalQualityProgressView?.setProgress(90)
        signalQualityProgressView?.progressBar?.progressTintList = ColorStateList.valueOf(Color.parseColor(getColor("color_progress")))
        signalQualityProgressView?.findViewById<TextView>(R.id.title_tv)!!.typeface=
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_light")
            )
        signalQualityProgressView?.findViewById<TextView>(R.id.progress_text)!!.typeface=
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )
        systemInformationListener = listener
    }

    private fun initTextContainer(container: ConstraintLayout?, title: String) {
        container!!.findViewById<TextView>(R.id.title)!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )

        container!!.findViewById<TextView>(R.id.title)!!
            .setTextColor(Color.parseColor(ConfigColorManager.getColor(ConfigColorManager.getColor("color_main_text"), 0.8)))

        container!!.findViewById<TextView>(R.id.title)!!.text = title

        container!!.findViewById<TextView>(R.id.content)!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )

        container!!.findViewById<TextView>(R.id.content)!!
            .setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
    }

    @SuppressLint("SuspiciousIndentation")
    override fun refresh(data: Any) {
        super.refresh(data)
        if (data is ReferenceSystemInformation) {
            var adapterItems: MutableList<PreferencesSystemInformationData> = mutableListOf()
            //Set data
            if (systemInformationListener!!.getConfigInfo("rf_channel_number_enabled"))
            adapterItems.add(PreferencesSystemInformationData(ConfigStringsManager.getStringById("rf_channel_number"),data.rfChannelNumber))
            if (systemInformationListener!!.getConfigInfo("ber_enabled"))
            adapterItems.add(PreferencesSystemInformationData(ConfigStringsManager.getStringById("ber"),data.ber))
            if (systemInformationListener!!.getConfigInfo("frequency_enabled"))
            adapterItems.add(PreferencesSystemInformationData(ConfigStringsManager.getStringById("frequency"),data.frequency))
            if (systemInformationListener!!.getConfigInfo("prog_enabled"))
            adapterItems.add(PreferencesSystemInformationData(ConfigStringsManager.getStringById("prog"),data.prog))

            if (!isGretzkyBoard()) {
                if (systemInformationListener!!.getConfigInfo("uec_enabled"))
                adapterItems.add(PreferencesSystemInformationData(ConfigStringsManager.getStringById("uec"),data.uec))
            }

            if (systemInformationListener!!.getConfigInfo("serviceId_enabled"))
            adapterItems.add(PreferencesSystemInformationData(ConfigStringsManager.getStringById("serviceId"),data.serviceId))
            if (!isGretzkyBoard()) {
                if (systemInformationListener!!.getConfigInfo("postViterbi_enabled"))
                adapterItems.add(PreferencesSystemInformationData(ConfigStringsManager.getStringById("postViterbi"),data.postViterbi))
            }

            if (systemInformationListener!!.getConfigInfo("tsId_enabled"))
            adapterItems.add(PreferencesSystemInformationData(ConfigStringsManager.getStringById("tsId"),data.tsId))
            if (!isGretzkyBoard()) {
                if (systemInformationListener!!.getConfigInfo("fiveS_enabled"))
                adapterItems.add(PreferencesSystemInformationData(ConfigStringsManager.getStringById("fiveS"),data.fiveS))
            }

            if (systemInformationListener!!.getConfigInfo("onId_enabled"))
            adapterItems.add(PreferencesSystemInformationData(ConfigStringsManager.getStringById("onId"),data.onId))
            if (!isGretzkyBoard()) { //0
                if (systemInformationListener!!.getConfigInfo("agc_enabled"))
                adapterItems.add(PreferencesSystemInformationData(ConfigStringsManager.getStringById("agc"),data.agc))
            }

            if (systemInformationListener!!.getConfigInfo("networkId_enabled"))
            adapterItems.add(PreferencesSystemInformationData(ConfigStringsManager.getStringById("networkId"),data.networkId))
            if(!isGretzkyBoard()) {
                if (systemInformationListener!!.getConfigInfo("networkName_enabled"))
                adapterItems.add(PreferencesSystemInformationData(ConfigStringsManager.getStringById("networkName"),data.networkName))
            }

            if (systemInformationListener!!.getConfigInfo("bandwidth_enabled"))
            adapterItems.add(PreferencesSystemInformationData(ConfigStringsManager.getStringById("bandwidth"),data.bandwidth))
            preferencesSystemInformationAdapter!!.refresh(adapterItems)

            signalStrengthProgressView!!.setProgress(data.signalStrength)
            signalQualityProgressView!!.setProgress(data.signalQuality)
            signalQualityProgressView!!.progressBar.progressTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_progress")))
        }
    }

    private fun isGretzkyBoard() = BuildConfig.FLAVOR.contains("gretzky")

    interface PreferencesSystemInformationListener: GWidgetListener{
        fun getConfigInfo(nameOfInfo: String): Boolean
    }
}