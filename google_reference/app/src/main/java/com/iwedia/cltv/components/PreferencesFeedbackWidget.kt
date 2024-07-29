package com.iwedia.cltv.components

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.PreferencesTermsOfServiceInformation
import world.widget.GWidget
import world.widget.GWidgetListener

/**
 * Preferences menu feedback widget
 * Contains information and instructions how to send feedback about Live TVx application
 *
 * @author Dejan Nadj
 */
class PreferencesFeedbackWidget :
    GWidget<ConstraintLayout, GWidgetListener> {

    private val E_MAIL = "support@cltv.dev"
    var context: Context? = null

    var zerothColumn = mutableListOf<PrefItem<Any>>()

    var information: PreferencesTermsOfServiceInformation? = null

    constructor(
        context: Context,
        listener: GWidgetListener
    ) : super(
        ReferenceWorldHandler.WidgetId.FEEDBACK,
        ReferenceWorldHandler.WidgetId.FEEDBACK,
        listener
    ) {
        this.context = context

        view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.preferences_feedback_layout, null) as ConstraintLayout

        view!!.findViewById<TextView>(R.id.emailTextView).apply {
            typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_medium")
            )
            setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            text = E_MAIL
        }

        view!!.findViewById<TextView>(R.id.textView).apply {
            typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular")
            )
            setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
            text = ConfigStringsManager.getStringById("feedback_instruction_title")
        }

        view!!.findViewById<TextView>(R.id.instructionsTitleTextView).apply {
            typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular")
            )
            setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
            text = ConfigStringsManager.getStringById("feedback_text_title")
        }

        view!!.findViewById<TextView>(R.id.instructionsTextView1).apply {
            typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular")
            )
            setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
            text = ConfigStringsManager.getStringById("feedback_instruction_text_1")
        }

        view!!.findViewById<TextView>(R.id.instructionsTextView2).apply {
            typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular")
            )
            setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
            text = ConfigStringsManager.getStringById("feedback_instruction_text_2")
        }
    }
}