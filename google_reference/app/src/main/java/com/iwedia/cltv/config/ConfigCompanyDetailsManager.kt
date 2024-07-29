package com.iwedia.cltv.config

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import android.graphics.drawable.BitmapDrawable
import com.iwedia.cltv.BuildConfig
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.content_provider.Contract
import org.json.JSONArray
import org.json.JSONObject
import java.io.*

/**
 * Company info manager
 *
 * @author Dragan Krnjaic
 */
class ConfigCompanyDetailsManager(context: Context) {

    companion object {
        private var companyName: String = ""
        private var companyLogo: String = ""
        private var welcomeMessage: String = ""

        /** Module provider */
        private lateinit var utilsInterface: UtilsInterface

        @RequiresApi(Build.VERSION_CODES.R)
        fun setup(utilsInterface: UtilsInterface) {
            this.utilsInterface = utilsInterface
            parseCompanyInfo()
        }

        @SuppressLint("UseCompatLoadingForDrawables", "ResourceType")
        private fun createCompanyLogo(context: Context): ByteArray {
            var imageId=if(BuildConfig.FLAVOR.contains("mt5") ) R.raw.company_logo else R.raw.cltv_logo
            var logo = (context.resources.getDrawable(imageId, null) as BitmapDrawable).bitmap
            var bos = ByteArrayOutputStream()
            logo.compress(Bitmap.CompressFormat.JPEG, 100, bos)
            return bos.toByteArray()
        }

        @RequiresApi(Build.VERSION_CODES.R)
        @SuppressLint("Range")
        private fun parseCompanyInfo() {

            companyName = readFromJson(Contract.OemCustomization.BRANDING_COMPANY_NAME_COLUMN)
            welcomeMessage = readFromJson(Contract.OemCustomization.BRANDING_WELCOME_MESSAGE_COLUMN)

            val companyLogoJsonText = readFromJson(Contract.OemCustomization.BRANDING_CHANNEL_LOGO_COLUMN)
            if (companyLogoJsonText.contains("//")) {
                companyLogo = "https:$companyLogoJsonText"
            } else{
                companyLogo = "R.raw.$companyLogoJsonText"
            }

            if (companyName.isEmpty() || companyLogo.isEmpty() || welcomeMessage.isEmpty()) {
                companyName = "iWedia"
                if(BuildConfig.FLAVOR.contains("mtk")) {
                    companyLogo = "R.raw.company_logo"
                }else{
                    companyLogo = "R.raw.cltv_logo"
                }
                welcomeMessage = "Welcome to Reference+ application"
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

        fun getCompanyDetailsInfo(companyParam: String): String {
            when (companyParam) {
                "company_name" -> {
                    return if (((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isFastOnly()) "Common Live TV" else companyName
                }
                "company_logo" -> {
                    return companyLogo
                }
                "welcome_message" -> {
                    return if (((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isFastOnly()) "Welcome" else welcomeMessage
                }
                "cltv_logo" -> {
                    return "R.raw.cltv_logo"
                }

                else -> return ""
            }
        }
    }
}