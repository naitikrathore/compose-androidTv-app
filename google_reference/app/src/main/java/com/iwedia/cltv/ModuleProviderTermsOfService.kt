package com.iwedia.cltv

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.ModuleFactory
import com.iwedia.cltv.platform.`interface`.TTSInterface

@RequiresApi(Build.VERSION_CODES.R)
class ModuleProviderTermsOfService(application: Application) {
    private val moduleFactory: ModuleFactory

    init {
        moduleFactory = ModuleFactory(application)
    }

    private var textToSpeechModule: TTSInterface? = null

    fun getTextToSpeechModule(): TTSInterface {
        if (textToSpeechModule == null) {
            textToSpeechModule = moduleFactory.createTextToSpeechModule()
        }
        return textToSpeechModule!!
    }
}