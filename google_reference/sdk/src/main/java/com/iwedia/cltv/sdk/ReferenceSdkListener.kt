package com.iwedia.cltv.sdk

import android.content.Intent
import core_entities.Notification

interface ReferenceSdkListener {

    /**
     * Execute on ui thread
     *
     * @param runnable runnable
     */
    fun runOnUiThread(runnable: Runnable?)

    /**
     * Start activity for result
     *
     * @param intent      intent
     * @param requestCode request code
     */
    fun startActivityForResult(intent: Intent?, requestCode: Int)

    /**
     * Show notification
     *
     * @param notification notification
     */
    fun showNotification(notification: Notification?)

    /**
     * Can show notification
     *
     * @param notification
     */
    fun isCanShowNotification(notification: Notification?): Boolean

    /**
     * Get active scene id
     */
    fun getActiveSceneId(): Int


    /**
     * Get string from files xml
     *
     * @param restId id of string
     */
    fun getNoInformationStringResource(restId: Int): String

    /**
     * Get string from files xml
     *
     * @param restId id of string
     */
    fun getStringResource(restId: Int): String

    /**
     * Get favorite string from files xml
     */
    fun getFavoriteStringTranslationResource(): String

    /**
     * Checks if the app is in paused
     */
    fun isAppInPaused(): Boolean

    /**
     * Check if the settings app is opened
     */
    fun isSettingsOpened(): Boolean

    /**
     * On app initialized callback
     */
    fun onAppInitialized()
}