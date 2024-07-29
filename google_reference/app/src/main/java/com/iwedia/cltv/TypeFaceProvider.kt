package com.iwedia.cltv

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.platform.model.Constants
import java.io.File
import java.util.*

class TypeFaceProvider {
    companion object {

        /**
         * sTypeFaces holds fonts from assets
         */
        val sTypeFaces = Hashtable<String, Typeface>(4)

        val TAG = javaClass.simpleName

        /**
         * Get Type Face
         *
         * @param fontName font name
         * @return typeface
         */
        fun getTypeFace(mContext: Context, fontName: String): Typeface {

            if (sTypeFaces[fontName] != null) {
                return sTypeFaces[fontName]!!
            }


            /**
             * TODO
             * No need to support font loading from file system
             * This is a legacy code from time when oem customization was used
             */
            //val font = if(fontName.startsWith("/")) File(fontName) else File(mContext.filesDir, fontName)

            var typeFace: Typeface? = null

            /*if (font != null && font.exists()) {
                typeFace = Typeface.createFromFile(font)

                if (typeFace != null) {
                    sTypeFaces[fontName] = typeFace
                }

            } else {*/
                try {
                    val resIDFont: Int =
                        mContext.resources.getIdentifier(fontName.replace(".ttf",""), "font", mContext.packageName)
                    typeFace = ResourcesCompat.getFont(mContext, resIDFont)
                    return typeFace!!
                }catch (E: Exception){
                    //todo
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getTypeFace: $E")
                    typeFace = ResourcesCompat.getFont(mContext, getFontFromResources(fontName))!!
                }
            //}

            return typeFace!!
        }

        fun getFontFromResources(fontName: String): Int {
            when (fontName) {
                "font_regular" -> {
                    return R.font.work_sans_regular
                }
                "font_medium" -> {
                    return R.font.work_sans_medium
                }
                "font_bold" -> {
                    return R.font.work_sans_bold
                }
                "font_light" -> {
                    return R.font.work_sans_italic
                }
            }

            return R.font.work_sans_regular
        }
    }
}