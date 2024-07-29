package com.iwedia.cltv.anoki_fast

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.iwedia.cltv.R
import com.iwedia.cltv.components.welcome_screen.CustomWelcomeScreen
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.fast_backend_utils.FastTosOptInHelper
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.utils.Utils

/**
 * Shows Anoki/Fast Terms of Service screen
 * Activity is started from FastTosMoreInfo widget on More Info button click
 * Gives option to the user to accept TOS later from Discover or Live tab
 *
 * @author Dejan Nadj
 */
class FastTosMoreInfoButtonActivity : Activity() {

    private var customWelcomeScreen: CustomWelcomeScreen? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_tos_activity)
        var  noInternetInfo: FastErrorInfo = findViewById(R.id.no_internet_info)
        noInternetInfo?.apply{
            visibility = View.GONE
        }
        customWelcomeScreen = findViewById(R.id.custom_welcome_screen)
        customWelcomeScreen?.enableCancelButton()
        customWelcomeScreen?.setListener(object : CustomWelcomeScreen.Listener {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onContinueClicked() {
                FastTosOptInHelper.putTosOptInServer(applicationContext, 1){
                    if(it){
                        CoroutineHelper.runCoroutineWithDelay({
                            Utils.restartApp()
                        }, 500)
                    }
                }
            }

            override fun onCancelClicked() {
                setResult(Activity.RESULT_OK, intent)
                finish()
            }

            override fun getContext(): Context {
                return applicationContext
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {}
        })
    }
}