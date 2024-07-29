package com.iwedia.cltv.platform.base.content_provider

import android.content.Context
import java.io.IOException
import java.io.InputStream

class LanguagesProvider constructor(val context: Context){

    fun getLanguageData(languageCode: String): String {
        var fileName = "strings-" + languageCode + ".xml"
        val inputStream: InputStream = context.classLoader.getResourceAsStream("assets/languages/$fileName")//context!!.assets.open(fileName)
        try {
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            return String(buffer)
        } catch (e: IOException) {
            // Error handling
        }
        return ""
    }
}