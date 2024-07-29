package com.iwedia.cltv.config

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.content_provider.Contract
import org.json.JSONArray
import org.json.JSONObject
import java.io.*

//todo remove those constants when those values are updated in OEM.
const val SELECTOR_COLOR: String = "#E8F0FE"
const val PROGRESS_COLOR: String = "#8AB4F8"
const val BACKGROUND_COLOR: String = "#202124"
const val MAIN_TEXT_COLOR: String = "#F8F9FA"
const val DESCRIPTION_TEXT_COLOR: String = "#BDC1C6"
const val BUTTON_COLOR: String = "#1AE8EAED"
const val ICONS_COLOR: String = "#1A73E8"

/**
 * Brand color manager
 *
 * @author Veljko Ilkic
 */
class ConfigColorManager(context: Context){

    companion object {

        private var colorBackground: String = ""
        private var colorNotSelected: String = ""
        private var colorTextDescription: String = ""
        private var colorMainText: String = ""
        private var colorSelector: String = ""
        private var colorProgress: String = ""
        private var colorPvrAndOther: String = ""
        private var colorButton: String = ""
        private var colorGradient: String = ""
        private var colorDark: String = ""
        private var colorPreferenceChecked: String = ""
        val alfa_zero_per: String = "#00"
        val alfa_fifty_per : String = "#7F"
        val alfa_sixty_per : String = "#99"
        val alfa_hundred_per: String = "#ff"
        val alfa_ten_per: String = "#1A"
        val alfa_light: String = "#4d"
        val alfa_light_bg: String = "#e6"
        val alfa_light_bg_cl: String = "#B3"
        val alfa_full: String = "#ff"
        val alfa_86: String = "#DB"
        val alfa_75: String = "#BF"
        const val alfa_97: String = "#F7"

        /** Module provider */
        private lateinit var utilsInterface: UtilsInterface

        @RequiresApi(Build.VERSION_CODES.R)
        fun setup(utilsInterface: UtilsInterface) {
            this.utilsInterface = utilsInterface
            parseColors()
        }

        @RequiresApi(Build.VERSION_CODES.R)
        @SuppressLint("Range")
        private fun parseColors() {
            colorBackground = readFromJson(Contract.OemCustomization.COLOR_BACKGROUND_COLUMN)
            colorNotSelected = readFromJson(Contract.OemCustomization.COLOR_NOT_SELECTED_COLUMN)
            colorTextDescription = readFromJson(Contract.OemCustomization.COLOR_TEXT_DESCRIPTION_COLUMN)
            colorMainText = readFromJson(Contract.OemCustomization.COLOR_MAIN_TEXT_COLUMN)
            colorSelector = readFromJson(Contract.OemCustomization.COLOR_SELECTOR_COLUMN)
            colorProgress = readFromJson(Contract.OemCustomization.COLOR_PROGRESS_COLUMN)
            colorPvrAndOther = readFromJson(Contract.OemCustomization.COLOR_PVR_AND_OTHER_COLUMN)
            colorGradient = readFromJson(Contract.OemCustomization.COLOR_GRADIENT_COLUMN)
            colorDark = readFromJson(Contract.OemCustomization.COLOR_DARK_COLUMN)
            colorPreferenceChecked = readFromJson(Contract.OemCustomization.COLOR_PREFERENCE_CHECKED)
            colorButton = readFromJson(Contract.OemCustomization.COLOR_BUTTON_COLUMN)
            if (
                colorBackground.isEmpty()
                || colorNotSelected.isEmpty()
                || colorTextDescription.isEmpty()
                || colorMainText.isEmpty()
                || colorSelector.isEmpty()
                || colorProgress.isEmpty()
                || colorGradient.isEmpty()
                || colorButton.isEmpty()
                || colorDark.isEmpty()
                || colorPreferenceChecked.isEmpty()
            ) {
                colorBackground = "#000000"
                colorNotSelected = "#2b2b2d"
                colorTextDescription = "#b7b7b7"
                colorMainText = "#eeeeee"
                colorSelector = "#ffb92e"
                colorProgress = "#3e50ee"
                colorPvrAndOther = "#f22c2c"
                colorButton = "#1AE8EAED"
                colorGradient = "#ffffff"
                colorDark = "#293241"
                colorPreferenceChecked = "#4285f4"
            }
        }

        @RequiresApi(Build.VERSION_CODES.R)
        private fun readFromJson(name: String): String {
            val iS = ReferenceApplication.applicationContext().classLoader.getResourceAsStream(utilsInterface.jsonPath)
            val buffer = ByteArray(iS.available())
            iS.read(buffer)
            val oS = ByteArrayOutputStream()
            oS.write(buffer)
            oS.close()
            iS.close()
            val json = JSONObject(String(buffer))
            val scanArray: JSONArray = json.getJSONArray("oem")
            var returnString: String = ""
            for (i in 0 until scanArray.length()) {
                val scanObject: JSONObject = scanArray.getJSONObject(i)
                returnString = scanObject.getString(name)
            }
            return returnString
        }

        fun getColor(colorName: String): String {
            when (colorName) {
                "color_background" -> {
                    return colorBackground
                }
                "color_not_selected" -> {
                    return colorNotSelected
                }
                "color_text_description" -> {
                    return colorTextDescription
                }
                "color_main_text" -> {
                    return colorMainText
                }
                "color_selector" -> {
                    return colorSelector
                }
                "color_progress" -> {
                    return colorProgress
                }
                "color_pvr_and_other" -> {
                    return colorPvrAndOther
                }
                "color_button" ->{
                    return colorButton
                }
                "color_gradient" ->{
                    return colorGradient
                }
                "color_dark" -> {
                    return colorDark
                }
                "color_preference_checked" -> {
                    return colorPreferenceChecked
                }
                else -> return ""
            }
        }

        fun generateSelectorDrawable(): Drawable {
            val gd = GradientDrawable()
            gd.setColor(Color.parseColor(colorBackground))//todo
            gd.cornerRadius = 15f
            gd.setStroke(3, Color.parseColor(colorSelector))
            return gd
        }

        fun generateBackground(): Drawable {
            val gd = GradientDrawable()
            gd.setColor(Color.parseColor(colorSelector))
            gd.cornerRadius = 15f
            return gd
        }

        fun generateGuideFocusDrawable(): Drawable {
            val gd = GradientDrawable()
            gd.cornerRadius = 2.5f
            gd.setColor(Color.parseColor(colorSelector))
            return gd
        }

        fun generateGuideEventListItemDrawable(
            context: Context?,
            isFocused: Boolean,
            isCenter: Boolean = false,
            isLeft: Boolean = false
        ): Drawable {
            val gd = if (isCenter) {
                GradientDrawable()
            } else {
                context?.let {
                    ContextCompat.getDrawable(
                        it,
                        if (isLeft)
                            R.drawable.left_rounded_corner_background
                        else
                            R.drawable.right_rounded_corner_background
                    )?.mutate() as GradientDrawable
                } ?: GradientDrawable()
            }
            if (isFocused) {
                gd.setColor(Color.parseColor(colorSelector))
            } else {
                gd.setColor(Color.parseColor(colorNotSelected))
                gd.alpha = 77
            }
            return gd
        }

        fun generateGuideDrawable(): Drawable {
            val gd = GradientDrawable()
            gd.setColor(Color.parseColor(colorNotSelected))
            gd.cornerRadius = 2.5f
            gd.alpha = 77
            return gd
        }

        fun generateGuideSelectedDrawable(): Drawable {
            val gd = GradientDrawable()
            gd.setColor(Color.parseColor(colorTextDescription))
            gd.cornerRadius = 2.5f
            gd.alpha = 77
            return gd
        }

        fun generateButtonBackground(): Drawable {
            return generateButtonBackgroundWithColor(Color.parseColor(getColor("color_selector")))
        }

        fun generateButtonNotFocusedBackground(): Drawable {
            return generateButtonBackgroundWithColor(
                Color.parseColor(getColor("color_button"))
            )
        }

        private fun generateButtonBackgroundWithColor(color: Int): Drawable {
            val gd = GradientDrawable()
            gd.setColor(color)
            gd.cornerRadius = 40f
            return gd
        }

        fun generateBackground(color: String, radius: Float, opacity: Double): Drawable {
            val gd = GradientDrawable()
            gd.setColor(Color.parseColor(getColor(getColor(color), opacity)))
            gd.cornerRadius = radius
            return gd
        }

        /**
         * Get color from resource id
         *
         * @param context    Context
         * @param resourceId Resource ID
         * @param opacity    Opacity
         * @return Color
         */
        fun getColor(colorHexValue: String, opacity: Double): String {
            var color = colorHexValue
            if (opacity in 0.0..1.0) {
                color = addAlpha(colorHexValue, opacity)
            }
            return color
        }

        /**
         * Add alpha
         *
         * @param originalColor Original hex color string
         * @param alpha         Alpha from 0 to 1
         * @return Nex hex color string including alpha
         */
        fun addAlpha(originalColor: String, alpha: Double): String {
            var originalColor = originalColor
            val alphaFixed = Math.round(alpha * 255)
            var alphaHex = java.lang.Long.toHexString(alphaFixed)
            if (alphaHex.length == 1) {
                alphaHex = "0$alphaHex"
            }
            originalColor = originalColor.replace("#", "#$alphaHex")
            return originalColor
        }
    }
}