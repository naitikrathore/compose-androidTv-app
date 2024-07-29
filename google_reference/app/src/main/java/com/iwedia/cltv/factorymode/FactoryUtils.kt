
package com.iwedia.cltv.factorymode
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import com.iwedia.cltv.platform.model.Constants


class FactoryUtils {
    val TAG = "FactoryUtils"
    companion object {
        private const val PREF_KEY_EXIST_FACTORY_APP = "pref_exist_factory_app"
        const val FACTORY_PACKAGE_NAME = "jp.funai.android.tv.factory"
        const val FACTORY_ENTRY_CLASS_NAME = "jp.funai.android.tv.factory.ui.InitialScreen"
        const val FACTORY_START_CODE = "START_CODE"
        const val FACTORY_EXTRA_NAME_CODE = "name"
        const val FACTORY_EXTRA_NUM_CODE = "number"
        const val FACTORY_START_CODE_EMPTY = 0
        const val FACTORY_START_CODE_AGING_MENU = 13
        const val FACTORY_START_CODE_TEST_PATTERN_MENU = 14
        const val FACTORY_START_CODE_CHANNEL_REGISTERNG_MENU = 19
        const val FACTORY_REQUEST_CODE = 999
        const val SELECTINPUT_REQUEST_CODE = 888
        const val RESULT_FINISH = -9
        const val FACTORY_EXTRA_SELECT_CODE = "select"
        //Preference key
        private const val PREF_KEY_NEED_RESTORE_HDMI_SETTINGS = "pref_need_restore_hdmi_settings"
        private const val PREF_KEY_HDMI_CONTROL = "pref_hdmi_control"
        private const val PREF_KEY_DEVICE_POWER_OFF = "pref_device_power_off"
        private const val PREF_KEY_TV_POWER_ON = "pref_tv_power_on"
        val tvInputId = "com.mediatek.tvinput/.tuner.TunerInputService/HW8"
    }
    fun isExistFactory(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PREF_KEY_EXIST_FACTORY_APP, true)
    }
    fun setIsExistFactory(context: Context?, param: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(PREF_KEY_EXIST_FACTORY_APP, param)
            .apply()
    }
    //update HDMI-CEC Settings param
    fun writeCecOption(context: Context, key: String, value: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, key, if (value) 1 else 0)
        } catch (e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "writeCecOption: ${e.printStackTrace()}")
        }
    }
    //check HDMI-CEC Settings param
    fun readCecOption(context: Context,key: String): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, key, 1) == 1
        } catch (e: java.lang.Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "readCecOption: ${e.printStackTrace()}")
            false
        }
    }
    fun isNeedRestoreHdmiSettings(context: Context?): Boolean {
        Log.v(TAG, "isNeedRestoreHdmiSettings()")
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PREF_KEY_NEED_RESTORE_HDMI_SETTINGS, false)
    }
    fun setNeedRestoreHdmiSettings(context: Context?, param: Boolean) {
        Log.v(TAG, "setNeedRestoreHdmiSettings():param= $param")
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(PREF_KEY_NEED_RESTORE_HDMI_SETTINGS, param)
            .apply()
    }
    fun isHdmiControlSettings(context: Context?): Boolean {
        Log.v(TAG, "isHdmiControlSettings()")
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PREF_KEY_HDMI_CONTROL, true)
    }
    fun setHdmiControlSettings(context: Context?, param: Boolean) {
        Log.v(TAG, "setNeedRestoreHdmiSettings():param= $param")
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(PREF_KEY_HDMI_CONTROL, param)
            .apply()
    }
    fun isDevicePowerOffSettings(context: Context?): Boolean {
        Log.v(TAG, "isDevicePowerOffSettings()")
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PREF_KEY_DEVICE_POWER_OFF, true)
    }
    fun setDevicePowerOffSettings(context: Context?, param: Boolean) {
        Log.v(TAG, "setDevicePowerOffSettings():param= $param")
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(PREF_KEY_DEVICE_POWER_OFF, param)
            .apply()
    }
    fun isTvPowerOffSettings(context: Context?): Boolean {
        Log.v(TAG, "isTvPowerOffSettings()")
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PREF_KEY_TV_POWER_ON, true)
    }
    fun setTvPowerOffSettings(context: Context?, param: Boolean) {
        Log.v(TAG, "setTvPowerOffSettings():param= $param")
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(PREF_KEY_TV_POWER_ON, param)
            .apply()
    }

    /*
     This always returns true (as communicated by Funai)
    */
    fun getInitialToVirginFlag(): Boolean {
        return true
    }

    fun setFactoryTvFlag(context: Context, flag: Boolean) {
        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +"FactoryModeActivity", "setFactoryTvFlag():flag= $flag")
        try {
            var flagValue: Int = FactoryTvProvider.FACTORY_TV_FLAG_OFF
            if (flag) {
                flagValue = FactoryTvProvider.FACTORY_TV_FLAG_ON
            }
            val values = ContentValues()
            values.put(FactoryTvProvider.KEY_FACTORY_TV_FLAG, flagValue)
            context.contentResolver.update(
                Uri.parse("content://com.iwedia.cltv.factorymode.FactoryTvProvider/" + FactoryTvProvider.KEY_FACTORY_TV_FLAG),
                values, null, null
            )
        } catch (throwable: Throwable) {
            Log.d(Constants.LogTag.CLTV_TAG + "FactoryModeActivity", "setFactoryTvFlag is NG")
            throwable.printStackTrace()
        }
    }
    @SuppressLint("Range")
    fun getFactoryTvFlag(context: Context): Boolean {
        Log.d(Constants.LogTag.CLTV_TAG + "FactoryModeActivity", "getFactoryTvFlag()")
        var flag = false
        try {
            val cursor = context.contentResolver.query(
                Uri.parse("content://com.iwedia.cltv.factorymode.FactoryTvProvider/" + FactoryTvProvider.KEY_FACTORY_TV_FLAG),
                arrayOf<String>(FactoryTvProvider.KEY_FACTORY_TV_FLAG),
                null,
                null,
                null
            )
            if (cursor!!.moveToFirst()) {
                Log.d(Constants.LogTag.CLTV_TAG + "FactoryModeActivity", "cursor!!.moveToFirst()")
                if (cursor.position >= 0) {
                    Log.d(Constants.LogTag.CLTV_TAG + "FactoryModeActivity", "cursor.position >= 0")
                    if (cursor.getString(cursor.getColumnIndex("key")) == FactoryTvProvider.KEY_FACTORY_TV_FLAG) {
                        Log.d(Constants.LogTag.CLTV_TAG + "FactoryModeActivity", "cursor.getString(cursor.getColumnIndex(\"key\")) == FactoryTvProvider.KEY_FACTORY_TV_FLAG")
                        flag = cursor.getInt(cursor.getColumnIndex("value")) > 0
                    }
                }
            }
            cursor.close()
        } catch (throwable: Throwable) {
            Log.d(Constants.LogTag.CLTV_TAG + "FactoryModeActivity", "getFactoryTvFlag is NG")
            throwable.printStackTrace()
            flag = false
        }
        return flag
    }
}
