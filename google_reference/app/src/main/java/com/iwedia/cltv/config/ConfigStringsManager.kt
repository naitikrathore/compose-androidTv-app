package com.iwedia.cltv.config

import android.content.Context
import android.util.Log
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.utils.Utils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.*
import java.util.Locale

/**
 * Multilanguage manager
 *
 * @author Dragan Krnjaic
 */
class ConfigStringsManager(context: Context) {

    companion object {

        const val DEFAULT_STRINGS_NAME = "strings-Default.xml"
        private val TAG = "ConfigStringsManager"

        private var strings: HashMap<String, String> = HashMap()
        private var stringsDefault: HashMap<String, String> = HashMap()

        fun setup(languageCode: String) {
            var newLanguageCode = languageCode
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Locale.getDefault().country = ${Locale.getDefault().country} || languageCode = ${languageCode}")
            //this is done since mtk mw gives for canada return value EN_USA
            if(Locale.getDefault().country != languageCode){
                if(languageCode == "EN_USA"){
                    if(Locale.getDefault().country.equals("CA")){
                        newLanguageCode = "EN_CA"
                    }
                }
            }
            parseStringsFromAssets(newLanguageCode)
        }

        private fun parseStringsFromAssets(languageCode: String){
            val pullParserFactory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
            try {
                var inputStream = ReferenceApplication.get().context!!.assets.open(DEFAULT_STRINGS_NAME)
                var parser = pullParserFactory.newPullParser().apply {
                    setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    setInput(inputStream, null)
                }
                stringsDefault = Utils.parseXML(parser)
                var fileName = "strings-$languageCode.xml"
                try {
                    inputStream = ReferenceApplication.get().context!!.assets.open("languages/$fileName")
                } catch (e: FileNotFoundException) {
                    val languageOnlyCode = languageCode.split("_").first()
                    fileName = "strings-$languageOnlyCode.xml"
                    try {
                        inputStream = ReferenceApplication.get().context!!.assets.open("languages/$fileName")
                    } catch (e: FileNotFoundException) {
                        strings = HashMap(stringsDefault)
                        return
                    }
                }
                parser = pullParserFactory.newPullParser().apply {
                    setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    setInput(inputStream, null)
                }
                strings = Utils.parseXML(parser)
            } catch (e: Exception) {
                strings = if (stringsDefault.isEmpty()) {
                    val inputStream = ReferenceApplication.get().context!!.assets.open(DEFAULT_STRINGS_NAME)
                    val parser = pullParserFactory.newPullParser().apply {
                        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                        setInput(inputStream, null)
                    }
                    Utils.parseXML(parser)
                } else {
                    HashMap(stringsDefault)
                }
            }
        }

        /**
         * Get string by id key
         */
        fun getStringById(key: String): String {
            var ret = ""
            ret = strings.get(key).toString().replace("\\n", "\n")
            if (ret == "null" || ret.isEmpty()) {
                ret = stringsDefault.get(key).toString().replace("\\n", "\n")
                if (ret == "null" || ret.isEmpty()) {
                    ret = key.replace("_", " ").replaceFirstChar(Char::uppercaseChar)
                }
            }
            return ret.replace("\"", "")
        }

        fun getSystemLanguageString(defaultValue: String): String {
            val keys = stringsDefault.filterValues { it == defaultValue }.keys
            if (keys.isEmpty()) {
                return defaultValue
            }
            val key = keys.first()
            if(!strings.contains(key)){
                return defaultValue
            }
            return strings[key]!!
        }
    }
}