package com.iwedia.cltv.platform.mal_service

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.GeneralConfigInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.JsonGeneralSettingsEntity
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.model.content_provider.Contract
import com.iwedia.cltv.platform.model.parental.Region
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GeneralConfigInterfaceImpl (private var context: Context, var serviceImpl: IServiceAPI):
    GeneralConfigInterface {

    private var virtual_rcu : Boolean = false
    private var vertical_epg : Boolean = false
    private var epgCellMerge : Boolean = false
    private var pvr : Boolean = false
    private var timeshift : Boolean = false
    private var hbbtv : Boolean = true
    private var ttx : Boolean = true
    private var ci : Boolean = true
    private var blue_mute : Boolean = true
    private var thirdPartyInput: Boolean = false
    private var generalSettingsJsonUrlPath: String = ""
    private var channel_logo_metadata : Boolean = false
    private var subtitleInt : Int = 0
    private var cable_scan_enabled : Boolean = false
    private var scan_type_switch : Boolean = false
    private var countrySelect : String = ""
    private var signal_status_monitoring_enabled : Boolean = false
    private var clear_channels_scan_option_enabled : Boolean = false
    private var aspect_ratio_enabled : Boolean = false
    private var teletext_enable_column: Boolean = false
    private var delete_third_party_enabled : Boolean = false

    private var rf_channel_number_enabled : Boolean = false
    private var ber_enabled : Boolean = false
    private var frequency_enabled : Boolean = false
    private var prog_enabled : Boolean = false
    private var uec_enabled : Boolean = false
    private var serviceId_enabled : Boolean = false
    private var postViterbi_enabled : Boolean = false
    private var tsId_enabled : Boolean = false
    private var fiveS_enabled : Boolean = false
    private var onId_enabled : Boolean = false
    private var agc_enabled : Boolean = false
    private var networkId_enabled : Boolean = false
    private var networkName_enabled : Boolean = false
    private var bandwidth_enabled : Boolean = false

    /** Module provider */

    @RequiresApi(Build.VERSION_CODES.R)
    override fun setup(raw: InputStream) {
        parseGeneralInfo(raw)
        parseSystemInformationInfo()
    }


    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("Range")
    private fun parseGeneralInfo(raw: InputStream) {

        vertical_epg = readFromJson(Contract.OemCustomization.VERTICAL_EPG_COLUMN) == 1
        virtual_rcu = readFromJson(Contract.OemCustomization.VIRTUAL_RCU_COLUMN) == 1
        channel_logo_metadata = readFromJson(Contract.OemCustomization.CHANNEL_LOGO_METADATA_ENABLED_COLUMN) == 1
        subtitleInt = readFromJson(Contract.OemCustomization.SUBTITLE_COLUMN_ID)
        signal_status_monitoring_enabled = readFromJson(Contract.OemCustomization.SIGNAL_STATUS_MONITORING_ENABLED_COLUMN) == 1
        aspect_ratio_enabled = readFromJson(Contract.OemCustomization.ASPECT_RATIO_ENABLED_COLUMN) == 1
        if(serviceImpl.region.ordinal == Region.EU.ordinal) {
            teletext_enable_column =
                readFromJson(Contract.OemCustomization.TELETEXT_ENABLE_COLUMN) == 1
        }else{
            teletext_enable_column = false
        }

        if(serviceImpl.region.ordinal == Region.US.ordinal) {
            pvr = false
            timeshift = false
        }else{
            pvr = readFromJson(Contract.OemCustomization.PVR_ENABLED_COLUMN) == 1
            timeshift = readFromJson(Contract.OemCustomization.TIMESHIFT_ENABLED_COLUMN) == 1
        }

        val contentResolver: ContentResolver = context.contentResolver
        var cursor = contentResolver.query(
            ContentProvider.OEM_CUSTOMIZATION_URI,
            null,
            null,
            null,
            null
        )
        if (cursor!!.count > 0) {
            cursor.moveToFirst()
            do {
                if (cursor.getInt(cursor.getColumnIndex(Contract.OemCustomization.EPG_CELL_MERGE_COLUMN)) != null) {
                    epgCellMerge = cursor.getInt(cursor.getColumnIndex(
                        Contract.OemCustomization.EPG_CELL_MERGE_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.OemCustomization.THIRD_PARTY_INPUT_ENABLED_COLUMN)) != null) {
                    thirdPartyInput = cursor.getInt(cursor.getColumnIndex(
                        Contract.OemCustomization.THIRD_PARTY_INPUT_ENABLED_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.OemCustomization.CABLE_SCAN_ENABLED_COLUMN)) != null) {
                    cable_scan_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.OemCustomization.CABLE_SCAN_ENABLED_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.OemCustomization.SCAN_TYPE_SWITCH)) != null) {
                    scan_type_switch = cursor.getInt(cursor.getColumnIndex(
                        Contract.OemCustomization.SCAN_TYPE_SWITCH)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.OemCustomization.COUNTRY_SELECT)) != null) {
                    countrySelect = cursor.getString(cursor.getColumnIndex(Contract.OemCustomization.COUNTRY_SELECT))
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.OemCustomization.CLEAR_CHANNELS_SCAN_OPTION_ENABLED_COLUMN)) != null) {
                    clear_channels_scan_option_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.OemCustomization.CLEAR_CHANNELS_SCAN_OPTION_ENABLED_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.OemCustomization.DELETE_THIRD_PARTY_INPUT_COLUMN)) != null) {
                    delete_third_party_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.OemCustomization.DELETE_THIRD_PARTY_INPUT_COLUMN)) == 1
                }
            } while (cursor.moveToNext())
        } else {
            val rd: Reader = BufferedReader(InputStreamReader(raw))
            val gson = Gson()
            val itemType = object : TypeToken<JsonGeneralSettingsEntity>() {}.type
            val generalDetails = gson.fromJson<JsonGeneralSettingsEntity>(rd, itemType)

            virtual_rcu = generalDetails.virtual_rcu!!
            vertical_epg = generalDetails.vertical_epg!!
            if(serviceImpl.region.ordinal == Region.EU.ordinal) {
                pvr = generalDetails.pvr_enabled!!
                timeshift = generalDetails.timeshift_enabled!!
            }else{
                pvr = false
                timeshift = false
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.R)
    private fun readFromJson(name: String): Int {
        val iS = context.classLoader.getResourceAsStream("assets/OemCustomizationJsonFolder/OemCustomizationBase.json")
        val buffer = ByteArray(iS.available())
        iS.read(buffer)
        val oS = ByteArrayOutputStream()
        oS.write(buffer)
        oS.close()
        iS.close()
        val json = JSONObject(String(buffer))
        val scanArray: JSONArray = json.getJSONArray("oem")
        var returnInt: Int = 0
        for (i in 0 until scanArray.length()) {
            val scanObject: JSONObject = scanArray.getJSONObject(i)
            returnInt = scanObject.getInt(name)
        }
        return returnInt
    }

    @SuppressLint("Range")
    private fun parseSystemInformationInfo() {
        val contentResolver: ContentResolver = context.contentResolver
        var cursor = contentResolver.query(
            ContentProvider.SYSTEM_INFO_URI,
            null,
            null,
            null,
            null
        )
        if (cursor!!.count > 0) {
            cursor.moveToFirst()
            do {
                if (cursor.getInt(cursor.getColumnIndex(Contract.SystemInfo.RF_CHANNEL_NUMBER_ENABLED_COLUMN)) != null) {
                    rf_channel_number_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.SystemInfo.RF_CHANNEL_NUMBER_ENABLED_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.SystemInfo.BER_ENABLED_COLUMN)) != null) {
                    ber_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.SystemInfo.BER_ENABLED_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.SystemInfo.FREQUENCY_ENABLED_COLUMN)) != null) {
                    frequency_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.SystemInfo.FREQUENCY_ENABLED_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.SystemInfo.PROG_ENABLED_COLUMN)) != null) {
                    prog_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.SystemInfo.PROG_ENABLED_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.SystemInfo.UEC_ENABLED_COLUMN)) != null) {
                    uec_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.SystemInfo.UEC_ENABLED_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.SystemInfo.SERVICEID_ENABLED_COLUMN)) != null) {
                    serviceId_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.SystemInfo.SERVICEID_ENABLED_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.SystemInfo.POSTVITERBI_ENABLED_COLUMN)) != null) {
                    postViterbi_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.SystemInfo.POSTVITERBI_ENABLED_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.SystemInfo.TSID_ENABLED_COLUMN)) != null) {
                    tsId_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.SystemInfo.TSID_ENABLED_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.SystemInfo.FIVES_ENABLED_COLUMN)) != null) {
                    fiveS_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.SystemInfo.FIVES_ENABLED_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.SystemInfo.ONID_ENABLED_COLUMN)) != null) {
                    onId_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.SystemInfo.ONID_ENABLED_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.SystemInfo.AGC_ENABLED_COLUMN)) != null) {
                    agc_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.SystemInfo.AGC_ENABLED_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.SystemInfo.NETWORKID_ENABLED_COLUMN)) != null) {
                    networkId_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.SystemInfo.NETWORKID_ENABLED_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.SystemInfo.NETWORKNAME_ENABLED_COLUMN)) != null) {
                    networkName_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.SystemInfo.NETWORKNAME_ENABLED_COLUMN)) == 1
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.SystemInfo.BANDWIDTH_ENABLED_COLUMN)) != null) {
                    bandwidth_enabled = cursor.getInt(cursor.getColumnIndex(
                        Contract.SystemInfo.BANDWIDTH_ENABLED_COLUMN)) == 1
                }
            } while (cursor.moveToNext())
        }
    }

    override fun getGeneralSettingsInfo(generalParam: String): Boolean {
        when (generalParam) {
            "virtual_rcu" -> {
                return virtual_rcu
            }
            "vertical_epg" -> {
                return vertical_epg
            }
            "pvr" -> {
                return pvr
            }
            "epg_cell_merge" -> {
                return epgCellMerge
            }
            "third_party_input" -> {
                return thirdPartyInput
            }
            "channel_logo_metadata_enabled" -> {
                return channel_logo_metadata
            }
            "cable_scan_enabled" -> {
                return cable_scan_enabled
            }
            "scan_type_switch" -> {
                return scan_type_switch
            }
            "signal_status_monitoring_enabled" ->{
                return signal_status_monitoring_enabled
            }
            "clear_channels_scan_option_enabled" -> {
                return clear_channels_scan_option_enabled
            }
            "aspect_ratio_enabled" -> {
                return aspect_ratio_enabled
            }
            "delete_third_party_enabled" -> {
                return delete_third_party_enabled
            }
            "rf_channel_number_enabled" -> {
                return rf_channel_number_enabled
            }
            "ber_enabled" -> {
                return ber_enabled
            }
            "frequency_enabled" -> {
                return frequency_enabled
            }
            "prog_enabled" -> {
                return prog_enabled
            }
            "uec_enabled" -> {
                return uec_enabled
            }
            "serviceId_enabled" -> {
                return serviceId_enabled
            }
            "postViterbi_enabled" -> {
                return postViterbi_enabled
            }
            "tsId_enabled" -> {
                return tsId_enabled
            }
            "fiveS_enabled" -> {
                return fiveS_enabled
            }
            "onId_enabled" -> {
                return onId_enabled
            }
            "agc_enabled" -> {
                return agc_enabled
            }
            "networkId_enabled" -> {
                return networkId_enabled
            }
            "networkName_enabled" -> {
                return networkName_enabled
            }
            "bandwidth_enabled" -> {
                return bandwidth_enabled
            }
            "teletext_enable_column" -> {
                return teletext_enable_column
            }
            "timeshift" -> {
                return timeshift
            }
            "hbbtv" -> {
                return hbbtv
            }
            "ci" -> {
                return ci
            }
            "ttx" -> {
                return ttx
            }
            "blue_mute" -> {
                return blue_mute
            }
            else -> return false
        }
    }
    override fun getCountryThatIsSelected(): String{
        return countrySelect
    }
    override val getEpgMergeStatus: Boolean
        get() = getGeneralSettingsInfo("epg_cell_merge")

}