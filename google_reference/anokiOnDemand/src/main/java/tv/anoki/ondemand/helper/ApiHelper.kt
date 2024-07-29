package tv.anoki.ondemand.helper

import android.content.SharedPreferences
import tv.anoki.ondemand.constants.ApiConstants
import tv.anoki.ondemand.constants.PrefsConstants

/**
 * The function to build options map for api calls
 *
 * @param sharedPreferences the interface to get locally stored data
 */
fun buildOptionsMap(sharedPreferences: SharedPreferences): HashMap<String, String> {
    val options: HashMap<String, String> = HashMap()
    options[ApiConstants.KEY_PARAM_COUNTRY] = sharedPreferences.getString(PrefsConstants.PREFS_KEY_CURRENT_COUNTRY_ALPHA3, "USA").toString()
    options[ApiConstants.KEY_PARAM_AUID] = sharedPreferences.getString(PrefsConstants.PREFS_KEY_AUID, "").toString()
    options[ApiConstants.KEY_PARAM_DEVICE_ID] = sharedPreferences.getString(PrefsConstants.PREFS_KEY_ADVERTISING_ID, "").toString()
    options[ApiConstants.KEY_PARAM_IP] = sharedPreferences.getString(PrefsConstants.PREFS_KEY_IP_ADDRESS, "").toString()

    return options
}