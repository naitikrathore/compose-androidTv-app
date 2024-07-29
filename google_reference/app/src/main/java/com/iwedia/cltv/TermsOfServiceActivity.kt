package com.iwedia.cltv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.iwedia.cltv.anoki_fast.FastErrorInfo
import com.iwedia.cltv.components.welcome_screen.CustomWelcomeScreen
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.fast_backend_utils.FastTosOptInHelper
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.concurrent.thread


/**
 * Terms of service activity
 *
 * @author Dejan Nadj
 */
class TermsOfServiceActivity : Activity() {
    companion object {
        const val TAG = "TermsOfServiceActivity"
    }

    private var customWelcomeScreen: CustomWelcomeScreen? = null
    private var noInternetInfo: FastErrorInfo? = null
    private var stopNetworkThread = false
    private var startedFromMainActivity = false
    private var inputSelected = "TV"
    private var inputTuneURL = ""
    var inputSourceSelected = "TV"
    var isBlocked: Boolean? = false
    var isTosLaunch:Boolean? =false
    private var tos = -1
    private var coroutineJob: Job?= null
    private var isPaused = false

//    /** Module provider */
//    private lateinit var moduleProviderTermsOfService: ModuleProviderTermsOfService

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreate")
//        moduleProviderTermsOfService = ModuleProviderTermsOfService(application)
        coroutineJob = GlobalScope.launch {
            if (BuildConfig.FLAVOR.contains("mal_service")) {
                InformationBus.serviceConnectionCallback = {
                    CoroutineScope(Dispatchers.IO).launch {
                        initialization()
                    }
                }
                ReferenceApplication.moduleProvider = ModuleProvider(this@TermsOfServiceActivity.application)
            } else {
                initialization()
            }
        }
    }

    private fun initialization() {
        FastTosOptInHelper.fetchTosOptInFromServer(applicationContext){
            tos = it
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "initialization: tos === $it")
            runOnUiThread {
                setContentView(R.layout.layout_tos_activity)
                noInternetInfo = findViewById(R.id.no_internet_info)
                noInternetInfo?.textToSpeechHandler!!.setupTextToSpeechTextSetterInterface(object : TTSSetterInterface {
                    @RequiresApi(Build.VERSION_CODES.R)
                    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
//                        moduleProviderTermsOfService.getTextToSpeechModule().setSpeechText(text = text, importance = importance)
                    }
                })
                customWelcomeScreen = findViewById(R.id.custom_welcome_screen)
                customWelcomeScreen?.enableCancelButton()
                customWelcomeScreen?.setListener(object : CustomWelcomeScreen.Listener {
                    @RequiresApi(Build.VERSION_CODES.R)
                    override fun onContinueClicked() {
                        CoroutineScope(Dispatchers.Default).launch {
                            FastTosOptInHelper.putTosOptInServer(applicationContext, 1){
                                if(it){
                                    startMainActivity()
                                }
                            }
//                            moduleProviderTermsOfService.getTextToSpeechModule().stopSpeech()
                        }
                    }

                    override fun onCancelClicked() {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCancelClicked")
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        intent.putExtra("tos_canceled", true)
                        startActivity(intent)
                        finishAffinity()
                    }

                    override fun getContext(): Context {
                        return applicationContext
                    }

                    @RequiresApi(Build.VERSION_CODES.R)
                    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
//                        moduleProviderTermsOfService.getTextToSpeechModule().setSpeechText(text = text, importance = importance)
                    }
                })

                customWelcomeScreen?.visibility = View.GONE
                noInternetInfo?.visibility = View.GONE
            }

            val inputString = applicationContext.getSharedPreferences(
                UtilsInterface.PREFS_TAG,
                Context.MODE_PRIVATE
            ).getString("inputSelectedString", "TV")
            if(inputString != "TV") {
                startMainActivity()
            } else {
                if (intent != null && intent.extras != null) {
                    if (intent?.action.equals("keycode_keyinput")) {
                        isTosLaunch = intent?.extras?.getBoolean("is_launch_tos")
                        inputSelected = intent?.extras?.getString("input_main_name").toString()
                        inputSourceSelected = intent?.extras?.getString("input_source_name").toString()
                        inputTuneURL = intent?.extras?.getString("input_tune_url").toString()
                        isBlocked = intent?.extras?.getBoolean("input_blocked")
                    }
                    startedFromMainActivity = intent.getBooleanExtra("from_main_activity", false)
                }
                if (BuildConfig.FLAVOR.contains("base")){
                    startNetworkTimer()
                } else {
                    checkTos()
                }
            }
        }
    }

    private fun startMainActivity() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        if (isTosLaunch == true) {
            intent.action = "keycode_keyinput"
            intent.putExtra("input_source_name", inputSourceSelected)
            intent.putExtra("input_main_name", inputSelected)
            intent.putExtra("input_tune_url", inputTuneURL)
            intent.putExtra("input_blocked", isBlocked)
            ReferenceApplication.isTosFromInput = true
        } else {
            ReferenceApplication.isTosFromInput = false
        }
        if (startedFromMainActivity) {
            setResult(Activity.RESULT_OK, intent)
            finish()
        } else {
            startActivity(intent)
            finishAffinity()
        }
    }

    override fun onResume() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResume: ")
        if(isPaused){
            isPaused = false
            stopNetworkThread = false
            if (BuildConfig.FLAVOR.contains("base")) startNetworkTimer()
        }
        super.onResume()
        runOnUiThread {
            customWelcomeScreen?.initFocus()
        }
    }

    override fun onPause() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onPause: ")
        isPaused = true
        stopNetworkThread = true
        super.onPause()
    }

    private fun checkTos() {
        if (tos != 0 || !checkInternetConnection(applicationContext)) {
            startMainActivity()
        } else {
            runOnUiThread {
                customWelcomeScreen?.visibility = View.VISIBLE
            }
        }
    }

    private fun showNoInternet() {
        runOnUiThread {
            if (noInternetInfo?.visibility != View.VISIBLE) {
                noInternetInfo?.visibility = View.VISIBLE
                customWelcomeScreen?.visibility = View.GONE
            }
        }
    }

    override fun onBackPressed() {
        if (startedFromMainActivity) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            setResult(Activity.RESULT_CANCELED, intent)
        }
        if (customWelcomeScreen?.isWebViewShown()==false) {
            super.onBackPressed()
        } else {
            customWelcomeScreen?.removeWebView()
        }
    }

    override fun onDestroy() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onDestroy")
        super.onDestroy()
        stopNetworkThread = true
    }

    private fun startNetworkTimer() {
        thread {
            while (!stopNetworkThread) {
                val hasInternet = checkInternetConnection(applicationContext)
                if (hasInternet) {
                    checkTos()
                } else {
                    showNoInternet()
                }
                try {
                    Thread.sleep(5000)
                } catch (e: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Thread sleep exception ${e.message}");
                }
            }
        }
    }
    private fun checkInternetConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw =
            connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        val result = when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
        return result
    }

    override fun onNewIntent(intent: Intent?) {
        if (intent?.action.equals("keycode_keyinput")) {
            isTosLaunch = intent?.extras?.getBoolean("is_launch_tos")
            inputSelected = intent?.extras?.getString("input_main_name").toString()
            inputSourceSelected = intent?.extras?.getString("input_source_name").toString()
            inputTuneURL = intent?.extras?.getString("input_tune_url").toString()
            isBlocked = intent?.extras?.getBoolean("input_blocked")
        }
    }
}