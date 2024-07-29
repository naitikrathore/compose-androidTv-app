package com.iwedia.cltv.config

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.content_provider.Contract
import org.json.JSONArray
import org.json.JSONObject
import java.io.*

/**
 * Brand color manager
 *
 * @author Veljko Ilkic
 */
class ConfigFontManager(context: Context){

    companion object {

        private var font_regular: String = ""
        private var font_medium: String = ""
        private var font_bold: String = ""
        private var font_light: String = ""

        /** Module provider */
        private lateinit var utilsInterface: UtilsInterface

        @RequiresApi(Build.VERSION_CODES.R)
        fun setup(utilsInterface: UtilsInterface) {
            this.utilsInterface = utilsInterface
            parseFonts()
        }

        @RequiresApi(Build.VERSION_CODES.R)
        @SuppressLint("Range")
        private fun parseFonts() {
            font_regular = readFromJson(Contract.OemCustomization.FONT_REGULAR)
            font_medium = readFromJson(Contract.OemCustomization.FONT_MEDIUM)
            font_bold = readFromJson(Contract.OemCustomization.FONT_BOLD)
            font_light = readFromJson(Contract.OemCustomization.FONT_LIGHT)
            if (font_regular.isEmpty()  || font_medium.isEmpty() || font_bold.isEmpty() || font_light.isEmpty()) {
                font_regular = "work_sans_regular.ttf"
                font_medium = "work_sans_medium.ttf"
                font_bold = "work_sans_bold.ttf"
                font_light = "work_sans_light.ttf"
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

        fun getFont(fontName: String): String {
            when (fontName) {
                "font_regular" -> {
                    return font_regular
                }
                "font_medium" -> {
                    return font_medium
                }
                "font_bold" -> {
                    return font_bold
                }
                "font_light" -> {
                    return font_light
                }
            }

            return font_regular
        }
    }
}