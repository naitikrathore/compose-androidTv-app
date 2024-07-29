package com.iwedia.cltv.fti.scan_models

import android.content.Context
import com.google.gson.Gson
import com.iwedia.cltv.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream


class ConfigParser(context: Context)  {
    val TAG = "ConfigParser"

    var instance: ConfigParser? = null
    var gson: Gson? = null

    var scanConfigList: ArrayList<DataParams>

    fun fillList(context: Context) {
        try {
            val iS: InputStream =
                context.getResources().openRawResource(R.raw.scan_range_array)
            val buffer = ByteArray(iS.available())
            iS.read(buffer)
            val oS = ByteArrayOutputStream()
            oS.write(buffer)
            oS.close()
            iS.close()

            val config = JSONObject(String(buffer))
            val scanArray: JSONArray = config.getJSONArray("Scan")
            for (i in 0 until scanArray.length()) {
                val scanObject: JSONObject = scanArray.getJSONObject(i)

                val countryTag: String = scanObject.getString("countryTag")
                val countryName: String = scanObject.getString("country")
                val defaultLcn: Int = scanObject.getInt("defaultLCN")

                val range: JSONArray = scanObject.getJSONArray("range")
                val rdm: ArrayList<TerrestrialRangeModel> = ArrayList<TerrestrialRangeModel>()
                if (range == null) {
                    continue
                } else {
                    if (!rdm.isEmpty()) {
                        rdm.clear()
                    }
                    for (j in 0 until range.length()) {
                        val opObject: JSONObject = range.getJSONObject(j)
                        rdm.add(
                            TerrestrialRangeModel(
                                opObject.getInt("frequencyMin"),
                                opObject.getInt("frequencyMax"),
                                opObject.getInt("step"),
                                opObject.getInt("bandwidth")
                            )
                        )
                    }
                }

                val operators: JSONArray = scanObject.getJSONArray("operators")
                val cdm: ArrayList<CableOperatorModel> = ArrayList<CableOperatorModel>()
                if (operators == null) {
                    continue
                } else {
                    if (!cdm.isEmpty()) {
                        cdm.clear()
                    }
                    for (j in 0 until operators.length()) {
                        val opObject: JSONObject = operators.getJSONObject(j)
                        cdm.add(
                            CableOperatorModel(
                                opObject.getString("name"),
                                opObject.getInt("frequency"),
                                opObject.getInt("modulation"),
                                opObject.getInt("symbolRate"),
                                opObject.getInt("networkId")
                            )
                        )
                    }
                }

                val pvrEnabled: Int = scanObject.getInt("pvrEnabled")

                scanConfigList.add(DataParams(countryTag,countryName,defaultLcn,rdm,cdm, pvrEnabled))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "TerrestrialConfigParser"
        private var instance: ConfigParser? = null
        fun getInstance(context: Context): ConfigParser? {
            if (instance == null) {
                instance = ConfigParser(context)
            }
            return instance
        }
    }

    init {
        scanConfigList = ArrayList<DataParams>()
        gson = Gson()
        fillList(context)
    }


}