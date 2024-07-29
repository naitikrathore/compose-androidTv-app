package com.iwedia.cltv.platform.base

import android.content.Context
import android.media.tv.TvView
import android.view.KeyEvent
import android.view.SurfaceView
import android.view.ViewGroup
import com.iwedia.cltv.platform.`interface`.HbbTvInterface
import com.iwedia.cltv.platform.`interface`.HbbTvInterface.HbbTvCookieSettingsValue
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel

open class HbbTvInterfaceBaseImpl(private val utilsInterfaceImpl: UtilsInterfaceBaseImpl) : HbbTvInterface {
    private val HBBTV_SUPPORT = "HbbTvSupport"
    private val HBBTV_TRACK = "HbbTvTrack"

    override fun initHbbTv(context: Context, tvChannel: TvChannel) {}

    override fun enableHbbTv() {
    }

    override fun disableHbbTv() {
    }

    override fun onActivityStart() {
    }

    override fun sendKeyToHbbTvEngine(keyCode: Int, event: KeyEvent?,type : HbbTvInterface.HbbTvKeyEventType): Boolean {
        return false
    }

    override fun checkSupportHbbtv(): Boolean{
        return false
    }

    override fun setHbbTVFocus(focused: Boolean, fullScreen: Boolean) {

    }

    override fun isHbbTvActive(): Boolean {
        return false
    }

    override fun supportHbbTv(isEnabled :Boolean) {
    }

    override fun disableHbbTvTracking(isEnabled :Boolean) {
    }

    override fun cookieSettingsHbbTv(value: HbbTvCookieSettingsValue) {
    }

    override fun persistentStorageHbbTv(isEnabled :Boolean) {
    }

    override fun blockTrackingSitesHbbTv(isEnabled :Boolean) {
    }

    override fun deviceIdHbbTv(isEnabled :Boolean) {
    }

    override fun resetDeviceIdHbbTv() {
    }

    override fun registerHbbTvUnusedKeyReply(callback: IAsyncDataCallback<HbbTvInterface.HbbTvUnusedKeyReply>) {
    }

    override fun setSurfaceView(hbbtvSurfaceView: SurfaceView, displayId: Int, sessionId: Int) {
    }

    override fun setTvView(tvView: TvView, tvViewParent: ViewGroup, windowWidth : Float, windowHeight : Float) {
    }

    override fun notifyHbbTvChannelLockUnlock(status: Boolean) {
    }

    override fun updateParentalControl() {
    }

    override fun getHbbtvFunctionSwitch(): Boolean {
        var defaultValue = utilsInterfaceImpl.getCountryPreferences(UtilsInterface.CountryPreference.ENABLE_HBBTV_BY_DEFAULT,true) as Boolean
        return utilsInterfaceImpl.getPrefsValue(
            HBBTV_SUPPORT,
            defaultValue
        ) as Boolean
    }

    override fun getHbbtvDoNotTrack(): Boolean {
        return utilsInterfaceImpl.getPrefsValue(
            HBBTV_TRACK,
            false
        ) as Boolean
    }
}