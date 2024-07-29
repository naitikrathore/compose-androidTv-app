package com.iwedia.cltv.platform.refplus5

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.GeneralConfigInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.TvConfigRegions
import com.iwedia.cltv.platform.model.parental.Region
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class GeneralConfigInterfaceImpl(private var context: Context,var utilsInterface: UtilsInterface): GeneralConfigInterfaceBaseImpl(context, utilsInterface) {

    private var hashMap: HashMap<String, TvConfigRegions> = HashMap()
    private var country = ""
    @RequiresApi(Build.VERSION_CODES.R)
    override fun setup(raw: InputStream) {
        super.setup(raw)
        CoroutineHelper.runCoroutine({
            println("General Config setup")
            val pathJson = "/vendor/tvconfig/config/app/tv_configuration.json"
            country = Settings.Global.getString(context.contentResolver, "M_CURRENT_COUNTRY_REGION")
            try {
                readConfigFromJson(pathJson)
            }catch (E: Exception){
                E.printStackTrace()
            }
            try {
                setGeneralInfo(country)
            }catch (E: Exception){
                E.printStackTrace()
            }
        }, context = Dispatchers.IO)
    }

    private fun setGeneralInfo(country: String){
        if(hashMap[country]?.pvrEnabled != null){
            if(utilsInterface.getRegion() == Region.US){
                pvr = false
            }else{
                pvr = hashMap[country]!!.pvrEnabled
            }
        }
        if(hashMap[country]?.hbbtvEnabled != null){
            hbbtv = hashMap[country]!!.hbbtvEnabled
        }
        if(hashMap[country]?.timeshiftEnabled != null){
            if(utilsInterface.getRegion() == Region.US){
                timeshift = false
            }else{
                timeshift = hashMap[country]!!.timeshiftEnabled
            }
        }
        if(hashMap[country]?.ttxEnabled != null){
            teletext_enable_column = hashMap[country]!!.ttxEnabled
        }
        if(hashMap[country]?.ciEnabled != null){
            ci = hashMap[country]!!.ciEnabled
        }
        if(hashMap[country]?.blueMute != null){
            blue_mute = hashMap[country]!!.blueMute
        }
        if(hashMap[country]?.softKeyBoardEnable != null){
            virtual_rcu = hashMap[country]!!.softKeyBoardEnable
        }
        if(hashMap[country]?.oad != null){
            oad = hashMap[country]!!.oad
        }
    }

    private fun readConfigFromJson(table: String) {
        var iS: InputStream? = null
        try {
            val file = File(table)
            iS = FileInputStream(file)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
        if(iS != null) {
            val buffer = ByteArray(iS.available())
            iS.read(buffer)
            val oS = ByteArrayOutputStream()
            oS.write(buffer)
            oS.close()
            iS.close()
            val json = JSONObject(String(buffer))
            var tvConfigRegionsCommon: TvConfigRegions?= null
            json.keys().forEach { configs ->
                val nestedJsonForConfig: JSONObject = json.opt(configs) as JSONObject
                nestedJsonForConfig.keys().forEach { nestedConfigs ->
                    if(configs.equals("region")){
                        var tvConfigRegions = TvConfigRegions(nestedConfigs)
                        try {
                            val secondNestedJsonForConfig: JSONObject = nestedJsonForConfig.opt(nestedConfigs) as JSONObject
                            if(tvConfigRegionsCommon == null) {
                                tvConfigRegions.hbbtvEnabled = secondNestedJsonForConfig.optBoolean("hbbtv")
                                tvConfigRegions.pvrEnabled = secondNestedJsonForConfig.optBoolean("pvr")
                                tvConfigRegions.timeshiftEnabled = secondNestedJsonForConfig.optBoolean("timeshift")
                                tvConfigRegions.ttxEnabled = secondNestedJsonForConfig.optBoolean("ttx")
                                tvConfigRegions.ciEnabled = secondNestedJsonForConfig.optBoolean("ci")
                                tvConfigRegions.blueMute = secondNestedJsonForConfig.optBoolean("blue_mute")
                                tvConfigRegions.softKeyBoardEnable = secondNestedJsonForConfig.optBoolean("soft_key_board")
                                tvConfigRegions.oad = secondNestedJsonForConfig.optBoolean("oad")
                            }
                            else{
                                if(!secondNestedJsonForConfig.isNull("hbbtv")){
                                    tvConfigRegions.hbbtvEnabled = secondNestedJsonForConfig.getBoolean("hbbtv")
                                }else{
                                    tvConfigRegions.hbbtvEnabled = tvConfigRegionsCommon!!.hbbtvEnabled
                                }
                                if(!secondNestedJsonForConfig.isNull("pvr")){
                                    tvConfigRegions.pvrEnabled = secondNestedJsonForConfig.getBoolean("pvr")
                                }else{
                                    tvConfigRegions.pvrEnabled = tvConfigRegionsCommon!!.pvrEnabled
                                }
                                if(!secondNestedJsonForConfig.isNull("timeshift")){
                                    tvConfigRegions.timeshiftEnabled = secondNestedJsonForConfig.getBoolean("timeshift")
                                }else{
                                    tvConfigRegions.timeshiftEnabled = tvConfigRegionsCommon!!.timeshiftEnabled
                                }
                                if(!secondNestedJsonForConfig.isNull("ttx")){
                                    tvConfigRegions.ttxEnabled = secondNestedJsonForConfig.getBoolean("ttx")
                                }else{
                                    tvConfigRegions.ttxEnabled = tvConfigRegionsCommon!!.ttxEnabled
                                }
                                if(!secondNestedJsonForConfig.isNull("ci")){
                                    tvConfigRegions.ciEnabled = secondNestedJsonForConfig.getBoolean("ci")
                                }else{
                                    tvConfigRegions.ciEnabled = tvConfigRegionsCommon!!.ciEnabled
                                }
                                if(!secondNestedJsonForConfig.isNull("blue_mute")){
                                    tvConfigRegions.blueMute = secondNestedJsonForConfig.getBoolean("blue_mute")
                                }else{
                                    tvConfigRegions.blueMute = tvConfigRegionsCommon!!.blueMute
                                }
                                if(!secondNestedJsonForConfig.isNull("soft_key_board")){
                                    tvConfigRegions.softKeyBoardEnable = secondNestedJsonForConfig.getBoolean("soft_key_board")
                                }else{
                                    tvConfigRegions.softKeyBoardEnable = tvConfigRegionsCommon!!.softKeyBoardEnable
                                }
                                if(!secondNestedJsonForConfig.isNull("oad")){
                                    tvConfigRegions.oad = secondNestedJsonForConfig.getBoolean("oad")
                                }else{
                                    tvConfigRegions.oad = tvConfigRegionsCommon!!.oad
                                }
                                val jsonArray = secondNestedJsonForConfig.optJSONArray("country")
                                if (jsonArray != null) {
                                    for (i in 0 until jsonArray.length()) {
                                        val item = jsonArray.optString(i)
                                        if(!hashMap.contains(item)) {
                                            hashMap.put(item, tvConfigRegions)
                                        }
                                    }
                                }
                            }
                        }catch (E: Exception){
                            E.printStackTrace()
                        }
                        if(tvConfigRegions.region == "common"){
                            tvConfigRegionsCommon = tvConfigRegions
                        }
                    }
                    else if(configs.equals("countries")){
                        try {
                            //skip the countries that are not needed only get info for selected country
                            if(!nestedConfigs.equals(country)){
                                return@forEach
                            }
                            val secondNestedJsonForConfig: JSONObject = nestedJsonForConfig.opt(nestedConfigs) as JSONObject
                            if(hashMap[nestedConfigs] != null) {
                                if (!secondNestedJsonForConfig.isNull("hbbtv")) {
                                    hashMap[nestedConfigs]!!.hbbtvEnabled = secondNestedJsonForConfig.getBoolean("hbbtv")
                                }
                                if (!secondNestedJsonForConfig.isNull("pvr")) {
                                    hashMap[nestedConfigs]!!.pvrEnabled =
                                        secondNestedJsonForConfig.getBoolean("pvr")
                                }
                                if (!secondNestedJsonForConfig.isNull("timeshift")) {
                                    hashMap[nestedConfigs]!!.timeshiftEnabled =
                                        secondNestedJsonForConfig.getBoolean("timeshift")
                                }
                                if (!secondNestedJsonForConfig.isNull("ttx")) {
                                    hashMap[nestedConfigs]!!.ttxEnabled =
                                        secondNestedJsonForConfig.getBoolean("ttx")
                                }
                                if (!secondNestedJsonForConfig.isNull("ci")) {
                                    hashMap[nestedConfigs]!!.ciEnabled =
                                        secondNestedJsonForConfig.getBoolean("ci")
                                }
                                if (!secondNestedJsonForConfig.isNull("blue_mute")) {
                                    hashMap[nestedConfigs]!!.blueMute =
                                        secondNestedJsonForConfig.getBoolean("blue_mute")
                                }
                                if (!secondNestedJsonForConfig.isNull("soft_key_board")) {
                                    hashMap[nestedConfigs]!!.softKeyBoardEnable =
                                        secondNestedJsonForConfig.getBoolean("soft_key_board")
                                }
                                if (!secondNestedJsonForConfig.isNull("oad")) {
                                    hashMap[nestedConfigs]!!.oad =
                                        secondNestedJsonForConfig.getBoolean("oad")
                                }
                            }
                            else{
                                var newTvConfigRegions = TvConfigRegions("common")
                                if (!secondNestedJsonForConfig.isNull("hbbtv")) {
                                    newTvConfigRegions.hbbtvEnabled =
                                        secondNestedJsonForConfig.getBoolean("hbbtv")
                                }
                                if (!secondNestedJsonForConfig.isNull("pvr")) {
                                    newTvConfigRegions.pvrEnabled =
                                        secondNestedJsonForConfig.getBoolean("pvr")
                                }

                                if (!secondNestedJsonForConfig.isNull("timeshift")) {
                                    newTvConfigRegions.timeshiftEnabled =
                                        secondNestedJsonForConfig.getBoolean("timeshift")
                                }

                                if (!secondNestedJsonForConfig.isNull("ttx")) {
                                    newTvConfigRegions.ttxEnabled =
                                        secondNestedJsonForConfig.getBoolean("ttx")
                                }
                                if (!secondNestedJsonForConfig.isNull("ci")) {
                                    newTvConfigRegions.ciEnabled =
                                        secondNestedJsonForConfig.getBoolean("ci")
                                }
                                if (!secondNestedJsonForConfig.isNull("blue_mute")) {
                                    newTvConfigRegions.blueMute =
                                        secondNestedJsonForConfig.getBoolean("blue_mute")
                                }
                                if (!secondNestedJsonForConfig.isNull("soft_key_board")) {
                                    newTvConfigRegions.softKeyBoardEnable =
                                        secondNestedJsonForConfig.getBoolean("soft_key_board")
                                }
                                if (!secondNestedJsonForConfig.isNull("oad")) {
                                    newTvConfigRegions.oad =
                                        secondNestedJsonForConfig.getBoolean("oad")
                                }
                            }
                        }catch (E: Exception){
                            E.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}