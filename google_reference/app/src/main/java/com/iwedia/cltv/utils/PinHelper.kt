package com.iwedia.cltv.utils

import android.app.Activity
import android.os.Handler
import android.os.UserHandle
import com.google.android.tv.inputplayer.api.InputLock
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.platform.model.CoroutineHelper
import kotlinx.coroutines.Dispatchers


/**
 * Helper class for the new Google Tv Input Player PIN dialog integration
 *
 * @author Dejan Nadj
 */
object PinHelper {

    val USE_PIN_INSERT_ACTIVITY = android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.R

    /**
     * PIN activity requestCode
     */
    val REQUEST_CONFIRM_PIN = 1

    /**
     * Pin insert callback
     */
    private var pinCallback: PinCallback? = null

    /**
     * Pin activity loading callback
     * Show black screen during activity starting
     */
    private lateinit var loadingCallback: (show: Boolean) -> Unit

    /**
     * Activity started flag
     * Used to avoid multiple activity start
     */
    private var isStarted = false

    fun isPinActivityStarted(): Boolean = isStarted

    interface PinCallback {
        fun pinCorrect()
        fun pinIncorrect()
    }

    /**
     * Start pin activity
     *
     * @param title PIN dialog title
     * @param desc PIN dialog description
     */
    fun startPinActivity(title: String, desc: String) {
        if (isStarted) {
            return
        }
        isStarted = true
        Handler().post {
            showLoading(true)
        }

        CoroutineHelper.runCoroutine({
            // Find startActivityForResultAsUser method and call it by using reflection mechanism
            val myClass = Activity::class
            val members = myClass.members
            val intent = InputLock.createPinAuthIntent(title, desc)
            val userHandle = UserHandle.getUserHandleForUid(-2)
            for (member in members) {
                if ((member.name == "startActivityForResultAsUser") && (member.parameters.size == 4)) {
                    member.call(
                        ReferenceApplication.getActivity(),
                        intent,
                        REQUEST_CONFIRM_PIN,
                        userHandle
                    )
                }
            }
        },Dispatchers.IO)

        Handler().postDelayed({
            showLoading(false)
        }, 5000)
    }

    private fun showLoading(show: Boolean) {
        if (::loadingCallback.isInitialized) {
            ReferenceApplication.runOnUiThread {
                loadingCallback.invoke(show)
            }
        }
    }

    fun setPinResultCallback(callback: PinCallback) {
        pinCallback = callback
    }

    fun setLoadingLayoutCallback(callback: (show: Boolean) -> Unit) {
        loadingCallback = callback
    }

    fun triggerPinResultCallback(isCorrect: Boolean) {
        if (pinCallback != null) {
            if (isCorrect) {
                pinCallback?.pinCorrect()
            } else {
                pinCallback?.pinIncorrect()
            }
        }
        pinCallback = null
        isStarted = false
    }
}